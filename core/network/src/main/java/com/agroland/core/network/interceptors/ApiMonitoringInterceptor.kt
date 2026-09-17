package com.agroland.core.network.interceptors

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/**
 * API сұраныстарын мониторингтеу интерцепторы — Flutter
 * monitoring_interceptor.dart 1:1 (Фаза 19):
 *  • әр сәтті жауап → api_request_success (endpoint, method, statusCode)
 *  • әр қате (не-2xx немесе IOException) → api_request_failure
 *  • /monitoring/events өзін-өзі бақыламайды (циклді болдырмау)
 * Endpoint — URL path бөлігі ғана (Flutter _extractEndpoint: baseUrl-сіз path).
 */
@Singleton
class ApiMonitoringInterceptor @Inject constructor(
    private val reporter: ApiEventReporter,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        if (url.contains("/monitoring/events")) {
            return chain.proceed(request)
        }
        val endpoint = request.url.encodedPath
        val method = request.method
        return try {
            val response = chain.proceed(request)
            if (response.isSuccessful) {
                reporter.apiRequestSuccess(endpoint, method, response.code)
            } else {
                reporter.apiRequestFailure(endpoint, method, response.code, response.message)
            }
            response
        } catch (e: java.io.IOException) {
            reporter.apiRequestFailure(endpoint, method, null, e.message)
            throw e
        }
    }
}