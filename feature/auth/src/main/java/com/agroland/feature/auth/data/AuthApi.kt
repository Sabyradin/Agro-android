package com.agroland.feature.auth.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.JsonObject
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Auth API (spec: POST /auth/login, /auth/signup, GET-POST /auth/mfa/{task_id}).
 * Барлық жауап JsonObject — HATEOAS форма тұрақсыз, кешірімді парсинг репозиторийде.
 */
interface AuthApi {

    /**
     * Бірінші шақыру (task_id жоқ): body {phone_number} → {links:[mfaSmsRequest, mfaSmsSubmit], task_id, email?}.
     * Екінші шақыру (header task_id): токендер {access_token, refresh_token, biometric_token}.
     */
    @retrofit2.http.POST("auth/login")
    suspend fun login(
        @retrofit2.http.Body body: JsonObject,
        @retrofit2.http.Header("task_id") taskId: String? = null,
    ): JsonObject

    /** Signup: бірінші {phone_number, name, user_type, company_bin?, email?} → links; екінші (task_id header) — тіркеу. */
    @retrofit2.http.POST("auth/signup")
    suspend fun signup(
        @retrofit2.http.Body body: JsonObject,
        @retrofit2.http.Header("task_id") taskId: String? = null,
    ): JsonObject

    /** MFA кодты генерациялап SMS жібереді (жеңілдікке жатқан номерлерге SMS жіберілмейді). */
    @retrofit2.http.GET("auth/mfa/{task_id}")
    suspend fun requestSms(@retrofit2.http.Path("task_id") taskId: String): JsonObject

    /** SMS кодты тексереді. */
    @retrofit2.http.POST("auth/mfa/{task_id}")
    suspend fun submitSmsCode(
        @retrofit2.http.Path("task_id") taskId: String,
        @retrofit2.http.Body body: JsonObject,
    ): JsonObject

    /** Кэштегі кодты email арқылы жібереді. */
    @retrofit2.http.POST("auth/mfa/{task_id}/email")
    suspend fun sendCodeByEmail(@retrofit2.http.Path("task_id") taskId: String): JsonObject

    /**
     * Биометриямен кіру: body {biometric_token} → жаңа токендер.
     * BACKEND БҰЛ МАРШРУТТЫ ТІРКЕМЕГЕН (404 мүмкін) — best-effort (ISSUES.md #2).
     */
    @retrofit2.http.POST("auth/biometric")
    suspend fun biometricLogin(@retrofit2.http.Body body: JsonObject): JsonObject
}

@Module
@InstallIn(SingletonComponent::class)
object AuthApiModule {

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
}