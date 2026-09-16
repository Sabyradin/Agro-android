package com.agroland.feature.reviews.data

import com.agroland.core.network.json.JsonParser
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Пікірлер (Phase 18) — Flutter review/ + SWIFT_REWRITE_SPEC L122-125, 1:1.
 * Екі пікір моделі: Review (жарнама деңгейі, rating 0-5) және SellerReview
 * (жеткізілген тапсырыстан кейін сатушыға, rating 1-5).
 */

/** GET /reviews/{announcement_id} жолы — ReviewModel.fromJson. */
data class Review(
    val id: Long,
    val userId: Long,
    val userName: String,
    val announcementId: Long,
    val text: String = "",
    val rating: Int = 0,
    val createdAt: String? = null,
)

/**
 * GET /user/{user_id}/reviews жолы — жарнаманың қысқаша пікір жиынтығы
 * (ProfileReviewModel: rating + total_reviews + баға + сурет).
 */
data class ProfileReview(
    val id: Long,
    val title: String,
    val price: Double = 0.0,
    val createdAt: String? = null,
    val mainImageUrl: String = "",
    val rating: Int = 0,
    val totalReviews: Int = 0,
)

/** GET /seller/{user_id}/reviews жолы — сатып алушы пікірі (like-пен). */
data class SellerReview(
    val id: Long,
    val authorId: Long? = null,
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val rating: Int = 0,
    val text: String = "",
    val createdAt: String? = null,
    val orderId: Long? = null,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
)

/** POST/DELETE /reviews/seller/{id}/like жауабы — авторитетті is_liked + like_count. */
data class SellerReviewLikeResult(
    val reviewId: Long,
    val isLiked: Boolean,
    val likeCount: Int,
)

/** GET /seller/{user_id}/reviews жиынтығы: avg_rating + total + items. */
data class SellerReviewSummary(
    val avgRating: Double = 0.0,
    val total: Int = 0,
    val items: List<SellerReview> = emptyList(),
)

/** FeedbackAdCard үшін жарнаманың қысқаша көрінісі (GET /announcement/{id}). */
data class ReviewAnnouncementInfo(
    val id: Long,
    val title: String = "",
    val price: Double? = null,
    val currency: String? = null,
    val imageUrl: String? = null,
    val createdAt: String? = null,
)

/** «Менің пікірлерім» үміткері (spec §10): қаралған жарнама не тапсырыс. */
data class MyReviewCandidate(
    val announcementId: Long,
    val title: String = "",
    val imageUrl: String? = null,
    val price: Double? = null,
    val currency: String? = null,
    val createdAt: String? = null,
    /** Өз пікірі табылса — толтырылады (done қойындысы). */
    val ownReview: Review? = null,
) {
    val hasOwnReview: Boolean get() = ownReview != null
}

/**
 * ReviewParser — кешірімді парсинг (backend жауаптары тұрақсыз типті,
 * spec §4). Барлық `{data}` ораулары ашылады, тікелей массив те қабылданады.
 */
object ReviewParser {

    // ── Жарнама пікірлері ──

    fun parseReviews(root: JsonElement): List<Review> {
        val array = arrayFrom(root) ?: return emptyList()
        return array.mapNotNull { el -> parseReview(el) }
    }

    private fun parseReview(el: JsonElement): Review? {
        val obj = unwrap(el) as? JsonObject ?: return null
        val id = JsonParser.long(obj, "id") ?: return null
        return Review(
            id = id,
            userId = JsonParser.long(obj, "user_id") ?: 0L,
            userName = JsonParser.string(obj, "user_name") ?: "",
            announcementId = JsonParser.long(obj, "announcement_id") ?: 0L,
            text = JsonParser.string(obj, "text") ?: "",
            rating = JsonParser.int(obj, "rating") ?: 0,
            createdAt = JsonParser.string(obj, "created_at"),
        )
    }

    // ── Профиль пікірлері (жарнама жиынтықтары) ──

    fun parseProfileReviews(root: JsonElement): List<ProfileReview> {
        val array = arrayFrom(root) ?: return emptyList()
        return array.mapNotNull { el ->
            val obj = unwrap(el) as? JsonObject ?: return@mapNotNull null
            val id = JsonParser.long(obj, "id") ?: return@mapNotNull null
            ProfileReview(
                id = id,
                title = JsonParser.string(obj, "title") ?: "",
                price = JsonParser.double(obj, "price") ?: 0.0,
                createdAt = JsonParser.string(obj, "created_at"),
                mainImageUrl = JsonParser.string(obj, "main_image_url") ?: "",
                rating = JsonParser.int(obj, "rating") ?: 0,
                totalReviews = JsonParser.int(obj, "total_reviews") ?: 0,
            )
        }
    }

    // ── Сатушы пікірлері ──

    fun parseSellerSummary(root: JsonElement): SellerReviewSummary {
        val obj = when (val unwrapped = unwrap(root)) {
            is JsonObject -> unwrapped
            is JsonArray -> {
                // Bare list payload — Flutter tolerance: avg/total = 0, items парсинг.
                return SellerReviewSummary(items = unwrapped.mapNotNull { parseSellerReview(it) })
            }
            else -> return SellerReviewSummary()
        }
        return SellerReviewSummary(
            avgRating = JsonParser.double(obj, "avg_rating") ?: 0.0,
            total = JsonParser.int(obj, "total") ?: 0,
            items = JsonParser.arr(obj, "items")?.mapNotNull { parseSellerReview(it) } ?: emptyList(),
        )
    }

    private fun parseSellerReview(el: JsonElement): SellerReview? {
        val obj = el as? JsonObject ?: return null
        return SellerReview(
            id = JsonParser.long(obj, "id") ?: return null,
            authorId = JsonParser.long(obj, "author_id"),
            authorName = JsonParser.string(obj, "author_name") ?: "",
            authorAvatarUrl = JsonParser.string(obj, "author_avatar_url"),
            rating = JsonParser.int(obj, "rating") ?: 0,
            text = JsonParser.string(obj, "text") ?: "",
            createdAt = JsonParser.string(obj, "created_at"),
            orderId = JsonParser.long(obj, "order_id"),
            likeCount = JsonParser.int(obj, "like_count") ?: 0,
            isLiked = JsonParser.bool(obj, "is_liked") ?: false,
        )
    }

    fun parseLikeResult(root: JsonElement, fallbackLiked: Boolean): SellerReviewLikeResult? {
        val obj = unwrap(root) as? JsonObject ?: return null
        val reviewId = JsonParser.long(obj, "review_id") ?: return null
        return SellerReviewLikeResult(
            reviewId = reviewId,
            isLiked = JsonParser.bool(obj, "is_liked") ?: fallbackLiked,
            likeCount = JsonParser.int(obj, "like_count") ?: 0,
        )
    }

    // ── FeedbackAdCard үшін жарнама қысқаша көрінісі ──

    fun parseAnnouncementInfo(root: JsonElement): ReviewAnnouncementInfo? {
        val obj = unwrap(root) as? JsonObject ?: return null
        val id = JsonParser.long(obj, "id") ?: return null
        val images = (obj["image_urls"] as? JsonArray)
            ?: (obj["images"] as? JsonArray)
        val image = images?.firstOrNull()?.let { (it as? JsonPrimitive)?.contentOrNull }
            ?: JsonParser.string(obj, "main_image_url")
            ?: JsonParser.string(obj, "image")
        return ReviewAnnouncementInfo(
            id = id,
            title = JsonParser.string(obj, "title") ?: "",
            price = JsonParser.double(obj, "price"),
            currency = JsonParser.string(obj, "currency"),
            imageUrl = image,
            createdAt = JsonParser.string(obj, "created_at"),
        )
    }

    // ── «Менің пікірлерім» үміткерлері: сатып алушы тапсырыстары ──

    /**
     * GET /orders?role=buyer → әр тапсырыстың announcement_id + title + бағасы.
     * Order өрісі орнына item.announcement_id келуі де мүмкін (legacy OrderModel
     * — тамырда, Smart Calculator — items[] ішінде) — екеуі де жиналады.
     */
    fun parseBuyerOrderCandidates(root: JsonElement): List<MyReviewCandidate> {
        val obj = unwrap(root) as? JsonObject ?: return emptyList()
        val orders = arrayFrom(obj["orders"] ?: obj["items"] ?: root)
            ?: (obj["orders"] as? JsonArray) ?: (obj["items"] as? JsonArray)
            ?: return emptyList()
        val result = LinkedHashMap<Long, MyReviewCandidate>()
        for (el in orders) {
            val order = el as? JsonObject ?: continue
            fun addCandidate(id: Long?, title: String?, price: Double?, image: String?, createdAt: String?) {
                if (id == null || id <= 0 || result.containsKey(id)) return
                result[id] = MyReviewCandidate(
                    announcementId = id,
                    title = title ?: "",
                    imageUrl = image,
                    price = price,
                    createdAt = createdAt,
                )
            }
            // Legacy: тамыр announcement_id.
            addCandidate(
                JsonParser.long(order, "announcement_id"),
                JsonParser.string(order, "title"),
                JsonParser.double(order, "agreed_price") ?: JsonParser.double(order, "total_price"),
                JsonParser.string(order, "main_image_url") ?: JsonParser.string(order, "image"),
                JsonParser.string(order, "created_at"),
            )
            // Smart Calculator: items[] әр жолы announcement_id ұстайды.
            (order["items"] as? JsonArray)?.forEach { itemEl ->
                val item = itemEl as? JsonObject ?: return@forEach
                addCandidate(
                    JsonParser.long(item, "announcement_id"),
                    JsonParser.string(item, "title"),
                    JsonParser.double(item, "unit_price"),
                    null,
                    JsonParser.string(order, "created_at"),
                )
            }
        }
        return result.values.toList()
    }

    // ── Көмекшілер ──

    /** `{data: ...}` / `{data: {…}}` орауларын ашады; массив/объект өзі қалады. */
    private fun unwrap(root: JsonElement): JsonElement {
        var current = root
        var depth = 0
        while (current is JsonObject && current["data"] != null && depth < 4) {
            current = current["data"] ?: break
            depth++
        }
        return current
    }

    private fun arrayFrom(root: JsonElement): List<JsonElement>? = when (root) {
        is JsonArray -> root.toList()
        is JsonObject -> (root["data"] as? JsonArray)?.toList() ?: (root["items"] as? JsonArray)?.toList()
        else -> null
    }
}