package com.agroland.feature.marketplace.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.analytics.MonitoringService
import com.agroland.core.network.ApiResult
import com.agroland.feature.marketplace.data.Announcement
import com.agroland.feature.marketplace.data.AnnouncementFilter
import com.agroland.feature.marketplace.data.MarketplaceRepository
import com.agroland.feature.marketplace.data.Suggestion
import com.agroland.feature.marketplace.domain.FavoriteSync
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * HomeViewModel — басты экран: ұсынылатын лента (announcements/recommended)
 * + іздеу ұсыныстары (500ms debounce, last-wins requestId — тез терілеуде
 * ескі жауап кешіктірілген жаңа жауапты баспайды).
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MarketplaceRepository,
    private val favoriteSync: FavoriteSync,
    private val monitoringService: MonitoringService,
) : ViewModel() {

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

    private val _suggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val suggestions: StateFlow<List<Suggestion>> = _suggestions

    private val _suggestionsLoading = MutableStateFlow(false)
    val suggestionsLoading: StateFlow<Boolean> = _suggestionsLoading

    private val _events = MutableSharedFlow<MarketplaceEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<MarketplaceEvent> = _events

    private val _filter = MutableStateFlow<AnnouncementFilter?>(null)

    /** Басты беттегі сүзгі — null болса ұсынылатын лента көрсетіледі. */
    val filter: StateFlow<AnnouncementFilter?> = _filter

    private var page = 1

    private var searchJob: Job? = null
    private var searchRequestId = 0

    init {
        refresh()
    }

    fun refresh() {
        page = 1
        _exhausted.value = false
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = loadPage(page)) {
                is ApiResult.Success -> _rawItems.value = result.value.items
                is ApiResult.Error -> _error.value = result.failure.toMarketplaceError()
            }
            _loading.value = false
        }
    }

    fun loadMore() {
        if (_loading.value || _loadingMore.value || _exhausted.value) return
        viewModelScope.launch {
            _loadingMore.value = true
            when (val result = loadPage(page + 1)) {
                is ApiResult.Success -> {
                    page = result.value.page
                    _exhausted.value = !result.value.hasMore
                    _rawItems.value = (_rawItems.value + result.value.items).distinctBy { it.id }
                }
                is ApiResult.Error -> _events.emit(MarketplaceEvent.ShowError(result.failure.toMarketplaceError()))
            }
            _loadingMore.value = false
        }
    }

    /**
     * Сүзгіні басты лентаға орнында қолданады — бұрын «Қолдану» бөлек лента
     * экранына апаратын, енді қолданушы басты беттен шықпайды. null немесе
     * әдепкі сүзгі — ұсынылатын лентаға қайтару.
     */
    fun applyFilter(filter: AnnouncementFilter?) {
        val normalized = filter?.takeIf { !it.isDefault() || it.isVip != null }
        if (normalized == _filter.value) return
        _filter.value = normalized
        refresh()
    }

    /** Сүзгі бар болса — сүзілген лента, әйтпесе ұсынылатын лента. */
    private suspend fun loadPage(page: Int) = _filter.value
        ?.let { repository.getAnnouncements(it, page) }
        ?: repository.getRecommended(page)

    fun toggleFavorite(id: Long) {
        val current = items.value.firstOrNull { it.id == id } ?: return
        val target = !current.isFavorite
        // Оптимистік күй: FavoriteSync бірден қолданылады, сәтсіз болса қайтарылады.
        favoriteSync.apply(id, target)
        viewModelScope.launch {
            when (val result = repository.toggleFavorite(id, target)) {
                is ApiResult.Error -> {
                    favoriteSync.apply(id, current.isFavorite)
                    _events.emit(MarketplaceEvent.ShowError(result.failure.toMarketplaceError()))
                }
                is ApiResult.Success -> favoriteSync.apply(id, target)
            }
        }
    }

    // Іздеу ұсыныстары.

    /** _SearchField: debounce 500ms; last-wins requestId тез тергенде ескі жауапты елемейді. */
    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            searchRequestId++
            _suggestions.value = emptyList()
            _suggestionsLoading.value = false
            return
        }
        _suggestionsLoading.value = true
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val requestId = ++searchRequestId
            when (val result = repository.searchSuggestions(trimmed)) {
                is ApiResult.Success -> if (requestId == searchRequestId) {
                    // Фаза 19 (Flutter SearchSuggestionNotifier parity):
                    // іздеу сұрауы мониторингке түседі + POST /search/log.
                    monitoringService.trackSearch(trimmed)
                    repository.logSearchQuery(trimmed)
                    _suggestions.value = result.value
                }
                is ApiResult.Error -> if (requestId == searchRequestId) {
                    _suggestions.value = emptyList()
                    _events.emit(MarketplaceEvent.ShowError(result.failure.toMarketplaceError()))
                }
            }
            if (requestId == searchRequestId) _suggestionsLoading.value = false
        }
    }

    fun clearSuggestions() {
        searchRequestId++
        searchJob?.cancel()
        _suggestions.value = emptyList()
        _suggestionsLoading.value = false
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 500L
    }
}