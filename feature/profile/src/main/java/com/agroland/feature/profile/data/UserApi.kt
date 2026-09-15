package com.agroland.feature.profile.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit

/**
 * Профиль/компания/KYC API (spec: GET/PATCH /user/profile, POST /user/avatar,
 * POST/PATCH/DELETE /user/location, POST /user/profile/company/{representative|about|contacts|decor},
 * POST /user/business-terms/accept, GET және POST — /verification/…).
 * Жауаптар JsonObject — кешірімді парсинг ProfileModels-те.
 */
interface UserApi {

    /** Өз профилі: user{...company_info...}, locations[], announcements{counts}, messages. */
    @retrofit2.http.GET("user/profile")
    suspend fun getProfile(): JsonObject

    /** Additive PATCH: {name?, email?, is_vat_payer?}; email "" → NULL. Толық профиль қайтады. */
    @retrofit2.http.PATCH("user/profile")
    suspend fun updateProfile(@retrofit2.http.Body body: JsonObject): JsonObject

    /** Avatar — multipart 'file' → {avatar_url}. */
    @retrofit2.http.Multipart
    @retrofit2.http.POST("user/avatar")
    suspend fun uploadAvatar(@retrofit2.http.Part file: MultipartBody.Part): JsonObject

    /** Email өзгерту (legacy add-email, домен whitelist). */
    @retrofit2.http.PUT("user/email")
    suspend fun updateEmail(@retrofit2.http.Body body: JsonObject): JsonObject

    /** Аккаунтты жою (soft delete, is_active=False). */
    @retrofit2.http.DELETE("user/account")
    suspend fun deleteAccount(): Response<ResponseBody>

    // Мекенжайлар (user locations).

    @retrofit2.http.POST("user/location")
    suspend fun createLocation(@retrofit2.http.Body body: JsonObject): JsonObject

    @retrofit2.http.PATCH("user/location/{id}")
    suspend fun updateLocation(
        @retrofit2.http.Path("id") id: Long,
        @retrofit2.http.Body body: JsonObject,
    ): JsonObject

    @retrofit2.http.DELETE("user/location/{id}")
    suspend fun deleteLocation(@retrofit2.http.Path("id") id: Long): Response<ResponseBody>

    // Компания саб-ресурстары (dealer).

    /** representative: {name, position} / about: {text} / contacts: {phone?, website?, telegram?, whatsapp?}. */
    @retrofit2.http.POST("user/profile/company/{section}")
    suspend fun updateCompanySection(
        @retrofit2.http.Path("section") section: String,
        @retrofit2.http.Body body: JsonObject,
    ): JsonObject

    /** decor: multipart {logo?, banner?}. */
    @retrofit2.http.Multipart
    @retrofit2.http.POST("user/profile/company/decor")
    suspend fun updateCompanyDecor(
        @retrofit2.http.Part logo: MultipartBody.Part?,
        @retrofit2.http.Part banner: MultipartBody.Part?,
    ): JsonObject

    /** Бизнес шарттарды қабылдау (idempotent; 403 non-BUSINESS үшін). */
    @retrofit2.http.POST("user/business-terms/accept")
    suspend fun acceptBusinessTerms(): JsonObject

    // KYC верификация.

    /** {status: not_submitted|pending|approved|rejected, rejection_reason?, submitted_at?, documents[]}. */
    @retrofit2.http.GET("verification/status")
    suspend fun getVerificationStatus(): JsonObject

    /** multipart documents[] (1..5, ≤10MB, jpeg/png/webp/pdf) + optional full_name, iin. */
    @retrofit2.http.Multipart
    @retrofit2.http.POST("verification/submit")
    suspend fun submitVerification(
        @retrofit2.http.Part("full_name") fullName: RequestBody?,
        @retrofit2.http.Part("iin") iin: RequestBody?,
        @retrofit2.http.Part documents: List<MultipartBody.Part>,
    ): JsonObject

    // Публіді мәліметтер (басқа фазаларда қолданылады: сатушы профилі, пікірлер).

    @retrofit2.http.GET("user/{user_id}")
    suspend fun getPublicUser(@retrofit2.http.Path("user_id") userId: Long): JsonObject

    @retrofit2.http.GET("user/{user_id}/reviews")
    suspend fun getPublicUserReviews(@retrofit2.http.Path("user_id") userId: Long): JsonObject
}

@Module
@InstallIn(SingletonComponent::class)
object UserApiModule {

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)
}