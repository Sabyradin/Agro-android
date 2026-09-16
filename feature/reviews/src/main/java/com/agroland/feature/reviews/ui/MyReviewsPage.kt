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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.reviews.data.MyReviewCandidate
import kotlinx.coroutines.launch

/**
 * «Менің пікірлерім» (spec §10, iOS MyReviewsStore 1:1): клиент жағынан
 * жинақталған үміткерлер (қаралған жарнамалар + тапсырыстар) екі қойындыға
 * бөлінеді. VM — MainActivity scope-та (чат бейджімен бөлісіледі).
 */
@Composable
fun MyReviewsPage(
    viewModel: MyReviewsViewModel,
    onBack: () -> Unit,
    onOpenSendReview: (Long) -> Unit,
) {
    val pending by viewModel.pending.collectAsState()
    val done by viewModel.done.collectAsState()
    val loading by viewModel.loading.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val networkError = stringResource(L10nR.string.error_no_internet)
    val genericError = stringResource(L10nR.string.error_generic_message)

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
            AgroAppBar(
                title = stringResource(L10nR.string.my_reviews_title),
                onBack = onBack,
            )
        },
    ) { modifier ->
        Column(modifier = modifier.fillMaxSize()) {
            val tabs = listOf(
                stringResource(L10nR.string.my_reviews_pending_tab),
                stringResource(L10nR.string.my_reviews_done_tab),
            )
            var tabIndex by remember { mutableIntStateOf(0) }
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { i, label ->
                    Tab(
                        selected = tabIndex == i,
                        onClick = { tabIndex = i },
                        text = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (tabIndex == i) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                val list = if (tabIndex == 0) pending else done
                when {
                    loading && list.isEmpty() -> LoadingWidget()
                    list.isEmpty() -> EmptyView(
                        title = stringResource(
                            if (tabIndex == 0) {
                                L10nR.string.my_reviews_pending_empty
                            } else {
                                L10nR.string.my_reviews_done_empty
                            },
                        ),
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                    ) {
                        items(list, key = { it.announcementId }) { candidate ->
                            MyReviewCandidateCard(
                                candidate = candidate,
                                showPending = tabIndex == 0,
                                onLeaveReview = { onOpenSendReview(candidate.announcementId) },
                            )
                        }
                    }
                }
                SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

/** Үміткер карточкасы: pending — «Пікір қалдыру» батырмасы; done — өз пікірі. */
@Composable
private fun MyReviewCandidateCard(
    candidate: MyReviewCandidate,
    showPending: Boolean,
    onLeaveReview: () -> Unit,
) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .padding(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ext.grey),
            ) {
                CachedImage(
                    url = candidate.imageUrl,
                    contentDescription = candidate.title,
                    modifier = Modifier.size(72.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = candidate.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = ext.primaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                val price = PriceFormatter.format(candidate.price, candidate.currency ?: "₸")
                if (price.isNotBlank()) {
                    Text(
                        text = price,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (showPending) {
                    Text(
                        text = stringResource(L10nR.string.you_viewed_announcement),
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.secondaryText,
                        maxLines = 2,
                    )
                }
            }
        }

        if (showPending) {
            Spacer(Modifier.height(10.dp))
            AgroButton(
                text = stringResource(L10nR.string.leave_feedback),
                onClick = onLeaveReview,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            candidate.ownReview?.let { review ->
                Spacer(Modifier.height(8.dp))
                RatingStars(review.rating, starSize = 16)
                if (review.text.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = review.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.primaryText,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = DateFormatter.formatDate(review.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                )
            }
        }
    }
}