package com.agroland.feature.cart.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroCheckbox
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.ShimmerCard
import com.agroland.core.ui.components.StatusChip
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.cart.data.CartItem
import com.agroland.feature.cart.data.CartSection
import com.agroland.feature.cart.data.Order
import com.agroland.feature.cart.data.canConfirmReceipt
import com.agroland.feature.china.data.ChinaOrder
import com.agroland.feature.china.ui.ChinaCartItemTile
import com.agroland.feature.china.ui.ChinaCheckoutSheet
import com.agroland.feature.china.ui.ChinaEvent
import com.agroland.feature.china.ui.ChinaOrderTile
import com.agroland.feature.china.ui.ChinaCartViewModel
import com.agroland.feature.china.ui.ChinaOrdersViewModel
import com.agroland.feature.china.ui.displayText as chinaErrorDisplayText
import com.agroland.feature.payment.ui.HalykLaunch
import com.agroland.feature.payment.ui.PaymentMethodSheet
import kotlinx.coroutines.launch

/** Чекауттан кейінгі төлем парағының жүктемесі. */
private data class PaymentLaunch(
    val orderIds: List<Long>,
    val totalAmount: Double,
)

/**
 * CartPage — Flutter cart_page: 5 бөлім (себет/төленді/күтілуде/жеткізілген/тарих),
 * себет тақталары + оптимистік сан/өшіру, тапсырыс бөлімдері, чекаут төменнен,
 * чекауттан кейін төлем парағы (Фаза 9: баланспен/картамен төлеу).
 */
@Composable
fun CartPage(
    onOpenOrder: (Long) -> Unit,
    onOpenAnnouncement: (Long) -> Unit = {},
    onOpenHalyk: (HalykLaunch) -> Unit = {},
    viewModel: CartViewModel = hiltViewModel(),
    chinaCartViewModel: ChinaCartViewModel = hiltViewModel(),
    chinaOrdersViewModel: ChinaOrdersViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val ordersLoading by viewModel.ordersLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val section by viewModel.section.collectAsState()
    val sectionOrders by viewModel.sectionOrders.collectAsState()
    val checkout by viewModel.checkout.collectAsState()
    val supplierPickerVisible by viewModel.supplierPickerVisible.collectAsState()
    val supplierGroups by viewModel.supplierGroups.collectAsState()

    // ── Қытай себеті (MercuryX) — Фаза 17: бөлек таңдау жиыны, аралас
    // таңдау блокталады (china_select_one_type). ──
    val chinaItems by chinaCartViewModel.items.collectAsState()
    val chinaCartLoading by chinaCartViewModel.loading.collectAsState()
    val chinaOrders by chinaOrdersViewModel.orders.collectAsState()
    val chinaOrdersLoading by chinaOrdersViewModel.loading.collectAsState()
    var selectedChinaIds by remember { mutableStateOf(setOf<Long>()) }
    var chinaCheckoutVisible by remember { mutableStateOf(false) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val shareHeader = stringResource(L10nR.string.cart_share_header)
    val noInternetText = stringResource(L10nR.string.error_no_internet)
    val genericErrorText = stringResource(L10nR.string.error_generic_message)
    val addedToast = stringResource(L10nR.string.cart_added_to_cart_toast)
    val reorderToast = stringResource(L10nR.string.cart_reorder_success)
    val receiptToast = stringResource(L10nR.string.cart_receipt_confirmed)
    val mixedSelectionText = stringResource(L10nR.string.china_select_one_type)
    val balance by viewModel.balance.collectAsState()
    // Чекауттан кейін төлем парағы (Flutter PaymentMethodSheet.show).
    var paymentLaunch by remember { mutableStateOf<PaymentLaunch?>(null) }
    // Баланспен төлеу сәтті — қысқа растау диалогі.
    var balancePaidDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CartEvent.ShowError ->
                    snackbar.showSnackbar(event.error.displayText(noInternetText, genericErrorText))
                is CartEvent.CheckoutDone ->
                    paymentLaunch = PaymentLaunch(event.orderIds, event.totalAmount)
                CartEvent.AddedToCart -> snackbar.showSnackbar(addedToast)
                CartEvent.ReorderDone -> snackbar.showSnackbar(reorderToast)
                CartEvent.ReceiptConfirmed -> snackbar.showSnackbar(receiptToast)
                CartEvent.OrderCancelled -> Unit
            }
        }
    }
    LaunchedEffect(Unit) {
        chinaCartViewModel.events.collect { event ->
            if (event is ChinaEvent.ShowError) {
                snackbar.showSnackbar(
                    event.error.chinaErrorDisplayText(noInternetText, genericErrorText),
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SectionSelector(
                section = section,
                basketCount = items.size + chinaItems.size,
                sectionOrders = sectionOrders,
                onSelect = viewModel::setSection,
            )
            Box(modifier = Modifier.weight(1f)) {
                when {
                    loading -> Column { repeat(5) { ShimmerCard() } }
                    error != null -> CenteredContent {
                        ErrorWithRetry(onRetry = viewModel::refresh, message = error!!.displayText())
                    }
                    else -> when (section) {
                        CartSection.BASKET_ITEMS -> BasketContent(
                            items = items,
                            selectedIds = selectedIds,
                            chinaItems = chinaItems,
                            chinaCartLoading = chinaCartLoading,
                            selectedChinaIds = selectedChinaIds,
                            onToggle = viewModel::toggleSelected,
                            onToggleSelectAll = viewModel::toggleSelectAll,
                            onQuantity = viewModel::changeQuantity,
                            onDelete = viewModel::deleteItem,
                            onCheckout = viewModel::startCheckout,
                            onToggleChina = { id ->
                                selectedChinaIds = if (id in selectedChinaIds) {
                                    selectedChinaIds - id
                                } else {
                                    selectedChinaIds + id
                                }
                            },
                            onToggleSelectAllChina = {
                                val all = selectedChinaIds == chinaItems.map { it.id }.toSet()
                                selectedChinaIds = if (all) emptySet() else chinaItems.map { it.id }.toSet()
                            },
                            onChinaQuantity = { item, newQuantity ->
                                if (newQuantity != item.quantity) {
                                    chinaCartViewModel.updateQuantity(item.id, newQuantity)
                                }
                            },
                            onDeleteChina = { item -> chinaCartViewModel.deleteItem(item.id) },
                            onShare = {
                                val text = viewModel.buildShareText(shareHeader)
                                if (text.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    }
                                    context.startActivity(Intent.createChooser(intent, shareHeader))
                                }
                            },
                            // Аралас таңдау мүмкін емес — тек бір түр таңдалады.
                            onProceed = {
                                val localSelected = selectedIds.isNotEmpty()
                                val chinaSelected = selectedChinaIds.isNotEmpty()
                                when {
                                    localSelected && chinaSelected ->
                                        scope.launch { snackbar.showSnackbar(mixedSelectionText) }
                                    chinaSelected -> chinaCheckoutVisible = true
                                    localSelected -> viewModel.startCheckout()
                                }
                            },
                        )
                        else -> OrdersSectionContent(
                            section = section,
                            orders = sectionOrders[section].orEmpty(),
                            loading = ordersLoading,
                            chinaOrders = if (section == CartSection.ORDER_HISTORY) chinaOrders else emptyList(),
                            chinaOrdersLoading = chinaOrdersLoading,
                            onOpenOrder = onOpenOrder,
                            onConfirmReceipt = viewModel::confirmReceipt,
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (checkout.visible) {
        CheckoutSheet(
            state = checkout,
            onDismiss = viewModel::dismissCheckout,
            onToggleDelivery = viewModel::toggleItemDelivery,
            onSetZone = viewModel::setItemZone,
            onSelectAddress = viewModel::selectAddress,
            onSubmit = viewModel::submitCheckout,
        )
    }
    if (chinaCheckoutVisible) {
        ChinaCheckoutSheet(
            items = chinaItems,
            selectedIds = selectedChinaIds,
            onDismiss = {
                chinaCheckoutVisible = false
                // Парақ жабылғанда Қытай себеті мен тапсырыстары жаңартылады.
                selectedChinaIds = emptySet()
                chinaCartViewModel.refresh()
                chinaOrdersViewModel.load()
            },
            viewModel = chinaOrdersViewModel,
        )
    }
    if (supplierPickerVisible) {
        SupplierPickerSheet(
            groups = supplierGroups,
            onChoose = viewModel::chooseSupplier,
            onDismiss = viewModel::dismissSupplierPicker,
        )
    }

    // ── Төлем парағы: баланспен төлеу немесе Halyk ePay картасы (Фаза 9) ──
    val launch = paymentLaunch
    if (launch != null) {
        PaymentMethodSheet(
            orderIds = launch.orderIds,
            totalAmount = launch.totalAmount,
            balance = balance,
            onDismiss = {
                paymentLaunch = null
                // Парақ жабылғанда себет/тапсырыстар сервер күйіне келеді.
                viewModel.refresh()
            },
            onBalancePaid = {
                paymentLaunch = null
                balancePaidDialog = true
                viewModel.refresh()
            },
            onHalykReady = { halyk ->
                paymentLaunch = null
                viewModel.refresh()
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

/** Үстіңгі дөңгелек бөлім таңдаушы — 5 бөлім + санда. */
@Composable
private fun SectionSelector(
    section: CartSection,
    basketCount: Int,
    sectionOrders: Map<CartSection, List<Order>>,
    onSelect: (CartSection) -> Unit,
) {
    val ext = extendedColors()
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(ext.card)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Spacer(Modifier.width(16.dp)) }
        items(CartSection.entries) { entry ->
            val count = when (entry) {
                CartSection.BASKET_ITEMS -> basketCount
                else -> sectionOrders[entry]?.size ?: 0
            }
            SectionChip(
                label = stringResource(sectionLabelRes(entry)),
                count = count,
                selected = entry == section,
                onClick = { onSelect(entry) },
            )
        }
        item { Spacer(Modifier.width(16.dp)) }
    }
}

private fun sectionLabelRes(section: CartSection): Int = when (section) {
    CartSection.BASKET_ITEMS -> L10nR.string.cart_section_basket
    CartSection.PAID_PENDING -> L10nR.string.cart_section_paid
    CartSection.IN_PROGRESS -> L10nR.string.cart_section_pending
    CartSection.DELIVERED -> L10nR.string.cart_section_delivered
    CartSection.ORDER_HISTORY -> L10nR.string.cart_section_history
}

@Composable
private fun SectionChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val ext = extendedColors()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else ext.grey)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = if (count > 0) "$label ($count)" else label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) ext.white else ext.primaryText,
            maxLines = 1,
        )
    }
}

/** Себет бөлімі: жергілікті тақталар + Қытай бөлімі + төлем жолағы. */
@Composable
private fun BasketContent(
    items: List<CartItem>,
    selectedIds: Set<Long>,
    chinaItems: List<com.agroland.feature.china.data.ChinaCartItem>,
    chinaCartLoading: Boolean,
    selectedChinaIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onToggleSelectAll: () -> Unit,
    onQuantity: (CartItem, Double) -> Unit,
    onDelete: (CartItem) -> Unit,
    onCheckout: () -> Unit,
    onToggleChina: (Long) -> Unit,
    onToggleSelectAllChina: () -> Unit,
    onChinaQuantity: (com.agroland.feature.china.data.ChinaCartItem, Int) -> Unit,
    onDeleteChina: (com.agroland.feature.china.data.ChinaCartItem) -> Unit,
    onShare: () -> Unit,
    onProceed: () -> Unit,
) {
    if (items.isEmpty() && chinaItems.isEmpty()) {
        CenteredContent {
            EmptyView(
                icon = Icons.Outlined.ShoppingCart,
                title = stringResource(L10nR.string.cart_empty),
                message = stringResource(L10nR.string.cart_empty_hint),
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(L10nR.string.cart_select_all),
                style = MaterialTheme.typography.labelLarge,
                color = extendedColors().primaryText,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onToggleSelectAll)
                    .padding(vertical = 8.dp),
            )
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    tint = extendedColors().secondaryText,
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(items, key = { it.id }) { item ->
                CartTile(
                    item = item,
                    selected = item.id in selectedIds,
                    onToggle = { onToggle(item.id) },
                    onQuantity = { delta -> onQuantity(item, (item.quantity + delta).coerceAtLeast(1.0)) },
                    onDelete = { onDelete(item) },
                )
            }
            if (items.isNotEmpty()) {
                item { Spacer(Modifier.height(8.dp)) }
            }
            if (chinaItems.isNotEmpty()) {
                // ── Қытай бөлімі (MercuryX) — өз таңдау жиынымен. ──
                item(key = "china_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(L10nR.string.china_cart_section),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(L10nR.string.cart_select_all),
                            style = MaterialTheme.typography.labelMedium,
                            color = extendedColors().secondaryText,
                            modifier = Modifier
                                .clickable(onClick = onToggleSelectAllChina)
                                .padding(vertical = 8.dp),
                        )
                    }
                }
                items(chinaItems, key = { "china_${it.id}" }) { chinaItem ->
                    Row(
                        // Tile өзіне horizontal 16dp қосады — тек сол жақ шеті қажет.
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AgroCheckbox(
                            checked = chinaItem.id in selectedChinaIds,
                            onCheckedChange = { onToggleChina(chinaItem.id) },
                        )
                        ChinaCartItemTile(
                            item = chinaItem,
                            onQuantityChange = { newQuantity -> onChinaQuantity(chinaItem, newQuantity) },
                            onDelete = { onDeleteChina(chinaItem) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        Surface(color = extendedColors().card) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (selectedIds.isNotEmpty() || selectedChinaIds.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            L10nR.string.cart_items_selected,
                            selectedIds.size + selectedChinaIds.size,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = extendedColors().secondaryText,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                AgroButton(
                    // Қытай таңдалса — «Тапсырыс (MercuryX)», әйтпесе төлем парағы.
                    text = stringResource(
                        if (selectedIds.isEmpty() && selectedChinaIds.isNotEmpty()) {
                            L10nR.string.china_checkout
                        } else {
                            L10nR.string.cart_proceed_to_payment
                        },
                    ),
                    onClick = onProceed,
                    enabled = selectedIds.isNotEmpty() || selectedChinaIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Себет тақтасы: құсбелгі + сурет + атау/баға + сан + өшіру. */
@Composable
private fun CartTile(
    item: CartItem,
    selected: Boolean,
    onToggle: () -> Unit,
    onQuantity: (Double) -> Unit,
    onDelete: () -> Unit,
) {
    val ext = extendedColors()
    val base = item.announcement?.base
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AgroCheckbox(checked = selected, onCheckedChange = { onToggle() })
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ext.grey),
        ) {
            CachedImage(
                url = base?.imageUrl,
                contentDescription = base?.title,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = base?.title?.takeIf { it.isNotBlank() } ?: "#${item.announcementId}",
                style = MaterialTheme.typography.bodySmall,
                color = ext.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = PriceFormatter.formatPrecise(base?.price, base?.currency ?: "₸"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            QuantityStepper(
                quantity = item.quantity,
                unit = item.measurementUnit ?: base?.measurementUnit,
                onChange = onQuantity,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = ext.secondaryText,
            )
        }
    }
}

/** Сан басқарғышы: «−» [сан бірлік] «+» (Flutter QuantitySelector). */
@Composable
private fun QuantityStepper(
    quantity: Double,
    unit: String?,
    onChange: (Double) -> Unit,
) {
    val ext = extendedColors()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(
            onClick = { onChange(-1.0) },
            enabled = quantity > 1.0,
            modifier = Modifier.size(30.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = null,
                tint = if (quantity > 1.0) ext.primaryText else ext.divider,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = formatQuantity(quantity) + (unit?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""),
            style = MaterialTheme.typography.labelLarge,
            color = ext.primaryText,
        )
        IconButton(onClick = { onChange(1.0) }, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = ext.primaryText,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

internal fun formatQuantity(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

/** Тапсырыс бөлімі: тапсырыс тақталары + «Тауарды қабылдау» (тарих — Қытаймен бірге). */
@Composable
private fun OrdersSectionContent(
    section: CartSection,
    orders: List<Order>,
    loading: Boolean,
    chinaOrders: List<ChinaOrder>,
    chinaOrdersLoading: Boolean,
    onOpenOrder: (Long) -> Unit,
    onConfirmReceipt: (Order) -> Unit,
) {
    if (loading && orders.isEmpty() && chinaOrdersLoading && chinaOrders.isEmpty()) {
        Column { repeat(4) { ShimmerCard() } }
        return
    }
    if (orders.isEmpty() && chinaOrders.isEmpty()) {
        CenteredContent {
            EmptyView(
                icon = Icons.Outlined.Inbox,
                title = stringResource(L10nR.string.cart_section_empty),
            )
        }
        return
    }
    // Тарих бөлімі — жергілікті және Қытай (MercuryX) тапсырыстары бір уақыт
    // бойынша DESC реттеліп көрсетіледі (спек §3.4).
    val historyRows = if (section == CartSection.ORDER_HISTORY) {
        val local = orders.map { HistoryRow.Local(it) }
        val china = chinaOrders.map { HistoryRow.China(it) }
        (local + china).sortedByDescending { row ->
            DateFormatter.parseOrNull(row.createdAt)
        }
    } else {
        orders.map { HistoryRow.Local(it) }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(historyRows, key = { it.key }) { row ->
            when (row) {
                is HistoryRow.Local -> OrderTile(
                    order = row.order,
                    onClick = { onOpenOrder(row.order.id) },
                    onConfirmReceipt = { onConfirmReceipt(row.order) },
                )
                is HistoryRow.China -> ChinaOrderTile(order = row.order)
            }
        }
        if (chinaOrdersLoading && chinaOrders.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator(strokeWidth = 3.dp)
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

/** Тарихтағы біріктірілген жол — жергілікті немесе Қытай тапсырысы. */
private sealed interface HistoryRow {
    val createdAt: String?
    val key: String

    data class Local(val order: Order) : HistoryRow {
        override val createdAt: String? get() = order.createdAt
        override val key: String get() = "local_${order.id}"
    }

    data class China(val order: ChinaOrder) : HistoryRow {
        override val createdAt: String? get() = order.createdAt
        override val key: String get() = "china_${order.id}"
    }
}

/** Тапсырыс тақтасы — тізім бөлімдерінде (деталь беті емес). */
@Composable
private fun OrderTile(
    order: Order,
    onClick: () -> Unit,
    onConfirmReceipt: () -> Unit,
) {
    val ext = extendedColors()
    var confirmDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ext.card)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(L10nR.string.cart_order_number, order.id),
                style = MaterialTheme.typography.labelLarge,
                color = ext.primaryText,
                modifier = Modifier.weight(1f),
            )
            StatusChip(
                text = stringResource(order.statusLabelRes()),
                color = order.orderStatus.statusColor(),
            )
        }
        val title = order.firstItemTitle
            ?: order.announcement?.base?.title?.takeIf { it.isNotBlank() }
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = ext.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val extras = order.itemsCount
            ?.takeIf { it > 1 }
            ?.let { stringResource(L10nR.string.cart_order_more_items, it - 1) }
        if (extras != null) {
            Text(
                text = extras,
                style = MaterialTheme.typography.labelMedium,
                color = ext.secondaryText,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = PriceFormatter.formatPrecise(order.orderTotal, order.currency ?: "₸"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                val date = DateFormatter.formatDate(order.createdAt)
                if (date.isNotBlank()) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.secondaryText,
                    )
                }
            }
            if (order.canConfirmReceipt) {
                AgroButton(
                    text = stringResource(L10nR.string.cart_confirm_receipt),
                    onClick = { confirmDialog = true },
                    modifier = Modifier.height(44.dp),
                )
            }
        }
    }

    if (confirmDialog) {
        AlertDialog(
            onDismissRequest = { confirmDialog = false },
            title = {
                Text(stringResource(L10nR.string.cart_confirm_receipt_question))
            },
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

/** Қонақ күйі — кіру үшін түйме (Flutter guest prompt). */
@Composable
fun GuestCartTab(onLoginClick: () -> Unit) {
    CenteredContent {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.ShoppingCart,
                contentDescription = null,
                tint = extendedColors().divider,
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(L10nR.string.cart_login_prompt),
                style = MaterialTheme.typography.bodySmall,
                color = extendedColors().secondaryText,
            )
            Spacer(Modifier.height(16.dp))
            AgroButton(
                text = stringResource(L10nR.string.auth_login_title),
                onClick = onLoginClick,
                modifier = Modifier.padding(horizontal = 48.dp).fillMaxWidth(),
            )
        }
    }
}