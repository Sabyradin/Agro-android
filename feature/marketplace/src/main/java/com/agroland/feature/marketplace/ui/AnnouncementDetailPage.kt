package com.agroland.feature.marketplace.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroIconButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.components.ShimmerBox
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.reviews.ui.AnnouncementReviewsSection

/**
 * AnnouncementDetailPage — толық жарнама (Фаза 5): галерея, баға, сатушы картасы,
 * сипаттама/сипаттамалар қойындылары, байланыс нөмірлері, ұқсас және қосымша
 * жарнамалар; сүйікті + бөлісу. Чат — Фаза 12, қоңырау — ACTION_DIAL.
 */
@Composable
fun AnnouncementDetailPage(
    announcementId: Long,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    /** Фаза 12: сатушымен чат — otherUserId (author_id) + announcement_id арқылы. */
    onOpenChat: (otherUserId: Long, username: String?, announcementId: Long) -> Unit = { _, _, _ -> },
    /** Фаза 18: галерея суреті — pinch-zoom фото көрсеткіші. */
    onOpenPhotoViewer: (images: List<String>, index: Int) -> Unit = { _, _ -> },
    /** Фаза 18: Kaspi-стиль пікірлер бөлімінен — толық пікірлер беті. */
    onOpenReviews: (Long) -> Unit = {},
    /** Фаза 18: сатушы пікірлері (seller card). */
    onOpenSellerReviews: (userId: Long) -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    viewModel: AnnouncementDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val localeTag = LocalConfiguration.current.locales[0]?.toLanguageTag()

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
    // Сүйікті өзгерісі — toast (алғашқы жүктеуді есепке алмайды).
    var lastFavorite by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(favorite) {
        when {
            lastFavorite == null -> lastFavorite = favorite
            lastFavorite != favorite -> {
                snackbar.showSnackbar(if (favorite) favoriteAdded else favoriteRemoved)
                lastFavorite = favorite
            }
        }
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = detail?.base?.title ?: stringResource(L10nR.string.detail_title),
                onBack = onBack,
                actions = {
                    AgroIconButton(
                        icon = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(L10nR.string.favorites_title),
                        onClick = {
                            viewModel.toggleFavorite()
                            // Тост кейін: snackbar favorite-өзгеріс effect-те көрсетілмейді — орынды.
                        },
                        tint = if (favorite) MaterialTheme.colorScheme.error
                        else extendedColors().secondaryText,
                    )
                    val item = detail
                    AgroIconButton(
                        icon = Icons.Outlined.Share,
                        contentDescription = stringResource(L10nR.string.common_share),
                        onClick = {
                            item?.let { d ->
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "${d.base.title} — ${PriceFormatter.format(d.base.price, d.base.currency ?: "")}",
                                    )
                                }
                                context.startActivity(Intent.createChooser(share, null))
                            }
                        },
                    )
                },
            )
        },
        bottomBar = { bottomBar?.invoke() },
    ) { inner ->
        Box(modifier = inner.fillMaxSize()) {
            when {
                loading && detail == null -> DetailSkeleton()
                error != null && detail == null -> CenteredContent {
                    ErrorWithRetry(
                        onRetry = { viewModel.load(announcementId) },
                        message = error!!.displayText(),
                    )
                }
                detail != null -> DetailContent(
                    detail = detail!!,
                    onOpenDetail = onOpenDetail,
                    announcementId = announcementId,
                    onOpenChat = onOpenChat,
                    onOpenPhotoViewer = onOpenPhotoViewer,
                    onOpenReviews = onOpenReviews,
                    onOpenSellerReviews = onOpenSellerReviews,
                )
                else -> LoadingWidget(Modifier.fillMaxSize())
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun DetailContent(
    detail: com.agroland.feature.marketplace.data.FullAnnouncement,
    onOpenDetail: (Long) -> Unit,
    announcementId: Long,
    onOpenChat: (otherUserId: Long, username: String?, announcementId: Long) -> Unit,
    onOpenPhotoViewer: (images: List<String>, index: Int) -> Unit,
    onOpenReviews: (Long) -> Unit,
    onOpenSellerReviews: (userId: Long) -> Unit,
) {
    val ext = extendedColors()
    val context = LocalContext.current
    val base = detail.base
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Галерея.
        item {
            val images = base.imageUrls.ifEmpty { listOfNotNull(base.imageUrl) }
            if (images.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(ext.grey),
                )
            } else if (images.size == 1) {
                CachedImage(
                    url = images.first(),
                    contentDescription = base.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clickable { onOpenPhotoViewer(images, 0) },
                )
            } else {
                val pagerState = rememberPagerState(pageCount = { images.size })
                Box {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                    ) { page ->
                        CachedImage(
                            url = images[page],
                            contentDescription = base.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clickable { onOpenPhotoViewer(images, page) },
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ext.black.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${images.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.white,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        // Баға + белгілер.
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val price = PriceFormatter.format(base.price, base.currency ?: "")
                    if (price.isNotBlank()) {
                        Text(
                            text = price,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        base.measurementUnit?.takeIf { it.isNotBlank() }?.let { unit ->
                            Text(
                                text = " / $unit",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.secondaryText,
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(L10nR.string.mp_negotiable_short),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (base.isVip) {
                        com.agroland.core.ui.components.StatusChip(
                            text = "VIP",
                            color = ext.accent,
                            textColor = ext.black,
                        )
                    }
                }
                listOfNotNull(base.city, base.district).joinToString(", ")
                    .takeIf { it.isNotBlank() }?.let { place ->
                        Text(
                            text = place,
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.secondaryText,
                        )
                    }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = DateFormatter.formatDate(base.createdAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.secondaryText,
                    )
                    if (base.negotiable) {
                        Text(
                            text = stringResource(L10nR.string.detail_negotiable),
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.secondaryText,
                        )
                    }
                }
            }
        }

        // Сатушы картасы — Фаза 18: басылса сатушы пікірлері ашылады.
        item {
            detail.seller?.let { seller ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ext.card)
                        .clickable(enabled = base.authorId != null && base.authorId > 0) {
                            base.authorId?.let { onOpenSellerReviews(it) }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ext.grey),
                    ) {
                        CachedImage(
                            url = seller.avatarUrl,
                            contentDescription = seller.name,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = seller.name ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (seller.isVipSeller) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "VIP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.accent,
                                )
                            }
                        }
                        val rating = seller.rating ?: base.rating
                        if (rating != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = ext.accent,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = " " + String.format(java.util.Locale.US, "%.1f", rating) +
                                        " (${base.reviewsCount})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ext.secondaryText,
                                )
                            }
                        }
                        seller.memberSince?.let { since ->
                            Text(
                                text = stringResource(L10nR.string.detail_member_since, DateFormatter.formatDate(since)),
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.secondaryText,
                            )
                        }
                    }
                }
            }
        }

        // Пікірлер — Kaspi-стиль превью (Фаза 18, spec §10): орташа рейтинг
        // + 3 превью + «Барлық пікірлер»; бөлім әрдайым көрінеді (Flutter-дегі
        // feedback бөлімінің Android паритеті).
        item {
            AnnouncementReviewsSection(
                announcementId = announcementId,
                rating = detail.seller?.rating ?: base.rating ?: 0.0,
                reviewsCount = base.reviewsCount,
                onOpenReviews = { onOpenReviews(announcementId) },
            )
        }

        // Байланыс нөмірлері + Чат + Қоңырау шалу.
        item {
            val numbers = detail.contactNumbers.ifEmpty {
                listOfNotNull(detail.seller?.phoneNumber)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val authorId = base.authorId
                if (authorId != null && authorId > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AgroButton(
                            text = stringResource(L10nR.string.chat),
                            onClick = { onOpenChat(authorId, detail.seller?.name ?: base.title, announcementId) },
                            modifier = Modifier.weight(1f),
                        )
                        if (numbers.isNotEmpty()) {
                            AgroButton(
                                text = stringResource(L10nR.string.detail_call),
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${numbers.first()}"))
                                    context.startActivity(intent)
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else {
                    numbers.firstOrNull()?.let { number ->
                        AgroButton(
                            text = stringResource(L10nR.string.detail_call),
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                numbers.drop(1).forEach { number ->
                    Text(
                        text = number,
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.secondaryText,
                    )
                }
            }
        }

        // Сипаттама.
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(L10nR.string.detail_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.primaryText,
                )
                Text(
                    text = detail.description ?: stringResource(L10nR.string.detail_no_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.secondaryText,
                )
            }
        }

        // Сипаттамалар.
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ext.card)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(L10nR.string.detail_characteristics),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.primaryText,
                )
                CharacteristicRow(stringResource(L10nR.string.detail_delivery), base.deliveryAvailable)
                CharacteristicRow(stringResource(L10nR.string.detail_pickup), base.pickupAvailable)
                base.pickupAddress?.takeIf { it.isNotBlank() }?.let { address ->
                    Text(
                        text = stringResource(L10nR.string.detail_pickup_address, address),
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.secondaryText,
                    )
                }
                if (detail.priceIncludesVat) {
                    Text(
                        text = stringResource(L10nR.string.detail_vat_included),
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.secondaryText,
                    )
                }
            }
        }

        // Қосымша (сол сатушының басқа жарнамалары).
        if (detail.additional.isNotEmpty()) {
            item {
                SectionHeader(stringResource(L10nR.string.detail_additional))
            }
            items(detail.additional, key = { "add_${it.id}" }) { item ->
                AnnouncementCard(
                    item = item,
                    onClick = { onOpenDetail(item.id) },
                )
            }
        }

        // Ұқсас жарнамалар.
        if (detail.similar.isNotEmpty()) {
            item {
                SectionHeader(stringResource(L10nR.string.detail_similar))
            }
            items(detail.similar, key = { "sim_${it.id}" }) { item ->
                AnnouncementCard(
                    item = item,
                    onClick = { onOpenDetail(item.id) },
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun CharacteristicRow(label: String, value: Boolean) {
    val ext = extendedColors()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ext.secondaryText,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(if (value) L10nR.string.common_yes else L10nR.string.common_no),
            style = MaterialTheme.typography.labelMedium,
            color = if (value) MaterialTheme.colorScheme.primary else ext.secondaryText,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = extendedColors().primaryText,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun DetailSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            height = 240,
        )
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.9f), height = 12)
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f), height = 12)
    }
}