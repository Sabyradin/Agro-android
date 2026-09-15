package com.agroland.core.l10n

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Backend `names: {ru, kz, en, ch}` объектілерінен дұрыс кілтті таңдайтын көмекші.
 * Дұрыс кілт таңдалмаса — атаулар бос шығады (iOS портындағы сабақ).
 *
 * Кері тәртіп: қосымша тілі → kz/ru/en/ch тікелей кілт → ru fallback.
 */
object LocalizedNames {

    fun backendKeyFor(locale: AppLocale): String = locale.backendKey

    /**
     * Backend жауабындағы names жүйесінен ағымдағы тілдегі атауды алады.
     * names: {"ru": "...", "kz": "...", "en": "...", "ch": "..."}
     */
    fun pick(
        names: JsonObject?,
        locale: AppLocale,
        fallback: String? = null,
    ): String {
        if (names == null) return fallback.orEmpty()
        val direct = names[locale.backendKey]?.jsonPrimitive?.contentOrNullSafe()
        if (!direct.isNullOrBlank()) return direct
        // Fallback тізбегі: ru → kz → en → ch → кез келген бос емес
        for (key in listOf("ru", "kz", "en", "ch")) {
            val v = names[key]?.jsonPrimitive?.contentOrNullSafe()
            if (!v.isNullOrBlank()) return v
        }
        return fallback.orEmpty()
    }

    /** Полярлы өрістер: name, kz_name, eng_name, ch_name, ru_name стилі (CategoryModel тәрізді). */
    fun pickLegacy(
        ru: String?,
        kz: String?,
        en: String?,
        ch: String?,
        locale: AppLocale,
    ): String = when (locale) {
        AppLocale.KK -> kz?.takeIf { it.isNotBlank() } ?: ru.orEmpty()
        AppLocale.RU -> ru.orEmpty()
        AppLocale.EN -> en?.takeIf { it.isNotBlank() } ?: ru.orEmpty()
        AppLocale.ZH -> ch?.takeIf { it.isNotBlank() } ?: ru.orEmpty()
    }

    private fun JsonPrimitive.contentOrNullSafe(): String? =
        if (isString) content else null
}