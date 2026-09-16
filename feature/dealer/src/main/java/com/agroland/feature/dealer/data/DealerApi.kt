package com.agroland.feature.dealer.data

import com.agroland.core.network.ApiResult
import com.agroland.core.network.NetworkModule.safeCall
import com.agroland.core.network.error.Failure
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Дилер консолі API (Flutter DealerRepository, 1:1):
 *  - GET  /dealer/products?status&search&page&limit
 *  - PATCH /announcement/{id}/activate|deactivate, DELETE /announcement/{id}
 *  - GET  /announcement/reject-message/{id}
 *  - GET/POST /dealer/delivery-zones, PATCH/DELETE /dealer/delivery-zones/{id}
 *  - GET  /dealer/analytics?period_days, /dealer/analytics/timeseries
 *  - GET  /dealer/orders?tab&page&limit
 *  - POST /orders/{id}/accept|reject|ship, GET /orders/{id}/tracking
 *  - GET  /orders/team-pool (bare list), POST /orders/{id}/claim
 *  - GET/POST /dealer/employees, DELETE /dealer/employees/{id}
 *  - GET  /subscription/current (аналитика тариф гейті, {data} қаптамасы)
 * Барлық жауап JsonElement — кешірімді парсинг DealerParser-де.
 */
interface DealerApi {

    @GET("dealer/products")
    suspend fun getProducts(
        @Query("status") status: String,
        @Query("search") search: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): JsonElement

    @PATCH("announcement/{id}/activate")
    suspend fun activateProduct(@Path("id") id: Long): JsonElement

    @PATCH("announcement/{id}/deactivate")
    suspend fun deactivateProduct(@Path("id") id: Long): JsonElement

    @DELETE("announcement/{id}")
    suspend fun deleteProduct(@Path("id") id: Long): JsonElement

    @GET("announcement/reject-message/{id}")
    suspend fun getRejectMessage(@Path("id") id: Long): JsonElement

    @GET("dealer/delivery-zones")
    suspend fun getDeliveryZones(@Query("is_active") isActive: String? = null): JsonElement

    @POST("dealer/delivery-zones")
    suspend fun createDeliveryZone(@Body body: JsonObject): JsonElement

    @PATCH("dealer/delivery-zones/{id}")
    suspend fun updateDeliveryZone(@Path("id") id: Long, @Body body: JsonObject): JsonElement

    @DELETE("dealer/delivery-zones/{id}")
    suspend fun deleteDeliveryZone(@Path("id") id: Long): JsonElement

    @GET("dealer/analytics")
    suspend fun getAnalytics(@Query("period_days") periodDays: Int): JsonElement

    @GET("dealer/analytics/timeseries")
    suspend fun getAnalyticsTimeseries(
        @Query("period_days") periodDays: Int,
        @Query("bucket") bucket: String,
    ): JsonElement

    @GET("dealer/orders")
    suspend fun getDealerOrders(
        @Query("tab") tab: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): JsonElement

    @POST("orders/{id}/accept")
    suspend fun acceptOrder(@Path("id") orderId: Long): JsonElement

    @POST("orders/{id}/reject")
    suspend fun rejectOrder(@Path("id") orderId: Long, @Body body: JsonObject): JsonElement

    @POST("orders/{id}/ship")
    suspend fun shipOrder(@Path("id") orderId: Long): JsonElement

    @GET("orders/{id}/tracking")
    suspend fun getOrderTracking(@Path("id") orderId: Long): JsonElement

    @GET("orders/team-pool")
    suspend fun getTeamPool(): JsonElement

    @POST("orders/{id}/claim")
    suspend fun claimOrder(@Path("id") orderId: Long): JsonElement

    @GET("dealer/employees")
    suspend fun getEmployees(): JsonElement

    @POST("dealer/employees")
    suspend fun addEmployee(@Body body: JsonObject): JsonElement

    @DELETE("dealer/employees/{id}")
    suspend fun deleteEmployee(@Path("id") employeeId: Long): JsonElement

    @GET("subscription/current")
    suspend fun getCurrentSubscription(): JsonElement
}

@Module
@InstallIn(SingletonComponent::class)
object DealerApiModule {

    @Provides
    @Singleton
    fun provideDealerApi(retrofit: Retrofit): DealerApi =
        retrofit.create(DealerApi::class.java)
}

/** UI-ға шығатын қате — backend message > generic (шикі error_code ешқашан жоқ). */
data class DealerError(
    val backendMessage: String? = null,
    val isNetwork: Boolean = false,
)

fun DealerError.displayText(networkMessage: String, genericMessage: String): String =
    backendMessage ?: if (isNetwork) networkMessage else genericMessage

fun Failure.toDealerError(): DealerError = when (this) {
    is Failure.Network -> DealerError(isNetwork = true)
    else -> DealerError(backendMessage = (this as? Failure.Http)?.error?.message)
}

/**
 * Дилер репозиторісі — барлық шақыру safeCall арқылы, парсинг DealerParser.
 * Flutter мінез-құлығы: timeseries {data} қаптамасы, team-pool bare list,
 * zones DELETE 404 tolerance (жергілікті жою жеткілікті), employees қате
 * денелері plain string болуы мүмкін — message жолы ретінде көрсетіледі.
 */
@Singleton
class DealerRepository @Inject constructor(
    private val api: DealerApi,
) {

    suspend fun getProducts(
        status: String,
        search: String? = null,
        page: Int = 1,
        limit: Int = 20,
    ): ApiResult<DealerProductsResponse> =
        safeCall { DealerParser.parseProducts(api.getProducts(status, search, page, limit)) }

    suspend fun activateProduct(id: Long): ApiResult<Unit> =
        safeCall { api.activateProduct(id); Unit }

    suspend fun deactivateProduct(id: Long): ApiResult<Unit> =
        safeCall { api.deactivateProduct(id); Unit }

    suspend fun deleteProduct(id: Long): ApiResult<Unit> =
        safeCall { api.deleteProduct(id); Unit }

    /** GET /announcement/reject-message/{id} → {message}; бос болса null. */
    suspend fun getRejectMessage(id: Long): ApiResult<String> = safeCall {
        val message = DealerParser.parseRejectMessage(api.getRejectMessage(id))
        if (message.isBlank()) throw NoSuchElementException("message missing") else message
    }

    suspend fun getDeliveryZones(isActive: Boolean? = null): ApiResult<List<DeliveryZone>> =
        safeCall {
            DealerParser.parseZones(api.getDeliveryZones(isActive?.toString()))
        }

    suspend fun createDeliveryZone(zone: DeliveryZoneDraft): ApiResult<DeliveryZone> =
        parseZoneBody { api.createDeliveryZone(zone.toJson()) }

    suspend fun updateDeliveryZone(id: Long, zone: DeliveryZoneDraft): ApiResult<DeliveryZone> =
        parseZoneBody { api.updateDeliveryZone(id, zone.toJson()) }

    /** 404 келсе — жауап «жеткілікті»: аймақ жергілікті тізімнен жойылады. */
    suspend fun deleteDeliveryZone(id: Long): ApiResult<Unit> =
        when (val result = safeCall { api.deleteDeliveryZone(id); Unit }) {
            is ApiResult.Success -> result
            is ApiResult.Error -> {
                val failure = result.failure
                if (failure is Failure.Http && failure.error.httpStatus == 404) {
                    ApiResult.Success(Unit)
                } else {
                    result
                }
            }
        }

    suspend fun getAnalytics(periodDays: Int): ApiResult<DealerAnalytics> =
        safeCall {
            DealerParser.parseAnalytics(api.getAnalytics(periodDays))
                ?: throw NoSuchElementException("analytics missing")
        }

    suspend fun getAnalyticsTimeseries(
        periodDays: Int,
        bucket: String,
    ): ApiResult<List<AnalyticsPoint>> =
        safeCall { DealerParser.parseTimeseries(api.getAnalyticsTimeseries(periodDays, bucket)) }

    suspend fun getDealerOrders(tab: String, page: Int = 1, limit: Int = 20): ApiResult<DealerOrdersResponse> =
        safeCall { DealerParser.parseOrders(api.getDealerOrders(tab, page, limit)) }

    suspend fun acceptOrder(orderId: Long): ApiResult<Unit> =
        safeCall { api.acceptOrder(orderId); Unit }

    suspend fun rejectOrder(orderId: Long, reason: String?): ApiResult<Unit> =
        safeCall {
            api.rejectOrder(orderId, buildJsonObject { reason?.let { put("reason", it) } })
            Unit
        }

    suspend fun shipOrder(orderId: Long): ApiResult<Unit> =
        safeCall { api.shipOrder(orderId); Unit }

    suspend fun getOrderTracking(orderId: Long): ApiResult<OrderTracking> =
        safeCall {
            DealerParser.parseTracking(api.getOrderTracking(orderId))
                ?: throw NoSuchElementException("tracking missing")
        }

    suspend fun getTeamPool(): ApiResult<List<TeamPoolOrder>> =
        safeCall { DealerParser.parseTeamPool(api.getTeamPool()) }

    suspend fun claimOrder(orderId: Long): ApiResult<ClaimResult> =
        safeCall {
            DealerParser.parseClaim(api.claimOrder(orderId))
                ?: throw NoSuchElementException("claim missing")
        }

    suspend fun getEmployees(): ApiResult<List<DealerEmployee>> =
        safeCall { DealerParser.parseEmployees(api.getEmployees()) }

    suspend fun addEmployee(
        name: String,
        phone: String,
        dealerRole: String,
        iin: String,
    ): ApiResult<DealerEmployee> = safeCall {
        val root = api.addEmployee(
            buildJsonObject {
                put("name", name)
                put("phone", phone)
                put("dealer_role", dealerRole)
                put("iin", iin)
            },
        )
        val obj = root as? JsonObject
        DealerParser.parseEmployees(
            buildJsonObject { put("employees", buildJsonArray { add(root) }) },
        ).firstOrNull()
            ?: DealerEmployee(
                id = 0L,
                name = name,
                dealerRole = dealerRole,
                iin = iin,
                phone = phone,
                profilePicture = null,
            )
    }

    suspend fun deleteEmployee(employeeId: Long): ApiResult<Unit> =
        safeCall { api.deleteEmployee(employeeId); Unit }

    /**
     * Тариф шарттары — GET /subscription/current → {data} → conditions ??
     * plan.conditions. Парсинг сәтсіз/жоқ жазылым — синтетикалық fallback
     * (Flutter CurrentTariffNotifier: гейттер ЕШҚАШАН қате көрсетпейді).
     */
    suspend fun getTariffConditions(): ApiResult<TariffConditions> = safeCall {
        val root = api.getCurrentSubscription() as? JsonObject
        TariffConditions.effectiveFrom(root)
    }

    private suspend fun parseZoneBody(block: suspend () -> JsonElement): ApiResult<DeliveryZone> {
        val raw = safeCall(block)
        return when (raw) {
            is ApiResult.Success ->
                DealerParser.parseZones(raw.value).firstOrNull()
                    ?.let { ApiResult.Success(it) }
                    ?: ApiResult.Error(Failure.Parsing(IllegalStateException("zone missing")))
            is ApiResult.Error -> raw
        }
    }
}

/**
 * Жеткізу аймағын сақтау денесі — Flutter AddEditLogisticsPage._save пішіні:
 * name, country_id, region_id (міндетті), delivery_cost (>0), is_active, note,
 * delivery_time_from/to, zone_prices[{district_id, delivery_cost}],
 * available_days (сұрыпталған, 1=Дс..7=Жс).
 */
data class DeliveryZoneDraft(
    val name: String,
    val countryId: Int,
    val regionId: Int,
    val deliveryCost: Double,
    val deliveryDaysMin: Int?,
    val deliveryDaysMax: Int?,
    val isActive: Boolean,
    val note: String,
    val timeStart: String?,
    val timeEnd: String?,
    val zonePrices: List<ZonePriceItem>,
    val availableDays: Set<Int>,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("name", name)
        put("country_id", countryId)
        put("region_id", regionId)
        put("delivery_cost", deliveryCost)
        deliveryDaysMin?.let { put("delivery_days_min", it) }
        deliveryDaysMax?.let { put("delivery_days_max", it) }
        put("is_active", isActive)
        put("note", note)
        timeStart?.takeIf { it.isNotEmpty() }?.let { put("delivery_time_from", it) }
        timeEnd?.takeIf { it.isNotEmpty() }?.let { put("delivery_time_to", it) }
        put("zone_prices", buildJsonArray {
            zonePrices.filter { it.districtId != null }.forEach { p ->
                add(
                    buildJsonObject {
                        put("district_id", p.districtId!!)
                        put("delivery_cost", p.price)
                    },
                )
            }
        })
        put("available_days", buildJsonArray {
            availableDays.sorted().forEach { add(it) }
        })
    }

    companion object {
        /** Өңдеу кезінде бар зонадан форма нұсқасы (available_days, time_start …). */
        fun from(zone: DeliveryZone): DeliveryZoneDraft = DeliveryZoneDraft(
            name = zone.name.ifBlank { zone.displayName },
            countryId = zone.countryId,
            regionId = zone.regionId,
            deliveryCost = zone.deliveryCost,
            deliveryDaysMin = zone.deliveryDaysMin,
            deliveryDaysMax = zone.deliveryDaysMax,
            isActive = zone.isActive,
            note = zone.note,
            timeStart = zone.timeStart,
            timeEnd = zone.timeEnd,
            zonePrices = zone.zonePrices,
            availableDays = zone.availableDays.toSet(),
        )
    }
}