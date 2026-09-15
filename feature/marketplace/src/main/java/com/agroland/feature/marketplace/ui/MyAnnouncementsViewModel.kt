package com.agroland.feature.marketplace.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.marketplace.data.Announcement
import com.agroland.feature.marketplace.data.AnnouncementsPage
import com.agroland.feature.marketplace.data.MarketplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * MyAnnouncementsViewModel — AnnouncementsByStatusNotifier: менің жарнамаларым
 * статус бойынша (active/inactive/pending/rejected) + activate/deactivate/delete.
 */
@HiltViewModel
class MyAnnouncementsViewModel @Inject constructor(
    private val repository: MarketplaceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    sealed interface Event {
        data class ShowError(val error: MarketplaceError) : Event
        data class StatusChanged(val newStatus: String) : Event
        data object Deleted : Event
    }

    /** Route параметрі — әдепкі active. */
    val status: String = savedStateHandle["status"] ?: STATUS_ACTIVE

    private val _items = MutableStateFlow<List<Announcement>>(emptyList())
    val items: StateFlow<List<Announcement>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private val _exhausted = MutableStateFlow(false)
    val exhausted: StateFlow<Boolean> = _exhausted.asStateFlow()

    private val _error = MutableStateFlow<MarketplaceError?>(null)
    val error: StateFlow<MarketplaceError?> = _error.asStateFlow()

    /** Қайтару себебі диалогы — ашылған жарнама id + мәтіні. */
    private val _rejectMessage = MutableStateFlow<Pair<Long, String>?>(null)
    val rejectMessage: StateFlow<Pair<Long, String>?> = _rejectMessage.asStateFlow()

    private val _actionInProgress = MutableStateFlow<Long?>(null)
    val actionInProgress: StateFlow<Long?> = _actionInProgress.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private var page = 1
    private var seenIds = mutableSetOf<Long>()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            page = 1
            seenIds = mutableSetOf()
            when (val result = repository.getMyAnnouncements(status, page)) {
                is ApiResult.Success -> {
                    _items.value = result.value.items.distinctBy { it.id }
                    seenIds = _items.value.map { it.id }.toMutableSet()
                    _exhausted.value = !result.value.hasMore
                }
                is ApiResult.Error -> _error.value = result.failure.toMarketplaceError()
            }
            _loading.value = false
        }
    }

    fun loadMore() {
        if (_loading.value || _loadingMore.value || _exhausted.value) return
        viewModelScope.launch {
            _loadingMore.value = true
            when (val result = repository.getMyAnnouncements(status, page + 1)) {
                is ApiResult.Success -> {
                    page += 1
                    val fresh = result.value.items.filter { seenIds.add(it.id) }
                    _items.value = _items.value + fresh
                    _exhausted.value = !result.value.hasMore
                }
                is ApiResult.Error -> _events.emit(Event.ShowError(result.failure.toMarketplaceError()))
            }
            _loadingMore.value = false
        }
    }

    /** Активация/деактивация — тізімнен бірден жоғалады (басқа статусқа кетті). */
    fun toggleActive(item: Announcement) {
        if (_actionInProgress.value != null) return
        viewModelScope.launch {
            _actionInProgress.value = item.id
            val activate = item.status?.equals("active", ignoreCase = true) != true
            val result = if (activate) {
                repository.activateAnnouncement(item.id)
            } else {
                repository.deactivateAnnouncement(item.id)
            }
            when (result) {
                is ApiResult.Success -> {
                    _items.value = _items.value.filter { it.id != item.id }
                    _events.emit(Event.StatusChanged(if (activate) STATUS_ACTIVE else STATUS_INACTIVE))
                }
                is ApiResult.Error -> _events.emit(Event.ShowError(result.failure.toMarketplaceError()))
            }
            _actionInProgress.value = null
        }
    }

    fun delete(item: Announcement) {
        if (_actionInProgress.value != null) return
        viewModelScope.launch {
            _actionInProgress.value = item.id
            when (val result = repository.deleteAnnouncement(item.id)) {
                is ApiResult.Success -> {
                    _items.value = _items.value.filter { it.id != item.id }
                    _events.emit(Event.Deleted)
                }
                is ApiResult.Error -> _events.emit(Event.ShowError(result.failure.toMarketplaceError()))
            }
            _actionInProgress.value = null
        }
    }

    /** Қайтару себебі — GET /announcement/reject-message/{id}. */
    fun showRejectMessage(id: Long) {
        viewModelScope.launch {
            when (val result = repository.getRejectMessage(id)) {
                is ApiResult.Success -> _rejectMessage.value = id to (result.value ?: "")
                is ApiResult.Error -> _events.emit(Event.ShowError(result.failure.toMarketplaceError()))
            }
        }
    }

    fun dismissRejectMessage() {
        _rejectMessage.value = null
    }

    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_INACTIVE = "inactive"
        const val STATUS_PENDING = "pending"
        const val STATUS_REJECTED = "rejected"

        /** Табтар — реті мен API кілттері. */
        val STATUS_TABS = listOf(STATUS_ACTIVE, STATUS_PENDING, STATUS_INACTIVE, STATUS_REJECTED)
    }
}

/**
 * AnnouncementActionsViewModel — ProfileAnnouncementPage (owner detail) әрекеттері:
 * activate/deactivate/delete бір жарнама үшін.
 */
@HiltViewModel
class AnnouncementActionsViewModel @Inject constructor(
    private val repository: MarketplaceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    sealed interface Event {
        data class ShowError(val error: MarketplaceError) : Event
        data object StatusChanged : Event
        data object Deleted : Event
    }

    val announcementId: Long = savedStateHandle["id"] ?: 0L

    private val _actionInProgress = MutableStateFlow(false)
    val actionInProgress: StateFlow<Boolean> = _actionInProgress.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    fun activate(id: Long) = runAction { repository.activateAnnouncement(id) }

    fun deactivate(id: Long) = runAction { repository.deactivateAnnouncement(id) }

    fun delete(id: Long) = runAction(isDelete = true) { repository.deleteAnnouncement(id) }

    private fun runAction(isDelete: Boolean = false, block: suspend () -> ApiResult<Unit>) {
        if (_actionInProgress.value) return
        viewModelScope.launch {
            _actionInProgress.value = true
            when (val result = block()) {
                is ApiResult.Success ->
                    _events.emit(if (isDelete) Event.Deleted else Event.StatusChanged)
                is ApiResult.Error -> _events.emit(Event.ShowError(result.failure.toMarketplaceError()))
            }
            _actionInProgress.value = false
        }
    }
}