package com.agroland.feature.demand.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DemandParser кешірімді парсингі — дев-ортада /demands жауап пішіні
әлі белгісіз, сондықтан `{items}` / `{data:{items}}` / `{data:[...]}`
/ тікелей массив орауларының бәрі ашылуы керек.
 */
class DemandParserTest {

    private fun obj(raw: String) = Json.parseToJsonElement(raw).jsonObject

    private val itemJson = """
        {"id": 12, "title": "Дэу шөп шабатын тілік іздеймін", "description": "МТЗ-80",
         "max_price": 1500000, "currency": "KZT", "category_id": 3,
         "category": {"name": "Техника"}, "subcategory": {"name_ru": "Тракторлар"},
         "measurement_unit": "шт", "quantity": 2, "status": "active",
         "created_at": "2026-09-10T08:00:00Z"}
    """

    // ── buildBody ──

    @Test
    fun `buildBody міндетті title-ды және толтырылған өрістерді ғана жібереді`() {
        val body = DemandParser.buildBody(
            DemandDraft(
                title = "Атауы",
                currency = "KZT",
                categoryId = 5,
                subcategoryId = 7,
                maxPrice = 100000.0,
                measurementUnit = "т",
                quantity = 3.0,
            ),
        )
        assertEquals("Атауы", body["title"]!!.jsonPrimitive.content)
        assertEquals("KZT", body["currency"]!!.jsonPrimitive.content)
        assertEquals(5, body["category_id"]!!.jsonPrimitive.content.toInt())
        assertEquals(7, body["subcategory_id"]!!.jsonPrimitive.content.toInt())
        assertEquals("100000.0", body["max_price"]!!.jsonPrimitive.content)
        assertEquals("т", body["measurement_unit"]!!.jsonPrimitive.content)
        // null өрістер (description) денеге мүлде түспейді.
        assertNull(body["description"])
    }

    // ── Жеке сұраныс ораулары ──

    @Test
    fun `parseDemand success-data және data-demand орауларын ашады`() {
        val fromEnvelope = DemandParser.parseDemand(
            obj("""{"success": true, "data": $itemJson}"""),
        )!!
        assertEquals(12L, fromEnvelope.id)
        assertEquals("Дэу шөп шабатын тілік іздеймін", fromEnvelope.title)
        assertEquals(1500000.0, fromEnvelope.maxPrice!!, 0.001)
        assertEquals(2.0, fromEnvelope.quantity!!, 0.001)
        assertEquals("Техника", fromEnvelope.categoryName)
        // subcategory name жоқ → name_ru fallback.
        assertEquals("Тракторлар", fromEnvelope.subcategoryName)
        assertTrue(fromEnvelope.isActive)

        val fromDemandKey = DemandParser.parseDemand(
            obj("""{"demand": $itemJson}"""),
        )!!
        assertEquals(12L, fromDemandKey.id)
    }

    @Test
    fun `parseDemand demand_id fallback пен null-ды өңдейді`() {
        val byDemandId = DemandParser.parseDemand(
            obj("""{"success": true, "data": {"demand_id": 40, "name": "Тұқым"}}"""),
        )!!
        assertEquals(40L, byDemandId.id)
        assertEquals("Тұқым", byDemandId.title)
        assertTrue(byDemandId.isActive) // status=null → active саналады

        assertNull(DemandParser.parseDemand(obj("""{"success": true, "data": null}""")))
    }

    @Test
    fun `deactivated статус актив емес деп танылады`() {
        val item = DemandParser.parseDemand(
            obj("""{"id": 1, "title": "X", "status": "deactivated"}"""),
        )!!
        assertTrue(!item.isActive)
    }

    // ── Тізім ораулары ──

    @Test
    fun `parseDemands items конвертінен парақтау өрістерін оқиды`() {
        val page = DemandParser.parseDemands(
            obj("""{"items": [$itemJson], "page": 2, "pages": 4, "total": 39}"""),
        )
        assertEquals(1, page.items.size)
        assertEquals(2, page.page)
        assertEquals(4, page.totalPages)
        assertEquals(39, page.total)
        assertTrue(page.canLoadMore)
    }

    @Test
    fun `parseDemands data-items орауынан оқиды`() {
        val page = DemandParser.parseDemands(
            obj("""{"data": {"items": [$itemJson], "page": 1, "last_page": 1}}"""),
        )
        assertEquals(1, page.items.size)
        assertEquals(1, page.totalPages)
        assertTrue(!page.canLoadMore)
    }

    @Test
    fun `parseDemands data-массив пен тікелей массивты қабылдайды`() {
        val a = DemandParser.parseDemands(obj("""{"data": [$itemJson]}"""))
        assertEquals(1, a.items.size)
        val b = DemandParser.parseDemands(Json.parseToJsonElement("""[$itemJson]"""))
        assertEquals(1, b.items.size)
        // Бос/белгісіз конверт → бос бет.
        val c = DemandParser.parseDemands(obj("""{"success": true}"""))
        assertTrue(c.items.isEmpty())
    }
}