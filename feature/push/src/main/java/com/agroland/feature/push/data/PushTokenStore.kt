package com.agroland.feature.push.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Push-талаптар күйі (Flutter PUSH_TOKEN_KEY prefs баламасы):
 * соңғы СӘТТІ жіберілген FCM token (dedup үшін) + онымен жіберілген тіл.
 * Сәтсіз POST → токен өшіріледі (Flutter: failure → prefs.remove).
 */
@Singleton
class PushTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("agroland_push", Context.MODE_PRIVATE)

    /** Соңғы сәтті жіберілген токен (болмаса null). */
    val sentToken: String?
        get() = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    /** [sentToken]мен бірге жіберілген тіл (kk/ru/en/zh). */
    val sentLanguage: String?
        get() = prefs.getString(KEY_LANGUAGE, null)?.takeIf { it.isNotBlank() }

    fun save(token: String, language: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    fun clearSent() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_LANGUAGE).apply()
    }

    private companion object {
        const val KEY_TOKEN = "push_sent_token"
        const val KEY_LANGUAGE = "push_sent_language"
    }
}