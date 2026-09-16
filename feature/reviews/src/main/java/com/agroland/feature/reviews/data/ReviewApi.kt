package com.agroland.feature.reviews.data

import com.agroland.core.network.ApiResult
import com.agroland.core.network.NetworkModule.safeCall
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Пікірлер API — SWIFT_REWRITE_SPEC L124 (Flutter reviews_repository.dart, 1:1):
 *  - POST /review                          (announcement_id, text, rating 0-5)
 *  - GET  /reviews/{announcement_id}        (жарнама пікірлері)
 *  - GET  /user/{user_id}/reviews           (жарнама пікір жиынтықтары)
 *  - POST /reviews/seller                   (seller_id, rating 1-5, text?, order_id?)
 *  - GET  /seller/{user_id}/reviews         (avg + total + items)
 *  - POST/DELETE /reviews/seller/{id}/like
 * «Менің пікірлерім» (spec §10) үшін: GET /orders?role=buyer + GET /announcement/{id}.
 */
interface ReviewApi {

    @POST("review")
    suspend fun sendReview(@Body body: JsonObject): JsonElement

    @GET("reviews/{announcementId}")
    suspend fun getReviews(@Path("announcementId") announcementId: Long): JsonElement

    @GET("user/{userId}/reviews")
    suspend fun getProfileReviews(@Path("userId") userId: Long): JsonElement

    @GET("announcement/{id}")
    suspend fun getAnnouncement(@Path("id") id: Long): JsonElement

    @POST("reviews/seller")
    suspend fun sendSellerReview(@Body body: JsonObject): JsonElement

    @GET("seller/{userId}/reviews")
    suspend fun getSellerReviews(@Path("userId") userId: Long): JsonElement

    @POST("reviews/seller/{reviewId}/like")
    suspend fun likeSellerReview(@Path("reviewId") reviewId: Long): JsonElement

    @DELETE("reviews/seller/{reviewId}/like")
    suspend fun unlikeSellerReview(@Path("reviewId") reviewId: Long): JsonElement

    @GET("orders")
    suspend fun getOrders(
        @Query("role") role: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
    ): JsonElement
}

@Singleton
class ReviewRepository @Inject constructor(
    private val api: ReviewApi,
) {

    /** Пікір қалдыру (POST /review) — rating 0-5, мәтін 1000 таңбадан аспайды. */
    suspend fun sendReview(announcementId: Long, text: String, rating: Int): ApiResult<Unit> =
        safeCall {
            api.sendReview(
                buildJsonObject {
                    put("announcement_id", announcementId)
                    put("rating", rating)
                    if (text.isNotBlank()) put("text", text)
                },
            )
            Unit
        }

    suspend fun getReviews(announcementId: Long): ApiResult<List<Review>> =
        safeCall { ReviewParser.parseReviews(api.getReviews(announcementId)) }

    suspend fun getProfileReviews(userId: Long): ApiResult<List<ProfileReview>> =
        safeCall { ReviewParser.parseProfileReviews(api.getProfileReviews(userId)) }

    suspend fun getAnnouncementInfo(id: Long): ApiResult<ReviewAnnouncementInfo> =
        safeCall { ReviewParser.parseAnnouncementInfo(api.getAnnouncement(id)) ?: ReviewAnnouncementInfo(id = id) }

    suspend fun getSellerReviews(userId: Long): ApiResult<SellerReviewSummary> =
        safeCall { ReviewParser.parseSellerSummary(api.getSellerReviews(userId)) }

    suspend fun sendSellerReview(sellerId: Long, rating: Int, text: String?, orderId: Long?): ApiResult<Unit> =
        safeCall {
            api.sendSellerReview(
                buildJsonObject {
                    put("seller_id", sellerId)
                    put("rating", rating)
                    if (!text.isNullOrBlank()) put("text", text)
                    orderId?.let { put("order_id", it) }
                },
            )
            Unit
        }

    /** Like — авторитетті is_liked + like_count қайтарылады (Flutter tolerance). */
    suspend fun likeSellerReview(reviewId: Long): ApiResult<SellerReviewLikeResult> =
        safeCall {
            ReviewParser.parseLikeResult(api.likeSellerReview(reviewId), fallbackLiked = true)
                ?: SellerReviewLikeResult(reviewId, isLiked = true, likeCount = 0)
        }

    suspend fun unlikeSellerReview(reviewId: Long): ApiResult<SellerReviewLikeResult> =
        safeCall {
            ReviewParser.parseLikeResult(api.unlikeSellerReview(reviewId), fallbackLiked = false)
                ?: SellerReviewLikeResult(reviewId, isLiked = false, likeCount = 0)
        }

    /** «Менің пікірлерім» үміткерлері — сатып алушы тапсырыстарының жарнамалары. */
    suspend fun getBuyerOrderCandidates(): ApiResult<List<MyReviewCandidate>> =
        safeCall { ReviewParser.parseBuyerOrderCandidates(api.getOrders(role = "buyer")) }
}

@Module
@InstallIn(SingletonComponent::class)
object ReviewApiModule {

    @Provides
    @Singleton
    fun provideReviewApi(retrofit: Retrofit): ReviewApi = retrofit.create(ReviewApi::class.java)
}