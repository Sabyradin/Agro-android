package com.agroland.feature.marketplace.data

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
 * Маркетплейс жазу API (spec: POST /announcements multipart, PATCH/DELETE
 * /announcement/{id}, activate|deactivate, reject-message, /user/announcements/{status},
 * bulk-upload, /ai/ad-content, POST /demands — MakeOffer).
 * Жауаптар JsonObject — кешірімді парсинг MarketplaceModels-те.
 */
interface WriteApi {

    /**
     * Жарнама жасау — multipart: мәтіндік өрістер PartMap-та (title, description,
     * price, category_id, ...), файлдар мен қайталанатын массивтер (images, video,
     * contact_numbers, keywords, delivery_zone_ids) parts ішінде.
     */
    @retrofit2.http.Multipart
    @retrofit2.http.POST("announcements")
    suspend fun createAnnouncement(
        @retrofit2.http.PartMap fields: Map<String, RequestBody>,
        @retrofit2.http.Part parts: List<MultipartBody.Part>,
    ): JsonObject

    /** Өңдеу — PATCH /announcement/{id} (multipart); backend статусты PENDING жасайды. */
    @retrofit2.http.Multipart
    @retrofit2.http.PATCH("announcement/{id}")
    suspend fun updateAnnouncement(
        @retrofit2.http.Path("id") id: Long,
        @retrofit2.http.PartMap fields: Map<String, RequestBody>,
        @retrofit2.http.Part parts: List<MultipartBody.Part>,
    ): JsonObject

    @retrofit2.http.PATCH("announcement/{id}/activate")
    suspend fun activateAnnouncement(@retrofit2.http.Path("id") id: Long): JsonObject

    @retrofit2.http.PATCH("announcement/{id}/deactivate")
    suspend fun deactivateAnnouncement(@retrofit2.http.Path("id") id: Long): JsonObject

    @retrofit2.http.DELETE("announcement/{id}")
    suspend fun deleteAnnouncement(@retrofit2.http.Path("id") id: Long): Response<ResponseBody>

    /** Модератор қайтарған себебі — {message} түрінде күтіледі, парсер иілімді. */
    @retrofit2.http.GET("announcement/reject-message/{id}")
    suspend fun getRejectMessage(@retrofit2.http.Path("id") id: Long): JsonObject

    /** Менің жарнамаларым — status: active|inactive|pending|rejected. */
    @retrofit2.http.GET("user/announcements/{status}")
    suspend fun getMyAnnouncements(
        @retrofit2.http.Path("status") status: String,
        @retrofit2.http.Query("page") page: Int,
        @retrofit2.http.Query("limit") limit: Int,
    ): JsonObject

    /** Excel шаблон (.xlsx) — lang: kk|ru|en|zh. Бинарлы файл → ResponseBody. */
    @retrofit2.http.Streaming
    @retrofit2.http.GET("announcements/bulk-upload/template")
    suspend fun getBulkTemplate(@retrofit2.http.Query("lang") lang: String): Response<ResponseBody>

    /** Кестелі файл (.xlsx/.csv/.tsv) → әр жолға жарнама. Бағандар: title, category_id, subcategory_id, user_location_id, measurement_unit. */
    @retrofit2.http.Multipart
    @retrofit2.http.POST("announcements/bulk-upload")
    suspend fun bulkUpload(@retrofit2.http.Part file: MultipartBody.Part): JsonObject

    /** AI мазмұн — {ad_title, lang_code}; сипаттама + категория/сабкатегория ұсынады. */
    @retrofit2.http.POST("ai/ad-content")
    suspend fun generateAdContent(@retrofit2.http.Body body: JsonObject): JsonObject

    /** Бизнес жеткізу аймақтары — create/edit формасындағы delivery_zone_ids үшін. */
    @retrofit2.http.GET("business/delivery-zones")
    suspend fun getDeliveryZones(): JsonObject

    /** Сұраныс (MakeOffer) — JSON негізгі әрекет. */
    @retrofit2.http.POST("demands")
    suspend fun createDemand(@retrofit2.http.Body body: JsonObject): JsonObject

    /** Сұраныс — form fallback (backend JSON емес form күтсе). */
    @retrofit2.http.Multipart
    @retrofit2.http.POST("demands")
    suspend fun createDemandForm(
        @retrofit2.http.PartMap fields: Map<String, RequestBody>,
    ): JsonObject
}

@Module
@InstallIn(SingletonComponent::class)
object WriteApiModule {

    @Provides
    @Singleton
    fun provideWriteApi(retrofit: Retrofit): WriteApi = retrofit.create(WriteApi::class.java)
}