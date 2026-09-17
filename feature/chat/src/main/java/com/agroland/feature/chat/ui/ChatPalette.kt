package com.agroland.feature.chat.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Чат бөлмесінің түстері — WhatsApp үлгісі: жылы сұр-беж фон, өз хабарламаң
 * ашық жасыл, қарсы жақтыкі ақ; қараңғы тақырыпта — WhatsApp dark реңктері.
 * Барлық мәтін қою (ақ-жасыл көпіршекте ақ мәтін оқылмайды).
 */
@Immutable
data class ChatPalette(
    val background: Color,
    val mineBubble: Color,
    val theirsBubble: Color,
    val text: Color,
    val meta: Color,
    val readTick: Color,
    val accent: Color,
    val quoteBar: Color,
    val quoteBackground: Color,
    val inputBar: Color,
    val inputField: Color,
    val dayChip: Color,
    val isDark: Boolean,
)

@Composable
fun chatPalette(): ChatPalette {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    return if (dark) {
        ChatPalette(
            background = Color(0xFF0B141A),
            mineBubble = Color(0xFF005C4B),
            theirsBubble = Color(0xFF202C33),
            text = Color(0xFFE9EDEF),
            meta = Color(0xFF8696A0),
            readTick = Color(0xFF53BDEB),
            accent = Color(0xFF21C063),
            quoteBar = Color(0xFF21C063),
            quoteBackground = Color(0x33000000),
            inputBar = Color(0xFF1F2C34),
            inputField = Color(0xFF2A3942),
            dayChip = Color(0xFF182229),
            isDark = true,
        )
    } else {
        ChatPalette(
            background = Color(0xFFEFEAE2),
            mineBubble = Color(0xFFD9FDD3),
            theirsBubble = Color(0xFFFFFFFF),
            text = Color(0xFF111B21),
            meta = Color(0xFF667781),
            readTick = Color(0xFF53BDEB),
            accent = scheme.primary,
            quoteBar = scheme.primary,
            quoteBackground = Color(0x0F000000),
            inputBar = Color(0xFFF0F2F5),
            inputField = Color(0xFFFFFFFF),
            dayChip = Color(0xFFFFFFFF),
            isDark = false,
        )
    }
}
