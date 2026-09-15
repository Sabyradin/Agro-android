package com.agroland.app

import androidx.lifecycle.ViewModel
import com.agroland.feature.auth.security.BiometricAuthenticator
import com.agroland.feature.auth.security.PinManager
import com.agroland.feature.auth.session.SessionController
import com.agroland.feature.auth.session.SessionState
import com.agroland.feature.payment.data.PendingPaymentStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** App деңгейіндегі күй: сессия + app-lock + үздік Halyk төлемі (Фаза 9). */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionController: SessionController,
    val pinManager: PinManager,
    val biometricAuthenticator: BiometricAuthenticator,
    private val pendingPaymentStore: PendingPaymentStore,
) : ViewModel() {

    val session: StateFlow<SessionState> = sessionController.state

    val isAuthorized: Boolean
        get() = sessionController.state.value == SessionState.Authorized

    val pinIsSet: Boolean
        get() = pinManager.isPinSet

    fun logout() {
        sessionController.onLoggedOut()
    }

    /**
     * Суық старт кезінде үзілген Halyk төлемінің order id-і (WebView ашылмай
     * тұрып сақталған). Нәтиже беті ашылып, төлем расталғанда тазаланады.
     */
    suspend fun pendingPaymentOrderId(): Long? = pendingPaymentStore.pendingOrderIdOnce()
}