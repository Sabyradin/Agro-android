package com.agroland.feature.auth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Auth ағынының біртұтас экраны: PhoneEntry → OtpEntry (↔ Register).
 * Күй машинасы AuthViewModel-де — экранаралық навигация қарапайым.
 * Authorized болғанда [onAuthorized] шақырылады (PIN орнату ұсынысымен).
 */
@Composable
fun AuthFlowPage(
    onAuthorized: (offerPinSetup: Boolean) -> Unit,
    onClosed: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Кіру аяқталды (OTP/MFA немесе биометрия) → навигация осы деңгейде.
    // Бұрын OtpPage ішіндегі LaunchedEffect шақыратын: бірақ Done күйінде
    // OtpPage композициядан бірден шығып, effect іске қоспай қалатын —
    // кіру сәтті болса да экран ақ болып тұрып қалатын.
    LaunchedEffect(state) {
        if (state is AuthUiState.Done) onAuthorized(!viewModel.pinManager.isPinSet)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val s = state) {
            is AuthUiState.PhoneEntry -> LoginPhonePage(
                viewModel = viewModel,
                onNavigateToOtp = { /* күй өзгереді — экран осы жерде қайта сызылады */ },
                onNavigateToRegister = { phone -> viewModel.openRegister(phone) },
                onClose = onClosed,
            )
            is AuthUiState.OtpEntry -> OtpPage(
                viewModel = viewModel,
                onBack = onClosed,
            )
            is AuthUiState.Register -> RegisterPage(
                initialPhone = s.phone,
                viewModel = viewModel,
                onNavigateToOtp = { },
                onBack = { viewModel.resetToPhoneEntry() },
            )
            is AuthUiState.Done -> Unit
        }
    }
}