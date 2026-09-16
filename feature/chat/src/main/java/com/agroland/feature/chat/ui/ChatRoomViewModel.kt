package com.agroland.feature.chat.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.common.settings.SettingsDataStore
import com.agroland.core.network.ApiResult
import com.agroland.core.network.error.Failure
import com.agroland.feature.chat.data.ChatGeocoder
import com.agroland.feature.chat.data.ChatMessage
import com.agroland.feature.chat.data.ChatParser
import com.agroland.feature.chat.data.ChatRepository
import com.agroland.feature.chat.data.ChatSocketManager
import com.agroland.feature.chat.data.ChatSocketManager.Subscription
import com.agroland.feature.chat.data.CurrentUserProvider
import com.agroland.feature.chat.data.FileTooLargeException
import com.agroland.feature.chat.domain.ChatSocketService
import com.agroland.feature.chat.domain.InferMessageType
import com.agroland.feature.chat.domain.InferredMessageType
import com.agroland.feature.marketplace.data.MarketplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.socket.client.Socket
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Жарнама карточкасының көрсетілетін деректері (Option A/B біріктірілген). */
data class AnnouncementCard(
    val id: Long,
    val title: String,
    val price: Double?,
    val mainImageUrl: String?,
)

/** Медиа жүктеу күйі (файл/дауыс/сурет). */
data class UploadState(
    val progress: Int,
    val kind: UploadKind,
)

enum class UploadKind { IMAGE, VIDEO, FILE, AUDIO }

/** Бөлме күйі (Flutter ChatRoomPage state паритеті). */
data class ChatRoomUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val replyTo: ChatMessage? = null,
    val editing: ChatMessage? = null,
    val roomId: Long? = null,
    val joinResponded: Boolean = false,
    val loadTimedOut: Boolean = false,
    val hasMore: Boolean = true,
    val loadingOlder: Boolean = false,
    val peerTyping: Boolean = false,
    val connectionState: ChatSocketManager.State = ChatSocketManager.State.DISCONNECTED,
    /** «Жеткізе аламын» жауабы → астына «Сумма» өрісі. */
    val dealerDeliveryReplyYes: Boolean = false,
    val deliveryPrice: String = "",
    /** Сатушы жауап берген delivery_request id-лары. */
    val answeredRequestIds: Set<Long> = emptySet(),
    /** Сатып алушы таңдаған (pay/pickup) delivery_request id-лары. */
    val buyerChosenIds: Set<Long> = emptySet(),
    /** Жүйелі чаттар: {31, 1003} жазуға болады, {1001,1002,1004,1005} — тек оқу. */
    val readOnly: Boolean = false,
    val announcementCardSent: Boolean = false,
    val deliveryRequestSent: Boolean = false,
    val upload: UploadState? = null,
    /** Option B префетчі: announcementId → карточка. */
    val announcements: Map<Long, AnnouncementCard> = emptyMap(),
    /** Жаңа хабарлама келді → төменге автожылжу сигналы (counter). */
    val scrollToBottom: Int = 0,
    /** load_older prepend саны — скролл позициясын сақтау үшін. */
    val prependCount: Int = 0,
    val me: CurrentUserProvider.CurrentUser? = null,
    /** Бөлме тізімінен келген мәтінмән (контекст-карточка). */
    val initialAnnouncementId: Long? = null,
)

/** Snackbar / навигация оқиғалары. */
sealed interface ChatRoomEvent {
    enum class Toast : ChatRoomEvent { QUEUED, CONNECTION_LOST, SEND_ERROR, COPIED, FILE_TOO_LARGE, VIDEO_PREPARING, SOMETHING_WRONG, DELIVERY_REQUEST_SENT, CHAT_CLEARED, VOICE_TOO_SHORT, RECORD_ERROR, FILE_OPEN_ERROR, AUDIO_PLAY_ERROR, LOAD_FAILED, LOCATION_ERROR, CAMERA_UNAVAILABLE, PHOTO_PICK_ERROR, FILE_SELECT_ERROR }
    data class OpenAnnouncement(val announcementId: Long) : ChatRoomEvent
    data object PopRoom : ChatRoomEvent
}

/**
 * Чат бөлмесі ViewModel (Flutter ChatRoomPage, 5555 жол паритеті):
 *  - join state machine: emit → 4с/8с/16с retry (макс 3), listeners БҰРЫН;
 *  - optimistic send (localId temp_N) + echo/fuzzy dedup + оффлайн кезек;
 *  - load_older (50, hasMore bool|string) + prepend;
 *  - typing, message_read, message_edited/deleted, history cleared;
 *  - delivery-request ағыны (сатып алушы → сатушы → таңдау чиптері);
 *  - жарнама карточкасы: Option A (payload) + Option B (getAnnouncement).
 */
@HiltViewModel
class ChatRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val manager: ChatSocketManager,
    private val service: ChatSocketService,
    private val repository: ChatRepository,
    private val currentUser: CurrentUserProvider,
    private val geocoder: ChatGeocoder,
    private val marketplace: MarketplaceRepository,
    private val settings: SettingsDataStore,
) : ViewModel() {

    private val initialRoomId: Long? = savedStateHandle["roomId"]
    private val initialOtherUserId: Long? = savedStateHandle["otherUserId"]
    private val initialUsername: String = savedStateHandle["username"] ?: ""
    private val initialAnnouncementId: Long? = savedStateHandle["announcementId"]
    private val initialDeliveryAddress: String = savedStateHandle["deliveryAddress"] ?: ""
    private val initialIsSystemChat: Boolean = savedStateHandle["isSystemChat"] ?: false
    private val initialRoomAnnouncementId: Long? = savedStateHandle["roomAnnouncementId"]

    private val _state = MutableStateFlow(
        ChatRoomUiState(
            roomId = initialRoomId,
            readOnly = initialIsSystemChat && initialOtherUserId in READ_ONLY_SERVICE_IDS,
            initialAnnouncementId = initialAnnouncementId ?: initialRoomAnnouncementId,
        ),
    )
    val state: StateFlow<ChatRoomUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ChatRoomEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<ChatRoomEvent> = _events.asSharedFlow()

    /** Peer кеші — тізімнен алынған бөлме деректері (аватар, online, last seen). */
    private val _peerRoom = MutableStateFlow<com.agroland.feature.chat.data.ChatRoom?>(null)
    val peerRoom: StateFlow<com.agroland.feature.chat.data.ChatRoom?> = _peerRoom.asStateFlow()

    private var language: String = "kk"
    private var roomName: String? = null
    private var joinAttempts = 0
    private var joinRespondedInternal = false
    private var joinTimer: Job? = null
    private var localIdCounter = 0
    private val queuedLocalIds = mutableSetOf<String>()
    private var renderedRoomId: Long? = null
    private var deliveryRequestSent = false
    private var announcementCardSent = false
    private var subscriptions = mutableListOf<Subscription>()
    private var subscribedSocket: Socket? = null
    /** Сатушы чип-жауаптарының локализацияланған мәтіндері (беттен беріледі). */
    private var deliveryYesText = ""
    private var deliveryNoText = ""
    private var deliverySumLabel = ""

    init {
        viewModelScope.launch { currentUser.refresh() }

        // Socket күйі + ашық бөлме.
        viewModelScope.launch {
            manager.state.collect { value ->
                _state.value = _state.value.copy(connectionState = value)
                if (value == ChatSocketManager.State.CONNECTED) {
                    // Қайта қосылым — join қайта жіберіледі (joinResponded болмаса).
                    ensureHandlers()
                    if (!joinRespondedInternal) {
                        scheduleJoinNow()
                    } else {
                        flushQueuedMessages()
                    }
                }
            }
        }
        viewModelScope.launch {
            currentUser.user.collect { me ->
                _state.value = _state.value.copy(me = me)
                maybeSendDeliveryRequest()
            }
        }
        // Peer бөлме деректері (аватар/online/last seen) — тізім кешінен.
        viewModelScope.launch {
            service.activeRooms.collect { rooms ->
                syncPeerRoom(rooms)
            }
        }
        viewModelScope.launch {
            service.archivedRooms.collect { rooms ->
                syncPeerRoom(rooms)
            }
        }

        // Бастапқы деректер + join.
        viewModelScope.launch {
            language = settings.locale.first()
            currentUser.get()
            awaitSocketThenJoin()
        }

        // Тізімнен келген announcementId → префетч.
        (initialAnnouncementId ?: initialRoomAnnouncementId)?.let { prefetchAnnouncement(it) }
    }

    override fun onCleared() {
        subscriptions.forEach { it.cancel() }
        subscriptions.clear()
        joinTimer?.cancel()
        service.setOpenRoom(null)
        stopTyping()
        super.onCleared()
    }

    private fun syncPeerRoom(rooms: List<com.agroland.feature.chat.data.ChatRoom>) {
        val roomId = _state.value.roomId ?: return
        val found = rooms.firstOrNull { it.roomId == roomId } ?: return
        _peerRoom.value = found
    }

    // ---- Join state machine ----

    /** Socket пайда болғанша polling, сосын handler'лер + бірінші emit. */
    private suspend fun awaitSocketThenJoin() {
        var attempts = 0
        while (manager.socket == null && attempts < 40) {
            delay(250)
            attempts++
        }
        if (manager.socket == null || (initialOtherUserId == null && initialRoomId == null)) {
            // Екі join кілті де жоқ — graceful timeout (Flutter паритеті).
            _state.value = _state.value.copy(loadTimedOut = true, joinResponded = true)
            joinRespondedInternal = true
            return
        }
        ensureHandlers()
        // Ашық бөлмені ертерек белгілеу — бейдж бұл бөлмеге өспесін.
        if (initialRoomId != null) service.setOpenRoom(initialRoomId)
        if (manager.connected) {
            emitJoin()
            startJoinRetryTimer()
        } else {
            manager.socket?.connect()
            startJoinRetryTimer()
        }
    }

    private fun scheduleJoinNow() {
        joinAttempts = 0
        emitJoin()
        startJoinRetryTimer()
    }

    private fun ensureHandlers() {
        val socket = manager.socket ?: return
        if (subscribedSocket === socket && subscriptions.isNotEmpty()) return
        subscriptions.forEach { it.cancel() }
        subscriptions.clear()
        subscribedSocket = socket

        manager.on("join_chat_success") { data -> onJoinSuccess(data) }?.let(subscriptions::add)
        manager.on("join_chat_error") { data -> onJoinError(data) }?.let(subscriptions::add)
        manager.on("chat_message") { data -> onRoomChatMessage(data) }?.let(subscriptions::add)
        manager.on("chat_message_error") { data -> onChatMessageError(data) }?.let(subscriptions::add)
        manager.on("message_deleted") { data -> onMessageDeleted(data) }?.let(subscriptions::add)
        manager.on("chat_history_cleared") { data -> onHistoryCleared(data) }?.let(subscriptions::add)
        manager.on("chat_deleted") { data ->
            _events.tryEmit(ChatRoomEvent.PopRoom)
        }?.let(subscriptions::add)
        manager.on("message_edited") { data -> onMessageEdited(data) }?.let(subscriptions::add)
        manager.on("message_read") { data -> onMessageRead(data) }?.let(subscriptions::add)
        manager.on("older_messages") { data -> onOlderMessages(data) }?.let(subscriptions::add)
        manager.on("typing_start") { data -> onPeerTyping(data, typing = true) }?.let(subscriptions::add)
        manager.on("typing_stop") { data -> onPeerTyping(data, typing = false) }?.let(subscriptions::add)
        manager.on("user_typing") { data -> onPeerTyping(data, typing = true) }?.let(subscriptions::add)
        manager.on("user_stop_typing") { data -> onPeerTyping(data, typing = false) }?.let(subscriptions::add)
    }

    /** join_chat payload (спек §2): екі бранш — room_id НЕ other_user_id+announcement. */
    private fun emitJoin() {
        val socket = manager.socket ?: return
        if (!manager.connected) socket.connect()
        joinAttempts++
        val params = buildJsonObject {
            put("language", language)
            initialRoomId?.let {
                put("room_id", it)
                put("chat_id", it)
            }
            if ((initialIsSystemChat || initialRoomId == null) && initialOtherUserId != null) {
                put("other_user_id", initialOtherUserId)
            }
            if (initialRoomId == null && initialAnnouncementId != null) {
                put("announcement_id", initialAnnouncementId)
                put("announcement_link", "https://agroland.kz/announcement/$initialAnnouncementId")
            }
        }
        manager.emit("join_chat", params)
    }

    /** 4с → 8с → 16с (бастапқы emit + 3 retry), кейін graceful timeout. */
    private fun startJoinRetryTimer() {
        joinTimer?.cancel()
        val delayMs = when {
            joinAttempts < JOIN_RETRY_DELAYS.size -> JOIN_RETRY_DELAYS[joinAttempts]
            else -> JOIN_RETRY_DELAYS.last()
        }
        joinTimer = viewModelScope.launch {
            delay(delayMs)
            if (joinRespondedInternal) return@launch
            if (joinAttempts < MAX_JOIN_ATTEMPTS) {
                if (!manager.connected) manager.socket?.connect()
                ensureHandlers()
                emitJoin()
                startJoinRetryTimer()
            } else {
                if (_state.value.messages.isEmpty()) {
                    _state.value = _state.value.copy(loadTimedOut = true)
                }
            }
        }
    }

    private fun onJoinSuccess(data: kotlinx.serialization.json.JsonElement?) {
        val root = data as? JsonObject ?: return
        joinTimer?.cancel()
        joinRespondedInternal = true

        val rawMessages = root["messages"] as? kotlinx.serialization.json.JsonArray
            ?: (root["data"] as? JsonObject)?.get("messages") as? kotlinx.serialization.json.JsonArray
        // Кезектегі optimistic хабарламаларды сақтап алып, тарихпен алмастырамыз.
        val queuedBeforeReplace = _state.value.messages.filter {
            it.localId != null && queuedLocalIds.contains(it.localId)
        }
        val history = ChatParser.parseMessageList(rawMessages)
        _state.value = _state.value.copy(
            messages = history + queuedBeforeReplace,
            hasMore = true,
            loadingOlder = false,
            loadTimedOut = false,
            joinResponded = true,
            scrollToBottom = _state.value.scrollToBottom + 1,
        )

        roomName = com.agroland.core.network.json.JsonParser.string(root, "room_name")
        // room_name 'chat_room_2923' форматында болуы мүмкін — соңғы санды аламыз.
        var resolvedRoomId = roomName
            ?.let { Regex("(\\d+)$").find(it)?.groupValues?.get(1)?.toLongOrNull() }
        if (resolvedRoomId == null) resolvedRoomId = initialRoomId

        // Chat деңгейіндегі announcement_id (backend жіберсе үстінен жазады).
        com.agroland.core.network.json.JsonParser.long(root, "announcement_id")?.let { annId ->
            if (annId > 0 && _state.value.initialAnnouncementId == null) {
                _state.value = _state.value.copy(initialAnnouncementId = annId)
            }
            prefetchAnnouncement(annId)
        }

        _state.value = _state.value.copy(roomId = resolvedRoomId)
        if (resolvedRoomId != null) {
            service.setOpenRoom(resolvedRoomId)
        }

        // ДЕДУП: flap кезінде 5× join_chat_success — әрі қарай rebuild жоқ.
        if (resolvedRoomId != null && renderedRoomId == resolvedRoomId && _state.value.messages.isNotEmpty()) {
            if (resolvedRoomId != null) {
                manager.emit("mark_chat_as_read", buildJsonObject { put("chat_id", resolvedRoomId) })
            }
            flushQueuedMessages()
            return
        }
        renderedRoomId = resolvedRoomId

        prefetchMessageAnnouncements()

        // «Сатушыдан нақтылаңыз» — delivery_request автоматты жіберу.
        maybeSendDeliveryRequest()
        flushQueuedMessages()
    }

    private fun onJoinError(data: kotlinx.serialization.json.JsonElement?) {
        joinTimer?.cancel()
        joinRespondedInternal = true
        if (_state.value.messages.isEmpty()) {
            _state.value = _state.value.copy(loadTimedOut = true)
        }
    }

    fun retryLoadHistory() {
        joinAttempts = 0
        joinRespondedInternal = false
        _state.value = _state.value.copy(loadTimedOut = false, joinResponded = false)
        viewModelScope.launch { awaitSocketThenJoin() }
    }

    // ---- Room chat_message (echo/broadcast) ----

    private fun onRoomChatMessage(data: kotlinx.serialization.json.JsonElement?) {
        val payload = data as? JsonObject ?: return
        val message = ChatParser.parseMessage(payload) ?: return

        // Cross-room қорғаны: басқа бөлменің хабарламасы лекпесін.
        val rawRoom = com.agroland.core.network.json.JsonParser.string(payload, "room_id")
        val msgRoomId = rawRoom?.let { service.parseRoomId(it) }
        if (msgRoomId != null && _state.value.roomId != null && msgRoomId != _state.value.roomId) return

        val me = _state.value.me ?: return

        val messages = _state.value.messages.toMutableList()
        if (message.id != null) {
            val byId = messages.indexOfFirst { it.id == message.id }
            when {
                byId >= 0 -> messages[byId] = message
                else -> {
                    val optIndex = findOptimisticMatch(message)
                    if (optIndex >= 0) {
                        messages[optIndex] = mergeWithOptimistic(message, messages[optIndex])
                    } else {
                        messages.add(message)
                    }
                }
            }
        } else {
            val isDuplicate = messages.any {
                it.senderId == message.senderId &&
                    it.message == message.message &&
                    kotlin.math.abs(it.timestamp - message.timestamp) <= 1000
            }
            if (!isDuplicate) {
                val optIndex = findOptimisticMatch(message)
                if (optIndex >= 0) {
                    messages[optIndex] = mergeWithOptimistic(message, messages[optIndex])
                } else {
                    messages.add(message)
                }
            }
        }
        _state.value = _state.value.copy(
            messages = messages,
            peerTyping = false,
            scrollToBottom = _state.value.scrollToBottom + 1,
        )
        prefetchMessageAnnouncements()
    }

    private fun onChatMessageError(data: kotlinx.serialization.json.JsonElement?) {
        if (data == null) return
        val messages = _state.value.messages
        val lastOptimistic = messages.lastOrNull { it.isOptimistic } ?: return
        val index = messages.indexOf(lastOptimistic)
        if (index >= 0) {
            val updated = messages.toMutableList()
            updated[index] = lastOptimistic.copy(sendError = true)
            _state.value = _state.value.copy(messages = updated)
        }
    }

    private fun onMessageDeleted(data: kotlinx.serialization.json.JsonElement?) {
        val root = data as? JsonObject ?: return
        val messageId = com.agroland.core.network.json.JsonParser.long(root, "message_id") ?: return
        val rawRoom = com.agroland.core.network.json.JsonParser.long(root, "room_id")
        if (rawRoom != null && rawRoom != _state.value.roomId) return
        _state.value = _state.value.copy(
            messages = _state.value.messages.filterNot { it.id == messageId },
        )
    }

    private fun onHistoryCleared(data: kotlinx.serialization.json.JsonElement?) {
        val root = data as? JsonObject ?: return
        val chatId = com.agroland.core.network.json.JsonParser.long(root, "chat_id") ?: return
        if (chatId == _state.value.roomId) {
            _state.value = _state.value.copy(messages = emptyList())
        }
    }

    private fun onMessageEdited(data: kotlinx.serialization.json.JsonElement?) {
        val root = data as? JsonObject ?: return
        val messageId = com.agroland.core.network.json.JsonParser.long(root, "message_id") ?: return
        val newText = com.agroland.core.network.json.JsonParser.string(root, "message") ?: return
        val rawRoom = com.agroland.core.network.json.JsonParser.long(root, "room_id")
        if (rawRoom != null && rawRoom != _state.value.roomId) return
        val messages = _state.value.messages
        val index = messages.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            val updated = messages.toMutableList()
            updated[index] = updated[index].copyForEdit(newText, System.currentTimeMillis())
            _state.value = _state.value.copy(messages = updated)
        }
    }

    private fun onMessageRead(data: kotlinx.serialization.json.JsonElement?) {
        val root = data as? JsonObject ?: return
        val messageId = com.agroland.core.network.json.JsonParser.long(root, "message_id") ?: return
        val rawRoom = com.agroland.core.network.json.JsonParser.long(root, "room_id")
        if (rawRoom != null && rawRoom != _state.value.roomId) return
        val messages = _state.value.messages
        val index = messages.indexOfFirst { it.id == messageId }
        if (index >= 0) {
            val updated = messages.toMutableList()
            updated[index] = updated[index].copy(isRead = true)
            _state.value = _state.value.copy(messages = updated)
        }
    }

    private fun onOlderMessages(data: kotlinx.serialization.json.JsonElement?) {
        val root = data as? JsonObject ?: return
        _state.value = _state.value.copy(loadingOlder = false)
        val rawRoom = com.agroland.core.network.json.JsonParser.long(root, "room_id")
        if (rawRoom != null && rawRoom != _state.value.roomId) return

        val hasMoreRaw = root["has_more"]
        val hasMore = when (hasMoreRaw) {
            is kotlinx.serialization.json.JsonPrimitive ->
                hasMoreRaw.content.lowercase() == "true" || hasMoreRaw.content == "1"
            else -> true
        }
        val older = ChatParser.parseMessageList(root["messages"])
        if (older.isEmpty()) {
            _state.value = _state.value.copy(hasMore = false)
            return
        }
        val existingIds = _state.value.messages.mapNotNull { it.id }.toSet()
        val fresh = older.filter { it.id == null || it.id !in existingIds }
        if (fresh.isEmpty()) return
        _state.value = _state.value.copy(
            messages = fresh + _state.value.messages,
            hasMore = hasMore,
            prependCount = fresh.size,
        )
        prefetchMessageAnnouncements()
    }

    private fun onPeerTyping(data: kotlinx.serialization.json.JsonElement?, typing: Boolean) {
        val root = data as? JsonObject
        val me = _state.value.me
        // Өз typing-імізді ignore етеміз (sanity).
        if (root != null && me != null) {
            val userId = com.agroland.core.network.json.JsonParser.long(root, "user_id")
            if (userId != null && userId == me.id) return
        }
        _state.value = _state.value.copy(peerTyping = typing)
    }

    /** Тарихтың үстіңгі бөлігін жүктеу (Flutter _loadOlderMessages паритеті). */
    fun loadOlderMessages() {
        val roomId = _state.value.roomId ?: return
        if (!_state.value.hasMore || _state.value.loadingOlder) return
        val oldest = _state.value.messages.firstOrNull()?.id ?: return
        _state.value = _state.value.copy(loadingOlder = true)
        manager.emit("load_older_messages", buildJsonObject {
            put("room_id", roomId)
            put("before_message_id", oldest)
            put("limit", 50)
        })
    }

    // ---- Хабарлама жіберу ----

    fun setInput(value: String) {
        _state.value = _state.value.copy(input = value)
        // typing_start/stop — 2с timer.
        if (value.isNotEmpty()) startTyping() else stopTyping()
    }

    fun setDeliveryPrice(value: String) {
        _state.value = _state.value.copy(deliveryPrice = value)
    }

    private var typingJob: Job? = null
    private var typingActive = false

    private fun startTyping() {
        val roomId = _state.value.roomId ?: return
        if (!typingActive) {
            typingActive = true
            manager.emit("typing_start", buildJsonObject { put("room_id", roomId) })
        }
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(2000)
            stopTyping()
        }
    }

    private fun stopTyping() {
        typingJob?.cancel()
        if (typingActive) {
            typingActive = false
            val roomId = _state.value.roomId
            if (roomId != null) {
                manager.emit("typing_stop", buildJsonObject { put("room_id", roomId) })
            }
        }
    }

    /** Мәтіндік хабарлама жіберу (Flutter _sendMessage паритеті). */
    fun sendMessage() {
        val me = _state.value.me ?: return
        var msg = _state.value.input.trim()
        if (msg.isEmpty()) return
        val receiverId = receiverIdFor()
        val receiverName = receiverNameFor()

        // «Жеткізе аламын» + Сумма өрісі (Flutter: '$msg. ${deliverySumLabel}: $price').
        if (_state.value.dealerDeliveryReplyYes) {
            val price = _state.value.deliveryPrice.trim()
            if (price.isNotEmpty()) {
                msg = if (deliverySumLabel.isBlank()) {
                    "$msg. $price"
                } else {
                    "$msg. $deliverySumLabel: $price"
                }
            }
            _state.value = _state.value.copy(dealerDeliveryReplyYes = false, deliveryPrice = "")
        }

        val connected = manager.connected && roomName != null

        // Оффлайн: өңдеу кезекте тұра алмайды.
        val editing = _state.value.editing
        if (editing?.id != null && !connected) {
            _events.tryEmit(ChatRoomEvent.Toast.CONNECTION_LOST)
            return
        }
        if (!connected) {
            _events.tryEmit(ChatRoomEvent.Toast.QUEUED)
            enqueueTextMessage(msg, me.id, receiverId, me.name, receiverName)
            return
        }

        // Өңдеу режимі.
        if (editing != null && editing.id != null) {
            manager.emit("edit_message", buildJsonObject {
                put("message_id", editing.id!!)
                put("message", msg)
                put("language", language)
            })
            val messages = _state.value.messages
            val index = messages.indexOfFirst { it.id == editing.id }
            if (index >= 0) {
                val updated = messages.toMutableList()
                updated[index] = updated[index].copyForEdit(msg, System.currentTimeMillis())
                _state.value = _state.value.copy(messages = updated)
            }
            clearEditing()
            return
        }

        // «Сатушыдан нақтылаңыз» — бірінші хабарламаға тег.
        val isClarifyFirst = initialAnnouncementId != null && !announcementCardSent
        if (isClarifyFirst) {
            msg = "$msg [announcement_id:$initialAnnouncementId]"
        }

        val replyTo = _state.value.replyTo
        val localId = "temp_${localIdCounter++}"
        val optimistic = ChatMessage(
            id = null,
            senderId = me.id,
            receiverId = receiverId,
            senderName = me.name,
            receiverName = receiverName,
            message = msg,
            messageType = "text",
            isRead = false,
            timestamp = System.currentTimeMillis(),
            senderAvatarUrl = null,
            receiverAvatarUrl = null,
            fileUrl = null,
            fileName = null,
            audioUrl = null,
            audioDuration = null,
            latitude = null,
            longitude = null,
            locationName = null,
            announcement = null,
            replyToId = replyTo?.id,
            replyToMessage = replyTo?.message,
            editedAt = null,
            isDeleted = false,
            localId = localId,
        )
        _state.value = _state.value.copy(
            messages = _state.value.messages + optimistic,
            input = "",
            replyTo = null,
            scrollToBottom = _state.value.scrollToBottom + 1,
        )
        if (isClarifyFirst) announcementCardSent = true

        manager.emit("chat_message", buildJsonObject {
            put("room_name", roomName!!)
            put("message", msg)
            put("message_type", "text")
            replyTo?.id?.let { put("reply_to_id", it) }
        })
        stopTyping()
    }

    /** Socket үзілгенде — optimistic кезекке. */
    private fun enqueueTextMessage(
        msg: String,
        senderId: Long,
        receiverId: Long?,
        senderName: String,
        receiverName: String,
    ) {
        val localId = "temp_${localIdCounter++}"
        queuedLocalIds.add(localId)
        var tagged = msg
        val isClarifyFirst = initialAnnouncementId != null && !announcementCardSent
        if (isClarifyFirst) {
            tagged = "$msg [announcement_id:$initialAnnouncementId]"
            announcementCardSent = true
        }
        val replyTo = _state.value.replyTo
        val queued = ChatMessage(
            id = null,
            senderId = senderId,
            receiverId = receiverId ?: 0L,
            senderName = senderName,
            receiverName = receiverName,
            message = tagged,
            messageType = "text",
            isRead = false,
            timestamp = System.currentTimeMillis(),
            senderAvatarUrl = null,
            receiverAvatarUrl = null,
            fileUrl = null,
            fileName = null,
            audioUrl = null,
            audioDuration = null,
            latitude = null,
            longitude = null,
            locationName = null,
            announcement = null,
            replyToId = replyTo?.id,
            replyToMessage = replyTo?.message,
            editedAt = null,
            isDeleted = false,
            localId = localId,
        )
        _state.value = _state.value.copy(
            messages = _state.value.messages + queued,
            input = "",
            replyTo = null,
            scrollToBottom = _state.value.scrollToBottom + 1,
        )
    }

    /** Байланыс қалпына келгенде кезекті жіберу. */
    private fun flushQueuedMessages() {
        if (queuedLocalIds.isEmpty()) return
        if (!manager.connected || roomName == null) return
        val toFlush = queuedLocalIds.toList()
        val messages = _state.value.messages
        for (localId in toFlush) {
            val m = messages.firstOrNull { it.localId == localId } ?: continue
            manager.emit("chat_message", buildJsonObject {
                put("room_name", roomName!!)
                put("message", m.message)
                put("message_type", "text")
                m.replyToId?.let { put("reply_to_id", it) }
            })
            queuedLocalIds.remove(localId)
        }
    }

    // ---- Медиа жіберу ----

    /** Сурет/бейне (Галерея + Файлpicker): upload → chat_message. */
    fun sendMedia(uri: android.net.Uri, kind: UploadKind, displayName: String?) {
        if (!canSendMedia()) return
        val inferred = InferMessageType.inferFromUrl(displayName ?: "")
        val type = when (kind) {
            // Галерея: сурет пен бейне екеуі де болуы мүмкін (Flutter extension-check).
            UploadKind.IMAGE -> when (inferred) {
                InferredMessageType.VIDEO -> "video"
                else -> "image"
            }
            UploadKind.VIDEO -> "video"
            // Файл picker: кез келген формат — типті URL/extension бойынша шығарамыз.
            else -> when (inferred) {
                InferredMessageType.IMAGE -> "image"
                InferredMessageType.VIDEO -> "video"
                else -> "file"
            }
        }
        uploadAndSend(uri, kind, type, displayName)
    }

    /** Дауыстық хабарлама: жазба файлы + ұзақтығы. */
    fun sendAudioRecording(path: String, durationSec: Int) {
        if (durationSec < 1) {
            _events.tryEmit(ChatRoomEvent.Toast.VOICE_TOO_SHORT)
            return
        }
        if (!canSendMedia()) return
        val uri = android.net.Uri.fromFile(java.io.File(path))
        viewModelScope.launch {
            _state.value = _state.value.copy(upload = UploadState(0, UploadKind.AUDIO))
            when (val result = repository.uploadAudio(uri) { progress ->
                _state.value = _state.value.copy(upload = UploadState(progress, UploadKind.AUDIO))
            }) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(upload = null)
                    emitMediaMessage(
                        message = result.value,
                        messageType = "audio",
                        fileUrl = result.value,
                        fileName = null,
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(upload = null)
                    _events.tryEmit(ChatRoomEvent.Toast.SEND_ERROR)
                }
            }
        }
    }

    private fun uploadAndSend(
        uri: android.net.Uri,
        kind: UploadKind,
        messageType: String,
        displayName: String?,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(upload = UploadState(0, kind))
            when (val result = repository.uploadFile(uri) { progress ->
                _state.value = _state.value.copy(upload = UploadState(progress, kind))
            }) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(upload = null)
                    val fileName = displayName?.substringAfterLast('/')
                        ?.substringAfter('\\')
                    emitMediaMessage(
                        message = result.value,
                        messageType = messageType,
                        fileUrl = result.value,
                        fileName = fileName,
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(upload = null)
                    val tooLarge = result.failure is Failure.Unknown &&
                        (result.failure as Failure.Unknown).cause is FileTooLargeException
                    _events.tryEmit(
                        if (tooLarge) {
                            ChatRoomEvent.Toast.FILE_TOO_LARGE
                        } else {
                            ChatRoomEvent.Toast.SEND_ERROR
                        },
                    )
                }
            }
        }
    }

    /** Медиа upload сәтті — chat_message emit (Flutter payload паритеті). */
    private fun emitMediaMessage(message: String, messageType: String, fileUrl: String, fileName: String?) {
        if (roomName == null || !manager.connected) {
            _events.tryEmit(ChatRoomEvent.Toast.CONNECTION_LOST)
            return
        }
        val me = _state.value.me ?: return
        val optimistic = ChatMessage(
            id = null,
            senderId = me.id,
            receiverId = receiverIdFor(),
            senderName = me.name,
            receiverName = receiverNameFor(),
            message = message,
            messageType = messageType,
            isRead = false,
            timestamp = System.currentTimeMillis(),
            senderAvatarUrl = null,
            receiverAvatarUrl = null,
            fileUrl = fileUrl,
            fileName = fileName,
            audioUrl = if (messageType == "audio") fileUrl else null,
            audioDuration = null,
            latitude = null,
            longitude = null,
            locationName = null,
            announcement = null,
            replyToId = null,
            replyToMessage = null,
            editedAt = null,
            isDeleted = false,
            localId = "temp_${localIdCounter++}",
        )
        _state.value = _state.value.copy(
            messages = _state.value.messages + optimistic,
            scrollToBottom = _state.value.scrollToBottom + 1,
        )
        manager.emit("chat_message", buildJsonObject {
            put("room_name", roomName!!)
            put("message", message)
            put("message_type", messageType)
            put("file_url", fileUrl)
            fileName?.let { put("file_name", it) }
        })
    }

    /** Локация жіберу: геокод → chat_message + optimistic location. */
    fun sendLocation(latitude: Double, longitude: Double) {
        if (!canSendMedia()) return
        viewModelScope.launch {
            val locationName = geocoder.reverseGeocode(latitude, longitude, language)
            if (roomName == null || !manager.connected) {
                _events.tryEmit(ChatRoomEvent.Toast.CONNECTION_LOST)
                return@launch
            }
            val me = _state.value.me ?: return@launch
            val message = locationName ?: "$latitude, $longitude"
            val optimistic = ChatMessage(
                id = null,
                senderId = me.id,
                receiverId = receiverIdFor(),
                senderName = me.name,
                receiverName = receiverNameFor(),
                message = message,
                messageType = "location",
                isRead = false,
                timestamp = System.currentTimeMillis(),
                senderAvatarUrl = null,
                receiverAvatarUrl = null,
                fileUrl = null,
                fileName = null,
                audioUrl = null,
                audioDuration = null,
                latitude = latitude,
                longitude = longitude,
                locationName = locationName,
                announcement = null,
                replyToId = null,
                replyToMessage = null,
                editedAt = null,
                isDeleted = false,
                localId = "temp_${localIdCounter++}",
            )
            _state.value = _state.value.copy(
                messages = _state.value.messages + optimistic,
                scrollToBottom = _state.value.scrollToBottom + 1,
            )
            manager.emit("chat_message", buildJsonObject {
                put("room_name", roomName!!)
                put("message", message)
                put("message_type", "location")
                put("language", language)
                put("latitude", latitude)
                put("longitude", longitude)
                locationName?.let { put("location_name", it) }
            })
        }
    }

    private fun canSendMedia(): Boolean {
        if (_state.value.readOnly) return false
        if (roomName == null || !manager.connected) {
            _events.tryEmit(ChatRoomEvent.Toast.CONNECTION_LOST)
            return false
        }
        return true
    }

    // ---- Хабарлама әрекеттері ----

    fun setReplyTo(message: ChatMessage) {
        _state.value = _state.value.copy(replyTo = message, editing = null)
    }

    fun clearReply() {
        _state.value = _state.value.copy(replyTo = null)
    }

    fun startEditing(message: ChatMessage) {
        // Тегтерді өңдеуге жібермейміз (таза мәтін).
        val clean = message.message.replace(Regex("\\s*\\[[^]]+]"), "").trim()
        _state.value = _state.value.copy(editing = message, replyTo = null, input = clean)
    }

    fun clearEditing() {
        _state.value = _state.value.copy(editing = null, input = "")
    }

    /** Хабарды жою: forMe — локалды; forEveryone — REST DELETE + optimistic. */
    fun deleteMessage(message: ChatMessage, forEveryone: Boolean) {
        val messages = _state.value.messages
        if (!forEveryone) {
            _state.value = _state.value.copy(
                messages = messages.filterNot { it.id == message.id || it.localId == message.localId },
            )
            return
        }
        _state.value = _state.value.copy(
            messages = messages.filterNot { it.id == message.id || it.localId == message.localId },
        )
        message.id?.let { id ->
            viewModelScope.launch {
                repository.deleteMessage(id)
            }
        }
    }

    /** Қатеге ұшыраған optimistic хабарламаны қайта жіберу. */
    fun resendMessage(message: ChatMessage) {
        if (message.localId == null || !manager.connected || roomName == null) return
        val messages = _state.value.messages.toMutableList()
        val index = messages.indexOfFirst { it.localId == message.localId }
        if (index < 0) return
        val newLocalId = "temp_${localIdCounter++}"
        messages[index] = message.copy(sendError = false, localId = newLocalId)
        _state.value = _state.value.copy(messages = messages)
        manager.emit("chat_message", buildJsonObject {
            put("room_name", roomName!!)
            put("message", message.message)
            put("message_type", message.messageType)
            message.replyToId?.let { put("reply_to_id", it) }
        })
    }

    /** Чат тарихын тазалау (меню). */
    fun clearHistory() {
        val roomId = _state.value.roomId ?: return
        viewModelScope.launch {
            when (repository.clearChatHistory(roomId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(messages = emptyList())
                    _events.tryEmit(ChatRoomEvent.Toast.CHAT_CLEARED)
                }
                is ApiResult.Error -> _events.tryEmit(ChatRoomEvent.Toast.SOMETHING_WRONG)
            }
        }
    }

    /** Бөлмеден мұрағаттау (меню). */
    fun archiveChat() {
        val roomId = _state.value.roomId ?: return
        viewModelScope.launch {
            repository.updateChatStatus(roomId, "ARCHIVED")
            _events.tryEmit(ChatRoomEvent.PopRoom)
        }
    }

    // ---- Delivery request ағыны ----

    /** Сатып алушы: бөлме ашылғанда автоматты delivery_request. */
    private fun maybeSendDeliveryRequest() {
        if (initialDeliveryAddress.isBlank() || deliveryRequestSent) return
        if (initialAnnouncementId == null) return
        val me = _state.value.me ?: return // profile күтеміз (≤10 × 400мс)
        if (roomName == null || !manager.connected) return
        deliveryRequestSent = true
        val text = "[announcement_id:$initialAnnouncementId] [address:$initialDeliveryAddress]".trim()
        val optimistic = ChatMessage(
            id = null,
            senderId = me.id,
            receiverId = receiverIdFor(),
            senderName = me.name,
            receiverName = receiverNameFor(),
            message = text,
            messageType = "delivery_request",
            isRead = false,
            timestamp = System.currentTimeMillis(),
            senderAvatarUrl = null,
            receiverAvatarUrl = null,
            fileUrl = null,
            fileName = null,
            audioUrl = null,
            audioDuration = null,
            latitude = null,
            longitude = null,
            locationName = null,
            announcement = null,
            replyToId = null,
            replyToMessage = null,
            editedAt = null,
            isDeleted = false,
            localId = "temp_${localIdCounter++}",
        )
        _state.value = _state.value.copy(
            messages = _state.value.messages + optimistic,
            deliveryRequestSent = true,
            scrollToBottom = _state.value.scrollToBottom + 1,
        )
        manager.emit("chat_message", buildJsonObject {
            put("room_name", roomName!!)
            put("message", text)
            put("message_type", "text")
        })
    }

    /** Беттен чип мәтіндерін береміз (l10n паритеті — Flutter deliveryQuickReplyYes/No). */
    fun setDeliveryReplyStrings(yes: String, no: String, sumLabel: String) {
        deliveryYesText = yes
        deliveryNoText = no
        deliverySumLabel = sumLabel
    }

    /** Сатушы чип-жауабы: иә → input префилл + сумма өрісі; жоқ → бірден жіберу. */
    fun onDeliveryQuickReply(message: ChatMessage, yes: Boolean) {
        _state.value = _state.value.copy(
            answeredRequestIds = _state.value.answeredRequestIds + (message.id ?: -1L),
        )
        if (yes) {
            // Flutter: input = deliveryQuickReplyYes — пайдаланушы «Жіберу» басады.
            _state.value = _state.value.copy(
                dealerDeliveryReplyYes = true,
                input = deliveryYesText,
            )
        } else {
            sendMessageQuickText(deliveryNoText)
        }
    }

    /** Дайын мәтінді бірден жіберу (чип-жауаптар). */
    private fun sendMessageQuickText(text: String) {
        val me = _state.value.me ?: return
        if (roomName == null || !manager.connected) {
            _events.tryEmit(ChatRoomEvent.Toast.CONNECTION_LOST)
            return
        }
        val localId = "temp_${localIdCounter++}"
        val optimistic = ChatMessage(
            id = null,
            senderId = me.id,
            receiverId = receiverIdFor(),
            senderName = me.name,
            receiverName = receiverNameFor(),
            message = text,
            messageType = "text",
            isRead = false,
            timestamp = System.currentTimeMillis(),
            senderAvatarUrl = null,
            receiverAvatarUrl = null,
            fileUrl = null,
            fileName = null,
            audioUrl = null,
            audioDuration = null,
            latitude = null,
            longitude = null,
            locationName = null,
            announcement = null,
            replyToId = null,
            replyToMessage = null,
            editedAt = null,
            isDeleted = false,
            localId = localId,
        )
        _state.value = _state.value.copy(
            messages = _state.value.messages + optimistic,
            scrollToBottom = _state.value.scrollToBottom + 1,
        )
        manager.emit("chat_message", buildJsonObject {
            put("room_name", roomName!!)
            put("message", text)
            put("message_type", "text")
        })
    }

    /** Сатушының delivery_request-іне жауап берілген бе? */
    fun isAnsweredDeliveryRequest(message: ChatMessage): Boolean {
        if (_state.value.answeredRequestIds.contains(message.id)) return true
        val messages = _state.value.messages
        val index = messages.indexOfFirst { it.id == message.id }
        if (index < 0) return false
        val me = _state.value.me ?: return false
        // Кейіннен сатушыдан (озат мәтін байқау) хабарлама келсе — жауап берілген.
        return messages.drop(index + 1).any {
            it.senderId == me.id
        }
    }

    /** Сатып алушы «Төлеу/Өзі алу» таңдауы → жарнама беті (Flutter паритеті). */
    fun onBuyerChoiceForRequest(request: ChatMessage, choice: String) {
        request.id?.let { id ->
            _state.value = _state.value.copy(buyerChosenIds = _state.value.buyerChosenIds + id)
        }
        val announcementId = Regex("\\[announcement_id:(\\d+)]")
            .find(request.message)?.groupValues?.get(1)?.toLongOrNull()
            ?: request.announcement?.id
            ?: _state.value.initialAnnouncementId
        if (announcementId != null) {
            _events.tryEmit(ChatRoomEvent.OpenAnnouncement(announcementId))
        } else {
            _events.tryEmit(ChatRoomEvent.Toast.SOMETHING_WRONG)
        }
    }

    /** Сатып алушы чиптері көрінетін бе? */
    fun shouldShowDeliveryAnswer(message: ChatMessage): Boolean {
        if (!isDeliveryRequestMessage(message)) return false
        // Сатып алушы жағында — сатушы «иә» деп жауап берген соң карточка.
        val me = _state.value.me ?: return false
        val messages = _state.value.messages
        val index = messages.indexOfFirst { it.id == message.id }
        if (index < 0) return false
        val answered = messages.drop(index + 1).any {
            it.senderId != me.id && isDeliveryDealerAnswer(it.message)
        }
        return answered
    }

    // ---- Жарнама префетчі (Option A/B) ----

    /** Тарихтағы [announcement_id:N] тегтері бойынша карточка префетчі. */
    private fun prefetchMessageAnnouncements() {
        val known = _state.value.announcements.keys
        for (message in _state.value.messages) {
            message.announcement?.id?.let { id ->
                if (id !in known) prefetchAnnouncement(id)
            }
            Regex("\\[announcement_id:(\\d+)]").find(message.message)?.groupValues?.get(1)
                ?.toLongOrNull()
                ?.let { id ->
                    if (id !in known) prefetchAnnouncement(id)
                }
        }
    }

    private fun prefetchAnnouncement(announcementId: Long) {
        if (announcementId <= 0) return
        if (_state.value.announcements.containsKey(announcementId)) return
        viewModelScope.launch {
            when (val result = marketplace.getAnnouncement(announcementId)) {
                is ApiResult.Success -> {
                    val base = result.value.base
                    // Race: бірнеше launch бірдей id — putIfAbsent.
                    if (!_state.value.announcements.containsKey(announcementId)) {
                        _state.value = _state.value.copy(
                            announcements = _state.value.announcements +
                                (
                                announcementId to AnnouncementCard(
                                    id = base.id,
                                    title = base.title,
                                    price = base.price,
                                    mainImageUrl = base.imageUrl,
                                )
                                ),
                        )
                    }
                }
                is ApiResult.Error -> Unit
            }
        }
    }

    /** Хабарлама тарихындағы announcement карточкасы (Option A payload → Option B). */
    fun announcementFor(message: ChatMessage): AnnouncementCard? {
        message.announcement?.let { ann ->
            return AnnouncementCard(ann.id, ann.title, ann.price, ann.mainImageUrl)
        }
        val id = Regex("\\[announcement_id:(\\d+)]").find(message.message)?.groupValues?.get(1)
            ?.toLongOrNull() ?: return null
        return _state.value.announcements[id] ?: _state.value.initialAnnouncementId
            ?.takeIf { it == id }
            ?.let { annId -> _state.value.announcements[annId] }
    }

    /** Delivery request хабарламасы ма? */
    fun isDeliveryRequestMessage(message: ChatMessage): Boolean {
        return message.messageType == "delivery_request" || Regex("\\[address:.*?]").containsMatchIn(message.message)
    }

    /** Сатушының delivery-жауап мәтіні ме? (чиптерден жіберілген иә/жоқ мәтіндері). */
    private fun isDeliveryDealerAnswer(text: String): Boolean {
        val trimmed = text.trim()
        if (deliveryYesText.isNotBlank() && trimmed.startsWith(deliveryYesText)) return true
        if (deliveryNoText.isNotBlank() && trimmed == deliveryNoText) return true
        return false
    }

    // ---- Optimistic сәйкестендіру ----

    private fun findOptimisticMatch(message: ChatMessage): Int {
        val messages = _state.value.messages
        val byText = messages.indexOfFirst {
            it.isOptimistic && it.senderId == message.senderId && it.message == message.message
        }
        if (byText >= 0) return byText
        val locationEcho = message.messageType == "location" ||
            message.latitude != null ||
            (
            message.messageType == "text" &&
                Regex("^-?\\d+\\.\\d+,\\s*-?\\d+\\.\\d+$").matches(message.message.trim())
            )
        if (locationEcho) {
            return messages.indexOfFirst {
                it.isOptimistic && it.senderId == message.senderId &&
                    it.messageType == "location" &&
                    kotlin.math.abs(it.timestamp - message.timestamp) <= 120_000
            }
        }
        return -1
    }

    private fun mergeWithOptimistic(echo: ChatMessage, optimistic: ChatMessage): ChatMessage {
        if (optimistic.messageType == "location" &&
            optimistic.latitude != null &&
            optimistic.longitude != null &&
            (echo.messageType != "location" || echo.latitude == null)
        ) {
            return echo.copy(
                messageType = "location",
                latitude = optimistic.latitude,
                longitude = optimistic.longitude,
                locationName = echo.locationName ?: optimistic.locationName,
            )
        }
        return echo
    }

    private fun receiverIdFor(): Long = initialOtherUserId ?: 0L

    private fun receiverNameFor(): String = initialUsername

    /** Тек оқу чаттарында input жабық (Flutter readOnly чаттар 31/1003 жазуға болады). */
    fun canWrite(): Boolean = !_state.value.readOnly

    // Markers: сатушы чип-жауаптарын сатып алушы карточкасында тану үшін.
    private companion object {
        val JOIN_RETRY_DELAYS = listOf(4000L, 8000L, 16000L)
        const val MAX_JOIN_ATTEMPTS = 4
        val READ_ONLY_SERVICE_IDS = setOf(1001L, 1002L, 1004L, 1005L)
    }
}