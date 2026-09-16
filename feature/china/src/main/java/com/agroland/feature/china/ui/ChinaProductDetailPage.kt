package com.agroland.feature.china.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.BottomActionContainer
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.ShimmerCard
import com.agroland.core.ui.theme.extendedColors
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.feature.china.data.ChinaOfferDetail
import com.agroland.feature.china.data.ChinaParser
import com.agroland.feature.china.data.ChinaProductDetail
import com.agroland.feature.china.data.pickChinaLocalized
import com.agroland.feature.china.data.stripChinaHtml

/**
 * MercuryX тауарының толық карточкасы (Flutter ChinaProductDetailPage):
 * галерея-pager + нүктелер, SKU-оффер чиптары, қойма, көтерме бағалар,
 * HTML-сипаттама, атрибуттар, сан тандайы, тұрақты «Корзинаға қосу».
 */
@Composable
fun ChinaProductDetailPage(
    onBack: () -> Unit,
    viewModel: ChinaProductDetailViewModel = hiltViewModel(),
    cartViewModel: ChinaCartViewModel = hiltViewModel(),
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val adding by cartViewModel.adding.collectAsStateWithLifecycle()

    val locale = AppLocale.fromTag(LocalConfiguration.current.locales[0]?.toLanguageTag())
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var addedDialog by remember { mutableStateOf(false) }
    var selectedOffer by remember { mutableIntStateOf(0) }

    val title = detail?.let { ChinaParser.localizedName(it.names, locale) } ?: ""

    LaunchedEffect(Unit) {
        cartViewModel.events.collect { event ->
            when (event) {
                is ChinaEvent.ShowError -> snackbarHostState.showSnackbar(
                    event.error.displayText(
                        context.getString(L10nR.string.error_no_internet),
                        context.getString(L10nR.string.error_generic_message),
                    ),
                )
                is ChinaEvent.ItemAdded -> addedDialog = true
                else -> Unit
            }
        }
    }

    AgroScaffold(
        topBar = { AgroAppBar(title = title, onBack = onBack) },
        bottomBar = {
            if (detail != null) {
                BottomActionContainer {
                    DetailBottomBar(
                        detail = detail!!,
                        locale = locale,
                        selectedOffer = selectedOffer,
                        adding = adding,
                        onAddToCart = { quantity, skuId, price, name, image ->
                            cartViewModel.addItem(
                                productId = detail!!.productId,
                                skuId = skuId,
                                quantity = quantity,
                                minQty = detail!!.minQty,
                                price = price,
                                titleSnapshot = name,
                                imageUrl = image,
                            )
                        },
                    )
                }
            }
        },
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                loading -> Column {
                    ShimmerCard()
                    ShimmerCard()
                }
                error != null && detail == null -> CenteredContent(Modifier.fillMaxSize()) {
                    ErrorWithRetry(onRetry = viewModel::load)
                }
                detail == null || detail!!.productId <= 0 -> EmptyView(
                    modifier = Modifier.fillMaxSize(),
                    title = stringResource(L10nR.string.nothing_found),
                )
                else -> DetailContent(detail!!, locale, selectedOffer) { selectedOffer = it }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp),
            )
        }
    }

    if (addedDialog) {
        AlertDialog(
            onDismissRequest = { addedDialog = false },
            confirmButton = {
                TextButton(onClick = { addedDialog = false }) {
                    Text(stringResource(L10nR.string.common_close))
                }
            },
            title = {
                Text(stringResource(L10nR.string.china_added_to_cart))
            },
        )
    }
}

/** Толық карточка мазмұны. */
@Composable
private fun DetailContent(
    detail: ChinaProductDetail,
    locale: AppLocale,
    selectedOffer: Int,
    onSelectOffer: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Галерея
        item { ChinaGallery(detail) }

        // Атау + баға + мин. саны
        item {
            val ext = extendedColors()
            val offerPrice = detail.offers.getOrNull(selectedOffer)?.price
                ?.takeIf { it > 0 } ?: detail.price
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = ChinaParser.localizedName(detail.names, locale),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ext.primaryText,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = PriceFormatter.format(offerPrice),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    if (detail.minQty > 1) {
                        Text(
                            text = stringResource(L10nR.string.china_min_qty_label, detail.minQty.toString()),
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.secondaryText,
                        )
                    }
                }
            }
        }

        // SKU-офферлер
        if (detail.offers.isNotEmpty()) {
            item { ChinaOffersSection(detail, locale, selectedOffer, onSelectOffer) }
        }

        // Көтерме бағалар
        if (detail.bulkPricing.isNotEmpty()) {
            item { ChinaBulkSection(detail) }
        }

        // Сипаттама
        val description = stripChinaHtml(detail.description)
        if (description != null) {
            item {
                DetailSection(title = stringResource(L10nR.string.china_information)) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = extendedColors().primaryText,
                    )
                }
            }
        }

        // Атрибуттар
        if (detail.attributes.isNotEmpty()) {
            item { ChinaAttributesSection(detail, locale) }
        }
    }
}

/** Галерея — HorizontalPager + нүкте индикаторы. */
@Composable
private fun ChinaGallery(detail: ChinaProductDetail) {
    val ext = extendedColors()
    val images = detail.images
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(ext.grey),
    ) {
        if (images.isEmpty()) {
            ChinaThumb(imageUrl = null, modifier = Modifier.fillMaxSize())
        } else if (images.size == 1) {
            ChinaThumb(imageUrl = images.first(), modifier = Modifier.fillMaxSize())
        } else {
            val pagerState = rememberPagerState(pageCount = { images.size })
            HorizontalPager(state = pagerState) { page ->
                ChinaThumb(imageUrl = images[page], modifier = Modifier.fillMaxSize())
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                repeat(images.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(if (selected) RoundedCornerShape(4.dp) else CircleShape)
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    ext.secondaryText.copy(alpha = 0.5f)
                                },
                            ),
                    )
                }
            }
        }
    }
}

/** SKU-оффер чиптары — таңдалған оффер бағаны/қойманы анықтайды. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ChinaOffersSection(
    detail: ChinaProductDetail,
    locale: AppLocale,
    selectedOffer: Int,
    onSelectOffer: (Int) -> Unit,
) {
    DetailSection(title = stringResource(L10nR.string.china_choose_offer)) {
        val ext = extendedColors()
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            detail.offers.forEachIndexed { index, offer ->
                val isSelected = index == selectedOffer
                val stock = offer.stock
                Surface(
                    onClick = { onSelectOffer(index) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else ext.grey,
                    contentColor = if (isSelected) androidx.compose.ui.graphics.Color.White else ext.primaryText,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = if (isSelected) androidx.compose.ui.graphics.Color.Transparent else ext.divider,
                    ),
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = offerSkuLabel(offer, locale),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Text(
                            text = PriceFormatter.format(offer.price.takeIf { it > 0 } ?: detail.price),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        if (stock != null) {
                            Text(
                                text = if (stock > 0) {
                                    stringResource(L10nR.string.china_in_stock, stock.toString())
                                } else {
                                    stringResource(L10nR.string.china_under_order)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) {
                                    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                                } else {
                                    ext.secondaryText
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Оффер SKU-белгісі — `value_trans ?? value` қосындысы. */
private fun offerSkuLabel(offer: ChinaOfferDetail, locale: AppLocale): String {
    val label = offer.skuAttributes.joinToString(", ") { attr ->
        pickChinaLocalized(attr["value_trans"] ?: attr["value"], locale)
    }.trim()
    return label.takeIf { it.isNotBlank() } ?: offer.externalSkuId?.toString() ?: "—"
}

/** Көтерме баға деңгейлері. */
@Composable
private fun ChinaBulkSection(detail: ChinaProductDetail) {
    val ext = extendedColors()
    DetailSection(title = stringResource(L10nR.string.china_bulk_pricing)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            detail.bulkPricing.sortedBy { it.minQty }.forEach { tier ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ext.grey)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(L10nR.string.china_min_qty_label, tier.minQty.toString()),
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.primaryText,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = PriceFormatter.format(tier.price),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** Атрибут жолдары — `attribute_name_trans ?? attribute_name`: value. */
@Composable
private fun ChinaAttributesSection(detail: ChinaProductDetail, locale: AppLocale) {
    val ext = extendedColors()
    DetailSection(title = stringResource(L10nR.string.china_attributes)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            detail.attributes.forEach { attr ->
                val name = pickChinaLocalized(attr["attribute_name_trans"] ?: attr["attribute_name"], locale)
                val value = pickChinaLocalized(attr["value_trans"] ?: attr["value"], locale)
                if (name.isNotBlank() || value.isNotBlank()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.secondaryText,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = ext.primaryText,
                        )
                    }
                }
            }
        }
    }
}

/** Секция контейнері — карточкалы атау + мазмұн. */
@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ext.card)
            .padding(14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ext.primaryText,
        )
        Spacer(Modifier.height(10.dp))
        content()
    }
}

/** Тұрақты астыңғы панель — баға + тандай + «Корзинаға қосу». */
@Composable
private fun DetailBottomBar(
    detail: ChinaProductDetail,
    locale: AppLocale,
    selectedOffer: Int,
    adding: Boolean,
    onAddToCart: (quantity: Int, skuId: Long?, price: Double, name: String, imageUrl: String?) -> Unit,
) {
    val ext = extendedColors()
    var quantity by remember(detail.productId) {
        mutableIntStateOf(maxOf(detail.minQty, 1))
    }
    val name = ChinaParser.localizedName(detail.names, locale)
    val offer = detail.offers.getOrNull(selectedOffer)
    val offerPrice = offer?.price?.takeIf { it > 0 } ?: detail.price

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = PriceFormatter.format(offerPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(L10nR.string.china_quantity) + ": " + quantity,
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                )
            }
            ChinaQuantityStepper(
                quantity = quantity,
                minQuantity = detail.minQty,
                onChange = { quantity = it },
            )
        }
        Spacer(Modifier.height(10.dp))
        AgroButton(
            text = stringResource(L10nR.string.china_add_to_cart),
            onClick = {
                onAddToCart(
                    quantity,
                    offer?.externalSkuId,
                    offerPrice,
                    name,
                    detail.images.firstOrNull(),
                )
            },
            loading = adding,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}