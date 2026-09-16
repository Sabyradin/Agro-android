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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroIconButton
import com.agroland.core.ui.components.AgroSearchField
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.components.ShimmerCard
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.AnnouncementFilter
import com.agroland.feature.marketplace.data.Suggestion
import com.agroland.feature.stories.ui.MainBannerCarousel
import com.agroland.feature.stories.ui.StoriesRow

/** Home лентасының режимдері — Flutter үш tablet-selector-ы. */
private enum class HomeFeedTab(val labelRes: Int) {
    ANNOUNCEMENTS(L10nR.string.home_tab_announcements),
    AGRO(L10nR.string.home_tab_agro),
    CHINA(L10nR.string.home_tab_china),
}

/**
 * HomeFeedPage — басты экран (Фаза 5): іздеу + ұсыныстар, фильтр/таңдаулылар/категориялар,
 * үш tablet-selector; ANNOUNCEMENTS — ұсынылатын лента (announcements/recommended).
 */
@Composable
fun HomeFeedPage(
    onOpenDetail: (Long) -> Unit,
    onOpenAll: (query: String) -> Unit,
    onOpenFilter: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    onCreateAnnouncement: () -> Unit = {},
    onOpenAdvertise: () -> Unit = {},
    onOpenChinaCatalog: () -> Unit = {},
    onOpenPromoted: () -> Unit = {},
    /** Аватар → профил беті (Flutter main_page app bar avatar). */
    onOpenProfile: () -> Unit = {},
    /** Фаза 18: QR сканер (Flutter main_page іздеу жолағындағы qr-иконка). */
    onOpenQrScanner: () -> Unit = {},
    /** CHINA қойындысының мазмұны — ChinaCatalogContent (feature:china). */
    chinaContent: @Composable () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val ext = extendedColors()
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val loadingMore by viewModel.loadingMore.collectAsState()
    val exhausted by viewModel.exhausted.collectAsState()
    val error by viewModel.error.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val suggestionsLoading by viewModel.suggestionsLoading.collectAsState()

    val listState = rememberLazyListState()
    PaginateEffect(listState, items.size, exhausted, loadingMore, viewModel::loadMore)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Жоғарғы жолақ: аватар (профиль) + іздеу + фильтр + таңдаулылар.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ProfileAvatarButton(onClick = onOpenProfile)
            AgroSearchField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.onSearchQueryChanged(it)
                },
                hint = stringResource(L10nR.string.home_search_hint),
                modifier = Modifier.weight(1f),
            )
            AgroIconButton(
                icon = Icons.Outlined.QrCodeScanner,
                contentDescription = stringResource(L10nR.string.scan_qr),
                onClick = onOpenQrScanner,
            )
            AgroIconButton(
                icon = Icons.Outlined.Notifications,
                contentDescription = stringResource(L10nR.string.notifications),
                onClick = onOpenNotifications,
            )
            AgroIconButton(
                icon = Icons.Outlined.FilterAlt,
                contentDescription = stringResource(L10nR.string.home_filter),
                onClick = onOpenFilter,
            )
            AgroIconButton(
                icon = Icons.Outlined.FavoriteBorder,
                contentDescription = stringResource(L10nR.string.favorites_title),
                onClick = onOpenFavorites,
            )
        }

        // Ұсыныстар ашылмасы — іздеу жолағының астында.
        if (query.isNotBlank() && (suggestions.isNotEmpty() || suggestionsLoading)) {
            SuggestionsDropdown(
                suggestions = suggestions,
                loading = suggestionsLoading,
                onPick = { suggestion ->
                    query = suggestion.title
                    viewModel.clearSuggestions()
                    onOpenAll(suggestion.title)
                },
            )
        }

        // Үш tablet-selector.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HomeFeedTab.entries.forEachIndexed { index, homeTab ->
                val selected = tab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary else ext.grey)
                        .clickable { tab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(homeTab.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) ext.white else ext.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        when (HomeFeedTab.entries.getOrNull(tab) ?: HomeFeedTab.ANNOUNCEMENTS) {
            HomeFeedTab.ANNOUNCEMENTS -> {
                // Stories: баннер-карусель (96×96) + admin сторилер жолы (80×80).
                MainBannerCarousel(
                    onOpenAnnouncement = onOpenDetail,
                    onOpenCreate = onCreateAnnouncement,
                    onOpenAdvertise = onOpenAdvertise,
                    onOpenChinaCatalog = onOpenChinaCatalog,
                    onOpenPromoted = onOpenPromoted,
                )
                StoriesRow()

                // Категориялар + барлық жарнамалар жолы.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ShortcutTile(
                        icon = Icons.Outlined.GridView,
                        text = stringResource(L10nR.string.categories_title),
                        onClick = onOpenCategories,
                        modifier = Modifier.weight(1f),
                    )
                    ShortcutTile(
                        icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        text = stringResource(L10nR.string.home_see_all),
                        onClick = { onOpenAll(query.trim()) },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Ұсынылатын лента.
                when {
                    loading -> FeedSkeleton()
                    error != null -> CenteredContent {
                        ErrorWithRetry(
                            onRetry = viewModel::refresh,
                            message = error!!.displayText(),
                        )
                    }
                    items.isEmpty() -> EmptyView(
                        title = stringResource(L10nR.string.feed_empty_title),
                        message = stringResource(L10nR.string.feed_empty_message),
                    )
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
            }
            // «Agro market» — дилерлік жарнамалар лентасы (type_ad=dealer,
            // Flutter _DealerAnnouncementsTab).
            HomeFeedTab.AGRO -> DealerFeedTab(onOpenDetail = onOpenDetail)
            // «Қытай тауарлары» — ChinaCatalogContent (feature:china, Фаза 17).
            HomeFeedTab.CHINA -> chinaContent()
        }
    }
}

/**
 * Аватар түймесі — дөңгелек фондегі профиль иконкасы → профил беті.
 */
@Composable
private fun ProfileAvatarButton(onClick: () -> Unit) {
    val ext = extendedColors()
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(38.dp)
            .clip(CircleShape)
            .background(ext.primaryLight)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = stringResource(L10nR.string.profile_title),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * «Agro market» қойындысы — type_ad=dealer лентасы. FeedViewModel-нің
 * жеке экземпляры (key) — ұсынылатын лентамен күй араласпайды.
 */
@Composable
private fun DealerFeedTab(onOpenDetail: (Long) -> Unit) {
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

    val listState = rememberLazyListState()
    PaginateEffect(listState, items.size, exhausted, loadingMore, dealerViewModel::loadMore)

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            loading -> FeedSkeleton()
            error != null -> CenteredContent {
                ErrorWithRetry(
                    onRetry = dealerViewModel::refresh,
                    message = error!!.displayText(),
                )
            }
            items.isEmpty() -> EmptyView(
                title = stringResource(L10nR.string.feed_empty_title),
                message = stringResource(L10nR.string.feed_empty_message),
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    AnnouncementCard(
                        item = item,
                        onClick = { onOpenDetail(item.id) },
                        onToggleFavorite = { dealerViewModel.toggleFavorite(item.id) },
                    )
                }
                if (loadingMore) {
                    item { LoadingWidget(Modifier.fillMaxWidth().padding(16.dp)) }
                }
            }
        }
        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** Ұсыныстар тізімі — іздеу жолағының астында карточка. */
@Composable
private fun SuggestionsDropdown(
    suggestions: List<Suggestion>,
    loading: Boolean,
    onPick: (Suggestion) -> Unit,
) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ext.card),
    ) {
        if (loading && suggestions.isEmpty()) {
            LoadingWidget(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            )
        }
        suggestions.forEach { suggestion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(suggestion) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.secondaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                }
            }
        }
    }
}

/** Кішкентай жылдам өту тақтасы (категориялар / барлығын көру). */
@Composable
private fun ShortcutTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ext = extendedColors()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ext.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = ext.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FeedSkeleton() {
    Column {
        repeat(6) { ShimmerCard() }
    }
}

/** Қате мәтіні: backend хабарламасы > локализацияланған fallback. Ешқашан error_code. */
@Composable
fun MarketplaceError.displayText(): String = displayText(
    networkMessage = stringResource(L10nR.string.error_no_internet),
    genericMessage = stringResource(L10nR.string.error_generic_message),
)