package com.agroland.core.network.interceptors

import com.agroland.core.network.NetworkConfig
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/** Логтау интерцепторы — MonitoringInterceptor (spec §4). */
@Singleton
class MonitoringInterceptor @Inject constructor(
    private val config: NetworkConfig,
) : Interceptor {

    private val logger = okhttp3.logging.HttpLoggingInterceptor().apply {
        level = if (config.isDebug) {
            okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        } else {
            okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response = logger.intercept(chain)
}