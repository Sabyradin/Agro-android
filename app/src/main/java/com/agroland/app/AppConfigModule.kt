package com.agroland.app

import com.agroland.core.analytics.AnalyticsConfig
import com.agroland.core.network.NetworkConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Flavor-ға тәуелді сеть конфигурациясы.
 * BASE_API_URL — BuildConfig өрісі: dev/prod flavor-да анықталған.
 * /api/v1 префиксі міндетті түрде осы жерде ғана (spec: "URL completion in one resolver").
 */
@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig = object : NetworkConfig {
        override val baseUrl: String = BuildConfig.BASE_API_URL
        override val isDebug: Boolean = BuildConfig.DEBUG && !BuildConfig.IS_PRODUCTION
    }

    /** Фаза 19: TikTok/monitoring қалтасы (Flutter app_config.json parity). */
    @Provides
    @Singleton
    fun provideAnalyticsConfig(): AnalyticsConfig = object : AnalyticsConfig {
        override val monitoringApiUrl: String = BuildConfig.MONITORING_API_URL
        override val tiktokAndroidAppId: String = BuildConfig.TIKTOK_APP_ID
        override val tiktokAndroidTtAppId: String = BuildConfig.TIKTOK_TT_APP_ID
        override val tiktokAndroidAccessToken: String = BuildConfig.TIKTOK_ACCESS_TOKEN
    }
}