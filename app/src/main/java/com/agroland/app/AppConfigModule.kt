package com.agroland.app

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
}