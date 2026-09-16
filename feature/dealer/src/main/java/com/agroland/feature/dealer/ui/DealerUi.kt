package com.agroland.feature.dealer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.agroland.core.l10n.R as L10nR

/**
 * Дилер консолінің ортақ UI көмекшілері: статус белгілерінің түсі/мәтіні,
 * өлшем бірліктері, қате хабарламалары. Барлығы core:l10n арқылы —
 * шики error_code ешқашан пайдаланушыға шықпайды.
 */

/** Тапсырыс статусының түсі (Flutter _OrderStatusBadge._color, 1:1). */
fun dealerOrderStatusColor(status: String): Color = when (status) {
    "supplier_query" -> Color(0xFFFF9800)
    "supplier_confirmed", "logistics_search", "logistics_assigned" -> Color(0xFF2196F3)
    "logistics_enroute", "loading", "loaded" -> Color(0xFF009688)
    "delivered", "act_signed", "paid" -> Color(0xFF4CAF50)
    "cancelled" -> Color(0xFFE53935)
    else -> Color(0xFF9E9E9E)
}

/** Тапсырыс статусының локализацияланған атауы (Flutter _OrderStatusBadge._label). */
fun dealerOrderStatusLabelRes(status: String): Int = when (status) {
    "supplier_query" -> L10nR.string.dealer_orders_new
    "supplier_confirmed", "logistics_search", "logistics_assigned" ->
        L10nR.string.dealer_orders_confirmed
    "logistics_enroute", "loading", "loaded" -> L10nR.string.dealer_orders_shipped
    "delivered", "act_signed", "paid" -> L10nR.string.dealer_orders_delivered
    "cancelled" -> L10nR.string.dealer_orders_cancelled
    else -> L10nR.string.order_status_unknown
}

/** Төлем статусы: түс + мәтін (Flutter _PaymentBadge, 1:1). */
fun dealerPaymentStatusColor(status: String): Color = when (status) {
    "paid" -> Color(0xFF4CAF50)
    "refunded" -> Color(0xFF9E9E9E)
    else -> Color(0xFFFF9800)
}

fun dealerPaymentStatusLabelRes(status: String): Int = when (status) {
    "paid" -> L10nR.string.order_status_paid
    "refunded" -> L10nR.string.order_status_refunded
    else -> L10nR.string.order_status_unpaid
}

/** Өлшем бірлігінің қысқа атауы — measurement_unit_* (13 бірлік, Flutter 1:1). */
fun measurementUnitLabelRes(unit: String?): Int? = when (unit) {
    "piece" -> L10nR.string.measurement_unit_piece
    "kilogram" -> L10nR.string.measurement_unit_kilogram
    "liter" -> L10nR.string.measurement_unit_liter
    "ton" -> L10nR.string.measurement_unit_ton
    "square_meter" -> L10nR.string.measurement_unit_square_meter
    "cubic_meter" -> L10nR.string.measurement_unit_cubic_meter
    "hectare" -> L10nR.string.measurement_unit_hectare
    "bag" -> L10nR.string.measurement_unit_bag
    "centner" -> L10nR.string.measurement_unit_centner
    "head" -> L10nR.string.measurement_unit_head
    "pair" -> L10nR.string.measurement_unit_pair
    "meter" -> L10nR.string.measurement_unit_meter
    "box" -> L10nR.string.measurement_unit_box
    else -> null
}

/** Комиссия (conversion) пайызбен: 0.1234 → «12,3%». */
fun formatConversionPercent(value: Double): String {
    val pct = value * 100
    val rounded = (pct * 10).toLong() / 10.0
    return rounded.toString().replace('.', ',') + "%"
}

/** Badge саны — 99+ шегі (Flutter tab badge format). */
fun badgeCount(count: Int): String = if (count > 99) "99+" else count.toString()

/** Мәтіндік түс көмекшісі — theme primary фон үшін контраст. */
val dealerErrorColor: Color
    @Composable get() = MaterialTheme.colorScheme.error

// ── Диалогтар және тізим көмекшілері (барлық дилер беттері бөліседі) ──

/**
 * Растау диалогы — жою/қайтару әрекеттері үшін (Flutter showDialog<bool>).
 * confirmLabel қызыл түспен көрсетіледі.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmColor: Color = MaterialTheme.colorScheme.error,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text(title) },
        text = { androidx.compose.material3.Text(message) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                androidx.compose.material3.Text(confirmLabel, color = confirmColor)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text(
                    androidx.compose.ui.res.stringResource(
                        com.agroland.core.l10n.R.string.common_cancel,
                    ),
                )
            }
        },
    )
}

/**
 * «Тізім соңына жақындағанда көбірек жүктеу» эффектісі — соңғы 3 элемент
 * көрініп тұрғанда loadMore шақырылады (бір рет, loadingMore күйімен қорғалған).
 */
@Composable
fun InfiniteScrollEffect(
    listState: androidx.compose.foundation.lazy.LazyListState,
    onLoadMore: () -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow {
            val layout = listState.layoutInfo
            val last = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= layout.totalItemsCount - 3 && layout.totalItemsCount > 0
        }.collect { shouldLoad ->
            if (shouldLoad) onLoadMore()
        }
    }
}