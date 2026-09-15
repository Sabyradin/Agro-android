package com.agroland.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroTextButton
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.theme.extendedColors

/**
 * OTP экраны: SMS код 6 цифр. Көрсетілген телефон + қайта жіберу + email арқылы жіберу.
 * masked_email бар болса — email түймесі көрінеді.
 */
@Composable
fun OtpPage(
    viewModel: AuthViewModel,
    onAuthorized: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val emailSent by viewModel.emailSent.collectAsState()

    var code by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf(false) }

    val otp = state as? AuthUiState.OtpEntry

    LaunchedEffect(state) {
        if (state is AuthUiState.Done) onAuthorized()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(L10nR.string.auth_otp_title),
            style = MaterialTheme.typography.displayMedium,
            color = extendedColors().primaryText,
        )
        otp?.let {
            Text(
                text = stringResource(L10nR.string.auth_otp_sent_to, it.phone),
                style = MaterialTheme.typography.bodySmall,
                color = extendedColors().secondaryText,
            )
        }
        AgroTextField(
            value = code,
            onValueChange = {
                code = it.filter(Char::isDigit).take(6)
                codeError = false
            },
            label = stringResource(L10nR.string.auth_otp_title),
            isError = codeError,
            errorText = stringResource(L10nR.string.auth_otp_error),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        error?.let { err ->
            Text(
                text = err.backendMessage ?: stringResource(
                    if (err.isNetwork) L10nR.string.error_no_internet
                    else L10nR.string.error_generic_message,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        AgroButton(
            text = stringResource(L10nR.string.common_next),
            onClick = {
                if (code.length == 6) {
                    viewModel.confirmOtp(code)
                } else {
                    codeError = true
                }
            },
            loading = loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AgroTextButton(
                text = stringResource(L10nR.string.auth_otp_resend),
                onClick = { viewModel.resendSms() },
            )
            if (otp?.maskedEmail != null) {
                AgroTextButton(
                    text = stringResource(L10nR.string.auth_otp_send_email),
                    onClick = { viewModel.sendCodeByEmail() },
                )
            }
        }
        if (emailSent) {
            Text(
                text = otp?.maskedEmail ?: "",
                style = MaterialTheme.typography.labelMedium,
                color = extendedColors().secondaryText,
            )
        }
        AgroTextButton(
            text = stringResource(L10nR.string.common_back),
            onClick = {
                viewModel.resetToPhoneEntry()
                onBack()
            },
        )
    }
}