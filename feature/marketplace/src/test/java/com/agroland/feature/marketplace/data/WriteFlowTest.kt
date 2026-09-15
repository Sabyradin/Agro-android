package com.agroland.feature.marketplace.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.RequestBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 6 — жазу ағыны: draft валидациясы, multipart құрылымы, жауап парсерлері. */
class WriteFlowTest {

    private fun obj(raw: String) = Json.parseToJsonElement(raw).jsonObject

    private fun RequestBodyText(body: RequestBody): String =
        Buffer().apply { body.writeTo(this) }.readUtf8()

    // ---- AdDraft.validate(): өріс басымдылығы (бірінші бос өріс) ----

    @Test
    fun `толық draft валид`() {
        val draft = AdDraft(
            title = "Бидай",
            description = "Сапалы бидай",
            price = "1000",
            categoryId = 1,
            subcategoryId = 2,
            contactNumbers = listOf("+77770001111"),
            userLocationId = 5L,
        )
        assertNull(draft.validate())
        assertTrue(draft.isValid())
        assertEquals(1000.0, draft.priceValue!!, 0.001)
    }

    @Test
    fun `validate басымдылығы title бірінші`() {
        assertEquals(AdDraft.FIELD_TITLE, AdDraft().validate())
        assertEquals(AdDraft.FIELD_DESCRIPTION, AdDraft(title = "а").validate())
        assertEquals(AdDraft.FIELD_PRICE, AdDraft(title = "а", description = "б").validate())
        assertEquals(
            AdDraft.FIELD_CATEGORY,
            AdDraft(title = "а", description = "б", price = "1").validate(),
        )
        assertEquals(
            AdDraft.FIELD_SUBCATEGORY,
            AdDraft(title = "а", description = "б", price = "1", categoryId = 1).validate(),
        )
        assertEquals(
            AdDraft.FIELD_PHONES,
            AdDraft(title = "а", description = "б", price = "1", categoryId = 1, subcategoryId = 2)
                .validate(),
        )
        assertEquals(
            AdDraft.FIELD_LOCATION,
            AdDraft(
                title = "а", description = "б", price = "1",
                categoryId = 1, subcategoryId = 2, contactNumbers = listOf("8"),
            ).validate(),
        )
        // Өзі алу қосылғанда мекенжай міндетті.
        assertEquals(
            AdDraft.FIELD_PICKUP_ADDRESS,
            AdDraft(
                title = "а", description = "б", price = "1",
                categoryId = 1, subcategoryId = 2, contactNumbers = listOf("8"),
                userLocationId = 1, pickupAvailable = true,
            ).validate(),
        )
    }

    @Test
    fun `нөл немесе теріс баға жарамсыз`() {
        val draft = AdDraft(
            title = "а", description = "б", price = "0",
            categoryId = 1, subcategoryId = 2, contactNumbers = listOf("8"), userLocationId = 1,
        )
        assertEquals(AdDraft.FIELD_PRICE, draft.validate())
    }

    // ---- AdRequests.buildMultipart ----

    @Test
    fun `multipart мәтіндік өрістері дұрыс құрылады`() {
        val draft = AdDraft(
            title = "  Бидай  ",
            description = "Сипаттама",
            price = "1500.5",
            currency = "₸",
            categoryId = 3,
            subcategoryId = 4,
            measurementUnit = MeasurementUnit.KILOGRAM,
            userLocationId = 9L,
            contactNumbers = listOf("+7777", " "),
            keywords = listOf("бидай", ""),
            deliveryZoneIds = listOf(1L, 2L),
            sku = " SKU-1 ",
            stockQuantity = "12",
            images = listOf("/media/a.jpg", "/media/b.jpg"),
            pickupAvailable = true,
            pickupAddress = " Астана ",
        )
        val (fields, parts) = AdRequests.buildMultipart(draft, emptyList(), null, skipTariffDialog = true)

        assertEquals("Бидай", RequestBodyText(fields["title"]!!))
        assertEquals("1500.5", RequestBodyText(fields["price"]!!))
        assertEquals("₸", RequestBodyText(fields["currency"]!!))
        assertEquals("3", RequestBodyText(fields["category_id"]!!))
        assertEquals("4", RequestBodyText(fields["subcategory_id"]!!))
        assertEquals("kilogram", RequestBodyText(fields["measurement_unit"]!!))
        assertEquals("9", RequestBodyText(fields["user_location_id"]!!))
        assertEquals("SKU-1", RequestBodyText(fields["sku"]!!))
        assertEquals("12", RequestBodyText(fields["stock_quantity"]!!))
        assertEquals("Астана", RequestBodyText(fields["pickup_address"]!!))
        assertEquals("/media/a.jpg,/media/b.jpg", RequestBodyText(fields["existing_images"]!!))
        assertEquals("true", RequestBodyText(fields["skipTariffDialog"]!!))

        // Қайталанатын кілттер — бөлек Part ретінде, бос мәндер түседі.
        fun partCount(name: String) = parts.count {
            it.headers?.values("Content-Disposition")?.firstOrNull()
                ?.contains("name=\"$name\"") == true
        }
        assertEquals(1, partCount("contact_numbers"))
        assertEquals(1, partCount("keywords"))
        assertEquals(2, partCount("delivery_zone_ids"))
    }

    @Test
    fun `қосылмаған опционал өрістер жіберілмейді`() {
        val draft = AdDraft(
            title = "а", description = "б", price = "1",
            categoryId = 1, subcategoryId = 2, contactNumbers = listOf("8"), userLocationId = 1,
        )
        val (fields, parts) = AdRequests.buildMultipart(draft, emptyList(), null)
        assertNull(fields["sku"])
        assertNull(fields["pickup_address"])
        assertNull(fields["existing_images"])
        assertNull(fields["skipTariffDialog"])
        // Тек contact_numbers Part-ы — басқа қосымша part жоқ.
        assertEquals(1, parts.size)
        assertTrue(
            parts[0].headers?.values("Content-Disposition").orEmpty()
                .firstOrNull()?.contains("name=\"contact_numbers\"") == true,
        )
    }

    // ---- DemandDraft ----

    @Test
    fun `demand draft validate және json`() {
        assertEquals(AdDraft.FIELD_TITLE, DemandDraft().validate())

        val draft = DemandDraft(
            title = "Трактор іздеймін",
            description = "Жаңа немесе пайдаланылған",
            maxPrice = "5000000",
            categoryId = 7,
            subcategoryId = 8,
            measurementUnit = MeasurementUnit.PIECE,
            quantity = "2",
            userLocationId = 3L,
        )
        assertNull(draft.validate())

        val json = draft.toJsonObject()
        assertEquals("Трактор іздеймін", json["title"]?.toString()?.trim('"'))
        assertEquals(5000000.0, (json["max_price"] as JsonPrimitive).content.toDouble(), 0.001)
        assertEquals(7, (json["category_id"] as JsonPrimitive).content.toInt())
        assertEquals("piece", json["measurement_unit"]?.toString()?.trim('"'))
        assertEquals(3L, (json["user_location_id"] as JsonPrimitive).content.toLong())

        val partMap = draft.toPartMap()
        assertEquals("5000000.0", RequestBodyText(partMap["max_price"]!!))
        assertEquals("7", RequestBodyText(partMap["category_id"]!!))
    }

    // ---- MeasurementUnit ----

    @Test
    fun `measurement unit кілттен оқылады`() {
        assertEquals(MeasurementUnit.SQUARE_METER, MeasurementUnit.fromKey("square_meter"))
        assertEquals(MeasurementUnit.BOX, MeasurementUnit.fromKey("Box"))
        assertNull(MeasurementUnit.fromKey("unknown_unit"))
        assertNull(MeasurementUnit.fromKey(null))
    }

    // ---- WriteParser ----

    @Test
    fun `жарнама id әртүрлі пішімдерден оқылады`() {
        assertEquals(11L, WriteParser.parseAnnouncementId(obj("""{"id": 11}""")))
        assertEquals(12L, WriteParser.parseAnnouncementId(obj("""{"announcement_id": 12}""")))
        assertEquals(13L, WriteParser.parseAnnouncementId(obj("""{"announcement": {"id": 13}}""")))
        assertEquals(14L, WriteParser.parseAnnouncementId(obj("""{"data": {"announcement_id": "14"}}""")))
        assertNull(WriteParser.parseAnnouncementId(obj("""{"title": "ештеңе жоқ"}""")))
    }

    @Test
    fun `demand id парсингі`() {
        assertEquals(21L, WriteParser.parseDemandId(obj("""{"id": 21}""")))
        assertEquals(22L, WriteParser.parseDemandId(obj("""{"demand": {"id": "22"}}""")))
        assertEquals(23L, WriteParser.parseDemandId(obj("""{"data": {"id": 23}}""")))
    }

    @Test
    fun `қайтару себебі бірінші кездескен өрістен`() {
        assertEquals("Сурет сапасы төмен", WriteParser.parseRejectMessage(obj("""{"message": "Сурет сапасы төмен"}""")))
        assertEquals("Баға дұрыс емес", WriteParser.parseRejectMessage(obj("""{"reason": "Баға дұрыс емес"}""")))
        assertEquals("R2", WriteParser.parseRejectMessage(obj("""{"detail": "R1", "text": "R2"}""")))
        assertNull(WriteParser.parseRejectMessage(obj("""{"id": 1}""")))
    }

    @Test
    fun `ai мазмұн data ішінен оқылады`() {
        val content = WriteParser.parseAdContent(
            obj(
                """
                {"data": {"description": "Сапалы бидай!", "category_id": 5,
                 "subcategory_id": 6, "ad_title": "Бидай"}}
                """.trimIndent(),
            ),
        )!!
        assertEquals("Сапалы бидай!", content.description)
        assertEquals(5, content.categoryId)
        assertEquals(6, content.subcategoryId)
        assertEquals("Бидай", content.title)

        assertNull(WriteParser.parseAdContent(obj("""{"id": 1}""")))
    }

    @Test
    fun `bulk нәтижесі сандарды оқиды`() {
        val result = WriteParser.parseBulkResult(
            obj("""{"data": {"created_count": 10, "errors_count": "2", "message": "Дайын"}}"""),
        )
        assertEquals(10, result.createdCount)
        assertEquals(2, result.errorCount)
        assertEquals("Дайын", result.message)

        val empty = WriteParser.parseBulkResult(obj("""{}"""))
        assertNull(empty.createdCount)
        assertNull(empty.errorCount)
    }

    @Test
    fun `жеткізу аймақтары items zones delivery_zones тізімдерінен`() {
        val zones = WriteParser.parseDeliveryZones(
            obj(
                """
                {"items": [{"id": 1, "name": "Астана", "region_name": "Астана", "delivery_cost": 500}]}
                """.trimIndent(),
            ),
        )
        assertEquals(1, zones.size)
        assertEquals(1L, zones[0].id)
        assertEquals("Астана", zones[0].name)
        assertEquals(500.0, zones[0].deliveryCost!!, 0.001)

        assertTrue(WriteParser.parseDeliveryZones(obj("""{"items": []}""")).isEmpty())
        assertTrue(WriteParser.parseDeliveryZones(null).isEmpty())
    }

    // ---- FullAnnouncement жаңа өрістері (өңдеу prefill) ----

    @Test
    fun `full announcement keywords user_location_id sku stock_quantity оқиды`() {
        val detail = MarketplaceParser.parseFullAnnouncement(
            obj(
                """
                {"id": 77, "title": "Трактор", "keywords": ["мтз", "техника"],
                 "tags": ["беларусь"], "user_location_id": 4, "sku": "TR-9",
                 "stock_quantity": "3", "description": "Сатылады"}
                """.trimIndent(),
            ),
        )!!
        assertEquals(listOf("мтз", "техника", "беларусь"), detail.keywords)
        assertEquals(4L, detail.userLocationId)
        assertEquals("TR-9", detail.sku)
        assertEquals(3, detail.stockQuantity)
        assertEquals("Сатылады", detail.description)
    }
}