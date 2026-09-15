package com.agroland.feature.cart.data

/**
 * Себет бетінің 5 бөлімі (Flutter CartSection) + әрқайсысының OrdersQuery сүзгісі.
 * BASKET_ITEMS — себеттің өзі; қалғандары — buyer тапсырыстарының көріністері.
 */
enum class CartSection {
    BASKET_ITEMS,
    PAID_PENDING,
    IN_PROGRESS,
    DELIVERED,
    ORDER_HISTORY,
}

/** GET /orders/ сұрауы — role/status/payment_status (үшеу де опционал). */
data class OrdersQuery(
    val role: String? = null,
    val status: String? = null,
    val paymentStatus: String? = null,
) {
    fun toQueryMap(): Map<String, String> = buildMap {
        role?.let { put("role", it) }
        status?.let { put("status", it) }
        paymentStatus?.let { put("payment_status", it) }
    }
}

/** Бөлім → сұрау (BASKET_ITEMS → null — тапсырыс емес, себеттің өзі). */
fun sectionQuery(section: CartSection): OrdersQuery? = when (section) {
    CartSection.BASKET_ITEMS -> null
    CartSection.PAID_PENDING -> OrdersQuery(
        role = "buyer",
        paymentStatus = "paid",
        status = "supplier_query",
    )
    CartSection.IN_PROGRESS -> OrdersQuery(
        role = "buyer",
        paymentStatus = "paid",
        status = "supplier_confirmed,logistics_search,logistics_assigned,logistics_enroute,loading,loaded",
    )
    CartSection.DELIVERED -> OrdersQuery(role = "buyer", status = "delivered,act_signed")
    CartSection.ORDER_HISTORY -> OrdersQuery(role = "buyer")
}

/**
 * Backend сұрауынан кейінгі клиенттік жиынтық сүзгі (Flutter _buildSectionContent
 * filter керіліктері) — бөлім мәнімметірасын дәлелдеу үшін қолданылады.
 */
fun sectionClientFilter(section: CartSection, orders: List<Order>): List<Order> = when (section) {
    CartSection.BASKET_ITEMS -> orders
    CartSection.PAID_PENDING -> orders.filter { it.orderStatus == OrderStatus.SUPPLIER_QUERY }
    CartSection.IN_PROGRESS -> orders.filter {
        it.orderStatus !in listOf(
            OrderStatus.SUPPLIER_QUERY,
            OrderStatus.DELIVERED,
            OrderStatus.ACT_SIGNED,
            OrderStatus.PAID,
            OrderStatus.CANCELLED,
        )
    }
    CartSection.DELIVERED -> orders.filter {
        it.orderStatus == OrderStatus.DELIVERED || it.orderStatus == OrderStatus.ACT_SIGNED
    }
    CartSection.ORDER_HISTORY -> orders
}

/** «Тауарды қабылдау» батырмасы — жолда/жүктелді күйлерінде (backend тізбегі). */
val Order.canConfirmReceipt: Boolean
    get() = orderStatus == OrderStatus.LOGISTICS_ENROUTE || orderStatus == OrderStatus.LOADED

/** confirmDelivery өтпелері: ағымдағы статус → «delivered»-ге дейінгі қадамдар. */
fun confirmDeliverySteps(currentStatus: String): List<String> = when (currentStatus) {
    "logistics_enroute" -> listOf("loading", "loaded", "delivered")
    "loading" -> listOf("loaded", "delivered")
    "loaded" -> listOf("delivered")
    "delivered" -> emptyList()
    else -> listOf("delivered")
}