package com.agroland.feature.china.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroland.core.l10n.AppLocale
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.components.ShimmerCard
import com.agroland.core.ui.theme.extendedColors
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.feature.china.data.ChinaParser
import androidx.compose.foundation.lazy.grid.GridItemSpan

/**
 * Категория балалары (Flutter ChinaSubcategoriesPage + ChinaCategoryListView).
 * 2-бақаналы плитка: сурет + атау + тауар саны; баласы бар болса қайтадан
 * осы бетке қарай түседі, әйтпесе — тауар тізіміне.
 */
@Composable
fun ChinaSubcategoriesPage(
    title: String,
    onBack: () -> Unit,
    onOpenSubcategories: (Long, String) -> Unit,
    onOpenProducts: (Long, String) -> Unit,
    viewModel: ChinaSubcategoriesViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val locale = AppLocale.fromTag(LocalConfiguration.current.locales[0]?.toLanguageTag())

    AgroScaffold(
        topBar = { AgroAppBar(title = title, onBack = onBack) },
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                loading -> Column {
                    repeat(4) { ShimmerCard() }
                }
                error != null -> CenteredContent(Modifier.fillMaxSize()) {
                    ErrorWithRetry(onRetry = viewModel::load)
                }
                categories.isEmpty() -> EmptyView(
                    modifier = Modifier.fillMaxSize(),
                    title = stringResource(L10nR.string.nothing_found),
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(categories, key = { it.id }, span = { GridItemSpan(1) }) { category ->
                        ChinaSubcategoryTile(
                            name = ChinaParser.localizedName(category.names, locale),
                            imageUrl = category.imageUrl,
                            productCount = category.productCount,
                            onClick = {
                                if (category.hasChildren) {
                                    onOpenSubcategories(category.id, ChinaParser.localizedName(category.names, locale))
                                } else {
                                    onOpenProducts(category.id, ChinaParser.localizedName(category.names, locale))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Сабкатегория плиткасы — 1:1 сурет, атау, саны. */
@Composable
private fun ChinaSubcategoryTile(
    name: String,
    imageUrl: String?,
    productCount: Int,
    onClick: () -> Unit,
) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ext.card)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(ext.grey),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl != null) {
                CachedImage(
                    url = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = null,
                    tint = ext.secondaryText,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = ext.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2,
            )
            if (productCount > 0) {
                Spacer(Modifier.size(2.dp))
                Text(
                    text = productCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.secondaryText,
                )
            }
        }
    }
}