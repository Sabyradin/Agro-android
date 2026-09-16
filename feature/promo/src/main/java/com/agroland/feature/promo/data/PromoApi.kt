package com.agroland.feature.promo.data

import com.agroland.core.network.ApiResult
import com.agroland.core.network.NetworkModule.safeCall
import com.agroland.core.network.error.ApiError
import com.agroland.core.network.error.Failure
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Url

/**
 * Промо v2 API (Flutter PromoV2Repository + AnnouncementsRepository промо
 * бөлігі, 1:1):
 *  - GET /promo-v2/catalog                        → {items:[...]} (анонимді)
 *  - SKU-ға тән activate_endpoint (POST/PATCH/PUT, динамикалық path)
 *  - POST /banners/main/image (multipart `file`)  → {image_url}
 *  - GET /announcement/{id}/promotion             → {has_promotion, promotion}
 *  - GET /user/announcements/promotions           → {announcements:[...]}
 *  - PATCH /promotion/{id}/auto-renewal            → {auto_renewal}
 */
interface PromoApi {

    @GET("promo-v2/catalog")
    suspend fun catalog(): JsonElement

    /** activate_endpoint.path бойынша динамикалық шақыру ({announcement_id} алмастырылған). */
    @POST
    suspend fun dynamicPost(@Url path: String, @Body body: JsonObject): JsonElement

    @PATCH
    suspend fun dynamicPatch(@Url path: String, @Body body: JsonObject): JsonElement

    @PUT
    suspend fun dynamicPut(@Url path: String, @Body body: JsonObject): JsonElement

    @Multipart
    @POST("banners/main/image")
    suspend fun uploadBannerImage(@Part file: MultipartBody.Part): JsonObject

    @GET("announcement/{id}/promotion")
    suspend fun announcementPromotion(@Path("id") announcementId: Long): JsonElement

    @GET("user/announcements/promotions")
    suspend fun userAnnouncementsPromotions(): JsonElement

    @PATCH("promotion/{id}/auto-renewal")
    suspend fun toggleAutoRenewal(
        @Path("id") promotionId: Long,
        @Body body: JsonObject,
    ): JsonElement
}

@Module
@InstallIn(SingletonComponent::class)
object PromoApiModule {

    @Provides
    @Singleton
    fun providePromoApi(retrofit: Retrofit): PromoApi =
        retrofit.create(PromoApi::class.java)
}

@Singleton
class PromoRepository @Inject constructor(
    private val api: PromoApi,
) {

    /** GET /promo-v2/catalog — сатып алуға болатын промо қызметтер каталогы. */
    suspend fun getCatalog(): ApiResult<List<PromoCatalogItem>> =
        safeCall { PromoCatalogItem.listFromJson(api.catalog()) }

    /**
     * Промо қызметті белсендіру (Flutter PromoV2Repository.activate, 1:1):
     *  - path ішіндегі {announcement_id} алмастырылады;
     *  - каталог body үлгісіне extraBody қосылады (banner: image_url/cta_url/...);
     *  - method бойынша диспетчер (әдепкі POST);
     *  - 409 BANNER_SLOT_ALREADY_ACTIVE → дәл сол path-пен PATCH fallback
     *    (тек banner kind — boost/vip/auto_renew-де PATCH семантикасы жоқ);
     *  - жауап {data:{...}} қаптамасында келуі мүмкін.
     */
    suspend fun activate(
        item: PromoCatalogItem,
        announcementId: Long? = null,
        extraBody: Map<String, Any?> = emptyMap(),
    ): ApiResult<PromoActivateResult> {
        val endpoint = item.activateEndpoint
            ?: return ApiResult.Error(Failure.Unknown(IllegalStateException("no activate_endpoint")))
        val path = endpoint.resolvedPath(announcementId)
        val body = mergeBody(endpoint.body, extraBody)
        val usePatch = endpoint.method == "PATCH"

        var result = dispatch(endpoint.method, path, body)

        // 409 BANNER_SLOT_ALREADY_ACTIVE → PATCH fallback (web handoff §1.3).
        if (result is ApiResult.Error && item.kind == "banner" && !usePatch) {
            val failure = result.failure
            if (failure is Failure.Http &&
                failure.error.code == "BANNER_SLOT_ALREADY_ACTIVE"
            ) {
                result = dispatch("PATCH", path, body)
            }
        }

        return when (result) {
            is ApiResult.Success -> safeCall {
                val root = result.value as? JsonObject
                val data = (root?.get("data") as? JsonObject) ?: root
                PromoActivateResult.fromJson(data)
                    ?: PromoActivateResult(
                        announcementId = null,
                        boostMultiplier = null,
                        boostExpiresAt = null,
                        isVipSeller = null,
                        vipSellerExpiresAt = null,
                        bannerId = null,
                        bannerStatus = null,
                        bannerStartsAt = null,
                        bannerExpiresAt = null,
                        balance = null,
                    )
            }
            is ApiResult.Error -> result
        }
    }

    private suspend fun dispatch(
        method: String,
        path: String,
        body: JsonObject,
    ): ApiResult<JsonElement> = safeCall {
        when (method.uppercase()) {
            "PATCH" -> api.dynamicPatch(path, body)
            "PUT" -> api.dynamicPut(path, body)
            else -> api.dynamicPost(path, body)
        }
    }

    private fun mergeBody(
        template: JsonObject?,
        extraBody: Map<String, Any?>,
    ): JsonObject = buildJsonObject {
        template?.forEach { (key, value) -> put(key, value) }
        extraBody.forEach { (key, value) ->
            when (value) {
                null -> Unit
                is String -> put(key, value)
                is Number -> put(key, value)
                is Boolean -> put(key, value)
            }
        }
    }

    /** POST /banners/main/image (multipart `file`) → {image_url}. */
    suspend fun uploadBannerImage(part: MultipartBody.Part): ApiResult<String> =
        safeCall {
            val root = api.uploadBannerImage(part)
            val data = (root["data"] as? JsonObject) ?: root
            val url = (data["image_url"] as? JsonPrimitive)?.contentOrNull
                ?: return@safeCall throw kotlinx.serialization.SerializationException(
                    "image_url missing",
                )
            url
        }

    /** GET /announcement/{id}/promotion — промо болмаса null (қате емес). */
    suspend fun getAnnouncementPromotion(announcementId: Long): ApiResult<Promotion?> =
        safeCall { Promotion.fromAnnouncementResponse(api.announcementPromotion(announcementId)) }

    /** GET /user/announcements/promotions — өз жарнамалары + промолары. */
    suspend fun getUserAnnouncementsPromotions(): ApiResult<List<AnnouncementPromotion>> =
        safeCall { AnnouncementPromotion.listFromResponse(api.userAnnouncementsPromotions()) }

    /** PATCH /promotion/{id}/auto-renewal — автожаңартуды қосу/өшіру. */
    suspend fun toggleAutoRenewal(promotionId: Long, autoRenewal: Boolean): ApiResult<Unit> =
        safeCall {
            api.toggleAutoRenewal(
                promotionId,
                buildJsonObject { put("auto_renewal", autoRenewal) },
            )
            Unit
        }

    companion object {
        /** INSUFFICIENT_BALANCE диалогіне керек «жетіспейтін» сома. */
        fun missingAmountOf(failure: Failure): Double {
            val error = (failure as? Failure.Http)?.error ?: return 0.0
            return error.missingAmount
                ?: error.requiredAmount?.let { required ->
                    error.balance?.let { balance -> (required - balance).coerceAtLeast(0.0) }
                }
                ?: error.balance ?: 0.0
        }

        /** Қатенің коды (құрылымдық диспетч үшін). */
        fun errorCodeOf(failure: Failure): String? =
            (failure as? Failure.Http)?.error?.code

        /** Backend-тің адам тіліндегі хабарламасы (жоқ болса null). */
        fun backendMessageOf(failure: Failure): String? =
            (failure as? Failure.Http)?.error?.message
    }
}