package com.agroland.feature.reviews.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.core.network.error.Failure
import com.agroland.feature.profile.data.ProfileRepository
import com.agroland.feature.reviews.data.MyReviewCandidate
import com.agroland.feature.reviews.data.ProfileReview
import com.agroland.feature.reviews.data.Review
import com.agroland.feature.reviews.data.ReviewAnnouncementInfo
import com.agroland.feature.reviews.data.ReviewRepository
import com.agroland.feature.reviews.data.SellerReview
import com.agroland.feature.reviews.data.SellerReviewSummary
import com.agroland.feature.reviews.data.ViewedAnnouncementsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI-ға берiлетiнет қате: backend адам тіліндегі message > generic
 * (спек §4 — шикі error_code ешқашан көрсетілмейді).
 */
data class ReviewError(
    val backendMessage: String? = null,
    val isNetwork: Boolean = false,
)

fun ReviewError.displayText(networkMessage: String, genericMessage: String): String =
    backendMessage ?: if (isNetwork) networkMessage else genericMessage

fun Failure.toReviewError(): ReviewError = when (this) {
    is Failure.Network -> ReviewError(isNetwork = true)
    else -> ReviewError(backendMessage = (this as? Failure.Http)?.error?.message)
}

sealed interface ReviewEvent {
    data class ShowError(val error: ReviewError) : ReviewEvent
    /** Пікір сәтті жіберілді (SendReviewPage → pop). */
    data object Sent : ReviewEvent
}

/**
 * Жарнама пікірлері — толық бет (AnnouncementReviewsPage) және деталь
 * бетіндегі Kaspi-стиль превью (AnnouncementReviewsSection) екеуі де
 * қолданады. Route арқылы келсе SavedStateHandle «announcementId»;
 * превью — keyed VM + initialize().
 */
@HiltViewModel
class AnnouncementReviewsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReviewRepository,
) : ViewModel() {

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<Failure?>(null)
    val error: StateFlow<Failure?> = _error.asStateFlow()

    /** Ortasha рейтинг — превью жиынтығы (base.rating-тан тәуелсіз есептеледі). */
    val averageRating: Double
        get() = _reviews.value.map { it.rating }.takeIf { it.isNotEmpty() }?.let { it.average() } ?: 0.0

    private var announcementId: Long = savedStateHandle.get<Long>("announcementId") ?: -1L
    private var loaded = false

    init {
        if (announcementId > 0) refresh()
    }

    /** Keyed VM (деталь бетінің превьюі) үшін — бір рет инициализация. */
    fun initialize(id: Long) {
        if (loaded || id <= 0) return
        announcementId = id
        refresh()
    }

    fun refresh() {
        if (announcementId <= 0) return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.getReviews(announcementId)) {
                is ApiResult.Success -> _reviews.value = result.value
                is ApiResult.Error -> _error.value = result.failure
            }
            loaded = true
            _loading.value = false
        }
    }
}

/** FeedbackAdCard — пікір беттеріндегі жарнама қысқаша көрінісі. */
@HiltViewModel
class ReviewAnnouncementViewModel @Inject constructor(
    private val repository: ReviewRepository,
) : ViewModel() {

    private val _info = MutableStateFlow<ReviewAnnouncementInfo?>(null)
    val info: StateFlow<ReviewAnnouncementInfo?> = _info.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun load(announcementId: Long) {
        if (_loading.value || (_info.value?.id == announcementId)) return
        viewModelScope.launch {
            _loading.value = true
            when (val result = repository.getAnnouncementInfo(announcementId)) {
                is ApiResult.Success -> _info.value = result.value
                is ApiResult.Error -> _info.value = ReviewAnnouncementInfo(id = announcementId)
            }
            _loading.value = false
        }
    }
}

/** Пікір қалдыру (SendReviewPage) — rating таңдау + мәтін ≤ 1000 таңба. */
@HiltViewModel
class SendReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReviewRepository,
) : ViewModel() {

    private val announcementId: Long = savedStateHandle.get<Long>("announcementId") ?: -1L

    private val _rating = MutableStateFlow(0)
    val rating: StateFlow<Int> = _rating.asStateFlow()

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _events = MutableSharedFlow<ReviewEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ReviewEvent> = _events.asSharedFlow()

    fun selectRating(value: Int) {
        _rating.value = value
    }

    fun updateText(value: String) {
        if (value.length <= MAX_TEXT_LENGTH) _text.value = value
    }

    /** Рейтинг 0 болса — backend жібермейді (Flutter PrimaryButton isEnabled). */
    fun send() {
        if (_rating.value == 0 || _sending.value) return
        viewModelScope.launch {
            _sending.value = true
            when (val result = repository.sendReview(announcementId, _text.value, _rating.value)) {
                is ApiResult.Success -> _events.tryEmit(ReviewEvent.Sent)
                is ApiResult.Error ->
                    _events.tryEmit(ReviewEvent.ShowError(result.failure.toReviewError()))
            }
            _sending.value = false
        }
    }

    companion object {
        const val MAX_TEXT_LENGTH = 1000
    }
}

/** GET /user/{userId}/reviews — жарнама пікір жиынтықтары (ProfileReviewsPage). */
@HiltViewModel
class ProfileReviewsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReviewRepository,
) : ViewModel() {

    private val userId: Long = savedStateHandle.get<Long>("userId") ?: -1L

    private val _items = MutableStateFlow<List<ProfileReview>>(emptyList())
    val items: StateFlow<List<ProfileReview>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<Failure?>(null)
    val error: StateFlow<Failure?> = _error.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (userId <= 0) return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.getProfileReviews(userId)) {
                is ApiResult.Success -> _items.value = result.value
                is ApiResult.Error -> _error.value = result.failure
            }
            _loading.value = false
        }
    }
}

/**
 * GET /seller/{userId}/reviews — сатушы пікірлері (SellerReviewsPage).
 * Like/unlike орында жаңартылады (backend авторитетті is_liked/like_count
 * қайтарады — Flutter SellerReviewsNotifier.toggleLike).
 */
@HiltViewModel
class SellerReviewsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReviewRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val userId: Long = savedStateHandle.get<Long>("userId") ?: -1L

    private val _summary = MutableStateFlow<SellerReviewSummary?>(null)
    val summary: StateFlow<SellerReviewSummary?> = _summary.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<Failure?>(null)
    val error: StateFlow<Failure?> = _error.asStateFlow()

    /** Өз пікіріндегі Like батырмасы жасырылады (Flutter isOwnReview). */
    private val _currentUserId = MutableStateFlow<Long?>(null)
    val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    private val _events = MutableSharedFlow<ReviewEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ReviewEvent> = _events.asSharedFlow()

    init {
        refresh()
        viewModelScope.launch {
            when (val result = profileRepository.getProfile()) {
                is ApiResult.Success -> _currentUserId.value = result.value.id
                is ApiResult.Error -> Unit
            }
        }
    }

    fun refresh() {
        if (userId <= 0) return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.getSellerReviews(userId)) {
                is ApiResult.Success -> _summary.value = result.value
                is ApiResult.Error -> _error.value = result.failure
            }
            _loading.value = false
        }
    }

    fun toggleLike(review: SellerReview) {
        viewModelScope.launch {
            val result = if (review.isLiked) {
                repository.unlikeSellerReview(review.id)
            } else {
                repository.likeSellerReview(review.id)
            }
            when (result) {
                is ApiResult.Success -> {
                    val echo = result.value
                    _summary.value = _summary.value?.let { summary ->
                        summary.copy(
                            items = summary.items.map { item ->
                                if (item.id != echo.reviewId) {
                                    item
                                } else {
                                    // Backend like_count=0 келсе (fallback echo) — оптимистік дельта.
                                    val newCount = if (echo.likeCount > 0) {
                                        echo.likeCount
                                    } else {
                                        (item.likeCount + if (echo.isLiked) 1 else -1).coerceAtLeast(0)
                                    }
                                    item.copy(isLiked = echo.isLiked, likeCount = newCount)
                                }
                            },
                        )
                    }
                }
                is ApiResult.Error ->
                    _events.tryEmit(ReviewEvent.ShowError(result.failure.toReviewError()))
            }
        }
    }
}

/**
 * «Менің пікірлерім» (spec §10, iOS MyReviewsStore) — клиент жағынан
 * жинау: қаралған жарнамалар + сатып алушы тапсырыстары; әрқайсысы үшін
 * GET /reviews/{id} → өз user_id пікірі бар ма. pendingCount — чат
 * тізіміндегі бейдж.
 */
@HiltViewModel
class MyReviewsViewModel @Inject constructor(
    private val repository: ReviewRepository,
    private val viewedStore: ViewedAnnouncementsStore,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _pending = MutableStateFlow<List<MyReviewCandidate>>(emptyList())
    val pending: StateFlow<List<MyReviewCandidate>> = _pending.asStateFlow()

    private val _done = MutableStateFlow<List<MyReviewCandidate>>(emptyList())
    val done: StateFlow<List<MyReviewCandidate>> = _done.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<Failure?>(null)
    val error: StateFlow<Failure?> = _error.asStateFlow()

    private val _events = MutableSharedFlow<ReviewEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ReviewEvent> = _events.asSharedFlow()

    /** Чат тізіміндегі бейдж (spec §10) — pending саны. */
    val pendingCount: StateFlow<Int> = _pending
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private var currentUserId: Long? = null
    private var lastRefreshAt = 0L

    /** Чат қойындысы ашылғанда шақырылады (бейдж жаңартуы). */
    fun refreshIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastRefreshAt < DEBOUNCE_MS) return
        lastRefreshAt = now
        refresh()
    }

    fun refresh() {
        if (_loading.value) return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            val userId = obtainUserId()
            if (userId == null || userId <= 0) {
                _pending.value = emptyList()
                _done.value = emptyList()
                _loading.value = false
                return@launch
            }
            currentUserId = userId

            // 1) Үміткерлер: тапсырыс жарнамалары + қаралғандар (тапсырыс бірінші).
            val orderCandidates = when (
                val result = repository.getBuyerOrderCandidates()
            ) {
                is ApiResult.Success -> result.value
                is ApiResult.Error -> emptyList() // тапсырыс сұрауы сәтсіз — қаралғандар жалғасады
            }
            val viewedIds = viewedStore.viewedIds.first()
            val byId = LinkedHashMap<Long, MyReviewCandidate>()
            orderCandidates.forEach { byId[it.announcementId] = it }
            viewedIds.forEach { id ->
                if (id > 0) byId.getOrPut(id) { MyReviewCandidate(announcementId = id) }
            }

            // 2) Қаралған үміткерлерге жарнама мәліметтерін тартамыз (атауы жоқтарға).
            val enriched = byId.values.map { candidate ->
                viewModelScope.async {
                    if (candidate.title.isNotBlank()) candidate
                    else when (val info = repository.getAnnouncementInfo(candidate.announcementId)) {
                        is ApiResult.Success -> candidate.copy(
                            title = info.value.title,
                            imageUrl = candidate.imageUrl ?: info.value.imageUrl,
                            price = candidate.price ?: info.value.price,
                            currency = candidate.currency ?: info.value.currency,
                            createdAt = candidate.createdAt ?: info.value.createdAt,
                        )
                        is ApiResult.Error -> candidate
                    }
                }
            }.awaitAll()

            // 3) Әр үміткер үшін өз пікірі бар ма — GET /reviews/{id}.
            val resolved = enriched.map { candidate ->
                viewModelScope.async {
                    when (val result = repository.getReviews(candidate.announcementId)) {
                        is ApiResult.Success -> candidate.copy(
                            ownReview = result.value.firstOrNull { it.userId == userId },
                        )
                        is ApiResult.Error -> candidate
                    }
                }
            }.awaitAll()

            _done.value = resolved.filter { it.hasOwnReview }
            _pending.value = resolved.filterNot { it.hasOwnReview }
            if (resolved.isEmpty() && orderCandidates.isEmpty() && viewedIds.isEmpty()) {
                _error.value = null // бос күй — қате емес
            }
            _loading.value = false
        }
    }

    private suspend fun obtainUserId(): Long? {
        currentUserId?.let { return it }
        return when (val result = profileRepository.getProfile()) {
            is ApiResult.Success -> result.value.id
            is ApiResult.Error -> null
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 30_000L
    }
}