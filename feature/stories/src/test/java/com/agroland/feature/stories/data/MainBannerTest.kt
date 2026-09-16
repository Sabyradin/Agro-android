package com.agroland.feature.stories.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MainBanner парсингі (Flutter promo_v2 BannerModel, 1:1):
 * GET /banners/main → {items: [...]}, дев-бекенд {items: []} толерантты.
 */
class MainBannerTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `listFromResponse parses items with optional fields`() {
        val root = json.parseToJsonElement(
            """
            {
              "items": [
                {
                  "id": 12,
                  "image_url": "https://cdn.agroland.kz/banner.jpg",
                  "cta_url": "https://agroland.kz/page",
                  "title": "Жеңілдіктер",
                  "announcement_id": 34891,
                  "status": "active"
                },
                {
                  "id": 13,
                  "image_url": "https://cdn.agroland.kz/banner2.jpg"
                }
              ]
            }
            """.trimIndent(),
        )
        val banners = MainBanner.listFromResponse(root)
        assertEquals(2, banners.size)
        val first = banners[0]
        assertEquals(12L, first.id)
        assertEquals("https://agroland.kz/page", first.ctaUrl)
        assertEquals(34891L, first.announcementId)
        assertEquals("active", first.status)
        // Міндетті емес өрістер null болуы мүмкін.
        val second = banners[1]
        assertNull(second.title)
        assertNull(second.announcementId)
        assertNull(second.ctaUrl)
    }

    @Test
    fun `listFromResponse tolerates dev backend empty items`() {
        val root = json.parseToJsonElement("""{"items": []}""")
        assertTrue(MainBanner.listFromResponse(root).isEmpty())
    }

    @Test
    fun `listFromResponse skips items without id or image_url`() {
        val root = json.parseToJsonElement(
            """
            {
              "items": [
                { "image_url": "https://x/1.jpg" },
                { "id": 5 },
                { "id": 7, "image_url": "https://x/7.jpg" }
              ]
            }
            """.trimIndent(),
        )
        val banners = MainBanner.listFromResponse(root)
        assertEquals(1, banners.size)
        assertEquals(7L, banners[0].id)
    }

    @Test
    fun `listFromResponse returns empty for non-object or missing items`() {
        assertTrue(MainBanner.listFromResponse(json.parseToJsonElement("""{"banners": []}""")).isEmpty())
        assertTrue(MainBanner.listFromResponse(json.parseToJsonElement("""[]""")).isEmpty())
        assertTrue(MainBanner.listFromResponse(null).isEmpty())
    }
}