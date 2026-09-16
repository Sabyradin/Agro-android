package com.agroland.feature.dealer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.dealer.data.OrderTracking
import com.agroland.feature.dealer.data.TrackingEvent
import com.agroland.feature.dealer.data.Waybill
import com.agroland.feature.dealer.data.displayText

/**
 * Тапсырыс трекингі — Flutter OrderTrackingPage (1:1): статус картасы,
 * мекенжайлар, сапарлық құжаттар (подписи чекмарктерімен) және тарих
 * (timeline нүктелері). GET /orders/{id}/tracking.
 */
@Composable
fun OrderTrackingPage(
    onBack: () -> Unit,
    viewModel: OrderTrackingViewModel = hiltViewModel(),
) {
    val tracking by viewModel.tracking.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    val networkError = stringResource(L10nR.string.error_no_internet)
    val genericError = stringResource(L10nR.string.error_generic_message)

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.dealer_tracking),
                onBack = onBack,
            )
        },
    ) { modifier ->
        Box(modifier = modifier) {
            when {
                loading -> LoadingWidget()
                error != null -> CenteredContent {
                    ErrorWithRetry(
                        onRetry = viewModel::load,
                        message = error!!.displayText(networkError, genericError),
                    )
                }
                tracking != null -> TrackingContent(tracking!!)
            }
        }
    }
}

@Composable
private fun TrackingContent(tracking: OrderTracking) {
    val ext = extendedColors()
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Статус картасы ──
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ext.card)
                    .border(0.5.dp, ext.divider, RoundedCornerShape(14.dp))
                    .padding(16.dp),
            ) {
                Text(
                    text = stringResource(L10nR.string.dealer_order_status),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(dealerOrderStatusColor(tracking.status)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(dealerOrderStatusLabelRes(tracking.status)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = dealerOrderStatusColor(tracking.status),
                    )
                }
            }
        }

        // ── Мекенжайлар және күндер ──
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ext.card)
                    .border(0.5.dp, ext.divider, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                tracking.pickupAddress?.let {
                    AddressRow(Icons.Outlined.ShoppingBag, stringResource(L10nR.string.dealer_pickup_address), it)
                }
                tracking.deliveryAddress?.let {
                    AddressRow(Icons.Outlined.Map, stringResource(L10nR.string.dealer_delivery_address), it)
                }
                tracking.loadingDate?.let {
                    AddressRow(Icons.Outlined.ReceiptLong, stringResource(L10nR.string.dealer_loading_date), it)
                }
                tracking.deliveryDate?.let {
                    AddressRow(Icons.Outlined.ReceiptLong, stringResource(L10nR.string.dealer_delivery_date), it)
                }
            }
        }

        // ── Сапарлық құжаттар ──
        if (tracking.waybills.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(L10nR.string.dealer_waybills),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ext.primaryText,
                )
            }
            items(tracking.waybills.size) { index ->
                WaybillCard(tracking.waybills[index])
            }
        }

        // ── Тарих ──
        if (tracking.timeline.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(L10nR.string.dealer_timeline),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ext.primaryText,
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ext.card)
                        .border(0.5.dp, ext.divider, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                ) {
                    tracking.timeline.forEachIndexed { index, event ->
                        TimelineRow(
                            event = event,
                            isLast = index == tracking.timeline.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

/** Мекенжай жолы — дөңгелек иконка + атау + мән. */
@Composable
private fun AddressRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    val ext = extendedColors()
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = ext.secondaryText,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = ext.primaryText,
            )
        }
    }
}

/** Сапарлық құжат картасы — нөмірі, көлігі, жүргізушісі, күндері, подписьдері. */
@Composable
private fun WaybillCard(waybill: Waybill) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .border(0.5.dp, ext.divider, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(
            text = "№" + waybill.waybillNumber,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = ext.primaryText,
        )
        Spacer(Modifier.height(8.dp))
        waybill.vehicleNumber?.let {
            TrackingInfoRow(stringResource(L10nR.string.dealer_vehicle), it)
        }
        waybill.driverName?.let {
            TrackingInfoRow(stringResource(L10nR.string.dealer_driver), it)
        }
        waybill.shipmentDate?.let {
            TrackingInfoRow(stringResource(L10nR.string.dealer_shipment_date), DateFormatter.formatDateTime(it))
        }
        waybill.loadingPoint?.let {
            TrackingInfoRow(stringResource(L10nR.string.dealer_pickup_address), it)
        }
        waybill.deliveryPoint?.let {
            TrackingInfoRow(stringResource(L10nR.string.dealer_delivery_address), it)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SignatureChip(
                label = stringResource(L10nR.string.dealer_signed_by_seller),
                signed = waybill.signedBySeller,
            )
            SignatureChip(
                label = stringResource(L10nR.string.dealer_signed_by_buyer),
                signed = waybill.signedByBuyer,
            )
        }
    }
}

/** Подпись чипі — жасыл чекмарк немесе сұр «күтілуде» нүктесі. */
@Composable
private fun SignatureChip(label: String, signed: Boolean) {
    val ext = extendedColors()
    val color = if (signed) {
        androidx.compose.ui.graphics.Color(0xFF4CAF50)
    } else {
        ext.secondaryText
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (signed) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(ext.secondaryText),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Атау-мән жолы (сапарлық құжат іші). */
@Composable
private fun TrackingInfoRow(label: String, value: String) {
    val ext = extendedColors()
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ext.secondaryText,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.W500,
            color = ext.primaryText,
        )
    }
}

/** Тарих жолы — түрлі-түсті нүкте + сызық + статус/ескертпе + күн. */
@Composable
private fun TimelineRow(event: TrackingEvent, isLast: Boolean) {
    val ext = extendedColors()
    val color = dealerOrderStatusColor(event.status)
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                        .background(ext.divider),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 12.dp)) {
            Text(
                text = stringResource(dealerOrderStatusLabelRes(event.status)),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.W600,
                color = ext.primaryText,
            )
            event.note?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.secondaryText,
                )
            }
            event.createdAt?.let {
                Text(
                    text = DateFormatter.formatDateTime(it),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = ext.secondaryText,
                )
            }
        }
    }
}