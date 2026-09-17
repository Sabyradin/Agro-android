package com.agroland.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.agroland.core.common.phone.CountryPhoneMask
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroOutlinedButton
import com.agroland.core.ui.components.AgroPhoneField
import com.agroland.core.ui.components.AgroTextButton
import com.agroland.core.ui.components.AppLogo
import com.agroland.core.ui.theme.AgroSpacing
import com.agroland.core.ui.theme.extendedColors

/**
 * Кіру экраны — iOS нұсқасымен бірдей: жоғарыда «Жабу», ортада логотип,
 * «Кіру» тақырыбы мен түсіндірмесі, ел коды бар телефон өрісі; астында
 * «Әрі қарай» батырмасы мен «Тіркелу» сілтемесі.
 *
 * Нөмір backend-ке `+7` + ұлттық цифрлар түрінде жіберіледі (Flutter/iOS
 * паритеті) — бұрын пайдаланушы терген жол сол күйі жіберіліп, аккаунт
 * табылмайтын (ISSUES #66).
 */
@Composable
fun LoginPhonePage(
    viewModel: AuthViewModel,
    onNavigateToOtp: () -> Unit,
    onNavigateToRegister: (String) -> Unit,
    onClose: (() -> Unit)? = null,
) {
    val ext = extendedColors()
    val state by viewModel.state.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    var country by remember { mutableStateOf(CountryPhoneMask.KZ) }
    var digits by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf(false) }
    var showBiometric by remember { mutableStateOf(false) }

    val activity = context as? FragmentActivity
    LaunchedEffect(activity) {
        showBiometric = activity != null &&
            viewModel.hasBiometricToken &&
            viewModel.isBiometricAvailable(activity)
    }

    LaunchedEffect(state) {
        if (state is AuthUiState.OtpEntry) onNavigateToOtp()
    }

    val biometricPromptTitle = stringResource(L10nR.string.auth_biometric_prompt)
    val biometricNegative = stringResource(L10nR.string.common_cancel)

    val submit = {
        if (country.isComplete(digits)) {
            viewModel.startLogin(country.toE164(digits))
        } else {
            phoneError = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding(),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (onClose != null) {
                AgroTextButton(
                    text = stringResource(L10nR.string.common_close),
                    onClick = onClose,
                    modifier = Modifier.padding(start = AgroSpacing.sm),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AgroSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(AgroSpacing.xl))
            AppLogo(size = 108.dp, card = true)
            Spacer(Modifier.height(AgroSpacing.xl))
            Text(
                text = stringResource(L10nR.string.auth_login_title),
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                color = ext.primaryText,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AgroSpacing.sm))
            Text(
                text = stringResource(L10nR.string.auth_login_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = ext.secondaryText,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(AgroSpacing.xl))
            AgroPhoneField(
                digits = digits,
                onDigitsChange = {
                    digits = it
                    phoneError = false
                },
                country = country,
                onCountryChange = { country = it },
                isError = phoneError,
                errorText = stringResource(L10nR.string.auth_phone_error),
            )
            error?.let { err ->
                Spacer(Modifier.height(AgroSpacing.md))
                // «Тіркелу» әрекеті бетте бір рет қана (астыңғы сілтеме) —
                // бұрын қатенің астына екінші батырма қосылып, қиылып тұратын.
                Text(
                    text = when {
                        err.isUserNotFound -> stringResource(L10nR.string.auth_user_not_found)
                        err.isNetwork -> stringResource(L10nR.string.error_no_internet)
                        else -> err.backendMessage
                            ?: stringResource(L10nR.string.error_generic_message)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(AgroSpacing.xl))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AgroSpacing.lg,
                    end = AgroSpacing.lg,
                    top = AgroSpacing.md,
                    bottom = AgroSpacing.lg,
                ),
            verticalArrangement = Arrangement.spacedBy(AgroSpacing.md),
        ) {
            AgroButton(
                text = stringResource(L10nR.string.common_next),
                onClick = submit,
                loading = loading,
                enabled = country.isComplete(digits),
                modifier = Modifier.fillMaxWidth(),
            )
            if (showBiometric) {
                AgroOutlinedButton(
                    text = stringResource(L10nR.string.auth_biometric_login),
                    onClick = {
                        activity?.let {
                            viewModel.showBiometricPrompt(it, biometricPromptTitle, biometricNegative)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AgroTextButton(
                    text = stringResource(L10nR.string.auth_register_title),
                    onClick = { onNavigateToRegister(country.toE164(digits)) },
                )
            }
        }
    }
}
