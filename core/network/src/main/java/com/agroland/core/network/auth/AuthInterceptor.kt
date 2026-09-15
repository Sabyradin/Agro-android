package com.agroland.core.network.auth

import com.agroland.core.network.NetworkConfig
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Single-flight refresh (spec §4): қатар келген 401-дер БІР refresh-ке жинақталады.
 * Refresh token single-use → параллель refresh сұраныстары шын мәнінде біреу болуы керек.
 *
 * Механизм: Mutex + «token өзгерді ме» тексерірісі. Бірінші жіберуші refresh жасайды;
 * кейінгілер күтіп, token-ның бұрын-соңды жаңарғанын көріп, қайта сұранысты жібереді.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    private val config: NetworkConfig,
) : Interceptor {

    private val refreshMutex = Mutex()

    /** Unauthorized/logout сигналі — SessionController (фаза 3) тыңдайды. */
    private val unauthorizedListeners = mutableSetOf<() -> Unit>()

    @Synchronized
    fun onUnauthorized(listener: () -> Unit) {
        unauthorizedListeners.add(listener)
    }

    private fun notifyUnauthorized() {
        val snapshot = synchronized(unauthorizedListeners) { unauthorizedListeners.toList() }
        snapshot.forEach { it() }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.accessToken.value
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        val response = chain.proceed(request)
        if (response.code != 401) return response

        // /auth/refresh өзі 401 берсе — refresh жарамсыз, цикл жоқ.
        if (request.url.encodedPath.contains("/auth/")) {
            response.close()
            tokenStore.clear()
            notifyUnauthorized()
            return response
        }

        val refreshed = runBlocking { refreshIfNeeded(failedToken = token) }
        response.close()
        if (!refreshed) return chain.proceed(request) // guest ретінде қайта жібереміз

        val newToken = tokenStore.accessToken.value
        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build(),
        )
    }

    /** Token бұрын жаңармаған болса ғана refresh жасайды (single-flight). */
    private suspend fun refreshIfNeeded(failedToken: String?): Boolean =
        refreshMutex.withLock {
            val current = tokenStore.accessToken.value
            // Басқа жіберуші аралықта жаңартқан — refresh қажет емес.
            if (failedToken == null || (current != null && current != failedToken)) return@withLock current != null

            val ok = doRefresh()
            if (!ok) {
                tokenStore.clear()
                notifyUnauthorized()
            }
            ok
        }

    /** POST {base}/auth/refresh — JSON {"refresh_token": ...}. Single-use: жаңасы бірден сақталады. */
    private suspend fun doRefresh(): Boolean {
        val refresh = tokenStore.refreshToken.value ?: return false
        val body = """{"refresh_token":"$refresh"}"""
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(config.baseUrl.trimEnd('/') + "/auth/refresh")
            .post(body)
            .build()
        return try {
            bareClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val raw = resp.body?.string() ?: return false
                val obj = Json.parseToJsonElement(raw).jsonObject
                val access = obj["access_token"]?.jsonPrimitive?.content
                val newRefresh = obj["refresh_token"]?.jsonPrimitive?.content
                val newBiometric = obj["biometric_token"]?.jsonPrimitive?.content
                if (access.isNullOrBlank()) return false
                tokenStore.saveRefreshedTokens(access, newRefresh ?: refresh, newBiometric)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /** Refresh өзіне Auth/Retry қоспау үшін жалаң клиент. */
    private val bareClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(20))
            .readTimeout(Duration.ofSeconds(20))
            .build()
    }
}