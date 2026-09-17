package com.agroland.feature.wallet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget

/**
 * Транзакциялар тарихы (Flutter TransactionHistoryPage): /business/balance/transactions
 * ledger-і — профиль мәзірінің «Транзакциялар тарихы» жолы ашады.
 */
@Composable
fun TransactionHistoryPage(
    onBack: () -> Unit,
    viewModel: BalanceViewModel = hiltViewModel(),
) {
    val transactions by viewModel.transactions.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.wallet_transaction_history),
                onBack = onBack,
            )
        },
    ) { modifier ->
        Box(modifier = modifier) {
            when {
                loading && transactions == null -> LoadingWidget()
                transactions == null -> CenteredContent {
                    ErrorWithRetry(
                        message = error?.displayText()
                            ?: stringResource(L10nR.string.error_generic_message),
                        onRetry = { viewModel.refresh() },
                    )
                }
                transactions!!.isEmpty() -> CenteredContent {
                    EmptyView(
                        icon = Icons.Rounded.ReceiptLong,
                        title = stringResource(L10nR.string.wallet_empty),
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(transactions!!.size) { index -> TxTile(transactions!![index]) }
                }
            }
        }
    }
}