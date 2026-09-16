package com.agroland.feature.china.data

import com.agroland.core.l10n.AppLocale
import com.agroland.core.network.json.JsonParser
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * MercuryX категория ағашының түйіні (GET /china/categories).
 * Нақты жауап (2026-09-16 probe): `id` (сан), `names` (JSON-строка),
 * `products_count`, `has_children` (`{cnt: N}` объектісі), `entity.image` (URL не []).
 */
data class ChinaCategory(
    val id: Long,
    val names: Map<String, String> = emptyMap(),
    val productCount: Int = 0,
    val hasChildren: Boolean = false,
    val imageUrl: String? = null,
)

/** MercuryX тауар тізімі элементі (GET /china/products). `productId` — BigInteger int64! */
data class ChinaProduct(
    val productId: Long,
    val names: Map<String, String> = emptyMap(),
    val images: List<String> = emptyList(),
    val price: Double = 0.0,
    val currency: String = "KZT",
    val minQty: Int = 1,
)

/** Laravel-paginator беті: `data.data` + `current_page`/`last_page`/`total`. */
data class ChinaProductsPage(
    val products: List<ChinaProduct> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
) {
    val canLoadMore: Boolean get() = totalPages > page
}

/** Толық карточкадағы SKU-оффер (GET /china/products/{id}). */
data class ChinaOfferDetail(
    val externalSkuId: Long? = null,
    val price: Double = 0.0,
    val stock: Int? = null,
    /** `{value, value_trans, attribute_name, attribute_name_trans}` объектілері. */
    val skuAttributes: List<JsonObject> = emptyList(),
)

/** Көтерме баға деңгейі (`priceRanges`: `{price, startQuantity}`). */
data class ChinaBulkTier(
    val minQty: Int = 1,
    val price: Double = 0.0,
)

/** Толық карточка (GET /china/products/{id}). */
data class ChinaProductDetail(
    val productId: Long,
    val names: Map<String, String> = emptyMap(),
    val description: String? = null,
    val images: List<String> = emptyList(),
    val price: Double = 0.0,
    val currency: String = "KZT",
    val minQty: Int = 1,
    /** `{attribute_name, value}` — MercuryX локальді объектілер (parse кезінде шешілмейді). */
    val attributes: List<JsonObject> = emptyList(),
    val offers: List<ChinaOfferDetail> = emptyList(),
    val bulkPricing: List<ChinaBulkTier> = emptyList(),
)

/** Қытай корзина айтемі (GET /china/cart) — толық снапшот. */
data class ChinaCartItem(
    val id: Long,
    val productId: Long,
    val skuId: Long? = null,
    val quantity: Int,
    val minQty: Int? = null,
    val priceAtAdd: Double? = null,
    val titleSnapshot: String? = null,
    val imageUrl: String? = null,
    val createdAt: String? = null,
) {
    /** Корзина тізіміндегі жол бойынша жалпы баға. */
    val lineTotal: Double get() = (priceAtAdd ?: 0.0) * quantity
}

/** china_order.status мәндері (спек 3.1). `pending` клиентке көрінбейді. */
enum class ChinaOrderStatus {
    FORWARDED,
    FAILED,
    CANCELLED,
    UNKNOWN;

    companion object {
        fun fromString(raw: String?): ChinaOrderStatus = when (raw) {
            "forwarded" -> FORWARDED
            "failed" -> FAILED
            "cancelled" -> CANCELLED
            else -> UNKNOWN
        }
    }
}

/** china_order.items снапшот жолы. */
data class ChinaOrderItem(
    val cartItemId: Long? = null,
    val productId: Long,
    val skuId: Long? = null,
    val quantity: Int,
    val titleSnapshot: String? = null,
    val imageUrl: String? = null,
    val priceAtSend: Double? = null,
)

/** Қытай тапсырысы (GET/POST /china/orders) — MercuryX-ке forward нәтижесі. */
data class ChinaOrder(
    val id: Long,
    val userId: Long? = null,
    val phone: String? = null,
    val bin: String? = null,
    val mercuryxId: Long? = null,
    val mercuryxNumber: String? = null,
    val status: ChinaOrderStatus = ChinaOrderStatus.UNKNOWN,
    val errorMessage: String? = null,
    val items: List<ChinaOrderItem> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/**
 * ChinaParser — MercuryX жауаптарының кешірімді парсингі (Flutter china_helpers +
 * repository fold-тары, 1:1). Барлық `success/data` ораулары ашылады, типтер
 * әр түрлі келеді (строка-JSON names, `{cnt}` has_children, string int).
 */
object ChinaParser {

    // ── names: JSON-строка не объект → Map<String, String> ──

    fun parseNames(value: JsonElement?): Map<String, String> {
        val decoded: JsonElement? = when (value) {
            null -> null
            is JsonPrimitive -> JsonParser.parseObject(value.contentOrNull)?.let {
                // строка өзі JSON-объект болуы мүмкін: "{\"cn\": ...}"
                it
            }
            else -> value
        }
        val obj = decoded as? JsonObject ?: return emptyMap()
        return obj.entries.mapNotNull { (k, v) ->
            val content = (v as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            k to content
        }.toMap()
    }

    /** MercuryX names кілттері: cn/en/kk/ru. zh → cn, қалғаны — өз тегі. Fallback ru→kk→en→cn. */
    fun localizedName(names: Map<String, String>, locale: AppLocale): String {
        val key = if (locale == AppLocale.ZH) "cn" else locale.tag
        names[key]?.let { if (it.isNotBlank()) return it }
        for (fallback in listOf("ru", "kk", "en", "cn")) {
            names[fallback]?.let { if (it.isNotBlank()) return it }
        }
        return "—"
    }

    /** `has_children`: bool | сан | `{cnt: N}` → cnt > 0. */
    private fun hasChildrenFrom(value: JsonElement?): Boolean {
        return when (value) {
            is JsonPrimitive ->
                value.booleanOrNull ?: ((value.content.toLongOrNull() ?: 0L) > 0L)
            is JsonObject -> (JsonParser.long(value, "cnt") ?: 0L) > 0L
            else -> false
        }
    }

    /** entity.image: URL строка не [] массив → жалғыз URL. */
    private fun imageFrom(value: JsonElement?): String? = when (value) {
        is JsonPrimitive -> value.contentOrNull?.takeIf { it.isNotBlank() }
        is JsonArray -> (value.firstOrNull() as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        else -> null
    }

    /** `image` (жалғыз URL) → тізім; `slides` (URL тізімі) → тізім. */
    private fun imagesFrom(value: JsonElement?): List<String> = when (value) {
        is JsonPrimitive -> listOfNotNull(value.contentOrNull?.takeIf { it.isNotBlank() })
        is JsonArray -> value.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf { c -> c.isNotBlank() } }
        else -> emptyList()
    }

    // ── Категориялар ──

    fun parseCategories(root: JsonElement): List<ChinaCategory> {
        val rootObj = root as? JsonObject ?: return emptyList()
        // data: массив не {data: массив} (Flutter tolerance).
        val list: List<JsonElement> = when (val data = rootObj["data"]) {
            is JsonArray -> data.toList()
            is JsonObject -> (JsonParser.arr(data, "data") ?: JsonArray(emptyList())).toList()
            else -> emptyList()
        }
        return list.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val entity = JsonParser.obj(obj, "entity")
            ChinaCategory(
                id = JsonParser.long(obj, "id") ?: return@mapNotNull null,
                names = parseNames(obj["names"]),
                productCount = JsonParser.int(obj, "products_count") ?: 0,
                hasChildren = hasChildrenFrom(obj["has_children"]),
                imageUrl = imageFrom(entity?.get("image") ?: obj["image"]),
            )
        }
    }

    // ── Тауар тізімі ──

    fun parseProducts(root: JsonElement): ChinaProductsPage {
        val rootObj = (root as? JsonObject)?.get("data") as? JsonObject
            ?: return ChinaProductsPage()
        val raw = (rootObj["data"] as? JsonArray)?.toList() ?: emptyList()
        val products = raw.mapNotNull { el -> parseProduct(el as? JsonObject ?: return@mapNotNull null) }
        return ChinaProductsPage(
            products = products,
            page = JsonParser.int(rootObj, "current_page") ?: 1,
            totalPages = JsonParser.int(rootObj, "last_page") ?: 1,
            total = JsonParser.int(rootObj, "total") ?: raw.size,
        )
    }

    private fun parseProduct(obj: JsonObject): ChinaProduct? {
        return ChinaProduct(
            productId = JsonParser.long(obj, "productId") ?: return null,
            names = parseNames(obj["names"]),
            images = imagesFrom(obj["image"]),
            price = JsonParser.double(obj, "price") ?: 0.0,
            currency = JsonParser.string(obj, "currency") ?: "KZT",
            minQty = JsonParser.int(obj, "minQty") ?: 1,
        )
    }

    // ── Толық карточка ──

    fun parseProductDetail(root: JsonElement): ChinaProductDetail {
        val obj = (root as? JsonObject)?.get("data") as? JsonObject
            ?: (root as? JsonObject)
            ?: return ChinaProductDetail(productId = -1)
        val offers = (obj["offers"] as? JsonArray)?.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            ChinaOfferDetail(
                externalSkuId = JsonParser.long(o, "external_sku_id") ?: JsonParser.long(o, "externalSkuId"),
                price = JsonParser.double(o, "price") ?: 0.0,
                stock = JsonParser.int(o, "stock"),
                skuAttributes = (o["sku_attributes"] as? JsonArray)
                    ?.mapNotNull { it as? JsonObject } ?: emptyList(),
            )
        } ?: emptyList()
        val bulk = (obj["priceRanges"] as? JsonArray)?.mapNotNull { el ->
            val t = el as? JsonObject ?: return@mapNotNull null
            ChinaBulkTier(
                minQty = JsonParser.int(t, "startQuantity") ?: 1,
                price = JsonParser.double(t, "price") ?: 0.0,
            )
        } ?: emptyList()
        return ChinaProductDetail(
            productId = JsonParser.long(obj, "productId") ?: -1,
            names = parseNames(obj["names"]),
            description = JsonParser.string(obj, "description"),
            images = imagesFrom(obj["slides"] ?: obj["image"]),
            price = JsonParser.double(obj, "price") ?: 0.0,
            currency = JsonParser.string(obj, "currency") ?: "KZT",
            minQty = JsonParser.int(obj, "minQty") ?: 1,
            attributes = (obj["attributes"] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList(),
            offers = offers,
            bulkPricing = bulk,
        )
    }

    // ── Корзина ──

    fun parseCart(root: JsonElement): List<ChinaCartItem> {
        val rootObj = root as? JsonObject ?: return emptyList()
        // Flutter: root['items']; {data:{items}} орауына да толерантты.
        val list = when (val data = rootObj["data"]) {
            is JsonObject -> JsonParser.arr(data, "items") ?: JsonParser.arr(rootObj, "items")
            else -> JsonParser.arr(rootObj, "items")
        } ?: return emptyList()
        return list.mapNotNull { el -> parseCartItem(el as? JsonObject ?: return@mapNotNull null) }
    }

    fun parseCartItem(root: JsonElement): ChinaCartItem {
        val obj = root as? JsonObject
            ?: return ChinaCartItem(id = -1, productId = -1, quantity = 0)
        return ChinaCartItem(
            id = JsonParser.long(obj, "id") ?: return ChinaCartItem(id = -1, productId = -1, quantity = 0),
            productId = JsonParser.long(obj, "product_id") ?: -1,
            skuId = JsonParser.long(obj, "sku_id"),
            quantity = JsonParser.int(obj, "quantity") ?: 1,
            minQty = JsonParser.int(obj, "min_qty"),
            priceAtAdd = JsonParser.double(obj, "price_at_add"),
            titleSnapshot = JsonParser.string(obj, "title_snapshot"),
            imageUrl = JsonParser.string(obj, "image_url"),
            createdAt = JsonParser.string(obj, "created_at"),
        )
    }

    fun buildAddCartBody(item: ChinaAddCartItemRequest): JsonObject = buildJsonObject {
        put("product_id", item.productId)
        item.skuId?.let { put("sku_id", it) }
        put("quantity", item.quantity)
        put("min_qty", item.minQty)
        put("price_at_add", item.priceAtAdd)
        put("title_snapshot", item.titleSnapshot)
        item.imageUrl?.let { put("image_url", it) }
    }

    // ── Тапсырыстар ──

    fun buildOrderBody(phone: String, bin: String, cartItemIds: List<Long>?): JsonObject = buildJsonObject {
        put("phone", phone)
        put("bin", bin)
        // Rule #1: consent_accepted міндетті — UI чекбоксы тек берілгенде шақырады.
        put("consent_accepted", true)
        cartItemIds?.let { ids ->
            put("cart_item_ids", kotlinx.serialization.json.buildJsonArray {
                ids.forEach { add(it) }
            })
        }
    }

    /** create-order жауабы: `{success, data}` орауы + `china_order_id` → `id` нормализациясы. */
    fun parseCreatedOrder(root: JsonElement): ChinaOrder {
        val rootObj = (root as? JsonObject) ?: return ChinaOrder(id = -1)
        val inner = rootObj["data"] as? JsonObject ?: rootObj
        val map = if (inner["id"] == null && inner["china_order_id"] != null) {
            // Нормализация: тарих `id`, create — `china_order_id` қайтарды.
            buildJsonObject {
                inner.entries.forEach { (k, v) -> put(k, v) }
                (JsonParser.long(inner, "china_order_id"))?.let { put("id", it) }
            }
        } else {
            inner
        }
        return parseOrder(map)
    }

    /** Тапсырыс тарихы: `{success,data}` / `data.orders` / `data.items` / `orders` — барлығы қолданылады. */
    fun parseOrders(root: JsonElement): List<ChinaOrder> {
        val rootObj = root as? JsonObject ?: return emptyList()
        val raw: List<JsonElement>? = when (val data = rootObj["data"]) {
            is JsonObject -> (JsonParser.arr(data, "orders") ?: JsonParser.arr(data, "items"))?.toList()
            is JsonArray -> data.toList()
            else -> null
        }
        val list = raw ?: (JsonParser.arr(rootObj, "orders") ?: JsonParser.arr(rootObj, "items"))?.toList()
            ?: return emptyList()
        return list.mapNotNull { el -> el as? JsonObject } .map { parseOrder(it) }
    }

    private fun parseOrder(obj: JsonObject): ChinaOrder = ChinaOrder(
        id = JsonParser.long(obj, "id") ?: -1,
        userId = JsonParser.long(obj, "user_id"),
        phone = JsonParser.string(obj, "phone"),
        bin = JsonParser.string(obj, "bin"),
        mercuryxId = JsonParser.long(obj, "mercuryx_id"),
        mercuryxNumber = JsonParser.string(obj, "mercuryx_number"),
        status = ChinaOrderStatus.fromString(JsonParser.string(obj, "status")),
        errorMessage = JsonParser.string(obj, "error_message"),
        items = (obj["items"] as? JsonArray)?.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            ChinaOrderItem(
                cartItemId = JsonParser.long(o, "cart_item_id"),
                productId = JsonParser.long(o, "product_id") ?: -1,
                skuId = JsonParser.long(o, "sku_id"),
                quantity = JsonParser.int(o, "quantity") ?: 1,
                titleSnapshot = JsonParser.string(o, "title_snapshot"),
                imageUrl = JsonParser.string(o, "image_url"),
                priceAtSend = JsonParser.double(o, "price_at_send"),
            )
        } ?: emptyList(),
        createdAt = JsonParser.string(obj, "created_at"),
        updatedAt = JsonParser.string(obj, "updated_at"),
    )
}

/**
 * MercuryX HTML сипаттамасын таза мәтінге айналдырады (Flutter chinaStripHtml).
 * Бос болса null.
 */
fun stripChinaHtml(html: String?): String? {
    if (html.isNullOrBlank()) return null
    val stripped = html
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    return stripped.takeIf { it.isNotEmpty() }
}

/**
 * MercuryX локальді өрісін (строка не `{ru, en, cn}` объект) ағымдақы тіл бойынша
 * шешеді — атрибут `attribute_name`/`value` өрістері үшін (Flutter _pickLocalized).
 */
fun pickChinaLocalized(value: JsonElement?, locale: AppLocale): String {
    when (value) {
        is JsonPrimitive -> return value.contentOrNull ?: ""
        is JsonObject -> {
            val key = if (locale == AppLocale.ZH) "cn" else locale.tag
            (value[key] as? JsonPrimitive)?.contentOrNull?.let { if (it.isNotBlank()) return it }
            for (fallback in listOf("ru", "en", "cn", "kk")) {
                (value[fallback] as? JsonPrimitive)?.contentOrNull?.let { if (it.isNotBlank()) return it }
            }
            value.entries.firstNotNullOfOrNull { entry ->
                (entry.value as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            }?.let { return it }
        }
        else -> return ""
    }
    return ""
}