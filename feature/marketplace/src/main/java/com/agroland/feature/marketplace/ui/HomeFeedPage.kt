package com.agroland.feature.marketplace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroIconButton
import com.agroland.core.ui.components.AgroSearchField
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.components.ShimmerGridCard
import com.agroland.core.ui.components.shellBottomPadding
import com.agroland.core.ui.theme.AgroRadius
import com.agroland.core.ui.theme.AgroSize
import com.agroland.core.ui.theme.AgroSpacing
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.AnnouncementFilter
import com.agroland.feature.marketplace.data.Suggestion
import com.agroland.feature.stories.ui.MainBannerCarousel
import com.agroland.feature.stories.ui.StoriesRow

/** Home лентасының режимдері — iOS үш сегменті. */
private enum class HomeFeedTab(val labelRes: Int) {
    ANNOUNCEMENTS(L10nR.string.home_tab_announcements),
    AGRO(L10nR.string.home_tab_agro),

    /** MercuryX каталогы (бұрын «Қытай тауарлары» деп аталған). */
    CHINA(L10nR.string.home_tab_china_short),
}

/** Лента торының жиек соқпасы мен бағандар арасы. */
private val GridSidePadding = AgroSpacing.screen
private val GridGap = AgroSpacing.md

/**
 * HomeFeedPage — басты экран.
 *
 * Құрылымы iOS нұсқасымен бірдей: жоғарыда QR / хабарлама / профиль
 * иконкалары, астында ірі «Agroland» тақырыбы мен толық енді іздеу өрісі,
 * содан кейін үш сегментті ауыстырғыш (оң жағында дөңгелек фильтр батырмасы),
 * ал лента — 2 бағанды тор (сурет үстінде, астында мәтін).
 */
@Composable
fun HomeFeedPage(
    onOpenDetail: (Long) -> Unit,
    onOpenAll: (query: String) -> Unit,
    /** Сүзгі бетін ағымдағы сүзгімен ашады (қайта ашқанда таңдау сақталады). */
    onOpenFilter: (AnnouncementFilter?) -> Unit,
    /** Сүзгі бетінен қайтқан нәтиже — лентаға орнында қолданылады. */
    appliedFilter: AnnouncementFilter? = null,
    /** «Тазарту» — сақталған сүзгі нәтижесін де өшіру керек. */
    onClearFilter: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onCreateAnnouncement: () -> Unit = {},
    onOpenAdvertise: () -> Unit = {},
    onOpenChinaCatalog: () -> Unit = {},
    onOpenPromoted: () -> Unit = {},
    /** Аватар → профил беті. */
    onOpenProfile: () -> Unit = {},
    /** Пайдаланушы аватарының URL-ы (бос болса — иконка). */
    avatarUrl: String? = null,
    /** Фаза 18: QR сканер. */
    onOpenQrScanner: () -> Unit = {},
    /** «Себетке» — себетке салуға рұқсат етілген жарнамаларда көрінеді. */
    onAddToCart: (com.agroland.feature.marketplace.data.Announcement) -> Unit = {},
    /** announcementId → себеттегі саны; бар болса карточкада «+ / −» шығады. */
    cartQuantities: Map<Long, Double> = emptyMap(),
    /** «+ / −» — (жарнама, өзгеріс). */
    onChangeCartQuantity: (com.agroland.feature.marketplace.data.Announcement, Double) -> Unit = { _, _ -> },
    /** CHINA қойындысының мазмұны — ChinaCatalogContent (feature:china). */
    chinaContent: @Composable () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val suggestions by viewModel.suggestions.collectAsState()
    val suggestionsLoading by viewModel.suggestionsLoading.collectAsState()
    val activeFilter by viewModel.filter.collectAsState()

    // Сүзгі бетінің нәтижесі лентаға орнында қолданылады — қолданушы басты
    // беттен шықпайды (бұрын бөлек лента экранына ауысатын).
    LaunchedEffect(appliedFilter) { viewModel.applyFilter(appliedFilter) }

    Column(modifier = Modifier.fillMaxSize()) {
        HomeHeader(
            query = query,
            onQueryChange = {
                query = it
                viewModel.onSearchQueryChanged(it)
            },
            onSearch = { onOpenAll(query.trim()) },
            onOpenQrScanner = onOpenQrScanner,
            onOpenNotifications = onOpenNotifications,
            onOpenProfile = onOpenProfile,
            avatarUrl = avatarUrl,
        )

        SegmentedTabs(
            selected = tab,
            onSelect = { tab = it },
            onOpenFilter = { onOpenFilter(activeFilter) },
            filterActive = activeFilter != null,
        )

        activeFilter?.let { current ->
            ActiveFilterBar(
                count = current.activeCount(),
                onClear = {
                    onClearFilter()
                    viewModel.applyFilter(null)
                },
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (HomeFeedTab.entries.getOrNull(tab) ?: HomeFeedTab.ANNOUNCEMENTS) {
                HomeFeedTab.ANNOUNCEMENTS -> RecommendedFeedTab(
                    viewModel = viewModel,
                    onOpenDetail = onOpenDetail,
                    onAddToCart = onAddToCart,
                    cartQuantities = cartQuantities,
                    onChangeCartQuantity = onChangeCartQuantity,
                    onCreateAnnouncement = onCreateAnnouncement,
                    onOpenAdvertise = onOpenAdvertise,
                    onOpenChinaCatalog = onOpenChinaCatalog,
                    onOpenPromoted = onOpenPromoted,
                )
                // «Agro Market» — дилерлік жарнамалар лентасы (type_ad=dealer).
                HomeFeedTab.AGRO -> DealerFeedTab(
                    onOpenDetail = onOpenDetail,
                    onAddToCart = onAddToCart,
                    cartQuantities = cartQuantities,
                    onChangeCartQuantity = onChangeCartQuantity,
                )
                // MercuryX каталогы (feature:china, Фаза 17).
                HomeFeedTab.CHINA -> chinaContent()
            }

            // Ұсыныстар — іздеу жолағының астындағы қалқыма.
            if (query.isNotBlank() && (suggestions.isNotEmpty() || suggestionsLoading)) {
                SuggestionsDropdown(
                    suggestions = suggestions,
                    loading = suggestionsLoading,
                    onPick = { suggestion ->
                        query = suggestion.title
                        viewModel.clearSuggestions()
                        onOpenAll(suggestion.title)
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}

/** Жоғарғы аймақ: иконкалар жолы + «Agroland» тақырыбы + іздеу өрісі. */
@Composable
private fun HomeHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    avatarUrl: String?,
) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = AgroSpacing.screen),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AgroIconButton(
                icon = Icons.Rounded.QrCodeScanner,
                contentDescription = stringResource(L10nR.string.scan_qr),
                onClick = onOpenQrScanner,
                tint = MaterialTheme.colorScheme.primary,
                size = 24.dp,
            )
            Spacer(Modifier.width(AgroSpacing.xs))
            AgroIconButton(
                icon = Icons.Rounded.Notifications,
                contentDescription = stringResource(L10nR.string.notifications),
                onClick = onOpenNotifications,
                tint = MaterialTheme.colorScheme.primary,
                size = 24.dp,
            )
            Spacer(Modifier.width(AgroSpacing.xs))
            ProfileAvatarButton(avatarUrl = avatarUrl, onClick = onOpenProfile)
        }
        Text(
            text = stringResource(L10nR.string.app_name),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.ExtraBold,
            ),
            color = ext.primaryText,
            maxLines = 1,
        )
        Spacer(Modifier.height(AgroSpacing.md))
        AgroSearchField(
            value = query,
            onValueChange = onQueryChange,
            hint = stringResource(L10nR.string.search_hint),
            onSearch = onSearch,
        )
        Spacer(Modifier.height(AgroSpacing.md))
    }
}

/**
 * iOS сегментті ауыстырғышы: сұр контейнер, таңдалған сегмент — ақ
 * «таблетка» көлеңкесімен; оң жағында бөлек дөңгелек фильтр батырмасы.
 */
@Composable
private fun SegmentedTabs(
    selected: Int,
    onSelect: (Int) -> Unit,
    onOpenFilter: () -> Unit,
    filterActive: Boolean,
) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.md),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(AgroSize.tabBar)
                .clip(AgroRadius.field)
                .background(ext.grey)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeFeedTab.entries.forEachIndexed { index, homeTab ->
                // iOS-тегідей: екі таңдалмаған сегменттің арасында ғана
                // жіңішке тік сызық болады.
                if (index > 0) {
                    val showDivider = selected != index && selected != index - 1
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(18.dp)
                            .background(if (showDivider) ext.divider else Color.Transparent),
                    )
                }
                TabSegment(
                    label = stringResource(homeTab.labelRes),
                    selected = selected == index,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        FilterButton(onClick = onOpenFilter, active = filterActive)
    }
}

/** Ауыстырғыштың бір сегменті. */
@Composable
private fun TabSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ext = extendedColors()
    Box(
        modifier = modifier
            .fillMaxHeight()
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(10.dp),
                        ambientColor = Color(0x1A121212),
                        spotColor = Color(0x1A121212),
                    )
                } else {
                    Modifier
                },
            )
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) ext.card else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) ext.primaryText else ext.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Ауыстырғыштың оң жағындағы сүзгі батырмасы — iOS-тағыдай жұқа шеңбер
 * ішіндегі кішкентай «кемитін сызықтар» иконкасы. Сүзгі қолданылғанда
 * шеңбер толтырылады — лента неге сүзілгені бірден көрінеді.
 */
@Composable
private fun FilterButton(onClick: () -> Unit, active: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (active) primary else Color.Transparent)
                .border(1.6.dp, primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Sort,
                contentDescription = stringResource(L10nR.string.home_filter),
                tint = if (active) extendedColors().white else primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Сүзгі қолданылғанда тақталардың астында шығатын жолақ: сан + «Тазарту». */
@Composable
private fun ActiveFilterBar(count: Int, onClear: () -> Unit) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screen, vertical = AgroSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(L10nR.string.filter_active_count, count),
            style = MaterialTheme.typography.labelLarge,
            color = ext.secondaryText,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(L10nR.string.filter_reset),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClear)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** Ұсынылатын лента — 2 бағанды тор; баннерлер мен сторилер онымен бірге скроллданады. */
@Composable
private fun RecommendedFeedTab(
    viewModel: HomeViewModel,
    onOpenDetail: (Long) -> Unit,
    onAddToCart: (com.agroland.feature.marketplace.data.Announcement) -> Unit,
    cartQuantities: Map<Long, Double>,
    onChangeCartQuantity: (com.agroland.feature.marketplace.data.Announcement, Double) -> Unit,
    onCreateAnnouncement: () -> Unit,
    onOpenAdvertise: () -> Unit,
    onOpenChinaCatalog: () -> Unit,
    onOpenPromoted: () -> Unit,
) {
    val ext = extendedColors()
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val loadingMore by viewModel.loadingMore.collectAsState()
    val exhausted by viewModel.exhausted.collectAsState()
    val error by viewModel.error.collectAsState()

    val gridState = rememberLazyGridState()
    PaginateGridEffect(gridState, items.size, exhausted, loadingMore, viewModel::loadMore)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = GridSidePadding,
            end = GridSidePadding,
            top = AgroSpacing.md,
            bottom = shellBottomPadding(extra = AgroSpacing.md),
        ),
        horizontalArrangement = Arrangement.spacedBy(GridGap),
        verticalArrangement = Arrangement.spacedBy(GridGap),
    ) {
        fullWidth(key = "banners") {
            MainBannerCarousel(
                onOpenAnnouncement = onOpenDetail,
                onOpenCreate = onCreateAnnouncement,
                onOpenAdvertise = onOpenAdvertise,
                onOpenChinaCatalog = onOpenChinaCatalog,
                onOpenPromoted = onOpenPromoted,
            )
        }
        fullWidth(key = "stories") { StoriesRow() }
        fullWidth(key = "section") {
            Text(
                text = stringResource(L10nR.string.home_recommended_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = ext.primaryText,
            )
        }

        when {
            loading -> items(6) { ShimmerGridCard() }
            error != null -> fullWidth(key = "error") {
                ErrorWithRetry(
                    onRetry = viewModel::refresh,
                    message = error!!.displayText(),
                    modifier = Modifier.padding(top = AgroSpacing.screen),
                )
            }
            items.isEmpty() -> fullWidth(key = "empty") {
                EmptyView(
                    title = stringResource(L10nR.string.feed_empty_title),
                    message = stringResource(L10nR.string.feed_empty_message),
                    modifier = Modifier.padding(top = AgroSpacing.screen),
                )
            }
            else -> {
                items(items, key = { it.id }) { item ->
                    AnnouncementGridCard(
                        item = item,
                        onClick = { onOpenDetail(item.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                        onAddToCart = if (item.allowCart) {
                            { onAddToCart(item) }
                        } else {
                            null
                        },
                        cartQuantity = cartQuantities[item.id],
                        onChangeCartQuantity = { delta -> onChangeCartQuantity(item, delta) },
                    )
                }
                if (loadingMore) {
                    fullWidth(key = "loading-more") {
                        LoadingWidget(Modifier.fillMaxWidth().padding(AgroSpacing.screen))
                    }
                }
            }
        }
    }
}

/**
 * «Agro Market» қойындысы — type_ad=dealer лентасы. FeedViewModel-нің
 * жеке экземпляры (key) — ұсынылатын лентамен күй араласпайды.
 */
@Composable
private fun DealerFeedTab(
    onOpenDetail: (Long) -> Unit,
    onAddToCart: (com.agroland.feature.marketplace.data.Announcement) -> Unit,
    cartQuantities: Map<Long, Double>,
    onChangeCartQuantity: (com.agroland.feature.marketplace.data.Announcement, Double) -> Unit,
) {
    val dealerViewModel: FeedViewModel = hiltViewModel(key = "home-agro-feed")
    LaunchedEffect(Unit) {
        dealerViewModel.initialize(AnnouncementFilter(typeAd = "dealer"))
    }
    val items by dealerViewModel.items.collectAsState()
    val loading by dealerViewModel.loading.collectAsState()
    val loadingMore by dealerViewModel.loadingMore.collectAsState()
    val exhausted by dealerViewModel.exhausted.collectAsState()
    val error by dealerViewModel.error.collectAsState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    // displayText — composable емес нұсқа: жолдар алдын ала дайындалады.
    val networkMessage = stringResource(L10nR.string.error_no_internet)
    val genericMessage = stringResource(L10nR.string.error_generic_message)

    LaunchedEffect(Unit) {
        dealerViewModel.events.collect { event ->
            val showError = event as? MarketplaceEvent.ShowError ?: return@collect
            snackbarHostState.showSnackbar(showError.error.displayText(networkMessage, genericMessage))
        }
    }

    val gridState = rememberLazyGridState()
    PaginateGridEffect(gridState, items.size, exhausted, loadingMore, dealerViewModel::loadMore)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = GridSidePadding,
                end = GridSidePadding,
                top = AgroSpacing.md,
                bottom = shellBottomPadding(extra = AgroSpacing.md),
            ),
            horizontalArrangement = Arrangement.spacedBy(GridGap),
            verticalArrangement = Arrangement.spacedBy(GridGap),
        ) {
            when {
                loading -> items(6) { ShimmerGridCard() }
                error != null -> fullWidth(key = "error") {
                    ErrorWithRetry(
                        onRetry = dealerViewModel::refresh,
                        message = error!!.displayText(),
                    )
                }
                items.isEmpty() -> fullWidth(key = "empty") {
                    EmptyView(
                        title = stringResource(L10nR.string.feed_empty_title),
                        message = stringResource(L10nR.string.feed_empty_message),
                    )
                }
                else -> {
                    items(items, key = { it.id }) { item ->
                        AnnouncementGridCard(
                            item = item,
                            onClick = { onOpenDetail(item.id) },
                            onToggleFavorite = { dealerViewModel.toggleFavorite(item.id) },
                            onAddToCart = if (item.allowCart) {
                                { onAddToCart(item) }
                            } else {
                                null
                            },
                            cartQuantity = cartQuantities[item.id],
                            onChangeCartQuantity = { delta -> onChangeCartQuantity(item, delta) },
                        )
                    }
                    if (loadingMore) {
                        fullWidth(key = "loading-more") {
                            LoadingWidget(Modifier.fillMaxWidth().padding(AgroSpacing.screen))
                        }
                    }
                }
            }
        }
        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Тордың екі бағанын да алатын элемент (тақырып, баннер, күй экраны). */
private fun LazyGridScope.fullWidth(
    key: String,
    content: @Composable () -> Unit,
) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

/**
 * Аватар түймесі — сурет бар болса дөңгелек фото, жоқ болса жасыл
 * дөңгелектегі ақ иконка (iOS-тағыдай).
 */
@Composable
private fun ProfileAvatarButton(avatarUrl: String?, onClick: () -> Unit) {
    val ext = extendedColors()
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            CachedImage(
                url = avatarUrl,
                contentDescription = stringResource(L10nR.string.profile_title),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = stringResource(L10nR.string.profile_title),
                tint = ext.white,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Ұсыныстар тізімі — іздеу жолағының астында қалқыма карточка. */
@Composable
private fun SuggestionsDropdown(
    suggestions: List<Suggestion>,
    loading: Boolean,
    onPick: (Suggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ext = extendedColors()
    Column(
        modifier = modifier
            .padding(horizontal = AgroSpacing.screen, vertical = AgroSpacing.sm)
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = AgroRadius.card,
                ambientColor = Color(0x1F000000),
                spotColor = Color(0x1F000000),
            )
            .clip(AgroRadius.card)
            .background(ext.card),
    ) {
        if (loading && suggestions.isEmpty()) {
            LoadingWidget(Modifier.fillMaxWidth().padding(AgroSpacing.md))
        }
        suggestions.forEachIndexed { index, suggestion ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AgroSpacing.screen)
                        .height(1.dp)
                        .background(ext.divider),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(suggestion) }
                    .padding(horizontal = AgroSpacing.screen, vertical = AgroSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AgroSpacing.sm),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = suggestion.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    listOfNotNull(suggestion.category, suggestion.location)
                        .joinToString(" • ")
                        .takeIf { it.isNotBlank() }
                        ?.let { sub ->
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ext.secondaryText,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Қате мәтіні: backend хабарламасы > локализацияланған fallback. Ешқашан error_code. */
@Composable
fun MarketplaceError.displayText(): String = displayText(
    networkMessage = stringResource(L10nR.string.error_no_internet),
    genericMessage = stringResource(L10nR.string.error_generic_message),
)
