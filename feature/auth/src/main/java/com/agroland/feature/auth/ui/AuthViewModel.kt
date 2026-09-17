package com.agroland.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.analytics.MonitoringService
import com.agroland.core.network.ApiResult
import com.agroland.core.network.auth.TokenStore
import com.agroland.core.network.error.Failure
import com.agroland.feature.auth.data.AuthRepository
import com.agroland.feature.auth.data.LoginStart
import com.agroland.feature.auth.session.SessionController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Auth экрандарының біртұтас күйі: телефон → OTP → (қажет болса) тіркелу.
 * Экранаралық навигация AuthUiState арқылы.
 */
sealed interface AuthUiState {
    data object PhoneEntry : AuthUiState
    data class OtpEntry(val taskId: String, val phone: String, val maskedEmail: String?) : AuthUiState
    data class Register(val phone: String) : AuthUiState
    data object Done : AuthUiState
}

/** Қате хабарламасы: backend адам тіліндегі message, болмаса локализацияланған generic. */
data class AuthError(
    val backendMessage: String? = null,
    val isUserNotFound: Boolean = false,
    val isNetwork: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val sessionController: SessionController,
    private val tokenStore: TokenStore,
    private val monitoringService: MonitoringService,
    private val biometricAuthenticator: com.agroland.feature.auth.security.BiometricAuthenticator,
    val pinManager: com.agroland.feature.auth.security.PinManager,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.PhoneEntry)
    val state: StateFlow<AuthUiState> = _state

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<AuthError?>(null)
    val error: StateFlow<AuthError?> = _error

    private val _emailSent = MutableStateFlow(false)
    val emailSent: StateFlow<Boolean> = _emailSent

    var isRegisterFlow: Boolean = false
        private set

    val hasBiometricToken: Boolean get() = tokenStore.biometricToken.value != null

    /** Биометрия қолжетімді ме (экранда түймені көрсету/жасыру). */
    fun isBiometricAvailable(activity: androidx.fragment.app.FragmentActivity): Boolean =
        biometricAuthenticator.isAvailable(activity)

    /** Prompt көрсетіп, сәтті болса биометриялық token-мен кіреді (best-effort). */
    fun showBiometricPrompt(
        activity: androidx.fragment.app.FragmentActivity,
        title: String,
        negativeButtonText: String,
    ) {
        biometricAuthenticator.authenticate(
            activity = activity,
            title = title,
            negativeButtonText = negativeButtonText,
            onSuccess = { loginWithBiometrics() },
            onError = { _error.value = AuthError() },
        )
    }

    fun consumeError() {
        _error.value = null
    }

    /** 1-қадам: телефон → login (пайдаланушы жоқ болса — register экранына). */
    fun startLogin(phone: String) {
        launchFlow {
            when (val result = repository.startLogin(phone)) {
                is ApiResult.Success -> handleLoginStart(result.value, phone)
                is ApiResult.Error -> _error.value = result.toAuthError()
            }
        }
    }

    /** OTP экранына кірер алдында SMS жіберу. */
    fun requestSms() {
        val otp = _state.value as? AuthUiState.OtpEntry ?: return
        launchFlow { repository.requestSms(otp.taskId) }
    }

    fun resendSms() {
        val otp = _state.value as? AuthUiState.OtpEntry ?: return
        launchFlow { repository.requestSms(otp.taskId) }
    }

    fun sendCodeByEmail() {
        val otp = _state.value as? AuthUiState.OtpEntry ?: return
        launchFlow {
            when (val result = repository.sendCodeByEmail(otp.taskId)) {
                is ApiResult.Success -> _emailSent.value = true
                is ApiResult.Error -> _error.value = result.toAuthError()
            }
        }
    }

    /** OTP растау → токендер → Done. */
    fun confirmOtp(code: String) {
        val otp = _state.value as? AuthUiState.OtpEntry ?: return
        launchFlow {
            val result = if (isRegisterFlow) {
                repository.confirmOtpAndSignup(otp.taskId, code)
            } else {
                repository.confirmOtpAndLogin(otp.taskId, code)
            }
            when (result) {
                is ApiResult.Success -> {
                    // Фаза 19 (Flutter SignInNotifier.submitCode parity):
                    // тіркелу/кіру сәтті — мониторинг + TikTok conversion.
                    if (isRegisterFlow) {
                        monitoringService.trackRegistration(otp.phone, success = true)
                    } else {
                        monitoringService.trackLogin(otp.phone, success = true)
                    }
                    sessionController.onLoggedIn()
                    _state.value = AuthUiState.Done
                }
                is ApiResult.Error -> {
                    val authError = result.toAuthError()
                    if (isRegisterFlow) {
                        monitoringService.trackRegistration(
                            otp.phone,
                            success = false,
                            errorMessage = authError.backendMessage,
                        )
                    } else {
                        monitoringService.trackLogin(
                            otp.phone,
                            success = false,
                            errorMessage = authError.backendMessage,
                        )
                    }
                    _error.value = authError
                }
            }
        }
    }

    /** Тіркелу: деректер жіберіледі → OTP экраны. */
    fun startRegister(phone: String, name: String, userType: String, companyBin: String?, email: String?) {
        launchFlow {
            val result = repository.startSignup(phone, name, userType, companyBin, email)
            when (result) {
                is ApiResult.Success -> {
                    isRegisterFlow = true
                    handleLoginStart(result.value, phone)
                }
                is ApiResult.Error -> _error.value = result.toAuthError()
            }
        }
    }

    /** Соңғы сессиядағы биометрия token-мен кіру (best-effort). */
    fun loginWithBiometrics() {
        val token = tokenStore.biometricToken.value ?: return
        launchFlow {
            when (val result = repository.loginWithBiometricToken(token)) {
                is ApiResult.Success -> {
                    sessionController.onLoggedIn()
                    _state.value = AuthUiState.Done
                }
                is ApiResult.Error -> _error.value = result.toAuthError()
            }
        }
    }

    fun resetToPhoneEntry() {
        isRegisterFlow = false
        _state.value = AuthUiState.PhoneEntry
    }

    /** Login экранынан тіркелуге өту. */
    fun openRegister(phone: String) {
        _state.value = AuthUiState.Register(phone)
    }

    private fun handleLoginStart(start: LoginStart, phone: String) {
        val taskId = start.taskId
        if (taskId == null) {
            _error.value = AuthError()
            return
        }
        _state.value = AuthUiState.OtpEntry(taskId, phone, start.maskedEmail)
        // MFA: SMS listener-ден БҰРЫН жібереміз.
        launchFlow { repository.requestSms(taskId) }
    }

    private fun launchFlow(block: suspend () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            try {
                block()
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Backend хабарламасын UI-ға шығаруға жарамдылығын тексереді: серверде
     * көп жауап ағылшынша техникалық мәтін («User not found», «Invalid code»)
     * — олар пайдаланушыға көрсетілмейді, орнына локализацияланған мәтін
     * шығады (MASTER_PLAN тығыздық-ережесі №2).
     */
    private fun String?.localizedOrNull(): String? =
        this?.takeIf { text -> text.any { it.code > 0x7F } }

    private fun ApiResult.Error.toAuthError(): AuthError {
        val httpFailure = failure as? Failure.Http
        return when (failure) {
            is Failure.Network -> AuthError(isNetwork = true)
            is Failure.Http -> when (httpFailure?.error?.code) {
                "USER_NOT_FOUND", "user_not_found" ->
                    // Мәтін локализацияланған: backend «User not found» деп
                    // ағылшынша қайтарады (MASTER_PLAN тығыздық-ережесі №2).
                    AuthError(isUserNotFound = true)
                else -> AuthError(backendMessage = httpFailure?.error?.message.localizedOrNull())
            }
            else -> AuthError()
        }
    }
}