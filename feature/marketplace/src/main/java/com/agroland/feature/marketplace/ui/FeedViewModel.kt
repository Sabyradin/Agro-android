package com.agroland.feature.marketplace.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.marketplace.data.Announcement
import com.agroland.feature.marketplace.data.AnnouncementFilter
import com.agroland.feature.marketplace.data.MarketplaceRepository
import com.agroland.feature.marketplace.domain.FavoriteSync
import com.agroland.feature.marketplace.domain.FeedInterleaver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * FeedViewModel — сүзгіленген хабарлама лентасы (AnnouncementsListPage).
 * FeedInterleaver арқылы 4 regular + 2 VIP аралас порциялап жүктейді
 * (Flutter AnnouncementsNotifier логикасы).
 */
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val repository: MarketplaceRepository,
    private val favoriteSync: FavoriteSync,
) : ViewModel() {

    private val interleaver = FeedInterleaver(repository)

    private val _filter = MutableStateFlow(AnnouncementFilter())
    val filter: StateFlow<AnnouncementFilter> = _filter

    private val _rawItems = MutableStateFlow<List<Announcement>>(emptyList())
    val items: StateFlow<List<Announcement>> = combine(_rawItems, favoriteSync.overrides) { list, ov ->
        list.map { it.copy(isFavorite = ov[it.id] ?: it.isFavorite) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore

    private val _exhausted = MutableStateFlow(false)
    val exhausted: StateFlow<Boolean> = _exhausted

    private val _error = MutableStateFlow<MarketplaceError?>(null)
    val error: StateFlow<MarketplaceError?> = _error

    private val _events = MutableSharedFlow<MarketplaceEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<MarketplaceEvent> = _events

    private var loadedOnce = false

    /** Алғашқы жүктеу — route параметрінен келген сүзгімен. */
    fun initialize(initial: AnnouncementFilter) {
        if (loadedOnce) return
        loadedOnce = true
        _filter.value = initial
        loadFirst()
    }

    fun applyFilter(newFilter: AnnouncementFilter) {
        if (newFilter == _filter.value && loadedOnce) return
        _filter.value = newFilter
        loadFirst()
    }

    private fun loadFirst() {
        interleaver.reset()
        _rawItems.value = emptyList()
        _exhausted.value = false
        _error.value = null
        viewModelScope.launch {
            _loading.value = true
            val chunk = interleaver.nextChunk(_filter.value)
            if (chunk.isEmpty()) {
                _exhausted.value = true
                // Екі ағын да қате берсе — қате экраны; шынымен бос болса — бос күй.
                interleaver.lastFailure
                    ?.takeIf { _rawItems.value.isEmpty() }
                    ?.let { _error.value = it.toMarketplaceError() }
            } else {
                _rawItems.value = chunk
            }
            _loading.value = false
        }
    }

    fun loadMore() {
        if (_loading.value || _loadingMore.value || _exhausted.value) return
        viewModelScope.launch {
            _loadingMore.value = true
            val chunk = interleaver.nextChunk(_filter.value)
            if (chunk.isEmpty()) {
                _exhausted.value = true
            } else {
                _rawItems.value = (_rawItems.value + chunk).distinctBy { it.id }
            }
            _loadingMore.value = false
        }
    }

    fun refresh() {
        if (!loadedOnce) return
        loadFirst()
    }

    fun toggleFavorite(id: Long) {
        val current = items.value.firstOrNull { it.id == id } ?: return
        val target = !current.isFavorite
        favoriteSync.apply(id, target)
        interleaver.markFavorite(id, target)
        viewModelScope.launch {
            when (val result = repository.toggleFavorite(id, target)) {
                is ApiResult.Error -> {
                    favoriteSync.apply(id, current.isFavorite)
                    interleaver.markFavorite(id, current.isFavorite)
                    _events.emit(MarketplaceEvent.ShowError(result.failure.toMarketplaceError()))
                }
                is ApiResult.Success -> favoriteSync.apply(id, target)
            }
        }
    }

    override fun onCleared() {
        interleaver.reset()
        super.onCleared()
    }
}