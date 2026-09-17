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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.theme.AgroRadius
import com.agroland.core.ui.theme.AgroSpacing
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.Announcement

/** Карточка суреті — қалған кеңістік мәтінге қалады. */
private val ThumbSize = 100.dp

/**
 * AnnouncementCard — лента карточкасы: сол жақта сурет, оң жақта атау,
 * баға, белгілер (VIP/HOT) және қала/күн жолы. Жүрек — суреттің үстінде.
 *
 * VIP — алтын жиек; HOT/VIP белгілері фоны бар чип түрінде (бұрын жалаң
 * мәтін болып, жолда жоғалып кететін).
 */
@Composable
fun AnnouncementCard(
    item: Announcement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: (() -> Unit)? = null,
) {
    val ext = extendedColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screen, vertical = 6.dp)
            .shadow(
                elevation = 2.dp,
                shape = AgroRadius.card,
                ambientColor = Color(0x14121212),
                spotColor = Color(0x14121212),
            )
            .clip(AgroRadius.card)
            .background(ext.card)
            .then(
                if (item.isVip) {
                    Modifier.border(1.5.dp, ext.accent, AgroRadius.card)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(AgroSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.md),
    ) {
        Box(modifier = Modifier.size(ThumbSize)) {
            CachedImage(
                url = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .size(ThumbSize)
                    .clip(RoundedCornerShape(12.dp)),
            )
            if (onToggleFavorite != null) {
                FavoriteButton(
                    isFavorite = item.isFavorite,
                    onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .height(ThumbSize),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall,
                color = ext.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            val price = PriceFormatter.format(item.price, displayCurrency(item.currency))
            if (price.isNotBlank()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = price,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    item.measurementUnit?.takeIf { it.isNotBlank() }?.let { unit ->
                        Text(
                            text = " / ${displayUnit(unit)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.secondaryText,
                            maxLines = 1,
                        )
                    }
                    if (item.negotiable) {
                        Spacer(Modifier.width(AgroSpacing.sm))
                        Text(
                            text = stringResource(L10nR.string.mp_negotiable_short),
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.secondaryText,
                            maxLines = 1,
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (item.isHot) {
                    MarkerChip(
                        icon = Icons.Rounded.LocalFireDepartment,
                        text = stringResource(L10nR.string.mp_hot_badge),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (item.isVip) {
                    MarkerChip(icon = null, text = "VIP", color = ext.accent)
                }
                item.placeLabel.takeIf { it.isNotBlank() }
                    ?.let { place ->
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = ext.secondaryText,
                            modifier = Modifier.size(13.dp),
                        )
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

/** Суреттің бұрышындағы жүрек — ақ дөңгелек фонда (суретте де көрінеді). */
@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(5.dp)
            .size(26.dp)
            .clip(CircleShape)
            .background(extendedColors().card.copy(alpha = 0.92f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = stringResource(L10nR.string.favorites_title),
            tint = if (isFavorite) MaterialTheme.colorScheme.error else extendedColors().secondaryText,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Кішкентай белгі (HOT/VIP) — түсті жұмсақ фондағы чип. */
@Composable
private fun MarkerChip(
    icon: ImageVector?,
    text: String,
    color: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
            maxLines = 1,
        )
    }
}
