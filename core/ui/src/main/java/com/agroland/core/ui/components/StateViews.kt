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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.theme.AgroRadius
import com.agroland.core.ui.theme.AgroSpacing
import com.agroland.core.ui.theme.extendedColors

/** Дөңгелек жүктеу индикаторы — LoadingWidget баламасы. */
@Composable
fun LoadingWidget(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(40.dp),
        )
    }
}

/**
 * Бос/қате/қонақ күйлерінің ортақ қаңқасы: жұмсақ дөңгелектегі иконка,
 * қалың тақырып, көмескі түсіндірме және ені шектелген әрекет батырмасы.
 *
 * Батырма экранның бүкіл енін алмайды — қысқа сөз («Кіру») созылған
 * жасыл жолаққа айналмас үшін ені 220dp-тен басталады.
 */
@Composable
private fun StatePlaceholder(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val ext = extendedColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.xl, vertical = AgroSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AgroIconCircle(
            icon = icon,
            size = 88.dp,
            iconSize = 40.dp,
            containerColor = ext.grey,
            tint = ext.secondaryText,
        )
        if (title != null) {
            Spacer(Modifier.height(AgroSpacing.screen))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = ext.primaryText,
                textAlign = TextAlign.Center,
            )
        }
        if (message != null) {
            Spacer(Modifier.height(if (title != null) AgroSpacing.sm else AgroSpacing.screen))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = ext.secondaryText,
                textAlign = TextAlign.Center,
            )
        }
        if (action != null) {
            Spacer(Modifier.height(AgroSpacing.xl))
            action()
        }
    }
}

/** Қате + қайта әрекет ету — ErrorWithRetry баламасы. Мәтіндер core:l10n-тан. */
@Composable
fun ErrorWithRetry(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    StatePlaceholder(
        icon = Icons.Rounded.CloudOff,
        modifier = modifier,
        title = stringResource(L10nR.string.error_generic_title),
        message = message ?: stringResource(L10nR.string.error_generic_message),
        action = {
            AgroButton(
                text = stringResource(L10nR.string.common_retry),
                onClick = onRetry,
                modifier = Modifier.widthIn(min = 220.dp),
            )
        },
    )
}

/** Бос тізім күйі. */
@Composable
fun EmptyView(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Inbox,
    title: String? = null,
    message: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    StatePlaceholder(
        icon = icon,
        modifier = modifier,
        title = title,
        message = message,
        action = if (actionText != null && onAction != null) {
            {
                AgroOutlinedButton(
                    text = actionText,
                    onClick = onAction,
                    modifier = Modifier.widthIn(min = 220.dp),
                )
            }
        } else {
            null
        },
    )
}

/**
 * Қонақ күйі — «кіру керек» экраны (себет, чат, профиль қойындылары).
 * Барлық жерде бірдей көрінуі үшін жалғыз орында.
 */
@Composable
fun GuestGate(
    icon: ImageVector,
    message: String,
    loginText: String,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        StatePlaceholder(
            icon = icon,
            message = message,
            action = {
                AgroButton(
                    text = loginText,
                    onClick = onLoginClick,
                    modifier = Modifier.widthIn(min = 220.dp),
                )
            },
        )
    }
}

/** Shimmer эффекті — тізім карточкаларының skeleton-ы. */
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

/**
 * Толық skeleton карточкасы — нақты хабарлама карточкасының пішінін
 * қайталайды (сол жақта сурет, оң жақта үш жол), сондықтан деректер
 * келгенде тізім «секірмейді».
 */
@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screen, vertical = 6.dp)
            .clip(AgroRadius.card)
            .background(extendedColors().card)
            .padding(AgroSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .shimmer(RoundedCornerShape(12.dp)),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.85f), height = 14)
            Spacer(Modifier.height(10.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f), height = 18)
            Spacer(Modifier.height(12.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.65f), height = 12)
        }
    }
}

/**
 * Тор ұяшығына арналған skeleton — нақты [AnnouncementGridCard]-тың
 * пішінін қайталайды (шаршы сурет, астында үш жол).
 */
@Composable
fun ShimmerGridCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AgroRadius.card)
            .background(extendedColors().card),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shimmer(RoundedCornerShape(0.dp)),
        )
        Column(modifier = Modifier.padding(AgroSpacing.sm + 2.dp)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f), height = 11)
            Spacer(Modifier.height(6.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.9f), height = 13)
            Spacer(Modifier.height(8.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f), height = 15)
        }
    }
}

/** Skeleton тізімі — жүктелу кезіндегі толық экран. */
@Composable
fun ShimmerList(count: Int = 6, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        repeat(count) { ShimmerCard() }
    }
}
