package com.agroland.feature.cart.data

import com.agroland.feature.marketplace.data.Announcement
import com.agroland.feature.marketplace.data.AnnouncementDeliveryZone

/**
 * Тапсырыстың орындалу статусы — payment_status-тан тәуелсіз (Flutter OrderStatus).
 * Рет маңызды: timeline «done» шекаралары enum индексімен салынады.
 */
enum class OrderStatus(val value: String) {
    SUPPLIER_QUERY("supplier_query"),
    SUPPLIER_CONFIRMED("supplier_confirmed"),
    LOGISTICS_SEARCH("logistics_search"),
    LOGISTICS_ASSIGNED("logistics_assigned"),
    LOGISTICS_ENROUTE("logistics_enroute"),
    LOADING("loading"),
    LOADED("loaded"),
    DELIVERED("delivered"),
    ACT_SIGNED("act_signed"),
    PAID("paid"),
    CANCELLED("cancelled");

    companion object {
        /** Белгісіз код → null (жаңа статус келсе құламайды). */
        fun tryParse(value: String?): OrderStatus? = entries.firstOrNull { it.value == value }

        /** Белгісіз код → SUPPLIER_QUERY (Flutter parse fallback). */
        fun parse(value: String?): OrderStatus = tryParse(value) ?: SUPPLIER_QUERY
    }
}

/** Төлем статусы (unpaid/paid/refunded). */
enum class PaymentStatus(val value: String) {
    UNPAID("unpaid"),
    PAID("paid"),
    REFUNDED("refunded");

    companion object {
        fun tryParse(value: String?): PaymentStatus? = entries.firstOrNull { it.value == value }
        fun parse(value: String?): PaymentStatus = tryParse(value) ?: UNPAID
    }
}

/**
 * Себет айтемінің ішіндегі жарнама снапшоты — AnnouncementModel (Flutter):
 * картадан келетін announcement нысаны; delivery_zones тек dealer жарнамаларында.
 */
data class CartAnnouncement(
    val base: Announcement,
    val deliveryZones: List<AnnouncementDeliveryZone> = emptyList(),
)

/** Себет айтемі — CartItemModel: id, announcement_id, quantity, measurement_unit, announcement. */
data class CartItem(
    val id: Long,
    val announcementId: Long,
    val quantity: Double,
    val measurementUnit: String?,
    val announcement: CartAnnouncement?,
) {
    /** Checkout үшін сатушы (authorId) — жарнама жоқ болса null. */
    val authorId: Long? get() = announcement?.base?.authorId
}

/** Smart Calculator (M7): /cart/preview тобы — бір сатушының үлесі. */
data class CartPreviewGroup(
    val supplierId: Long?,
    val supplierName: String,
    val isVatPayer: Boolean,
    val goodsSubtotal: Double,
    val deliveryFee: Double,
    val vatRate: Double,
    val vatAmount: Double,
    val vatIncludedInPrice: Boolean,
    val total: Double,
)

/** /cart/preview жауабы — per-supplier breakdown + cart-wide warnings. */
data class CartPreview(
    val grandTotal: Double,
    val groups: List<CartPreviewGroup>,
    val warnings: List<String>,
) {
    val hasVat: Boolean get() = groups.any { it.vatAmount > 0 }
}

/** POST /cart/checkout жауабы — {order_ids} (warnings оқылмайды, preview-де көрсетіледі). */
data class CheckoutResult(val orderIds: List<Long>)

/** Smart Calculator (M8): атомарлы чекаут денесінің бір жолы. */
data class CheckoutLine(val cartItemId: Long, val deliveryZoneId: Long?)

/** Тапсырыстың тауар жолы (Smart Calculator): {id, announcement_id, title, quantity, unit_price, line_subtotal}. */
data class OrderItem(
    val id: Long,
    val announcementId: Long?,
    val title: String?,
    val quantity: Double,
    val unitPrice: Double?,
    val lineSubtotal: Double?,
) {
    val lineTotal: Double get() = lineSubtotal ?: ((unitPrice ?: 0.0) * quantity)
}

/** Тапсырыстағы қарсы тарап (buyer/seller) — {user:{id, avatar_url, phone_number}, user_name}. */
data class OrderParty(
    val id: Long?,
    val name: String?,
    val avatarUrl: String?,
    val phone: String?,
)

/**
 * Тапсырыс — OrderModel: fulfillment (status) + payment_status тәуелсіз;
 * Smart Calculator өрістері (subtotal/delivery_fee/vat/items) мен legacy
 * жалпы өрістері (agreed_price/total_price) қатар өмір сүреді.
 */
data class Order(
    val id: Long,
    val announcementId: Long,
    val quantity: Double,
    val measurementUnit: String?,
    val status: String,
    val paymentStatus: String?,
    val buyerId: Long?,
    val sellerId: Long?,
    val agreedPrice: Double?,
    val totalPrice: Double?,
    val totalAmount: Double?,
    val pickupAddress: String?,
    val deliveryAddress: String?,
    val createdAt: String?,
    val paidAt: String?,
    val loadingDate: String?,
    val deliveryDate: String?,
    val updatedAt: String?,
    val currency: String?,
    val notes: String?,
    val announcement: CartAnnouncement?,
    val seller: OrderParty?,
    // ── Smart Calculator breakdown (GET /orders/{id}) ──
    val subtotal: Double?,
    val deliveryFee: Double?,
    val vatAmount: Double?,
    val vatRate: Double?,
    val items: List<OrderItem>,
    // ── Тізім жиынтығы (GET /orders/) ──
    val firstItemTitle: String?,
    val itemsCount: Int?,
) {
    val orderStatus: OrderStatus get() = OrderStatus.parse(status)
    val orderPaymentStatus: PaymentStatus get() = PaymentStatus.parse(paymentStatus)

    /** Smart Calculator тапсырысы ма (items[] толтырылған)? */
    val hasItemizedBreakdown: Boolean get() = items.isNotEmpty()

    /** Тауар құны — API subtotal > legacy agreed_price × quantity. */
    val goodsSubtotal: Double
        get() = subtotal ?: ((agreedPrice ?: totalPrice ?: 0.0) * quantity)

    val effectiveDeliveryFee: Double get() = deliveryFee ?: 0.0
    val effectiveVatAmount: Double get() = vatAmount ?: 0.0

    /** Толық сома — API total_amount > itemized (тауар+жеткізу+ҚҚС) > legacy. */
    val orderTotal: Double
        get() = totalAmount ?: if (hasItemizedBreakdown) {
            goodsSubtotal + effectiveDeliveryFee + effectiveVatAmount
        } else {
            (agreedPrice ?: totalPrice ?: 0.0) * quantity
        }

    /** Өзі алу ма — pickup_address толтырылған және delivery_address жоқ. */
    val isPickup: Boolean
        get() = !pickupAddress.isNullOrBlank() && deliveryAddress.isNullOrBlank()

    /** Төлем жүргізілген бе? */
    val isPaid: Boolean get() = orderPaymentStatus == PaymentStatus.PAID
}

/** Waybill (GET /orders/{id}/tracking) — жүргізуші/көлік деректері timeline-ға. */
data class Waybill(
    val id: Long,
    val waybillNumber: String,
    val vehicleNumber: String?,
    val driverName: String?,
)

/** Тапсырыс трекингі — waybills бос болса да бет жұмыс істейді (қате = үнсіз). */
data class OrderTracking(
    val orderId: Long,
    val status: String?,
    val waybills: List<Waybill>,
) {
    val driverInfo: String?
        get() = waybills.firstOrNull()?.let { wb ->
            listOfNotNull(
                wb.driverName?.takeIf { it.isNotBlank() },
                wb.vehicleNumber?.takeIf { it.isNotBlank() },
            ).joinToString(" • ").takeIf { it.isNotEmpty() }
        }
}