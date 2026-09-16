package com.agroland.feature.promo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.components.ShimmerCard
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.promo.data.AnnouncementPromotion
import com.agroland.feature.promo.data.Promotion
import java.util.Locale

/**
 * PromotedAnnouncementsPage (Flutter, 1:1): GET /user/announcements/promotions —
 * өз жарнамаларының әрқайсысының жанында промо карточкасы (пакет түрі, статус
 * чипі, прогресс жолағы, қараулар есебі, қалған күндер).
 */
@Composable
fun PromotedAnnouncementsPage(
    onBack: () -> Unit,
    onOpenAnnouncement: (Long) -> Unit,
    viewModel: PromotedAnnouncementsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.promoted_announcements),
                onBack = onBack,
            )
        },
    ) { inner ->
        when {
            state.loading -> Column(modifier = inner.fillMaxSize()) {
                repeat(4) { ShimmerCard() }
            }
            state.error -> CenteredContent(modifier = inner.fillMaxSize()) {
                ErrorWithRetry(
                    onRetry = viewModel::refresh,
                    message = stringResource(L10nR.string.error_generic_message),
                )
            }
            state.items.isEmpty() -> CenteredContent(modifier = inner.fillMaxSize()) {
                EmptyView(
                    title = stringResource(L10nR.string.promoted_announcements),
                    message = stringResource(L10nR.string.no_promoted_announcements),
                )
            }
            else -> LazyColumn(
                modifier = inner.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp,
                ),
            ) {
                items(state.items.size, key = { state.items[it].announcementId }) { index ->
                    val item = state.items[index]
                    Column {
                        PromotedAnnouncementCard(
                            item = item,
                            onClick = { onOpenAnnouncement(item.announcementId) },
                        )
                        if (item.hasPromotion && item.promotion != null) {
                            Spacer(Modifier.height(12.dp))
                            PromotionCard(promotion = item.promotion)
                        }
                    }
                }
            }
        }
    }
}

/** Өз жарнамасы карточкасы (Flutter ProfileAnnouncementItemView, showActions: false). */
@Composable
private fun PromotedAnnouncementCard(
    item: AnnouncementPromotion,
    onClick: () -> Unit,
) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ext.card, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CachedImage(
            url = item.announcementImageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .background(ext.grey, RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.announcementTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (item.announcementPrice != null) {
                Text(
                    text = PriceFormatter.format(
                        item.announcementPrice,
                        currencySymbol(item.announcementCurrency ?: "KZT"),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Промо карточкасы (Flutter _PromotionCard): «Промо» + медаль, статус чипі
 * (активті → primary, күтуде → қызғылт сары, аяқталған → сұр), пакет түрі,
 * прогресс % + қараулар, қалған күндер.
 */
@Composable
private fun PromotionCard(promotion: Promotion) {
    val ext = extendedColors()
    val isActive = promotion.status == "active"
    val isPending = promotion.status == "pending"
    val accent = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else if (isPending) {
        Color(0xFFF57C00)
    } else {
        Color(0xFF9E9E9E)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ext.card, RoundedCornerShape(12.dp))
            .border(1.5.dp, if (isActive) MaterialTheme.colorScheme.primary else ext.divider, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(L10nR.string.promotion),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = stringResource(
                    when (promotion.status) {
                        "active" -> L10nR.string.promotion_active
                        "pending" -> L10nR.string.promotion_pending
                        else -> L10nR.string.promotion_expired
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = accent,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(accent.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(L10nR.string.promo_package),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.secondaryText,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = promotion.packageType.uppercase(Locale.US),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(L10nR.string.progress),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.secondaryText,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${promotion.progressPercentage}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(L10nR.string.views_used),
                style = MaterialTheme.typography.bodySmall,
                color = ext.secondaryText,
            )
            Text(
                text = "${promotion.viewsUsed} / ${promotion.maxViews}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { promotion.progressPercentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF9E9E9E),
            trackColor = ext.divider,
        )

        if (promotion.remainingTimeDays != null) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = ext.secondaryText,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(
                        L10nR.string.remaining_days,
                        String.format(Locale.US, "%.1f", promotion.remainingTimeDays),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.secondaryText,
                )
            }
        }
    }
}