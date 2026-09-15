package com.agroland.core.network.interceptors

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Retry-интерцептор (spec §4): тек GET, максимум 2 қайта әрекет,
 * 800мс × 2^(n-1) экспоненциалды кідіріспен (800мс, 1600мс).
 * POST/PUT/PATCH/DELETE ешқашан қайта жіберілмейді (idempotent емес).
 */
@Singleton
class RetryInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET") {
            return chain.proceed(request)
        }

        var lastError: IOException? = null
        var attempt = 0
        var response: Response? = null
        while (attempt <= MAX_RETRIES) {
            try {
                response?.close()
                response = chain.proceed(request)
                // 5xx — қайта әрекет; 4xx — клиент қатесі, қайталау мағынасыз.
                if (response.code < 500 || attempt == MAX_RETRIES) {
                    return response
                }
            } catch (e: IOException) {
                lastError = e
                response = null
            }
            attempt++
            if (attempt <= MAX_RETRIES) {
                Thread.sleep(BASE_DELAY_MS * (1L shl (attempt - 1)))
            }
        }
        return response ?: throw (lastError ?: IOException("Request failed"))
    }

    private companion object {
        const val MAX_RETRIES = 2
        const val BASE_DELAY_MS = 800L
    }
}