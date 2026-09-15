package com.agroland.feature.marketplace.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.components.ShimmerCard
import com.agroland.core.ui.theme.extendedColors

/**
 * FavoritesPage — таңдаулылар: пагинацияланған тізім, жүрек басу — тізімнен
 * бірден алынады (optimistic).
 */
@Composable
fun FavoritesPage(
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
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

    val listState = rememberLazyListState()
    PaginateEffect(
        listState = listState,
        itemCount = items.size,
        exhausted = exhausted,
        loadingMore = loadingMore,
        onLoadMore = viewModel::loadMore,
    )

    AgroScaffold(
        topBar = {
            AgroAppBar(title = stringResource(L10nR.string.favorites_title), onBack = onBack)
        },
    ) { inner ->
        Box(modifier = inner.fillMaxSize()) {
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
                items.isEmpty() -> CenteredContent {
                    EmptyView(
                        icon = Icons.Outlined.FavoriteBorder,
                        title = stringResource(L10nR.string.favorites_empty_title),
                        message = stringResource(L10nR.string.favorites_empty_message),
                    )
                }
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
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}