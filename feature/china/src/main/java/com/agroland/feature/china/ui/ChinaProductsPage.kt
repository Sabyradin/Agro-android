package com.agroland.feature.china.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroland.core.l10n.AppLocale
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.components.ShimmerCard
import com.agroland.core.ui.theme.extendedColors
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Категория тауарлары — 2-бақаналы grid + шексіз жүктеу
 * (Flutter ChinaProductsPage + PaginateEffect).
 */
@Composable
fun ChinaProductsPage(
    title: String,
    onBack: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    viewModel: ChinaProductsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val loadingMore by viewModel.loadingMore.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val locale = AppLocale.fromTag(LocalConfiguration.current.locales[0]?.toLanguageTag())
    val gridState = rememberLazyGridState()

    // Шексіз жүктеу: соңғы толы көрінбейтін жолға жақындағанда келесі бет.
    val shouldLoadMore by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            items.isNotEmpty() && lastVisible >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    AgroScaffold(
        topBar = { AgroAppBar(title = title, onBack = onBack) },
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                loading -> Column {
                    repeat(6) { ShimmerCard() }
                }
                error != null && items.isEmpty() -> CenteredContent(Modifier.fillMaxSize()) {
                    ErrorWithRetry(onRetry = viewModel::loadFirst)
                }
                items.isEmpty() -> EmptyView(
                    modifier = Modifier.fillMaxSize(),
                    title = stringResource(L10nR.string.nothing_found),
                )
                else -> LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items, key = { it.productId }) { product ->
                        ChinaProductCard(
                            product = product,
                            locale = locale,
                            onClick = { onOpenProduct(product.productId) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (loadingMore) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(strokeWidth = 3.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}