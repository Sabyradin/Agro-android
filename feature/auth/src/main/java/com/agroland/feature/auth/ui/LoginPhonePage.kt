package com.agroland.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.agroland.core.common.validators.Validators
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.AgroTextButton
import com.agroland.core.ui.components.AppLogo
import com.agroland.core.ui.theme.extendedColors

/**
 * Кіру экраны: телефон енгізу → Next.
 * Соңғы сессияда биометрия token бар болса — биометриямен кіру (best-effort).
 */
@Composable
fun LoginPhonePage(
    viewModel: AuthViewModel,
    onNavigateToOtp: () -> Unit,
    onNavigateToRegister: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    var phone by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf(false) }
    var showBiometric by remember { mutableStateOf(false) }

    val activity = context as? FragmentActivity
    LaunchedEffect(activity) {
        showBiometric = activity != null &&
            viewModel.hasBiometricToken &&
            viewModel.isBiometricAvailable(activity)
    }

    // OTP/Registration күйіне өткенде навигация.
    LaunchedEffect(state) {
        if (state is AuthUiState.OtpEntry) onNavigateToOtp()
    }

    val biometricPromptTitle = stringResource(L10nR.string.auth_biometric_prompt)
    val biometricNegative = stringResource(L10nR.string.common_cancel)

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppLogo()
        Text(
            text = stringResource(L10nR.string.auth_login_title),
            style = MaterialTheme.typography.displayMedium,
            color = extendedColors().primaryText,
        )
        AgroTextField(
            value = phone,
            onValueChange = {
                phone = it
                phoneError = false
            },
            label = stringResource(L10nR.string.auth_phone_hint),
            isError = phoneError,
            errorText = stringResource(L10nR.string.auth_phone_error),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        error?.let { err ->
            Text(
                text = err.backendMessage ?: stringResource(
                    if (err.isNetwork) L10nR.string.error_no_internet
                    else L10nR.string.error_generic_message,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
            )
            if (err.isUserNotFound) {
                AgroTextButton(
                    text = stringResource(L10nR.string.auth_user_not_found),
                    onClick = { onNavigateToRegister(phone) },
                )
            }
        }
        AgroButton(
            text = stringResource(L10nR.string.auth_login_title),
            onClick = {
                if (Validators.isValidContactPhone(phone)) {
                    viewModel.startLogin(phone)
                } else {
                    phoneError = true
                }
            },
            loading = loading,
            modifier = Modifier.fillMaxWidth(),
        )
        AgroTextButton(
            text = stringResource(L10nR.string.auth_register_title),
            onClick = { onNavigateToRegister(phone) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (showBiometric) {
            AgroTextButton(
                text = stringResource(L10nR.string.auth_biometric_login),
                onClick = {
                    activity?.let {
                        viewModel.showBiometricPrompt(it, biometricPromptTitle, biometricNegative)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}