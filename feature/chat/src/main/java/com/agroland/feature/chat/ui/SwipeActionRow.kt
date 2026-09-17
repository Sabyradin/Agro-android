package com.agroland.feature.chat.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Свайп-жол (iOS / WhatsApp чат тізімі): жолды солға тартқанда оң жақтан
 * толық биіктіктегі түсті әрекет панельдері (иконка + жазу) шығады.
 *  - панельдер саусақпен бірге біртіндеп ашылады (әрқайсысы өз үлесін алады);
 *  - қол жібергенде жылдамдық пен қашықтыққа қарай серпімді ашылады/жабылады;
 *  - ашық жолды түрту немесе басқа жолды ашу — жабады;
 *  - соңына дейін қатты тартса — басты әрекет (мұрағат) + діріл.
 */
data class SwipeAction(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val isPrimary: Boolean = false,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

private val ActionWidth = 78.dp

@Composable
fun SwipeActionRow(
    actions: List<SwipeAction>,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val openPx = with(density) { (ActionWidth * actions.size).toPx() }
    val fullSwipePx = openPx * 1.9f
    val offset = remember { Animatable(0f) }
    val fullSwipeArmed = remember { booleanArrayOf(false) }
    val springSpec = spring<Float>(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)

    fun animateTo(target: Float, velocity: Float = 0f) {
        scope.launch { offset.animateTo(target, springSpec, initialVelocity = velocity) }
    }

    // Сырттан жабу (басқа жол ашылды / әрекет орындалды).
    LaunchedEffect(isOpen) {
        if (!isOpen && offset.targetValue != 0f) {
            offset.animateTo(0f, springSpec)
        }
    }

    // Саусақ позициясы синхронды есептеледі: snapTo асинхронды, сондықтан
    // offset.value-ды оқысақ, жылдам қозғалыста мәндер «секіреді».
    val dragPos = remember { floatArrayOf(0f) }
    val dragState = rememberDraggableState { delta ->
        val current = dragPos[0]
        // Шектен асқанда «резеңке» кедергі — iOS сезімі.
        val resisted = if (current < -openPx && delta < 0) delta * 0.45f else delta
        val next = (current + resisted).coerceIn(-fullSwipePx, 0f)
        dragPos[0] = next
        scope.launch { offset.snapTo(next) }
        val armed = -next > openPx * 1.55f
        if (armed != fullSwipeArmed[0]) {
            fullSwipeArmed[0] = armed
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds(),
    ) {
        // Артқы қабат: әрекет панельдері оң жаққа тураланған.
        ActionsLayer(
            actions = actions,
            revealPx = { -offset.value },
            openPx = openPx,
            fullSwipeArmed = { fullSwipeArmed[0] },
            onAction = { action ->
                action.onClick()
                animateTo(0f)
                onOpenChange(false)
            },
            modifier = Modifier.matchParentSize(),
        )

        // Алдыңғы қабат — жолдың өзі.
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(MaterialTheme.colorScheme.surface)
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = {
                        offset.stop()
                        dragPos[0] = offset.value
                        if (!isOpen) onOpenChange(true)
                    },
                    onDragStopped = { velocity ->
                        val reveal = -dragPos[0]
                        when {
                            fullSwipeArmed[0] -> {
                                fullSwipeArmed[0] = false
                                actions.firstOrNull { it.isPrimary }?.onClick?.invoke()
                                animateTo(0f)
                                onOpenChange(false)
                            }
                            // Жылдам солға сілтеу немесе жартысынан асса — ашық.
                            velocity < -600f || (reveal > openPx * 0.45f && velocity < 600f) -> {
                                animateTo(-openPx, velocity)
                                onOpenChange(true)
                            }
                            else -> {
                                animateTo(0f, velocity)
                                onOpenChange(false)
                            }
                        }
                    },
                ),
        ) {
            content()
            // Ашық күйде жолды түрту — ашылмай, жабылады.
            if (isOpen && abs(offset.value) > 1f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            animateTo(0f)
                            onOpenChange(false)
                        },
                )
            }
        }
    }
}

/**
 * Панельдер қабаты: ашылған ен панельдер арасында тең бөлінеді, әр панель
 * өз бөлігінің оң жағынан «шығып» келеді. Толық свайпта басты панель
 * бүкіл енді алады.
 */
@Composable
private fun ActionsLayer(
    actions: List<SwipeAction>,
    revealPx: () -> Float,
    openPx: Float,
    fullSwipeArmed: () -> Boolean,
    onAction: (SwipeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        modifier = modifier,
        content = {
            actions.forEach { action ->
                ActionPanel(action = action, onClick = { onAction(action) })
            }
        },
    ) { measurables, constraints ->
        val reveal = revealPx().coerceAtLeast(0f)
        val height = constraints.maxHeight
        val count = measurables.size.coerceAtLeast(1)
        val armed = fullSwipeArmed()
        val slot = reveal / count
        val placeables = measurables.mapIndexed { index, measurable ->
            val isPrimary = actions[index].isPrimary
            val w = when {
                armed && isPrimary -> reveal
                armed -> 0f
                else -> slot
            }.roundToInt().coerceAtLeast(0)
            measurable.measure(androidx.compose.ui.unit.Constraints.fixed(w, height))
        }
        layout(constraints.maxWidth, height) {
            var x = constraints.maxWidth - reveal.roundToInt()
            placeables.forEach { placeable ->
                placeable.place(x, 0)
                x += placeable.width
            }
        }
    }
}

@Composable
private fun ActionPanel(action: SwipeAction, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .background(action.color)
            .clickable(onClick = onClick)
            .clipToBounds(),
        contentAlignment = Alignment.CenterStart,
    ) {
        // Иконка мен жазу панельдің сол жағына бекітілген — панель кеңейгенде
        // саусақтың соңынан ілесіп жүреді (iOS mail/WhatsApp мінезі).
        Column(
            modifier = Modifier
                .width(ActionWidth)
                .padding(horizontal = 4.dp)
                .graphicsLayer { },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
            if (action.label.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = action.label,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
