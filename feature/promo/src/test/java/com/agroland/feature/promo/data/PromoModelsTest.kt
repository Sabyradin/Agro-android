package com.agroland.feature.promo.data

import com.agroland.core.network.error.ApiErrorParser
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Промо v2 модельдерінің парсингі (Flutter PromoCatalogItem/PromotionModel/
 * serverFromData, 1:1): {items: [...]} каталог, локализация fallback-тері,
 * мұрағат промо, құрылымдық қате сандары.
 */
class PromoModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun parse(raw: String) = json.parseToJsonElement(raw)

    @Test
    fun `catalog parses items with activate endpoint`() {
        val root = parse(
            """
            {
              "items": [
                {
                  "sku": "boost_x2",
                  "kind": "boost",
                  "target": "announcement",
                  "duration_days": 7,
                  "effects": {"score": 2},
                  "titles": {"ru": "Буст x2", "kk": "Буст x2 kk", "cn": "推广 x2"},
                  "descriptions": {"ru": "Описание"},
                  "price": 5000,
                  "currency": "KZT",
                  "activate_endpoint": {
                    "method": "post",
                    "path": "/user/announcements/{announcement_id}/boost",
                    "body": {"duration_days": 7}
                  }
                }
              ]
            }
            """.trimIndent(),
        )
        val items = PromoCatalogItem.listFromJson(root)
        assertEquals(1, items.size)
        val item = items.first()
        assertEquals("boost_x2", item.sku)
        assertEquals("boost", item.kind)
        assertEquals("announcement", item.target)
        assertEquals(7, item.durationDays)
        assertEquals(mapOf("score" to "2"), item.effects)
        assertEquals(5000.0, item.price, 0.01)
        assertEquals("KZT", item.currency)
        assertTrue(item.requiresAnnouncement)
        // Методды uppercase, path плейсхолдер алмастырылған және бастапқы / жоқ.
        assertEquals("POST", item.activateEndpoint!!.method)
        assertEquals(
            "user/announcements/42/boost",
            item.activateEndpoint!!.resolvedPath(42),
        )
        assertEquals("score" to "2", item.effects.entries.first().toPair())
    }

    @Test
    fun `localized falls back zh-cn direct ru first`() {
        val item = PromoCatalogItem(
            sku = "s",
            kind = "vip",
            target = "user",
            durationDays = 0,
            effects = emptyMap(),
            titles = mapOf("ru" to "Только ru", "cn" to "中文", "kk" to "Қазақша"),
            descriptions = emptyMap(),
            price = 0.0,
            currency = "KZT",
            activateEndpoint = null,
        )
        // zh → cn бірінші.
        assertEquals("中文", item.localizedTitle("zh"))
        // Тікелей сәйкес.
        assertEquals("Қазақша", item.localizedTitle("kk"))
        // Белгісіз тіл → ru fallback.
        assertEquals("Только ru", item.localizedTitle("fr"))
        // zh мәтіні жоқ болса → ru.
        assertEquals("", item.localizedDescription("kk"))

        val onlyKk = item.copy(titles = mapOf("kk" to "Қазақша"))
        assertEquals("Қазақша", onlyKk.localizedTitle("en"))
        val empty = item.copy(titles = emptyMap())
        assertEquals("", empty.localizedTitle("ru"))
    }

    @Test
    fun `catalog tolerates missing items and skips entries without sku`() {
        assertTrue(PromoCatalogItem.listFromJson(parse("""{"items": []}""")).isEmpty())
        assertTrue(PromoCatalogItem.listFromJson(parse("""{}""")).isEmpty())
        val root = parse(
            """
            {"items": [{"kind": "boost"}, {"sku": "ok", "kind": "vip", "target": "user"}]}
            """.trimIndent(),
        )
        val items = PromoCatalogItem.listFromJson(root)
        assertEquals(1, items.size)
        assertEquals("ok", items.first().sku)
        assertFalse(items.first().requiresAnnouncement)
    }

    @Test
    fun `promotion parses and coerces status`() {
        val promotion = Promotion.fromJson(
            parse(
                """
                {
                  "id": 7,
                  "package_type": "MINIMAL",
                  "status": "ACTIVE",
                  "max_views": 1000,
                  "start_views": 0,
                  "current_views": 250,
                  "views_used": 250,
                  "remaining_views": 750,
                  "progress_percentage": 25,
                  "remaining_time_seconds": 432000,
                  "remaining_time_days": 5.0,
                  "auto_renewal": true
                }
                """.trimIndent(),
            ),
        )!!
        assertEquals(7L, promotion.id)
        assertEquals("minimal", promotion.packageType)
        assertEquals("active", promotion.status)
        assertEquals(250, promotion.viewsUsed)
        assertEquals(25, promotion.progressPercentage)
        assertEquals(5.0, promotion.remainingTimeDays!!, 0.01)
        assertTrue(promotion.autoRenewal)
    }

    @Test
    fun `announcement promotion response unwraps has_promotion`() {
        assertNull(Promotion.fromAnnouncementResponse(parse("""{"has_promotion": false}""")))
        assertNull(Promotion.fromAnnouncementResponse(parse("""{"has_promotion": true}""")))

        val withPromotion = Promotion.fromAnnouncementResponse(
            parse("""{"has_promotion": true, "promotion": {"id": 3, "status": "pending"}}"""),
        )!!
        assertEquals(3L, withPromotion.id)
        assertEquals("pending", withPromotion.status)
    }

    @Test
    fun `user announcements promotions parse list`() {
        val root = parse(
            """
            {
              "announcements": [
                {
                  "announcement": {"id": 11, "title": "Трактор", "price": 250000},
                  "has_promotion": true,
                  "promotion": {"id": 9, "package_type": "optimal", "status": "active",
                                "max_views": 500, "views_used": 100, "progress_percentage": 20}
                },
                {
                  "announcement": {"id": 12, "title": "Бидай"},
                  "has_promotion": false
                },
                {"has_promotion": false}
              ]
            }
            """.trimIndent(),
        )
        val items = AnnouncementPromotion.listFromResponse(root)
        assertEquals(2, items.size)
        assertEquals(11L, items[0].announcementId)
        assertEquals("Трактор", items[0].announcementTitle)
        assertTrue(items[0].hasPromotion)
        assertEquals(9L, items[0].promotion!!.id)
        assertFalse(items[1].hasPromotion)
        assertNull(items[1].promotion)
    }

    @Test
    fun `activate endpoint path substitution variants`() {
        val endpoint = PromoActivateEndpoint(
            method = "PATCH",
            path = "/banners/main",
            body = null,
        )
        assertEquals("banners/main", endpoint.resolvedPath(null))
        assertEquals("banners/main", endpoint.resolvedPath(99))

        val withPlaceholder = PromoActivateEndpoint(
            method = "POST",
            path = "promotion/{announcement_id}/auto-renewal",
            body = null,
        )
        assertEquals("promotion/5/auto-renewal", withPlaceholder.resolvedPath(5))
        // announcementId жоқ — бос жол қалады (Flutter сияқты).
        assertEquals("promotion//auto-renewal", withPlaceholder.resolvedPath(null))
    }

    @Test
    fun `api error parser extracts structured numbers from nested error block`() {
        val nested = ApiErrorParser.parse(
            409,
            """{"error": {"error_code": "INSUFFICIENT_BALANCE", "message": "Баланс жетпейді",
                  "balance": 250.0, "required": 5000, "missing": 4750}}""",
        )
        assertEquals("INSUFFICIENT_BALANCE", nested.code)
        assertEquals("Баланс жетпейді", nested.message)
        assertEquals(250.0, nested.balance!!, 0.01)
        assertEquals(5000.0, nested.requiredAmount!!, 0.01)
        assertEquals(4750.0, nested.missingAmount!!, 0.01)

        val topLevel = ApiErrorParser.parse(
            409,
            """{"error_code": "BANNER_SLOT_ALREADY_ACTIVE", "balance": 100}""",
        )
        assertEquals("BANNER_SLOT_ALREADY_ACTIVE", topLevel.code)
        assertEquals(100.0, topLevel.balance!!, 0.01)
        assertNull(topLevel.missingAmount)
    }
}