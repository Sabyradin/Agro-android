package com.agroland.feature.marketplace.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.components.ShimmerBox
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.Announcement
import com.agroland.feature.marketplace.data.FullAnnouncement
import com.agroland.feature.reviews.ui.AnnouncementReviewsSection
import kotlinx.coroutines.launch

/** iOS VIP чипінің алтын түсі. */
private val VipGold = Color(0xFFE9A800)

/**
 * AnnouncementDetailPage — толық жарнама, iOS макетіне сай:
 * галерея (нүкте-индикатор) → негізгі ақпарат карточкасы (күні, атауы, баға,
 * VIP/келісім чиптері, орны, қаралым/сүйікті/ID) → «Ақпарат | Сипаттама»
 * ауыстырғышы → «Жеткізу / Өзі алу» → пікірлер → сатушы → сатушының барлық
 * жарнамалары мен ұқсас жарнамалар (көлденең тізім).
 *
 * Төменгі панель: себетке салуға болатын тауарда — сатып алу панелі
 * ([purchaseBar]), қалған жарнамада — «Жазу» (чат) + «Хабарласу» (телефон
 * немесе қолданба ішіндегі қоңырау). [bottomBar] берілсе (ие беті) — сол шығады.
 */
@Composable
fun AnnouncementDetailPage(
    announcementId: Long,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    /** Сатушымен чат — otherUserId (author_id) + announcement_id арқылы. */
    onOpenChat: (otherUserId: Long, username: String?, announcementId: Long) -> Unit = { _, _, _ -> },
    /** Қолданба ішіндегі дауыстық қоңырау (null — батырма көрсетілмейді). */
    onCallInApp: ((peerId: Long, peerName: String?, peerAvatarUrl: String?) -> Unit)? = null,
    /** Галерея суреті — pinch-zoom фото көрсеткіші. */
    onOpenPhotoViewer: (images: List<String>, index: Int) -> Unit = { _, _ -> },
    /** Пікірлер бөлімінен — толық пікірлер беті. */
    onOpenReviews: (Long) -> Unit = {},
    /** Сатушы пікірлері (сатушы карточкасы). */
    onOpenSellerReviews: (userId: Long) -> Unit = {},
    /** Себет тауарының панелі («Сатып алу» + «Себетке»). */
    purchaseBar: (@Composable () -> Unit)? = null,
    /** Жарнаманы себет арқылы сатып алуға бола ма (feature:cart ережесі). */
    isPurchasable: (FullAnnouncement) -> Boolean = { false },
    /** Толық ауыстыратын панель (ие беті). */
    bottomBar: (@Composable () -> Unit)? = null,
    viewModel: AnnouncementDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val detail by viewModel.detail.collectAsState()
    val favorite by viewModel.favorite.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val favoriteAdded = stringResource(L10nR.string.favorite_added_toast)
    val favoriteRemoved = stringResource(L10nR.string.favorite_removed_toast)
    val noInternetText = stringResource(L10nR.string.error_no_internet)
    val genericErrorText = stringResource(L10nR.string.error_generic_message)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MarketplaceEvent.ShowError ->
                    snackbar.showSnackbar(event.error.displayText(noInternetText, genericErrorText))
                MarketplaceEvent.FavoriteFailed -> Unit
            }
        }
    }
    LaunchedEffect(announcementId) {
        viewModel.load(announcementId)
    }
    // Сүйікті өзгерісі — toast тек пайдаланушы жүректі басқанда. (Бұрын жүктеу
    // кезіндегі false → true ауысуы да «Таңдаулыларға қосылды» деп көрсетілетін.)
    var favoriteTapped by remember { mutableStateOf(false) }
    LaunchedEffect(favorite) {
        if (favoriteTapped) {
            favoriteTapped = false
            snackbar.showSnackbar(if (favorite) favoriteAdded else favoriteRemoved)
        }
    }

    var contactSheetOpen by remember { mutableStateOf(false) }

    AgroScaffold(
        // Галерея статус-бардың астына дейін созылады — жоғарғы inset контентке берілмейді.
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        bottomBar = {
            val current = detail
            when {
                bottomBar != null -> bottomBar()
                current == null -> Unit
                purchaseBar != null && isPurchasable(current) -> purchaseBar()
                else -> {
                    val authorId = current.base.authorId?.takeIf { it > 0 }
                    if (authorId != null) {
                        ContactBottomBar(
                            onWrite = {
                                onOpenChat(authorId, current.seller?.name ?: current.base.title, announcementId)
                            },
                            onContact = { contactSheetOpen = true },
                        )
                    }
                }
            }
        },
    ) { inner ->
        Box(modifier = inner.fillMaxSize()) {
            when {
                loading && detail == null -> DetailSkeleton()
                error != null && detail == null -> CenteredContent(Modifier.statusBarsPadding()) {
                    ErrorWithRetry(
                        onRetry = { viewModel.load(announcementId) },
                        message = error!!.displayText(),
                    )
                }
                detail != null -> DetailContent(
                    detail = detail!!,
                    favoritesCount = detail!!.base.favoritesCount,
                    onOpenDetail = onOpenDetail,
                    announcementId = announcementId,
                    onOpenPhotoViewer = onOpenPhotoViewer,
                    onOpenReviews = onOpenReviews,
                    onOpenSellerReviews = onOpenSellerReviews,
                )
                else -> LoadingWidget(Modifier.fillMaxSize())
            }
            // Артқа / бөлісу / сүйікті — суреттің үстінде қалқып тұрады (iOS).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FloatingCircleButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    onClick = onBack,
                )
                Spacer(Modifier.weight(1f))
                val item = detail
                if (item != null) {
                    FloatingCircleButton(
                        icon = Icons.Rounded.Share,
                        contentDescription = stringResource(L10nR.string.common_share),
                        onClick = {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "${item.base.title} — ${displayPrice(item.base)}\n" +
                                        "https://agroland.kz/announcement/${item.base.id}",
                                )
                            }
                            context.startActivity(Intent.createChooser(share, null))
                        },
                    )
                    FloatingCircleButton(
                        icon = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = stringResource(L10nR.string.favorites_title),
                        onClick = {
                            favoriteTapped = true
                            viewModel.toggleFavorite()
                        },
                        tint = if (favorite) FavoriteRed else Color.White,
                    )
                }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    val current = detail
    if (contactSheetOpen && current != null) {
        ContactSheet(
            detail = current,
            onDismiss = { contactSheetOpen = false },
            onCallInApp = onCallInApp?.let { call ->
                current.base.authorId?.takeIf { it > 0 }?.let { peerId ->
                    { call(peerId, current.seller?.name, current.seller?.avatarUrl) }
                }
            },
        )
    }
}

/** Сүйікті жүректің түсі — жартылай мөлдір қара фонда анық көрінеді. */
private val FavoriteRed = Color(0xFFFF3B30)

/** Сурет үстіндегі дөңгелек батырма: жартылай мөлдір қара фон + ақ иконка. */
@Composable
private fun FloatingCircleButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color = Color.White,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.38f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Бағаның көрсетілетін мәтіні: KZT → ₸ (iOS-тағыдай). */
private fun displayPrice(base: Announcement): String {
    return PriceFormatter.format(base.price, displayCurrency(base.currency))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    detail: FullAnnouncement,
    favoritesCount: Int,
    onOpenDetail: (Long) -> Unit,
    announcementId: Long,
    onOpenPhotoViewer: (images: List<String>, index: Int) -> Unit,
    onOpenReviews: (Long) -> Unit,
    onOpenSellerReviews: (userId: Long) -> Unit,
) {
    val base = detail.base
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val additionalIndex = 7

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 0: Галерея + нүкте-индикатор.
        item(key = "gallery") {
            Gallery(
                images = base.imageUrls.ifEmpty { listOfNotNull(base.imageUrl) },
                title = base.title,
                onOpenPhotoViewer = onOpenPhotoViewer,
            )
        }

        // 1: Негізгі ақпарат.
        item(key = "info") { MainInfoCard(detail = detail, favoritesCount = favoritesCount) }

        // 2: Ақпарат | Сипаттама.
        item(key = "tabs") { InfoTabsCard(detail = detail) }

        // 3: Жеткізу / Өзі алу.
        item(key = "delivery") { DeliveryCard(base = base) }

        // 4: Пікірлер.
        item(key = "reviews") {
            AnnouncementReviewsSection(
                announcementId = announcementId,
                // Жарнаманың өз рейтингі — сатушының жалпы рейтингі емес.
                rating = base.rating ?: detail.seller?.rating ?: 0.0,
                reviewsCount = base.reviewsCount,
                onOpenReviews = { onOpenReviews(announcementId) },
            )
        }

        // 5: Сатушы.
        item(key = "seller") {
            SellerCard(
                detail = detail,
                onOpenSeller = { base.authorId?.takeIf { it > 0 }?.let(onOpenSellerReviews) },
                onShowAllAds = if (detail.additional.isNotEmpty()) {
                    { scope.launch { listState.animateScrollToItem(additionalIndex) } }
                } else {
                    null
                },
            )
        }

        // 6: бос орын (индекс тұрақты болуы үшін).
        item(key = "spacer") { Spacer(Modifier.height(4.dp)) }

        // 7: Сатушының барлық жарнамалары.
        item(key = "additional") {
            if (detail.additional.isNotEmpty()) {
                AnnouncementCarousel(
                    title = stringResource(L10nR.string.detail_seller_all_ads),
                    items = detail.additional,
                    onOpenDetail = onOpenDetail,
                )
            }
        }

        // 8: Ұқсас жарнамалар.
        item(key = "similar") {
            if (detail.similar.isNotEmpty()) {
                AnnouncementCarousel(
                    title = stringResource(L10nR.string.detail_similar),
                    items = detail.similar,
                    onOpenDetail = onOpenDetail,
                )
            }
        }
    }
}

/** Галерея биіктігі (статус-бар inset-інсіз). */
private val GalleryHeight = 340.dp

/** Галерея: толық енді pager + астында «таблеткадағы» нүктелер (iOS). */
@Composable
private fun Gallery(
    images: List<String>,
    title: String,
    onOpenPhotoViewer: (images: List<String>, index: Int) -> Unit,
) {
    val ext = extendedColors()
    // Статус-бар биіктігі қосылады — сурет экранның ең жоғарғы шетінен басталады.
    val galleryHeight = GalleryHeight + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    if (images.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(galleryHeight)
                .background(ext.grey),
            contentAlignment = Alignment.Center,
        ) {
            CachedImage(url = null, contentDescription = title, modifier = Modifier.size(64.dp))
        }
        return
    }
    val pagerState = rememberPagerState(pageCount = { images.size })
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(galleryHeight)
                    .background(ext.card),
            ) { page ->
                CachedImage(
                    url = images[page],
                    contentDescription = title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onOpenPhotoViewer(images, page) },
                )
            }
            // Жоғарғы күңгірт градиент — статус-бар мен батырмалар ашық суретте де оқылады.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent))),
            )
        }
        if (images.size > 1) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ext.grey)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Көп сурет болса нүктелер 7-ден аспайды (iOS page control).
                val count = images.size.coerceAtMost(7)
                val selected = pagerState.currentPage.coerceAtMost(count - 1)
                repeat(count) { index ->
                    val active = index == selected
                    val width by animateDpAsState(if (active) 16.dp else 7.dp, label = "dot")
                    val color by animateColorAsState(
                        if (active) MaterialTheme.colorScheme.primary else ext.secondaryText.copy(alpha = 0.4f),
                        label = "dotColor",
                    )
                    Box(
                        modifier = Modifier
                            .height(7.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
            }
        }
    }
}

/** Ақ карточка — iOS бөлімдерінің ортақ қаңқасы. */
@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(extendedColors().card)
            .padding(16.dp),
        content = content,
    )
}

/** Күні, атауы, баға, чиптер, орны, статистика. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainInfoCard(detail: FullAnnouncement, favoritesCount: Int) {
    val ext = extendedColors()
    val base = detail.base
    DetailCard {
        DateFormatter.formatDate(base.createdAt).takeIf { it.isNotBlank() }?.let { date ->
            Text(
                text = stringResource(L10nR.string.detail_published_at, date),
                fontSize = 13.sp,
                color = ext.secondaryText,
            )
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = base.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ext.primaryText,
            lineHeight = 26.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            val price = displayPrice(base)
            Text(
                text = price.ifBlank { stringResource(L10nR.string.mp_negotiable_short) },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (price.isNotBlank()) {
                base.measurementUnit?.takeIf { it.isNotBlank() }?.let { unit ->
                    Text(
                        text = " / ${displayUnit(unit)}",
                        fontSize = 17.sp,
                        color = ext.secondaryText,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
            }
        }

        val showChips = base.isVip || base.negotiable
        if (showChips) {
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (base.isVip) {
                    InfoChip(text = "VIP", color = VipGold, icon = Icons.Rounded.WorkspacePremium)
                }
                if (base.negotiable) {
                    InfoChip(
                        text = stringResource(L10nR.string.detail_negotiable_chip),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        base.placeLabel.takeIf { it.isNotBlank() }?.let { place ->
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Place,
                    contentDescription = null,
                    tint = ext.secondaryText,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(text = place, fontSize = 15.sp, color = ext.secondaryText)
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = ext.divider)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatItem(icon = Icons.Rounded.Visibility, value = base.viewsCount.toString())
            Spacer(Modifier.width(18.dp))
            StatItem(icon = Icons.Rounded.FavoriteBorder, value = favoritesCount.toString())
            Spacer(Modifier.weight(1f))
            Text(
                text = "ID ${"%,d".format(base.id).replace(',', ' ')}",
                fontSize = 13.sp,
                color = ext.secondaryText.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun InfoChip(text: String, color: Color, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun StatItem(icon: ImageVector, value: String) {
    val ext = extendedColors()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = ext.secondaryText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(value, fontSize = 14.sp, color = ext.secondaryText)
    }
}

/** «Ақпарат | Сипаттама» — iOS сегментті ауыстырғышы бар карточка. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoTabsCard(detail: FullAnnouncement) {
    val ext = extendedColors()
    var tab by remember { mutableIntStateOf(0) }
    DetailCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ext.grey)
                .padding(3.dp),
        ) {
            listOf(L10nR.string.detail_tab_info, L10nR.string.detail_tab_specs).forEachIndexed { index, res ->
                val selected = tab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) ext.card else Color.Transparent)
                        .clickable { tab = index },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(res),
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = ext.primaryText,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (tab == 0) {
            Text(
                text = detail.description?.takeIf { it.isNotBlank() }
                    ?: stringResource(L10nR.string.detail_no_description),
                fontSize = 16.sp,
                lineHeight = 23.sp,
                color = ext.primaryText,
            )
            val tags = detail.keywords.map { it.trim().trimStart('#') }.filter { it.isNotBlank() }.distinct()
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tags.take(12).forEach { tag ->
                        Text(
                            text = "#$tag",
                            fontSize = 13.sp,
                            color = ext.secondaryText,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(ext.grey)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        } else {
            val yes = stringResource(L10nR.string.common_yes)
            val no = stringResource(L10nR.string.common_no)
            val base = detail.base
            SpecRow(stringResource(L10nR.string.detail_delivery), if (base.deliveryAvailable) yes else no)
            SpecRow(stringResource(L10nR.string.detail_pickup), if (base.pickupAvailable) yes else no)
            detail.sku?.takeIf { it.isNotBlank() }?.let { SpecRow(stringResource(L10nR.string.detail_sku), it) }
            detail.stockQuantity?.let { SpecRow(stringResource(L10nR.string.detail_stock), it.toString()) }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.sp, color = ext.secondaryText, modifier = Modifier.weight(1f))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ext.primaryText)
    }
}

/** «Жеткізу / Өзі алу» — қолжетімді тәсілдер жасыл иконкамен. */
@Composable
private fun DeliveryCard(base: Announcement) {
    val ext = extendedColors()
    val primary = MaterialTheme.colorScheme.primary
    DetailCard {
        Text(
            text = stringResource(L10nR.string.detail_delivery_pickup),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ext.primaryText,
        )
        Spacer(Modifier.height(10.dp))
        if (!base.deliveryAvailable && !base.pickupAvailable) {
            Text(stringResource(L10nR.string.detail_no_delivery_info), fontSize = 15.sp, color = ext.secondaryText)
        }
        if (base.deliveryAvailable) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(L10nR.string.detail_delivery), fontSize = 17.sp, color = primary)
            }
        }
        if (base.pickupAvailable) {
            if (base.deliveryAvailable) Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Storefront, contentDescription = null, tint = primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(stringResource(L10nR.string.detail_pickup), fontSize = 17.sp, color = primary)
                    base.pickupAddress?.takeIf { it.isNotBlank() }?.let {
                        Text(it, fontSize = 13.sp, color = ext.secondaryText)
                    }
                }
            }
        }
    }
}

/** Сатушы: аватар/инициал, аты, мүшелік күні, рейтинг + «барлық жарнамалары». */
@Composable
private fun SellerCard(
    detail: FullAnnouncement,
    onOpenSeller: () -> Unit,
    onShowAllAds: (() -> Unit)?,
) {
    val ext = extendedColors()
    val primary = MaterialTheme.colorScheme.primary
    val seller = detail.seller ?: return
    val name = seller.name?.takeIf { it.isNotBlank() } ?: "—"
    DetailCard {
        Text(
            text = stringResource(L10nR.string.detail_seller),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ext.primaryText,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenSeller),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                if (!seller.avatarUrl.isNullOrBlank()) {
                    CachedImage(
                        url = seller.avatarUrl,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = name.first().uppercase(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primary,
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ext.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (seller.isVipSeller) {
                        Spacer(Modifier.width(6.dp))
                        InfoChip(text = "VIP", color = VipGold)
                    }
                }
                seller.memberSince?.let { DateFormatter.formatDate(it) }?.takeIf { it.isNotBlank() }?.let { since ->
                    Text(
                        text = stringResource(L10nR.string.detail_member_since_short, since),
                        fontSize = 13.sp,
                        color = primary.copy(alpha = 0.6f),
                    )
                }
                val rating = seller.rating?.takeIf { it > 0.0 } ?: detail.base.rating
                if (rating != null && rating > 0.0) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val full = rating.coerceIn(0.0, 5.0).let { kotlin.math.round(it).toInt() }
                        repeat(5) { i ->
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = if (i < full) VipGold else ext.grey,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", rating),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primary,
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = ext.secondaryText,
            )
        }
        if (onShowAllAds != null) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(primary.copy(alpha = 0.16f))
                    .clickable(onClick = onShowAllAds)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.GridView, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(L10nR.string.detail_seller_all_ads),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primary,
                )
            }
        }
    }
}

/** Көлденең жарнама тізімі (iOS-тағы «Пайдаланушының барлық жарнамалары»). */
@Composable
private fun AnnouncementCarousel(
    title: String,
    items: List<Announcement>,
    onOpenDetail: (Long) -> Unit,
) {
    val ext = extendedColors()
    Column {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = ext.primaryText,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { item ->
                AnnouncementGridCard(
                    item = item,
                    onClick = { onOpenDetail(item.id) },
                    modifier = Modifier.width(170.dp),
                )
            }
        }
    }
}

/** Төменгі панель: сол жақта «Жазу» (чат), оң жақта «Хабарласу». */
@Composable
private fun ContactBottomBar(onWrite: () -> Unit, onContact: () -> Unit) {
    val ext = extendedColors()
    val primary = MaterialTheme.colorScheme.primary
    Surface(color = ext.card, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BarButton(
                text = stringResource(L10nR.string.detail_write),
                icon = Icons.Rounded.ChatBubbleOutline,
                container = primary,
                content = Color.White,
                onClick = onWrite,
                modifier = Modifier.weight(1f),
            )
            BarButton(
                text = stringResource(L10nR.string.detail_contact),
                icon = Icons.Rounded.Call,
                container = primary.copy(alpha = 0.16f),
                content = primary,
                onClick = onContact,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BarButton(
    text: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = content, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * «Хабарласу» парағы: телефон нөміріне қоңырау (әр нөмір бөлек) және
 * қолданба ішіндегі дауыстық қоңырау.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactSheet(
    detail: FullAnnouncement,
    onDismiss: () -> Unit,
    onCallInApp: (() -> Unit)?,
) {
    val ext = extendedColors()
    val context = LocalContext.current
    val numbers = detail.contactNumbers
        .ifEmpty { listOfNotNull(detail.seller?.phoneNumber) }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ext.card) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(L10nR.string.detail_contact_choose),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ext.primaryText,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            if (numbers.isEmpty()) {
                ContactOption(
                    icon = Icons.Rounded.PhoneInTalk,
                    title = stringResource(L10nR.string.detail_call_phone),
                    subtitle = stringResource(L10nR.string.detail_no_phone),
                    enabled = false,
                    onClick = {},
                )
            }
            numbers.forEach { phone ->
                ContactOption(
                    icon = Icons.Rounded.PhoneInTalk,
                    title = stringResource(L10nR.string.detail_call_phone),
                    subtitle = phone,
                    onClick = {
                        onDismiss()
                        try {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                        } catch (_: Exception) {
                        }
                    },
                )
            }
            if (onCallInApp != null) {
                ContactOption(
                    icon = Icons.Rounded.Headset,
                    title = stringResource(L10nR.string.detail_call_in_app),
                    subtitle = stringResource(L10nR.string.detail_call_in_app_hint),
                    onClick = {
                        onDismiss()
                        onCallInApp()
                    },
                )
            }
        }
    }
}

@Composable
private fun ContactOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val ext = extendedColors()
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.grey.copy(alpha = 0.55f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (enabled) primary else ext.secondaryText.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = ext.primaryText)
            Text(
                subtitle,
                fontSize = 14.sp,
                color = ext.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (enabled) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = ext.secondaryText,
            )
        }
    }
}

@Composable
private fun DetailSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            height = 300,
        )
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.9f), height = 12)
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f), height = 12)
    }
}
