package com.agroland.feature.cart.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroChip
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.FullAnnouncement
import com.agroland.feature.marketplace.ui.AnnouncementDetailViewModel

/** Детальдағы сатып алу мүмкіндігі (Flutter _canPurchaseViaCart). */
val FullAnnouncement.canPurchaseViaCart: Boolean
    get() = base.allowCart && (base.typeAd == "dealer" || base.isMarketplace)

/**
 * DetailBuyBar — деталь бетінің төменгі жолағы (Flutter
 * announcement_action_buttons): «Себетке» + «Сатып алу» (buy sheet арқылы).
 */
@Composable
fun DetailBuyBar(
    detailViewModel: AnnouncementDetailViewModel,
    onOpenOrder: (Long) -> Unit,
    viewModel: BuyBarViewModel = hiltViewModel(),
) {
    val detail by detailViewModel.detail.collectAsState()
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current
    val noInternetText = stringResource(L10nR.string.error_no_internet)
    val genericErrorText = stringResource(L10nR.string.error_generic_message)
    val addedToast = stringResource(L10nR.string.cart_added_to_cart_toast)
    var buyDoneOrderId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BuyEvent.ShowError -> Toast.makeText(
                    context,
                    event.error.displayText(noInternetText, genericErrorText),
                    Toast.LENGTH_LONG,
                ).show()
                BuyEvent.AddedToCart -> Toast.makeText(context, addedToast, Toast.LENGTH_SHORT).show()
                is BuyEvent.BuyDone -> buyDoneOrderId = event.orderId
            }
        }
    }

    val current = detail
    if (current == null || !current.canPurchaseViaCart) return

    Surface(color = extendedColors().card) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AgroButton(
                text = stringResource(L10nR.string.cart_add_to_cart_short),
                onClick = { viewModel.addToCart(current) },
                containerColor = extendedColors().grey,
                contentColor = extendedColors().primaryText,
                modifier = Modifier.weight(1f),
            )
            AgroButton(
                text = stringResource(L10nR.string.cart_buy),
                onClick = { viewModel.openSheet(current) },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (state.sheetVisible) {
        BuySheet(
            detail = current,
            state = state,
            onDismiss = viewModel::closeSheet,
            onQuantity = viewModel::changeQuantity,
            onSetDeliveryMode = viewModel::setDeliveryMode,
            onSelectAddress = viewModel::selectAddress,
            onSelectZone = viewModel::selectZone,
            onBuy = { viewModel.buyNow(current) },
        )
    }

    val orderId = buyDoneOrderId
    if (orderId != null) {
        AlertDialog(
            onDismissRequest = { buyDoneOrderId = null },
            title = { Text(stringResource(L10nR.string.cart_order_accepted)) },
            confirmButton = {
                TextButton(onClick = {
                    buyDoneOrderId = null
                    onOpenOrder(orderId)
                }) {
                    Text(stringResource(L10nR.string.cart_order))
                }
            },
            dismissButton = {
                TextButton(onClick = { buyDoneOrderId = null }) {
                    Text(stringResource(L10nR.string.common_close))
                }
            },
        )
    }
}

/**
 * BuySheet — Flutter buy_bottom_sheet: сан, delivery-check (жеткізу/өзі алу),
 * мекенжай, зона, buy-now.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuySheet(
    detail: FullAnnouncement,
    state: BuyState,
    onDismiss: () -> Unit,
    onQuantity: (Double) -> Unit,
    onSetDeliveryMode: (Boolean) -> Unit,
    onSelectAddress: (Long) -> Unit,
    onSelectZone: (Long) -> Unit,
    onBuy: () -> Unit,
) {
    val ext = extendedColors()
    val zones = detail.deliveryZones.ifEmpty {
        state.deliveryCheck?.zone?.let { listOf(it) } ?: emptyList()
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Сан ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(L10nR.string.cart_product),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.primaryText,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { onQuantity(-1.0) },
                    enabled = state.quantity > 1.0,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = null,
                        tint = if (state.quantity > 1.0) ext.primaryText else ext.divider,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = formatQuantity(state.quantity) +
                        (detail.base.measurementUnit?.let { " $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.primaryText,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
                IconButton(onClick = { onQuantity(1.0) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = ext.primaryText,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            HorizontalDivider()

            // ── Delivery-check күйі ──
            when {
                state.checkLoading -> Text(
                    text = stringResource(L10nR.string.cart_checking_delivery),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                )
                state.checkKnown -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.canPickup) {
                        AgroChip(
                            text = stringResource(L10nR.string.cart_pickup),
                            selected = !state.isDelivery,
                            onClick = { onSetDeliveryMode(false) },
                        )
                    }
                    if (state.canDeliver) {
                        AgroChip(
                            text = stringResource(L10nR.string.cart_delivery),
                            selected = state.isDelivery,
                            onClick = { onSetDeliveryMode(true) },
                        )
                    }
                    if (!state.canDeliver && !state.canPickup) {
                        Text(
                            text = stringResource(L10nR.string.cart_delivery_not_available),
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.secondaryText,
                        )
                    }
                }
                else -> Text(
                    text = stringResource(L10nR.string.cart_delivery_not_available),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                )
            }

            // ── Мекенжай (жеткізу режимі) ──
            if (state.isDelivery && state.canDeliver) {
                Text(
                    text = stringResource(L10nR.string.cart_delivery_address),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                )
                state.addresses.forEach { address ->
                    val title = address.title.ifBlank { address.fullAddress }
                    val selected = address.id == state.selectedAddressId
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.primary else ext.primaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                } else {
                                    ext.grey
                                },
                            )
                            .clickable { onSelectAddress(address.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
                if (state.addresses.isEmpty()) {
                    Text(
                        text = stringResource(L10nR.string.cart_address_not_specified),
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.secondaryText,
                    )
                }

                // ── Зона таңдауы ──
                if (zones.isNotEmpty()) {
                    Text(
                        text = stringResource(L10nR.string.cart_select_delivery_zone),
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.secondaryText,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        zones.forEach { zone ->
                            AgroChip(
                                text = zone.displayName,
                                selected = state.selectedZoneId == zone.id,
                                onClick = { onSelectZone(zone.id) },
                            )
                        }
                    }
                }
            } else if (!state.isDelivery && state.canPickup) {
                Text(
                    text = stringResource(L10nR.string.cart_pickup_label) +
                        (
                            (state.deliveryCheck?.pickupAddress ?: detail.base.pickupAddress)
                                ?.let { ": $it" } ?: ""
                            ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.primaryText,
                )
            }

            HorizontalDivider()

            // ── Сома ──
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(L10nR.string.cart_total_amount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ext.primaryText,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = PriceFormatter.formatPrecise(
                        (detail.base.price ?: 0.0) * state.quantity,
                        detail.base.currency ?: "₸",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            AgroButton(
                text = stringResource(L10nR.string.cart_buy),
                onClick = onBuy,
                enabled = state.canSubmit,
                loading = state.submitting,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}