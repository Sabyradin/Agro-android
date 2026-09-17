package com.agroland.app.appversion

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject
import retrofit2.Retrofit

/**
 * App version API (Фаза 20) — Flutter app_version_remote_service.dart 1:1.
 * GET /app-version?platform=android&current_version=X — public эндпоинт
 * (auth interceptor токен қоспайды, 401 мәнісіз).
 */
interface AppVersionApi {

    @retrofit2.http.GET("app-version")
    suspend fun getAppVersion(
        @retrofit2.http.Query("platform") platform: String,
        @retrofit2.http.Query("current_version") currentVersion: String,
    ): JsonObject
}

@Module
@InstallIn(SingletonComponent::class)
object AppVersionApiModule {

    @Provides
    @Singleton
    fun provideAppVersionApi(retrofit: Retrofit): AppVersionApi =
        retrofit.create(AppVersionApi::class.java)
}