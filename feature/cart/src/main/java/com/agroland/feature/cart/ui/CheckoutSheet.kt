package com.agroland.feature.cart.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroChip
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.AnnouncementDeliveryZone as Zone

/**
 * CheckoutSheet — Flutter checkout_bottom_sheet: әр тауар үшін өзі алу/жеткізу
 * ауыстырғышы, зона таңдауы, сақталған мекенжайлар, Smart Calculator preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutSheet(
    state: CheckoutUiState,
    onDismiss: () -> Unit,
    onToggleDelivery: (Long) -> Unit,
    onSetZone: (Long, Long) -> Unit,
    onSelectAddress: (Long) -> Unit,
    onSubmit: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Тауарлар: әрқайсысында өзі алу / жеткізу + зона ──
            state.items.forEach { entry ->
                CheckoutItemRow(
                    entry = entry,
                    pickupLocked = state.pickupOnly,
                    onToggleDelivery = { onToggleDelivery(entry.item.id) },
                    onSetZone = { zoneId -> onSetZone(entry.item.id, zoneId) },
                )
            }

            HorizontalDivider()

            if (state.anyDelivery) {
                Text(
                    text = stringResource(L10nR.string.cart_delivery_address),
                    style = MaterialTheme.typography.bodySmall,
                    color = extendedColors().primaryText,
                )
                // ── Сақталған мекенжайлар — радио-карточкалар ──
                state.addresses.forEach { address ->
                    AddressRadioCard(
                        title = address.title.ifBlank { address.fullAddress },
                        subtitle = address.subtitle.ifBlank { null },
                        selected = address.id == state.selectedAddressId,
                        onClick = { onSelectAddress(address.id) },
                    )
                }
                if (state.addresses.isEmpty()) {
                    Text(
                        text = stringResource(L10nR.string.cart_address_not_specified),
                        style = MaterialTheme.typography.labelMedium,
                        color = extendedColors().secondaryText,
                    )
                }
            } else {
                // Тек өзі алу — сатушы мекенжайы көрсетіледі.
                Text(
                    text = stringResource(L10nR.string.cart_pickup_label) +
                        (state.pickupAddress?.let { ": $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = extendedColors().primaryText,
                )
            }

            HorizontalDivider()

            // ── Smart Calculator: preview үлесі ──
            PreviewBreakdown(state = state)

            state.error?.let { error ->
                Text(
                    text = error.displayText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            AgroButton(
                text = stringResource(L10nR.string.cart_proceed_to_payment),
                onClick = onSubmit,
                enabled = state.canSubmit,
                loading = state.submitting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Бір тауар: сурет + атау + өзі алу/жеткізу чиптері + зона таңдауы. */
@Composable
private fun CheckoutItemRow(
    entry: CheckoutItemUi,
    pickupLocked: Boolean,
    onToggleDelivery: () -> Unit,
    onSetZone: (Long) -> Unit,
) {
    val ext = extendedColors()
    val base = entry.item.announcement?.base
    val zones = entry.item.announcement?.deliveryZones.orEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ext.card)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ext.grey),
            ) {
                CachedImage(url = base?.imageUrl, contentDescription = base?.title)
            }
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = base?.title?.takeIf { it.isNotBlank() } ?: "#${entry.item.announcementId}",
                    style = MaterialTheme.typography.labelLarge,
                    color = ext.primaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        L10nR.string.cart_quantity_label,
                        formatQuantity(entry.item.quantity),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                )
            }
        }
        if (!pickupLocked) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AgroChip(
                    text = stringResource(L10nR.string.cart_pickup),
                    selected = !entry.isDelivery,
                    onClick = onToggleDelivery,
                )
                val canDeliver = base?.deliveryAvailable == true && zones.isNotEmpty()
                if (canDeliver) {
                    AgroChip(
                        text = stringResource(L10nR.string.cart_delivery),
                        selected = entry.isDelivery,
                        onClick = onToggleDelivery,
                    )
                }
            }
        }
        if (entry.isDelivery && zones.isNotEmpty()) {
            Text(
                text = stringResource(L10nR.string.cart_select_delivery_zone),
                style = MaterialTheme.typography.labelMedium,
                color = ext.secondaryText,
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(zones, key = { it.id }) { zone ->
                    ZoneChip(
                        zone = zone,
                        selected = entry.deliveryZoneId == zone.id,
                        onClick = { onSetZone(zone.id) },
                    )
                }
            }
        }
    }
}

/** Зона чипі: атау + баға + күндер. */
@Composable
private fun ZoneChip(
    zone: Zone,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ext = extendedColors()
    val freeText = stringResource(L10nR.string.cart_free_delivery)
    val daysUnit = stringResource(L10nR.string.cart_days_unit)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else ext.grey)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = zone.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) ext.white else ext.primaryText,
            maxLines = 1,
        )
        val days = zone.daysRangeRaw
        Text(
            text = buildString {
                append(
                    if (zone.deliveryCost > 0) {
                        PriceFormatter.format(zone.deliveryCost, "₸")
                    } else {
                        freeText
                    },
                )
                if (days.isNotBlank()) append(" · $days $daysUnit")
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) ext.white else ext.secondaryText,
            maxLines = 1,
        )
    }
}

/** Сақталған мекенжай радио-карточкасы. */
@Composable
private fun AddressRadioCard(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ext.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = ext.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Smart Calculator preview: тауарлар / әр топ / жалпы (+ ескертулер). */
@Composable
private fun PreviewBreakdown(state: CheckoutUiState) {
    val ext = extendedColors()
    if (state.previewLoading) {
        Text(
            text = stringResource(L10nR.string.cart_checking_delivery),
            style = MaterialTheme.typography.labelMedium,
            color = ext.secondaryText,
        )
        return
    }
    val preview = state.preview ?: return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        preview.groups.forEach { group ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = group.supplierName.ifBlank { stringResource(L10nR.string.cart_seller) },
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.primaryText,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = PriceFormatter.formatPrecise(group.total, "₸"),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.primaryText,
                )
            }
        }
        preview.warnings.forEach { warning ->
            Text(
                text = warning,
                style = MaterialTheme.typography.labelSmall,
                color = ext.secondaryText,
            )
        }
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(L10nR.string.cart_grand_total),
                style = MaterialTheme.typography.bodyMedium,
                color = ext.primaryText,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = PriceFormatter.formatPrecise(preview.grandTotal, "₸"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}