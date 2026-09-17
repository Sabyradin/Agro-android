package com.agroland.feature.demand.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.ShimmerCard
import com.agroland.core.ui.theme.extendedColors
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.feature.demand.data.DemandItem

/**
 * «Сұраныстарым» — пайдаланушының сұраныстар тізімі (Flutter-де UI жоқ —
 * спек қосымшасы). Парақталған карточкалар + жаңа сұраныс FAB.
 */
@Composable
fun DemandListPage(
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onOpenCreate: () -> Unit,
    viewModel: DemandListViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val loadingMore by viewModel.loadingMore.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            items.isNotEmpty() && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val showError = event as? DemandEvent.ShowError ?: return@collect
            snackbarHostState.showSnackbar(
                showError.error.displayText(
                    context.getString(L10nR.string.error_no_internet),
                    context.getString(L10nR.string.error_generic_message),
                ),
            )
        }
    }

    AgroScaffold(
        topBar = { AgroAppBar(title = stringResource(L10nR.string.demand_list_title), onBack = onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenCreate,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.padding(start = 6.dp))
                Text(
                    text = stringResource(L10nR.string.demand_create),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                loading -> Column {
                    repeat(4) { ShimmerCard() }
                }
                error != null && items.isEmpty() -> CenteredContent(Modifier.fillMaxSize()) {
                    ErrorWithRetry(onRetry = viewModel::loadFirst)
                }
                items.isEmpty() -> EmptyView(
                    modifier = Modifier.fillMaxSize(),
                    title = stringResource(L10nR.string.nothing_found),
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 96.dp,
                    ),
                ) {
                    items(items, key = { it.id }) { demand ->
                        DemandCard(
                            demand = demand,
                            onClick = { onOpenDetail(demand.id) },
                        )
                    }
                    if (loadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(strokeWidth = 3.dp)
                            }
                        }
                    }
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
            )
        }
    }
}

/** Сұраныс карточкасы — тақырып, сипаттама, шекті баға, мөлшер, күй чипі. */
@Composable
fun DemandCard(
    demand: DemandItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ext = extendedColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.card)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = demand.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ext.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.padding(start = 8.dp))
            DemandStatusChip(isActive = demand.isActive)
        }
        if (!demand.description.isNullOrBlank()) {
            Text(
                text = demand.description,
                style = MaterialTheme.typography.bodySmall,
                color = ext.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (demand.maxPrice != null) {
                Text(
                    text = PriceFormatter.format(demand.maxPrice, demand.currency ?: "₸"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (demand.quantity != null) {
                Text(
                    text = formatQuantity(demand),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            Text(
                text = DateFormatter.formatDate(demand.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = ext.secondaryText,
            )
        }
    }
}

/** «мөлшер + өлшем бірлігі» жолы. */
private fun formatQuantity(demand: DemandItem): String {
    val quantity = demand.quantity?.let {
        if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
    } ?: return ""
    return quantity + (demand.measurementUnit?.let { " $it" } ?: "")
}

/** Белсенді/белсенді емес күй чипі. */
@Composable
fun DemandStatusChip(isActive: Boolean, modifier: Modifier = Modifier) {
    val ext = extendedColors()
    val (label, container, content) = if (isActive) {
        Triple(
            L10nR.string.demand_status_active,
            MaterialTheme.colorScheme.primary,
            androidx.compose.ui.graphics.Color.White,
        )
    } else {
        Triple(L10nR.string.demand_status_inactive, ext.grey, ext.secondaryText)
    }
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}