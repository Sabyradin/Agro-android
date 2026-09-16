package com.agroland.feature.stories.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * StoryItem парсингі (Flutter StoryModel, 1:1): GET /stories → {stories: [...]},
 * тілдерге бөлек атаулар, толерантты парсинг (id жоқ элемент түсіп қалады).
 */
class StoryItemTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `listFromJson parses stories array with localized titles`() {
        val root = json.parseToJsonElement(
            """
            {
              "stories": [
                {
                  "id": "6639a1c2f7d3e",
                  "title": "Default",
                  "kz_title": "Жаңалық kz",
                  "eng_title": "News en",
                  "ru_title": "Новости ru",
                  "ch_title": "新闻 ch",
                  "image_url": "https://cdn.agroland.kz/story.jpg",
                  "is_viewed": true,
                  "cta_link": "https://agroland.kz/promo"
                }
              ]
            }
            """.trimIndent(),
        )
        val stories = StoryItem.listFromJson(root)
        assertEquals(1, stories.size)
        val story = stories.first()
        assertEquals("6639a1c2f7d3e", story.id)
        assertTrue(story.isViewed)
        assertEquals("https://agroland.kz/promo", story.ctaLink)
        assertEquals("Жаңалық kz", story.titleLocalized("kk"))
        assertEquals("News en", story.titleLocalized("en"))
        assertEquals("Новости ru", story.titleLocalized("ru"))
        assertEquals("新闻 ch", story.titleLocalized("zh"))
        // Белгісіз тіл — әдепкі title.
        assertEquals("Default", story.titleLocalized("fr"))
    }

    @Test
    fun `titleLocalized falls back to default title when localized missing`() {
        val story = StoryItem(
            id = "x",
            title = "Default",
            kzTitle = null,
            engTitle = null,
            ruTitle = "Только ru",
            chTitle = null,
            imageUrl = "",
            isViewed = false,
            ctaLink = null,
        )
        assertEquals("Default", story.titleLocalized("kk"))
        assertEquals("Только ru", story.titleLocalized("ru"))
    }

    @Test
    fun `listFromJson skips items without id and tolerates empty`() {
        val root = json.parseToJsonElement(
            """
            {
              "stories": [
                { "title": "no id here" },
                { "id": "ok", "image_url": "https://x/y.jpg" }
              ]
            }
            """.trimIndent(),
        )
        val stories = StoryItem.listFromJson(root)
        assertEquals(1, stories.size)
        assertEquals("ok", stories.first().id)
        assertFalse(stories.first().isViewed)
    }

    @Test
    fun `listFromJson returns empty for missing stories key or non-object root`() {
        assertTrue(StoryItem.listFromJson(json.parseToJsonElement("""{"items": []}""")).isEmpty())
        assertTrue(StoryItem.listFromJson(json.parseToJsonElement("""[]""")).isEmpty())
        assertTrue(StoryItem.listFromJson(null).isEmpty())
    }

    @Test
    fun `fromJson requires id`() {
        assertNull(StoryItem.fromJson(json.parseToJsonElement("""{"title":"x"}""")))
        assertNotNull(StoryItem.fromJson(json.parseToJsonElement("""{"id":"a"}""")))
    }
}