package com.agroland.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.common.settings.SettingsDataStore
import com.agroland.core.common.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App деңгейіндегі ViewModel — MainActivity тема/тіл ағындарын осыдан алады,
 * LanguagePage таңдауын осы арқылы жазады.
 */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val settings: SettingsDataStore,
) : ViewModel() {

    /** Тема режимі — AgroTheme(darkTheme = ...) шешуі үшін. */
    val themeMode = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    /** Таңдалған тіл тегі — AppCompatDelegate.setApplicationLocales үшін. */
    val localeTag = settings.localeTag
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun selectLocale(tag: String) {
        viewModelScope.launch { settings.setLocale(tag) }
    }
}