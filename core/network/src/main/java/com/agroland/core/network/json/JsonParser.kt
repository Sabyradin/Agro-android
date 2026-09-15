package com.agroland.core.network.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/**
 * Кешірімді JSON парсері. Backend JSON типтері тұрақсыз (spec §4):
 * int кейде string, null кейде жоқ өріс, массив кейде жалғыз объект.
 * Барлық DTO оқуы осы көмекшілер арқылы жүреді.
 */
object JsonParser {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun parseObject(raw: String?): JsonObject? = try {
        if (raw.isNullOrBlank()) null else json.parseToJsonElement(raw).jsonObject
    } catch (e: Exception) {
        null
    }

    fun obj(root: JsonObject?, key: String): JsonObject? =
        (root?.get(key) as? JsonObject)

    fun arr(root: JsonObject?, key: String): JsonArray? =
        (root?.get(key) as? JsonArray)

    /** Массив немесе жалғыз объект → тізім (items-or-array қолдауы). */
    fun arrayOrSingle(root: JsonObject?, key: String): List<JsonElement> {
        val value = root?.get(key) ?: return emptyList()
        return when (value) {
            is JsonArray -> value.toList()
            is JsonObject -> listOf(value)
            else -> emptyList()
        }
    }

    fun string(root: JsonObject?, key: String): String? {
        val el = root?.get(key) ?: return null
        return (el as? JsonPrimitive)?.contentOrNull
    }

    fun int(root: JsonObject?, key: String): Int? {
        val el = root?.get(key) ?: return null
        return (el as? JsonPrimitive)?.let {
            it.intOrNull ?: it.contentOrNull?.toIntOrNull()
        }
    }

    fun long(root: JsonObject?, key: String): Long? {
        val el = root?.get(key) ?: return null
        return (el as? JsonPrimitive)?.let {
            it.longOrNull ?: it.contentOrNull?.toLongOrNull()
        }
    }

    fun double(root: JsonObject?, key: String): Double? {
        val el = root?.get(key) ?: return null
        return (el as? JsonPrimitive)?.let {
            it.doubleOrNull ?: it.contentOrNull?.toDoubleOrNull()
        }
    }

    fun bool(root: JsonObject?, key: String): Boolean? {
        val el = root?.get(key) ?: return null
        return (el as? JsonPrimitive)?.let {
            it.booleanOrNull ?: it.contentOrNull?.lowercase()?.toBooleanStrictOrNull()
        }
    }
}