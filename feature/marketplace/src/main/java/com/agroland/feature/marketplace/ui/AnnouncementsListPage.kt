package com.agroland.feature.marketplace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroIconButton
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.components.ShimmerCard
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.AnnouncementFilter

/**
 * AnnouncementsListPage — сүзгіленген жарнама лентасы: 4 regular + 2 VIP аралас,
 * иілімді жүктеу, сүзгі жолағы (FilterPage нәтижесі осы бетке қайтып келеді).
 */
@Composable
fun AnnouncementsListPage(
    filter: AnnouncementFilter,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onOpenFilter: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
    // Фаза 15: HotAnnouncementsPage осы бетті қайта пайдаланады —
    // тақырып пен бос тізім мәтінін алмастырады (type_ad=vip лентасы).
    titleOverride: String? = null,
    emptyMessageOverride: String? = null,
    showFilterControls: Boolean = true,
) {
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val loadingMore by viewModel.loadingMore.collectAsState()
    val exhausted by viewModel.exhausted.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val noInternetText = stringResource(L10nR.string.error_no_internet)
    val genericErrorText = stringResource(L10nR.string.error_generic_message)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is MarketplaceEvent.ShowError) {
                snackbar.showSnackbar(event.error.displayText(noInternetText, genericErrorText))
            }
        }
    }

    // FilterPage-тен келген немесе route параметріндегі сүзгі — бір loadFirst шақырады.
    LaunchedEffect(filter) { viewModel.applyFilter(filter) }

    val listState = rememberLazyListState()
    PaginateEffect(
        listState = listState,
        itemCount = items.size,
        exhausted = exhausted,
        loadingMore = loadingMore,
        onLoadMore = viewModel::loadMore,
    )

    val title = titleOverride ?: filter.query?.takeIf { it.isNotBlank() }
        ?: stringResource(L10nR.string.announcements_title)

    com.agroland.core.ui.components.AgroScaffold(
        topBar = {
            com.agroland.core.ui.components.AgroAppBar(
                title = title,
                onBack = onBack,
                actions = if (showFilterControls) {
                    {
                        AgroIconButton(
                            icon = Icons.Rounded.FilterAlt,
                            contentDescription = stringResource(L10nR.string.home_filter),
                            onClick = onOpenFilter,
                        )
                    }
                } else {
                    {}
                },
            )
        },
    ) { inner ->
        Box(modifier = inner.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (showFilterControls) {
                    FilterSummaryBar(filter = filter, onOpenFilter = onOpenFilter)
                }

                when {
                    loading -> Column {
                        repeat(6) { ShimmerCard() }
                    }
                    error != null -> CenteredContent {
                        ErrorWithRetry(
                            onRetry = viewModel::refresh,
                            message = error!!.displayText(),
                        )
                    }
                    items.isEmpty() -> EmptyView(
                        title = stringResource(L10nR.string.feed_empty_title),
                        message = emptyMessageOverride
                            ?: stringResource(L10nR.string.feed_empty_message),
                    )
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(items, key = { it.id }) { item ->
                            AnnouncementCard(
                                item = item,
                                onClick = { onOpenDetail(item.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                            )
                        }
                        if (loadingMore) {
                            item { LoadingWidget(Modifier.fillMaxWidth().padding(16.dp)) }
                        }
                    }
                }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** Ағымдағы сүзгілердің қысқаша жолағы — қолданылған сүзгілер саны + өңдеу. */
@Composable
private fun FilterSummaryBar(
    filter: AnnouncementFilter,
    onOpenFilter: () -> Unit,
) {
    val ext = extendedColors()
    val activeCount = listOfNotNull(
        filter.query?.takeIf { it.isNotBlank() },
        filter.categoryId?.let { "cat" },
        filter.subcategoryId?.let { "sub" },
        filter.minPrice?.let { "min" },
        filter.maxPrice?.let { "max" },
        filter.negotiable?.let { "neg" },
        filter.sort.takeIf { it != com.agroland.feature.marketplace.data.FilterSort.DEFAULT }
            ?.let { "sort" },
        filter.location?.let { "loc" },
    ).size
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ext.card)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (activeCount == 0) {
                stringResource(L10nR.string.filter_none)
            } else {
                stringResource(L10nR.string.filter_active_count, activeCount)
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (activeCount == 0) ext.secondaryText else MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        com.agroland.core.ui.components.AgroTextButton(
            text = stringResource(L10nR.string.home_filter),
            onClick = onOpenFilter,
        )
    }
}