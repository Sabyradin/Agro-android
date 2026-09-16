package com.agroland.feature.demand.data

import com.agroland.core.network.json.JsonParser
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Сұраныс (demand) — пайдаланушының «тауар/қызмет іздеймін» жарияламасы.
 * Спек §2 (SWIFT_REWRITE_SPEC L118): GET/POST /demands, GET/PATCH/DELETE
 * /demand/{id}, PATCH /demand/{id}/activate | /demand/{id}/deactivate.
 * Дев-ортада тізім бос — парсер толығымен кешірімді (белгісіз өрістерге tolerant).
 */
data class DemandItem(
    val id: Long,
    val title: String = "",
    val description: String? = null,
    val maxPrice: Double? = null,
    val currency: String? = null,
    val categoryId: Int? = null,
    val subcategoryId: Int? = null,
    val categoryName: String? = null,
    val subcategoryName: String? = null,
    val measurementUnit: String? = null,
    val quantity: Double? = null,
    val status: String? = null,
    val createdAt: String? = null,
) {
    val isActive: Boolean
        get() = when (status?.lowercase()) {
            null, "active", "activated", "published", "1", "true" -> true
            else -> false
        }
}

/** Laravel-түрі парақтау: `{items, total, page, size, pages}` — бәрі optional. */
data class DemandsPage(
    val items: List<DemandItem> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
) {
    val canLoadMore: Boolean get() = totalPages > page
}

/** Сұраныс жасау/өзгерту денесі — backend createDemand өрістерімен сайткес. */
data class DemandDraft(
    val title: String,
    val description: String? = null,
    val currency: String? = null,
    val categoryId: Int? = null,
    val subcategoryId: Int? = null,
    val maxPrice: Double? = null,
    val measurementUnit: String? = null,
    val quantity: Double? = null,
)

object DemandParser {

    fun buildBody(draft: DemandDraft): JsonObject = buildJsonObject {
        put("title", draft.title)
        draft.description?.let { put("description", it) }
        draft.currency?.let { put("currency", it) }
        draft.categoryId?.let { put("category_id", it) }
        draft.subcategoryId?.let { put("subcategory_id", it) }
        draft.maxPrice?.let { put("max_price", it) }
        draft.measurementUnit?.let { put("measurement_unit", it) }
        draft.quantity?.let { put("quantity", it) }
    }

    /** Жеке сұраныс: `{success, data}` / `{data}` / тікелей объект — бәрі ашылады. */
    fun parseDemand(root: JsonElement): DemandItem? {
        var obj = root as? JsonObject ?: return null
        (obj["data"] as? JsonObject)?.let { obj = it }
        (obj["demand"] as? JsonObject)?.let { obj = it }
        val id = JsonParser.long(obj, "id") ?: JsonParser.long(obj, "demand_id") ?: return null
        return parseItem(id, obj)
    }

    /** Тізім: `{items,...}` / `{data:{items}}` / `{data:[...]}` / тікелей массив. */
    fun parseDemands(root: JsonElement): DemandsPage {
        val rootObj = root as? JsonObject
            ?: return DemandsPage(items = parseList(root))
        if (rootObj["items"] is JsonArray || rootObj["items"] is JsonObject) {
            val list = JsonParser.arrayOrSingle(rootObj, "items").mapNotNull { parseDemand(it) }
            return DemandsPage(
                items = list,
                page = JsonParser.int(rootObj, "page") ?: 1,
                totalPages = JsonParser.int(rootObj, "pages")
                    ?: JsonParser.int(rootObj, "last_page") ?: 1,
                total = JsonParser.int(rootObj, "total") ?: list.size,
            )
        }
        (rootObj["data"] as? JsonObject)?.let { data ->
            val list = JsonParser.arrayOrSingle(data, "items").mapNotNull { parseDemand(it) }
            if (list.isNotEmpty() || data["items"] != null) {
                return DemandsPage(
                    items = list,
                    page = JsonParser.int(data, "page") ?: 1,
                    totalPages = JsonParser.int(data, "pages")
                        ?: JsonParser.int(data, "last_page") ?: 1,
                    total = JsonParser.int(data, "total") ?: list.size,
                )
            }
        }
        (rootObj["data"] as? JsonArray)?.let {
            val list = it.toList().mapNotNull { el -> parseDemand(el) }
            return DemandsPage(items = list, total = list.size)
        }
        return DemandsPage()
    }

    private fun parseList(root: JsonElement): List<DemandItem> =
        (root as? JsonArray)?.toList()?.mapNotNull { parseDemand(it) } ?: emptyList()

    private fun parseItem(id: Long, obj: JsonObject): DemandItem {
        val category = obj["category"] as? JsonObject
        val subcategory = obj["subcategory"] as? JsonObject
        return DemandItem(
            id = id,
            title = JsonParser.string(obj, "title")
                ?: JsonParser.string(obj, "name") ?: "",
            description = JsonParser.string(obj, "description"),
            maxPrice = JsonParser.double(obj, "max_price") ?: JsonParser.double(obj, "maxPrice"),
            currency = JsonParser.string(obj, "currency"),
            categoryId = JsonParser.int(obj, "category_id") ?: JsonParser.int(obj, "categoryId"),
            subcategoryId = JsonParser.int(obj, "subcategory_id")
                ?: JsonParser.int(obj, "subcategoryId"),
            categoryName = localizedCategoryName(category),
            subcategoryName = localizedCategoryName(subcategory),
            measurementUnit = JsonParser.string(obj, "measurement_unit")
                ?: JsonParser.string(obj, "measurementUnit"),
            quantity = JsonParser.double(obj, "quantity"),
            status = JsonParser.string(obj, "status"),
            createdAt = JsonParser.string(obj, "created_at") ?: JsonParser.string(obj, "createdAt"),
        )
    }

    /** `category` объектінің атауы — name_??/name/title кезекпен. */
    private fun localizedCategoryName(category: JsonObject?): String? {
        if (category == null) return null
        return JsonParser.string(category, "name")
            ?: JsonParser.string(category, "name_ru")
            ?: JsonParser.string(category, "name_kk")
            ?: JsonParser.string(category, "title")
    }

    /** Қарапайым текст өрісі (parse кезінде қажет болса). */
    fun primitiveString(value: JsonElement?): String? =
        (value as? JsonPrimitive)?.contentOrNull
}