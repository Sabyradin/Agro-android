package com.agroland.feature.chat.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Свайп-жол (Flutter Slidable BehindMotion + DismissiblePane паритеті):
 * солға тартқанда 3 дөңгелек әрекет батырмасы ашылады. Толық свайп —
 * басты әрекет (мұрағат). Бір уақытта экранда бір ғана жол ашық.
 */
data class SwipeAction(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val isPrimary: Boolean = false,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
fun SwipeActionRow(
    actions: List<SwipeAction>,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { (72 * actions.size).dp.toPx() }
    val fullSwipePx = with(density) { 96.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val dragStartOffset = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    // Басқа жол ашылса — бұл жол жабылады.
    LaunchedEffect(isOpen) {
        if (!isOpen && offset.value != 0f && !offset.isRunning) {
            offset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(actions.size, actions.map { it.label }.joinToString()) {
                detectHorizontalDragGestures(
                    onDragStart = { dragStartOffset.floatValue = offset.value },
                    onDragEnd = {
                        val current = offset.value
                        val total = dragStartOffset.floatValue - current
                        when {
                            // Толық свайп (шектен асқан немесе лездік жылдамдық) → басты әрекет.
                            -current > actionWidthPx + fullSwipePx * 0.4f && total > 0 -> {
                                actions.firstOrNull { it.isPrimary }?.let { action ->
                                    scope.launch {
                                        offset.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                    }
                                    action.onClick()
                                }
                            }
                            // Жартылай — ашық қалады.
                            -current > actionWidthPx * 0.5f -> {
                                scope.launch {
                                    offset.animateTo(-actionWidthPx, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                                onOpenChange(true)
                            }
                            else -> {
                                scope.launch {
                                    offset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                                onOpenChange(false)
                            }
                        }
                    },
                    onDragCancel = {
                        if (-offset.value > actionWidthPx * 0.5f) {
                            scope.launch {
                                offset.animateTo(-actionWidthPx, spring(stiffness = Spring.StiffnessMediumLow))
                            }
                            onOpenChange(true)
                        } else {
                            scope.launch {
                                offset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            }
                            onOpenChange(false)
                        }
                    },
                ) { change, dragAmount ->
                    val newX = (offset.value + dragAmount).coerceIn(-actionWidthPx - fullSwipePx, 0f)
                    if (offset.isRunning) scope.launch { offset.stop() }
                    scope.launch { offset.snapTo(newX) }
                    change.consume()
                }
            },
    ) {
        // Behind-pane: 3 дөңгелек батырма, оң жаққа тураланған.
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEach { action ->
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(action.color)
                        .clickable(onClick = action.onClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.label,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
        // Алдыңғы жол — көлденең жылжытылған.
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(MaterialTheme.colorScheme.surface),
        ) {
            content()
        }
    }
}