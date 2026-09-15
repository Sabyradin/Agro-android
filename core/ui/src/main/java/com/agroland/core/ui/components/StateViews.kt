package com.agroland.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.theme.extendedColors

/** Дөңгелек жүктеу индикаторы — LoadingWidget баламасы. */
@Composable
fun LoadingWidget(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(44.dp),
        )
    }
}

/** Қате + қайта әрекет ету — ErrorWithRetry баламасы. Мәтіндер core:l10n-тан. */
@Composable
fun ErrorWithRetry(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = extendedColors().secondaryText,
            modifier = Modifier.size(56.dp),
        )
        Text(
            text = message ?: stringResource(L10nR.string.error_generic_message),
            style = MaterialTheme.typography.bodySmall,
            color = extendedColors().secondaryText,
        )
        AgroButton(
            text = stringResource(L10nR.string.common_retry),
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}

/** Бос тізім күйі. */
@Composable
fun EmptyView(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox,
    title: String? = null,
    message: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = extendedColors().divider,
            modifier = Modifier.size(72.dp),
        )
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = extendedColors().primaryText,
            )
        }
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = extendedColors().secondaryText,
            )
        }
    }
}

/** Shimmer эффекті — тізим карточкаларының skeleton-ы. */
fun Modifier.shimmer(shape: Shape = RoundedCornerShape(12.dp)): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    val base = extendedColors().grey
    val highlight = extendedColors().card
    val brush = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = androidx.compose.ui.geometry.Offset(x = 600f * progress - 300f, y = 0f),
        end = androidx.compose.ui.geometry.Offset(x = 600f * progress, y = 120f),
    )
    clip(shape).background(brush)
}

/** Skeleton жол блокы. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, height: Int = 16) {
    Box(
        modifier = modifier
            .height(height.dp)
            .shimmer(),
    )
}

/** Толық skeleton карточкасы — тізім экрандарының placeholder-ы. */
@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shimmer(RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f))
        Spacer(Modifier.height(10.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.9f), height = 12)
        Spacer(Modifier.height(8.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.4f), height = 12)
    }
}