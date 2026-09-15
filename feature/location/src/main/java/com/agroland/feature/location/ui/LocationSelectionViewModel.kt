package com.agroland.feature.location.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.location.data.LocationRepository
import com.agroland.feature.location.data.ReverseGeocodeResult
import com.agroland.feature.location.data.SelectedLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Карта/каталог арқылы локация таңдау күйі (LocationSelectionPage).
 * Reverse geocode — backend Nominatim проксисі (GET /location/reverse?lat=&lng=),
 * каталог сәйкестігі болмаса қолмен таңдау қолжетімді (spec: MUST keep catalogue IDs).
 */
@HiltViewModel
class LocationSelectionViewModel @Inject constructor(
    private val repository: LocationRepository,
) : ViewModel() {

    data class State(
        val selection: SelectedLocation = SelectedLocation(),
        /** Пин қойылды (карта/гпс) — reverse жүгіріп жатыр немесе аяқталды. */
        val pinPlaced: Boolean = false,
        val reverseLoading: Boolean = false,
        /** Reverse жүктелмеді — қолмен таңдау керек. */
        val reverseFailed: Boolean = false,
        /** GPS бұрышы — LocationServices орнын күтудеміз. */
        val locating: Boolean = false,
        val gpsFailed: Boolean = false,
    ) {
        val canConfirm: Boolean get() = selection.isNotEmpty
    }

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    fun resetWith(prefill: SelectedLocation?) {
        if (prefill == null) return
        if (_state.value.selection == prefill) return
        _state.value = State(
            selection = prefill,
            pinPlaced = prefill.latitude != null && prefill.longitude != null,
        )
    }

    /** Картаға пин қойылды → reverse geocode іске қосылады (10 с шегі — spec). */
    fun onPinPlaced(latitude: Double, longitude: Double) {
        _state.value = _state.value.copy(
            pinPlaced = true,
            reverseLoading = true,
            reverseFailed = false,
            selection = _state.value.selection.copy(latitude = latitude, longitude = longitude),
        )
        viewModelScope.launch {
            val result = try {
                withTimeout(REVERSE_TIMEOUT_MS) { repository.reverseGeocode(latitude, longitude) }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                null
            }
            when {
                result is ApiResult.Success && result.value != null && result.value!!.hasCatalogMatch -> {
                    val rev = result.value!!
                    _state.value = _state.value.copy(
                        reverseLoading = false,
                        reverseFailed = false,
                        selection = _state.value.selection.copy(
                            countryId = rev.countryId ?: _state.value.selection.countryId,
                            countryName = rev.countryName ?: _state.value.selection.countryName,
                            regionId = rev.regionId ?: _state.value.selection.regionId,
                            regionName = rev.regionName ?: _state.value.selection.regionName,
                            districtId = rev.districtId ?: _state.value.selection.districtId,
                            districtName = rev.districtName ?: _state.value.selection.districtName,
                        ),
                    )
                }
                else -> _state.value = _state.value.copy(
                    reverseLoading = false,
                    reverseFailed = true,
                )
            }
        }
    }

    fun onGpsFix(latitude: Double, longitude: Double) {
        _state.value = _state.value.copy(locating = false, gpsFailed = false)
        onPinPlaced(latitude, longitude)
    }

    fun onGpsFailed() {
        _state.value = _state.value.copy(locating = false, gpsFailed = true)
    }

    fun onGpsStarted() {
        _state.value = _state.value.copy(locating = true, gpsFailed = false)
    }

    /** Каталогтан қолмен таңдау (reverse сәтсіз/қажет емес жағдайда). */
    fun selectCountry(id: Int?, name: String?) {
        _state.value = _state.value.copy(
            selection = _state.value.selection.copy(
                countryId = id,
                countryName = name,
                regionId = null,
                regionName = null,
                districtId = null,
                districtName = null,
            ),
        )
    }

    fun selectRegion(id: Int?, name: String?) {
        _state.value = _state.value.copy(
            selection = _state.value.selection.copy(
                regionId = id,
                regionName = name,
                districtId = null,
                districtName = null,
            ),
        )
    }

    fun selectDistrict(id: Int?, name: String?) {
        _state.value = _state.value.copy(
            selection = _state.value.selection.copy(districtId = id, districtName = name),
        )
    }

    private companion object {
        const val REVERSE_TIMEOUT_MS = 10_000L
    }
}