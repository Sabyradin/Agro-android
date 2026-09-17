package com.agroland.feature.wallet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.BottomActionContainer
import com.agroland.core.ui.theme.extendedColors

/** ҚР IBAN: «KZ» + 17 цифр (Flutter withdraw_page валидаторы). */
private val IBAN_REGEX = Regex("^KZ\\d{17}$")

/** Минималды шығару сома — backend шектеуімен сәйкес (web handoff: MIN 5000 KZT). */
private const val MIN_WITHDRAW = 5000.0

/**
 * Қаражат шығару (Flutter WithdrawPage): сома (мин 5 000 ₸, ≤ қолжетімді),
 * IBAN (KZ + 17 цифр) + банк атауы міндетті, БИК/SWIFT және шот нөмірі өз еркімен.
 * Сәттілікте қабылданды диалогы + бет жабылады (баланс admin мақұлдағанда кемиді).
 */
@Composable
fun WithdrawPage(
    availableBalance: Double,
    onBack: () -> Unit,
    onDone: () -> Unit = onBack,
    viewModel: WithdrawViewModel = hiltViewModel(),
) {
    val loading by viewModel.loading.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var successDialog by remember { mutableStateOf(false) }

    val networkError = stringResource(L10nR.string.error_no_internet)
    val genericError = stringResource(L10nR.string.error_generic_message)

    // Валидация мәтіндері — submit() ішінде stringResource шақырылмайды.
    val errValidAmount = stringResource(L10nR.string.wallet_enter_valid_amount)
    val errMinWithdraw = stringResource(L10nR.string.wallet_withdraw_min)
    val errTooHigh = stringResource(L10nR.string.wallet_withdraw_too_high)
    val errRequired = stringResource(L10nR.string.wallet_field_required)
    val errIbanFormat = stringResource(L10nR.string.wallet_withdraw_iban_format)

    // Форма өрістері — process death-тен кейін де сақталады.
    var amount by rememberSaveable { mutableStateOf("") }
    var iban by rememberSaveable { mutableStateOf("") }
    var bankName by rememberSaveable { mutableStateOf("") }
    var bik by rememberSaveable { mutableStateOf("") }
    var account by rememberSaveable { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var ibanError by remember { mutableStateOf<String?>(null) }
    var bankNameError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is WalletEvent.ShowError -> snackbar.showSnackbar(
                    event.error.displayText(networkError, genericError),
                )
                WalletEvent.WithdrawDone -> successDialog = true
                is WalletEvent.TopUpHtmlReady -> Unit
            }
        }
    }

    fun submit() {
        val cleanAmount = amount.trim().replace(Regex("\\s"), "").toDoubleOrNull()
        val cleanIban = iban.trim().replace(Regex("\\s"), "").uppercase()
        amountError = when {
            cleanAmount == null || cleanAmount <= 0.0 -> errValidAmount
            cleanAmount < MIN_WITHDRAW -> errMinWithdraw
            cleanAmount > availableBalance -> errTooHigh
            else -> null
        }
        ibanError = when {
            cleanIban.isBlank() -> errRequired
            !IBAN_REGEX.matches(cleanIban) -> errIbanFormat
            else -> null
        }
        bankNameError = bankName.trim().takeIf { it.isBlank() }?.let { errRequired }
        if (amountError != null || ibanError != null || bankNameError != null) return
        viewModel.request(cleanAmount!!, cleanIban, bankName.trim(), bik.trim(), account.trim())
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.wallet_withdraw_title),
                onBack = onBack,
            )
        },
    ) { modifier ->
        Box(modifier = modifier) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                AvailableBalanceCard(availableBalance)
                AgroTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = stringResource(L10nR.string.wallet_withdraw_amount),
                    isError = amountError != null,
                    errorText = amountError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                AgroTextField(
                    value = iban,
                    onValueChange = { iban = it.uppercase() },
                    label = "IBAN",
                    isError = ibanError != null,
                    errorText = ibanError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                )
                AgroTextField(
                    value = bankName,
                    onValueChange = { bankName = it; bankNameError = null },
                    label = stringResource(L10nR.string.wallet_bank_name),
                    isError = bankNameError != null,
                    errorText = bankNameError,
                )
                AgroTextField(
                    value = bik,
                    onValueChange = { bik = it },
                    label = stringResource(L10nR.string.wallet_bank_bik),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                )
                AgroTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = stringResource(L10nR.string.wallet_bank_account),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                )
                Spacer(modifier = Modifier.height(120.dp))
            }

            BottomActionContainer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                AgroButton(
                    text = stringResource(L10nR.string.wallet_withdraw_confirm),
                    onClick = { submit() },
                    enabled = !loading,
                    loading = loading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    if (successDialog) {
        AlertDialog(
            onDismissRequest = onDone,
            title = { Text(stringResource(L10nR.string.wallet_withdraw_success)) },
            confirmButton = {
                TextButton(onClick = onDone) {
                    Text(stringResource(L10nR.string.common_ok))
                }
            },
        )
    }
}

/** Қолжетімді баланс жолы — карточка түрінде (Flutter withdraw_page басы). */
@Composable
private fun AvailableBalanceCard(availableBalance: Double) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ext.card)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.AccountBalanceWallet,
            contentDescription = null,
            tint = ext.secondaryText,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = stringResource(L10nR.string.wallet_available),
            style = MaterialTheme.typography.bodyMedium,
            color = ext.secondaryText,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = PriceFormatter.format(availableBalance),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = ext.primaryText,
        )
    }
}