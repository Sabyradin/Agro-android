package com.agroland.app

import androidx.lifecycle.ViewModel
import com.agroland.feature.auth.security.BiometricAuthenticator
import com.agroland.feature.auth.security.PinManager
import com.agroland.feature.auth.session.SessionController
import com.agroland.feature.auth.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** App деңгейіндегі күй: сессия + app-lock. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionController: SessionController,
    val pinManager: PinManager,
    val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {

    val session: StateFlow<SessionState> = sessionController.state

    val isAuthorized: Boolean
        get() = sessionController.state.value == SessionState.Authorized

    val pinIsSet: Boolean
        get() = pinManager.isPinSet

    fun logout() {
        sessionController.onLoggedOut()
    }
}