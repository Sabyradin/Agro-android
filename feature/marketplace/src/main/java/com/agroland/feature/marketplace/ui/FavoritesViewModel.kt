package com.agroland.feature.marketplace.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.marketplace.data.Announcement
import com.agroland.feature.marketplace.data.MarketplaceRepository
import com.agroland.feature.marketplace.domain.FavoriteSync
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * FavoritesViewModel — таңдаулылар тізімі: пагинация + id dedup.
 * Сүйіктіден алу сәтті болған item тізімнен бірден алынып тасталады.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: MarketplaceRepository,
    private val favoriteSync: FavoriteSync,
) : ViewModel() {

    private val _items = MutableStateFlow<List<Announcement>>(emptyList())
    val items: StateFlow<List<Announcement>> = _items

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

    private var page = 1
    private val seenIds = mutableSetOf<Long>()

    init {
        refresh()
    }

    fun refresh() {
        page = 1
        seenIds.clear()
        _exhausted.value = false
        _error.value = null
        viewModelScope.launch {
            _loading.value = true
            when (val result = repository.getFavorites(page)) {
                is ApiResult.Success -> {
                    _items.value = result.value.items
                    result.value.items.forEach { seenIds.add(it.id) }
                    _exhausted.value = !result.value.hasMore
                    // Деталь/лентада жасалған toggle-дар кейін қайта кіргенде — ғана таза көрінеді.
                    favoriteSync.applyAll(result.value.items.associate { it.id to true })
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
            when (val result = repository.getFavorites(page + 1)) {
                is ApiResult.Success -> {
                    page = result.value.page
                    _exhausted.value = !result.value.hasMore
                    _items.value = (_items.value + result.value.items.filter { seenIds.add(it.id) })
                }
                is ApiResult.Error -> _events.emit(MarketplaceEvent.ShowError(result.failure.toMarketplaceError()))
            }
            _loadingMore.value = false
        }
    }

    fun toggleFavorite(id: Long) {
        if (_items.value.none { it.id == id }) return
        viewModelScope.launch {
            when (val result = repository.toggleFavorite(id, false)) {
                is ApiResult.Success -> {
                    favoriteSync.apply(id, false)
                    _items.value = _items.value.filterNot { it.id == id }
                }
                is ApiResult.Error ->
                    _events.emit(MarketplaceEvent.ShowError(result.failure.toMarketplaceError()))
            }
        }
    }
}