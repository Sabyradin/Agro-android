package com.agroland.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.agroland.core.common.validators.Validators
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.common.phone.CountryPhoneMask
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroPhoneField
import com.agroland.core.ui.components.AgroChip
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.AgroTextButton
import com.agroland.core.ui.theme.extendedColors

/** Пайдаланушы түрлері — backend жібереді: individual | business («Бизнес» UI сөзі). */
private const val USER_TYPE_INDIVIDUAL = "individual"
private const val USER_TYPE_BUSINESS = "business"

/**
 * Тіркелу экраны: телефон, аты, түрі (Жеке/Бизнес), БИН (бизнес үшін міндетті).
 */
@Composable
fun RegisterPage(
    initialPhone: String,
    viewModel: AuthViewModel,
    onNavigateToOtp: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Кіру бетінен толық нөмір (+7…) келеді — оны ел коды мен ұлттық
    // бөлікке жіктеп аламыз (Flutter/iOS паритеті, ISSUES #66).
    var country by remember {
        mutableStateOf(CountryPhoneMask.fromE164(initialPhone) ?: CountryPhoneMask.KZ)
    }
    var digits by remember {
        mutableStateOf(
            CountryPhoneMask.nationalDigits(
                initialPhone,
                CountryPhoneMask.fromE164(initialPhone) ?: CountryPhoneMask.KZ,
            ),
        )
    }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isBusiness by remember { mutableStateOf(false) }
    var bin by remember { mutableStateOf("") }

    var phoneError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var binError by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is AuthUiState.OtpEntry) onNavigateToOtp()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(L10nR.string.auth_register_title),
            style = MaterialTheme.typography.displayMedium,
            color = extendedColors().primaryText,
        )
        AgroPhoneField(
            digits = digits,
            onDigitsChange = { digits = it; phoneError = false },
            country = country,
            onCountryChange = { country = it },
            isError = phoneError,
            errorText = stringResource(L10nR.string.auth_phone_error),
        )
        AgroTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            label = stringResource(L10nR.string.auth_name_hint),
            isError = nameError,
            errorText = stringResource(L10nR.string.auth_name_error),
        )
        Text(
            text = stringResource(L10nR.string.auth_user_type_title),
            style = MaterialTheme.typography.bodySmall,
            color = extendedColors().primaryText,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AgroChip(
                text = stringResource(L10nR.string.auth_user_type_individual),
                selected = !isBusiness,
                onClick = { isBusiness = false },
                modifier = Modifier,
            )
            AgroChip(
                text = stringResource(L10nR.string.auth_user_type_business),
                selected = isBusiness,
                onClick = { isBusiness = true },
                modifier = Modifier,
            )
        }
        if (isBusiness) {
            AgroTextField(
                value = bin,
                onValueChange = { bin = it.filter(Char::isDigit).take(12); binError = false },
                label = stringResource(L10nR.string.auth_company_bin_hint),
                isError = binError,
                errorText = stringResource(L10nR.string.auth_company_bin_error),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        AgroTextField(
            value = email,
            onValueChange = { email = it },
            label = stringResource(L10nR.string.auth_email_hint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
        }
        AgroButton(
            text = stringResource(L10nR.string.auth_register_title),
            onClick = {
                var valid = true
                if (!country.isComplete(digits)) {
                    phoneError = true; valid = false
                }
                if (name.isBlank()) {
                    nameError = true; valid = false
                }
                if (isBusiness && !Validators.isValidBin(bin)) {
                    binError = true; valid = false
                }
                if (valid) {
                    viewModel.startRegister(
                        phone = country.toE164(digits),
                        name = name,
                        userType = if (isBusiness) USER_TYPE_BUSINESS else USER_TYPE_INDIVIDUAL,
                        companyBin = bin.takeIf { isBusiness },
                        email = email.takeIf { it.isNotBlank() },
                    )
                }
            },
            loading = loading,
            modifier = Modifier.fillMaxWidth(),
        )
        AgroTextButton(
            text = stringResource(L10nR.string.common_back),
            onClick = onBack,
        )
    }
}