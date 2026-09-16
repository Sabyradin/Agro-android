package com.agroland.feature.notifications.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Хабарламалар моделдері (Flutter NotificationModel / NotificationCounterModel):
 * backend типтері тұрақсыз болғандықтан парсерлер кешірімді (JsonParser).
 */

/** Бір хабарлама (GET /notifications/{type} → items[]). */
@Serializable
data class NotificationItem(
    val id: Long,
    val title: String,
    val text: String,
    /** Сурет: URL (http/https) немесе base64 (Flutter buildNotificationImage). */
    val file: String,
    val createdAt: Long,
)

/** Үш бөлімнің оқылмаған санағы (GET /notifications). */
data class NotificationCounter(
    val serviceCount: Int,
    val supportCount: Int,
    val promotionsCount: Int,
)

/** Хабарлама түрі (Flutter NotificationType enum: service|support|promotions). */
enum class NotificationType(val path: String) {
    SERVICE("service"),
    SUPPORT("support"),
    PROMOTIONS("promotions");

    companion object {
        fun fromString(raw: String): NotificationType =
            entries.firstOrNull { it.path.equals(raw, ignoreCase = true) } ?: SERVICE
    }
}

/** Толерантты парсерлер (backend: int/string/null аралас). */
object NotificationsParser {

    /** `{service_count, support_count, promotions_count}` (түбірде). */
    fun parseCounter(root: JsonElement?): NotificationCounter {
        val o: JsonObject? = when {
            root is JsonObject && (root.containsKey("service_count") ||
                root.containsKey("support_count") ||
                root.containsKey("promotions_count")) -> root
            root is JsonObject -> (root["data"] as? JsonObject) ?: root
            else -> null
        }
        return NotificationCounter(
            serviceCount = intOr(o, "service_count") ?: 0,
            supportCount = intOr(o, "support_count") ?: 0,
            promotionsCount = intOr(o, "promotions_count") ?: 0,
        )
    }

    /** `{items: [...]}` (Flutter fromJsonList). */
    fun parseItems(root: JsonElement?): List<NotificationItem> {
        val obj = root as? JsonObject ?: return emptyList()
        val arr = (obj["items"] as? JsonArray)
            ?: ((obj["data"] as? JsonObject)?.get("items") as? JsonArray)
            ?: (obj["data"] as? JsonArray)
            ?: return emptyList()
        return arr.filterIsInstance<JsonObject>().mapNotNull(::parseItem)
    }

    fun parseItem(obj: JsonObject): NotificationItem? {
        val id = longOr(obj, "id") ?: return null
        return NotificationItem(
            id = id,
            title = stringOr(obj, "title").orEmpty(),
            text = stringOr(obj, "text").orEmpty(),
            file = stringOr(obj, "file").orEmpty(),
            createdAt = parseTime(stringOr(obj, "created_at")),
        )
    }

    /** Backend уақыты (ISO 8601) → epoch millis; оқылмаса — қазір. */
    fun parseTime(raw: String?): Long = try {
        if (raw.isNullOrBlank()) System.currentTimeMillis() else java.time.Instant.parse(raw).toEpochMilli()
    } catch (_: Exception) {
        try {
            java.time.OffsetDateTime.parse(raw).toInstant().toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun stringOr(obj: JsonObject, key: String): String? =
        com.agroland.core.network.json.JsonParser.string(obj, key)

    private fun longOr(obj: JsonObject, key: String): Long? =
        com.agroland.core.network.json.JsonParser.long(obj, key)

    private fun intOr(obj: JsonObject?, key: String): Int? =
        obj?.let { com.agroland.core.network.json.JsonParser.int(it, key) }
}