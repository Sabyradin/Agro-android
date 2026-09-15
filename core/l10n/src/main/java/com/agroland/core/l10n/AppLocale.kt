package com.agroland.core.l10n

import kotlinx.serialization.Serializable

/**
 * Қосымша ішкі тілдері. kk — әдепкі.
 * [backendKey] — backend `names: {ru, kz, en, ch}` кілттерімен сәйкестік: kk→kz, zh→ch (КРИЗИСТІК маңызды).
 */
@Serializable
enum class AppLocale(val tag: String, val nativeName: String, val backendKey: String) {
    KK("kk", "Қазақша", "kz"),
    RU("ru", "Русский", "ru"),
    EN("en", "English", "en"),
    ZH("zh", "中文", "ch");

    companion object {
        fun fromTagOrNull(tag: String?): AppLocale? =
            tag?.trim()?.lowercase()?.let { t -> entries.firstOrNull { it.tag == t } }

        /** Танымал емес тег → әдепкі kk. */
        fun fromTag(tag: String?): AppLocale = fromTagOrNull(tag) ?: KK
    }
}