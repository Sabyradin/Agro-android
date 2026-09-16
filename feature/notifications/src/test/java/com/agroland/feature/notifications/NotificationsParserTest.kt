package com.agroland.feature.notifications

import com.agroland.feature.notifications.data.NotificationType
import com.agroland.feature.notifications.data.NotificationsParser
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** NotificationsParser — counter + items толерантты парсингі. */
class NotificationsParserTest {

    @Test
    fun `counter — түбірлік пішім`() {
        val c = NotificationsParser.parseCounter(
            Json.parseToJsonElement("""{"service_count": 2, "support_count": "5", "promotions_count": 0}"""),
        )
        assertEquals(2, c.serviceCount)
        assertEquals(5, c.supportCount) // string → int
        assertEquals(0, c.promotionsCount)
    }

    @Test
    fun `counter — data ішіне оралған жағдайда да оқылады`() {
        val c = NotificationsParser.parseCounter(
            Json.parseToJsonElement("""{"data": {"service_count": 1, "support_count": 1, "promotions_count": 1}}"""),
        )
        assertEquals(1, c.serviceCount)
        assertEquals(1, c.supportCount)
    }

    @Test
    fun `counter — бөлімдер жоқ болса нөл`() {
        val c = NotificationsParser.parseCounter(Json.parseToJsonElement("""{"other": 1}"""))
        assertEquals(0, c.serviceCount)
        assertEquals(0, c.supportCount)
        assertEquals(0, c.promotionsCount)
    }

    @Test
    fun `items — items массиві және толық емес жазбалар`() {
        val items = NotificationsParser.parseItems(
            Json.parseToJsonElement(
                """
                {"items": [
                  {"id": "10", "title": "T1", "text": "<b>Hello</b>", "file": "https://a.kz/i.jpg",
                   "created_at": "2026-09-15T10:00:00Z"},
                  {"id": 11, "title": "", "text": "", "file": ""},
                  {"id": null}
                ]}
                """.trimIndent(),
            ),
        )
        assertEquals(2, items.size)
        assertEquals(10L, items[0].id) // string → long
        assertEquals("<b>Hello</b>", items[0].text)
        assertTrue(items[0].createdAt > 0)
        assertEquals("", items[1].file)
    }

    @Test
    fun `items — бүркемеленген data ішіндегі items`() {
        val items = NotificationsParser.parseItems(
            Json.parseToJsonElement("""{"data": {"items": [{"id": 1}]}}"""),
        )
        assertEquals(1, items.size)
        assertNotNull(items[0])
    }

    @Test
    fun `parseTime — жарамсыз болса қазір`() {
        assertTrue(NotificationsParser.parseTime("not-a-date") > 0)
        assertTrue(NotificationsParser.parseTime(null) > 0)
    }

    @Test
    fun `NotificationType fromString — регистрге төзімді, fallback SERVICE`() {
        assertEquals(NotificationType.SUPPORT, NotificationType.fromString("support"))
        assertEquals(NotificationType.PROMOTIONS, NotificationType.fromString("PROMOTIONS"))
        assertEquals(NotificationType.SERVICE, NotificationType.fromString("unknown"))
        assertEquals("service", NotificationType.SERVICE.path)
        assertEquals("promotions", NotificationType.PROMOTIONS.path)
    }
}