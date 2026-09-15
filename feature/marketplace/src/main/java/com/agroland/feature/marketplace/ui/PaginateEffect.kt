package com.agroland.feature.marketplace.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

/** Иілімді жүктеу: соңғы элементке жақындағанда келесі порция сұралады. */
@Composable
fun PaginateEffect(
    listState: LazyListState,
    itemCount: Int,
    exhausted: Boolean,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
) {
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            itemCount > 0 && last >= itemCount - 4
        }
    }
    LaunchedEffect(shouldLoadMore, exhausted, loadingMore) {
        if (shouldLoadMore && !exhausted && !loadingMore) onLoadMore()
    }
}