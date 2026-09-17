package com.agroland.feature.china.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.AppLocale
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.ShimmerBox
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.china.data.ChinaCategory
import com.agroland.feature.china.data.ChinaParser
import com.agroland.feature.china.data.ChinaProduct
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * «Қытай тауарлары» каталог мазмұны — HomeFeedPage CHINA табы (Flutter
 * ChinaCatalogPage). Градиент hero, категория чиптары, «Барлығы» шыты,
 * «Танымал тауарлар» каруселі.
 */
@Composable
fun ChinaCatalogContent(
    onOpenSubcategories: (Long, String) -> Unit,
    onOpenProducts: (Long, String) -> Unit,
    onOpenProduct: (Long) -> Unit,
    viewModel: ChinaCatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locale = AppLocale.fromTag(LocalConfiguration.current.locales[0]?.toLanguageTag())
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showAllCategories by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val error = (event as? ChinaEvent.ShowError)?.error ?: return@collect
            snackbarHostState.showSnackbar(
                error.displayText(
                    context.getString(L10nR.string.error_no_internet),
                    context.getString(L10nR.string.error_generic_message),
                ),
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Қалқымалы төменгі навигация соңғы карточканы жаппауы үшін.
            contentPadding = PaddingValues(
                bottom = com.agroland.core.ui.components.shellBottomPadding(extra = 24.dp),
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ChinaHeroCard() }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(L10nR.string.china_categories_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors().primaryText,
                    )
                    Spacer(Modifier.weight(1f))
                    if (state.categories.isNotEmpty()) {
                        TextButton(onClick = { showAllCategories = true }) {
                            Text(stringResource(L10nR.string.china_more_categories))
                        }
                    }
                }
            }

            item {
                when {
                    state.categoriesLoading -> ChinaCategoriesSkeleton()
                    state.categoriesError != null ->
                        ErrorWithRetry(onRetry = viewModel::retryCategories)
                    else -> ChinaCategoryRow(
                        categories = state.categories,
                        locale = locale,
                        onOpenSubcategories = onOpenSubcategories,
                        onOpenProducts = onOpenProducts,
                    )
                }
            }

            if (state.popular.isNotEmpty() || state.popularLoading) {
                item {
                    Text(
                        text = stringResource(L10nR.string.china_popular_products),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors().primaryText,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                item {
                    if (state.popularLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            repeat(3) { ChinaProductSkeleton() }
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.popular, key = { it.productId }) { product ->
                                ChinaProductCard(
                                    product = product,
                                    locale = locale,
                                    modifier = Modifier.width(150.dp),
                                    onClick = { onOpenProduct(product.productId) },
                                )
                            }
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

    if (showAllCategories && state.categories.isNotEmpty()) {
        ChinaAllCategoriesSheet(
            categories = state.categories,
            locale = locale,
            onDismiss = { showAllCategories = false },
            onOpenSubcategories = onOpenSubcategories,
            onOpenProducts = onOpenProducts,
        )
    }
}

/** Hero карточкасы — жасыл градиент, MercuryX жөнелту белгісімен. */
@Composable
private fun ChinaHeroCard() {
    val ext = extendedColors()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        ext.primaryLight,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalShipping,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(L10nR.string.home_tab_china),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(L10nR.string.china_catalog_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.92f),
            )
        }
    }
}

/** Категория чиптарының жүктеу скелеті. */
@Composable
private fun ChinaCategoriesSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(4) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ShimmerBox(
                    modifier = Modifier.size(84.dp),
                    height = 84,
                )
                Spacer(Modifier.height(6.dp))
                ShimmerBox(modifier = Modifier.width(72.dp), height = 10)
            }
        }
    }
}

/** Горизонталды категория жолы — сурет + атау + тауар саны. */
@Composable
private fun ChinaCategoryRow(
    categories: List<ChinaCategory>,
    locale: AppLocale,
    onOpenSubcategories: (Long, String) -> Unit,
    onOpenProducts: (Long, String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(categories, key = { it.id }) { category ->
            ChinaCategoryChip(
                category = category,
                locale = locale,
                onClick = {
                    category.clickAction(locale, onOpenSubcategories, onOpenProducts)
                },
            )
        }
    }
}

/** Категория таңдау әрекеті: баласы болса — сабкатегория, болмаса — тауарлар. */
private fun ChinaCategory.clickAction(
    locale: AppLocale,
    onOpenSubcategories: (Long, String) -> Unit,
    onOpenProducts: (Long, String) -> Unit,
) {
    val name = ChinaParser.localizedName(names, locale)
    if (hasChildren) {
        onOpenSubcategories(id, name)
    } else {
        onOpenProducts(id, name)
    }
}

/** Жалғыз категория чипы — 84dp сурет, атау, саны. */
@Composable
fun ChinaCategoryChip(
    category: ChinaCategory,
    locale: AppLocale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ext = extendedColors()
    Column(
        modifier = modifier
            .width(92.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(ext.card),
            contentAlignment = Alignment.Center,
        ) {
            if (category.imageUrl != null) {
                CachedImage(
                    url = category.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = null,
                    tint = ext.secondaryText,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = ChinaParser.localizedName(category.names, locale),
            style = MaterialTheme.typography.labelMedium,
            color = ext.primaryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        if (category.productCount > 0) {
            Text(
                text = category.productCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = ext.secondaryText,
            )
        }
    }
}

/** «Барлығы» толық категория тізімі — ModalBottomSheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChinaAllCategoriesSheet(
    categories: List<ChinaCategory>,
    locale: AppLocale,
    onDismiss: () -> Unit,
    onOpenSubcategories: (Long, String) -> Unit,
    onOpenProducts: (Long, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ext = extendedColors()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ext.card,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(L10nR.string.china_categories_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ext.primaryText,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(categories, key = { it.id }) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                category.clickAction(locale, onOpenSubcategories, onOpenProducts)
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ext.grey),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (category.imageUrl != null) {
                                CachedImage(
                                    url = category.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Image,
                                    contentDescription = null,
                                    tint = ext.secondaryText,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = ChinaParser.localizedName(category.names, locale),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.primaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (category.productCount > 0) {
                            Text(
                                text = category.productCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.secondaryText,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = ext.secondaryText,
                        )
                    }
                }
            }
        }
    }
}

/** Тауар картасы — карусель мен grid ортақ (Flutter ChinaProductCard). */
@Composable
fun ChinaProductCard(
    product: ChinaProduct,
    locale: AppLocale,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ext = extendedColors()
    val price = PriceFormatter.format(product.price, currency = "₸")
    Column(
        modifier = modifier
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
            ChinaThumb(imageUrl = product.images.firstOrNull())
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = ChinaParser.localizedName(product.names, locale),
                style = MaterialTheme.typography.labelMedium,
                color = ext.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = price,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (product.minQty > 1) {
                Text(
                    text = stringResource(L10nR.string.china_min_qty_label, product.minQty.toString()),
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.secondaryText,
                )
            }
        }
    }
}

/** Тауар скелеті — карусель жүктелгенде. */
@Composable
private fun ChinaProductSkeleton() {
    ShimmerBox(
        modifier = Modifier
            .width(150.dp)
            .height(230.dp),
        height = 230,
    )
}