package com.agroland.feature.reviews.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.reviews.data.ProfileReview

/**
 * ProfileReviewsPage (Flutter 1:1): GET /user/{id}/reviews — сатушының
 * жарнамалары бойынша пікір жиынтықтары. Карточка басылса — сол жарнаманың
 * пікірлер беті ашылады.
 */
@Composable
fun ProfileReviewsPage(
    onBack: () -> Unit,
    onOpenAnnouncementReviews: (Long) -> Unit,
    viewModel: ProfileReviewsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    AgroScaffold(
        topBar = { AgroAppBar(title = stringResource(L10nR.string.feedbacks), onBack = onBack) },
    ) { modifier ->
        Box(modifier = modifier.fillMaxSize()) {
            when {
                loading -> LoadingWidget()
                error != null -> CenteredContent { ErrorWithRetry(onRetry = viewModel::refresh) }
                items.isEmpty() -> EmptyView(
                    title = stringResource(L10nR.string.no_reviews_title),
                    message = stringResource(L10nR.string.no_reviews_subtitle),
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) { item ->
                        ProfileReviewCard(item = item, onClick = { onOpenAnnouncementReviews(item.id) })
                    }
                }
            }
        }
    }
}

/** Flutter карточкасы: сурет + атау + баға + күн + рейтинг/пікір саны. */
@Composable
private fun ProfileReviewCard(
    item: ProfileReview,
    onClick: () -> Unit,
) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
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
                url = item.mainImageUrl,
                contentDescription = item.title,
                modifier = Modifier.size(92.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = ext.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = PriceFormatter.format(item.price),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(L10nR.string.published_at, DateFormatter.formatDate(item.createdAt)),
                style = MaterialTheme.typography.labelMedium,
                color = ext.secondaryText,
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RatingStars(item.rating, starSize = 16)
                if (item.totalReviews > 0) {
                    Text(
                        text = stringResource(
                            L10nR.string.rating,
                            String.format(java.util.Locale.US, "%.1f", item.rating.toDouble()),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.secondaryText,
                    )
                }
            }
        }
    }
}