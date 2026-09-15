package com.agroland.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Түс токендері — Flutter AppColors-пен сайткес (spec §5).
 * Қосымша токендер (primaryText/secondaryText/divider/card/grey/backgroundLight)
 * ColorScheme-те жоқ → [ExtendedColors] арқылы беріледі.
 */
object AgroColors {
    // Light
    val primary = Color(0xFF147F26)
    val primaryLight = Color(0xFF7AB30E)
    val accent = Color(0xFFFFCC00)
    val errorRed = Color(0xFFC20B0B)
    val text1 = Color(0xFF222222)
    val text2 = Color(0xFF939393)
    val background = Color(0xFFF9F9F9)
    val divider = Color(0xFFEDEDED)
    val backgroundLight = Color(0xFFDCECDF)
    val grey = Color(0xFFEFEFEF)
    val cardLight = Color(0xFFFFFFFF)
    val white = Color(0xFFFFFFFF)
    val black = Color(0xFF000000)

    // Dark
    val darkBackground = Color(0xFF1C1C1E)
    val darkSurface = Color(0xFF2C2C2E)
    val darkCard = Color(0xFF3A3A3C)
    val darkText1 = Color(0xFFFFFFFF)
    val darkText2 = Color(0xFFAEAEB2)
    val darkDivider = Color(0xFF3A3A3C)
    val darkGrey = Color(0xFF2C2C2E)
    val darkBackgroundLight = Color(0xFF2C3A2E)
}

/** ColorScheme-ке сыймайтын қосымша токендер. */
@Immutable
data class ExtendedColors(
    val primaryText: Color,
    val secondaryText: Color,
    val primaryLight: Color,
    val accent: Color,
    val divider: Color,
    val card: Color,
    val grey: Color,
    val backgroundLight: Color,
    val white: Color,
    val black: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        primaryText = AgroColors.text1,
        secondaryText = AgroColors.text2,
        primaryLight = AgroColors.primaryLight,
        accent = AgroColors.accent,
        divider = AgroColors.divider,
        card = AgroColors.cardLight,
        grey = AgroColors.grey,
        backgroundLight = AgroColors.backgroundLight,
        white = AgroColors.white,
        black = AgroColors.black,
    )
}

/** Компоненттер ішінен қолжетімділік: `val ext = extendedColors()`. */
@androidx.compose.runtime.Composable
fun extendedColors(): ExtendedColors = LocalExtendedColors.current

val lightScheme = lightColorScheme(
    primary = AgroColors.primary,
    onPrimary = AgroColors.white,
    primaryContainer = AgroColors.backgroundLight,
    onPrimaryContainer = AgroColors.primary,
    secondary = AgroColors.primaryLight,
    onSecondary = AgroColors.white,
    tertiary = AgroColors.accent,
    onTertiary = AgroColors.black,
    background = AgroColors.background,
    onBackground = AgroColors.text1,
    surface = AgroColors.white,
    onSurface = AgroColors.text1,
    surfaceVariant = AgroColors.grey,
    onSurfaceVariant = AgroColors.text2,
    outline = AgroColors.text2,
    outlineVariant = AgroColors.divider,
    error = AgroColors.errorRed,
    onError = AgroColors.white,
)

val darkScheme = darkColorScheme(
    primary = AgroColors.primary,
    onPrimary = AgroColors.white,
    primaryContainer = AgroColors.darkBackgroundLight,
    onPrimaryContainer = AgroColors.white,
    secondary = AgroColors.primaryLight,
    onSecondary = AgroColors.black,
    tertiary = AgroColors.accent,
    onTertiary = AgroColors.black,
    background = AgroColors.darkBackground,
    onBackground = AgroColors.darkText1,
    surface = AgroColors.darkSurface,
    onSurface = AgroColors.darkText1,
    surfaceVariant = AgroColors.darkGrey,
    onSurfaceVariant = AgroColors.darkText2,
    outline = AgroColors.darkText2,
    outlineVariant = AgroColors.darkDivider,
    error = AgroColors.errorRed,
    onError = AgroColors.white,
)

/** Ағымдағы темаға сай ExtendedColors. */
internal fun extendedColorsFor(dark: Boolean): ExtendedColors =
    if (dark) {
        ExtendedColors(
            primaryText = AgroColors.darkText1,
            secondaryText = AgroColors.darkText2,
            primaryLight = AgroColors.primaryLight,
            accent = AgroColors.accent,
            divider = AgroColors.darkDivider,
            card = AgroColors.darkCard,
            grey = AgroColors.darkGrey,
            backgroundLight = AgroColors.darkBackgroundLight,
            white = AgroColors.white,
            black = AgroColors.black,
        )
    } else {
        ExtendedColors(
            primaryText = AgroColors.text1,
            secondaryText = AgroColors.text2,
            primaryLight = AgroColors.primaryLight,
            accent = AgroColors.accent,
            divider = AgroColors.divider,
            card = AgroColors.cardLight,
            grey = AgroColors.grey,
            backgroundLight = AgroColors.backgroundLight,
            white = AgroColors.white,
            black = AgroColors.black,
        )
    }