package com.agroland.feature.payment.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** PaymentParser кешірімді парсингі: {data} орамы, mock, поллинг күйі, HTML. */
class PaymentParserTest {

    private fun obj(raw: String) = Json.parseToJsonElement(raw).jsonObject

    // ── Баланспен төлеу (POST /payments/from-balance) ──

    @Test
    fun `баланспен төлеу нәтижесі түбірден оқылады`() {
        val result = PaymentParser.parseBalanceResult(
            obj(
                """
                {
                  "status": "paid",
                  "method": "balance",
                  "order_id": 42,
                  "amount": 12500,
                  "buyer_balance_after": 7500
                }
                """.trimIndent(),
            ),
        )

        assertEquals(42L, result!!.orderId)
        assertEquals("paid", result.status)
        assertEquals("balance", result.method)
        assertEquals(12500.0, result.amount, 0.001)
        assertEquals(7500.0, result.buyerBalanceAfter, 0.001)
    }

    @Test
    fun `баланспен төлеу нәтижесі data орамынан оқылады`() {
        val result = PaymentParser.parseBalanceResult(
            obj("""{"data": {"status": "paid", "method": "balance", "order_id": 7, "amount": 100, "buyer_balance_after": 0}}"""),
        )

        assertEquals(7L, result!!.orderId)
        assertEquals(0.0, result.buyerBalanceAfter, 0.001)
    }

    @Test
    fun `order_id жоқ болса нәтиже null қайтарады`() {
        assertNull(PaymentParser.parseBalanceResult(obj("""{"status": "paid"}""")))
    }

    // ── Halyk init (POST /payments/halyk/init) ──

    @Test
    fun `halyk init payment_url invoice_id mock оқылады`() {
        val init = PaymentParser.parseHalykInit(
            obj(
                """
                {
                  "payment_url": "https://epay.halyk.bank/pay/abc123",
                  "invoice_id": 9001,
                  "amount": 25000,
                  "currency": "KZT",
                  "mock": false
                }
                """.trimIndent(),
            ),
        )

        assertEquals("https://epay.halyk.bank/pay/abc123", init!!.paymentUrl)
        assertEquals(9001L, init.invoiceId)
        assertEquals(25000.0, init.amount, 0.001)
        assertEquals("KZT", init.currency)
        assertFalse(init.mock)
    }

    @Test
    fun `halyk init data орамында mock режим белгісімен оқылады`() {
        val init = PaymentParser.parseHalykInit(
            obj(
                """
                {"data": {"payment_url": "https://mock.local/pay", "invoice_id": 1, "amount": 5, "mock": true}}
                """.trimIndent(),
            ),
        )

        assertTrue(init!!.mock)
        // currency әдепкі — KZT.
        assertEquals("KZT", init.currency)
    }

    @Test
    fun `payment_url жоқ болса init null қайтарады`() {
        assertNull(PaymentParser.parseHalykInit(obj("""{"invoice_id": 1}""")))
    }

    // ── Төлем күйінің поллингі (GET /orders/{id}) ──

    @Test
    fun `поллинг payment_status бойынша төленді деп танылады`() {
        val state = PaymentParser.parseOrderPaymentState(
            obj("""{"id": 42, "payment_status": "paid", "status": "supplier_query", "total_amount": 12500, "currency": "₸"}"""),
        )

        assertTrue(state!!.isPaid)
        assertEquals(42L, state.orderId)
        assertEquals(12500.0, state.totalAmount!!, 0.001)
    }

    @Test
    fun `поллинг ескі тапсырыста status fallback арқылы төленді деп танылады`() {
        val state = PaymentParser.parseOrderPaymentState(
            obj("""{"id": 5, "status": "paid"}"""),
        )

        assertTrue(state!!.isPaid)
        // total_amount жоқ — null, валюта әдепкі ₸.
        assertNull(state.totalAmount)
        assertEquals("₸", state.currency)
    }

    @Test
    fun `поллинг төленбеген тапсырысты төленбеген деп қалдырады`() {
        val state = PaymentParser.parseOrderPaymentState(
            obj("""{"id": 9, "payment_status": "unpaid", "status": "supplier_query"}"""),
        )

        assertFalse(state!!.isPaid)
    }

    // ── BCC legacy 3D Secure формасы (POST /payment/generate) ──

    @Test
    fun `generate жауабы шикі JSON жол ретінде HTML қайтарады`() {
        val html = PaymentParser.parseGenerateHtml(
            Json.parseToJsonElement("\"<form action=\\\"bcc\\\">P_SIGN...</form>\""),
        )

        assertTrue(html!!.contains("P_SIGN"))
    }

    @Test
    fun `generate жауабы data өрісінен HTML оқылады`() {
        val html = PaymentParser.parseGenerateHtml(
            obj("""{"data": "<html>topup</html>"}"""),
        )

        assertEquals("<html>topup</html>", html)
    }

    @Test
    fun `generate бос жауабы null қайтарады`() {
        assertNull(PaymentParser.parseGenerateHtml(null))
        assertNull(PaymentParser.parseGenerateHtml(obj("""{"data": ""}""")))
    }
}