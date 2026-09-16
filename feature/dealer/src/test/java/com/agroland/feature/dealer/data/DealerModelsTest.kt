package com.agroland.feature.dealer.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DealerParser — Фаза 16: backend тип тұрақсыздығына төзімділік
 * (tab_counts string сандар, bare list / бір объект zone, {data:{points}}
 * қаптамасы, profilePicture camelCase, tariff fallback merge).
 */
class DealerModelsTest {

    private fun obj(raw: String) = Json.parseToJsonElement(raw).jsonObject

    // ── tab_counts: string «5», double, бүлдірген → {} ──

    @Test
    fun `tab_counts string және double санды шыдайды`() {
        val response = DealerParser.parseOrders(
            obj(
                """
                {"tab": "new", "items": [], "total": 7, "page": 1, "pages": 1,
                 "tab_counts": {"new": "5", "confirmed": 2.0, "delivered": "abc"}}
                """.trimIndent(),
            ),
        )
        assertEquals("new", response.tab)
        assertEquals(5, response.tabCounts["new"]) // string «5» → 5
        assertEquals(2, response.tabCounts["confirmed"]) // double 2.0 → 2
        assertFalse("delivered" in response.tabCounts) // «abc» алынып тасталады
        assertFalse(response.canLoadMore)
    }

    @Test
    fun `tab_counts жоқ болса бос карта береді және crash жасамайды`() {
        val response = DealerParser.parseOrders(obj("""{"tab": "confirmed", "items": []}"""))
        assertTrue(response.tabCounts.isEmpty())
        assertEquals("confirmed", response.tab)
        assertFalse(response.canLoadMore)
    }

    // ── Zones: bare list, бір объект (POST/PATCH жауабы), zone_prices ──

    @Test
    fun `zones bare list және бір объектті де оқиды`() {
        val list = DealerParser.parseZones(
            Json.parseToJsonElement(
                """
                [{"id": 1, "name": "Астана", "delivery_cost": 1500, "country_id": 1,
                  "region_id": 10, "is_active": true, "available_days": [1, 3, 5]}]
                """.trimIndent(),
            ).jsonArray,
        )
        assertEquals(1, list.size)
        assertEquals("Астана", list[0].name)
        assertEquals(1500.0, list[0].deliveryCost, 0.01)
        assertEquals(listOf(1, 3, 5), list[0].availableDays)
        assertEquals("Астана", list[0].displayName)

        // POST/PATCH жауабы — бір объект, id бар.
        val single = DealerParser.parseZones(
            obj("""{"id": 9, "name": "Қарағанды", "delivery_cost": 2000}"""),
        )
        assertEquals(1, single.size)
        assertEquals(9L, single[0].id)
        assertEquals("Қарағанды", single[0].name)
    }

    @Test
    fun `zone_prices district name_ru оқиды және daysRange пішімдейді`() {
        val zone = DealerParser.parseZones(
            obj(
                """
                {"id": 2, "region_name": "Астана", "delivery_days_min": 1,
                 "delivery_days_max": 3, "zone_prices": [
                   {"district_id": 100, "district": {"name_ru": "Есіл"}, "delivery_cost": 900}
                 ]}
                """.trimIndent(),
            ),
        ).single()
        assertEquals("Астана", zone.displayName) // name жоқ → region
        assertEquals("1–3", zone.daysRange)
        assertEquals(1, zone.zonePrices.size)
        assertEquals("Есіл", zone.zonePrices[0].districtName)
        assertEquals(900.0, zone.zonePrices[0].price, 0.01)
        assertEquals(100, zone.zonePrices[0].districtId)
    }

    // ── Timeseries: {data:{points}} қаптамасы, бүлдірген → бос ──

    @Test
    fun `timeseries data points қаптамасын ашады`() {
        val points = DealerParser.parseTimeseries(
            obj(
                """
                {"data": {"points": [
                  {"date": "2026-09-01", "revenue": 12500.5, "orders": 3},
                  {"date": "2026-09-02", "revenue": 0, "orders": 0}
                ]}}
                """.trimIndent(),
            ),
        )
        assertEquals(2, points.size)
        assertEquals("2026-09-01", points[0].date)
        assertEquals(12500.5, points[0].revenue, 0.01)
        assertEquals(3, points[0].orders)
    }

    @Test
    fun `timeseries бүлдірген жауапта қате емес бос тізім береді`() {
        assertTrue(DealerParser.parseTimeseries(null).isEmpty())
        assertTrue(DealerParser.parseTimeseries(obj("""{"unexpected": true}""")).isEmpty())
        // Тікелей points (қаптамасыз) де оқылады.
        val direct = DealerParser.parseTimeseries(
            obj("""{"points": [{"date": "2026-09-01", "revenue": 10, "orders": 1}]}"""),
        )
        assertEquals(1, direct.size)
    }

    // ── Employees: profilePicture camelCase, {employees} қаптамасы ──

    @Test
    fun `employees қаптамасын және profilePicture camelCase оқиды`() {
        val employees = DealerParser.parseEmployees(
            obj(
                """
                {"employees": [
                  {"id": 1, "name": "Асан", "dealer_role": "manager",
                   "phone": "+77770001122", "iin": "123456789012",
                   "profilePicture": "/media/p/a.png"},
                  {"id": 2, "name": "Үсен", "dealer_role": "financier",
                   "profile_picture": "/media/p/u.png"}
                ]}
                """.trimIndent(),
            ),
        )
        assertEquals(2, employees.size)
        assertEquals("/media/p/a.png", employees[0].profilePicture) // camelCase!
        assertEquals("/media/p/u.png", employees[1].profilePicture) // snake_case fallback
        assertEquals("manager", employees[0].dealerRole)
        assertEquals("123456789012", employees[0].iin)
    }

    // ── Tariff conditions: conditions ?? plan.conditions → fallback merge ──

    @Test
    fun `tariff conditions data қаптамасынан fallback merge арқылы оқиды`() {
        val conditions = TariffConditions.effectiveFrom(
            obj(
                """
                {"data": {"conditions": {
                  "max_active_announcements": 50,
                  "analytics_enabled": true,
                  "personal_manager": true
                }}}
                """.trimIndent(),
            ),
        )
        assertEquals(50, conditions.maxActiveAnnouncements)
        assertTrue(conditions.hasFeature("analytics_enabled"))
        assertTrue(conditions.hasFeature("personal_manager"))
        // Көрсетілмегендер FALLBACK-тан мұраға іледі.
        assertEquals(5, conditions.maxPhotosPerAd)
        assertEquals(1.0, conditions.searchBoostMultiplier!!, 0.01)
        assertEquals("standard", conditions.supportPriority)
    }

    @Test
    fun `tariff conditions plan conditions балама жолын да оқиды`() {
        val conditions = TariffConditions.effectiveFrom(
            obj("""{"data": {"plan": {"conditions": {"analytics_enabled": true}}}}"""),
        )
        assertTrue(conditions.hasFeature("analytics_enabled"))
        assertEquals(3, conditions.maxActiveAnnouncements) // FALLBACK
    }

    @Test
    fun `tariff conditions бүлдірген жауапта FALLBACK береді`() {
        val conditions = TariffConditions.effectiveFrom(null)
        assertEquals(TariffConditions.FALLBACK.maxActiveAnnouncements, conditions.maxActiveAnnouncements)
        assertFalse(conditions.hasFeature("analytics_enabled"))
        val noData = TariffConditions.effectiveFrom(obj("""{"data": {"nope": 1}}"""))
        assertFalse(noData.hasFeature("analytics_enabled"))
        assertEquals("standard", noData.supportPriority)
    }

    // ── Claim: POST /orders/{id}/claim жауабы ──

    @Test
    fun `claim жауабын парсингтейді`() {
        val claim = DealerParser.parseClaim(
            obj("""{"order_id": 42, "assigned_user_id": 7, "status": "confirmed"}"""),
        )
        assertNotNull(claim)
        assertEquals(42L, claim!!.orderId)
        assertEquals(7L, claim.assignedUserId)
        assertEquals("confirmed", claim.status)
        assertNull(DealerParser.parseClaim(obj("""{"status": "x"}"""))) // order_id жоқ → null
    }

    // ── Orders: delivery_zones, quantity default, өнім статусы ──

    @Test
    fun `order delivery_zones пен quantity default оқиды`() {
        val response = DealerParser.parseOrders(
            obj(
                """
                {"tab": "new", "items": [
                  {"id": 5, "status": "SUPPLIER_QUERY", "total_amount": "12500",
                   "quantity": 2.5, "measurement_unit": "ton",
                   "delivery_zones": [{"id": 1, "region": "Астана", "district": "Есіл",
                     "delivery_days_min": 1, "delivery_days_max": 2, "delivery_cost": 800}]}
                ], "page": 1, "pages": 2, "tab_counts": {"new": 1}}
                """.trimIndent(),
            ),
        )
        assertTrue(response.canLoadMore) // page 1 < pages 2
        val order = response.items.single()
        assertEquals("supplier_query", order.status) // lowercase
        assertEquals(12500.0, order.totalAmount!!, 0.01) // string сома
        assertEquals(2.5, order.quantity, 0.01)
        assertEquals("Астана — Есіл", order.deliveryZones.single().displayName)
        assertEquals(1, response.tabCounts["new"])

        // quantity жоқ → 1.0 (Flutter default).
        val bare = DealerParser.parseOrders(
            obj("""{"items": [{"id": 6, "status": "delivered"}]}"""),
        )
        assertEquals(1.0, bare.items.single().quantity, 0.01)
    }

    // ── Reject message: {message} ──

    @Test
    fun `reject message message өрісін оқиды`() {
        assertEquals("Сурет сапасы төмен", DealerParser.parseRejectMessage(obj("""{"message": "Сурет сапасы төмен"}""")))
        assertEquals("", DealerParser.parseRejectMessage(obj("""{"other": 1}""")))
    }
}