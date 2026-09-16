package com.agroland.feature.reviews.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.reviews.data.Review
import com.agroland.feature.reviews.data.ReviewAnnouncementInfo

/** Flutter FeedbackAdView — пікір беттерінің басындағы жарнама карточкасы. */
@Composable
fun FeedbackAdCard(
    announcementId: Long,
    modifier: Modifier = Modifier,
    viewModel: ReviewAnnouncementViewModel = hiltViewModel(key = "reviewAd_$announcementId"),
) {
    LaunchedEffect(announcementId) { viewModel.load(announcementId) }
    val info by viewModel.info.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val ext = extendedColors()

    Box(modifier = modifier.fillMaxWidth()) {
        when {
            info == null && loading -> LoadingWidget(Modifier.padding(vertical = 32.dp))
            info != null -> FeedbackAdCardContent(info!!)
        }
    }
}

@Composable
private fun FeedbackAdCardContent(info: ReviewAnnouncementInfo) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(3f)
                .height(80.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ext.grey),
        ) {
            if (info.imageUrl != null) {
                CachedImage(
                    url = info.imageUrl,
                    contentDescription = info.title,
                    modifier = Modifier.height(80.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(7f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = info.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = ext.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val price = PriceFormatter.format(info.price, info.currency ?: "₸")
            if (price.isNotBlank()) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(
                    L10nR.string.published_at,
                    DateFormatter.formatDate(info.createdAt),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = ext.secondaryText,
                maxLines = 1,
            )
        }
    }
}

/** Қатар жұлдызшалар — тек көрсету үшін (ReviewItem, SellerReview tile). */
@Composable
fun RatingStars(
    rating: Int,
    modifier: Modifier = Modifier,
    starSize: Int = 20,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondary,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(5) { i ->
            Icon(
                imageVector = if (i < rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (i < rating) tint else extendedColors().secondaryText,
                modifier = Modifier.size(starSize.dp),
            )
        }
    }
}

/** Интерактивті 1–5 жұлдыз таңдау (SendReviewPage, Flutter IconButton row). */
@Composable
fun RatingInput(
    rating: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        for (i in 1..5) {
            IconButton(onClick = { onSelect(i) }) {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = i.toString(),
                    tint = if (i <= rating) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        extendedColors().secondaryText
                    },
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

/** Flutter ReviewItem — Kaspi-стиль пікір жолы: аты, жұлдыздар, күн, мәтіні. */
@Composable
fun ReviewItem(
    review: Review,
    modifier: Modifier = Modifier,
) {
    val ext = extendedColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ext.card)
            .padding(16.dp),
    ) {
        Text(
            text = review.userName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = ext.primaryText,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RatingStars(review.rating)
            Spacer(Modifier.weight(1f))
            Text(
                text = DateFormatter.formatDate(review.createdAt),
                style = MaterialTheme.typography.bodyMedium,
                color = ext.secondaryText,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = review.text,
            style = MaterialTheme.typography.bodyMedium,
            color = ext.primaryText,
        )
    }
}