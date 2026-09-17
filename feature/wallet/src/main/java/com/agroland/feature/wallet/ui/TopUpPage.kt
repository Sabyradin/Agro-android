package com.agroland.feature.wallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.BottomActionContainer
import com.agroland.core.ui.theme.extendedColors

/** Дайын сомалар — Flutter payment_page predefinedAmounts. */
private val PRESET_AMOUNTS = listOf(1_000.0, 5_000.0, 20_000.0, 100_000.0)

/**
 * Әмиянды толтыру (Flutter PaymentPage): баланс картасы + бірлік сипаттамасы,
 * сома енгізу + дайын чиптер, «Банк карталары» қызметі. Растау → BCC 3D Secure
 * HTML формасы WebView-қа ашылады (exitRedirectUrl = agroland.kz).
 */
@Composable
fun TopUpPage(
    onBack: () -> Unit,
    onOpenWebView: (html: String) -> Unit,
    viewModel: TopUpViewModel = hiltViewModel(),
) {
    val balance by viewModel.balance.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var amount by rememberSaveable { mutableStateOf("") }
    var infoDialog by remember { mutableStateOf(false) }

    val networkError = stringResource(L10nR.string.error_no_internet)
    val genericError = stringResource(L10nR.string.error_generic_message)
    val validAmountError = stringResource(L10nR.string.wallet_enter_valid_amount)

    LaunchedEffect(Unit) {
        viewModel.loadBalance()
        viewModel.events.collect { event ->
            when (event) {
                is WalletEvent.TopUpHtmlReady -> onOpenWebView(event.html)
                is WalletEvent.ShowError ->
                    snackbar.showSnackbar(event.error.displayText(networkError, genericError))
                WalletEvent.WithdrawDone -> Unit
            }
        }
    }

    // Толтыру WebView-ы жабылғанда баланс жаңартылады (Flutter pop(true) → getUser).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadBalance()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val scope = rememberCoroutineScope()
    fun confirm() {
        val value = amount.trim().toDoubleOrNull()
        if (value == null || value <= 0.0) {
            // Үнсіз қалмайды — соманы енгізу керегін айтамыз (Flutter toast).
            scope.launch { snackbar.showSnackbar(validAmountError) }
            return
        }
        viewModel.makePayment(value)
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.wallet_replenish),
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
                BalanceInfoCard(balance = balance, onInfo = { infoDialog = true })
                AmountCard(
                    amount = amount,
                    onAmountChange = { amount = it.filter { ch -> ch.isDigit() } },
                )
                ServiceCard()
                Spacer(modifier = Modifier.height(120.dp))
            }

            BottomActionContainer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                AgroButton(
                    text = stringResource(L10nR.string.wallet_confirm),
                    onClick = { confirm() },
                    enabled = !loading,
                    loading = loading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    if (infoDialog) {
        AlertDialog(
            onDismissRequest = { infoDialog = false },
            title = { Text(stringResource(L10nR.string.wallet_your_balance)) },
            text = { Text(stringResource(L10nR.string.wallet_units_description)) },
            confirmButton = {
                TextButton(onClick = { infoDialog = false }) {
                    Text(stringResource(L10nR.string.common_ok))
                }
            },
        )
    }
}

/** Баланс картасы + бірлік сипаттамасы (Flutter UnitsDescription). */
@Composable
private fun BalanceInfoCard(balance: Double, onInfo: () -> Unit) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ext.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(L10nR.string.wallet_your_balance) + ": ",
                style = MaterialTheme.typography.bodyMedium,
                color = ext.primaryText,
            )
            Text(
                text = PriceFormatter.format(balance),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = ext.secondaryText,
                modifier = Modifier.size(24.dp),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = ext.secondaryText,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(L10nR.string.wallet_units_description),
                style = MaterialTheme.typography.bodyMedium,
                color = ext.secondaryText,
            )
        }
    }
}

/** Сома картасы: енгізу өрісі + дайын чиптер (Flutter payment_page). */
@Composable
private fun AmountCard(amount: String, onAmountChange: (String) -> Unit) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ext.card),
    ) {
        Text(
            text = stringResource(L10nR.string.wallet_topup_amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = ext.primaryText,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp),
        )
        AgroTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = stringResource(L10nR.string.wallet_enter_hint),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PRESET_AMOUNTS.forEach { preset ->
                PresetChip(
                    text = PriceFormatter.format(preset),
                    selected = amount.toDoubleOrNull() == preset,
                    onClick = { onAmountChange(preset.toLong().toString()) },
                )
            }
        }
    }
}

/** Дайын сома чипі (Flutter ChoiceChip). */
@Composable
private fun PresetChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val ext = extendedColors()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else ext.grey)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) ext.white else ext.primaryText,
            maxLines = 1,
        )
    }
}

/** Толтыру қызметі — банк карталары (Flutter payment_service картасы). */
@Composable
private fun ServiceCard() {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ext.card),
    ) {
        Text(
            text = stringResource(L10nR.string.wallet_topup_service),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = ext.primaryText,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.CreditCard,
                contentDescription = null,
                tint = ext.primaryText,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(L10nR.string.wallet_bank_cards),
                style = MaterialTheme.typography.bodyMedium,
                color = ext.primaryText,
            )
        }
    }
}