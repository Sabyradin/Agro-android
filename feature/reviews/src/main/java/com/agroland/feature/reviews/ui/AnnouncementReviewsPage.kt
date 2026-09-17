package com.agroland.feature.reviews.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.reviews.data.Review

/**
 * AnnouncementReviewsPage (Flutter 1:1): үстінде FeedbackAdCard, астында
 * пікірлер тізімі (ReviewItem), бос болса EmptyView.
 */
@Composable
fun AnnouncementReviewsPage(
    announcementId: Long,
    onBack: () -> Unit,
    viewModel: AnnouncementReviewsViewModel = hiltViewModel(),
) {
    val reviews by viewModel.reviews.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    AgroScaffold(
        topBar = { AgroAppBar(title = stringResource(L10nR.string.feedbacks), onBack = onBack) },
    ) { modifier ->
        Box(modifier = modifier.fillMaxSize()) {
            when {
                loading && reviews.isEmpty() -> LoadingWidget()
                error != null && reviews.isEmpty() -> CenteredContent {
                    ErrorWithRetry(onRetry = viewModel::refresh)
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        FeedbackAdCard(
                            announcementId = announcementId,
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(extendedColors().card)
                                .padding(16.dp),
                        )
                    }
                    if (reviews.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp)) {
                                EmptyView(
                                    title = stringResource(L10nR.string.no_reviews_title),
                                    message = stringResource(L10nR.string.no_reviews_subtitle),
                                )
                            }
                        }
                    } else {
                        item { Spacer(Modifier.height(8.dp)) }
                        items(reviews, key = { it.id }) { review ->
                            ReviewItem(review)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Kaspi-стиль пікірлер бөлімі — деталь бетіне ендіріледі (spec §10):
 * орташа рейтинг + саны, 3 превью, «Барлық пікірлер» батырмасы.
 * Кілттелген VM (initialize) — толық беттің VM-імен шатаспайды.
 */
@Composable
fun AnnouncementReviewsSection(
    announcementId: Long,
    rating: Double,
    reviewsCount: Int,
    onOpenReviews: () -> Unit,
    viewModel: AnnouncementReviewsViewModel = hiltViewModel(key = "detailReviews_$announcementId"),
) {
    androidx.compose.runtime.LaunchedEffect(announcementId) {
        viewModel.initialize(announcementId)
    }
    val reviews by viewModel.reviews.collectAsState()
    val ext = extendedColors()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(L10nR.string.feedbacks),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ext.primaryText,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = String.format(java.util.Locale.US, "%.1f", rating.coerceIn(0.0, 5.0)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ext.primaryText,
            )
            if (reviewsCount > 0) {
                Text(
                    text = "· $reviewsCount",
                    style = MaterialTheme.typography.titleMedium,
                    color = ext.secondaryText,
                )
            }
        }

        val previews = reviews.take(3)
        if (previews.isEmpty() && reviewsCount == 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(L10nR.string.no_reviews_title),
                style = MaterialTheme.typography.bodyMedium,
                color = ext.secondaryText,
            )
        } else if (previews.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            previews.forEachIndexed { index, review ->
                ReviewPreview(review)
                if (index != previews.lastIndex) {
                    HorizontalDivider(color = ext.divider, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        AgroButton(
            text = stringResource(L10nR.string.all_reviews),
            onClick = onOpenReviews,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Превью: аты + күн + жұлдыздар + мәтін (2 жол шектеледі). */
@Composable
private fun ReviewPreview(review: Review) {
    val ext = extendedColors()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = review.userName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = ext.primaryText,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = com.agroland.core.common.formatters.DateFormatter.formatDate(review.createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = ext.secondaryText,
            )
        }
        Spacer(Modifier.height(4.dp))
        RatingStars(review.rating, starSize = 16)
        if (review.text.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = review.text,
                style = MaterialTheme.typography.bodySmall,
                color = ext.primaryText,
                maxLines = 2,
            )
        }
    }
}