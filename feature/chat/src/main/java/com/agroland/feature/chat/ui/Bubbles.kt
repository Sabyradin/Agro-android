package com.agroland.feature.chat.ui

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.CachedImage
import com.agroland.feature.chat.data.ChatMessage
import com.agroland.feature.chat.domain.CallMessage
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Хабарлама мәтінінің көрсетілетін нұсқасы — [tag] тегтерін алып тастайды. */
fun ChatMessage.displayText(): String = message
    .replace(Regex("\\s*\\[[^]]+]"), "")
    .trim()

/** [address:X] тегінен жеткізу адресін шығарады. */
fun ChatMessage.deliveryAddress(): String? =
    Regex("\\[address:(.*?)]").find(message)?.groupValues?.get(1)?.trim()

/** Уақыт үтірі: 14:05. */
fun formatClock(millis: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    return "%02d:%02d".format(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
}

/** Дауыс ұзақтығы: 1:23. */
fun formatDuration(totalSec: Int): String =
    "%d:%02d".format(totalSec / 60, abs(totalSec % 60))

/** Көпіршектің ең үлкен ені — экранның ~80%-ы (WhatsApp). */
@Composable
private fun maxBubbleWidth(): Dp = (LocalConfiguration.current.screenWidthDp * 0.8f).dp

/**
 * WhatsApp көпіршек пішіні: жоғарғы «құйрық» бұрышы өткір (2dp),
 * қалғандары 12dp. Топтағы келесі хабарламаларда құйрық жоқ.
 */
fun bubbleShape(isMine: Boolean, tail: Boolean = true): Shape = RoundedCornerShape(
    topStart = if (!isMine && tail) 2.dp else 12.dp,
    topEnd = if (isMine && tail) 2.dp else 12.dp,
    bottomStart = 12.dp,
    bottomEnd = 12.dp,
)

/** Көпіршек ыдысы: түс + пішін + жұмсақ көлеңке. */
@Composable
private fun BubbleSurface(
    isMine: Boolean,
    modifier: Modifier = Modifier,
    padding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = chatPalette()
    Surface(
        modifier = modifier.widthIn(max = maxBubbleWidth()),
        shape = bubbleShape(isMine),
        color = if (isMine) palette.mineBubble else palette.theirsBubble,
        contentColor = palette.text,
        shadowElevation = 0.6.dp,
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

/** Уақыт + (өзімдікі болса) ✓/✓✓ + «өңделді» — көпіршектің оң жақ астында. */
@Composable
private fun MessageMeta(
    message: ChatMessage,
    isMine: Boolean,
    onResend: (() -> Unit)? = null,
    overlay: Boolean = false,
) {
    val palette = chatPalette()
    val metaColor = if (overlay) Color.White else palette.meta
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (message.editedAt != null) {
            Text(stringResource(L10nR.string.edited), color = metaColor, fontSize = 11.sp)
            Spacer(Modifier.width(4.dp))
        }
        Text(formatClock(message.timestamp), color = metaColor, fontSize = 11.sp)
        if (isMine) {
            Spacer(Modifier.width(3.dp))
            when {
                message.sendError -> Icon(
                    Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .clickable(enabled = onResend != null) { onResend?.invoke() },
                )
                // Сервер әлі растамаған (optimistic) — сағат.
                message.id == null -> Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = metaColor,
                    modifier = Modifier.size(13.dp),
                )
                else -> Icon(
                    imageVector = if (message.isRead) Icons.Filled.DoneAll else Icons.Filled.Done,
                    contentDescription = null,
                    tint = if (message.isRead) palette.readTick else metaColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * Мәтін + мета: соңғы жолда орын болса уақыт сол жолдың оң жағына
 * «кіріп» тұрады, болмаса — жаңа жолға түседі (WhatsApp мінезі).
 */
@Composable
private fun TextWithInlineMeta(
    text: String,
    color: Color,
    meta: @Composable () -> Unit,
) {
    val layoutHolder = remember { arrayOfNulls<TextLayoutResult>(1) }
    Layout(
        content = {
            Text(
                text = text,
                color = color,
                fontSize = 15.5.sp,
                lineHeight = 21.sp,
                onTextLayout = { layoutHolder[0] = it },
            )
            meta()
        },
    ) { measurables, constraints ->
        val textPlaceable = measurables[0].measure(constraints.copy(minWidth = 0))
        val metaPlaceable = measurables[1].measure(Constraints())
        val layout = layoutHolder[0]
        val gap = 10.dp.roundToPx()
        val lastLineWidth = layout?.let {
            val last = it.lineCount - 1
            (it.getLineRight(last) - it.getLineLeft(last)).toInt()
        } ?: textPlaceable.width
        val inlineWidth = lastLineWidth + gap + metaPlaceable.width
        val fitsInline = inlineWidth <= constraints.maxWidth
        val width = if (fitsInline) {
            max(textPlaceable.width, inlineWidth)
        } else {
            max(textPlaceable.width, metaPlaceable.width)
        }.coerceAtMost(constraints.maxWidth)
        val height = if (fitsInline) {
            max(textPlaceable.height, metaPlaceable.height)
        } else {
            textPlaceable.height + metaPlaceable.height
        }
        layout(width, height) {
            textPlaceable.place(0, 0)
            metaPlaceable.place(width - metaPlaceable.width, height - metaPlaceable.height)
        }
    }
}

/** Мәтіндік көпіршек: жауап-цитата, сілтеме превьюі, мәтін + inline мета. */
@Composable
fun TextBubble(
    message: ChatMessage,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    replyAuthor: String? = null,
    linkPreview: AnnouncementCard? = null,
    onOpenLinkPreview: ((Long) -> Unit)? = null,
    onResend: () -> Unit,
) {
    val palette = chatPalette()
    BubbleSurface(isMine = isMine, modifier = modifier, padding = 4.dp) {
        ReplyQuote(message = message, author = replyAuthor)
        if (linkPreview != null) {
            LinkPreviewCard(card = linkPreview, onClick = { onOpenLinkPreview?.invoke(linkPreview.id) })
        }
        Box(modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)) {
            val text = message.displayText()
            TextWithInlineMeta(
                text = text.ifEmpty { " " },
                color = palette.text,
                meta = { MessageMeta(message = message, isMine = isMine, onResend = onResend) },
            )
        }
    }
}

/** Жауап-цитата: сол жақта түсті жолақ + автор + қысқа мәтін. */
@Composable
private fun ReplyQuote(message: ChatMessage, author: String?) {
    val palette = chatPalette()
    val replyText = message.replyToMessage?.takeIf { it.isNotBlank() } ?: return
    val preview = replyText.replace(Regex("\\s*\\[[^]]+]"), "").trim().ifBlank { "…" }
    Row(
        modifier = Modifier
            .padding(bottom = 4.dp)
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.quoteBackground),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(palette.quoteBar),
        )
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
            if (!author.isNullOrBlank()) {
                Text(
                    author,
                    color = palette.quoteBar,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                preview,
                color = palette.meta,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Жарнама сілтемесінің превьюі (көпіршек ішінде, мәтіннің үстінде). */
@Composable
private fun LinkPreviewCard(card: AnnouncementCard, onClick: () -> Unit) {
    val palette = chatPalette()
    Column(
        modifier = Modifier
            .padding(bottom = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.quoteBackground)
            .clickable(onClick = onClick),
    ) {
        if (!card.mainImageUrl.isNullOrBlank()) {
            CachedImage(
                url = card.mainImageUrl,
                contentDescription = card.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
            )
        }
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                card.title,
                color = palette.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            card.price?.let {
                Text(
                    "${formatPriceValue(it)} ₸",
                    color = palette.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text("agroland.kz", color = palette.meta, fontSize = 12.sp)
        }
    }
}

/** Қоңырау хабарламасы — WhatsApp «Дауыстық қоңырау» карточкасы. */
@Composable
fun CallBubble(
    message: ChatMessage,
    isMine: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = chatPalette()
    val parsed = CallMessage.parse(message.message)
    val completed = stringResource(L10nR.string.call_msg_completed)
    val missed = stringResource(L10nR.string.call_msg_missed)
    val declined = stringResource(L10nR.string.call_msg_declined)
    val label = when (parsed?.first) {
        CallMessage.Status.COMPLETED -> completed
        CallMessage.Status.MISSED -> missed
        CallMessage.Status.DECLINED -> declined
        null -> message.displayText()
    }
    val problem = parsed?.first == CallMessage.Status.MISSED || parsed?.first == CallMessage.Status.DECLINED
    BubbleSurface(isMine = isMine, modifier = modifier.width(230.dp), padding = 8.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(palette.quoteBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when {
                        problem -> Icons.AutoMirrored.Filled.CallMissed
                        isMine -> Icons.AutoMirrored.Filled.CallMade
                        else -> Icons.AutoMirrored.Filled.CallReceived
                    },
                    contentDescription = null,
                    tint = if (problem) Color(0xFFE53935) else palette.accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = palette.text)
                Text(
                    parsed?.second?.takeIf { it > 0 }?.let { formatDuration(it) } ?: formatClock(message.timestamp),
                    color = palette.meta,
                    fontSize = 12.sp,
                )
            }
        }
        Box(Modifier.align(Alignment.End)) { MessageMeta(message = message, isMine = isMine) }
    }
}

/** Сурет көпіршегі: жиекте 3dp рамка, уақыт суреттің үстінде. */
@Composable
fun ImageBubble(
    url: String?,
    timestamp: Long,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    message: ChatMessage? = null,
    onOpenImage: (String) -> Unit,
) {
    BubbleSurface(isMine = isMine, modifier = modifier.width(250.dp), padding = 3.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 4.4f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = url != null) { url?.let(onOpenImage) },
        ) {
            if (url != null) {
                CachedImage(url = url, contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                MediaUnavailableBox(modifier = Modifier.fillMaxSize())
            }
            MediaMetaOverlay(message = message, timestamp = timestamp, isMine = isMine)
        }
    }
}

/** Бейне көпіршегі: алғашқы кадр + ойнату батырмасы. */
@Composable
fun VideoBubble(
    url: String?,
    timestamp: Long,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    message: ChatMessage? = null,
    onPlayVideo: (String) -> Unit,
) {
    val videoLabel = stringResource(L10nR.string.video_message)
    var thumbnail by remember(url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) {
        if (url == null) return@LaunchedEffect
        thumbnail = withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(url, mapOf())
                val bmp = retriever.frameAtTime
                retriever.release()
                bmp
            } catch (_: Exception) {
                null
            }
        }
    }
    BubbleSurface(isMine = isMine, modifier = modifier.width(250.dp), padding = 3.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1C1C1E))
                .clickable(enabled = url != null) { url?.let(onPlayVideo) },
        ) {
            if (thumbnail != null) {
                androidx.compose.foundation.Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = videoLabel,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                MediaUnavailableBox(modifier = Modifier.fillMaxSize(), icon = Icons.Outlined.PlayCircle)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = videoLabel, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            MediaMetaOverlay(message = message, timestamp = timestamp, isMine = isMine)
        }
    }
}

/** Медиа үстіндегі уақыт — төменгі градиентте ақ мәтін. */
@Composable
private fun BoxScope.MediaMetaOverlay(message: ChatMessage?, timestamp: Long, isMine: Boolean) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        if (message != null) {
            MessageMeta(message = message, isMine = isMine, overlay = true)
        } else {
            Text(formatClock(timestamp), color = Color.White, fontSize = 11.sp)
        }
    }
}

/**
 * Дауыстық хабарлама (WhatsApp): ойнату батырмасы + толқын-форм (сүйреп
 * жылжытуға болады) + ұзақтық; оң жақта микрофон белгісі.
 */
@Composable
fun VoiceBubble(
    audioUrl: String?,
    durationSec: Int?,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    message: ChatMessage? = null,
    onError: () -> Unit,
) {
    val palette = chatPalette()
    val player = remember(audioUrl) { MediaPlayer() }
    var prepared by remember(audioUrl) { mutableStateOf(false) }
    var preparing by remember(audioUrl) { mutableStateOf(false) }
    var playing by remember(audioUrl) { mutableStateOf(false) }
    var positionMs by remember(audioUrl) { mutableIntStateOf(0) }
    var durationMs by remember(audioUrl) { mutableIntStateOf((durationSec ?: 0) * 1000) }
    var seekPreview by remember(audioUrl) { mutableFloatStateOf(-1f) }

    // Серверден ұзақтық келмесе — аудио метадеректерінен оқимыз (ойнатпай).
    LaunchedEffect(audioUrl) {
        if (audioUrl == null || durationMs > 0) return@LaunchedEffect
        val ms = withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(audioUrl, mapOf())
                val value = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull()
                retriever.release()
                value
            } catch (_: Exception) {
                null
            }
        }
        if (ms != null && ms > 0 && durationMs == 0) durationMs = ms
    }

    DisposableEffect(audioUrl) {
        onDispose {
            try {
                player.release()
            } catch (_: Exception) {
            }
        }
    }

    fun toggle() {
        if (audioUrl == null || preparing) return
        try {
            if (playing) {
                player.pause()
                playing = false
                return
            }
            if (!prepared) {
                // Желіден асинхронды дайындау — UI қатпайды.
                preparing = true
                player.reset()
                player.setDataSource(audioUrl)
                player.setOnPreparedListener {
                    preparing = false
                    prepared = true
                    if (it.duration > 0) durationMs = it.duration
                    if (positionMs > 0) it.seekTo(positionMs)
                    it.start()
                    playing = true
                }
                player.setOnCompletionListener {
                    playing = false
                    positionMs = 0
                }
                player.setOnErrorListener { _, _, _ ->
                    preparing = false
                    playing = false
                    prepared = false
                    onError()
                    true
                }
                player.prepareAsync()
            } else {
                player.start()
                playing = true
            }
        } catch (_: Exception) {
            preparing = false
            playing = false
            onError()
        }
    }

    LaunchedEffect(playing, audioUrl) {
        while (playing) {
            try {
                positionMs = player.currentPosition
            } catch (_: Exception) {
                playing = false
            }
            delay(120)
        }
    }

    val progress = when {
        seekPreview >= 0f -> seekPreview
        durationMs > 0 -> (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        else -> 0f
    }

    BubbleSurface(isMine = isMine, modifier = modifier.width(270.dp), padding = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable { toggle() },
                contentAlignment = Alignment.Center,
            ) {
                if (preparing) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = palette.meta,
                    )
                } else {
                    Icon(
                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = palette.meta,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                WaveformBars(
                    seed = (audioUrl ?: "").hashCode(),
                    progress = progress,
                    playedColor = palette.readTick,
                    idleColor = palette.meta.copy(alpha = 0.45f),
                    onSeek = { fraction, done ->
                        if (!done) {
                            seekPreview = fraction
                        } else {
                            seekPreview = -1f
                            if (durationMs > 0) {
                                positionMs = (fraction * durationMs).toInt()
                                if (prepared) {
                                    try {
                                        player.seekTo(positionMs)
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        }
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val shownSec = if (playing || positionMs > 0) positionMs / 1000 else durationMs / 1000
                    Text(formatDuration(shownSec), fontSize = 11.sp, color = palette.meta)
                    Spacer(Modifier.weight(1f))
                    if (message != null) MessageMeta(message = message, isMine = isMine)
                }
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null, tint = palette.accent, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** Толқын-форм: 30 жолақ, ойнатылғаны боялған; түрту/сүйреу → орынға жылжу. */
@Composable
private fun WaveformBars(
    seed: Int,
    progress: Float,
    playedColor: Color,
    idleColor: Color,
    onSeek: (fraction: Float, done: Boolean) -> Unit,
) {
    val bars = 30
    val heights = remember(seed) {
        val rnd = java.util.Random(seed.toLong())
        List(bars) { 5 + rnd.nextInt(18) }
    }
    val lastFraction = remember { floatArrayOf(0f) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { onSeek(lastFraction[0], true) },
                    onDragCancel = { onSeek(lastFraction[0], true) },
                ) { change, _ ->
                    lastFraction[0] = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek(lastFraction[0], false)
                    change.consume()
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width).coerceIn(0f, 1f), true)
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        heights.forEachIndexed { i, h ->
            val played = progress > 0f && (i + 0.5f) / bars <= progress
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (played) playedColor else idleColor),
            )
        }
    }
}

/** Файл көпіршегі: құжат иконкасы + атауы + кеңейтпесі. */
@Composable
fun FileBubble(
    fileName: String?,
    url: String?,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    message: ChatMessage? = null,
    onOpen: () -> Unit,
) {
    val palette = chatPalette()
    val openFileLabel = stringResource(L10nR.string.open_file)
    val name = fileName?.takeIf { it.isNotBlank() } ?: openFileLabel
    val ext = name.substringAfterLast('.', "").uppercase().take(4)
    BubbleSurface(isMine = isMine, modifier = modifier.width(260.dp), padding = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(palette.quoteBackground)
                .clickable(onClick = onOpen)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (ext == "PDF") Color(0xFFE53935) else palette.accent),
                contentAlignment = Alignment.Center,
            ) {
                if (ext.isNotBlank()) {
                    Text(ext, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Outlined.Description, contentDescription = null, tint = Color.White)
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = palette.text,
                modifier = Modifier.weight(1f),
            )
        }
        if (message != null) {
            Box(Modifier.align(Alignment.End).padding(horizontal = 6.dp, vertical = 2.dp)) {
                MessageMeta(message = message, isMine = isMine)
            }
        }
    }
}

/** Локация: статикалық карта + атауы. */
@Composable
fun LocationBubble(
    message: ChatMessage,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    onOpenMap: () -> Unit,
) {
    val palette = chatPalette()
    val openInLabel = stringResource(L10nR.string.open_in2gis)
    val sharedLabel = stringResource(L10nR.string.shared_location)
    val lat = message.latitude
    val lng = message.longitude
    BubbleSurface(isMine = isMine, modifier = modifier.width(260.dp), padding = 3.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            if (lat != null && lng != null) {
                StaticMapView(
                    latitude = lat,
                    longitude = lng,
                    modifier = Modifier.fillMaxSize(),
                    onOpenMap = onOpenMap,
                )
            } else {
                MediaUnavailableBox(modifier = Modifier.fillMaxSize(), icon = Icons.Outlined.LocationOn)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenMap)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(
                text = message.locationName ?: message.displayText().takeIf { it.isNotBlank() && !it.first().isDigit() && it.first() != '-' } ?: sharedLabel,
                color = palette.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(openInLabel, color = palette.accent, fontSize = 12.sp, modifier = Modifier.weight(1f))
                MessageMeta(message = message, isMine = isMine)
            }
        }
    }
}

/** Жарнама карточкасы (хабарлама астында бөлек). */
@Composable
fun AnnouncementBanner(
    card: AnnouncementCard,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = chatPalette()
    Surface(
        modifier = modifier
            .widthIn(max = maxBubbleWidth())
            .width(270.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = palette.theirsBubble,
        shadowElevation = 0.6.dp,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CachedImage(
                url = card.mainImageUrl,
                contentDescription = card.title,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = card.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = palette.text,
                )
                card.price?.let { price ->
                    Text(
                        text = "${formatPriceValue(price)} ₸",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent,
                    )
                }
            }
        }
    }
}

/** Бағаны адам оқитын пішімге: 12 500 / 1 250 000,5. */
fun formatPriceValue(price: Double): String {
    val asLong = price.toLong().toDouble()
    return if (price == asLong) {
        "%,d".format(price.toLong()).replace(',', ' ')
    } else {
        "%,.1f".format(price).replace(',', ' ')
    }
}

/**
 * Жеткізуді сұрау карточкасы (Flutter DeliveryRequestCard):
 * сатушы жағында — Иә/Жоқ чиптері (жауап берілгенше).
 */
@Composable
fun DeliveryRequestCard(
    message: ChatMessage,
    answered: Boolean,
    isDealer: Boolean,
    modifier: Modifier = Modifier,
    onQuickReply: (yes: Boolean) -> Unit,
) {
    val palette = chatPalette()
    val primary = palette.accent
    val title = stringResource(L10nR.string.delivery_request_card_title)
    val yesLabel = stringResource(L10nR.string.delivery_quick_reply_yes)
    val noLabel = stringResource(L10nR.string.delivery_quick_reply_no)
    val address = message.deliveryAddress()
    BubbleSurface(isMine = !isDealer, modifier = modifier.width(270.dp), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Archive, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = palette.text)
        }
        address?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, fontSize = 14.sp, color = palette.meta)
        }
        if (isDealer && !answered) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeliveryChip(text = yesLabel, filled = true, enabled = true) { onQuickReply(true) }
                DeliveryChip(text = noLabel, filled = false, enabled = true) { onQuickReply(false) }
            }
        }
        Box(Modifier.align(Alignment.End).padding(top = 4.dp)) {
            MessageMeta(message = message, isMine = !isDealer)
        }
    }
}

/** Жеткізу жауабы карточкасы (сатып алушы жағында): Төлеу / Өзі алу. */
@Composable
fun DeliveryAnswerCard(
    requestMessage: ChatMessage,
    chosen: Boolean,
    modifier: Modifier = Modifier,
    onChoose: (choice: String) -> Unit,
) {
    val palette = chatPalette()
    val title = stringResource(L10nR.string.delivery_answer_card_title)
    val payLabel = stringResource(L10nR.string.pay_now)
    val pickupLabel = stringResource(L10nR.string.pickup)
    BubbleSurface(isMine = true, modifier = modifier.width(270.dp), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = palette.text)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeliveryChip(text = payLabel, filled = true, enabled = !chosen) { onChoose("pay") }
            DeliveryChip(text = pickupLabel, filled = false, enabled = !chosen) { onChoose("pickup") }
        }
    }
}

/** Чип-батырма (жеткізу ағыны). */
@Composable
private fun DeliveryChip(
    text: String,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            filled -> primary
            else -> Color.Transparent
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (filled) primary else primary.copy(alpha = 0.4f),
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                filled -> Color.White
                else -> primary
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Медиа жүктелмеген кездегі орын толғыш. */
@Composable
fun MediaUnavailableBox(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.BrokenImage,
) {
    val palette = chatPalette()
    Box(
        modifier = modifier.background(palette.quoteBackground),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = palette.meta, modifier = Modifier.size(40.dp))
    }
}
