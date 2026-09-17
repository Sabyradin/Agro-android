package com.agroland.feature.cart.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.components.ShimmerCard
import com.agroland.core.ui.components.StatusChip
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.cart.data.Order
import com.agroland.feature.cart.data.OrderStatus
import com.agroland.feature.cart.data.PaymentStatus
import com.agroland.feature.cart.data.canConfirmReceipt
import com.agroland.feature.payment.ui.HalykLaunch
import com.agroland.feature.payment.ui.PaymentMethodSheet

/**
 * OrderDetailPage — Flutter order_detail_page: статусы, тауарлар + Smart
 * Calculator чекі, жеткізу/өзі алу мәліметтері, timeline, сатушы карточкасы,
 * төлем (Фаза 9), болдырмау/қабылдау/қайта тапсырыс.
 */
@Composable
fun OrderDetailPage(
    orderId: Long,
    onBack: () -> Unit,
    onOpenHalyk: (HalykLaunch) -> Unit = {},
    viewModel: OrderDetailViewModel = hiltViewModel(),
) {
    val order by viewModel.order.collectAsState()
    val tracking by viewModel.tracking.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val actionLoading by viewModel.actionLoading.collectAsState()
    val balance by viewModel.balance.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val noInternetText = stringResource(L10nR.string.error_no_internet)
    val genericErrorText = stringResource(L10nR.string.error_generic_message)
    val cancelToast = stringResource(L10nR.string.cart_order_cancelled)
    val reorderToast = stringResource(L10nR.string.cart_reorder_success)
    val receiptToast = stringResource(L10nR.string.cart_receipt_confirmed)
    // «Төлеу» батырмасы — төлем парағы (баланспен/картамен, Фаза 9).
    var paySheetVisible by remember { mutableStateOf(false) }
    var balancePaidDialog by remember { mutableStateOf(false) }
    LaunchedEffect(orderId) { viewModel.load(orderId) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CartEvent.ShowError ->
                    snackbar.showSnackbar(event.error.displayText(noInternetText, genericErrorText))
                CartEvent.OrderCancelled -> snackbar.showSnackbar(cancelToast)
                CartEvent.ReorderDone -> snackbar.showSnackbar(reorderToast)
                CartEvent.ReceiptConfirmed -> snackbar.showSnackbar(receiptToast)
                else -> Unit
            }
        }
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.cart_order_number, orderId),
                onBack = onBack,
            )
        },
    ) { inner ->
        Box(modifier = inner.fillMaxSize()) {
            when {
                loading && order == null -> Column { repeat(5) { ShimmerCard() } }
                error != null && order == null -> CenteredContent {
                    ErrorWithRetry(
                        onRetry = { viewModel.load(orderId) },
                        message = error!!.displayText(),
                    )
                }
                order != null -> OrderDetailContent(
                    order = order!!,
                    driverInfo = tracking?.driverInfo,
                    actionLoading = actionLoading,
                    onCancel = viewModel::cancelOrder,
                    onConfirmReceipt = viewModel::confirmReceipt,
                    onReorder = viewModel::reorder,
                    onPay = {
                        viewModel.loadBalance()
                        paySheetVisible = true
                    },
                )
                else -> CenteredContent { LoadingWidget() }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    // ── Төлем парағы: баланспен төлеу немесе Halyk ePay картасы (Фаза 9) ──
    val currentOrder = order
    if (paySheetVisible && currentOrder != null) {
        PaymentMethodSheet(
            orderIds = listOf(currentOrder.id),
            totalAmount = currentOrder.orderTotal,
            balance = balance,
            onDismiss = {
                paySheetVisible = false
                viewModel.reload()
            },
            onBalancePaid = {
                paySheetVisible = false
                balancePaidDialog = true
                viewModel.reload()
            },
            onHalykReady = { halyk ->
                paySheetVisible = false
                viewModel.reload()
                onOpenHalyk(halyk)
            },
        )
    }
    if (balancePaidDialog) {
        AlertDialog(
            onDismissRequest = { balancePaidDialog = false },
            title = { Text(stringResource(L10nR.string.payment_successful)) },
            confirmButton = {
                TextButton(onClick = { balancePaidDialog = false }) {
                    Text(stringResource(L10nR.string.common_close))
                }
            },
        )
    }
}

@Composable
private fun OrderDetailContent(
    order: Order,
    driverInfo: String?,
    actionLoading: Boolean,
    onCancel: () -> Unit,
    onConfirmReceipt: () -> Unit,
    onReorder: () -> Unit,
    onPay: () -> Unit,
) {
    var cancelDialog by remember { mutableStateOf(false) }
    var confirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { StatusCard(order) }
        item { OrderItemsCard(order) }
        item { DeliveryInfoCard(order, driverInfo) }
        item { TimelineCard(order) }
        item { SellerCard(order) }
        item {
            ActionButtons(
                order = order,
                loading = actionLoading,
                onCancel = { cancelDialog = true },
                onConfirmReceipt = { confirmDialog = true },
                onReorder = onReorder,
                onPay = onPay,
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (cancelDialog) {
        AlertDialog(
            onDismissRequest = { cancelDialog = false },
            title = { Text(stringResource(L10nR.string.cart_cancel_order_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    cancelDialog = false
                    onCancel()
                }) {
                    Text(stringResource(L10nR.string.cart_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelDialog = false }) {
                    Text(stringResource(L10nR.string.cart_no))
                }
            },
        )
    }
    if (confirmDialog) {
        AlertDialog(
            onDismissRequest = { confirmDialog = false },
            title = { Text(stringResource(L10nR.string.cart_confirm_receipt_question)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDialog = false
                    onConfirmReceipt()
                }) {
                    Text(stringResource(L10nR.string.cart_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDialog = false }) {
                    Text(stringResource(L10nR.string.cart_no))
                }
            },
        )
    }
}

/** Статус + күндер карточкасы. */
@Composable
private fun StatusCard(order: Order) {
    val ext = extendedColors()
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(L10nR.string.cart_order_status),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                )
            }
            StatusChip(
                text = stringResource(order.statusLabelRes()),
                color = order.orderStatus.statusColor(),
            )
        }
        val created = DateFormatter.formatDate(order.createdAt)
        if (created.isNotBlank()) {
            Text(
                text = created,
                style = MaterialTheme.typography.labelMedium,
                color = ext.secondaryText,
            )
        }
        val paid = DateFormatter.formatDate(order.paidAt)
        if (order.isPaid && paid.isNotBlank()) {
            Text(
                text = stringResource(L10nR.string.cart_order_paid) + " · $paid",
                style = MaterialTheme.typography.labelMedium,
                color = ext.secondaryText,
            )
        }
    }
}

/** Тауарлар + Smart Calculator чекі. */
@Composable
private fun OrderItemsCard(order: Order) {
    val ext = extendedColors()
    SectionCard {
        Text(
            text = stringResource(L10nR.string.cart_order_items),
            style = MaterialTheme.typography.bodySmall,
            color = ext.primaryText,
        )
        if (order.hasItemizedBreakdown) {
            order.items.forEach { item ->
                ReceiptRow(
                    label = item.title?.takeIf { it.isNotBlank() }
                        ?: item.announcementId?.let { "#$it" }
                        ?: stringResource(L10nR.string.cart_product),
                    value = PriceFormatter.formatPrecise(item.lineTotal, order.currency ?: "₸") +
                        " ×${formatQuantity(item.quantity)}",
                )
            }
        } else {
            val title = order.announcement?.base?.title
                ?.takeIf { it.isNotBlank() }
                ?: order.firstItemTitle
                ?: stringResource(L10nR.string.cart_product)
            ReceiptRow(
                label = title,
                value = stringResource(
                    L10nR.string.cart_quantity_label,
                    formatQuantity(order.quantity) +
                        (order.measurementUnit?.let { " $it" } ?: ""),
                ),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        ReceiptRow(
            label = stringResource(L10nR.string.cart_product_price),
            value = PriceFormatter.formatPrecise(order.goodsSubtotal, order.currency ?: "₸"),
        )
        if (order.effectiveDeliveryFee > 0) {
            ReceiptRow(
                label = stringResource(L10nR.string.cart_delivery_cost),
                value = PriceFormatter.formatPrecise(order.effectiveDeliveryFee, order.currency ?: "₸"),
            )
        }
        if (order.effectiveVatAmount > 0) {
            val rate = order.vatRate ?: 0.0
            val rateText = if (rate == rate.toLong().toDouble()) {
                rate.toLong().toString()
            } else {
                rate.toString()
            }
            ReceiptRow(
                label = stringResource(L10nR.string.cart_vat_rate_label, rateText),
                value = PriceFormatter.formatPrecise(order.effectiveVatAmount, order.currency ?: "₸"),
            )
        }
        ReceiptRow(
            label = stringResource(
                if (order.isPaid) L10nR.string.cart_total_paid else L10nR.string.cart_total_amount,
            ),
            value = PriceFormatter.formatPrecise(order.orderTotal, order.currency ?: "₸"),
            emphasized = true,
        )
    }
}

/** Жеткізу/өзі алу + жүргізуші. */
@Composable
private fun DeliveryInfoCard(order: Order, driverInfo: String?) {
    val ext = extendedColors()
    SectionCard {
        val date = DateFormatter.formatDate(order.deliveryDate ?: order.loadingDate)
        if (order.isPickup) {
            Text(
                text = stringResource(L10nR.string.cart_pickup_label) +
                    (order.pickupAddress?.let { ": $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = ext.primaryText,
            )
            if (date.isNotBlank()) {
                Text(
                    text = stringResource(L10nR.string.cart_pickup_to, date),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                )
            }
        } else {
            Text(
                text = stringResource(L10nR.string.cart_delivery),
                style = MaterialTheme.typography.bodySmall,
                color = ext.primaryText,
            )
            Text(
                text = order.deliveryAddress
                    ?: stringResource(L10nR.string.cart_address_not_specified),
                style = MaterialTheme.typography.labelMedium,
                color = ext.secondaryText,
            )
        }
        if (!driverInfo.isNullOrBlank()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = driverInfo,
                style = MaterialTheme.typography.labelMedium,
                color = ext.secondaryText,
            )
        }
    }
}

/** Timeline — өзі алу / жеткізу 4 қадамы; болдырылған — жалғыз қызыл қадам. */
@Composable
private fun TimelineCard(order: Order) {
    val ext = extendedColors()
    SectionCard {
        Text(
            text = stringResource(L10nR.string.cart_order_details),
            style = MaterialTheme.typography.bodySmall,
            color = ext.primaryText,
        )
        Spacer(Modifier.height(10.dp))
        val labels = timelineLabels(order)
        if (order.orderStatus == OrderStatus.CANCELLED) {
            TimelineStep(
                label = stringResource(L10nR.string.cart_order_cancelled),
                done = true,
                isLast = true,
                failed = true,
            )
        } else {
            val doneCount = timelineDoneCount(order)
            labels.forEachIndexed { index, labelRes ->
                TimelineStep(
                    label = stringResource(labelRes),
                    done = index < doneCount,
                    isLast = index == labels.lastIndex,
                )
            }
        }
    }
}

private fun timelineLabels(order: Order): List<Int> = if (order.isPickup) {
    listOf(
        L10nR.string.cart_order_paid,
        L10nR.string.cart_seller_confirmed,
        L10nR.string.cart_ready_for_pickup,
        L10nR.string.cart_picked_up,
    )
} else {
    listOf(
        L10nR.string.cart_order_paid,
        L10nR.string.cart_handed_to_logistics,
        L10nR.string.cart_in_transit,
        L10nR.string.cart_delivered_status,
    )
}

/** Ағымдағы статусқа сәйкес аяқталған қадам саны. */
private fun timelineDoneCount(order: Order): Int = if (order.isPickup) {
    when (order.orderStatus) {
        OrderStatus.SUPPLIER_QUERY, OrderStatus.PAID -> 1
        OrderStatus.SUPPLIER_CONFIRMED, OrderStatus.LOGISTICS_SEARCH,
        OrderStatus.LOGISTICS_ASSIGNED, OrderStatus.LOGISTICS_ENROUTE,
        OrderStatus.LOADING, OrderStatus.LOADED,
        -> 3
        OrderStatus.DELIVERED, OrderStatus.ACT_SIGNED -> 4
        OrderStatus.CANCELLED -> 0
    }
} else {
    when (order.orderStatus) {
        OrderStatus.SUPPLIER_QUERY, OrderStatus.PAID, OrderStatus.SUPPLIER_CONFIRMED,
        OrderStatus.LOGISTICS_SEARCH, OrderStatus.LOGISTICS_ASSIGNED,
        -> 2
        OrderStatus.LOGISTICS_ENROUTE, OrderStatus.LOADING, OrderStatus.LOADED -> 3
        OrderStatus.DELIVERED, OrderStatus.ACT_SIGNED -> 4
        OrderStatus.CANCELLED -> 0
    }
}

@Composable
private fun TimelineStep(
    label: String,
    done: Boolean,
    isLast: Boolean,
    failed: Boolean = false,
) {
    val ext = extendedColors()
    val accent = when {
        failed -> Color0Red
        done -> Color0Green
        else -> ext.divider
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(24.dp)
                        .background(if (done) accent.copy(alpha = 0.4f) else ext.divider),
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (done || failed) ext.primaryText else ext.secondaryText,
        )
    }
}

private val Color0Green = androidx.compose.ui.graphics.Color(0xFF388E3C)
private val Color0Red = androidx.compose.ui.graphics.Color(0xFFD32F2F)

/** Сатушы карточкасы: аватар + аты + қоңырау. */
@Composable
private fun SellerCard(order: Order) {
    val ext = extendedColors()
    val context = LocalContext.current
    val seller = order.seller
    SectionCard {
        Text(
            text = stringResource(L10nR.string.cart_seller_info),
            style = MaterialTheme.typography.bodySmall,
            color = ext.primaryText,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ext.grey),
            ) {
                CachedImage(url = seller?.avatarUrl, contentDescription = seller?.name)
            }
            Spacer(Modifier.size(12.dp))
            Text(
                text = seller?.name?.takeIf { it.isNotBlank() }
                    ?: order.announcement?.base?.authorId?.toString()
                    ?: stringResource(L10nR.string.cart_seller),
                style = MaterialTheme.typography.bodySmall,
                color = ext.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            val phone = seller?.phone?.takeIf { it.isNotBlank() }
            if (phone != null) {
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    context.startActivity(intent)
                }) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = stringResource(L10nR.string.cart_call_seller),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** Төлем / болдырмау / қабылдау / қайта тапсырыс батырмалары. */
@Composable
private fun ActionButtons(
    order: Order,
    loading: Boolean,
    onCancel: () -> Unit,
    onConfirmReceipt: () -> Unit,
    onReorder: () -> Unit,
    onPay: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // «Төлеу» — төленбеген және supplier_query/supplier_confirmed/logistics_search
        // күйлерінде (Flutter _canPay) → PaymentMethodSheet.
        val canPay = order.orderPaymentStatus != PaymentStatus.PAID &&
            order.orderStatus in listOf(
                OrderStatus.SUPPLIER_QUERY,
                OrderStatus.SUPPLIER_CONFIRMED,
                OrderStatus.LOGISTICS_SEARCH,
            )
        if (canPay) {
            AgroButton(
                text = stringResource(L10nR.string.payment_pay_now),
                onClick = onPay,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (order.canConfirmReceipt) {
            AgroButton(
                text = stringResource(L10nR.string.cart_confirm_receipt),
                onClick = onConfirmReceipt,
                loading = loading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val canCancel = order.orderStatus == OrderStatus.SUPPLIER_QUERY ||
            order.orderStatus == OrderStatus.PAID
        if (canCancel) {
            AgroButton(
                text = stringResource(L10nR.string.cart_cancel_order),
                onClick = onCancel,
                enabled = !loading,
                containerColor = extendedColors().grey,
                contentColor = extendedColors().primaryText,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val canReorder = order.orderStatus in listOf(
            OrderStatus.DELIVERED,
            OrderStatus.ACT_SIGNED,
            OrderStatus.CANCELLED,
        )
        if (canReorder) {
            TextButton(onClick = onReorder, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(L10nR.string.cart_add_to_cart_short))
            }
        }
    }
}

/** Жалпы карточка контейнері. */
@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}

/** Чек жолы — атау солда, сома оңда. */
@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    emphasized: Boolean = false,
) {
    val ext = extendedColors()
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = if (emphasized) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelMedium,
            color = if (emphasized) ext.primaryText else ext.secondaryText,
            modifier = Modifier.weight(1f),
        )
        if (value.isNotBlank()) {
            Text(
                text = value,
                style = if (emphasized) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelMedium,
                color = if (emphasized) MaterialTheme.colorScheme.primary else ext.primaryText,
            )
        }
    }
}