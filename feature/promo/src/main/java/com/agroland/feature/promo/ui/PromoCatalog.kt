package com.agroland.feature.promo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Rocket
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.promo.data.PromoCatalogItem

/**
 * Промо v2 каталог бөлімі (Flutter PromoCatalogWidget, 1:1): «Тарифы» + медаль
 * иконкасы, әр SKU үшін PromoItemCard. target бойынша сүзу — announcement
 * мақсатты SKU-лар өз жарнама контекстінде ғана көрсетіледі.
 */
@Composable
fun PromoCatalogSection(
    items: List<PromoCatalogItem>,
    loading: Boolean,
    announcementId: Long?,
    localeTag: String?,
    activatingSku: String?,
    onActivate: (PromoCatalogItem) -> Unit,
    bannerForm: @Composable (item: PromoCatalogItem, submitting: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ext = extendedColors()
    val filtered = items.filter { item ->
        // announcement-targeted SKU-лар өз жарнама id-імен ғана.
        item.target != "announcement" || announcementId != null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ext.card, RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(L10nR.string.tariffs),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
            Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        when {
            loading -> LoadingWidget(Modifier.fillMaxWidth())
            filtered.isEmpty() -> Text(
                text = stringResource(L10nR.string.no_available_tariffs),
                style = MaterialTheme.typography.bodyMedium,
                color = ext.secondaryText,
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                filtered.forEach { item ->
                    PromoItemCard(
                        item = item,
                        localeTag = localeTag,
                        isActivating = activatingSku == item.sku,
                        onActivate = { onActivate(item) },
                        bannerForm = if (item.kind == "banner") {
                            { submitting -> bannerForm(item, submitting) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

/**
 * Бір каталог карточкасы (Flutter PromoItemCard): түр түсі/иконкасы,
 * атау + ұзақтық, сипаттама, баға, белсендіру батырмасы (banner түріне — форма).
 */
@Composable
private fun PromoItemCard(
    item: PromoCatalogItem,
    localeTag: String?,
    isActivating: Boolean,
    onActivate: () -> Unit,
    bannerForm: (@Composable (submitting: Boolean) -> Unit)?,
) {
    val ext = extendedColors()
    val kindColor = item.kindColor()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ext.card, RoundedCornerShape(16.dp))
            .border(1.dp, ext.divider, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = item.kindIcon(),
                contentDescription = null,
                tint = kindColor,
                modifier = Modifier
                    .background(kindColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .padding(8.dp)
                    .size(20.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = item.localizedTitle(localeTag),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (item.durationDays > 0) {
                Text(
                    text = stringResource(L10nR.string.promo_duration_days, item.durationDays),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.secondaryText,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = item.localizedDescription(localeTag),
            style = MaterialTheme.typography.bodySmall,
            color = ext.secondaryText,
        )

        Spacer(Modifier.height(12.dp))
        Text(
            text = PriceFormatter.format(item.price, currencySymbol(item.currency)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        if (bannerForm != null) {
            Spacer(Modifier.height(12.dp))
            bannerForm(isActivating)
        } else {
            if (item.requiresAnnouncement) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(L10nR.string.promo_select_ad_first),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(12.dp))
            AgroButton(
                text = stringResource(L10nR.string.promo_activate),
                onClick = onActivate,
                enabled = !isActivating && !item.requiresAnnouncement,
                loading = isActivating,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Түр түсі (Flutter _kindColor): boost көк / vip қызғылт сары / banner күлгін / auto_renew көкжасыл. */
private fun PromoCatalogItem.kindColor(): Color = when (kind) {
    "boost" -> Color(0xFF2196F3)
    "vip" -> Color(0xFFF57C00)
    "banner" -> Color(0xFF9C27B0)
    "auto_renew" -> Color(0xFF009688)
    else -> Color(0xFF147F26)
}

private fun PromoCatalogItem.kindIcon(): ImageVector = when (kind) {
    "boost" -> Icons.Rounded.Rocket
    "vip" -> Icons.Rounded.Star
    "banner" -> Icons.Rounded.Image
    "auto_renew" -> Icons.Rounded.Autorenew
    else -> Icons.Rounded.Flag
}

/** Валюта коды → символ (Flutter UiUtils.currencySymbol): KZT → ₸. */
internal fun currencySymbol(currency: String): String =
    if (currency.equals("KZT", ignoreCase = true)) "₸" else currency