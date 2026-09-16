package com.agroland.feature.china.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.china.data.ChinaCartItem
import com.agroland.feature.china.data.ChinaOrder
import com.agroland.feature.china.data.ChinaOrderStatus

/**
 * Қытай модулінің ортақ плиталары — ChinaThumb, сан тандайы, корзина жолы
 * және тапсырыс картасы. feature:cart осы композиттерді қайта қолданады
 * (қытай позициялары жалпы CartPage-ке кіріктірілген).
 */

/** Тауар суреті — суреті жоқ болса фотоплейсхолдер иконкасы. */
@Composable
fun ChinaThumb(imageUrl: String?, modifier: Modifier = Modifier) {
    if (imageUrl != null) {
        CachedImage(
            url = imageUrl,
            contentDescription = null,
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = extendedColors().secondaryText,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

/** Сан тандайы — − / саны / +, minQty шегінен төмен түспейді. */
@Composable
fun ChinaQuantityStepper(
    quantity: Int,
    minQuantity: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ext = extendedColors()
    Row(
        modifier = modifier
            .border(1.dp, ext.divider, RoundedCornerShape(10.dp))
            .background(ext.card, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { if (quantity > minQuantity) onChange(quantity - 1) },
            enabled = quantity > minQuantity,
            modifier = Modifier.size(34.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = null,
                tint = if (quantity > minQuantity) ext.primaryText else ext.divider,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = ext.primaryText,
            modifier = Modifier
                .width(44.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = { onChange(quantity + 1) }, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Қытай корзина жолы — сурет, атау, баға × саны, тандай, жою. */
@Composable
fun ChinaCartItemTile(
    item: ChinaCartItem,
    onQuantityChange: (Int) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ext = extendedColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ext.grey),
        ) {
            ChinaThumb(
                imageUrl = item.imageUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.titleSnapshot ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = ext.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = PriceFormatter.format(item.priceAtAdd) + " × " + item.quantity,
                style = MaterialTheme.typography.labelMedium,
                color = ext.secondaryText,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = PriceFormatter.format(item.lineTotal),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ChinaQuantityStepper(
                quantity = item.quantity,
                minQuantity = item.minQty ?: 1,
                onChange = onQuantityChange,
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = ext.secondaryText,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** Қытай тапсырысының күй чипі — түспен кодталған. */
@Composable
fun ChinaOrderStatusChip(status: ChinaOrderStatus, modifier: Modifier = Modifier) {
    val ext = extendedColors()
    val (label, container, content) = when (status) {
        ChinaOrderStatus.FORWARDED ->
            Triple(L10nR.string.china_status_forwarded, MaterialTheme.colorScheme.primary, Color.White)
        ChinaOrderStatus.FAILED ->
            Triple(L10nR.string.china_status_failed, MaterialTheme.colorScheme.error, Color.White)
        ChinaOrderStatus.CANCELLED ->
            Triple(L10nR.string.china_status_cancelled, ext.grey, ext.secondaryText)
        ChinaOrderStatus.UNKNOWN ->
            Triple(L10nR.string.china_status_unknown, ext.grey, ext.secondaryText)
    }
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** Қытай тапсырыс картасы — ORDER_HISTORY ішіндегі аралас тізім үшін. */
@Composable
fun ChinaOrderTile(
    order: ChinaOrder,
    modifier: Modifier = Modifier,
) {
    val ext = extendedColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ext.card)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    L10nR.string.china_order_number,
                    order.mercuryxNumber ?: order.id.toString(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ext.primaryText,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ChinaOrderStatusChip(status = order.status)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(L10nR.string.china_from_mercuryx),
            style = MaterialTheme.typography.labelMedium,
            color = ext.secondaryText,
        )
        Spacer(Modifier.height(8.dp))
        for (line in order.items) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = (line.titleSnapshot ?: "—") + " × " + line.quantity,
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.primaryText,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = PriceFormatter.format(line.priceAtSend),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ext.primaryText,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = DateFormatter.formatDateTime(order.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = ext.secondaryText,
                modifier = Modifier.weight(1f),
            )
        }
        if (!order.errorMessage.isNullOrBlank() && order.status == ChinaOrderStatus.FAILED) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = order.errorMessage,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}