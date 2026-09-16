package com.agroland.feature.notifications.ui

import android.util.Base64
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.notifications.data.NotificationItem
import com.agroland.feature.notifications.data.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/**
 * Бөлім хабарламалары тізімі (Flutter NotificationByTypePage):
 * карточка = сурет (экран енінің 40%) + тақырып + HTML превью + дата.
 * 24dp аралықпен, басқанда — толық хабарлама беті.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsByTypePage(
    onBack: () -> Unit,
    onOpenItem: (NotificationItem, NotificationType) -> Unit,
    viewModel: NotificationsByTypeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title = when (viewModel.type) {
        NotificationType.SERVICE -> stringResource(L10nR.string.app_title)
        NotificationType.SUPPORT -> stringResource(L10nR.string.support)
        NotificationType.PROMOTIONS -> stringResource(L10nR.string.promotions)
    }

    AgroScaffold(
        topBar = { com.agroland.core.ui.components.AgroAppBar(title = title, onBack = onBack) },
    ) { padding ->
        val pullState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = viewModel::load,
            state = pullState,
            modifier = padding.fillMaxSize(),
        ) {
            when {
                state.loading && state.items.isEmpty() -> LoadingWidget(Modifier.fillMaxSize())
                state.error && state.items.isEmpty() -> Box(Modifier.fillMaxSize()) {
                    ErrorWithRetry(
                        onRetry = viewModel::load,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                state.items.isEmpty() -> Box(Modifier.fillMaxSize()) {
                    EmptyView(
                        icon = Icons.Outlined.NotificationsNone,
                        title = stringResource(L10nR.string.no_notifications_yet),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        NotificationCard(
                            item = item,
                            onClick = { onOpenItem(item, viewModel.type) },
                        )
                    }
                }
            }
        }
    }
}

/** Хабарлама карточкасы — сурет + мәтін блогы + дата (Flutter паритеті). */
@Composable
private fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit,
) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.card)
            .clickable(onClick = onClick),
    ) {
        if (item.file.isNotEmpty()) {
            NotificationImage(
                file = item.file,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.5f), // экран енінің 40% биіктігі
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            )
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = ext.primaryText,
            )
            PlainHtmlText(
                html = item.text,
                maxLines = 2,
                modifier = Modifier.padding(top = 6.dp),
            )
            HorizontalDivider(
                color = ext.divider,
                modifier = Modifier.padding(vertical = 10.dp),
            )
            Text(
                text = formatNotificationDate(item.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = ext.secondaryText,
            )
        }
    }
}

/**
 * Хабарлама суреті (Flutter buildNotificationImage): URL (http/https) →
 * CachedImage; әйтпесе base64 деп оқып bitmap-ке айналдырамыз.
 */
@Composable
internal fun NotificationImage(
    file: String,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val isUrl = file.startsWith("http://") || file.startsWith("https://")
    val base = if (shape != null) modifier.clip(shape) else modifier

    if (isUrl) {
        CachedImage(
            url = file,
            contentDescription = null,
            modifier = base,
            contentScale = contentScale,
        )
    } else {
        // Base64 → bitmap (Flutter Image.memory паритеті).
        val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, file) {
            value = withContext(Dispatchers.IO) {
                try {
                    val bytes = Base64.decode(file, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (_: Exception) {
                    null
                }
            }
        }
        bitmap?.let { bm ->
            androidx.compose.foundation.Image(
                bitmap = bm.asImageBitmap(),
                contentDescription = null,
                contentScale = contentScale,
                modifier = base,
            )
        }
    }
}

/** «12.09.2026» пішімі (UiUtils.dateFormatter). */
internal fun formatNotificationDate(millis: Long): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return "%02d.%02d.%04d".format(date.dayOfMonth, date.monthValue, date.year)
}