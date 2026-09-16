package com.agroland.feature.services.data

import com.agroland.core.network.ApiResult
import com.agroland.core.network.NetworkModule.safeCall
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * EGOV API — ауыл шаруашылығы техникасын VIN бойынша тексеру
 * (спек §2: GET /agri-machinery/{vin}, конверт `{status, code, data, message}`).
 */
interface EgovApi {

    @GET("agri-machinery/{vin}")
    suspend fun lookupMachinery(@Path("vin") vin: String): JsonElement
}

@Module
@InstallIn(SingletonComponent::class)
object EgovApiModule {

    @Provides
    @Singleton
    fun provideEgovApi(retrofit: Retrofit): EgovApi =
        retrofit.create(EgovApi::class.java)
}

@Singleton
class EgovRepository @Inject constructor(
    private val api: EgovApi,
) {

    suspend fun lookupMachinery(vin: String): ApiResult<EgovMachineryLookup> =
        safeCall { EgovParser.parseMachineryLookup(api.lookupMachinery(vin)) }
}