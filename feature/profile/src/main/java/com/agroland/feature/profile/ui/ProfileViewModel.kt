package com.agroland.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.auth.security.PinManager
import com.agroland.feature.auth.session.SessionController
import com.agroland.feature.profile.data.ProfileRepository
import com.agroland.feature.profile.data.UserLocation
import com.agroland.feature.location.data.SelectedLocation
import com.agroland.feature.profile.data.UserProfile
import com.agroland.feature.profile.data.VerificationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

/**
 * Профиль ағынының бір реттелген оқиғалары — әр экран өз кезегінде collector арқылы алады.
 * Saved — артқа қайту/refresh; ShowError — snackbar/қате жолы.
 */
sealed interface ProfileEvent {
    data object Saved : ProfileEvent
    data object TermsAccepted : ProfileEvent
    data object VerificationSubmitted : ProfileEvent
    data object AvatarUploaded : ProfileEvent
    data object LoggedOut : ProfileEvent
    data object LocationDeleted : ProfileEvent
    data class ShowError(val error: ProfileError) : ProfileEvent
}

/**
 * Профиль/компания/KYC экрандарының ортақ ViewModel-і (activity scope).
 * Хаб + барлық саб-беттер бір дереккөзді көреді: бір fetch, бірдей күй.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val sessionController: SessionController,
    private val biometricAuthenticator: com.agroland.feature.auth.security.BiometricAuthenticator,
    val pinManager: PinManager,
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    private val _verification = MutableStateFlow<VerificationStatus?>(null)
    val verification: StateFlow<VerificationStatus?> = _verification

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    private val _events = MutableSharedFlow<ProfileEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<ProfileEvent> = _events

    /** Дилер/Бизнес бөлімдері көріне ме (UI сөзі — «Бизнес», API мәні dealer|business). */
    val isDealer: Boolean
        get() = _profile.value?.userType.equals("dealer", ignoreCase = true) ||
            _profile.value?.userType.equals("business", ignoreCase = true)

    init {
        if (sessionController.state.value == com.agroland.feature.auth.session.SessionState.Authorized) {
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            when (val result = repository.getProfile()) {
                is ApiResult.Success -> _profile.value = result.value
                is ApiResult.Error -> _events.emit(ProfileEvent.ShowError(result.failure.toProfileError()))
            }
            _loading.value = false
        }
    }

    /** Additive PATCH — тек өзгерген өрістер жіберіледі; email "" → NULL (тазарту). */
    fun updateProfileFields(originalName: String?, originalEmail: String?, newName: String, newEmail: String) {
        val name = if (newName.trim() != (originalName ?: "")) newName.trim() else null
        val email = if (newEmail.trim() != (originalEmail ?: "")) newEmail.trim() else null
        if (name == null && email == null) {
            viewModelScope.launch { _events.emit(ProfileEvent.Saved) }
            return
        }
        viewModelScope.launch {
            _saving.value = true
            when (val result = repository.updateProfile(name, email)) {
                is ApiResult.Success -> {
                    _profile.value = result.value
                    _events.emit(ProfileEvent.Saved)
                }
                is ApiResult.Error -> _events.emit(ProfileEvent.ShowError(result.failure.toProfileError()))
            }
            _saving.value = false
        }
    }

    /** Avatar multipart — URI→Part түрлендіруі UI-да (MultipartHelper). */
    fun uploadAvatar(part: MultipartBody.Part) {
        viewModelScope.launch {
            _saving.value = true
            when (val result = repository.uploadAvatar(part)) {
                is ApiResult.Success -> {
                    refresh()
                    _events.emit(ProfileEvent.AvatarUploaded)
                }
                is ApiResult.Error -> _events.emit(ProfileEvent.ShowError(result.failure.toProfileError()))
            }
            _saving.value = false
        }
    }

    // Мекенжайлар.

    /**
     * Мекенжайды сақтау. Catalog міндетті (country+region+district — Flutter
     * _buildLocationModel сияқты); координаттар жаңа карта таңдауынан, болмаса
     * бұрынғы мекенжайдан, болмаса 0.0 (backend district_id-ды өздігінен
     * турындатады). Өңдеу = POST жаңа + DELETE ескі (PATCH жоқ).
     * Сақтағаннан кейін 3-тен асқаны өшіріледі (Flutter maxAddresses = 3).
     */
    fun saveLocation(
        existing: UserLocation?,
        street: String,
        house: String,
        catalog: SelectedLocation?,
    ) {
        viewModelScope.launch {
            _saving.value = true
            val latitude = catalog?.latitude ?: existing?.latitude
            val longitude = catalog?.longitude ?: existing?.longitude
            val result = if (existing == null) {
                repository.createLocation(street, house, catalog, latitude, longitude)
            } else {
                repository.replaceLocation(existing.id, street, house, catalog, latitude, longitude)
            }
            when (result) {
                is ApiResult.Success -> {
                    refresh()
                    trimExcessAddresses()
                    _events.emit(ProfileEvent.Saved)
                }
                is ApiResult.Error -> _events.emit(ProfileEvent.ShowError(result.failure.toProfileError()))
            }
            _saving.value = false
        }
    }

    /** 3-тен асқан мекенжайдан ең ескісін (кіші user_location_id) өшіреді. */
    private suspend fun trimExcessAddresses() {
        val locations = _profile.value?.locations ?: return
        if (locations.size <= MAX_ADDRESSES) return
        locations.sortedBy { it.id }
            .dropLast(MAX_ADDRESSES)
            .forEach { repository.deleteLocation(it.id) }
        refresh()
    }

    fun deleteLocation(id: Long) {
        viewModelScope.launch {
            _saving.value = true
            when (val result = repository.deleteLocation(id)) {
                is ApiResult.Success -> {
                    refresh()
                    _events.emit(ProfileEvent.LocationDeleted)
                }
                is ApiResult.Error -> _events.emit(ProfileEvent.ShowError(result.failure.toProfileError()))
            }
            _saving.value = false
        }
    }

    // Компания саб-ресурстары.

    fun saveCompanyRepresentative(name: String, position: String) {
        viewModelScope.launch {
            _saving.value = true
            when (val result = repository.updateCompanyRepresentative(name, position)) {
                is ApiResult.Success -> {
                    refresh()
                    _events.emit(ProfileEvent.Saved)
                }
                is ApiResult.Error -> _events.emit(ProfileEvent.ShowError(result.failure.toProfileError()))
            }
            _saving.value = false
        }
    }

    fun saveCompanyAbout(text: String) {
        viewModelScope.launch {
            _saving.value = true
            when (val result = repository.updateCompanyAbout(text)) {
                is ApiResult.Success -> {
                    refresh()
                    _events.emit(ProfileEvent.Saved)
                }
                is ApiResult.Error -> _events.emit(ProfileEvent.ShowError(result.failure.toProfileError()))
            }
            _saving.value = false
        }
    }

    fun saveCompanyContacts(phone: String?, website: String?, telegram: String?, whatsapp: String?) {
        viewModelScope.launch {
            _saving.value = true
            when (
                val result = repository.updateCompanyContacts(
                    phone?.trim()?.takeIf { it.isNotEmpty() },
                    website?.trim()?.takeIf { it.isNotEmpty() },
                    telegram?.trim()?.takeIf { it.isNotEmpty() },
                    whatsapp?.trim()?.takeIf { it.isNotEmpty() },
                )
            ) {
                is ApiResult.Success -> {
                    refresh()
                    _events.emit(ProfileEvent.Saved)
                }
                is ApiResult.Error -> _events.emit(ProfileEvent.ShowError(result.failure.toProfileError()))
            }
            _saving.value = false
        }
    }

    fun saveCompanyDecor(logo: MultipartBody.Part?, banner: MultipartBody.Part?) {
        viewModelScope.launch {
            if (logo == null && banner == null) {
                _events.emit(ProfileEvent.Saved)
                return@launch
            }
            _saving.value = true
            when (val result = repository.updateCompanyDecor(logo, banner)) {
                is ApiResult.Success -> {
                    refresh()
                    _events.emit(ProfileEvent.Saved)
                }
                is ApiResult.Error -> _events.emit(ProfileEvent.ShowError(result.failure.toProfileError()))
            }
            _saving.value = false
        }
    }

    // Бизнес шарттар (DealerTerms гейт).

    fun acceptBusinessTerms() {
        viewModelScope.launch {
            _saving.value = true
            when (val result = repository.acceptBusinessTerms()) {
                is ApiResult.Success -> {
                    refresh()
                    _events.emit(ProfileEvent.TermsAccepted)
                }
                is ApiResult.Error -> _events.emit(ProfileEvent.ShowError(result.failure.toProfileError()))
            }
            _saving.value = false
        }
    }

    // KYC верификация.

    fun loadVerificationStatus() {
        viewModelScope.launch {
            when (val result = repository.getVerificationStatus()) {
                is ApiResult.Success -> _verification.value = result.value
                is ApiResult.Error -> _events.emit(ProfileEvent.ShowError(result.failure.toProfileError()))
            }
        }
    }

    fun submitVerification(fullName: String?, iin: String?, documents: List<MultipartBody.Part>) {
        viewModelScope.launch {
            _saving.value = true
            when (val result = repository.submitVerification(fullName, iin, documents)) {
                is ApiResult.Success -> {
                    _verification.value = result.value
                    _events.emit(ProfileEvent.VerificationSubmitted)
                }
                is ApiResult.Error -> _events.emit(ProfileEvent.ShowError(result.failure.toProfileError()))
            }
            _saving.value = false
        }
    }

    // App-lock (биометрия) және шығу.

    fun setBiometricLockEnabled(enabled: Boolean) {
        pinManager.biometricLockEnabled = enabled
    }

    fun isBiometricAvailable(activity: androidx.fragment.app.FragmentActivity): Boolean =
        biometricAuthenticator.isAvailable(activity)

    fun logout() {
        viewModelScope.launch {
            sessionController.onLoggedOut()
            _events.emit(ProfileEvent.LoggedOut)
        }
    }

    private companion object {
        /** Flutter ProfileAddressNotifier.maxAddresses. */
        const val MAX_ADDRESSES = 3
    }
}