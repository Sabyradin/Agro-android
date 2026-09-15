package com.agroland.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Түбірлік тема. Тема режимі (system/light/dark) SettingsDataStore-тан келеді —
 * экран деңгейінде шешіліп, [darkTheme] параметріне беріледі.
 */
@Composable
fun AgroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) darkScheme else lightScheme
    val extended = extendedColorsFor(darkTheme)

    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AgroTypography,
            content = content,
        )
    }
}