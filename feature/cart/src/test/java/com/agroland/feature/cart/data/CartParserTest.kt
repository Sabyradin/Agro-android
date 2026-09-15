package com.agroland.feature.cart.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** CartParser кешірімді парсингі + CartRequests денелері + бөлім сүзгілері. */
class CartParserTest {

    private fun obj(raw: String) = Json.parseToJsonElement(raw).jsonObject

    // ── Себет ──

    @Test
    fun `себет items нысанынан оқылады ішкі жарнамамен және зоналармен`() {
        val root = obj(
            """
            {
              "items": [
                {
                  "id": 10,
                  "announcement_id": 55,
                  "quantity": 2.5,
                  "measurement_unit": "т",
                  "announcement": {
                    "id": 55,
                    "title": "Бидай",
                    "price": 120000,
                    "allow_cart": true,
                    "type_ad": "dealer",
                    "delivery_available": true,
                    "pickup_available": true,
                    "pickup_address": "Алматы, базар",
                    "delivery_zones": [
                      {
                        "id": 7,
                        "delivery_cost": 2500,
                        "region_name": "Алматы облысы",
                        "delivery_days_min": 1,
                        "delivery_days_max": 3
                      }
                    ]
                  }
                },
                {"id": 11, "announcement_id": 56, "quantity": 1}
              ]
            }
            """.trimIndent(),
        )
        val cart = CartParser.parseCart(root)

        assertEquals(2, cart.size)
        val first = cart[0]
        assertEquals(10L, first.id)
        assertEquals(55L, first.announcementId)
        assertEquals(2.5, first.quantity, 0.001)
        assertEquals("т", first.measurementUnit)
        val announcement = first.announcement!!
        assertEquals("Бидай", announcement.base.title)
        assertTrue(announcement.base.deliveryAvailable)
        assertEquals(1, announcement.deliveryZones.size)
        val zone = announcement.deliveryZones[0]
        assertEquals(7L, zone.id)
        assertEquals(2500.0, zone.deliveryCost, 0.001)
        assertEquals("Алматы облысы", zone.displayName)
        assertEquals("1–3", zone.daysRangeRaw)
        // Екінші айтем announcement-сыз — enrichment талабымен келеді.
        assertNull(cart[1].announcement)
        assertNull(cart[1].authorId)
    }

    @Test
    fun `себет тікелей массив түрінде де оқылады`() {
        val root = Json.parseToJsonElement("""[{"id": 3, "announcement_id": 4, "quantity": 1}]""").jsonArray
        val cart = CartParser.parseCart(root)
        assertEquals(1, cart.size)
        assertEquals(3L, cart[0].id)
    }

    // ── Preview (Smart Calculator) ──

    @Test
    fun `preview топтары мен ескертулері оқылады`() {
        val preview = CartParser.parseCartPreview(
            obj(
                """
                {
                  "grand_total": 150500,
                  "groups": [
                    {
                      "supplier_id": 9,
                      "supplier_name": "АгроБизнес",
                      "is_vat_payer": true,
                      "goods_subtotal": 100000,
                      "delivery_fee": 2500,
                      "vat_rate": 12,
                      "vat_amount": 48000,
                      "vat_included_in_price": true,
                      "total": 150500
                    }
                  ],
                  "warnings": ["", "Жеткізу кейбір аудандарға қолжетімсіз"]
                }
                """.trimIndent(),
            ),
        )!!
        assertEquals(150500.0, preview.grandTotal, 0.001)
        assertEquals(1, preview.groups.size)
        val group = preview.groups[0]
        assertEquals(9L, group.supplierId)
        assertEquals("АгроБизнес", group.supplierName)
        assertTrue(group.isVatPayer)
        assertTrue(preview.hasVat)
        // Бос warning құлады.
        assertEquals(1, preview.warnings.size)
    }

    // ── Тапсырыс ──

    @Test
    fun `тапсырыс Smart Calculator өрістерімен оқылады`() {
        val order = CartParser.parseOrder(
            obj(
                """
                {
                  "id": 77,
                  "announcement_id": 55,
                  "quantity": 3,
                  "measurement_unit": "т",
                  "status": "logistics_enroute",
                  "payment_status": "paid",
                  "supplier_id": 9,
                  "delivery_address": "Алматы, Абая 10",
                  "subtotal": 300000,
                  "delivery_fee": 5000,
                  "vat_rate": 12,
                  "vat_amount": 36000,
                  "total_amount": 341000,
                  "items": [
                    {"id": 1, "announcement_id": 55, "title": "Бидай", "quantity": 3, "unit_price": 100000, "line_subtotal": 300000}
                  ],
                  "seller": {
                    "user_name": "АгроБизнес",
                    "user": {"id": 9, "avatar_url": "/media/a.png", "phone_number": "+7 777 111 22 33"}
                  },
                  "created_at": "2026-09-01T08:00:00Z",
                  "paid_at": "2026-09-01T08:05:00Z"
                }
                """.trimIndent(),
            ),
        )!!
        assertEquals(77L, order.id)
        assertEquals(OrderStatus.LOGISTICS_ENROUTE, order.orderStatus)
        assertEquals(PaymentStatus.PAID, order.orderPaymentStatus)
        assertTrue(order.hasItemizedBreakdown)
        assertEquals(300000.0, order.goodsSubtotal, 0.001)
        assertEquals(5000.0, order.effectiveDeliveryFee, 0.001)
        assertEquals(341000.0, order.orderTotal, 0.001)
        assertFalse(order.isPickup)
        assertTrue(order.isPaid)
        assertEquals("АгроБизнес", order.seller!!.name)
        assertEquals("+7 777 111 22 33", order.seller!!.phone)
        assertEquals(36000.0, order.effectiveVatAmount, 0.001)
        // canConfirmReceipt — жолда күйінде.
        assertTrue(order.canConfirmReceipt)
    }

    @Test
    fun `тапсырыс legacy өрістерімен жалпы сомаға қайта ыдырайды`() {
        val order = CartParser.parseOrder(
            obj(
                """
                {
                  "id": 78,
                  "announcement_id": 55,
                  "quantity": 2,
                  "status": "supplier_query",
                  "agreed_price": 50000,
                  "pickup_address": "базар",
                  "payment_status": "unpaid"
                }
                """.trimIndent(),
            ),
        )!!
        assertFalse(order.hasItemizedBreakdown)
        assertEquals(100000.0, order.goodsSubtotal, 0.001)
        assertEquals(100000.0, order.orderTotal, 0.001)
        assertTrue(order.isPickup)
        assertFalse(order.isPaid)
        // supplier_query — қабылдау мүмкін емес.
        assertFalse(order.canConfirmReceipt)
    }

    @Test
    fun `белгісіз статус SUPPLIER_QUERY-ге ыдырайды`() {
        val order = CartParser.parseOrder(
            obj("""{"id": 79, "announcement_id": 1, "quantity": 1, "status": "brand_new_state"}"""),
        )!!
        assertEquals(OrderStatus.SUPPLIER_QUERY, order.orderStatus)
        assertNull(OrderStatus.tryParse("brand_new_state"))
    }

    // ── CheckoutResult ──

    @Test
    fun `checkout order_ids массив және жалғыз сан түрінде`() {
        val many = CartParser.parseCheckoutResult(obj("""{"order_ids": [10, 20]}"""))
        assertEquals(listOf(10L, 20L), many.orderIds)
        val single = CartParser.parseCheckoutResult(obj("""{"order_ids": 33}"""))
        assertEquals(listOf(33L), single.orderIds)
        val empty = CartParser.parseCheckoutResult(obj("""{"warnings": []}"""))
        assertTrue(empty.orderIds.isEmpty())
    }

    // ── Трекинг ──

    @Test
    fun `tracking жүргізуші ақпараты біріктіріледі`() {
        val tracking = CartParser.parseOrderTracking(
            obj(
                """
                {
                  "order_id": 77,
                  "status": "logistics_enroute",
                  "waybills": [
                    {"id": 1, "waybill_number": "WB-1", "vehicle_number": "123ABC", "driver_name": "Ерлан"}
                  ]
                }
                """.trimIndent(),
            ),
        )
        assertEquals(77L, tracking.orderId)
        assertEquals(1, tracking.waybills.size)
        assertEquals("Ерлан • 123ABC", tracking.driverInfo)
    }

    // ── CartRequests ──

    @Test
    fun `checkout денесі тек толтырылған өрістермен құрылады`() {
        val body = CartRequests.checkout(
            deliveryDistrictId = 12,
            pickup = false,
            deliveryAddress = "Алматы",
            lines = listOf(CheckoutLine(10, 7L), CheckoutLine(11, null)),
        )
        assertEquals(12, body["delivery_district_id"]!!.toString().toInt())
        assertEquals("Алматы", body["delivery_address"]!!.toString().trim('"'))
        val items = body["items"]!!.jsonArray
        assertEquals(2, items.size)
        val line = items[0].jsonObject
        assertEquals(10, line["cart_item_id"]!!.toString().toInt())
        assertEquals(7, line["delivery_zone_id"]!!.toString().toLong())
        // zone жоқ жолда delivery_zone_id болмайды.
        assertFalse("delivery_zone_id" in items[1].jsonObject)
        // only_supplier_id жіберілмейді.
        assertFalse("only_supplier_id" in body)
    }

    @Test
    fun `pickup чекаут бос дене жібереді`() {
        val body = CartRequests.checkout()
        assertTrue(body.keys.isEmpty())
    }

    // ── Статус тізбегі ──

    @Test
    fun `confirmDeliverySteps аралық статустарды тізбектейді`() {
        assertEquals(listOf("loading", "loaded", "delivered"), confirmDeliverySteps("logistics_enroute"))
        assertEquals(listOf("loaded", "delivered"), confirmDeliverySteps("loading"))
        assertEquals(listOf("delivered"), confirmDeliverySteps("loaded"))
        assertEquals(emptyList<String>(), confirmDeliverySteps("delivered"))
        assertEquals(listOf("delivered"), confirmDeliverySteps("supplier_confirmed"))
    }

    // ── Бөлім сүзгілері ──

    @Test
    fun `бөлім сұраулары buyer рөлімен келеді`() {
        assertNull(sectionQuery(CartSection.BASKET_ITEMS))
        val paid = sectionQuery(CartSection.PAID_PENDING)!!
        assertEquals("buyer", paid.role)
        assertEquals("paid", paid.paymentStatus)
        assertEquals("supplier_query", paid.status)
        val progress = sectionQuery(CartSection.IN_PROGRESS)!!
        assertTrue(progress.status!!.split(",").contains("logistics_enroute"))
        assertEquals("buyer", sectionQuery(CartSection.DELIVERED)!!.role)
        assertNull(sectionQuery(CartSection.ORDER_HISTORY)!!.status)
    }

    @Test
    fun `клиенттік сүзгі бөлім мәнін дәлелдейді`() {
        fun order(status: String, paymentStatus: String = "paid") = Order(
            id = 1, announcementId = 1, quantity = 1.0, measurementUnit = null,
            status = status, paymentStatus = paymentStatus, buyerId = null, sellerId = null,
            agreedPrice = null, totalPrice = null, totalAmount = null,
            pickupAddress = null, deliveryAddress = null, createdAt = null, paidAt = null,
            loadingDate = null, deliveryDate = null, updatedAt = null, currency = null,
            notes = null, announcement = null, seller = null, subtotal = null,
            deliveryFee = null, vatAmount = null, vatRate = null, items = emptyList(),
            firstItemTitle = null, itemsCount = null,
        )
        val all = listOf(
            order("supplier_query"), order("supplier_confirmed"), order("logistics_enroute"),
            order("delivered"), order("act_signed"), order("cancelled"),
        )
        assertEquals(
            listOf("supplier_query"),
            sectionClientFilter(CartSection.PAID_PENDING, all).map { it.status },
        )
        assertEquals(
            listOf("supplier_confirmed", "logistics_enroute"),
            sectionClientFilter(CartSection.IN_PROGRESS, all).map { it.status },
        )
        assertEquals(
            listOf("delivered", "act_signed"),
            sectionClientFilter(CartSection.DELIVERED, all).map { it.status },
        )
        assertEquals(all.size, sectionClientFilter(CartSection.ORDER_HISTORY, all).size)
    }
}