package com.agroland.feature.promo.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Rocket
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.promo.data.Promotion

/**
 * Ағымдағы промо күйі (Flutter PromoCurrentStatusWidget, 1:1): активті
 * промо-v2 қызметтері чиптер түрінде (boost/vip/vip_seller/auto_renew) +
 * мұрағат қарау/прогресс блогы (болса). Прогресс жүктеліп жатқанда —
 * жіңішке индикатор, промо жоқ — бүкіл секция жасырылады.
 */
@Composable
fun PromoCurrentStatus(
    boostMultiplier: Int?,
    isVip: Boolean,
    isVipSeller: Boolean,
    autoRenewEnabled: Boolean,
    promotion: Promotion?,
    promotionLoading: Boolean,
    onToggleAutoRenewal: (promotionId: Long, enabled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ext = extendedColors()
    val hasV2Status = (boostMultiplier ?: 0) > 0 || isVip || isVipSeller || autoRenewEnabled

    if (!hasV2Status && promotion == null && !promotionLoading) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(ext.card, RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) {
        if (hasV2Status) {
            Text(
                text = stringResource(L10nR.string.current_promotion),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if ((boostMultiplier ?: 0) > 0) {
                    PromoStatusChip(
                        icon = Icons.Rounded.Rocket,
                        label = stringResource(
                            L10nR.string.promo_current_boost,
                            boostMultiplier ?: 0,
                        ),
                    )
                }
                if (isVip) {
                    PromoStatusChip(icon = Icons.Rounded.Star, label = stringResource(L10nR.string.promo_vip_title))
                }
                if (isVipSeller) {
                    PromoStatusChip(
                        icon = Icons.Rounded.Verified,
                        label = stringResource(L10nR.string.promo_vip_seller_title),
                    )
                }
                if (autoRenewEnabled) {
                    PromoStatusChip(
                        icon = Icons.Rounded.Autorenew,
                        label = stringResource(L10nR.string.promo_auto_renew_title),
                    )
                }
            }
            if (promotion == null && !promotionLoading) return
            Spacer(Modifier.height(16.dp))
        }

        when {
            promotionLoading && promotion == null -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp),
                )
            }
            promotion != null -> LegacyPromotionBlock(
                promotion = promotion,
                onToggleAutoRenewal = onToggleAutoRenewal,
            )
        }
    }
}

/** Статус чипі (Flutter _StatusChip): primary фон 8%, иконка + мәтін. */
@Composable
private fun PromoStatusChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .background(primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = primary,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
    }
}

/**
 * Мұрағат промо блогы (Flutter _LegacyProgress): прогрес % + жолақ +
 * қараулар есебі + автожаңарту тумблері (Flutter _AutoRenewalToggle).
 */
@Composable
private fun LegacyPromotionBlock(
    promotion: Promotion,
    onToggleAutoRenewal: (promotionId: Long, enabled: Boolean) -> Unit,
) {
    val ext = extendedColors()
    val isActive = promotion.status == "active"
    val barColor = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF9E9E9E)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(L10nR.string.progress),
            style = MaterialTheme.typography.bodySmall,
            color = ext.secondaryText,
        )
        Text(
            text = "${promotion.progressPercentage}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
    }

    Spacer(Modifier.height(8.dp))
    LinearProgressIndicator(
        progress = { promotion.progressPercentage / 100f },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        color = barColor,
        trackColor = ext.divider,
    )

    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = ext.secondaryText,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(L10nR.string.views_used),
                style = MaterialTheme.typography.bodySmall,
                color = ext.secondaryText,
            )
        }
        Text(
            text = "${promotion.viewsUsed} / ${promotion.maxViews}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
    }

    if (promotion.remainingTimeDays != null) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = ext.secondaryText,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    L10nR.string.remaining_days,
                    String.format(java.util.Locale.US, "%.1f", promotion.remainingTimeDays),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = ext.secondaryText,
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(L10nR.string.toggle_auto_renewal),
            style = MaterialTheme.typography.bodySmall,
        )
        Switch(
            checked = promotion.autoRenewal,
            onCheckedChange = { enabled -> onToggleAutoRenewal(promotion.id, enabled) },
        )
    }
}