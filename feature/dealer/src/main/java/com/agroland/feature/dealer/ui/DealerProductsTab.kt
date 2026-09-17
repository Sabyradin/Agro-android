package com.agroland.feature.dealer.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroChip
import com.agroland.core.ui.components.AgroSearchField
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.dealer.data.DealerProduct
import com.agroland.feature.dealer.data.displayText
import com.agroland.feature.dealer.ui.DealerProductsViewModel.Event
import kotlinx.coroutines.launch

/**
 * Өнімдер табы — Flutter DealerActiveProductsTab (1:1): іздеу (debounce 500ms),
 * 4 статус чипі, карточка (сурет + баға + SKU + статистика) және
 * әрекеттер жолағы (белсендіру/өшіру/себеп/өңдеу/жою).
 */
@Composable
fun DealerProductsTab(
    onEditProduct: (Long) -> Unit,
    viewModel: DealerProductsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val loadingMore by viewModel.loadingMore.collectAsState()
    val error by viewModel.error.collectAsState()
    val status by viewModel.status.collectAsState()
    val search by viewModel.search.collectAsState()
    val actionInProgress by viewModel.actionInProgress.collectAsState()

    val networkError = stringResource(L10nR.string.error_no_internet)
    val genericError = stringResource(L10nR.string.error_generic_message)
    val deletedText = stringResource(L10nR.string.ad_deleted_toast)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Диалог күйлері — state-driven (AlertDialog композицияда рендерленеді).
    var deleteCandidate by remember { mutableStateOf<DealerProduct?>(null) }
    var rejectMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is Event.ShowError -> scope.launch {
                    snackbar.showSnackbar(event.error.displayText(networkError, genericError))
                }
                is Event.ShowRejectMessage -> rejectMessage = event.message
                Event.ProductDeleted -> scope.launch { snackbar.showSnackbar(deletedText) }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AgroSearchField(
            value = search,
            onValueChange = viewModel::setSearch,
            hint = stringResource(L10nR.string.search_hint),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        )
        StatusFilterRow(
            selected = status,
            onSelect = viewModel::setStatus,
        )
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                loading -> LoadingWidget()
                error != null -> CenteredContent {
                    ErrorWithRetry(onRetry = viewModel::refresh, message = error!!.displayText(networkError, genericError))
                }
                items.isEmpty() -> EmptyView(
                    icon = Icons.Rounded.RemoveRedEye,
                    title = stringResource(L10nR.string.nothing_found),
                )
                else -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items, key = { it.id }) { product ->
                            DealerProductCard(
                                product = product,
                                actionInProgress = actionInProgress == product.id,
                                onActivate = { viewModel.activate(product.id) },
                                onDeactivate = { viewModel.deactivate(product.id) },
                                onRejectReason = { viewModel.showRejectReason(product.id) },
                                onEdit = { onEditProduct(product.id) },
                                onDelete = { deleteCandidate = product },
                            )
                        }
                        if (loadingMore) {
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
                    InfiniteScrollEffect(listState, viewModel::loadMore)
                }
            }
            SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    deleteCandidate?.let { product ->
        ConfirmDialog(
            title = stringResource(L10nR.string.common_delete),
            message = product.title,
            confirmLabel = stringResource(L10nR.string.common_delete),
            onDismiss = { deleteCandidate = null },
            onConfirm = {
                viewModel.delete(product.id)
                deleteCandidate = null
            },
        )
    }
    rejectMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { rejectMessage = null },
            title = { Text(stringResource(L10nR.string.my_ads_status_rejected)) },
            text = { Text(message.ifEmpty { stringResource(L10nR.string.nothing_found) }) },
            confirmButton = {
                TextButton(onClick = { rejectMessage = null }) {
                    Text(stringResource(L10nR.string.common_close))
                }
            },
        )
    }
}

/** 4 статус чипі: active/pending/inactive/rejected (my_ads_status_*). */
@Composable
private fun StatusFilterRow(selected: String, onSelect: (String) -> Unit) {
    val statuses = listOf("active", "pending", "inactive", "rejected")
    val labels = listOf(
        L10nR.string.my_ads_status_active,
        L10nR.string.my_ads_status_pending,
        L10nR.string.my_ads_status_inactive,
        L10nR.string.my_ads_status_rejected,
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(statuses) { index, status ->
            AgroChip(
                text = stringResource(labels[index]),
                selected = selected == status,
                onClick = { onSelect(status) },
            )
        }
    }
}

/** Өнім карточкасы — сурет + тақырып/статус + баға/VIP + SKU + статистика + әрекеттер. */
@Composable
private fun DealerProductCard(
    product: DealerProduct,
    actionInProgress: Boolean,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onRejectReason: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .border(0.5.dp, ext.divider, RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProductImage(product)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.W600,
                        color = ext.primaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    ProductStatusBadge(status = product.status)
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = PriceFormatter.format(product.price, currencySymbol(product.currency)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (product.isVip) {
                        VipBadge()
                    }
                }
                product.sku?.let { sku ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(L10nR.string.dealer_sku) + ": " + sku,
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.secondaryText,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Stat(icon = Icons.Rounded.RemoveRedEye, count = product.viewsCount)
                    Spacer(Modifier.width(10.dp))
                    Stat(icon = Icons.Rounded.Phone, count = product.callsCount)
                    Spacer(Modifier.width(10.dp))
                    Stat(icon = Icons.Rounded.FavoriteBorder, count = product.favoritesCount)
                    Spacer(Modifier.weight(1f))
                    product.createdAt?.let {
                        Text(
                            text = DateFormatter.formatDate(it),
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.secondaryText,
                        )
                    }
                }
                if (product.deliveryAvailable || product.pickupAvailable) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (product.deliveryAvailable) {
                            MiniBadge(stringResource(L10nR.string.dealer_delivery))
                        }
                        if (product.pickupAvailable) {
                            MiniBadge(stringResource(L10nR.string.detail_pickup))
                        }
                    }
                }
            }
        }

        // ── Әрекеттер жолағы ──
        HorizontalDivider(color = ext.divider)
        Row(modifier = Modifier.fillMaxWidth()) {
            when (product.status) {
                "active" -> ActionButton(
                    icon = Icons.Rounded.PauseCircle,
                    label = stringResource(L10nR.string.ad_action_deactivate),
                    color = Color(0xFFFF9800),
                    loading = actionInProgress,
                    onClick = onDeactivate,
                )
                "inactive" -> ActionButton(
                    icon = Icons.Rounded.PlayArrow,
                    label = stringResource(L10nR.string.ad_action_activate),
                    color = MaterialTheme.colorScheme.primary,
                    loading = actionInProgress,
                    onClick = onActivate,
                )
                "rejected" -> ActionButton(
                    icon = Icons.Rounded.Info,
                    label = stringResource(L10nR.string.dealer_cause_reject),
                    color = Color(0xFFFF9800),
                    loading = actionInProgress,
                    onClick = onRejectReason,
                )
                else -> Text(
                    text = stringResource(L10nR.string.my_ads_status_pending),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.secondaryText,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            if (product.status != "rejected") {
                ActionButton(
                    icon = Icons.Rounded.Edit,
                    label = stringResource(L10nR.string.common_edit),
                    color = ext.secondaryText,
                    loading = actionInProgress,
                    onClick = onEdit,
                )
            }
            if (product.status == "inactive" || product.status == "rejected") {
                ActionButton(
                    icon = Icons.Rounded.Delete,
                    label = stringResource(L10nR.string.common_delete),
                    color = MaterialTheme.colorScheme.error,
                    loading = actionInProgress,
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun ProductImage(product: DealerProduct) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(extendedColors().grey),
        contentAlignment = Alignment.Center,
    ) {
        if (product.mainImageUrl.isNotEmpty()) {
            CachedImage(
                url = product.mainImageUrl,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.RemoveRedEye,
                contentDescription = null,
                tint = extendedColors().secondaryText,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/** Статус белгісі — нүкте + мәтін (Flutter _statusConfig түстері). */
@Composable
private fun ProductStatusBadge(status: String) {
    val color = when (status) {
        "active" -> MaterialTheme.colorScheme.primary
        "pending" -> Color(0xFFFF9800)
        "inactive" -> Color(0xFF9E9E9E)
        "rejected" -> Color(0xFFE53935)
        else -> Color(0xFF9E9E9E)
    }
    val label = when (status) {
        "active" -> L10nR.string.my_ads_status_active
        "pending" -> L10nR.string.my_ads_status_pending
        "inactive" -> L10nR.string.my_ads_status_inactive
        "rejected" -> L10nR.string.my_ads_status_rejected
        else -> L10nR.string.order_status_unknown
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(color))
            Text(
                text = stringResource(label),
                fontSize = 10.sp,
                fontWeight = FontWeight.W600,
                color = color,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun VipBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFFD700).copy(alpha = 0.2f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            text = "VIP",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFA000),
        )
    }
}

@Composable
private fun Stat(icon: ImageVector, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = extendedColors().secondaryText,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = extendedColors().secondaryText,
        )
    }
}

@Composable
private fun MiniBadge(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Әрекет батырмасы — иконка + мәтін, loading күйі (Flutter _ActionBtn). */
@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !loading, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = color,
            )
        } else {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.W500,
                color = color,
            )
        }
    }
}

/** Валюта символы — KZT → ₸ (UiUtils.currencySymbol). */
internal fun currencySymbol(currency: String): String = when (currency.uppercase()) {
    "KZT", "₸" -> "₸"
    "USD" -> "$"
    "RUB" -> "₽"
    else -> currency
}