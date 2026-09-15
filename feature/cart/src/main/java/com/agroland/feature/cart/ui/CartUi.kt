package com.agroland.feature.cart.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.agroland.core.network.error.Failure
import com.agroland.feature.cart.data.Order
import com.agroland.feature.cart.data.OrderStatus
import com.agroland.core.l10n.R as L10nR

/**
 * UI-ға берілетін қате: backend адам тіліндегі message, болмаса UI локализацияланған
 * generic мәтінді таңдайды. Шикі error_code ешқашан көрсетілмейді.
 */
data class CartError(
    val backendMessage: String? = null,
    val isNetwork: Boolean = false,
    /** MULTI_SUPPLIER_CART — SupplierPicker қайта ашылады. */
    val isMultiSupplier: Boolean = false,
)

fun CartError.displayText(networkMessage: String, genericMessage: String): String =
    backendMessage ?: if (isNetwork) networkMessage else genericMessage

/** Қате мәтіні: backend хабарламасы > локализацияланған fallback. Ешқашан error_code. */
@Composable
fun CartError.displayText(): String = displayText(
    networkMessage = stringResource(L10nR.string.error_no_internet),
    genericMessage = stringResource(L10nR.string.error_generic_message),
)

fun Failure.toCartError(): CartError = when (this) {
    is Failure.Network -> CartError(isNetwork = true)
    else -> CartError(
        backendMessage = (this as? Failure.Http)?.error?.message,
        isMultiSupplier = this is Failure.Http && error.code == "MULTI_SUPPLIER_CART",
    )
}

/** Себет экрандарының бір реттелген оқиғалары. */
sealed interface CartEvent {
    data class ShowError(val error: CartError) : CartEvent

    /** Чекаут сәтті — orderIds + таңдалған тауарлардың сомасы (төлем парағына). */
    data class CheckoutDone(val orderIds: List<Long>, val totalAmount: Double) : CartEvent
    data object AddedToCart : CartEvent
    data object ReorderDone : CartEvent
    data object ReceiptConfirmed : CartEvent
    data object OrderCancelled : CartEvent
}

/** Статус түсі (Flutter statusColor): orange/blue/teal/green/red. */
fun OrderStatus.statusColor(): Color = when (this) {
    OrderStatus.SUPPLIER_QUERY -> Color(0xFFF57C00)
    OrderStatus.SUPPLIER_CONFIRMED,
    OrderStatus.LOGISTICS_SEARCH,
    OrderStatus.LOGISTICS_ASSIGNED,
    -> Color(0xFF1976D2)
    OrderStatus.LOGISTICS_ENROUTE,
    OrderStatus.LOADING,
    OrderStatus.LOADED,
    -> Color(0xFF00796B)
    OrderStatus.DELIVERED,
    OrderStatus.ACT_SIGNED,
    OrderStatus.PAID,
    -> Color(0xFF388E3C)
    OrderStatus.CANCELLED -> Color(0xFFD32F2F)
}

/** Статус мәтіні — локализацияланған түрлендіру кестесі. */
fun OrderStatus.labelRes(): Int = when (this) {
    OrderStatus.SUPPLIER_QUERY -> L10nR.string.order_status_supplier_query
    OrderStatus.SUPPLIER_CONFIRMED -> L10nR.string.order_status_supplier_confirmed
    OrderStatus.LOGISTICS_SEARCH -> L10nR.string.order_status_logistics_search
    OrderStatus.LOGISTICS_ASSIGNED -> L10nR.string.order_status_logistics_assigned
    OrderStatus.LOGISTICS_ENROUTE -> L10nR.string.order_status_logistics_enroute
    OrderStatus.LOADING -> L10nR.string.order_status_loading
    OrderStatus.LOADED -> L10nR.string.order_status_loaded
    OrderStatus.DELIVERED -> L10nR.string.order_status_delivered
    OrderStatus.ACT_SIGNED -> L10nR.string.order_status_act_signed
    OrderStatus.PAID -> L10nR.string.order_status_paid
    OrderStatus.CANCELLED -> L10nR.string.order_status_cancelled
}

/** Тапсырыстың көрсетілетін мәтіні: fulfillment статус → payment fallback → unknown. */
fun Order.statusLabelRes(): Int = when {
    orderStatus != OrderStatus.SUPPLIER_QUERY -> orderStatus.labelRes()
    orderPaymentStatus == com.agroland.feature.cart.data.PaymentStatus.PAID ->
        L10nR.string.order_status_paid
    else -> L10nR.string.order_status_supplier_query
}