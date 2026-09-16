package com.agroland.feature.demand.data

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
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Сұраныс API — спек §2 (SWIFT_REWRITE_SPEC L118):
 *  - GET    /demands?page=&limit=   (Laravel парақтауы)
 *  - POST   /demands               (title, description, currency, category_id,
 *                                   subcategory_id, max_price, user_location_id,
 *                                   measurement_unit, quantity)
 *  - GET/PATCH/DELETE /demand/{id}
 *  - PATCH  /demand/{id}/activate | /demand/{id}/deactivate
 *
 * ISSUES.md #39: SWIFT_REWRITE_MAPS `/demands/{id}` нұсқасын қолданбаймыз —
 * спек мәтініндегі `/demand/{id}` (жалғыз) /announcement конвенциясына сай.
 */
interface DemandApi {

    @GET("demands")
    suspend fun getDemands(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): JsonElement

    @POST("demands")
    suspend fun createDemand(@Body body: JsonObject): JsonElement

    @GET("demand/{id}")
    suspend fun getDemand(@Path("id") id: Long): JsonElement

    @PATCH("demand/{id}")
    suspend fun updateDemand(
        @Path("id") id: Long,
        @Body body: JsonObject,
    ): JsonElement

    @DELETE("demand/{id}")
    suspend fun deleteDemand(@Path("id") id: Long): JsonElement

    @PATCH("demand/{id}/activate")
    suspend fun activateDemand(@Path("id") id: Long): JsonElement

    @PATCH("demand/{id}/deactivate")
    suspend fun deactivateDemand(@Path("id") id: Long): JsonElement
}

@Module
@InstallIn(SingletonComponent::class)
object DemandApiModule {

    @Provides
    @Singleton
    fun provideDemandApi(retrofit: Retrofit): DemandApi =
        retrofit.create(DemandApi::class.java)
}

@Singleton
class DemandRepository @Inject constructor(
    private val api: DemandApi,
) {

    suspend fun getDemands(page: Int, limit: Int = 20): ApiResult<DemandsPage> =
        safeCall { DemandParser.parseDemands(api.getDemands(page, limit)) }

    suspend fun getDemand(id: Long): ApiResult<DemandItem?> =
        safeCall { DemandParser.parseDemand(api.getDemand(id)) }

    suspend fun createDemand(draft: DemandDraft): ApiResult<DemandItem> =
        safeCall {
            DemandParser.parseDemand(api.createDemand(DemandParser.buildBody(draft)))
                ?: DemandItem(id = -1, title = draft.title)
        }

    suspend fun updateDemand(id: Long, draft: DemandDraft): ApiResult<DemandItem> =
        safeCall {
            DemandParser.parseDemand(api.updateDemand(id, DemandParser.buildBody(draft)))
                ?: DemandItem(id = id, title = draft.title)
        }

    suspend fun deleteDemand(id: Long): ApiResult<Unit> =
        safeCall { api.deleteDemand(id); Unit }

    suspend fun activateDemand(id: Long): ApiResult<Unit> =
        safeCall { api.activateDemand(id); Unit }

    suspend fun deactivateDemand(id: Long): ApiResult<Unit> =
        safeCall { api.deactivateDemand(id); Unit }
}