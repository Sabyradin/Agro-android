package com.agroland.feature.wallet.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WalletParser кешірімді парсингі: {data} орамы, ledger пішіндері, TxType мапингі. */
class WalletParserTest {

    private fun obj(raw: String) = Json.parseToJsonElement(raw).jsonObject

    // ── Баланс (GET /business/balance) ──

    @Test
    fun `баланс түбірден оқылады`() {
        val balance = WalletParser.parseBalance(
            obj(
                """
                {
                  "balance": 12500,
                  "pending": 3000,
                  "hold": 500,
                  "currency": "KZT"
                }
                """.trimIndent(),
            ),
        )

        assertEquals(12500.0, balance!!.balance, 0.001)
        assertEquals(3000.0, balance.pending, 0.001)
        assertEquals(500.0, balance.hold, 0.001)
        assertEquals("KZT", balance.currency)
    }

    @Test
    fun `баланс data орамында өрістер жоқ болса нөлдермен толады`() {
        val balance = WalletParser.parseBalance(obj("""{"data": {"balance": 100}}"""))

        assertEquals(100.0, balance!!.balance, 0.001)
        assertEquals(0.0, balance.pending, 0.001)
        assertEquals(0.0, balance.hold, 0.001)
        assertEquals("KZT", balance.currency)
    }

    @Test
    fun `толық бос жауап баланс парсіне жол бермейді`() {
        assertNull(WalletParser.parseBalance(null))
    }

    // ── Ledger (GET /business/balance/transactions) ──

    @Test
    fun `транзакциялар items өрісінен оқылады`() {
        val list = WalletParser.parseTransactions(
            obj(
                """
                {
                  "items": [
                    {
                      "id": 1,
                      "type": "dealer_credit",
                      "amount": 2500,
                      "created_at": "2026-09-15T10:30:00",
                      "description": "Трактор сатылды"
                    },
                    {
                      "id": 2,
                      "type": "withdrawal_request",
                      "amount": 5000,
                      "status": "pending"
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals(2, list.size)
        assertEquals(TxType.ACCRUAL, list[0].type)
        assertEquals(2500.0, list[0].amount, 0.001)
        assertEquals("Трактор сатылды", list[0].description)
        assertEquals(TxType.HOLD, list[1].type)
        assertEquals("pending", list[1].status)
    }

    @Test
    fun `транзакциялар data орамының ішіндегі items оқылады`() {
        val list = WalletParser.parseTransactions(
            obj(
                """
                {"data": {"items": [{"id": 7, "type": "promotion_spending", "amount": 1500}]}}
                """.trimIndent(),
            ),
        )

        assertEquals(1, list.size)
        assertEquals(TxType.WITHDRAW, list[0].type)
        assertEquals(7L, list[0].id)
    }

    @Test
    fun `түбірдегі жалаң массив де қолдау табады`() {
        val list = WalletParser.parseTransactions(
            Json.parseToJsonElement("""[{"id": 1, "type": "refund", "amount": 900}]"""),
        )

        assertEquals(1, list.size)
        assertEquals(TxType.ACCRUAL, list[0].type)
    }

    @Test
    fun `бұрмаланған жауап бос тізім береді`() {
        assertTrue(WalletParser.parseTransactions(null).isEmpty())
        assertTrue(WalletParser.parseTransactions(obj("""{"items": "oops"}""")).isEmpty())
        assertTrue(WalletParser.parseTransactions(obj("""{"success": true}""")).isEmpty())
    }

    // ── TxType атау мапингі (Flutter BalanceTxType.fromString) ──

    @Test
    fun `tx type атаулары толық мапингтеледі`() {
        assertEquals(TxType.ACCRUAL, TxType.fromString("credit"))
        assertEquals(TxType.ACCRUAL, TxType.fromString("order_payment"))
        assertEquals(TxType.ACCRUAL, TxType.fromString("balance_top_up"))
        assertEquals(TxType.ACCRUAL, TxType.fromString("withdrawal_rejected"))
        assertEquals(TxType.WITHDRAW, TxType.fromString("withdrawal"))
        assertEquals(TxType.WITHDRAW, TxType.fromString("payout"))
        assertEquals(TxType.WITHDRAW, TxType.fromString("commission"))
        assertEquals(TxType.WITHDRAW, TxType.fromString("subscription_purchase"))
        assertEquals(TxType.HOLD, TxType.fromString("holding"))
        assertEquals(TxType.UNKNOWN, TxType.fromString("something_new"))
        assertEquals(TxType.UNKNOWN, TxType.fromString(null))
    }

    @Test
    fun `tx type регистрі мәнсіз`() {
        assertEquals(TxType.ACCRUAL, TxType.fromString("Dealer_Credit"))
    }

    // ── Шығару денесі (POST /business/withdraw) ──

    @Test
    fun `withdraw денесі қажетті өрістерді құрады`() {
        val body = WalletRequests.withdraw(
            amount = 7500.0,
            iban = "KZ001150013549876543",
            bankName = "Kaspi Bank",
            bik = "ALBNKZKA",
            account = null,
        )

        assertEquals(7500.0, body["amount"].toString().toDouble(), 0.001)
        val details = body["bank_details"]!!.toString()
        assertTrue(details.contains("KZ001150013549876543"))
        assertTrue(details.contains("Kaspi Bank"))
        assertTrue(details.contains("ALBNKZKA"))
        assertFalse(details.contains("account"))
    }
}