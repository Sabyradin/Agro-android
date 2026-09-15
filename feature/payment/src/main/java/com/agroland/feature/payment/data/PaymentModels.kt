package com.agroland.feature.payment.data

import com.agroland.core.network.json.JsonParser
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * POST /payments/from-balance нәтижесі — BalancePaymentResult (Flutter):
 * {status, method, order_id, amount, buyer_balance_after}.
 */
data class BalancePaymentResult(
    val status: String,
    val method: String,
    val orderId: Long,
    val amount: Double,
    val buyerBalanceAfter: Double,
)

/**
 * POST /payments/halyk/init нәтижесі — HalykPaymentInitResult (Flutter):
 * {payment_url, invoice_id, amount, currency, mock}. mock=true — DEV ортада
 * WebView-сіз тікелей нәтиже бетіне жүреді (backend тапсырысты төленді деп белгілейді).
 */
data class HalykPaymentInit(
    val paymentUrl: String,
    val invoiceId: Long,
    val amount: Double,
    val currency: String,
    val mock: Boolean,
)

/**
 * GET /orders/{id} поллинг күйі (PaymentResultPage): тек төлем расталуын
 * тексереді — payment_status, болмаса status fallback (Flutter мінез-құлығы).
 */
data class OrderPaymentState(
    val orderId: Long,
    val paymentStatus: String?,
    val status: String?,
    val totalAmount: Double?,
    val currency: String,
) {
    /** Төленді ме: payment_status == paid немесе ескі тапсырыстарда status == paid. */
    val isPaid: Boolean
        get() = paymentStatus.equals("paid", ignoreCase = true) ||
            status.equals("paid", ignoreCase = true)
}

/** Парсер — бір ғана орыннан; {data:{...}} орамы да, түбір нысан да қолдауда. */
object PaymentParser {

    private fun JsonObject.unwrapData(): JsonObject = JsonParser.obj(this, "data") ?: this

    fun parseBalanceResult(root: JsonObject?): BalancePaymentResult? {
        if (root == null) return null
        val data = root.unwrapData()
        return BalancePaymentResult(
            status = JsonParser.string(data, "status") ?: "",
            method = JsonParser.string(data, "method") ?: "",
            orderId = JsonParser.long(data, "order_id") ?: return null,
            amount = JsonParser.double(data, "amount") ?: 0.0,
            buyerBalanceAfter = JsonParser.double(data, "buyer_balance_after") ?: 0.0,
        )
    }

    fun parseHalykInit(root: JsonObject?): HalykPaymentInit? {
        if (root == null) return null
        val data = root.unwrapData()
        return HalykPaymentInit(
            paymentUrl = JsonParser.string(data, "payment_url") ?: return null,
            invoiceId = JsonParser.long(data, "invoice_id") ?: 0L,
            amount = JsonParser.double(data, "amount") ?: 0.0,
            currency = JsonParser.string(data, "currency") ?: "KZT",
            mock = JsonParser.bool(data, "mock") ?: false,
        )
    }

    /** Поллинг үшін тапсырыстың төлем күйі — толық тапсырыс нысанынан қажетті өрістер. */
    fun parseOrderPaymentState(root: JsonObject?): OrderPaymentState? {
        if (root == null) return null
        val id = JsonParser.long(root, "id") ?: return null
        return OrderPaymentState(
            orderId = id,
            paymentStatus = JsonParser.string(root, "payment_status"),
            status = JsonParser.string(root, "status"),
            totalAmount = JsonParser.double(root, "total_amount")
                ?: JsonParser.double(root, "total_price"),
            currency = JsonParser.string(root, "currency") ?: "₸",
        )
    }

    /**
     * POST /payment/generate (BCC legacy 3D Secure формасы) — жауап шикі HTML
     * жолды (JSON string) немесе {data: "html"} нысанын қайтаруы мүмкін.
     * Backend P_SIGN (HMAC-SHA1) өзі құрады — клиент тек WebView-та көрсетеді.
     */
    fun parseGenerateHtml(element: JsonElement?): String? = when (element) {
        null -> null
        is JsonPrimitive -> element.content.takeIf { it.isNotBlank() }
        is JsonObject ->
            listOfNotNull(
                JsonParser.string(element, "data"),
                JsonParser.string(element, "html"),
                JsonParser.string(element, "payment_url"),
            ).firstOrNull { it.isNotBlank() }
        else -> null
    }
}