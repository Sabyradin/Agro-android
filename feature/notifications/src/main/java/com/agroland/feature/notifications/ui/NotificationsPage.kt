package com.agroland.feature.notifications.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.notifications.data.NotificationCounter
import com.agroland.feature.notifications.data.NotificationType

/**
 * Хабарламалар хабы (Flutter NotificatinosPage): үш бөлім жолы —
 * AgroLand / Қолдау / Акциялар, оқылмаған бейдждерімен. Pull-to-refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsPage(
    onBack: () -> Unit,
    onOpenType: (NotificationType) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title = stringResource(L10nR.string.notifications)

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
            val counter = state.counter
            when {
                state.error && counter == null -> Box(Modifier.fillMaxSize()) {
                    ErrorWithRetry(
                        onRetry = viewModel::load,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                counter == null -> LoadingWidget(Modifier.fillMaxSize())
                else -> HubList(
                    counter = counter,
                    onOpenType = onOpenType,
                )
            }
        }
    }
}

@Composable
private fun HubList(
    counter: NotificationCounter,
    onOpenType: (NotificationType) -> Unit,
) {
    val ext = extendedColors()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(ext.card),
        ) {
            Column(Modifier.fillMaxWidth()) {
                HubRow(
                    icon = Icons.Outlined.Campaign,
                    title = stringResource(L10nR.string.app_title),
                    count = counter.serviceCount,
                    onClick = { onOpenType(NotificationType.SERVICE) },
                )
                HorizontalDivider(color = ext.divider, modifier = Modifier.padding(start = 68.dp))
                HubRow(
                    icon = Icons.Outlined.SupportAgent,
                    title = stringResource(L10nR.string.support),
                    count = counter.supportCount,
                    onClick = { onOpenType(NotificationType.SUPPORT) },
                )
                HorizontalDivider(color = ext.divider, modifier = Modifier.padding(start = 68.dp))
                HubRow(
                    icon = Icons.Outlined.Percent,
                    title = stringResource(L10nR.string.promotions),
                    count = counter.promotionsCount,
                    onClick = { onOpenType(NotificationType.PROMOTIONS) },
                    isLast = true,
                )
            }
        }
    }
}

@Composable
private fun HubRow(
    icon: ImageVector,
    title: String,
    count: Int,
    onClick: () -> Unit,
    isLast: Boolean = false,
) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = if (isLast) 12.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Иконка — primary-дің 12% фонді бар дөңгелекте.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = ext.primaryText,
            modifier = Modifier.weight(1f),
        )
        if (count > 0) {
            BadgeChip(count = count)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = ext.secondaryText,
        )
    }
}

/** Қызыл бейдж чипі — оқылмаған санақ. */
@Composable
internal fun BadgeChip(count: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.error),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onError,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}