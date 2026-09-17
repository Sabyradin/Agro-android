package com.agroland.feature.dealer.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroIconButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.dealer.data.DealerOrder
import com.agroland.feature.dealer.data.displayText
import com.agroland.feature.dealer.ui.DealerOrdersViewModel.Event
import com.agroland.feature.dealer.ui.DealerOrdersViewModel.TabState

/**
 * Дилер тапсырыстары — Flutter DealerOrdersPage (1:1): 5 таб
 * (Жаңа/Растымды/Жолда/Жеткізілген/Бас тартылған) badge сандарымен
 * ('new' жауабының tab_counts), TeamPool иконкасы dealer_role бойынша
 * (manager|director). Әр таб өз күйін ұстайды (per-tab map).
 */
@Composable
fun DealerOrdersPage(
    initialTab: Int,
    onBack: () -> Unit,
    onOpenTeamPool: () -> Unit,
    onOpenTracking: (Long) -> Unit,
    viewModel: DealerOrdersViewModel = hiltViewModel(),
    accessViewModel: DealerAccessViewModel = hiltViewModel(),
) {
    var tabIndex by rememberSaveable { mutableStateOf(initialTab.coerceIn(0, viewModel.tabs.size - 1)) }
    val states by viewModel.states.collectAsState()
    val tabCounts by viewModel.tabCounts.collectAsState()
    val actionInProgress by viewModel.actionInProgress.collectAsState()
    val profile by accessViewModel.profile.collectAsState()

    val networkError = stringResource(L10nR.string.error_no_internet)
    val genericError = stringResource(L10nR.string.error_generic_message)
    val acceptedText = stringResource(L10nR.string.dealer_order_accepted_toast)
    val shippedText = stringResource(L10nR.string.dealer_order_shipped_toast)
    val snackbar = remember { SnackbarHostState() }

    // Бас тарту диалогы — reason мәтінімен.
    var rejectTarget by remember { mutableStateOf<Pair<DealerOrder, String>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is Event.ShowError -> snackbar.showSnackbar(event.error.displayText(networkError, genericError))
                Event.OrderAccepted -> snackbar.showSnackbar(acceptedText)
                Event.OrderShipped -> snackbar.showSnackbar(shippedText)
            }
        }
    }

    // Таб бірінші рет ашылғанда жүктеледі (Flutter family provider parity).
    LaunchedEffect(tabIndex) {
        val tab = viewModel.tabs[tabIndex]
        if (states[tab]?.loaded != true) viewModel.load(tab)
    }

    val tabLabels = listOf(
        L10nR.string.dealer_orders_new,
        L10nR.string.dealer_orders_confirmed,
        L10nR.string.dealer_orders_shipped,
        L10nR.string.dealer_orders_delivered,
        L10nR.string.dealer_orders_cancelled,
    )

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.dealer_my_orders),
                onBack = onBack,
                actions = {
                    if (profile?.canAccessTeamPool == true) {
                        AgroIconButton(
                            icon = Icons.Rounded.Groups,
                            contentDescription = stringResource(L10nR.string.dealer_team_pool),
                            onClick = onOpenTeamPool,
                        )
                    }
                },
            )
        },
    ) { modifier ->
        Column(modifier = modifier) {
            TabRow(selectedTabIndex = tabIndex) {
                viewModel.tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = {
                            val count = tabCounts[tab] ?: 0
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(tabLabels[index]))
                                if (count > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                            .padding(horizontal = 6.dp, vertical = 1.dp),
                                    ) {
                                        Text(
                                            text = badgeCount(count),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = extendedColors().white,
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                val tab = viewModel.tabs[tabIndex]
                val state = states[tab] ?: TabState()
                when {
                    state.loading -> LoadingWidget()
                    state.error != null -> CenteredContent {
                        ErrorWithRetry(
                            onRetry = { viewModel.load(tab) },
                            message = state.error!!.displayText(networkError, genericError),
                        )
                    }
                    state.items.isEmpty() -> EmptyView(
                        icon = Icons.Rounded.Inbox,
                        title = stringResource(L10nR.string.nothing_found),
                    )
                    else -> {
                        val listState = rememberLazyListState()
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(state.items, key = { it.id }) { order ->
                                DealerOrderCard(
                                    order = order,
                                    tab = tab,
                                    actionInProgress = actionInProgress == order.id,
                                    onAccept = { viewModel.accept(order.id, tab) },
                                    onReject = { rejectTarget = order to "" },
                                    onShip = { viewModel.ship(order.id, tab) },
                                    onOpenTracking = { onOpenTracking(order.id) },
                                )
                            }
                            if (state.loadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.5.dp,
                                        )
                                    }
                                }
                            }
                        }
                        InfiniteScrollEffect(listState) { viewModel.loadMore(tab) }
                    }
                }
                SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }

    // ── Бас тарту себебі диалогы (Flutter _handleReject) ──
    rejectTarget?.let { (order, _) ->
        var reason by remember(order.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { rejectTarget = null },
            title = { Text(stringResource(L10nR.string.dealer_decline)) },
            text = {
                AgroTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = stringResource(L10nR.string.dealer_reason),
                    singleLine = false,
                    maxLines = 3,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.reject(order.id, reason, viewModel.tabs[tabIndex])
                        rejectTarget = null
                    },
                ) { Text(stringResource(L10nR.string.dealer_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { rejectTarget = null }) {
                    Text(stringResource(L10nR.string.common_cancel))
                }
            },
        )
    }
}

/**
 * Тапсырыс карточкасы — Flutter _DealerOrderCard (1:1): шапка (сурет +
 * №id + статус/төлем белгілері + атау + күн + сома/саны) және ашылатын
 * бөлім (деталь торы, зоналар, қабылдау/бас тарту/жіберу + трекинг).
 */
@Composable
private fun DealerOrderCard(
    order: DealerOrder,
    tab: String,
    actionInProgress: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onShip: () -> Unit,
    onOpenTracking: () -> Unit,
) {
    val ext = extendedColors()
    var expanded by rememberSaveable(order.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .border(0.5.dp, ext.divider, RoundedCornerShape(14.dp)),
    ) {
        // ── Шапка ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                if (order.mainImageUrl != null) {
                    CachedImage(
                        url = order.mainImageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.ShoppingBag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "№" + order.id,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ext.primaryText,
                    )
                    OrderStatusBadge(order.status)
                    order.paymentStatus?.let { PaymentBadge(it) }
                }
                order.title?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = DateFormatter.formatDateTime(order.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.secondaryText,
                )
            }
            // Сома бағаны — өте үлкен сандарда екі жолға сынатын (maxWidth 110).
            Column(
                modifier = Modifier.widthIn(max = 110.dp),
                horizontalAlignment = Alignment.End,
            ) {
                order.totalAmount?.let { amount ->
                    Text(
                        text = PriceFormatter.format(amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ext.primaryText,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (order.quantity != 0.0) {
                    val unitRes = measurementUnitLabelRes(order.measurementUnit)
                    Text(
                        text = if (unitRes != null) {
                            "${formatQuantity(order.quantity)} ${stringResource(unitRes)}"
                        } else {
                            formatQuantity(order.quantity)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.secondaryText,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = if (expanded) {
                    Icons.Rounded.KeyboardArrowUp
                } else {
                    Icons.Rounded.KeyboardArrowDown
                },
                contentDescription = null,
                tint = ext.secondaryText,
                modifier = Modifier.size(20.dp),
            )
        }

        // ── Ашылатын бөлім ──
        if (expanded) {
            HorizontalDivider(color = ext.divider)
            Column(
                modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 14.dp),
            ) {
                OrderDetailGrid(order)

                if (order.deliveryZones.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(L10nR.string.dealer_delivery_zones),
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.secondaryText,
                    )
                    Spacer(Modifier.height(6.dp))
                    order.deliveryZones.forEach { zone ->
                        OrderZoneRow(zone.displayName, zone.deliveryDaysMin, zone.deliveryDaysMax, zone.deliveryCost)
                    }
                }

                if (tab == "new") {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onReject,
                            enabled = !actionInProgress,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(L10nR.string.dealer_decline),
                                fontSize = 14.sp,
                            )
                        }
                        Button(
                            onClick = onAccept,
                            enabled = !actionInProgress,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = ext.white,
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            if (actionInProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = ext.white,
                                )
                            } else {
                                Text(
                                    text = stringResource(L10nR.string.dealer_accept),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.W600,
                                )
                            }
                        }
                    }
                }
                if (tab == "confirmed") {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onShip,
                        enabled = !actionInProgress,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = ext.white,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (actionInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = ext.white,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.LocalShipping,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(L10nR.string.dealer_ship_order),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.W600,
                            )
                        }
                    }
                }
                // Android жақсартуы: трекинг бетіне тікелей жол (Flutter-де
                // route тіркелген, бірақ еш жерден ашылмайды — ISSUES).
                TextButton(onClick = onOpenTracking) {
                    Text(stringResource(L10nR.string.dealer_tracking))
                }
            }
        }
    }
}

/** Деталь элементі (тордың бір ұяшығы) — мәтіні қажет болса tel: dial intent. */
private data class DetailItemData(
    val label: String,
    val value: String,
    val fullWidth: Boolean = false,
    val tel: String? = null,
)

/** Деталь торы — екі бағанды жолдар, «Ескертпелер» толық енімен. */
@Composable
private fun OrderDetailGrid(order: DealerOrder) {
    val context = LocalContext.current

    val items = buildList {
        order.buyerName?.let { add(DetailItemData(stringResource(L10nR.string.dealer_buyer), it)) }
        order.buyerPhone?.let {
            add(DetailItemData(stringResource(L10nR.string.dealer_phone), it, tel = it))
        }
        order.agreedPrice?.let {
            add(DetailItemData(stringResource(L10nR.string.dealer_agreed_price), PriceFormatter.format(it)))
        }
        order.pickupAddress?.let { add(DetailItemData(stringResource(L10nR.string.dealer_pickup_address), it)) }
        order.deliveryAddress?.let { add(DetailItemData(stringResource(L10nR.string.dealer_delivery_address), it)) }
        order.loadingDate?.let {
            add(DetailItemData(stringResource(L10nR.string.dealer_loading_date), DateFormatter.formatDateTime(it)))
        }
        order.deliveryDate?.let {
            add(DetailItemData(stringResource(L10nR.string.dealer_delivery_date), DateFormatter.formatDateTime(it)))
        }
        order.paidAt?.let {
            add(DetailItemData(stringResource(L10nR.string.dealer_paid_at), DateFormatter.formatDateTime(it)))
        }
        order.notes?.let {
            add(DetailItemData(stringResource(L10nR.string.dealer_notes), it, fullWidth = true))
        }
    }
    if (items.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var index = 0
        while (index < items.size) {
            val item = items[index]
            if (item.fullWidth || index == items.size - 1) {
                DetailItem(item, context)
                index++
            } else {
                val next = items[index + 1]
                if (next.fullWidth) {
                    DetailItem(item, context)
                    index++
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) { DetailItem(item, context) }
                        Box(modifier = Modifier.weight(1f)) { DetailItem(next, context) }
                    }
                    index += 2
                }
            }
        }
    }
}

@Composable
private fun DetailItem(item: DetailItemData, context: android.content.Context) {
    val ext = extendedColors()
    Column {
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = ext.secondaryText,
        )
        Spacer(Modifier.height(2.dp))
        if (item.tel != null) {
            // Телефон — түпкі мәтінді түрліктеу: басу → dial intent.
            Text(
                text = item.value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + item.tel)))
                },
            )
        } else {
            Text(
                text = item.value,
                style = MaterialTheme.typography.bodySmall,
                color = ext.primaryText,
            )
        }
    }
}

/** Зона жолы — атау + күндер + баға (Flutter _ZoneRow). */
@Composable
private fun OrderZoneRow(name: String, daysMin: Int?, daysMax: Int?, cost: Double?) {
    val ext = extendedColors()
    val days = when {
        daysMin != null && daysMax != null && daysMax != daysMin ->
            "$daysMin–$daysMax " + stringResource(L10nR.string.dealer_days)
        daysMin != null -> "$daysMin " + stringResource(L10nR.string.dealer_days)
        else -> null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ext.backgroundLight)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.W500,
                color = ext.primaryText,
            )
            if (days != null) {
                Text(
                    text = days,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = ext.secondaryText,
                )
            }
        }
        Text(
            text = cost?.let { PriceFormatter.format(it) } ?: "—",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.W600,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Статус белгісі — түсі 0.12 alpha, мәтіні түстің өзі. */
@Composable
private fun OrderStatusBadge(status: String) {
    val color = dealerOrderStatusColor(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = stringResource(dealerOrderStatusLabelRes(status)),
            fontSize = 10.sp,
            fontWeight = FontWeight.W600,
            color = color,
            maxLines = 1,
        )
    }
}

/** Төлем статусы: төленді (жасыл) / қайтарылды (сұр) / төленбеген (қызғылт сары). */
@Composable
private fun PaymentBadge(status: String) {
    val color = dealerPaymentStatusColor(status)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = stringResource(dealerPaymentStatusLabelRes(status)),
            fontSize = 10.sp,
            fontWeight = FontWeight.W600,
            color = color,
            maxLines = 1,
        )
    }
}

/** Санты — бүтін болса «.0» жоқ (1.0 → «1», 2.5 → «2,5»). */
private fun formatQuantity(value: Double): String =
    if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        value.toString().replace('.', ',')
    }