package com.agroland.feature.wallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.wallet.data.TxType
import com.agroland.feature.wallet.data.WalletBalance
import com.agroland.feature.wallet.data.WalletTransaction

/** Backend валюта коды → рәміз (Flutter UiUtils.currencySymbol: KZT → ₸). */
internal fun currencySymbol(currency: String): String =
    if (currency.equals("KZT", ignoreCase = true)) "₸" else currency

/**
 * Әмиян (Flutter BalancePage): primary баланс картасы (қолжетімді + күтуде +
 * ұсталған), «Қаражатты шығару» батырмасы және ledger транзакциялары.
 * WebView/шығару бетінен оралғанда ON_RESUME жаңартылады.
 */
@Composable
fun BalancePage(
    onBack: () -> Unit,
    onWithdraw: (Double) -> Unit,
    viewModel: BalanceViewModel = hiltViewModel(),
) {
    val balance by viewModel.balance.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Шығару/толтыру бетінен қайтқанда баланс жаңа күйде алынады.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.wallet_title),
                onBack = onBack,
            )
        },
    ) { modifier ->
        Box(modifier = modifier) {
            when {
                loading && balance == null -> LoadingWidget()
                balance == null -> CenteredContent {
                    ErrorWithRetry(
                        message = error?.displayText()
                            ?: stringResource(L10nR.string.error_generic_message),
                        onRetry = { viewModel.load() },
                    )
                }
                else -> BalanceContent(
                    balance = balance!!,
                    transactions = transactions,
                    onWithdraw = { onWithdraw(balance!!.balance) },
                )
            }
        }
    }
}

@Composable
private fun BalanceContent(
    balance: WalletBalance,
    transactions: List<WalletTransaction>?,
    onWithdraw: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { BalanceCard(balance) }
        item {
            AgroButton(
                text = stringResource(L10nR.string.wallet_withdraw),
                onClick = onWithdraw,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            Text(
                text = stringResource(L10nR.string.wallet_transactions),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = extendedColors().primaryText,
            )
        }
        if (transactions == null) {
            item { LoadingWidget(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)) }
        } else if (transactions.isEmpty()) {
            item {
                Text(
                    text = stringResource(L10nR.string.wallet_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = extendedColors().secondaryText,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                )
            }
        } else {
            items(transactions.size) { index -> TxTile(transactions[index]) }
        }
    }
}

/** Primary фондық картасы: қолжетімді (ірі) + күтуде/ұсталған (қатар). */
@Composable
private fun BalanceCard(balance: WalletBalance) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(20.dp),
    ) {
        Text(
            text = stringResource(L10nR.string.wallet_available),
            style = MaterialTheme.typography.bodySmall,
            color = extendedColors().white.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = PriceFormatter.format(balance.balance, currencySymbol(balance.currency)),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = extendedColors().white,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BalanceSub(
                label = stringResource(L10nR.string.wallet_pending),
                value = balance.pending,
                modifier = Modifier.weight(1f),
            )
            BalanceSub(
                label = stringResource(L10nR.string.wallet_hold),
                value = balance.hold,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BalanceSub(label: String, value: Double, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = extendedColors().white.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = PriceFormatter.format(value),
            style = MaterialTheme.typography.titleMedium,
            color = extendedColors().white,
        )
    }
}

/** Ledger жолы — иконка + сипаттама + күн ± сома (Flutter _TxTile). */
@Composable
internal fun TxTile(tx: WalletTransaction) {
    val ext = extendedColors()
    val isCredit = tx.type == TxType.ACCRUAL
    val color = if (isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val (icon, label) = when (tx.type) {
        TxType.ACCRUAL -> Icons.Rounded.ArrowDownward to stringResource(L10nR.string.wallet_tx_accrual)
        TxType.WITHDRAW -> Icons.Rounded.ArrowUpward to stringResource(L10nR.string.wallet_tx_withdraw)
        TxType.HOLD -> Icons.Rounded.PauseCircle to stringResource(L10nR.string.wallet_tx_hold)
        TxType.UNKNOWN -> Icons.Rounded.SwapHoriz to stringResource(L10nR.string.wallet_tx_unknown)
    }
    val dateText = DateFormatter.formatDateTime(tx.createdAt)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.description?.takeIf { it.isNotBlank() } ?: label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = ext.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (dateText.isNotBlank()) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.secondaryText,
                )
            }
        }
        Text(
            text = (if (isCredit) "+" else "-") + PriceFormatter.format(tx.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}