package com.agroland.feature.marketplace.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.marketplace.data.FullAnnouncement
import com.agroland.feature.marketplace.data.MarketplaceRepository
import com.agroland.feature.marketplace.domain.FavoriteSync
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
 * AnnouncementDetailViewModel — деталь экраны: толық жарнама (FIFO кешпен fallback),
 * сүйіктіге қосу/алу, ұқсас және қосымша жарнамалар.
 */
@HiltViewModel
class AnnouncementDetailViewModel @Inject constructor(
    private val repository: MarketplaceRepository,
    private val favoriteSync: FavoriteSync,
) : ViewModel() {

    private val _detail = MutableStateFlow<FullAnnouncement?>(null)
    val detail: StateFlow<FullAnnouncement?> = _detail

    val favorite: StateFlow<Boolean> = combine(_detail, favoriteSync.overrides) { d, ov ->
        d?.let { ov[it.base.id] ?: it.base.isFavorite } ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<MarketplaceError?>(null)
    val error: StateFlow<MarketplaceError?> = _error

    private val _events = MutableSharedFlow<MarketplaceEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<MarketplaceEvent> = _events

    private var currentId: Long? = null

    /** Route параметрінен келген id — бір рет қана жүктеледі. */
    fun load(id: Long) {
        if (currentId == id && _detail.value != null) return
        currentId = id
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.getAnnouncement(id)) {
                is ApiResult.Success -> _detail.value = result.value
                is ApiResult.Error -> _error.value = result.failure.toMarketplaceError()
            }
            _loading.value = false
        }
    }

    /** Кештегі нұсқадан қайта жүктеу (сүйікті өзгерістерін backend-тен алу). */
    fun reload() {
        currentId?.let { currentId = null; load(it) }
    }

    fun toggleFavorite() {
        val id = currentId ?: return
        val target = !favorite.value
        favoriteSync.apply(id, target)
        viewModelScope.launch {
            when (val result = repository.toggleFavorite(id, target)) {
                is ApiResult.Error -> {
                    favoriteSync.apply(id, !target)
                    _events.emit(MarketplaceEvent.ShowError(result.failure.toMarketplaceError()))
                }
                is ApiResult.Success -> favoriteSync.apply(id, target)
            }
        }
    }
}