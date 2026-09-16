package com.agroland.feature.call.data

import com.agroland.core.network.NetworkModule.safeCall
import com.agroland.core.network.ApiResult
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import retrofit2.Retrofit
import retrofit2.http.GET

/**
 * `GET /turn/credentials` — coturn REST credentials (Flutter
 * TurnCredentialsRepository, 1:1 мінез-құлық):
 *  - backend-test ортасында TURN жоқ → 404 келеді — SILENT fallback (null);
 *  - кэш TTL бойы ұсталады, 5 минут қалғанда қайта алынады;
 *  - қайта сәтсіздік ешқашан қоңырауды үзбейді (STUN-only қалады).
 */
interface TurnCredentialsApi {

    @GET("turn/credentials")
    suspend fun fetch(): JsonElement
}

@Module
@InstallIn(SingletonComponent::class)
object TurnCredentialsApiModule {

    @Provides
    @Singleton
    fun provideTurnCredentialsApi(retrofit: Retrofit): TurnCredentialsApi =
        retrofit.create(TurnCredentialsApi::class.java)
}

@Singleton
class TurnCredentialsRepository @Inject constructor(
    private val api: TurnCredentialsApi,
) {
    @Volatile
    private var cached: TurnCredentials? = null

    @Volatile
    private var fetchedAtMs: Long = 0

    /**
     * Кэш жарамды болса — онтайды; жарамсыз/егде болса — қайта алып қояды.
     * Қандай жағдайда да null қайтаруы мүмкін (silent fallback).
     */
    suspend fun fetch(): TurnCredentials? {
        val cachedCreds = cached
        val fetchedAt = fetchedAtMs
        if (cachedCreds != null && fetchedAt > 0) {
            val marginMs = 5 * 60 * 1000L
            val ttlMs = cachedCreds.ttl * 1000L
            if (System.currentTimeMillis() - fetchedAt < ttlMs - marginMs) {
                return cachedCreds
            }
        }
        return when (val result = safeCall { api.fetch() }) {
            is ApiResult.Success -> {
                val creds = TurnCredentials.fromBackendJson(result.value)
                if (creds != null) {
                    cached = creds
                    fetchedAtMs = System.currentTimeMillis()
                }
                creds
            }
            // 404 (TURN config жоқ) / 401 / қате жауап — silent fallback:
                // TURN-сіз (signaling ice_servers / STUN-only) қоңырау
                // жұмыс істеуі мүмкін, қателікті пайдаланушыға шығармаймыз.
            is ApiResult.Error -> null
        }
    }
}