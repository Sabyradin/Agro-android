package com.agroland.feature.payment.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.theme.extendedColors

/**
 * PaymentMethodSheet — Flutter payment_method_sheet: төлем тәсілін таңдау.
 * Баланс жетсе — «Баланстан төлеу», әрқашан — «Картамен төлеу» (Halyk ePay).
 * orderIds көп болса да Halyk init бір order_id алады — multi-supplier чекаут
 * SupplierPicker арқылы бір бизнеске шектелген, бірінші id қолданылады.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodSheet(
    orderIds: List<Long>,
    totalAmount: Double,
    balance: Double,
    onDismiss: () -> Unit,
    onBalancePaid: () -> Unit,
    onHalykReady: (HalykLaunch) -> Unit,
    viewModel: CheckoutPaymentViewModel = hiltViewModel(),
) {
    val loading by viewModel.loading.collectAsState()
    var error by remember { mutableStateOf<PaymentError?>(null) }

    LaunchedEffect(Unit) { viewModel.reset() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PaymentEvent.BalancePaid -> onBalancePaid()
                is PaymentEvent.HalykReady -> onHalykReady(
                    HalykLaunch(
                        paymentUrl = event.result.paymentUrl,
                        orderId = event.orderId,
                        mock = event.result.mock,
                    ),
                )
                is PaymentEvent.ShowError -> error = event.error
            }
        }
    }

    val ext = extendedColors()
    val orderId = orderIds.firstOrNull() ?: return
    val hasSufficientBalance = balance >= totalAmount
    val deficit = totalAmount - balance
    val remaining = balance - totalAmount

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(L10nR.string.payment_choose_method),
                style = MaterialTheme.typography.titleLarge,
                color = ext.primaryText,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            // ── Жалпы сома + баланс карточкасы ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ext.backgroundLight)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(
                        L10nR.string.payment_order_total,
                        PriceFormatter.format(totalAmount, "₸"),
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = ext.primaryText,
                )
                Text(
                    text = stringResource(
                        L10nR.string.payment_your_balance,
                        PriceFormatter.format(balance, "₸"),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.secondaryText,
                )
            }

            // ── Баланспен төлеу (жетпесе — өшірулі, тапшылық көрсетіледі) ──
            AgroButton(
                text = if (hasSufficientBalance) {
                    stringResource(L10nR.string.payment_pay_with_balance)
                } else {
                    stringResource(
                        L10nR.string.payment_insufficient_balance,
                        PriceFormatter.format(deficit, "₸"),
                    )
                },
                onClick = { viewModel.payFromBalance(orderId) },
                enabled = hasSufficientBalance && !loading,
                loading = loading && hasSufficientBalance,
                modifier = Modifier.fillMaxWidth(),
            )
            if (hasSufficientBalance) {
                Text(
                    text = stringResource(
                        L10nR.string.payment_remaining_balance,
                        PriceFormatter.format(remaining, "₸"),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.secondaryText,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            // ── Картамен төлеу (Halyk ePay) ──
            AgroButton(
                text = stringResource(L10nR.string.payment_pay_with_card),
                onClick = { viewModel.payWithHalyk(orderId) },
                enabled = !loading,
                loading = loading && !hasSufficientBalance,
                containerColor = ext.card,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Қате — парақ ішінде, адам тіліндегі мәтінмен ──
            val currentError = error
            if (currentError != null) {
                Text(
                    text = currentError.displayText(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}