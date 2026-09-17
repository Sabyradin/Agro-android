package com.agroland.core.analytics

import com.agroland.core.network.interceptors.ApiEventReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Фаза 19: core:network-тің OkHttp тізбегі мониторинг оқиғаларын осы
 * байлым арқылы алады — градл тәуелділігі кері бағытта болғандықтан
 * (analytics → network) интерфейс арқылы қосылады.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindApiEventReporter(impl: MonitoringService): ApiEventReporter
}