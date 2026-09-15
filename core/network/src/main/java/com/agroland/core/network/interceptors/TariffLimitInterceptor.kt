package com.agroland.core.network.interceptors

import com.agroland.core.network.error.ApiErrorParser
import com.agroland.core.network.error.TariffLimitException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Тариф шегі интерцепторы (spec §4): 403 + error_code TARIFF_LIMIT_*
 * → UI-де «Тариф жаңарту» диалогын көрсететін типтелген ерекшелік.
 */
@Singleton
class TariffLimitInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 403) {
            val body = response.peekBody(64 * 1024)
            val error = ApiErrorParser.parse(403, body.string())
            if (error.code?.startsWith("TARIFF_LIMIT_") == true) {
                response.close()
                throw TariffLimitException(error)
            }
        }
        return response
    }
}