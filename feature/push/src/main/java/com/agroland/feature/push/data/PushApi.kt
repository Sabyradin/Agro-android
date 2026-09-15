package com.agroland.feature.push.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Құрылғы тіркеу API (Flutter PushNotificationsRepository):
 * POST /device — FCM token + тіл; DELETE /device/{device_id} — шыққанда.
 */
interface PushApi {

    /** POST /device — push жіберу үшін құрылғыны тіркеу. */
    @POST("device")
    suspend fun registerDevice(@Body body: JsonObject): JsonElement

    /** DELETE /device/{device_id} — тіркеуді өшіру (шығу кезінде). */
    @DELETE("device/{deviceId}")
    suspend fun unregisterDevice(@Path("deviceId") deviceId: String): JsonElement
}

@Module
@InstallIn(SingletonComponent::class)
object PushApiModule {

    @Provides
    @Singleton
    fun providePushApi(retrofit: Retrofit): PushApi = retrofit.create(PushApi::class.java)
}