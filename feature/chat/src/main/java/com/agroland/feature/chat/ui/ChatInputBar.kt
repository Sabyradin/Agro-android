package com.agroland.feature.chat.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.chat.data.ChatMessage
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Чат енгізу жолағы (Flutter ChatInputBar + recording overlay паритеті):
 *  - reply/edit үстіңгі жолақтары;
 *  - қағаз қайшы — AttachmentPicker ModalBottomSheet;
 *  - бос енгізу кезінде — микрофон (ұстап тұру → жазу, солға → жою,
 *    жоғары → бұғаттау), мәтін болса — жіберу батырмасы;
 *  - delivery «иә» кезінде — Сумма өрісі;
 *  - read-only чаттарда — түсініктеме мәтіні.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
) {
    val colors = extendedColors()
    val context = LocalContext.current
    val writeHint = stringResource(L10nR.string.write)
    val readOnlyHint = stringResource(L10nR.string.read_only_chat_message)
    val sumLabel = stringResource(L10nR.string.delivery_sum_label)
    val priceHint = stringResource(L10nR.string.delivery_price_hint)

    var attachOpen by remember { mutableStateOf(false) }
    val recorder = remember { VoiceRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }

    // Жазба таймері (1с қадам) + амплитуданы жаңарту.
    LaunchedEffect(recording) {
        seconds = 0
        while (recording) {
            delay(1000)
            if (recording) seconds++
        }
    }
    DisposableEffect(Unit) {
        onDispose { recorder.cancel() }
    }

    fun stopAndSend() {
        val result = recorder.stop()
        recording = false
        locked = false
        if (result != null) {
            val (file, duration) = result
            if (duration < 1) onVoiceTooShort() else onSendVoice(file.absolutePath, duration)
        } else {
            onVoiceError()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Upload прогресі.
        upload?.let { state ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                val label = when (state.kind) {
                    UploadKind.IMAGE -> stringResource(L10nR.string.chat_file_uploading)
                    UploadKind.AUDIO -> stringResource(L10nR.string.chat_audio_uploading)
                    else -> stringResource(L10nR.string.chat_file_uploading)
                }
                Text(label, fontSize = 12.sp, color = colors.secondaryText)
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        }

        // Reply жолағы.
        AnimatedVisibility(visible = replyTo != null, enter = fadeIn(), exit = fadeOut()) {
            replyTo?.let { target ->
                BarHeader(
                    icon = Icons.Outlined.Reply,
                    text = target.displayText(),
                    onClose = onClearReply,
                )
            }
        }
        // Edit жолағы.
        AnimatedVisibility(visible = editing != null, enter = fadeIn(), exit = fadeOut()) {
            editing?.let { target ->
                BarHeader(
                    icon = Icons.Outlined.Edit,
                    text = target.displayText(),
                    onClose = onClearEditing,
                )
            }
        }
        // Delivery «иә» — Сумма өрісі.
        AnimatedVisibility(visible = showDeliveryPrice, enter = fadeIn(), exit = fadeOut()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("$sumLabel: ", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    BasicTextField(
                        value = deliveryPrice,
                        onValueChange = onDeliveryPriceChange,
                        textStyle = TextStyle(fontSize = 15.sp, color = colors.primaryText),
                        modifier = Modifier.weight(1f),
                    )
                    priceHint.takeIf { deliveryPrice.isBlank() }?.let {
                        Text(it, fontSize = 14.sp, color = colors.secondaryText)
                    }
                }
            }
        }

        if (readOnly) {
            // Тек оқу чаттары (1001, 1002, 1004, 1005).
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(14.dp),
                color = colors.grey.copy(alpha = 0.35f),
            ) {
                Text(
                    readOnlyHint,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    fontSize = 14.sp,
                    color = colors.secondaryText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // Қағаз қайшы.
                CircleIconButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = null,
                    onClick = { attachOpen = true },
                )
                // Мәтін өрісі.
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = colors.grey.copy(alpha = 0.28f),
                ) {
                    BasicTextField(
                        value = input,
                        onValueChange = onInputChange,
                        textStyle = TextStyle(fontSize = 15.5.sp, color = colors.primaryText),
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (input.isEmpty()) {
                                    Text(writeHint, fontSize = 15.5.sp, color = colors.secondaryText)
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (input.isNotEmpty() || editing != null) {
                    // Жіберу батырмасы.
                    CircleIconButton(
                        icon = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = writeHint,
                        onClick = onSend,
                        primary = true,
                    )
                } else if (canRecord) {
                    // Микрофон — ұстап тұрып жазу.
                    MicButton(
                        recording = recording,
                        recorder = recorder,
                        onRecordingChange = { recording = it },
                        onLocked = { locked = true },
                        onSend = { stopAndSend() },
                        onCancel = {
                            recorder.cancel()
                            recording = false
                            locked = false
                        },
                    )
                }
            }
        }

        // Жазу режимі — таймер + амплитуда + басқару.
        AnimatedVisibility(
            visible = recording,
            enter = slideInVertically(tween(160)) { it } + fadeIn(),
            exit = slideOutVertically(tween(140)) { it } + fadeOut(),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Жою (солға тарту да жояды).
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(L10nR.string.voice_cancel_hint),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .clickable {
                                recorder.cancel()
                                recording = false
                                locked = false
                            },
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "%02d:%02d".format(seconds / 60, seconds % 60),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    // «Жазылуда…» + биение амплитудасы.
                    Text(
                        stringResource(L10nR.string.voice_recording),
                        fontSize = 13.sp,
                        color = colors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (locked) {
                        // Бұғатталған — тоқтату+жіберу батырмасы.
                        Icon(
                            Icons.AutoMirrored.Outlined.Send,
                            contentDescription = stringResource(L10nR.string.voice_send_hint),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable { stopAndSend() },
                        )
                    } else {
                        Text(
                            stringResource(L10nR.string.voice_lock_hint),
                            fontSize = 11.sp,
                            color = colors.secondaryText,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    // AttachmentPicker — 4 опция + камера екеуі.
    if (attachOpen) {
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { attachOpen = false }) {
            AttachmentOption(icon = Icons.Outlined.Image, label = stringResource(L10nR.string.attach_gallery)) {
                attachOpen = false; onPickImage()
            }
            AttachmentOption(icon = Icons.Outlined.AttachFile, label = stringResource(L10nR.string.attach_file)) {
                attachOpen = false; onPickFile()
            }
            AttachmentOption(icon = Icons.Outlined.AudioFile, label = stringResource(L10nR.string.attach_audio)) {
                attachOpen = false; onPickAudio()
            }
            AttachmentOption(icon = Icons.Outlined.LocationOn, label = stringResource(L10nR.string.send_location)) {
                attachOpen = false; onPickLocation()
            }
            AttachmentOption(icon = Icons.Outlined.PhotoCamera, label = stringResource(L10nR.string.take_photo)) {
                attachOpen = false; onTakePhoto()
            }
            AttachmentOption(icon = Icons.Outlined.Videocam, label = stringResource(L10nR.string.record_video)) {
                attachOpen = false; onRecordVideo()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Микрофон батырмасы: ұстап тұру → жазу; жіберу — қолды жібер; жою — солға; бұғаттау — жоғары. */
@Composable
private fun MicButton(
    recording: Boolean,
    recorder: VoiceRecorder,
    onRecordingChange: (Boolean) -> Unit,
    onLocked: () -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    val density = LocalDensity.current
    val cancelPx = with(density) { 80.dp.toPx() }
    val lockPx = with(density) { 100.dp.toPx() }
    val tint = if (recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    if (recorder.isActive.not()) {
                        if (recorder.start()) onRecordingChange(true)
                    }
                    var locked = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.any { it.pressed }
                        val pos = event.changes.firstOrNull()?.position
                        if (pos != null) {
                            val dx = pos.x - down.position.x
                            val dy = pos.y - down.position.y
                            if (dx < -cancelPx) {
                                // Солға тарту → жою.
                                onCancel()
                                event.changes.forEach { it.consume() }
                                return@awaitEachGesture
                            }
                            if (dy < -lockPx) {
                                // Жоғары тарту → бұғаттау.
                                locked = true
                                event.changes.forEach { it.consume() }
                                break
                            }
                        }
                        event.changes.forEach { it.consume() }
                        if (!pressed) break
                    }
                    // Қол жібергенде: бұғатталмаса → тоқтатып жіберу.
                    if (!locked && recorder.isActive) {
                        onSend()
                    } else if (locked && recorder.isActive) {
                        onLocked()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Mic,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Қосымшалар парағының бір өрісі. */
@Composable
private fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

/** Reply/Edit үстіңгі жолағы. */
@Composable
private fun BarHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClose: () -> Unit,
) {
    val colors = extendedColors()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                fontSize = 13.sp,
                color = colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Outlined.Close,
                contentDescription = stringResource(L10nR.string.cancel),
                tint = colors.secondaryText,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose)
                    .padding(2.dp),
            )
        }
    }
}

/** Дөңгелек мөлдір батырма. */
@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(
                if (primary) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (primary) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}