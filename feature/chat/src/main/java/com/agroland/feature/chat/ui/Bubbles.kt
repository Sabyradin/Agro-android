package com.agroland.feature.chat.ui

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.chat.data.ChatMessage
import com.agroland.feature.chat.domain.CallMessage
import kotlin.math.abs
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

/**
 * Мәтіндік көпіршек (Flutter TextBubbleMessage паритеті, Material 3 рухында):
 * жауап-пилюля, «өңделді» белгісі, уақыт + ✓/✓✓, қате → қайта жіберу.
 */
@Composable
fun TextBubble(
    message: ChatMessage,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    onResend: () -> Unit,
) {
    val colors = extendedColors()
    val editedLabel = stringResource(L10nR.string.edited)
    BubbleContainer(
        isMine = isMine,
        modifier = modifier.width(240.dp),
    ) {
        ReplyPill(message, isMine)
        val text = message.displayText()
        if (text.isNotEmpty()) {
            Text(
                text = text,
                color = if (isMine) Color.White else colors.primaryText,
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )
        }
        Spacer(Modifier.height(3.dp))
        Row(
            modifier = Modifier.align(Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (message.editedAt != null) {
                Text(
                    editedLabel,
                    color = if (isMine) Color.White.copy(0.7f) else colors.secondaryText,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.width(4.dp))
            }
            if (message.sendError) {
                Icon(
                    Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFFF6B6B),
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onResend),
                )
                Spacer(Modifier.width(3.dp))
            } else if (isMine) {
                Icon(
                    imageVector = if (message.isRead) Icons.Filled.DoneAll else Icons.Filled.Done,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = if (message.isRead) 1f else 0.7f),
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(3.dp))
            }
            Text(
                formatClock(message.timestamp),
                color = if (isMine) Color.White.copy(0.7f) else colors.secondaryText,
                fontSize = 11.sp,
            )
        }
    }
}

/** Қоңырау хабарламасы — тарихтағы карточка (Phase 13 жібереді, мұнда рендер). */
@Composable
fun CallBubble(
    message: ChatMessage,
    isMine: Boolean,
    modifier: Modifier = Modifier,
) {
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
    val accent = when (parsed?.first) {
        CallMessage.Status.MISSED -> Color(0xFFE53935)
        CallMessage.Status.DECLINED -> Color(0xFF8D8D93)
        else -> MaterialTheme.colorScheme.primary
    }
    val colors = extendedColors()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (isMine) MaterialTheme.colorScheme.primary else colors.card,
        contentColor = if (isMine) Color.White else colors.primaryText,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Phone,
                contentDescription = null,
                tint = if (isMine) Color.White else accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                parsed?.second?.takeIf { it > 0 }?.let { sec ->
                    Text(
                        formatDuration(sec),
                        color = if (isMine) Color.White.copy(0.7f) else colors.secondaryText,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                formatClock(message.timestamp),
                color = if (isMine) Color.White.copy(0.7f) else colors.secondaryText,
                fontSize = 11.sp,
            )
        }
    }
}

/**
 * Сурет көпіршегі — толық қанат медиа, бұрыштары 18dp, уақыт жабындысы.
 * Тап → толық экранды қарау (ChatRoomPage-тен келетін onOpenImage).
 */
@Composable
fun ImageBubble(
    url: String?,
    timestamp: Long,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    onOpenImage: (String) -> Unit,
) {
    val colors = extendedColors()
    Box(
        modifier = modifier
            .width(232.dp)
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.card)
            .clickable(enabled = url != null) { url?.let(onOpenImage) },
    ) {
        if (url != null) {
            CachedImage(
                url = url,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            MediaUnavailableBox()
        }
        TimeOverlay(timestamp, Alignment.BottomEnd)
    }
}

/** Бейне көпіршегі: MediaMetadataRetriever thumbnail + ойнату батырмасы. */
@Composable
fun VideoBubble(
    url: String?,
    timestamp: Long,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    onPlayVideo: (String) -> Unit,
) {
    val colors = extendedColors()
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
    Box(
        modifier = modifier
            .width(232.dp)
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1C1C1E))
            .clickable(enabled = url != null) { url?.let(onPlayVideo) },
    ) {
        if (thumbnail != null) {
            androidx.compose.foundation.Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = videoLabel,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            MediaUnavailableBox(icon = Icons.Outlined.PlayCircle)
        }
        // Ойнату батырмасы — жарқын шыны эффекті.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f))
                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = videoLabel,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
            )
        }
        TimeOverlay(timestamp, Alignment.BottomEnd)
    }
}

/** Дауыстық хабарлама — ойнату/тоқтату + толқын-форм + ұзақтық. */
@Composable
fun VoiceBubble(
    audioUrl: String?,
    durationSec: Int?,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    onError: () -> Unit,
) {
    val context = LocalContext.current
    val colors = extendedColors()
    val player = remember(audioUrl) { MediaPlayer() }
    var playing by remember(audioUrl) { mutableStateOf(false) }
    var position by remember(audioUrl) { mutableIntStateOf(0) }
    var duration by remember(audioUrl) { mutableIntStateOf(durationSec ?: 0) }

    DisposableEffect(audioUrl) {
        onDispose {
            try {
                player.release()
            } catch (_: Exception) {
            }
        }
    }

    fun toggle() {
        if (audioUrl == null) return
        try {
            if (!playing) {
                if (player.isPlaying.not() && position == 0) {
                    player.reset()
                    player.setDataSource(audioUrl)
                    player.prepare()
                    if (player.duration > 0) duration = player.duration / 1000
                }
                player.start()
                playing = true
            } else {
                player.pause()
                position = player.currentPosition / 1000
                playing = false
            }
        } catch (_: Exception) {
            playing = false
            onError()
        }
    }

    // Позиция тикері — ойнату кезінде 200мс сайын.
    LaunchedEffect(playing, audioUrl) {
        while (playing) {
            try {
                position = player.currentPosition / 1000
                if (!player.isPlaying) {
                    playing = false
                    position = 0
                }
            } catch (_: Exception) {
                playing = false
            }
            delay(200)
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (isMine) MaterialTheme.colorScheme.primary else colors.card,
        contentColor = if (isMine) Color.White else colors.primaryText,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isMine) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable { toggle() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (playing) Icons.Outlined.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = if (isMine) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            // Толқын-форм — хабарлама id-інен детерминистік «биоритм».
            WaveformBars(
                progress = if (duration > 0) position.toFloat() / duration else 0f,
                playedColor = if (isMine) Color.White else MaterialTheme.colorScheme.primary,
                idleColor = if (isMine) Color.White.copy(alpha = 0.45f) else colors.grey,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                formatDuration(if (playing || position > 0) (duration - position).coerceAtLeast(0) else duration),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** Толқын-форм: 24 тік жолақ, ойнатылғаны боялған. */
@Composable
private fun WaveformBars(progress: Float, playedColor: Color, idleColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(24) { i ->
            // Детерминистік псевдо-биоритм (синус + модуль).
            val heightDp = 6 + ((i * 7) % 17)
            val played = (i / 24f) <= progress && progress > 0f
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(heightDp.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (played) playedColor else idleColor),
            )
        }
    }
}

/** Файл көпіршегі: icon + атауы + «ашу» әрекеті. */
@Composable
fun FileBubble(
    fileName: String?,
    url: String?,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
) {
    val colors = extendedColors()
    val openFileLabel = stringResource(L10nR.string.open_file)
    Surface(
        modifier = modifier.clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        color = if (isMine) MaterialTheme.colorScheme.primary else colors.card,
        contentColor = if (isMine) Color.White else colors.primaryText,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .width(232.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isMine) Color.White.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.InsertDriveFile,
                    contentDescription = null,
                    tint = if (isMine) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = fileName ?: openFileLabel,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
                Text(
                    openFileLabel,
                    fontSize = 12.sp,
                    color = if (isMine) Color.White.copy(0.7f) else colors.secondaryText,
                )
            }
        }
    }
}

/** Локация карточкасы: статикалық карта + атау + 2GIS ашу. */
@Composable
fun LocationBubble(
    message: ChatMessage,
    isMine: Boolean,
    modifier: Modifier = Modifier,
    onOpenMap: () -> Unit,
) {
    val colors = extendedColors()
    val openInLabel = stringResource(L10nR.string.open_in2gis)
    val sharedLabel = stringResource(L10nR.string.shared_location)
    val lat = message.latitude
    val lng = message.longitude
    Column(
        modifier = modifier
            .width(240.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isMine) MaterialTheme.colorScheme.primary else colors.card),
    ) {
        if (lat != null && lng != null) {
            StaticMapView(
                latitude = lat,
                longitude = lng,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                onOpenMap = onOpenMap,
            )
        }
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = if (isMine) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = message.locationName ?: message.displayText().ifBlank { sharedLabel },
                    color = if (isMine) Color.White else colors.primaryText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    openInLabel,
                    color = if (isMine) Color.White.copy(0.7f) else colors.secondaryText,
                    fontSize = 12.sp,
                )
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = openInLabel,
                tint = if (isMine) Color.White.copy(0.7f) else colors.secondaryText,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Жарнама карточкасы (ChatRoom-контекст): сурет + атау + баға → түйіндеу. */
@Composable
fun AnnouncementBanner(
    card: AnnouncementCard,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = extendedColors()
    Surface(
        modifier = modifier
            .width(260.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = colors.card,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.grey),
            ) {
                CachedImage(
                    url = card.mainImageUrl,
                    contentDescription = card.title,
                    modifier = Modifier.size(56.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = card.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.primaryText,
                )
                card.price?.let { price ->
                    Text(
                        text = formatPriceValue(price),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.secondaryText,
                modifier = Modifier.size(18.dp),
            )
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
 * primary 8% фон + 30% жиек + archivebox + адрес.
 * Сатушы жағында — Иә/Жоқ чиптері (жауап берілгенше).
 */
@Composable
fun DeliveryRequestCard(
    message: ChatMessage,
    answered: Boolean,
    isDealer: Boolean,
    modifier: Modifier = Modifier,
    onQuickReply: (yes: Boolean) -> Unit,
) {
    val colors = extendedColors()
    val primary = MaterialTheme.colorScheme.primary
    val title = stringResource(L10nR.string.delivery_request_card_title)
    val yesLabel = stringResource(L10nR.string.delivery_quick_reply_yes)
    val noLabel = stringResource(L10nR.string.delivery_quick_reply_no)
    val address = message.deliveryAddress()
    Surface(
        modifier = modifier.width(260.dp),
        shape = RoundedCornerShape(18.dp),
        color = primary.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, primary.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Archive,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.primaryText,
                )
            }
            address?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it,
                    fontSize = 13.sp,
                    color = colors.secondaryText,
                )
            }
            if (isDealer && !answered) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeliveryChip(
                        text = yesLabel,
                        filled = true,
                        enabled = true,
                    ) { onQuickReply(true) }
                    DeliveryChip(
                        text = noLabel,
                        filled = false,
                        enabled = true,
                    ) { onQuickReply(false) }
                }
            }
            if (answered) {
                Spacer(Modifier.height(6.dp))
                Text(
                    yesLabel,
                    fontSize = 12.sp,
                    color = colors.secondaryText,
                )
            }
        }
    }
}

/**
 * Жеткізу жауабы карточкасы (сатып алушы жағында):
 * сатушы «иә» дегеннен кейін — Төлеу/Өзі алу чиптері.
 */
@Composable
fun DeliveryAnswerCard(
    requestMessage: ChatMessage,
    chosen: Boolean,
    modifier: Modifier = Modifier,
    onChoose: (choice: String) -> Unit,
) {
    val colors = extendedColors()
    val primary = MaterialTheme.colorScheme.primary
    val title = stringResource(L10nR.string.delivery_answer_card_title)
    val payLabel = stringResource(L10nR.string.pay_now)
    val pickupLabel = stringResource(L10nR.string.pickup)
    Surface(
        modifier = modifier.width(260.dp),
        shape = RoundedCornerShape(18.dp),
        color = primary.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, primary.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.LocalShipping,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.primaryText,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeliveryChip(
                    text = payLabel,
                    filled = true,
                    enabled = !chosen,
                ) { onChoose("pay") }
                DeliveryChip(
                    text = pickupLabel,
                    filled = false,
                    enabled = !chosen,
                ) { onChoose("pickup") }
            }
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

/** Жауап-пилюля көпіршектің ішінде (reply_to). */
@Composable
private fun ReplyPill(message: ChatMessage, isMine: Boolean) {
    val replyText = message.replyToMessage ?: return
    val preview = replyText.replace(Regex("\\s*\\[[^]]+]"), "").trim().ifBlank { "…" }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isMine) Color.White.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            preview,
            color = if (isMine) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Уақыт жабындысы (медиа көпіршектері үшін) — align сырттан келеді. */
@Composable
private fun BoxScope.TimeOverlay(timestamp: Long, alignment: Alignment) {
    Text(
        formatClock(timestamp),
        color = Color.White,
        fontSize = 11.sp,
        modifier = Modifier
            .align(alignment)
            .padding(8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Медиа жүктелмеген кездегі орын толғыш (Flutter mediaUnavailableBox). */
@Composable
fun MediaUnavailableBox(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.BrokenImage,
) {
    val colors = extendedColors()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.grey.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = colors.secondaryText,
            modifier = Modifier.size(40.dp),
        )
    }
}

/** Көпіршек ыдысы — мәтін үшін ( mine — primary / theirs — card ). */
@Composable
private fun BubbleContainer(
    isMine: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val colors = extendedColors()
    Column(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isMine) 18.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 18.dp,
                ),
            )
            .background(if (isMine) MaterialTheme.colorScheme.primary else colors.card)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        content = content,
    )
}