package com.agroland.feature.marketplace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.theme.AgroSpacing
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.Announcement

private val CardShape = RoundedCornerShape(14.dp)

/**
 * Лента карточкасы — iOS нұсқасындағыдай ТІК плитка (2 бағанды тор үшін):
 * жоғарыда шаршы сурет (үстінде VIP белгісі мен жүрек), астында қала,
 * атау, баға, бөлгіш сызық және жарияланған күні.
 *
 * Тізім экрандарында (таңдаулылар, менің жарнамаларым) көлденең
 * [AnnouncementCard] қалады — ол жерде тік плитка орынды үнемдемейді.
 */
@Composable
fun AnnouncementGridCard(
    item: Announcement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: (() -> Unit)? = null,
    /** «Себетке» — тек себетке салуға рұқсат етілген жарнамаларда көрінеді. */
    onAddToCart: (() -> Unit)? = null,
    /** Себетте бар болса — саны; сонда «Себетке» орнына «+ / −» көрінеді. */
    cartQuantity: Double? = null,
    /** «+ / −» басылғандағы өзгеріс (+1 / −1). */
    onChangeCartQuantity: ((Double) -> Unit)? = null,
) {
    val ext = extendedColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = CardShape,
                ambientColor = Color(0x14121212),
                spotColor = Color(0x14121212),
            )
            .clip(CardShape)
            .background(ext.card)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            CachedImage(
                url = item.imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(AgroSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.xs),
            ) {
                if (item.isVip) {
                    OverlayBadge(
                        text = "VIP",
                        containerColor = ext.accent,
                        contentColor = ext.white,
                        icon = Icons.Rounded.WorkspacePremium,
                    )
                }
                if (item.isHot) {
                    OverlayBadge(
                        text = stringResource(L10nR.string.mp_hot_badge),
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = ext.white,
                        icon = Icons.Rounded.LocalFireDepartment,
                    )
                }
            }
            if (onToggleFavorite != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(AgroSpacing.sm)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ext.card.copy(alpha = 0.92f))
                        .clickable(onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) {
                            Icons.Rounded.Favorite
                        } else {
                            Icons.Rounded.FavoriteBorder
                        },
                        contentDescription = stringResource(L10nR.string.favorites_title),
                        tint = if (item.isFavorite) {
                            MaterialTheme.colorScheme.error
                        } else {
                            ext.secondaryText
                        },
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(AgroSpacing.sm + 2.dp)) {
            // Орын белгісіз болса жолды мүлдем салмаймыз — бұрын бос мәтінмен
            // жалғыз пин иконкасы қалып қоятын. Биіктік бірдей болу үшін
            // орнына сол өлшемді бос аралық қалады.
            val place = item.placeLabel
            if (place.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = ext.secondaryText,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.size(3.dp))
                    Text(
                        text = place,
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(4.dp))
            } else {
                Spacer(Modifier.height(AgroSpacing.xs))
            }

            // minLines = 2: бір жолдық және екі жолдық атаулар қатардағы
            // карточкалардың биіктігін әртүрлі етпеуі керек.
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = ext.primaryText,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            val price = PriceFormatter.format(item.price, displayCurrency(item.currency))
            if (price.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = price,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
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
                }
            }

            // Жеткізу жолы — әрқашан бірдей орын алады (болмаса бос қалады),
            // сондықтан қатардағы екі карточка бірдей биіктікте тұрады.
            Spacer(Modifier.height(3.dp))
            Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.CenterStart) {
                if (item.deliveryAvailable) {
                    Text(
                        text = stringResource(L10nR.string.detail_delivery),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
            }

            val date = DateFormatter.formatDate(item.createdAt)
            run {
                Spacer(Modifier.height(AgroSpacing.sm))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ext.divider),
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = ext.secondaryText,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.secondaryText,
                        maxLines = 1,
                    )
                }
            }

            // Себетте бар тауарда батырманың орнын «− сан +» басқарғышы алады.
            val inCart = cartQuantity != null && cartQuantity > 0.0
            if (onAddToCart != null || inCart) {
                Spacer(Modifier.height(AgroSpacing.sm))
                if (inCart && onChangeCartQuantity != null) {
                    CartQuantityStepper(
                        quantity = cartQuantity!!,
                        unit = item.measurementUnit,
                        onChange = onChangeCartQuantity,
                    )
                } else if (onAddToCart != null) {
                    AddToCartButton(onClick = onAddToCart)
                }
            }
        }
    }
}

/**
 * Себеттегі саны — «− сан +». Бірден төмен түскенде тауар себеттен алынады,
 * сондықтан «−» әрқашан белсенді (себет бетіндегі басқарғыштан айырмашылығы).
 */
@Composable
private fun CartQuantityStepper(
    quantity: Double,
    unit: String?,
    onChange: (Double) -> Unit,
) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(icon = Icons.Rounded.Remove, onClick = { onChange(-1.0) })
        Text(
            text = formatCartQuantity(quantity) +
                (unit?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        StepperButton(icon = Icons.Rounded.Add, onClick = { onChange(1.0) })
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** 2.0 → «2», 2.5 → «2.5» (себет бетіндегі формат). */
private fun formatCartQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString().trimEnd('0').trimEnd('.')

/** Карточканың астындағы «Себетке» — толық енді жасыл батырма (iOS үлгісі). */
@Composable
private fun AddToCartButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.AddShoppingCart,
            contentDescription = null,
            tint = extendedColors().white,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = stringResource(L10nR.string.cart_add_to_cart_short),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = extendedColors().white,
            maxLines = 1,
        )
    }
}

/** Суреттің үстіндегі белгі (VIP / Ыстық) — толтырылған дөңгелек «таблетка». */
@Composable
private fun OverlayBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = contentColor,
            maxLines = 1,
        )
    }
}
