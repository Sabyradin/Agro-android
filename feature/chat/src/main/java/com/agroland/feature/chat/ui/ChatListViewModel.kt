package com.agroland.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.chat.data.ChatPrefs
import com.agroland.feature.chat.data.ChatRepository
import com.agroland.feature.chat.data.ChatRoom
import com.agroland.feature.chat.data.CurrentUserProvider
import com.agroland.feature.chat.domain.ChatSocketService
import com.agroland.feature.chat.data.ChatSocketManager.State
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Чат тізіміндегі бір қатардың көрсетілетін деректері (ChatViewItem паритеті). */
data class ChatRow(
    val room: ChatRoom,
    /** Пинделген/үнсіз күйі — ChatPrefs-тен. */
    val pinned: Boolean,
    val muted: Boolean,
)

/** Чат тізімі күйі. */
data class ChatListUiState(
    val loading: Boolean = true,
    /** REST сәтсіз + тізімдер бос → ErrorWithRetry. */
    val loadFailed: Boolean = false,
    val searchQuery: String = "",
    val rows: List<ChatRow> = emptyList(),
    val archivedRows: List<ChatRow> = emptyList(),
    val archivedCount: Int = 0,
    val archivedPreview: String = "",
    /** Ағымда свайппен ашық тұрған қатар (бір уақытта біреу ғана). */
    val openSwipeRoomId: Long? = null,
)

/** Snackbar оқиғалары (Flutter паритеті — растау/орындалды мәтіндері). */
sealed interface ChatListEvent {
    data class Message(val text: ChatListMessage) : ChatListEvent
}

enum class ChatListMessage { ARCHIVED, UNARCHIVED, DELETED, PINNED, UNPINNED }

/**
 * Чат тізімі ViewModel (Flutter ChatPage + ChatSocketService паритеті):
 *  - дерек көзі — ChatSocketService (socket push + REST fallback);
 *  - іздеу — 300мс debounce (Flutter Debouncer(300ms));
 *  - топтау: жүйелі (authorId=0) → мен сатып аламын → мен сатамын,
 *    пинделгендер басына (Flutter _RoomsView реті);
 *  - әрекеттер: пин/үнсіз/мұрағат (optimistic + rollback)/жою.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val service: ChatSocketService,
    private val repository: ChatRepository,
    private val prefs: ChatPrefs,
    private val currentUser: CurrentUserProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListUiState())
    val state: StateFlow<ChatListUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ChatListEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ChatListEvent> = _events.asSharedFlow()

    /** Іздеу жолы (UI терімін жедел көрсету үшін) — debounce-сіз. */
    private val _searchInput = MutableStateFlow("")
    val searchInput: StateFlow<String> = _searchInput.asStateFlow()

    /** Ағымдағы қолданушы (Сіз: префиксі + топтау үшін) — CurrentUserProvider. */
    val me: StateFlow<com.agroland.feature.chat.data.CurrentUserProvider.CurrentUser?> =
        currentUser.user

    private var initialLoaded = false
    private var initialFailed = false

    init {
        // Іздеу: 300мс debounce кейін фильтр қайта есептеледі.
        _searchInput
            .debounce(300)
            .onEach { query ->
                _state.value = _state.value.copy(searchQuery = query)
                rebuild()
            }
            .launchIn(viewModelScope)

        // Тізімдер өзгерсе — қайта құрамыз (socket push / REST / presence).
        combine(
            service.activeRooms,
            service.archivedRooms,
            prefs.pinnedIds,
            prefs.mutedIds,
        ) { active, archived, pinned, muted ->
            Triple(active, archived, pinned to muted)
        }.onEach { rebuild() }.launchIn(viewModelScope)

        // Socket күйі өзгерсе — loading жалауын қайта бағалаймыз.
        service.connectionState.onEach { state ->
            if (state == State.CONNECTED && !initialLoaded) {
                initialLoaded = true
            }
            refreshUiFlags()
        }.launchIn(viewModelScope)

        // Бірінші жүктелу — REST-тан дәл хабар (сәтсіз/сәтті).
        viewModelScope.launch {
            var ok = false
            when (val result = repository.getChats("ACTIVE")) {
                is ApiResult.Success -> ok = true
                is ApiResult.Error -> Unit
            }
            when (val result = repository.getChats("ARCHIVED")) {
                is ApiResult.Success -> ok = true
                is ApiResult.Error -> Unit
            }
            initialLoaded = true
            initialFailed = !ok
            refreshUiFlags()
        }

        viewModelScope.launch { currentUser.refresh() }
        service.refreshFromRest()
    }

    /** Іздеу жолы (Flutter ChatSearchController.onChanged — debounce ішінде). */
    fun onSearchChange(value: String) {
        _searchInput.value = value
    }

    fun clearSearch() {
        if (_searchInput.value.isNotEmpty()) {
            _searchInput.value = ""
            _state.value = _state.value.copy(searchQuery = "")
            rebuild()
        }
    }

    fun retry() {
        initialFailed = false
        _state.value = _state.value.copy(loading = true, loadFailed = false)
        viewModelScope.launch {
            var ok = false
            when (val result = repository.getChats("ACTIVE")) {
                is ApiResult.Success -> ok = true
                is ApiResult.Error -> Unit
            }
            when (val result = repository.getChats("ARCHIVED")) {
                is ApiResult.Success -> ok = true
                is ApiResult.Error -> Unit
            }
            initialLoaded = true
            initialFailed = !ok
            refreshUiFlags()
        }
        service.refreshFromRest()
    }

    fun refresh() {
        service.refreshFromRest()
    }

    /** Бір уақытта бір ғана свайп ашық болсын. */
    fun setOpenSwipe(roomId: Long?) {
        if (_state.value.openSwipeRoomId != roomId) {
            _state.value = _state.value.copy(openSwipeRoomId = roomId)
        }
    }

    fun togglePin(room: ChatRoom) {
        viewModelScope.launch {
            val wasPinned = prefs.isPinned(room.roomId)
            prefs.togglePin(room.roomId)
            _events.tryEmit(
                ChatListEvent.Message(
                    if (wasPinned) ChatListMessage.UNPINNED else ChatListMessage.PINNED,
                ),
            )
        }
    }

    fun toggleMute(room: ChatRoom) {
        viewModelScope.launch { prefs.toggleMute(room.roomId) }
    }

    /** Мұрағатқа/мұрағаттан — optimistic + REST сәтсіз болса rollback (Flutter паритеті). */
    fun setArchived(room: ChatRoom, archived: Boolean) {
        val previousActive = service.activeRooms.value
        val previousArchived = service.archivedRooms.value
        // Optimistic: екі тізімді бірден жылжытамыз.
        service.applyArchivedOptimistic(room.roomId, archived)
        _events.tryEmit(
            ChatListEvent.Message(
                if (archived) ChatListMessage.ARCHIVED else ChatListMessage.UNARCHIVED,
            ),
        )
        viewModelScope.launch {
            val result = repository.updateChatStatus(room.roomId, if (archived) "ARCHIVED" else "ACTIVE")
            if (result is ApiResult.Error) {
                // Rollback — қолданушы көрген күйге қайтарамыз.
                service.restoreRooms(previousActive, previousArchived)
            }
        }
    }

    fun deleteChat(room: ChatRoom) {
        viewModelScope.launch {
            // Flutter: deleteChat + clearChatHistory екеуі де шақырылады,
            // тізімнен бірден алынады, chat_deleted push келсе де идиотент.
            service.applyDeletedOptimistic(room.roomId)
            repository.clearChatHistory(room.roomId)
            repository.deleteChat(room.roomId)
            _events.tryEmit(ChatListEvent.Message(ChatListMessage.DELETED))
        }
    }

    private fun refreshUiFlags() {
        val hasContent = service.activeRooms.value.isNotEmpty() ||
            service.archivedRooms.value.isNotEmpty()
        val failed = initialFailed && !hasContent
        _state.value = _state.value.copy(
            loading = !initialLoaded && !failed,
            loadFailed = failed,
        )
    }

    /** Топтау + фильтр (Flutter _RoomsView.build): pinned-first, 3 топ реті. */
    private fun rebuild() {
        val query = _state.value.searchQuery.trim().lowercase()
        val me = currentUser.user.value
        val active = service.activeRooms.value
        val archived = service.archivedRooms.value
        val pinnedIds = prefs.pinnedIds.value
        val mutedIds = prefs.mutedIds.value

        val system = active.filter { it.announcementAuthorId == 0L }
        val meSelling = active.filter { it.announcementAuthorId != 0L && it.announcementAuthorId == me?.id }
        val meBuying = active.filter { it.announcementAuthorId != me?.id && it.announcementAuthorId != 0L }

        val ordered = system + meBuying + meSelling
        val pinnedRooms = ordered.filter { pinnedIds.contains(it.roomId) }
        val unpinnedRooms = ordered.filter { !pinnedIds.contains(it.roomId) }

        val filtered = (pinnedRooms + unpinnedRooms).filter { room ->
            query.isEmpty() || matchesQuery(room, query, me?.id)
        }

        val rows = filtered.map { ChatRow(room = it, pinned = pinnedIds.contains(it.roomId), muted = mutedIds.contains(it.roomId)) }

        val archivedPreview = archived.joinToString(", ") { peerName(it, me?.id) }

        _state.value = _state.value.copy(
            rows = rows,
            archivedRows = archived.map {
                ChatRow(room = it, pinned = pinnedIds.contains(it.roomId), muted = mutedIds.contains(it.roomId))
            },
            archivedCount = archived.size,
            archivedPreview = archivedPreview,
        )
    }

    /** otherUserName ?? (user.id == senderId ? receiverName : senderName). */
    fun peerName(room: ChatRoom, meId: Long?): String {
        room.otherUserName?.takeIf { it.isNotBlank() }?.let { return it }
        return if (meId != null && room.senderId == meId) room.receiverName else room.senderName
    }

    private fun matchesQuery(room: ChatRoom, query: String, meId: Long?): Boolean {
        val name = peerName(room, meId).lowercase()
        val title = room.announcementTitle.lowercase()
        val message = room.message.lowercase()
        return name.contains(query) || title.contains(query) || message.contains(query)
    }

    /** Мұрағат беті үшін — архивтенгендер тізімі (ArchivedChatsPage). */
    fun archivedRows(): List<ChatRow> = _state.value.archivedRows

    /** Синхронды күй сұраулары (мұрағат бетінің свайп-батырмалары). */
    fun isPinned(room: ChatRoom): Boolean = prefs.pinnedIds.value.contains(room.roomId)
    fun isMuted(room: ChatRoom): Boolean = prefs.mutedIds.value.contains(room.roomId)

    companion object {
        /** Иә/жоқ қатарларында пайдаланылатын қысқа кешігу (rollback уақытына жақын). */
        const val ARCHIVE_ROLLBACK_DELAY_MS = 0L
    }
}