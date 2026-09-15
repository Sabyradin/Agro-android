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
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.components.ShimmerCard
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.Category

/**
 * SubcategoriesPage — категория атауы + «Барлығы» жолы (бүкіл категория лентасы)
 * + сабкатегория тізімі. CategoriesViewModel кешін қайта пайдаланады.
 */
@Composable
fun SubcategoriesPage(
    categoryId: Int,
    onBack: () -> Unit,
    onOpenFeed: (categoryId: Int?, subcategoryId: Int?) -> Unit,
    viewModel: CategoriesViewModel = rememberCategoriesViewModel(),
) {
    val localeTag = LocalConfiguration.current.locales[0]?.toLanguageTag()
    val parent = viewModel.findCategory(categoryId)
    val subcategories by viewModel.subcategories.collectAsState()
    val subLoading by viewModel.subLoading.collectAsState()
    val subError by viewModel.subError.collectAsState()

    LaunchedEffect(categoryId) { viewModel.loadSubcategories(categoryId) }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = parent?.localizedName(localeTag)
                    ?: stringResource(L10nR.string.categories_title),
                onBack = onBack,
            )
        },
    ) { inner ->
        Column(modifier = inner.fillMaxSize()) {
            // Барлық жарнамалар (бүкіл категория бойынша).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(extendedColors().grey)
                    .clickable { onOpenFeed(categoryId, null) }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.GridView,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(L10nR.string.filter_any_category),
                    style = MaterialTheme.typography.bodySmall,
                    color = extendedColors().primaryText,
                    modifier = Modifier.weight(1f),
                )
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = extendedColors().secondaryText,
                    modifier = Modifier.size(20.dp),
                )
            }

            when {
                subLoading && subcategories.isEmpty() -> Column {
                    repeat(6) { ShimmerCard() }
                }
                subError != null && subcategories.isEmpty() -> CenteredContent {
                    ErrorWithRetry(
                        onRetry = { viewModel.loadSubcategories(categoryId) },
                        message = subError!!.displayText(),
                    )
                }
                subcategories.isEmpty() && !subLoading -> EmptyView(
                    title = stringResource(L10nR.string.empty_generic_title),
                    message = stringResource(L10nR.string.empty_generic_message),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(subcategories, key = { it.id }) { sub ->
                        SubcategoryRow(
                            subcategory = sub,
                            localeTag = localeTag,
                            onClick = { onOpenFeed(categoryId, sub.id) },
                        )
                    }
                    if (subLoading) {
                        item { LoadingWidget(Modifier.fillMaxWidth().padding(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubcategoryRow(
    subcategory: Category,
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
                .size(40.dp)
                .clip(CircleShape)
                .background(ext.grey),
            contentAlignment = Alignment.Center,
        ) {
            CachedImage(
                url = subcategory.iconUrl,
                contentDescription = subcategory.localizedName(localeTag),
                modifier = Modifier.size(40.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subcategory.localizedName(localeTag),
                style = MaterialTheme.typography.bodySmall,
                color = ext.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subcategory.announcementCount > 0) {
                Text(
                    text = stringResource(L10nR.string.category_count, subcategory.announcementCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                )
            }
        }
    }
}