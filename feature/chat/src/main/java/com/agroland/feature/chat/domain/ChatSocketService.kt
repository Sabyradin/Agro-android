package com.agroland.feature.chat.domain

import com.agroland.core.network.ApiResult
import com.agroland.feature.chat.data.ChatGeocoder
import com.agroland.feature.chat.data.ChatRepository
import com.agroland.feature.chat.data.ChatRoom
import com.agroland.feature.chat.data.ChatRoomMerger
import com.agroland.feature.chat.data.ChatParser
import com.agroland.feature.chat.data.ChatSocketManager
import com.agroland.feature.chat.data.ChatSocketManager.State
import com.agroland.feature.chat.data.CurrentUserProvider
import com.agroland.feature.chat.data.MessageSoundPlayer
import com.agroland.core.network.json.JsonParser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put

/**
 * Чат socket оркестраторы (Flutter ChatSocketService — статика → Hilt
 * синглтон): бүкіл қолданба деңгейіндегі чат тізімі күйі.
 *
 *  - socket push (connect_chat_success / send_updated_chat_list /
 *    chat_rooms_updated) → MERGE (REPLACE емес — push кейде толық емес);
 *  - әрбір `chat_message` (бөлме ашық болмаса да) → preview/unread
 *    жаңарту + бөлек бөлмеден келсе дыбыс;
 *  - user_online/user_offline → presence (жасыл нүкте + last seen);
 *  - chat_deleted → екі тізімнен де өшіру;
 *  - үзіліс/қате → REST fallback (GET /chat/?chat_status=...);
 *  - REST — парақ ашылғанда АВТОРИТЕТТІ толық тізім (Flutter паритеті).
 */
@Singleton
class ChatSocketService @Inject constructor(
    val manager: ChatSocketManager,
    private val repository: ChatRepository,
    private val currentUser: CurrentUserProvider,
    private val soundPlayer: MessageSoundPlayer,
    private val geocoder: ChatGeocoder,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _activeRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val activeRooms: StateFlow<List<ChatRoom>> = _activeRooms.asStateFlow()

    private val _archivedRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val archivedRooms: StateFlow<List<ChatRoom>> = _archivedRooms.asStateFlow()

    private val _connectionState = MutableStateFlow(State.DISCONNECTED)
    val connectionState: StateFlow<State> = _connectionState.asStateFlow()

    /** Ағымда ашық тұрған бөлме — оқылмаған бейдж осы бөлмеге өспейді. */
    private val _currentOpenRoomId = MutableStateFlow<Long?>(null)
    val currentOpenRoomId: StateFlow<Long?> = _currentOpenRoomId.asStateFlow()

    private val _totalUnread = MutableStateFlow(0)
    val totalUnread: StateFlow<Int> = _totalUnread.asStateFlow()

    /** Бейдж (ISSUES.md #24): чат unread + хабарламалар санағының қосындысы. */
    private val _notificationsUnread = MutableStateFlow(0)
    val notificationsUnread: StateFlow<Int> = _notificationsUnread.asStateFlow()

    private var started = false

    /** Сессия Authorized болғанда шақырылады (MainActivity, Flutter initialize). */
    fun start() {
        if (started) {
            // Socket өліп қалған болуы мүмкін (resume) — тіркелгендер қайта
            // қосылады, push list handler'лер socket-та сақталған.
            if (!manager.connected && manager.socket != null) {
                manager.socket?.connect()
            }
            restFallback()
            return
        }
        started = true
        scope.launch {
            manager.refreshLanguageFromSettings()
            manager.connect()
            registerGlobalHandlers()
            restFallback()
        }
        scope.launch {
            manager.state.collect { _connectionState.value = it }
        }
    }

    /** Шығу (Flutter dispose): тыңдаушылар + socket өшіріледі. */
    fun stop() {
        started = false
        _activeRooms.value = emptyList()
        _archivedRooms.value = emptyList()
        _currentOpenRoomId.value = null
        _totalUnread.value = 0
        _notificationsUnread.value = 0
        currentUser.clear()
        geocoder.clearCache()
        manager.disconnect()
    }

    /** App background→foreground (Flutter onAppResumed). */
    fun onAppResumed() {
        if (!started) return
        if (!manager.connected) manager.connect()
        restFallback()
    }

    fun setOpenRoom(roomId: Long?) {
        _currentOpenRoomId.value = roomId
        if (roomId != null) {
            markChatAsRead(roomId)
        }
    }

    fun markChatAsRead(roomId: Long) {
        // Локалды unread қайта орнату + mark_chat_as_read emit.
        _activeRooms.value = _activeRooms.value.map {
            if (it.roomId == roomId && it.unreadCount > 0) it.copy(unreadCount = 0, isRead = true) else it
        }
        _archivedRooms.value = _archivedRooms.value.map {
            if (it.roomId == roomId && it.unreadCount > 0) it.copy(unreadCount = 0, isRead = true) else it
        }
        recomputeTotalUnread()
        scope.launch {
            manager.emit("mark_chat_as_read", kotlinx.serialization.json.buildJsonObject {
                put("chat_id", roomId)
            })
        }
    }

    /** Хабарламалар санағы (бейдждің екінші құрамдасы) — app деңгейінен жаңартылады. */
    fun updateNotificationsUnread(count: Int) {
        _notificationsUnread.value = count
    }

    /** Бейдж (ISSUES.md #24): чат unread + хабарламалар санағының қосындысы. */
    val badgeCount: Int
        get() = _totalUnread.value + _notificationsUnread.value

    /** Чат тізімі парағы ашылды — REST толық тізім (авторитетті, Flutter паритеті). */
    fun refreshFromRest() {
        scope.launch {
            when (val result = repository.getChats("ACTIVE")) {
                is ApiResult.Success -> replaceActive(result.value)
                is ApiResult.Error -> Unit
            }
            when (val result = repository.getChats("ARCHIVED")) {
                is ApiResult.Success -> replaceArchived(result.value)
                is ApiResult.Error -> Unit
            }
        }
    }

    // ---- Глобалды socket handler'лер (әр socket өміріне бір рет) ----

    private fun registerGlobalHandlers() {
        // Тізім push-тары: {chats} немесе {data:{chats}} — үшеуі де бір merge-ке.
        listOf("connect_chat_success", "send_updated_chat_list", "chat_rooms_updated")
            .forEach { event ->
                manager.on(event) { data -> updateChatRooms(data) }
            }

        manager.on("chat_message") { data -> onChatMessage(data) }
        manager.on("user_online") { data -> onUserPresence(data, online = true) }
        manager.on("user_offline") { data -> onUserPresence(data, online = false) }
        manager.on("chat_deleted") { data -> onChatDeleted(data) }

        // Үзіліс/қате → REST fallback (Flutter паритеті).
        manager.on(ChatSocketManagerEvents.DISCONNECT) { restFallback() }
        manager.on(ChatSocketManagerEvents.CONNECT_ERROR) { restFallback() }
    }

    private fun updateChatRooms(data: JsonElement?) {
        val root = data as? JsonObject ?: return
        val rawChats = (root["chats"] as? kotlinx.serialization.json.JsonArray)
            ?: ((root["data"] as? JsonObject)?.get("chats") as? kotlinx.serialization.json.JsonArray)
            ?: return
        val incoming = rawChats.filterIsInstance<JsonObject>()
            .mapNotNull(ChatParser::parseRoom)
        val merged = ChatRoomMerger.merge(
            incoming = incoming,
            active = _activeRooms.value,
            archived = _archivedRooms.value,
        )
        // Мазмұны өзгермесе — артық rebuild жоқ (Flutter listEquals паритеті).
        if (merged.active != _activeRooms.value) _activeRooms.value = merged.active
        if (merged.archived != _archivedRooms.value) _archivedRooms.value = merged.archived
        recomputeTotalUnread()
    }

    private fun onChatMessage(data: JsonElement?) {
        val payload = data as? JsonObject ?: return
        val message = ChatParser.parseMessage(payload) ?: return
        scope.launch {
            val me = currentUser.get() ?: return@launch
            val isOutgoing = message.senderId == me.id
            val peerId = if (isOutgoing) message.receiverId else message.senderId

            // room_id "chat_<id>" түрінде келуі мүмкін — префиксті алып тастаймыз.
            val rawRoomId = JsonParser.string(payload, "chat_id")
                ?: JsonParser.string(payload, "room_id")
            val roomId = parseRoomId(rawRoomId ?: return@launch)

            val openRoomId = _currentOpenRoomId.value
            // Дыбыс — тек БӨЛЕК бөлмеден келген кіріс хабарламаға.
            if (!isOutgoing && roomId != null && openRoomId != roomId) {
                soundPlayer.play()
            }

            val updatedActive = _activeRooms.value.map { room ->
                if (matchesRoom(room, roomId, peerId, me.id)) {
                    room.copy(
                        message = message.message,
                        timestamp = message.timestamp,
                        unreadCount = if (!isOutgoing && openRoomId != room.roomId) {
                            room.unreadCount + 1
                        } else {
                            room.unreadCount
                        },
                    )
                } else {
                    room
                }
            }
            if (updatedActive != _activeRooms.value) _activeRooms.value = updatedActive
            val updatedArchived = _archivedRooms.value.map { room ->
                if (matchesRoom(room, roomId, peerId, me.id)) {
                    room.copy(
                        message = message.message,
                        timestamp = message.timestamp,
                        unreadCount = if (!isOutgoing && openRoomId != room.roomId) {
                            room.unreadCount + 1
                        } else {
                            room.unreadCount
                        },
                    )
                } else {
                    room
                }
            }
            if (updatedArchived != _archivedRooms.value) _archivedRooms.value = updatedArchived
            recomputeTotalUnread()
        }
    }

    /** roomId белгілі болса — тек сонымен; жоқ болса peer бойынша fallback. */
    private fun matchesRoom(room: ChatRoom, roomId: Long?, peerId: Long, meId: Long): Boolean {
        if (roomId != null) return room.roomId == roomId
        return room.otherUserId == peerId || roomPeerId(room, meId) == peerId
    }

    private fun roomPeerId(room: ChatRoom, meId: Long): Long? {
        room.otherUserId?.let { return it }
        if (room.senderId == meId) return room.receiverId
        if (room.receiverId == meId) return room.senderId
        return room.senderId ?: room.receiverId
    }

    private fun onUserPresence(data: JsonElement?, online: Boolean) {
        val root = data as? JsonObject ?: return
        val userId = JsonParser.long(root, "user_id") ?: return
        val lastOnline = if (!online) {
            JsonParser.string(root, "last_online")
                ?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }
        } else {
            null
        }
        fun update(list: List<ChatRoom>): List<ChatRoom> = list.map { room ->
            if (room.otherUserId == userId) {
                room.copy(
                    isOnline = online,
                    lastOnline = if (online) null else (lastOnline ?: room.lastOnline),
                )
            } else {
                room
            }
        }
        _activeRooms.value = update(_activeRooms.value)
        _archivedRooms.value = update(_archivedRooms.value)
    }

    private fun onChatDeleted(data: JsonElement?) {
        val root = data as? JsonObject ?: return
        val chatId = parseRoomId(
            JsonParser.string(root, "chat_id") ?: JsonParser.string(root, "room_id") ?: return,
        ) ?: return
        _activeRooms.value = _activeRooms.value.filter { it.roomId != chatId }
        _archivedRooms.value = _archivedRooms.value.filter { it.roomId != chatId }
        recomputeTotalUnread()
    }

    private fun replaceActive(rooms: List<ChatRoom>) {
        _activeRooms.value = rooms
        recomputeTotalUnread()
    }

    private fun replaceArchived(rooms: List<ChatRoom>) {
        _archivedRooms.value = rooms
    }

    // ---- Optimistic әрекеттер (ChatListViewModel свайп-жолдары) ----

    /**
     * Мұрағатқа/мұрағаттан — REST күтпестен тізімдерді жылжыту
     * (Flutter optimistic archive). Rollback — [restoreRooms].
     */
    fun applyArchivedOptimistic(roomId: Long, archived: Boolean) {
        val active = _activeRooms.value
        val archivedList = _archivedRooms.value
        if (archived) {
            val room = active.find { it.roomId == roomId } ?: return
            _activeRooms.value = active.filterNot { it.roomId == roomId }
            _archivedRooms.value = (archivedList + room).sortedByDescending { it.timestamp }
        } else {
            val room = archivedList.find { it.roomId == roomId } ?: return
            _archivedRooms.value = archivedList.filterNot { it.roomId == roomId }
            _activeRooms.value = (active + room).sortedByDescending { it.timestamp }
        }
        recomputeTotalUnread()
    }

    /** REST сәтсіз болғандағы rollback — екі тізімді қалпына келтіру. */
    fun restoreRooms(active: List<ChatRoom>, archived: List<ChatRoom>) {
        _activeRooms.value = active
        _archivedRooms.value = archived
        recomputeTotalUnread()
    }

    /** Жою — optimistic (chat_deleted push келсе де идемпотент). */
    fun applyDeletedOptimistic(roomId: Long) {
        _activeRooms.value = _activeRooms.value.filterNot { it.roomId == roomId }
        _archivedRooms.value = _archivedRooms.value.filterNot { it.roomId == roomId }
        recomputeTotalUnread()
    }

    private fun recomputeTotalUnread() {
        _totalUnread.value = _activeRooms.value.sumOf { it.unreadCount }
    }

    /** "123" / "chat_123" → 123 (payload пішімі тұрақсыз). */
    fun parseRoomId(raw: String): Long? = parseRoomIdOrNull(raw)

    private fun restFallback() {
        scope.launch {
            when (val result = repository.getChats("ACTIVE")) {
                is ApiResult.Success -> replaceActive(result.value)
                is ApiResult.Error -> Unit
            }
            when (val result = repository.getChats("ARCHIVED")) {
                is ApiResult.Success -> replaceArchived(result.value)
                is ApiResult.Error -> Unit
            }
        }
    }

    private object ChatSocketManagerEvents {
        val DISCONNECT = "disconnect"
        val CONNECT_ERROR = "connect_error"
    }
}