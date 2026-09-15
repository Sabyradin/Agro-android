package com.agroland.feature.location.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.feature.location.data.CatalogLocation
import com.agroland.feature.location.data.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Каталог пикерлерінің ортақ VM-і (LocationSelectionPage, FilterPage, AddressEditPage,
 * CreateAdPage ішіндегі диалогтармен бөліседі). Елдер/облыстар/аудандар кештеледі.
 */
@HiltViewModel
class LocationPickerViewModel @Inject constructor(
    private val repository: LocationRepository,
) : ViewModel() {

    /** Каталог деңгейінің күйі. items бос + error — жүктеу сәтсіз. */
    data class CatalogState(
        val loading: Boolean = false,
        val error: Boolean = false,
        val items: List<CatalogLocation> = emptyList(),
    )

    private val _countries = MutableStateFlow(CatalogState())
    val countries = _countries.asStateFlow()

    private val _regions = MutableStateFlow<Map<Int, CatalogState>>(emptyMap())
    private val _districts = MutableStateFlow<Map<Int, CatalogState>>(emptyMap())

    fun regions(countryId: Int): Flow<CatalogState> = _regions.map { it[countryId] ?: CatalogState() }

    fun districts(regionId: Int): Flow<CatalogState> = _districts.map { it[regionId] ?: CatalogState() }

    /** Елдер тізімі — бірінші шақырыста жүктеледі, кейін кештен. */
    fun ensureCountries() {
        if (_countries.value.loading || _countries.value.items.isNotEmpty() || _countries.value.error) return
        _countries.update { it.copy(loading = true, error = false) }
        viewModelScope.launch {
            when (val result = repository.getCountries()) {
                is com.agroland.core.network.ApiResult.Success ->
                    _countries.update { CatalogState(items = result.value) }
                else -> _countries.update { CatalogState(error = true) }
            }
        }
    }

    fun ensureRegions(countryId: Int) {
        val current = _regions.value[countryId]
        if (current != null && (current.loading || current.items.isNotEmpty() || current.error)) return
        _regions.update { it + (countryId to CatalogState(loading = true)) }
        viewModelScope.launch {
            val state = when (val result = repository.getRegions(countryId)) {
                is com.agroland.core.network.ApiResult.Success -> CatalogState(items = result.value)
                else -> CatalogState(error = true)
            }
            _regions.update { it + (countryId to state) }
        }
    }

    fun ensureDistricts(regionId: Int) {
        val current = _districts.value[regionId]
        if (current != null && (current.loading || current.items.isNotEmpty() || current.error)) return
        _districts.update { it + (regionId to CatalogState(loading = true)) }
        viewModelScope.launch {
            val state = when (val result = repository.getDistricts(regionId = regionId)) {
                is com.agroland.core.network.ApiResult.Success -> CatalogState(items = result.value)
                else -> CatalogState(error = true)
            }
            _districts.update { it + (regionId to state) }
        }
    }

    /** Қатені қайта жүктеу үшін кешті тазарту. */
    fun retryCountries() {
        _countries.value = CatalogState()
        ensureCountries()
    }

    fun retryRegions(countryId: Int) {
        _regions.update { it - countryId }
        ensureRegions(countryId)
    }

    fun retryDistricts(regionId: Int) {
        _districts.update { it - regionId }
        ensureDistricts(regionId)
    }
}