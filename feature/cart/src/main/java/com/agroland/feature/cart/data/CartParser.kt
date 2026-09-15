package com.agroland.feature.cart.data

import com.agroland.core.network.json.JsonParser
import com.agroland.feature.marketplace.data.Announcement
import com.agroland.feature.marketplace.data.MarketplaceParser
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Себет/тапсырыс JSON парсері — кешірімді (тип тұрақсыздығына төзімді,
 * backend жаңа өріс қосса құламайды). Барлық окымыс осында.
 */
object CartParser {

    /** Себет тізімі — {items:[...]} немесе тікелей массив. */
    fun parseCart(root: JsonElement?): List<CartItem> {
        val array = when (root) {
            is JsonObject -> JsonParser.arrayOrSingle(root, "items")
            is kotlinx.serialization.json.JsonArray -> root
            else -> emptyList()
        }
        return array.mapNotNull { parseCartItem(it as? JsonObject) }
    }

    /** Себет айтемі: id  міндетті, announcement ішкі нысан — кешірімді. */
    fun parseCartItem(obj: JsonObject?): CartItem? {
        if (obj == null) return null
        val id = JsonParser.long(obj, "id") ?: return null
        return CartItem(
            id = id,
            announcementId = JsonParser.long(obj, "announcement_id") ?: return null,
            quantity = JsonParser.double(obj, "quantity") ?: 0.0,
            measurementUnit = JsonParser.string(obj, "measurement_unit"),
            announcement = parseCartAnnouncement(JsonParser.obj(obj, "announcement")),
        )
    }

    /** Картадағы ішкі жарнама — дәл Flutter AnnouncementModel пішімі (delivery_zones қоса). */
    fun parseCartAnnouncement(obj: JsonObject?): CartAnnouncement? {
        if (obj == null) return null
        val base: Announcement = MarketplaceParser.parseAnnouncement(obj) ?: return null
        return CartAnnouncement(
            base = base,
            deliveryZones = MarketplaceParser.parseDeliveryZoneInfos(obj),
        )
    }

    /** /cart/preview — grand_total/groups/warnings. */
    fun parseCartPreview(root: JsonObject?): CartPreview? {
        if (root == null) return null
        return CartPreview(
            grandTotal = JsonParser.double(root, "grand_total") ?: 0.0,
            groups = JsonParser.arrayOrSingle(root, "groups").mapNotNull { parsePreviewGroup(it as? JsonObject) },
            warnings = JsonParser.arrayOrSingle(root, "warnings")
                .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                .filter { it.isNotBlank() },
        )
    }

    private fun parsePreviewGroup(obj: JsonObject?): CartPreviewGroup? {
        if (obj == null) return null
        return CartPreviewGroup(
            supplierId = JsonParser.long(obj, "supplier_id"),
            supplierName = JsonParser.string(obj, "supplier_name") ?: "",
            isVatPayer = JsonParser.bool(obj, "is_vat_payer") ?: false,
            goodsSubtotal = JsonParser.double(obj, "goods_subtotal") ?: 0.0,
            deliveryFee = JsonParser.double(obj, "delivery_fee") ?: 0.0,
            vatRate = JsonParser.double(obj, "vat_rate") ?: 0.0,
            vatAmount = JsonParser.double(obj, "vat_amount") ?: 0.0,
            vatIncludedInPrice = JsonParser.bool(obj, "vat_included_in_price") ?: true,
            total = JsonParser.double(obj, "total") ?: 0.0,
        )
    }

    /** /cart/checkout — {order_ids} (сандық массив немесе бір сан — екеуі де қолдауда). */
    fun parseCheckoutResult(root: JsonObject?): CheckoutResult {
        if (root == null) return CheckoutResult(emptyList())
        val ids = when (val raw = root["order_ids"]) {
            is kotlinx.serialization.json.JsonArray ->
                raw.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull() }
            else -> JsonParser.long(root, "order_ids")?.let { listOf(it) } ?: emptyList()
        }
        return CheckoutResult(ids)
    }

    /** Тапсырыстар тізімі — {items:[...]} немесе тікелей массив. */
    fun parseOrders(root: JsonElement?): List<Order> {
        val array = when (root) {
            is JsonObject -> JsonParser.arrayOrSingle(root, "items")
            is kotlinx.serialization.json.JsonArray -> root
            else -> emptyList()
        }
        return array.mapNotNull { parseOrder(it as? JsonObject) }
    }

    /** Толық тапсырыс — id/announcement_id міндетті, қалғандары кешірімді. */
    fun parseOrder(obj: JsonObject?): Order? {
        if (obj == null) return null
        val id = JsonParser.long(obj, "id") ?: return null
        return Order(
            id = id,
            announcementId = JsonParser.long(obj, "announcement_id") ?: 0L,
            quantity = JsonParser.double(obj, "quantity") ?: 0.0,
            measurementUnit = JsonParser.string(obj, "measurement_unit"),
            status = JsonParser.string(obj, "status") ?: "supplier_query",
            paymentStatus = JsonParser.string(obj, "payment_status"),
            buyerId = JsonParser.long(obj, "buyer_id"),
            sellerId = JsonParser.long(obj, "supplier_id"),
            agreedPrice = JsonParser.double(obj, "agreed_price"),
            totalPrice = JsonParser.double(obj, "total_price"),
            totalAmount = JsonParser.double(obj, "total_amount"),
            pickupAddress = JsonParser.string(obj, "pickup_address"),
            deliveryAddress = JsonParser.string(obj, "delivery_address"),
            createdAt = JsonParser.string(obj, "created_at"),
            paidAt = JsonParser.string(obj, "paid_at"),
            loadingDate = JsonParser.string(obj, "loading_date"),
            deliveryDate = JsonParser.string(obj, "delivery_date"),
            updatedAt = JsonParser.string(obj, "updated_at"),
            currency = JsonParser.string(obj, "currency"),
            notes = JsonParser.string(obj, "notes"),
            announcement = parseCartAnnouncement(JsonParser.obj(obj, "announcement")),
            seller = parseOrderParty(JsonParser.obj(obj, "seller")),
            subtotal = JsonParser.double(obj, "subtotal"),
            deliveryFee = JsonParser.double(obj, "delivery_fee"),
            vatAmount = JsonParser.double(obj, "vat_amount"),
            vatRate = JsonParser.double(obj, "vat_rate"),
            items = JsonParser.arrayOrSingle(obj, "items").mapNotNull { parseOrderItem(it as? JsonObject) },
            firstItemTitle = JsonParser.string(obj, "first_item_title"),
            itemsCount = JsonParser.int(obj, "items_count"),
        )
    }

    private fun parseOrderItem(obj: JsonObject?): OrderItem? {
        if (obj == null) return null
        val id = JsonParser.long(obj, "id") ?: return null
        return OrderItem(
            id = id,
            announcementId = JsonParser.long(obj, "announcement_id"),
            title = JsonParser.string(obj, "title"),
            quantity = JsonParser.double(obj, "quantity") ?: 0.0,
            unitPrice = JsonParser.double(obj, "unit_price"),
            lineSubtotal = JsonParser.double(obj, "line_subtotal"),
        )
    }

    /** seller/buyer — {user_name, user:{id, avatar_url, phone_number}} кешірімді оқымыс. */
    fun parseOrderParty(obj: JsonObject?): OrderParty? {
        if (obj == null) return null
        val user = JsonParser.obj(obj, "user")
        return OrderParty(
            id = JsonParser.long(obj, "id") ?: JsonParser.long(user, "id"),
            name = JsonParser.string(obj, "user_name")
                ?: JsonParser.string(obj, "name")
                ?: JsonParser.string(user, "name"),
            avatarUrl = JsonParser.string(obj, "avatar_url") ?: JsonParser.string(user, "avatar_url"),
            phone = JsonParser.string(obj, "phone_number")
                ?: JsonParser.string(obj, "phone")
                ?: JsonParser.string(user, "phone_number"),
        )
    }

    /** /orders/{id}/tracking — waybills жоқ/бос болса да объект қайтарылады. */
    fun parseOrderTracking(root: JsonObject?): OrderTracking {
        val orderId = JsonParser.long(root, "order_id") ?: 0L
        val waybills = JsonParser.arrayOrSingle(root, "waybills").mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            Waybill(
                id = JsonParser.long(obj, "id") ?: return@mapNotNull null,
                waybillNumber = JsonParser.string(obj, "waybill_number") ?: "",
                vehicleNumber = JsonParser.string(obj, "vehicle_number"),
                driverName = JsonParser.string(obj, "driver_name"),
            )
        }
        return OrderTracking(orderId = orderId, status = JsonParser.string(root, "status"), waybills = waybills)
    }
}

/** POST/PATCH денелерін жасаушы — тек толтырылған өрістер жіберіледі. */
object CartRequests {

    fun addToCart(announcementId: Long, quantity: Double, measurementUnit: String?): JsonObject =
        buildJsonObject {
            put("announcement_id", announcementId)
            put("quantity", quantity)
            measurementUnit?.takeIf { it.isNotBlank() }?.let { put("measurement_unit", it) }
        }

    fun updateQuantity(quantity: Double): JsonObject = buildJsonObject {
        put("quantity", quantity)
    }

    fun updateOrderStatus(status: String, note: String? = null): JsonObject = buildJsonObject {
        put("status", status)
        note?.takeIf { it.isNotBlank() }?.let { put("note", it) }
    }

    fun updateOrder(pickupAddress: String? = null, deliveryAddress: String? = null): JsonObject =
        buildJsonObject {
            pickupAddress?.let { put("pickup_address", it) }
            deliveryAddress?.let { put("delivery_address", it) }
        }

    /**
     * Атомарлы чекаут денесі (M8): бос опциялар жіберілмейді (pickup-тек
     * чекаут бос дене жібереді — legacy үйлесімділік).
     */
    fun checkout(
        deliveryDistrictId: Int? = null,
        pickup: Boolean? = null,
        deliveryAddress: String? = null,
        pickupAddress: String? = null,
        onlySupplierId: Long? = null,
        lines: List<CheckoutLine> = emptyList(),
    ): JsonObject = buildJsonObject {
        deliveryDistrictId?.let { put("delivery_district_id", it) }
        pickup?.let { put("pickup", it) }
        deliveryAddress?.let { put("delivery_address", it) }
        pickupAddress?.let { put("pickup_address", it) }
        onlySupplierId?.let { put("only_supplier_id", it) }
        if (lines.isNotEmpty()) {
            put(
                "items",
                buildJsonArray {
                    lines.forEach { line ->
                        add(
                            buildJsonObject {
                                put("cart_item_id", line.cartItemId)
                                line.deliveryZoneId?.let { put("delivery_zone_id", it) }
                            },
                        )
                    }
                },
            )
        }
    }

    fun buyNow(
        announcementId: Long,
        quantity: Double,
        measurementUnit: String? = null,
        deliveryAddress: String? = null,
        deliveryZoneId: Long? = null,
    ): JsonObject = buildJsonObject {
        put("announcement_id", announcementId)
        put("quantity", quantity)
        measurementUnit?.takeIf { it.isNotBlank() }?.let { put("measurement_unit", it) }
        deliveryAddress?.let { put("delivery_address", it) }
        deliveryZoneId?.let { put("delivery_zone_id", it) }
    }
}