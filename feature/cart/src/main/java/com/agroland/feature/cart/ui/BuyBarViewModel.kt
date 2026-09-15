package com.agroland.feature.cart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.cart.data.CartRepository
import com.agroland.feature.marketplace.data.DeliveryCheckResult
import com.agroland.feature.marketplace.data.FullAnnouncement
import com.agroland.feature.marketplace.data.MarketplaceRepository
import com.agroland.feature.profile.data.ProfileRepository
import com.agroland.feature.profile.data.UserLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** BuySheet күйі (Flutter buy_bottom_sheet). */
data class BuyState(
    val sheetVisible: Boolean = false,
    val quantity: Double = 1.0,
    val checkLoading: Boolean = false,
    val deliveryCheck: DeliveryCheckResult? = null,
    /** true = жеткізу, false = өзі алу (deliveryCheck рұқсат еткенде). */
    val isDelivery: Boolean = true,
    val selectedZoneId: Long? = null,
    val addresses: List<UserLocation> = emptyList(),
    val selectedAddressId: Long? = null,
    val submitting: Boolean = false,
) {
    val selectedAddress: UserLocation?
        get() = addresses.firstOrNull { it.id == selectedAddressId }

    /** Delivery-check аяқталды ма? */
    val checkKnown: Boolean get() = deliveryCheck != null

    /** Жеткізу мүмкін бе? */
    val canDeliver: Boolean get() = deliveryCheck?.canDeliver == true

    /** Өзі алу мүмкін бе? */
    val canPickup: Boolean get() = deliveryCheck?.pickupAvailable == true

    /** Сатып алуға дайын ба? */
    val canSubmit: Boolean
        get() = checkKnown && !submitting && when {
            isDelivery -> canDeliver && selectedAddress?.districtId != null
            else -> canPickup
        }
}

/** Buy sheet бір реттелген оқиғалары. */
sealed interface BuyEvent {
    data class ShowError(val error: CartError) : BuyEvent
    data object AddedToCart : BuyEvent
    data class BuyDone(val orderId: Long) : BuyEvent
}

/**
 * BuyBarViewModel — деталь бетінің төменгі «Себетке»/«Сатып алу» жолағының
 * моделі: delivery-check, buy-now (PATCH pickup_address), себетке қосу.
 */
@HiltViewModel
class BuyBarViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val marketplaceRepository: MarketplaceRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BuyState())
    val state: StateFlow<BuyState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<BuyEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<BuyEvent> = _events

    /** Buy sheet ашылды: профиль мекенжайы + delivery-check жүктеледі. */
    fun openSheet(detail: FullAnnouncement) {
        _state.value = BuyState(
            sheetVisible = true,
            quantity = 1.0,
            selectedZoneId = null,
        )
        viewModelScope.launch {
            var districtId: Int? = null
            when (val profile = profileRepository.getProfile()) {
                is ApiResult.Success -> {
                    val locations = profile.value.locations
                    val selected = locations.firstOrNull { it.districtId != null }
                        ?: locations.firstOrNull()
                    districtId = selected?.districtId
                    _state.update {
                        it.copy(
                            addresses = locations,
                            selectedAddressId = selected?.id,
                        )
                    }
                }
                is ApiResult.Error -> _events.emit(BuyEvent.ShowError(profile.failure.toCartError()))
            }
            loadDeliveryCheck(detail.base.id, districtId)
        }
    }

    private suspend fun loadDeliveryCheck(announcementId: Long, districtId: Int?) {
        _state.update { it.copy(checkLoading = true) }
        when (val result = marketplaceRepository.deliveryCheck(announcementId, districtId ?: 0)) {
            is ApiResult.Success -> _state.update {
                it.copy(
                    checkLoading = false,
                    deliveryCheck = result.value,
                    isDelivery = result.value.canDeliver || !result.value.pickupAvailable,
                    // Delivery-check zone — әдепкі таңдау.
                    selectedZoneId = result.value.zone?.id,
                )
            }
            is ApiResult.Error -> {
                // Delivery-check қатесі — sheet жабылмайды, режим таңдалмайды.
                _state.update { it.copy(checkLoading = false) }
                _events.emit(BuyEvent.ShowError(result.failure.toCartError()))
            }
        }
    }

    fun closeSheet() {
        _state.update { it.copy(sheetVisible = false) }
    }

    fun changeQuantity(delta: Double) {
        _state.update { it.copy(quantity = (it.quantity + delta).coerceAtLeast(1.0)) }
    }

    /** Режим ауыстыру — delivery-check рұқсат еткен ғана. */
    fun setDeliveryMode(isDelivery: Boolean) {
        _state.update { state ->
            if (isDelivery && !state.canDeliver) return@update state
            if (!isDelivery && !state.canPickup) return@update state
            state.copy(isDelivery = isDelivery)
        }
    }

    fun selectAddress(id: Long) {
        _state.update { it.copy(selectedAddressId = id) }
    }

    fun selectZone(zoneId: Long) {
        _state.update { it.copy(selectedZoneId = zoneId) }
    }

    /** «Сатып алу» — buy-now → (pickup болса) PATCH pickup_address → тапсырыс. */
    fun buyNow(detail: FullAnnouncement) {
        val state = _state.value
        if (!state.canSubmit || state.submitting) return
        val address = state.selectedAddress
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            val result = if (state.isDelivery) {
                cartRepository.buyNow(
                    announcementId = detail.base.id,
                    quantity = state.quantity,
                    measurementUnit = detail.base.measurementUnit,
                    deliveryAddress = address?.fullAddress,
                    deliveryZoneId = state.selectedZoneId,
                )
            } else {
                cartRepository.buyNow(
                    announcementId = detail.base.id,
                    quantity = state.quantity,
                    measurementUnit = detail.base.measurementUnit,
                    pickupAddress = state.deliveryCheck?.pickupAddress
                        ?: detail.base.pickupAddress,
                )
            }
            _state.update { it.copy(submitting = false) }
            when (result) {
                is ApiResult.Success -> {
                    _state.update { it.copy(sheetVisible = false) }
                    _events.emit(BuyEvent.BuyDone(result.value.id))
                }
                is ApiResult.Error ->
                    _events.emit(BuyEvent.ShowError(result.failure.toCartError()))
            }
        }
    }

    /** «Себетке» — 1 дана (Flutter addToCart qty=1). */
    fun addToCart(detail: FullAnnouncement) {
        viewModelScope.launch {
            when (
                val result = cartRepository.addToCart(
                    detail.base.id,
                    1.0,
                    detail.base.measurementUnit,
                )
            ) {
                is ApiResult.Success -> _events.emit(BuyEvent.AddedToCart)
                is ApiResult.Error ->
                    _events.emit(BuyEvent.ShowError(result.failure.toCartError()))
            }
        }
    }
}