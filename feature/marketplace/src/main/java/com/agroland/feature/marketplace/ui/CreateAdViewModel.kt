package com.agroland.feature.marketplace.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.analytics.MonitoringService
import com.agroland.core.network.ApiResult
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.agroland.feature.marketplace.data.AdDraft
import com.agroland.feature.marketplace.data.DemandDraft
import com.agroland.feature.marketplace.data.DeliveryZone
import com.agroland.feature.marketplace.data.MeasurementUnit
import com.agroland.feature.marketplace.data.MarketplaceRepository
import com.agroland.feature.profile.data.MultipartHelper
import com.agroland.feature.profile.data.ProfileRepository
import com.agroland.feature.profile.data.UserLocation

/**
 * CreateAdViewModel — жарнама жасау/өңдеу формасы (CreateAdNotifier + EditAdNotifier).
 * CreateAdRoute (id=null) — жасау; EditAdRoute(id) — FullAnnouncement-нан prefill.
 */
@HiltViewModel
class CreateAdViewModel @Inject constructor(
    private val repository: MarketplaceRepository,
    private val profileRepository: ProfileRepository,
    private val monitoringService: MonitoringService,
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Edit режимі — null болса жаңа жарнама. */
    val editId: Long? = savedStateHandle["id"]

    sealed interface Event {
        /** Сәтті жіберілді/жаңартылды — id (null болса жауапта id жоқ). */
        data class Submitted(val id: Long?, val updated: Boolean) : Event
        data class ShowError(val error: MarketplaceError) : Event
    }

    private val _draft = MutableStateFlow(AdDraft())
    val draft: StateFlow<AdDraft> = _draft.asStateFlow()

    private val _images = MutableStateFlow<List<Uri>>(emptyList())
    val images: StateFlow<List<Uri>> = _images.asStateFlow()

    private val _video = MutableStateFlow<Uri?>(null)
    val video: StateFlow<Uri?> = _video.asStateFlow()

    private val _loading = MutableStateFlow(editId != null)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _locations = MutableStateFlow<List<UserLocation>>(emptyList())
    val locations: StateFlow<List<UserLocation>> = _locations.asStateFlow()

    private val _deliveryZones = MutableStateFlow<List<DeliveryZone>>(emptyList())
    val deliveryZones: StateFlow<List<DeliveryZone>> = _deliveryZones.asStateFlow()

    /** Бизнес пайдаланушы ма (dealer/business) — SKU/қойма/себет өрістері көрсетіледі. */
    private val _isBusiness = MutableStateFlow(false)
    val isBusiness: StateFlow<Boolean> = _isBusiness.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val result = profileRepository.getProfile()
            if (result is ApiResult.Success) {
                val profile = result.value
                _isBusiness.value = profile.userType == "business" || profile.userType == "dealer"
                _locations.value = profile.locations
                if (editId == null) {
                    _draft.update { current ->
                        current.copy(
                            contactNumbers = listOfNotNull(profile.phone?.takeIf { it.isNotBlank() }),
                            // Backend locations жауабында is_primary жоқ — біріншісын аламыз.
                            userLocationId = profile.locations.firstOrNull()?.id,
                        )
                    }
                }
                if (_isBusiness.value) {
                    val zones = repository.getDeliveryZones()
                    if (zones is ApiResult.Success) _deliveryZones.value = zones.value
                }
            }
        }
        if (editId != null) {
            viewModelScope.launch { loadForEdit(editId!!) }
        }
    }

    /** Edit prefill — EditAdNotifier.setInitialState(FullAnnouncementModel). */
    private suspend fun loadForEdit(id: Long) {
        _loading.value = true
        when (val result = repository.getAnnouncement(id)) {
            is ApiResult.Success -> {
                val detail = result.value
                _draft.value = AdDraft(
                    title = detail.base.title,
                    description = detail.description ?: "",
                    price = detail.base.price?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "",
                    currency = detail.base.currency ?: AdDraft.DEFAULT_CURRENCY,
                    negotiable = detail.base.negotiable,
                    priceIncludesVat = detail.priceIncludesVat,
                    categoryId = detail.categoryId,
                    subcategoryId = detail.subcategoryId,
                    measurementUnit = MeasurementUnit.fromKey(detail.base.measurementUnit),
                    userLocationId = detail.userLocationId ?: _locations.value.firstOrNull()?.id,
                    contactNumbers = detail.contactNumbers,
                    keywords = detail.keywords,
                    deliveryAvailable = detail.base.deliveryAvailable,
                    pickupAvailable = detail.base.pickupAvailable,
                    pickupAddress = detail.base.pickupAddress ?: "",
                    sku = detail.sku ?: "",
                    stockQuantity = detail.stockQuantity?.toString() ?: "",
                    allowCart = detail.base.allowCart,
                    isMarketplace = detail.base.isMarketplace,
                    images = detail.base.imageUrls,
                )
            }
            is ApiResult.Error -> _events.emit(Event.ShowError(result.failure.toMarketplaceError()))
        }
        _loading.value = false
    }

    fun updateDraft(transform: (AdDraft) -> AdDraft) {
        _draft.update(transform)
    }

    fun addImages(uris: List<Uri>) {
        _images.update { current -> (current + uris).distinct() }
    }

    fun removeNewImage(uri: Uri) {
        _images.update { current -> current - uri }
    }

    /** Өңдеу режиміндегі қолданылған (URL) суретті алып тастау. */
    fun removeExistingImage(url: String) {
        _draft.update { it.copy(images = it.images - url) }
    }

    fun setVideo(uri: Uri?) {
        _video.value = uri
    }

    fun setDeliveryZones(ids: List<Long>) {
        _draft.update { it.copy(deliveryZoneIds = ids) }
    }

    /** AI — сипаттама және (келсе) категория ұсынысын толтырады. */
    fun generateAiContent(langCode: String) {
        val title = _draft.value.title.trim()
        if (title.isBlank() || _aiLoading.value) return
        viewModelScope.launch {
            _aiLoading.value = true
            when (val result = repository.generateAdContent(title, langCode)) {
                is ApiResult.Success -> {
                    val content = result.value
                    if (content != null) {
                        _draft.update { current ->
                            current.copy(
                                description = content.description ?: current.description,
                                categoryId = content.categoryId ?: current.categoryId,
                                subcategoryId = content.subcategoryId ?: current.subcategoryId,
                            )
                        }
                    } else {
                        _events.emit(Event.ShowError(MarketplaceError(backendMessage = null)))
                    }
                }
                is ApiResult.Error ->
                    _events.emit(Event.ShowError(result.failure.toMarketplaceError()))
            }
            _aiLoading.value = false
        }
    }

    /** Жарнаманы жіберу — create немесе PATCH. */
    fun submit() {
        val draft = _draft.value
        if (!draft.isValid() || _submitting.value) return
        viewModelScope.launch {
            _submitting.value = true
            val imageParts = _images.value.mapNotNull { uri ->
                MultipartHelper.toPart(appContext, uri, "images")
            }
            val videoPart = _video.value?.let { uri ->
                MultipartHelper.toPart(appContext, uri, "video")
            }
            val result = if (editId == null) {
                repository.createAnnouncement(draft, imageParts, videoPart, skipTariffDialog = true)
            } else {
                repository.updateAnnouncement(editId!!, draft, imageParts, videoPart)
            }
            // Фаза 19 (Flutter CreateAdNotifier parity): тек ЖАСАУ жолы —
            // trackAdvertise('new'); өңдеу оқиға емес.
            if (editId == null) {
                when (result) {
                    is ApiResult.Success -> monitoringService.trackAdvertise(
                        adId = "new",
                        success = true,
                        data = buildMap {
                            draft.categoryId?.let {
                                put("categoryId", kotlinx.serialization.json.JsonPrimitive(it))
                            }
                            draft.subcategoryId?.let {
                                put("subcategoryId", kotlinx.serialization.json.JsonPrimitive(it))
                            }
                        },
                    )
                    is ApiResult.Error -> monitoringService.trackAdvertise(
                        adId = "new",
                        success = false,
                        errorMessage = result.failure.toString(),
                    )
                }
            }
            when (result) {
                is ApiResult.Success ->
                    _events.emit(Event.Submitted(result.value, updated = editId != null))
                is ApiResult.Error ->
                    _events.emit(Event.ShowError(result.failure.toMarketplaceError()))
            }
            _submitting.value = false
        }
    }
}

/** Сұраныс (MakeOffer) — CreateOfferNotifier баламасы. */
@HiltViewModel
class MakeOfferViewModel @Inject constructor(
    private val repository: MarketplaceRepository,
    private val profileRepository: ProfileRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    sealed interface Event {
        data object Submitted : Event
        data class ShowError(val error: MarketplaceError) : Event
    }

    private val _draft = MutableStateFlow(DemandDraft())
    val draft: StateFlow<DemandDraft> = _draft.asStateFlow()

    private val _locations = MutableStateFlow<List<UserLocation>>(emptyList())
    val locations: StateFlow<List<UserLocation>> = _locations.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            (profileRepository.getProfile() as? ApiResult.Success)?.let { result ->
                _locations.value = result.value.locations
                _draft.value = _draft.value.copy(
                    userLocationId = result.value.locations.firstOrNull()?.id,
                )
            }
        }
    }

    fun updateDraft(transform: (DemandDraft) -> DemandDraft) {
        _draft.value = transform(_draft.value)
    }

    fun submit() {
        val draft = _draft.value
        if (draft.validate() != null || _submitting.value) return
        viewModelScope.launch {
            _submitting.value = true
            when (val result = repository.createDemand(draft)) {
                is ApiResult.Success -> _events.emit(Event.Submitted)
                is ApiResult.Error -> _events.emit(Event.ShowError(result.failure.toMarketplaceError()))
            }
            _submitting.value = false
        }
    }
}