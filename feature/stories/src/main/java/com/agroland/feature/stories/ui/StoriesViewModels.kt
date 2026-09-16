package com.agroland.feature.stories.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.common.settings.SettingsDataStore
import com.agroland.core.network.ApiResult
import com.agroland.core.network.auth.TokenStore
import com.agroland.feature.stories.data.MainBanner
import com.agroland.feature.stories.data.StoriesRepository
import com.agroland.feature.stories.data.StoryItem
import com.agroland.feature.stories.domain.StoryViewerStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Басты беттің баннер-каруселі (Flutter mainBannersProvider +
 * MainBannerViewedNotifier): GET /banners/main + көрілген id-лер DataStore-тан.
 */
data class MainBannersState(
    val banners: List<MainBanner> = emptyList(),
    val loading: Boolean = true,
)

@HiltViewModel
class MainBannerViewModel @Inject constructor(
    private val repository: StoriesRepository,
    private val settings: SettingsDataStore,
    val viewer: StoryViewerStateHolder,
) : ViewModel() {

    private val _banners = MutableStateFlow(MainBannersState())
    val banners: StateFlow<MainBannersState> = _banners.asStateFlow()

    /** Көрілген баннер id-лері (жиек түсі үшін) — DataStore бұлағы. */
    val viewedIds: StateFlow<Set<String>> = settings.viewedBanners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = repository.getMainBanners()) {
                is ApiResult.Success ->
                    _banners.update { it.copy(banners = result.value, loading = false) }

                is ApiResult.Error ->
                    // Карусель ешқашан бос қалмайды: статик промо + fallback
                    // суреттері қалады (Flutter mainBannersProvider error → []).
                    _banners.update { it.copy(banners = emptyList(), loading = false) }
            }
        }
    }

    /** Viewer сторисі көрсетілгенде — id DataStore-қа (жиек сұрға ауысады). */
    fun markViewed(id: String) {
        viewModelScope.launch { settings.markBannerViewed(id) }
    }
}

/**
 * Admin сторилер жолы (Flutter StoriesNotifier): GET /stories,
 * көрілгенді белгілеу — ТЕК авторизацияланған қолданушы үшін (auth-gate),
 * қате — silent (сориді көруге әсер етпейді).
 */
data class StoriesState(
    val stories: List<StoryItem> = emptyList(),
    val loading: Boolean = true,
)

@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val repository: StoriesRepository,
    private val tokenStore: TokenStore,
    val viewer: StoryViewerStateHolder,
) : ViewModel() {

    private val _state = MutableStateFlow(StoriesState())
    val state: StateFlow<StoriesState> = _state.asStateFlow()

    init {
        // Бос/қате → жол мүлдем жасырылады (Flutter SizedBox.shrink()).
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = repository.getStories()) {
                is ApiResult.Success ->
                    _state.update { it.copy(stories = result.value, loading = false) }

                is ApiResult.Error ->
                    _state.update { it.copy(stories = emptyList(), loading = false) }
            }
        }
    }

    /** Стори көрсетілді: авторизацияланған болса ғана POST view + жергілікті
     * isViewed=true (Flutter StoriesNotifier.markStoryAsViewed). */
    fun onStoryShown(storyId: String) {
        val token = tokenStore.accessToken.value
        if (token.isNullOrBlank()) return
        viewModelScope.launch {
            if (repository.markStoryViewed(storyId)) {
                _state.update { current ->
                    current.copy(
                        stories = current.stories.map {
                            if (it.id == storyId && !it.isViewed) it.copy(isViewed = true) else it
                        },
                    )
                }
            }
        }
    }

    /** CTA түртуді тіркеу (POST /marketing/posts/{id}/click, анонимді де). */
    fun recordCtaClick(storyId: String) {
        viewModelScope.launch { repository.recordCtaClick(storyId) }
    }
}