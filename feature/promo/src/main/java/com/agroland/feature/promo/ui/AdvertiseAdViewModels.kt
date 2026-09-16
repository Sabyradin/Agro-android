package com.agroland.feature.promo.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.core.network.error.Failure
import com.agroland.feature.profile.data.MultipartHelper
import com.agroland.feature.profile.data.ProfileRepository
import com.agroland.feature.promo.data.PromoActivateResult
import com.agroland.feature.promo.data.PromoCatalogItem
import com.agroland.feature.promo.data.PromoRepository
import com.agroland.feature.promo.data.Promotion
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * AdvertiseAd беті (Flutter AdvertiseAdPage + PromoCatalogWidget):
 * каталог + профиль балансы + мұрағат промо + белсендіру оркестрациясы.
 * Қателер событиелер арқылы UI-ға — коды бойынша диспетч
 * (INSUFFICIENT_BALANCE → диалог, жұмсақ кодтар → toast).
 */
data class AdvertiseAdState(
    val catalog: List<PromoCatalogItem> = emptyList(),
    val catalogLoading: Boolean = true,
    val balance: Double? = null,
    val isVipSeller: Boolean = false,
    val promotion: Promotion? = null,
    val promotionLoading: Boolean = false,
    val activatingSku: String? = null,
    /** Banner SKU: сурет жүктелгеннен кейінгі image_url. */
    val uploadedImageUrl: String? = null,
    val uploadingImage: Boolean = false,
    /** Жарнама превьюсы (Flutter AdvertiseAdPage → ProfileAnnouncementItemView). */
    val announcement: com.agroland.feature.marketplace.data.Announcement? = null,
)

sealed interface PromoEvent {
    /** Белсендіру сәтті — promo_activated диалогы + баланс/промо refresh. */
    data object Activated : PromoEvent

    /** Баланс жетпейді — диалог: promo_need_to_top_up(missing) + «Толтыру». */
    data class InsufficientBalance(val missingAmount: Double) : PromoEvent

    /** Жұмсақ валидация (BANNER_SLOT_ALREADY_ACTIVE/NOT_YOUR_ANNOUNCEMENT/...) — toast. */
    data class SoftError(val message: String?) : PromoEvent

    /** Қалған қателер — қате диалогы (backend хабарламасы не generic). */
    data class Error(val failure: Failure) : PromoEvent

    /** Баннер суреті жүктелмеді — toast. */
    data object BannerUploadFailed : PromoEvent
}

/** Қате — жұмсақ валидация кодтары тізімі (Flutter _handleFailure). */
private val SOFT_ERROR_CODES = setOf(
    "BANNER_SLOT_ALREADY_ACTIVE",
    "NOT_YOUR_ANNOUNCEMENT",
    "BANNER_SLOT_NOT_PENDING",
    "IMAGE_URL_REQUIRED",
    "INVALID_DURATION",
)

@HiltViewModel
class AdvertiseAdViewModel @Inject constructor(
    private val promoRepository: PromoRepository,
    private val profileRepository: ProfileRepository,
    private val marketplaceRepository: com.agroland.feature.marketplace.data.MarketplaceRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(AdvertiseAdState())
    val state: StateFlow<AdvertiseAdState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PromoEvent>()
    val events: SharedFlow<PromoEvent> = _events.asSharedFlow()

    fun refresh(announcementId: Long?) {
        loadCatalog()
        loadProfile()
        loadPromotion(announcementId)
        loadAnnouncement(announcementId)
    }

    /** Жарнама превьюсы — GET /announcement/{id} (қате silent: карточка жоқ болады). */
    fun loadAnnouncement(announcementId: Long?) {
        if (announcementId == null) return
        viewModelScope.launch {
            when (val result = marketplaceRepository.getAnnouncement(announcementId)) {
                is ApiResult.Success ->
                    _state.update { it.copy(announcement = result.value.base) }

                is ApiResult.Error -> Unit
            }
        }
    }

    fun loadCatalog() {
        viewModelScope.launch {
            when (val result = promoRepository.getCatalog()) {
                is ApiResult.Success ->
                    _state.update { it.copy(catalog = result.value, catalogLoading = false) }

                is ApiResult.Error -> {
                    _state.update { it.copy(catalogLoading = false) }
                    _events.emit(PromoEvent.Error(result.failure))
                }
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            when (val result = profileRepository.getProfile()) {
                is ApiResult.Success ->
                    _state.update {
                        it.copy(
                            balance = result.value.balance,
                            isVipSeller = result.value.isVipSeller,
                        )
                    }

                is ApiResult.Error -> Unit // Баланс карта — қате silent, кейін retry
            }
        }
    }

    fun loadPromotion(announcementId: Long?) {
        if (announcementId == null) return
        latestAnnouncementId = announcementId
        viewModelScope.launch {
            _state.update { it.copy(promotionLoading = true) }
            when (val result = promoRepository.getAnnouncementPromotion(announcementId)) {
                is ApiResult.Success ->
                    _state.update {
                        it.copy(promotion = result.value, promotionLoading = false)
                    }

                is ApiResult.Error ->
                    _state.update { it.copy(promotionLoading = false) }
            }
        }
    }

    /**
     * Промо белсендіру. [bannerBody] — banner SKU үшін форма мәндері
     * (image_url/cta_url/title/announcement_id).
     */
    fun activate(item: PromoCatalogItem, announcementId: Long?, bannerBody: Map<String, Any?>) {
        if (_state.value.activatingSku != null) return
        viewModelScope.launch {
            _state.update { it.copy(activatingSku = item.sku) }
            val result = promoRepository.activate(item, announcementId, bannerBody)
            _state.update { it.copy(activatingSku = null) }
            when (result) {
                is ApiResult.Success -> {
                    // Баланс + промо + жарнама күйін жаңартамыз (Flutter сәтті
                    // кейін getUser + getPromotion; boost/vip чиптері де жаңарады).
                    loadProfile()
                    loadPromotion(announcementId)
                    loadAnnouncement(announcementId)
                    _events.emit(PromoEvent.Activated)
                }

                is ApiResult.Error -> {
                    val failure = result.failure
                    when {
                        PromoRepository.errorCodeOf(failure) == "INSUFFICIENT_BALANCE" ->
                            _events.emit(
                                PromoEvent.InsufficientBalance(
                                    PromoRepository.missingAmountOf(failure),
                                ),
                            )

                        PromoRepository.errorCodeOf(failure)?.let { it in SOFT_ERROR_CODES } == true ->
                            _events.emit(PromoEvent.SoftError(PromoRepository.backendMessageOf(failure)))

                        else -> _events.emit(PromoEvent.Error(failure))
                    }
                }
            }
        }
    }

    /** Баннер суретін таңдау → POST /banners/main/image → image_url. */
    fun uploadBannerImage(uri: android.net.Uri) {
        if (_state.value.uploadingImage) return
        viewModelScope.launch {
            _state.update { it.copy(uploadingImage = true) }
            val part = MultipartHelper.toPart(appContext, uri, "file")
            if (part == null) {
                _state.update { it.copy(uploadingImage = false) }
                _events.emit(PromoEvent.BannerUploadFailed)
                return@launch
            }
            when (val result = promoRepository.uploadBannerImage(part)) {
                is ApiResult.Success ->
                    _state.update { current ->
                        current.copy(uploadedImageUrl = result.value, uploadingImage = false)
                    }

                is ApiResult.Error -> {
                    _state.update { it.copy(uploadingImage = false) }
                    _events.emit(PromoEvent.BannerUploadFailed)
                }
            }
        }
    }

    /** Басқа сурет таңдау алдында ескі URL тазаланады. */
    fun clearUploadedImage() {
        _state.update { it.copy(uploadedImageUrl = null) }
    }

    /**
     * PATCH /promotion/{id}/auto-renewal — оптимистік жаңарту (Flutter
     * _AutoRenewalToggle): жергілікті күй бірден ауысады, қате болса
     * промо серверден қайта оқылады.
     */
    fun toggleAutoRenewal(promotionId: Long, enabled: Boolean) {
        val current = _state.value.promotion ?: return
        if (current.autoRenewal == enabled) return
        _state.update { state ->
            state.copy(promotion = state.promotion?.copy(autoRenewal = enabled))
        }
        viewModelScope.launch {
            when (promoRepository.toggleAutoRenewal(promotionId, enabled)) {
                is ApiResult.Error -> loadPromotion(latestAnnouncementId)
                is ApiResult.Success -> Unit
            }
        }
    }

    /** loadPromotion шақырылған соңғы жарнама id-і (қатеден кейінгі refresh үшін). */
    private var latestAnnouncementId: Long? = null
}

/**
 * PromotedAnnouncementsPage (Flutter UserAnnouncementsPromotionsNotifier):
 * өз жарнамалары + промо карточкалары.
 */
data class PromotionsState(
    val items: List<com.agroland.feature.promo.data.AnnouncementPromotion> = emptyList(),
    val loading: Boolean = true,
    val error: Boolean = false,
)

@HiltViewModel
class PromotedAnnouncementsViewModel @Inject constructor(
    private val promoRepository: PromoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PromotionsState())
    val state: StateFlow<PromotionsState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PromoEvent>()
    val events: SharedFlow<PromoEvent> = _events.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = false) }
            when (val result = promoRepository.getUserAnnouncementsPromotions()) {
                is ApiResult.Success ->
                    _state.update { it.copy(items = result.value, loading = false) }

                is ApiResult.Error ->
                    _state.update { it.copy(loading = false, error = true) }
            }
        }
    }
}