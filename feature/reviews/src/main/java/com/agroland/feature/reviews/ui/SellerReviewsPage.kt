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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.reviews.data.SellerReview
import kotlinx.coroutines.launch

/**
 * SellerReviewsPage (Flutter 1:1): GET /seller/{id}/reviews — басында
 * орташа рейтинг карточкасы, астында пікірлер. Like — backend авторитетті
 * жауабымен жаңартылады; өз пікіріңізде Like жасырылады.
 */
@Composable
fun SellerReviewsPage(
    onBack: () -> Unit,
    viewModel: SellerReviewsViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    val networkError = stringResource(L10nR.string.error_no_internet)
    val genericError = stringResource(L10nR.string.error_generic_message)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ReviewEvent.ShowError -> scope.launch {
                    snackbar.showSnackbar(event.error.displayText(networkError, genericError))
                }
                ReviewEvent.Sent -> Unit
            }
        }
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(title = stringResource(L10nR.string.seller_reviews_title), onBack = onBack)
        },
    ) { modifier ->
        Box(modifier = modifier.fillMaxSize()) {
            val items = summary?.items
            when {
                loading -> LoadingWidget()
                error != null -> CenteredContent { ErrorWithRetry(onRetry = viewModel::refresh) }
                items == null || items.isEmpty() -> EmptyView(
                    title = stringResource(L10nR.string.seller_reviews_empty),
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        SellerSummaryCard(
                            avgRating = summary?.avgRating ?: 0.0,
                            total = summary?.total ?: 0,
                        )
                    }
                    items(items, key = { it.id }) { review ->
                        SellerReviewTile(
                            review = review,
                            isOwnReview = currentUserId != null && review.authorId == currentUserId,
                            onToggleLike = { viewModel.toggleLike(review) },
                        )
                    }
                }
            }
            SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** Бас карточка: үлкен жұлдыз + орташа рейтинг + plural саны. */
@Composable
private fun SellerSummaryCard(
    avgRating: Double,
    total: Int,
) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = String.format(java.util.Locale.US, "%.1f", avgRating.coerceIn(0.0, 5.0)),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ext.primaryText,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = pluralStringResource(L10nR.plurals.seller_reviews_count, total, total),
            style = MaterialTheme.typography.bodyMedium,
            color = ext.secondaryText,
        )
    }
}

/** Пікір жолы: аватар + аты (аноним fallback) + күн + жұлдыздар + мәтін + Like. */
@Composable
private fun SellerReviewTile(
    review: SellerReview,
    isOwnReview: Boolean,
    onToggleLike: () -> Unit,
) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Avatar(review)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = review.authorName.ifBlank {
                        stringResource(L10nR.string.anonymous)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = ext.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = DateFormatter.formatDate(review.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                    maxLines = 1,
                )
            }
            if (!isOwnReview) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = review.likeCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.secondaryText,
                    )
                    IconButton(onClick = onToggleLike, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (review.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (review.isLiked) {
                                MaterialTheme.colorScheme.error
                            } else {
                                ext.secondaryText
                            },
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            } else if (review.likeCount > 0) {
                Text(
                    text = review.likeCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        RatingStars(review.rating, starSize = 18)
        if (review.text.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = review.text,
                style = MaterialTheme.typography.bodyMedium,
                color = ext.primaryText,
            )
        }
    }
}

/** Аватар: сурет болмаса — инициалдармен дөңгелек. */
@Composable
private fun Avatar(review: SellerReview) {
    val ext = extendedColors()
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(ext.grey),
        contentAlignment = Alignment.Center,
    ) {
        if (!review.authorAvatarUrl.isNullOrBlank()) {
            CachedImage(
                url = review.authorAvatarUrl,
                contentDescription = review.authorName,
                modifier = Modifier.size(40.dp),
            )
        } else {
            val initial = review.authorName.trim().takeIf { it.isNotBlank() }
                ?.firstOrNull()?.uppercase()
                ?: stringResource(L10nR.string.anonymous).firstOrNull()?.toString().orEmpty()
            Text(
                text = initial,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ext.secondaryText,
            )
        }
    }
}