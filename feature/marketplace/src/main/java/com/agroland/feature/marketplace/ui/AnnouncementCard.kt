package com.agroland.feature.marketplace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.Announcement

/**
 * AnnouncementCard — Flutter карточкасымен сайткес: сол жақта сурет, оң жақта
 * атау + баға + қала/күн; VIP — алтын жиек, HOT — қызыл белгі; жүрек — сүйікті.
 */
@Composable
fun AnnouncementCard(
    item: Announcement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: (() -> Unit)? = null,
) {
    val ext = extendedColors()
    val borderModifier = if (item.isVip) {
        Modifier.border(1.5.dp, ext.accent, RoundedCornerShape(14.dp))
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ext.grey),
        ) {
            CachedImage(
                url = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.size(92.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.primaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (onToggleFavorite != null) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(L10nR.string.favorites_title),
                            tint = if (item.isFavorite) MaterialTheme.colorScheme.error else ext.secondaryText,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            val price = PriceFormatter.format(item.price, item.currency ?: "")
            if (price.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = price,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    item.measurementUnit?.takeIf { it.isNotBlank() }?.let { unit ->
                        Text(
                            text = " / $unit",
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.secondaryText,
                            maxLines = 1,
                        )
                    }
                    if (item.negotiable) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(L10nR.string.mp_negotiable_short),
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.secondaryText,
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (item.isHot) {
                    Badge(
                        icon = Icons.Outlined.LocalFireDepartment,
                        text = stringResource(L10nR.string.mp_hot_badge),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (item.isVip) {
                    Badge(
                        icon = null,
                        text = "VIP",
                        color = ext.accent,
                    )
                }
                listOfNotNull(item.city, item.district).joinToString(", ").takeIf { it.isNotBlank() }
                    ?.let { place ->
                        Text(
                            text = place,
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                Text(
                    text = DateFormatter.formatDate(item.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Кішкентай белгі (HOT/VIP) — иконкамен немесе жай мәтінмен. */
@Composable
private fun Badge(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    text: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}