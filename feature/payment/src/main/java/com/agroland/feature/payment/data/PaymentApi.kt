package com.agroland.feature.payment.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import retrofit2.Retrofit

/**
 * Төлем API (Flutter PaymentRepository):
 * /payments/from-balance, /payments/halyk/init, /payment/generate (BCC legacy
 * 3D Secure HTML формасы) + төлем нәтижесін тексеретін GET /orders/{id}.
 * Барлық сұрауға PlatformInterceptor X-Platform: android қосады.
 */
interface PaymentApi {

    /** Баланспен төлеу — {order_id} → {status, method, order_id, amount, buyer_balance_after}. */
    @retrofit2.http.POST("payments/from-balance")
    suspend fun payFromBalance(@retrofit2.http.Body body: JsonObject): JsonObject

    /** Halyk ePay инициализациясы — {order_id} → {payment_url, invoice_id, amount, currency, mock}. */
    @retrofit2.http.POST("payments/halyk/init")
    suspend fun initHalyk(@retrofit2.http.Body body: JsonObject): JsonObject

    /**
     * Баланс толтыру (BCC legacy 3D Secure, Фаза 10 әмиян) — {amount, currency} →
     * HTML форма (P_SIGN backend жағында құрылады).
     */
    @retrofit2.http.POST("payment/generate")
    suspend fun generatePayment(@retrofit2.http.Body body: JsonObject): JsonElement

    /** Төлем нәтижесін поллинг — payment_status/status өрістері оқылады. */
    @retrofit2.http.GET("orders/{id}")
    suspend fun getOrder(@retrofit2.http.Path("id") id: Long): JsonObject
}

/** Дене құрушылар — тек толтырылған өрістер жіберіледі. */
object PaymentRequests {

    fun orderId(orderId: Long): JsonObject = buildJsonObject {
        put("order_id", orderId)
    }

    fun generate(amount: Double, currency: String = "KZT"): JsonObject = buildJsonObject {
        put("amount", amount)
        put("currency", currency)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object PaymentApiModule {

    @Provides
    @Singleton
    fun providePaymentApi(retrofit: Retrofit): PaymentApi = retrofit.create(PaymentApi::class.java)
}