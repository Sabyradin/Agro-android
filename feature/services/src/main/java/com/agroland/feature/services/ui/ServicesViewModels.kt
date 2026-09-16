package com.agroland.feature.services.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.core.network.error.Failure
import com.agroland.feature.services.data.EgovMachineryLookup
import com.agroland.feature.services.data.EgovRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI-ға берiлетін қате: backend адам тіліндегі message > generic
 * (спек §4 — шикі error_code ешқашан көрсетілмейді).
 */
data class ServicesError(
    val backendMessage: String? = null,
    val isNetwork: Boolean = false,
)

fun ServicesError.displayText(networkMessage: String, genericMessage: String): String =
    backendMessage ?: if (isNetwork) networkMessage else genericMessage

fun Failure.toServicesError(): ServicesError = when (this) {
    is Failure.Network -> ServicesError(isNetwork = true)
    else -> ServicesError(backendMessage = (this as? Failure.Http)?.error?.message)
}

/** VIN сұрауының UI күйі. */
data class VinLookupState(
    val loading: Boolean = false,
    val result: EgovMachineryLookup? = null,
    val error: ServicesError? = null,
)

/** VIN бойынша техниканы тексеру (EgovServicesPage жоғарғы карточкасы). */
@HiltViewModel
class EgovVinViewModel @Inject constructor(
    private val repository: EgovRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(VinLookupState())
    val state: StateFlow<VinLookupState> = _state.asStateFlow()

    fun lookup(vin: String) {
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.value = VinLookupState(loading = true)
            _state.value = when (val result = repository.lookupMachinery(vin)) {
                is ApiResult.Success ->
                    VinLookupState(result = result.value)
                is ApiResult.Error ->
                    VinLookupState(error = result.failure.toServicesError())
            }
        }
    }

    fun reset() {
        _state.value = VinLookupState()
    }
}