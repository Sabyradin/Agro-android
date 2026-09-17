package com.agroland.feature.auth.data

import com.agroland.core.network.json.JsonParser
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * HATEOAS auth жауабынан MFA `task_id` шығарады.
 *
 * DEV backend `links`-ті МАССИВ түрінде береді:
 * ```
 * {"links":[{"name":"mfaSmsRequest","href":"/auth/mfa/<uuid>","verb":"GET"}, …]}
 * ```
 * Бұрын код оны тек объект деп оқитын да (`as? JsonObject`), массив келгенде
 * `null` қайтарып, кіру бірінші қадамнан әрі жүрмейтін — экранда «Бірдеңе
 * дұрыс болмады» шығатын (ISSUES #73). Сондықтан екі пішін де қолдауда.
 */
internal object AuthTaskIdParser {

    fun extract(root: JsonObject?): String? {
        JsonParser.string(root, "task_id")?.let { return it }

        val entries: List<JsonElement> = when (val links = root?.get("links")) {
            is JsonArray -> links
            is JsonObject -> links.values.toList()
            else -> return null
        }
        for (entry in entries) {
            val href = when (entry) {
                is JsonObject -> JsonParser.string(entry, "href")
                is JsonPrimitive -> entry.contentOrNull
                else -> null
            } ?: continue
            fromHref(href)?.let { return it }
        }
        return null
    }

    /** `/auth/mfa/<uuid>` немесе толық URL → `<uuid>`. */
    private fun fromHref(href: String): String? {
        val idx = href.indexOf("mfa/")
        if (idx < 0) return null
        return href.substring(idx + 4)
            .substringBefore('?')
            .substringBefore('/')
            .takeIf { it.isNotBlank() }
    }
}
