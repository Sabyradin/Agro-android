package com.agroland.core.analytics

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Monitoring репозиторийі — Flutter monitoring_repository.dart 1:1.
 * POST {monitoringApiUrl}/monitoring/events (бөлек Cloud Run host,
 * /api/v1 ЖОҚ). Өз OkHttp клиенті (15с таймаут — fire-and-forget, scale-to-zero
 * cold start блогы болмауы керек) — негізгі клиентпен байланыспайды,
 * MonitoringInterceptor циклі пайда болмайды.
 */
@Singleton
class MonitoringRepository @Inject constructor(
    private val config: AnalyticsConfig,
) {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(java.time.Duration.ofSeconds(15))
            .readTimeout(java.time.Duration.ofSeconds(15))
            .writeTimeout(java.time.Duration.ofSeconds(15))
            .build()
    }

    /** Сәтті болса true (2xx); қате/желі жоқ — false. Ешқашан exception лақтырмайды. */
    suspend fun sendEvent(event: MonitoringEvent): Boolean {
        val url = config.monitoringApiUrl.trimEnd('/') + "/monitoring/events"
        return withContext(Dispatchers.IO) {
            try {
                val body = monitoringEventToJson(event).toString()
                    .toRequestBody("application/json".toMediaType())
                val response = client.newCall(
                    Request.Builder().url(url).post(body).build(),
                ).execute()
                response.use { it.isSuccessful }
            } catch (_: Exception) {
                false
            }
        }
    }
}