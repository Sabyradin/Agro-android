package com.agroland.feature.wallet.data

import com.agroland.core.network.json.JsonParser
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * GET /business/balance — әмиян балансы (Flutter DealerBalance):
 * {balance, pending, hold, currency}.
 */
data class WalletBalance(
    val balance: Double,
    val pending: Double,
    val hold: Double,
    val currency: String,
)

/**
 * Транзакция түрі — backend TransactionTypeEnum → accrual/withdraw/hold
 * (Flutter BalanceTxType.fromString атаулары толық сақталған).
 */
enum class TxType {
    ACCRUAL,
    WITHDRAW,
    HOLD,
    UNKNOWN;

    companion object {
        fun fromString(raw: String?): TxType = when (raw?.trim()?.lowercase()) {
            // Балансқа түсетін (ендіріс, +)
            "accrual", "credit", "dealer_credit", "order_payment",
            "balance_top_up", "withdrawal_rejected", "refund",
            -> ACCRUAL
            // Баланстан алынатын (шығыс, −)
            "withdraw", "withdrawal", "payout", "commission",
            "withdrawal_completed", "subscription_purchase", "promotion_spending",
            -> WITHDRAW
            // Ұсталған (процесте)
            "hold", "holding", "withdrawal_request" -> HOLD
            else -> UNKNOWN
        }
    }
}

/** GET /business/balance/transactions — ledger жазбасы. */
data class WalletTransaction(
    val id: Long,
    val type: TxType,
    val amount: Double,
    val createdAt: String?,
    val description: String?,
    val status: String?,
)

/** Парсер — бір ғана орыннан; {data:{...}} орамы да, түбір нысан да қолдауда. */
object WalletParser {

    private fun JsonObject.unwrapData(): JsonObject = JsonParser.obj(this, "data") ?: this

    fun parseBalance(root: JsonObject?): WalletBalance? {
        if (root == null) return null
        val data = root.unwrapData()
        return WalletBalance(
            balance = JsonParser.double(data, "balance") ?: 0.0,
            pending = JsonParser.double(data, "pending") ?: 0.0,
            hold = JsonParser.double(data, "hold") ?: 0.0,
            currency = JsonParser.string(data, "currency") ?: "KZT",
        )
    }

    /**
     * Ledger тізімі — пішіні тұрақсыз: {items:[...]}, {data:{items:[...]}},
     * түбірде жалғыз массив, немесе бүтіндей бос/бұрмаланған жауап (бос тізім).
     */
    fun parseTransactions(root: JsonElement?): List<WalletTransaction> = when (root) {
        is JsonArray -> parseList(root)
        is JsonObject -> {
            val data = root.unwrapData()
            val list = JsonParser.arr(data, "items")
                ?: JsonParser.arr(data, "transactions")
                ?: JsonParser.arr(root, "items")
            list?.let { parseList(it) } ?: emptyList()
        }
        else -> emptyList()
    }

    private fun parseList(list: JsonArray): List<WalletTransaction> =
        list.filterIsInstance<JsonObject>().map { tx ->
            WalletTransaction(
                id = JsonParser.long(tx, "id") ?: 0L,
                type = TxType.fromString(JsonParser.string(tx, "type")),
                amount = JsonParser.double(tx, "amount") ?: 0.0,
                createdAt = JsonParser.string(tx, "created_at"),
                description = JsonParser.string(tx, "description"),
                status = JsonParser.string(tx, "status"),
            )
        }
}