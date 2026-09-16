package com.agroland.feature.reviews.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ReviewParser — пікір JSON пішіндері (backend конверт-варианттары). */
class ReviewParserTest {

    private fun obj(raw: String) = Json.parseToJsonElement(raw).jsonObject
    private fun arr(raw: String) = Json.parseToJsonElement(raw).jsonArray

    @Test
    fun `parseReviews empty data array`() {
        val reviews = ReviewParser.parseReviews(
            obj("""{"success":true,"data":[]}"""),
        )
        assertTrue(reviews.isEmpty())
    }

    @Test
    fun `parseReviews data array of objects`() {
        val reviews = ReviewParser.parseReviews(
            obj(
                """
                {"data":[{"id":1,"user_id":7,"user_name":"Аслан","announcement_id":42,
                "text":"Жақсы тауар","rating":5,"created_at":"2026-09-01T10:00:00Z"},
                {"id":2,"user_id":8,"user_name":"Дана","announcement_id":42,
                "text":"Орташа","rating":3,"created_at":"2026-09-02T10:00:00Z"}]}
                """.trimIndent(),
            ),
        )
        assertEquals(2, reviews.size)
        assertEquals(7L, reviews[0].userId)
        assertEquals("Аслан", reviews[0].userName)
        assertEquals(5, reviews[0].rating)
        assertEquals(3, reviews[1].rating)
    }

    @Test
    fun `parseReviews items array`() {
        val reviews = ReviewParser.parseReviews(
            obj(
                """
                {"success":true,"items":[{"id":9,"user_id":1,"user_name":"A",
                "announcement_id":2,"text":"","rating":1,"created_at":null}]}
                """.trimIndent(),
            ),
        )
        assertEquals(1, reviews.size)
        assertEquals("", reviews[0].text)
        assertEquals(9L, reviews[0].id)
    }

    @Test
    fun `parseReviews non-array data tolerated`() {
        // {data:"not-a-list"} → бос тізім, ешқашан crash емес.
        val reviews = ReviewParser.parseReviews(
            obj("""{"data":"not-a-list"}"""),
        )
        assertTrue(reviews.isEmpty())
    }

    @Test
    fun `parseProfileReviews maps summary fields`() {
        val items = ReviewParser.parseProfileReviews(
            obj(
                """
                {"data":[{"id":5,"title":"Трактор","price":1500000.0,
                "created_at":"2026-08-01T00:00:00Z","main_image_url":"http://x/1.jpg",
                "rating":4,"total_reviews":12}]}
                """.trimIndent(),
            ),
        )
        assertEquals(1, items.size)
        assertEquals("Трактор", items[0].title)
        assertEquals(4, items[0].rating)
        assertEquals(12, items[0].totalReviews)
    }

    @Test
    fun `parseSellerSummary unwraps data and reads avg`() {
        val summary = ReviewParser.parseSellerSummary(
            obj(
                """
                {"data":{"avg_rating":4.7,"total":25,"items":[
                {"id":1,"author_id":2,"author_name":"Бек","author_avatar_url":null,
                "rating":5,"text":"Үздік","created_at":"2026-09-01T00:00:00Z",
                "order_id":3,"like_count":4,"is_liked":false}]}}
                """.trimIndent(),
            ),
        )
        assertEquals(4.7, summary.avgRating, 0.001)
        assertEquals(25, summary.total)
        assertEquals(1, summary.items.size)
        assertEquals(4, summary.items[0].likeCount)
        assertFalse(summary.items[0].isLiked)
    }

    @Test
    fun `parseSellerSummary bare array items only`() {
        val summary = ReviewParser.parseSellerSummary(
            arr(
                """
                [{"id":2,"author_id":3,"author_name":"","rating":1,
                "text":"наш","created_at":null,"like_count":0,"is_liked":true}]
                """.trimIndent(),
            ),
        )
        // Bare list payload — avg/total = 0, items парсинг.
        assertEquals(0.0, summary.avgRating, 0.001)
        assertEquals(0, summary.total)
        assertEquals(1, summary.items.size)
        assertTrue(summary.items[0].isLiked)
    }

    @Test
    fun `parseLikeResult authoritative backend echo`() {
        val result = ReviewParser.parseLikeResult(
            obj("""{"review_id":11,"is_liked":true,"like_count":7}"""),
            fallbackLiked = false,
        )
        assertEquals(11L, result!!.reviewId)
        assertTrue(result.isLiked)
        assertEquals(7, result.likeCount)
    }

    @Test
    fun `parseLikeResult minimal success gives null and repository falls back`() {
        // Backend тек {"success":true} қайтарса — parser null, fallback ReviewRepository-де.
        val result = ReviewParser.parseLikeResult(
            obj("""{"success":true}"""),
            fallbackLiked = true,
        )
        assertNull(result)
    }

    @Test
    fun `parseAnnouncementInfo image chain prefers image_urls`() {
        val info = ReviewParser.parseAnnouncementInfo(
            obj(
                """
                {"id":42,"title":"Тауар","price":1000.0,"currency":"₸",
                "created_at":"2026-09-10T00:00:00Z",
                "image_urls":["http://x/1.jpg","http://x/2.jpg"],
                "main_image_url":"http://x/main.jpg"}
                """.trimIndent(),
            ),
        )
        assertEquals("http://x/1.jpg", info!!.imageUrl)
        assertEquals("Тауар", info.title)
    }

    @Test
    fun `parseAnnouncementInfo falls back through image variants`() {
        val a = ReviewParser.parseAnnouncementInfo(
            obj("""{"id":1,"title":"","main_image_url":"http://x/m.jpg"}"""),
        )
        assertEquals("http://x/m.jpg", a!!.imageUrl)
        val b = ReviewParser.parseAnnouncementInfo(
            obj("""{"id":1,"title":"","image":"http://x/i.jpg"}"""),
        )
        assertEquals("http://x/i.jpg", b!!.imageUrl)
        val c = ReviewParser.parseAnnouncementInfo(
            obj("""{"id":1,"title":"","images":["http://x/0.jpg"]}"""),
        )
        assertEquals("http://x/0.jpg", c!!.imageUrl)
    }

    @Test
    fun `parseAnnouncementInfo without id gives null`() {
        assertNull(
            ReviewParser.parseAnnouncementInfo(
                obj("""{"title":"id жоқ"}"""),
            ),
        )
    }

    @Test
    fun `parseBuyerOrderCandidates legacy root announcement_id`() {
        // /orders нақты пішіні — {items:[...]} (CartParser.parseOrders parity).
        val candidates = ReviewParser.parseBuyerOrderCandidates(
            obj(
                """
                {"items":[{"announcement_id":11,"agreed_price":500.0,"currency":"₸",
                "created_at":"2026-09-01T00:00:00Z","title":"Ескі тапсырыс"},
                {"announcement_id":12,"total_price":900.0}]}
                """.trimIndent(),
            ),
        )
        assertEquals(2, candidates.size)
        assertEquals(11L, candidates[0].announcementId)
        assertEquals(500.0, candidates[0].price!!, 0.001)
        assertEquals(12L, candidates[1].announcementId)
        assertEquals(900.0, candidates[1].price!!, 0.001)
    }

    @Test
    fun `parseBuyerOrderCandidates data-wrapped smart calculator dedupe`() {
        val candidates = ReviewParser.parseBuyerOrderCandidates(
            obj(
                """
                {"data":{"items":[
                {"announcement_id":21,"unit_price":100.0,"title":"Бірінші"},
                {"announcement_id":21,"unit_price":100.0},
                {"announcement_id":22,"unit_price":200.0}]}}
                """.trimIndent(),
            ),
        )
        // 21 екі рет келеді — LinkedHashMap dedupe.
        assertEquals(2, candidates.size)
        assertEquals(21L, candidates[0].announcementId)
        assertEquals("Бірінші", candidates[0].title)
        assertEquals(22L, candidates[1].announcementId)
    }

    @Test
    fun `parseBuyerOrderCandidates nested order items also collected`() {
        // Smart Calculator: тапсырыстың өзіндегі items[] әр жолы да үміткер.
        val candidates = ReviewParser.parseBuyerOrderCandidates(
            obj(
                """
                {"items":[{"id":90,"created_at":"2026-09-05T00:00:00Z","items":[
                {"announcement_id":31,"unit_price":100.0,"title":"Тұқым"},
                {"announcement_id":31,"unit_price":100.0},
                {"announcement_id":32,"unit_price":250.0,"title":"Тыңайтқыш"}]}]}
                """.trimIndent(),
            ),
        )
        assertEquals(2, candidates.size)
        assertEquals(31L, candidates[0].announcementId)
        assertEquals("Тұқым", candidates[0].title)
        assertEquals(32L, candidates[1].announcementId)
    }

    @Test
    fun `parseBuyerOrderCandidates bad root gives empty`() {
        assertTrue(
            ReviewParser.parseBuyerOrderCandidates(
                obj("""{"error":"nope"}"""),
            ).isEmpty(),
        )
    }
}