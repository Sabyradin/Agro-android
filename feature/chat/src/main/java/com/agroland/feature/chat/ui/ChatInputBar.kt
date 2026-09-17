package com.agroland.feature.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroland.core.l10n.R as L10nR
import com.agroland.feature.chat.data.ChatMessage
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/** Жазу күйі: бос / саусақ басулы тұр / бекітілген (қолсыз жазу). */
private enum class RecordMode { IDLE, HOLDING, LOCKED }

/**
 * Чат енгізу жолағы — WhatsApp (iOS) үлгісі:
 *  - сол жақта «+» (қосымшалар торы), ортада көпжолды ақ өріс + камера,
 *    оң жақта жасыл дөңгелек: бос кезде микрофон, мәтін болса — жіберу;
 *  - микрофонды басып тұру → жазу басталады; солға сырғыту → болдырмау;
 *    жоғары сырғыту → бекіту (қолды жіберуге болады, астынан жою/жіберу);
 *  - жауап / өңдеу — өрістің үстінде түсті жолақты цитата.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    replyTo: ChatMessage?,
    onClearReply: () -> Unit,
    editing: ChatMessage?,
    onClearEditing: () -> Unit,
    readOnly: Boolean,
    canRecord: Boolean,
    upload: UploadState?,
    showDeliveryPrice: Boolean,
    deliveryPrice: String,
    onDeliveryPriceChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onPickAudio: () -> Unit,
    onPickLocation: () -> Unit,
    onTakePhoto: () -> Unit,
    onRecordVideo: () -> Unit,
    onSendVoice: (path: String, durationSec: Int) -> Unit,
    onVoiceError: () -> Unit,
    onVoiceTooShort: () -> Unit,
    /** Микрофон рұқсаты бар ма (жоқ болса [onRequestMicPermission]). */
    hasMicPermission: () -> Boolean = { true },
    onRequestMicPermission: () -> Unit = {},
    /** Қысқа түрту — «басып тұрыңыз» кеңесі. */
    onHoldHint: () -> Unit = {},
    /** Жауап берілетін хабарлама авторының аты. */
    replyAuthor: String? = null,
) {
    val palette = chatPalette()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var attachOpen by remember { mutableStateOf(false) }
    val recorder = remember { VoiceRecorder(context) }
    var mode by remember { mutableStateOf(RecordMode.IDLE) }
    var seconds by remember { mutableIntStateOf(0) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val levels = remember { mutableStateListOf<Float>() }

    // Таймер + дыбыс деңгейі (толқын-форм үшін).
    LaunchedEffect(mode != RecordMode.IDLE) {
        if (mode == RecordMode.IDLE) return@LaunchedEffect
        seconds = 0
        levels.clear()
        var tick = 0
        while (true) {
            delay(100)
            tick++
            if (tick % 10 == 0) seconds++
            levels.add(recorder.amplitude().coerceIn(0f, 1f))
            if (levels.size > 60) levels.removeAt(0)
            // Ең ұзақ шекке жетсе — автоматты жіберу.
            if (seconds * 1000 >= VoiceRecorder.MAX_DURATION_MS) break
        }
    }
    DisposableEffect(Unit) {
        onDispose { recorder.cancel() }
    }

    fun resetRecording() {
        mode = RecordMode.IDLE
        dragX = 0f
        dragY = 0f
    }

    fun cancelRecording() {
        recorder.cancel()
        resetRecording()
    }

    fun stopAndSend() {
        val result = recorder.stop()
        resetRecording()
        if (result != null) {
            val (file, duration) = result
            if (duration < 1) {
                file.delete()
                onVoiceTooShort()
            } else {
                onSendVoice(file.absolutePath, duration)
            }
        } else {
            onVoiceError()
        }
    }

    LaunchedEffect(seconds) {
        if (mode != RecordMode.IDLE && seconds * 1000 >= VoiceRecorder.MAX_DURATION_MS) stopAndSend()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.inputBar),
    ) {
        // Жүктеу прогресі.
        upload?.let { state ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                val label = when (state.kind) {
                    UploadKind.AUDIO -> stringResource(L10nR.string.chat_audio_uploading)
                    else -> stringResource(L10nR.string.chat_file_uploading)
                }
                Text("$label ${state.progress}%", fontSize = 12.sp, color = palette.meta)
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    color = palette.accent,
                    trackColor = palette.quoteBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(50)),
                )
            }
        }

        // Жеткізу «иә» — Сумма өрісі.
        AnimatedVisibility(visible = showDeliveryPrice, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            val sumLabel = stringResource(L10nR.string.delivery_sum_label)
            val priceHint = stringResource(L10nR.string.delivery_price_hint)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.inputField)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$sumLabel: ", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = palette.text)
                Box(Modifier.weight(1f)) {
                    if (deliveryPrice.isBlank()) Text(priceHint, fontSize = 15.sp, color = palette.meta)
                    BasicTextField(
                        value = deliveryPrice,
                        onValueChange = onDeliveryPriceChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        textStyle = TextStyle(fontSize = 15.sp, color = palette.text),
                        cursorBrush = SolidColor(palette.accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (readOnly) {
            Text(
                stringResource(L10nR.string.read_only_chat_message),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                fontSize = 14.sp,
                color = palette.meta,
                textAlign = TextAlign.Center,
            )
            return@Column
        }

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Crossfade(targetState = mode, animationSpec = tween(160), label = "inputMode") { current ->
                        when (current) {
                            RecordMode.HOLDING -> HoldingStrip(
                                seconds = seconds,
                                dragX = dragX,
                                cancelFraction = (abs(dragX) / with(LocalDensity.current) { CancelDistance.toPx() }).coerceIn(0f, 1f),
                            )
                            RecordMode.LOCKED -> LockedStrip(
                                seconds = seconds,
                                levels = levels,
                                onDelete = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    cancelRecording()
                                },
                            )
                            RecordMode.IDLE -> ComposeRow(
                                input = input,
                                onInputChange = onInputChange,
                                replyTo = replyTo,
                                replyAuthor = replyAuthor,
                                onClearReply = onClearReply,
                                editing = editing,
                                onClearEditing = onClearEditing,
                                onOpenAttach = { attachOpen = true },
                                onTakePhoto = onTakePhoto,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))

                val showSend = mode == RecordMode.LOCKED ||
                    (mode == RecordMode.IDLE && (input.isNotBlank() || editing != null))
                if (showSend) {
                    RoundActionButton(
                        icon = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(L10nR.string.voice_send_hint),
                        onClick = {
                            if (mode == RecordMode.LOCKED) stopAndSend() else onSend()
                        },
                    )
                } else if (canRecord) {
                    MicButton(
                        holding = mode == RecordMode.HOLDING,
                        dragX = dragX,
                        hasPermission = hasMicPermission,
                        onRequestPermission = onRequestMicPermission,
                        onStart = {
                            val started = recorder.start()
                            if (started) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                mode = RecordMode.HOLDING
                            } else {
                                onVoiceError()
                            }
                            started
                        },
                        onDrag = { x, y ->
                            dragX = x
                            dragY = y
                        },
                        onCancel = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            cancelRecording()
                        },
                        onLock = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            dragX = 0f
                            dragY = 0f
                            mode = RecordMode.LOCKED
                        },
                        onRelease = {
                            if (recorder.elapsedMs() < 700) {
                                // Қысқа түрту — жазба жіберілмейді, кеңес шығады.
                                cancelRecording()
                                onHoldHint()
                            } else {
                                stopAndSend()
                            }
                        },
                    )
                } else {
                    RoundActionButton(
                        icon = Icons.Filled.Mic,
                        contentDescription = null,
                        onClick = {},
                        enabled = false,
                    )
                }
            }

            // Бекіту көрсеткіші — микрофонның үстінде, саусақ жоғары жылжыған сайын көтеріледі.
            if (mode == RecordMode.HOLDING) {
                val density = LocalDensity.current
                val lockLiftPx = with(density) { LockDistance.toPx() }
                val progress = (abs(dragY) / lockLiftPx).coerceIn(0f, 1f)
                val bob = rememberInfiniteTransition(label = "lockBob")
                val bobOffset by bob.animateFloat(
                    initialValue = 0f,
                    targetValue = -6f,
                    animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                    label = "lockBobValue",
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = palette.inputField,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp)
                        .offset {
                            IntOffset(0, (-(76.dp.toPx()) + dragY * 0.6f + bobOffset * density.density).roundToInt())
                        },
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = stringResource(L10nR.string.voice_lock_hint),
                            tint = if (progress > 0.8f) palette.accent else palette.meta,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Icon(
                            Icons.Outlined.KeyboardArrowUp,
                            contentDescription = null,
                            tint = palette.meta,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }

    if (attachOpen) {
        AttachmentSheet(
            onDismiss = { attachOpen = false },
            onPickImage = onPickImage,
            onTakePhoto = onTakePhoto,
            onRecordVideo = onRecordVideo,
            onPickFile = onPickFile,
            onPickAudio = onPickAudio,
            onPickLocation = onPickLocation,
        )
    }
}

private val CancelDistance = 110.dp
private val LockDistance = 90.dp

/** Қалыпты күй: «+» + ақ дөңгелек өріс (жауап/өңдеу цитатасымен) + камера. */
@Composable
private fun ComposeRow(
    input: String,
    onInputChange: (String) -> Unit,
    replyTo: ChatMessage?,
    replyAuthor: String?,
    onClearReply: () -> Unit,
    editing: ChatMessage?,
    onClearEditing: () -> Unit,
    onOpenAttach: () -> Unit,
    onTakePhoto: () -> Unit,
) {
    val palette = chatPalette()
    val writeHint = stringResource(L10nR.string.write)
    Row(verticalAlignment = Alignment.Bottom) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onOpenAttach),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = palette.accent, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(2.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(palette.inputField),
        ) {
            val quoteTarget = editing ?: replyTo
            AnimatedVisibility(
                visible = quoteTarget != null,
                enter = expandVertically(spring()) + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                quoteTarget?.let { target ->
                    ComposerQuote(
                        icon = if (editing != null) Icons.Outlined.Edit else Icons.AutoMirrored.Outlined.Reply,
                        title = if (editing != null) stringResource(L10nR.string.edit) else (replyAuthor ?: stringResource(L10nR.string.reply)),
                        text = target.displayText(),
                        onClose = if (editing != null) onClearEditing else onClearReply,
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .padding(start = 14.dp, end = 4.dp, top = 11.dp, bottom = 11.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (input.isEmpty()) {
                        Text(writeHint, fontSize = 16.sp, color = palette.meta)
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = onInputChange,
                        maxLines = 6,
                        textStyle = TextStyle(fontSize = 16.sp, color = palette.text, lineHeight = 21.sp),
                        cursorBrush = SolidColor(palette.accent),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (input.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onTakePhoto),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = stringResource(L10nR.string.attach_camera),
                            tint = palette.meta,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Өрістің ішіндегі жауап/өңдеу цитатасы. */
@Composable
private fun ComposerQuote(
    icon: ImageVector,
    title: String,
    text: String,
    onClose: () -> Unit,
) {
    val palette = chatPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 6.dp)
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.quoteBackground),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(palette.quoteBar),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = palette.quoteBar, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(title, color = palette.quoteBar, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Text(text.ifBlank { "…" }, color = palette.meta, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Close, contentDescription = stringResource(L10nR.string.cancel), tint = palette.meta, modifier = Modifier.size(18.dp))
        }
    }
}

/** Басып тұрып жазу: жыпылықтайтын қызыл микрофон + таймер + «‹ болдырмау». */
@Composable
private fun HoldingStrip(seconds: Int, dragX: Float, cancelFraction: Float) {
    val palette = chatPalette()
    val blink = rememberInfiniteTransition(label = "recBlink")
    val alpha by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
        label = "recBlinkAlpha",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(palette.inputField)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Mic,
            contentDescription = null,
            tint = Color(0xFFE53935).copy(alpha = alpha),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            formatDuration(seconds),
            fontSize = 16.sp,
            color = palette.text,
            fontWeight = FontWeight.Medium,
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier.graphicsLayer {
                    translationX = dragX * 0.7f
                    this.alpha = 1f - cancelFraction * 0.9f
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = palette.meta,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    stringResource(L10nR.string.voice_slide_to_cancel),
                    fontSize = 14.sp,
                    color = palette.meta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Бекітілген жазу: жою + таймер + тірі толқын-форм (жіберу — оң жақтағы батырма). */
@Composable
private fun LockedStrip(seconds: Int, levels: List<Float>, onDelete: () -> Unit) {
    val palette = chatPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(palette.inputField)
            .padding(end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(L10nR.string.voice_cancel_hint),
                tint = palette.meta,
                modifier = Modifier.size(24.dp),
            )
        }
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935)),
        )
        Spacer(Modifier.width(6.dp))
        Text(formatDuration(seconds), fontSize = 15.sp, color = palette.text, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(10.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .height(28.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            levels.takeLast(40).forEach { level ->
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height((3 + level * 25).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(palette.meta),
                )
            }
        }
    }
}

/** Жасыл дөңгелек батырма (жіберу). */
@Composable
private fun RoundActionButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val palette = chatPalette()
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (enabled) palette.accent else palette.meta.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

/**
 * Микрофон: басу → жазу; саусақпен бірге солға жылжиды, ұстап тұрғанда
 * үлкейеді. Солға [CancelDistance] — болдырмау, жоғары [LockDistance] — бекіту.
 */
@Composable
private fun MicButton(
    holding: Boolean,
    dragX: Float,
    hasPermission: () -> Boolean,
    onRequestPermission: () -> Unit,
    onStart: () -> Boolean,
    onDrag: (x: Float, y: Float) -> Unit,
    onCancel: () -> Unit,
    onLock: () -> Unit,
    onRelease: () -> Unit,
) {
    val palette = chatPalette()
    val density = LocalDensity.current
    val cancelPx = with(density) { CancelDistance.toPx() }
    val lockPx = with(density) { LockDistance.toPx() }
    val scale by animateFloatAsState(if (holding) 1.45f else 1f, spring(dampingRatio = 0.6f), label = "micScale")

    val currentHasPermission by rememberUpdatedState(hasPermission)
    val currentRequest by rememberUpdatedState(onRequestPermission)
    val currentStart by rememberUpdatedState(onStart)
    val currentDrag by rememberUpdatedState(onDrag)
    val currentCancel by rememberUpdatedState(onCancel)
    val currentLock by rememberUpdatedState(onLock)
    val currentRelease by rememberUpdatedState(onRelease)

    // Жест түрленбейтін сыртқы қабатта ұсталады: ішкі дөңгелек саусақпен
    // бірге жылжып/үлкейгенде координаталар бұрмаланбауы керек.
    Box(
        modifier = Modifier
            .size(48.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    if (!currentHasPermission()) {
                        currentRequest()
                        return@awaitEachGesture
                    }
                    if (!currentStart()) return@awaitEachGesture
                    var result = 0 // 0 — жіберу, 1 — болдырмау, 2 — бекіту
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val dx = (change.position.x - down.position.x).coerceAtMost(0f)
                        val dy = (change.position.y - down.position.y).coerceAtMost(0f)
                        // Бір бағытты таңдаймыз: көлденең не тік.
                        if (abs(dx) > abs(dy)) currentDrag(dx, 0f) else currentDrag(0f, dy)
                        change.consume()
                        if (dx < -cancelPx) {
                            result = 1
                            break
                        }
                        if (dy < -lockPx) {
                            result = 2
                            break
                        }
                    }
                    when (result) {
                        1 -> currentCancel()
                        2 -> currentLock()
                        else -> currentRelease()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    translationX = if (holding) dragX.coerceAtLeast(-cancelPx) * 0.35f else 0f
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.7f, 0.7f)
                }
                .clip(CircleShape)
                .background(palette.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = stringResource(L10nR.string.voice_hold_to_record),
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** WhatsApp-тағыдай қосымшалар торы: түсті дөңгелектер + жазу. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentSheet(
    onDismiss: () -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onRecordVideo: () -> Unit,
    onPickFile: () -> Unit,
    onPickAudio: () -> Unit,
    onPickLocation: () -> Unit,
) {
    val palette = chatPalette()
    val items = listOf(
        AttachItem(Icons.Filled.Image, stringResource(L10nR.string.attach_gallery), Color(0xFF3D8BFD), onPickImage),
        AttachItem(Icons.Filled.PhotoCamera, stringResource(L10nR.string.attach_camera), Color(0xFFFF2E74), onTakePhoto),
        AttachItem(Icons.Filled.Videocam, stringResource(L10nR.string.attach_video), Color(0xFFC861FA), onRecordVideo),
        AttachItem(Icons.Filled.Description, stringResource(L10nR.string.attach_document), Color(0xFF7F66FF), onPickFile),
        AttachItem(Icons.Filled.Headphones, stringResource(L10nR.string.attach_audio_short), Color(0xFFFF7F2E), onPickAudio),
        AttachItem(Icons.Filled.LocationOn, stringResource(L10nR.string.attach_location), Color(0xFF1FA855), onPickLocation),
    )
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = palette.inputField) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            items.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { item ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = androidx.compose.material3.ripple(bounded = true),
                                ) {
                                    onDismiss()
                                    item.onClick()
                                }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(item.color),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(item.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(item.label, fontSize = 13.sp, color = palette.text, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private data class AttachItem(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val onClick: () -> Unit,
)
