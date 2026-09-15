package com.agroland.core.common.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Қосымша деңгейіндегі персистентті баптаулар (DataStore).
 * Flutter-дегі SharedPreferences кілттерінің баламасы:
 *  - APP_LOCALE_KEY        → locale
 *  - APP_THEME_MODE_KEY    → theme_mode
 *  - APP_REGION_KEY        → app_region_country_id / app_region_city_id
 *  - бірінші іске қосу      → language_selected
 */
class SettingsDataStore(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val LOCALE = stringPreferencesKey("locale")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE_SELECTED = booleanPreferencesKey("language_selected")
        val APP_REGION_COUNTRY_ID = intPreferencesKey("app_region_country_id")
        val APP_REGION_CITY_ID = intPreferencesKey("app_region_city_id")
    }

    /** Тіл тегі («kk»/«ru»/«en»/«zh») немесе null (таңдалмаған → Languages бетіне redirect). */
    val localeTag: Flow<String?> = dataStore.data.map { it[Keys.LOCALE] }

    /** AppLocale ретінде; null → әдепкі KK (экран әрқашан осы арқылы көрсетеді). */
    val locale: Flow<String> = dataStore.data.map { it[Keys.LOCALE] ?: "kk" }

    val themeMode: Flow<ThemeMode> =
        dataStore.data.map { ThemeMode.fromPersisted(it[Keys.THEME_MODE]) }

    /** Тіл бірінші рет таңдалды ма (splash redirect үшін). */
    val languageSelected: Flow<Boolean> =
        dataStore.data.map { it[Keys.LANGUAGE_SELECTED] == true }

    val appRegionCountryId: Flow<Int?> = dataStore.data.map { it[Keys.APP_REGION_COUNTRY_ID] }

    val appRegionCityId: Flow<Int?> = dataStore.data.map { it[Keys.APP_REGION_CITY_ID] }

    suspend fun setLocale(tag: String) {
        dataStore.edit { prefs ->
            prefs[Keys.LOCALE] = tag
            prefs[Keys.LANGUAGE_SELECTED] = true
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.persisted }
    }

    suspend fun setAppRegion(countryId: Int?, cityId: Int?) {
        dataStore.edit { prefs ->
            if (countryId != null) prefs[Keys.APP_REGION_COUNTRY_ID] = countryId
            if (cityId != null) prefs[Keys.APP_REGION_CITY_ID] = cityId
        }
    }

    /** Бір реттік оқу — старттық шешімдер үшін. */
    suspend fun localeTagOnce(): String? = localeTag.first()
    suspend fun languageSelectedOnce(): Boolean = languageSelected.first()
    suspend fun appRegionCountryIdOnce(): Int? = appRegionCountryId.first()
}