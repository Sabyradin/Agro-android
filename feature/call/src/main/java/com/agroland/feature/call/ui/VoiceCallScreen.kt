package com.agroland.feature.call.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.CachedImage
import com.agroland.feature.call.R
import com.agroland.feature.call.domain.CallEndReason
import com.agroland.feature.call.domain.CallPhase
import com.agroland.feature.call.domain.VoiceCallState
import kotlin.math.sin
import kotlinx.coroutines.delay

/**
 * Дауыстық қоңырау экраны — app үстіндегі толықэкран overlay (Flutter
 * VoiceCallScreen + VoiceCallHost, 1:1 мінез-құлық; визуал — iOS-тан
 * әдемі болу директивасы).
 *
 * Рингтон — res/raw/ringtone.mp3 (Flutter asset-і), INCOMING кезінде
 * қайталанып ойнайды + вибрация. Terminal себебі 2200мс көрсетіледі.
 */
@Composable
fun VoiceCallScreen(
    state: VoiceCallState,
    onAccept: (micGranted: Boolean) -> Unit,
    onDecline: () -> Unit,
    onHangup: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onClearTerminal: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Экран қоңырау барысында өшпейді.
    val keepScreenOn = state.phase == CallPhase.INCOMING ||
        state.phase == CallPhase.ACTIVE ||
        state.phase == CallPhase.RECONNECTING
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    // Рингтон: INCOMING бойы қайталанады (Flutter audioplayers loop).
    val ringtonePlayer = remember {
        try {
            MediaPlayer.create(context, R.raw.ringtone)?.apply { isLooping = true }
        } catch (_: Throwable) {
            null
        }
    }
    DisposableEffect(state.phase == CallPhase.INCOMING) {
        val player = ringtonePlayer
        if (state.phase == CallPhase.INCOMING && player != null) {
            try {
                player.start()
            } catch (_: Throwable) {
                // Дыбыс ойнамаса да, қоңырау экраны жұмысын жалғастырады.
            }
        }
        onDispose {
            try {
                if (player?.isPlaying == true) player.stop()
                player?.seekTo(0)
            } catch (_: Throwable) {
                // Плеер әлдеқашан босатылған.
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            try {
                ringtonePlayer?.release()
            } catch (_: Throwable) {
                // ok
            }
        }
    }

    // Вибрация — INCOMING бойы үлгілі, қайталанатын толқын.
    DisposableEffect(state.phase == CallPhase.INCOMING) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (state.phase == CallPhase.INCOMING && vibrator?.hasVibrator() == true) {
            try {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 900), 1))
            } catch (_: Throwable) {
                // Вибрация қолжетімсіз.
            }
        }
        onDispose {
            // Рұқсат/сервис қолжетімсіз болса да қоңырау экраны құламауы керек.
            try {
                vibrator?.cancel()
            } catch (_: Throwable) {
                // ok
            }
        }
    }

    // Terminal себебі 2200мс көрсетіліп тазартылады (Flutter паритеті).
    LaunchedEffect(state.terminalReason) {
        if (state.terminalReason != null) {
            delay(2200)
            onClearTerminal()
        }
    }

    // Артқа кнопка: кіріс қоңырауда — бас тарту, қалғанда — аяқтау.
    BackHandler(enabled = state.phase != CallPhase.IDLE) {
        if (state.phase == CallPhase.INCOMING) onDecline() else onHangup()
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> onAccept(granted) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0E2417),
                        Color(0xFF07130C),
                        Color(0xFF030A06),
                    ),
                ),
            ),
    ) {
        // Аватардың артындағы жасыл нұр (radial glow) — «тыныс алатын» градиент.
        val glow by rememberInfiniteTransition(label = "glow").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "glowAlpha",
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF147F26).copy(alpha = 0.30f + 0.10f * glow),
                            Color(0xFF7AB30E).copy(alpha = 0.10f + 0.05f * glow),
                            Color.Transparent,
                        ),
                        radius = 460f,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))

            AvatarWithPulse(state, pulsing = state.phase == CallPhase.INCOMING)

            Spacer(Modifier.height(36.dp))

            val name = state.peerName?.takeIf { it.isNotBlank() }
                ?: stringResource(L10nR.string.app_name)
            Text(
                text = name,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )

            Spacer(Modifier.height(10.dp))

            CallStatusText(state)

            Spacer(Modifier.weight(1f))

            when {
                state.terminalReason != null -> Unit

                state.phase == CallPhase.INCOMING -> IncomingControls(
                    onAccept = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            onAccept(true)
                        } else {
                            // Экран бірінші көрінеді, рұқсат диалогы кейін
                            // (Flutter invite-flow паритеті).
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onDecline = onDecline,
                )

                state.phase == CallPhase.OUTGOING ||
                    state.phase == CallPhase.CONNECTING ||
                    state.phase == CallPhase.WAITING_ANSWER ||
                    state.phase == CallPhase.RECONNECTING -> ConnectingControls(
                    onHangup = onHangup,
                )

                state.phase == CallPhase.ACTIVE -> ActiveControls(
                    state = state,
                    onToggleMute = onToggleMute,
                    onHangup = onHangup,
                    onToggleSpeaker = onToggleSpeaker,
                )

                else -> Unit
            }
        }
    }
}

// ── Аватар + пульс сақиналары ────────────────────────────────────────

@Composable
private fun AvatarWithPulse(state: VoiceCallState, pulsing: Boolean) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = LinearEasing),
        ),
        label = "pulseProgress",
    )
    val avatarSize = 132.dp

    Box(contentAlignment = Alignment.Center) {
        if (pulsing) {
            // Екі сақина, фазасы жарты уақытқа ығысқан — «дем алатын» әсер.
            listOf(pulse, (pulse + 0.5f) % 1f).forEach { p ->
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .scale(1f + 0.62f * p)
                        .clip(CircleShape)
                        .border(2.dp, Color.White.copy(alpha = (1f - p) * 0.38f), CircleShape),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .border(3.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                .background(Color(0xFF14351F)),
            contentAlignment = Alignment.Center,
        ) {
            val initials = state.peerName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.split(Regex("\\s+"))
                ?.take(2)
                ?.joinToString("") { it.take(1).uppercase() }
            if (state.peerAvatarUrl == null) {
                if (initials != null) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                CachedImage(
                    url = state.peerAvatarUrl,
                    contentDescription = state.peerName,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

// ── Статус жолы ──────────────────────────────────────────────────────

@Composable
private fun CallStatusText(state: VoiceCallState) {
    val statusRes = when (state.terminalReason) {
        CallEndReason.DECLINED -> L10nR.string.call_msg_declined
        CallEndReason.NO_ANSWER -> L10nR.string.call_msg_no_answer
        CallEndReason.OFFLINE -> L10nR.string.call_peer_offline
        CallEndReason.MIC_DENIED -> L10nR.string.call_mic_permission_denied
        CallEndReason.CONNECTION_ERROR -> L10nR.string.call_connection_error
        null -> when (state.phase) {
            CallPhase.INCOMING -> L10nR.string.call_incoming_title
            CallPhase.OUTGOING -> L10nR.string.call_calling
            CallPhase.CONNECTING, CallPhase.WAITING_ANSWER -> L10nR.string.call_connecting
            CallPhase.RECONNECTING -> L10nR.string.call_reconnecting
            CallPhase.IDLE -> L10nR.string.call_ended
            CallPhase.ACTIVE -> L10nR.string.call_ended
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.phase == CallPhase.CONNECTING || state.phase == CallPhase.WAITING_ANSWER) {
            CircularProgressIndicator(
                color = Color(0xFF7AB30E),
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(10.dp))
        }
        if (state.phase == CallPhase.ACTIVE && state.terminalReason == null) {
            Text(
                text = formatDuration(state.durationSeconds),
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
            )
        } else {
            Text(
                text = stringResource(statusRes),
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

// ── Фаза батырмалары ─────────────────────────────────────────────────

@Composable
private fun IncomingControls(onAccept: () -> Unit, onDecline: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        CallCircleButton(
            label = stringResource(L10nR.string.call_decline),
            icon = Icons.Filled.PhoneDisabled,
            container = Color(0xFFE23B3B),
            onClick = onDecline,
        )
        CallCircleButton(
            label = stringResource(L10nR.string.call_accept),
            icon = Icons.Filled.Phone,
            container = Color(0xFF147F26),
            onClick = onAccept,
            pulsing = true,
        )
    }
}

@Composable
private fun ConnectingControls(onHangup: () -> Unit) {
    CallCircleButton(
        label = stringResource(L10nR.string.call_hangup),
        icon = Icons.Filled.CallEnd,
        container = Color(0xFFE23B3B),
        onClick = onHangup,
    )
}

@Composable
private fun ActiveControls(
    state: VoiceCallState,
    onToggleMute: () -> Unit,
    onHangup: () -> Unit,
    onToggleSpeaker: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CallCircleButton(
            label = stringResource(
                if (state.muted) L10nR.string.call_unmute else L10nR.string.call_mute,
            ),
            icon = if (state.muted) Icons.Filled.MicOff else Icons.Filled.Mic,
            container = if (state.muted) Color.White else Color.White.copy(alpha = 0.14f),
            content = if (state.muted) Color(0xFF0E2417) else Color.White,
            onClick = onToggleMute,
        )
        CallCircleButton(
            label = stringResource(L10nR.string.call_hangup),
            icon = Icons.Filled.CallEnd,
            container = Color(0xFFE23B3B),
            onClick = onHangup,
        )
        CallCircleButton(
            label = stringResource(L10nR.string.call_speaker),
            icon = if (state.speakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
            container = if (state.speakerOn) Color(0xFFFFCC00) else Color.White.copy(alpha = 0.14f),
            content = if (state.speakerOn) Color(0xFF1C1C1E) else Color.White,
            onClick = onToggleSpeaker,
        )
    }
}

// ── Дөңгелек батырма ──────────────────────────────────────────────────

@Composable
private fun CallCircleButton(
    label: String,
    icon: ImageVector,
    container: Color,
    onClick: () -> Unit,
    content: Color = Color.White,
    size: Int = 68,
    pulsing: Boolean = false,
) {
    // Accept батырмасының жұмсақ пульсациясы (тек INCOMING қабылдау).
    val pulseScale by rememberInfiniteTransition(label = "btnPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "btnPulseProgress",
    )
    val scale = if (pulsing) 1f + 0.045f * pulseScale else 1f

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(container)
                .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}