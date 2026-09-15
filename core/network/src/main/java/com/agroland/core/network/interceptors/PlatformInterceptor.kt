package com.agroland.core.network.interceptors

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Төлем сұрауларына `X-Platform: android` тақырыбы қосылады (spec §5; iOS-та ios).
 * Backend төлем провайдеріне платформа туралы хабарды осылай береді.
 */
class PlatformInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val updated = if (request.url.encodedPath.contains(PATH_MARKER)) {
            request.newBuilder()
                .header(HEADER_PLATFORM, PLATFORM_ANDROID)
                .build()
        } else {
            request
        }
        return chain.proceed(updated)
    }

    private companion object {
        const val PATH_MARKER = "payment"
        const val HEADER_PLATFORM = "X-Platform"
        const val PLATFORM_ANDROID = "android"
    }
}