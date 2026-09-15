package com.agroland.feature.location.data

import com.agroland.core.network.ApiResult
import com.agroland.core.network.NetworkModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject
import retrofit2.Retrofit

/**
 * Локация каталогы — GET /countries, /regions, /districts, /location/reverse.
 * Атамасына қарамастан бұл жария (auth емес) эндпоинттер.
 */
interface LocationApi {

    /** {countries:[{id, name_kz/en/zh/ru, ...}]}. */
    @retrofit2.http.GET("countries")
    suspend fun getCountries(): JsonObject

    /** {regions:[{id, country_id, name_*, latitude, longitude}]}. */
    @retrofit2.http.GET("regions")
    suspend fun getRegions(@retrofit2.http.Query("country_id") countryId: Int): JsonObject

    /** {districts:[{id, region_id, name_*, latitude, longitude}]} — region_id немесе country_id бойынша. */
    @retrofit2.http.GET("districts")
    suspend fun getDistricts(
        @retrofit2.http.Query("region_id") regionId: Int? = null,
        @retrofit2.http.Query("country_id") countryId: Int? = null,
    ): JsonObject

    /** Nominatim reverse → каталог ID-лері: {country_id, region_id, district_id, *_ru}. */
    @retrofit2.http.GET("location/reverse")
    suspend fun reverseGeocode(
        @retrofit2.http.Query("lat") lat: Double,
        @retrofit2.http.Query("lng") lng: Double,
    ): JsonObject
}

@Module
@InstallIn(SingletonComponent::class)
internal object LocationModule {

    @Provides
    @Singleton
    fun provideLocationApi(retrofit: Retrofit): LocationApi =
        retrofit.create(LocationApi::class.java)

    @Provides
    @Singleton
    fun provideLocationRepository(api: LocationApi): LocationRepository =
        LocationRepository(api)
}

/**
 * Локация репозиторісі — оқу ғана (жазу /user/location profile модулінде).
 * Барлық шақыру NetworkModule.safeCall арқылы.
 */
@Singleton
class LocationRepository @Inject constructor(
    private val api: LocationApi,
) {

    suspend fun getCountries(): ApiResult<List<CatalogLocation>> =
        safeCall { LocationParser.parseCountries(api.getCountries()) }

    suspend fun getRegions(countryId: Int): ApiResult<List<CatalogLocation>> =
        safeCall { LocationParser.parseRegions(api.getRegions(countryId)) }

    /** Аудандар — облыс бойынша (болмаса ел бойынша). */
    suspend fun getDistricts(regionId: Int? = null, countryId: Int? = null): ApiResult<List<CatalogLocation>> =
        safeCall { LocationParser.parseDistricts(api.getDistricts(regionId, countryId)) }

    suspend fun reverseGeocode(lat: Double, lng: Double): ApiResult<ReverseGeocodeResult?> = safeCall {
        LocationParser.parseReverseGeocode(api.reverseGeocode(lat, lng))
    }

    private suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> =
        NetworkModule.safeCall(block)
}