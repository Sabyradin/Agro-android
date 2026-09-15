package com.agroland.feature.marketplace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.ShimmerCard
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.Category

/**
 * CategoriesPage — 6 bucket (crops/livestock/products/technology/services/other),
 * әрқайсысы категория жолдарымен; басу — сабкатегориялар экранына ашылады.
 * Activity-scoped CategoriesViewModel — кеш бір рет жүктеледі.
 */
@Composable
fun CategoriesPage(
    onBack: () -> Unit,
    onOpenSubcategories: (Int) -> Unit,
    viewModel: CategoriesViewModel = rememberCategoriesViewModel(),
) {
    val localeTag = LocalConfiguration.current.locales[0]?.toLanguageTag()
    val grouped by viewModel.grouped.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    AgroScaffold(
        topBar = {
            AgroAppBar(title = stringResource(L10nR.string.categories_title), onBack = onBack)
        },
    ) { inner ->
        when {
            loading && grouped.values.all { it.isEmpty() } -> Column(modifier = inner) {
                repeat(8) { ShimmerCard() }
            }
            error != null && grouped.values.all { it.isEmpty() } -> CenteredContent(modifier = inner) {
                ErrorWithRetry(
                    onRetry = viewModel::refresh,
                    message = error!!.displayText(),
                )
            }
            else -> LazyColumn(
                modifier = inner.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val bucketTitles = listOf(
                    "crops" to L10nR.string.category_bucket_crops,
                    "livestock" to L10nR.string.category_bucket_livestock,
                    "products" to L10nR.string.category_bucket_products,
                    "technology" to L10nR.string.category_bucket_technology,
                    "services" to L10nR.string.category_bucket_services,
                    "other" to L10nR.string.category_bucket_other,
                )
                bucketTitles.forEach { (bucket, titleRes) ->
                    val categories = grouped[bucket].orEmpty()
                    if (categories.isEmpty()) return@forEach
                    item(key = "header_$bucket") {
                        Text(
                            text = stringResource(titleRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = extendedColors().primaryText,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(categories, key = { it.id }) { category ->
                        CategoryRow(
                            category = category,
                            localeTag = localeTag,
                            onClick = { onOpenSubcategories(category.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    localeTag: String?,
    onClick: () -> Unit,
) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(ext.grey),
            contentAlignment = Alignment.Center,
        ) {
            CachedImage(
                url = category.iconUrl,
                contentDescription = category.localizedName(localeTag),
                modifier = Modifier.size(44.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.localizedName(localeTag),
                style = MaterialTheme.typography.bodySmall,
                color = ext.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (category.announcementCount > 0) {
                Text(
                    text = stringResource(L10nR.string.category_count, category.announcementCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                )
            }
        }
        androidx.compose.material3.Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = ext.secondaryText,
            modifier = Modifier.size(20.dp),
        )
    }
}