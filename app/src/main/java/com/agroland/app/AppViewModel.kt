package com.agroland.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.analytics.MonitoringService
import com.agroland.core.network.ApiResult
import com.agroland.feature.auth.security.BiometricAuthenticator
import com.agroland.feature.auth.security.PinManager
import com.agroland.feature.auth.session.SessionController
import com.agroland.feature.auth.session.SessionState
import com.agroland.feature.payment.data.PendingPaymentStore
import com.agroland.feature.profile.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** App деңгейіндегі күй: сессия + app-lock + үздік Halyk төлемі (Фаза 9). */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionController: SessionController,
    val pinManager: PinManager,
    val biometricAuthenticator: BiometricAuthenticator,
    private val pendingPaymentStore: PendingPaymentStore,
    private val profileRepository: ProfileRepository,
    private val monitoringService: MonitoringService,
) : ViewModel() {

    val session: StateFlow<SessionState> = sessionController.state

    val isAuthorized: Boolean
        get() = sessionController.state.value == SessionState.Authorized

    val pinIsSet: Boolean
        get() = pinManager.isPinSet

    private var analyticsBound = false

    /**
     * Басты беттегі аватар. [bindUserAnalytics] профильді бәрібір оқиды —
     * қосымша сұрау жасамай, сол нәтижеден аламыз.
     */
    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    fun logout() {
        sessionController.onLoggedOut()
    }

    /**
     * Фаза 19 (Flutter UserNotifier profile load parity): Authorized болған
     * сайын БІР рет профильді оқып, userId/phone мониторингке байлайды
     * (TikTok identify + Firebase setUserId). Guest күйге оралғанда
     * байланыс қайта жүктеледі (қайта кіргенде жаңартылады).
     */
    fun bindUserAnalytics() {
        if (analyticsBound) return
        analyticsBound = true
        viewModelScope.launch {
            (profileRepository.getProfile() as? ApiResult.Success)?.let { result ->
                monitoringService.setUserId(result.value.id?.toString())
                monitoringService.setPhoneNumber(result.value.phone)
                _avatarUrl.value = result.value.avatarUrl
            }
        }
    }

    /** Guest күйі — analytics байланысын қайта қарастыруға дайындайды. */
    fun unbindUserAnalytics() {
        analyticsBound = false
        _avatarUrl.value = null
    }

    /**
     * Суық старт кезінде үзілген Halyk төлемінің order id-і (WebView ашылмай
     * тұрып сақталған). Нәтиже беті ашылып, төлем расталғанда тазаланады.
     */
    suspend fun pendingPaymentOrderId(): Long? = pendingPaymentStore.pendingOrderIdOnce()
}