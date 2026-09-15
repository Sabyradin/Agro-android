package com.agroland.feature.wallet.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import javax.inject.Singleton

/**
 * Әмиян API (Flutter BalanceRepository; /dealer/... атауларының /business/...
 * алиастары — SPEC «Wallet»: target /business/... напрямую).
 */
interface WalletApi {

    /** GET /business/balance — қолжетімді + күтуде + ұсталған. */
    @GET("business/balance")
    suspend fun getBalance(): JsonObject

    /** GET /business/balance/transactions — ledger (limit=50, type сүзгісі опционалды). */
    @GET("business/balance/transactions")
    suspend fun getTransactions(
        @Query("type") type: String? = null,
        @Query("limit") limit: Int = 50,
    ): kotlinx.serialization.json.JsonElement

    /** POST /business/withdraw — шығару сұрауы {amount, bank_details}. */
    @POST("business/withdraw")
    suspend fun withdraw(@Body body: JsonObject): JsonObject
}

/** Дене құрастырғыштары — парсер тесттерімен бірге тексеріледі. */
object WalletRequests {

    /**
     * POST /business/withdraw денесі: {amount, bank_details:{iban, bank_name,
     * bik?, account?}}. IBAN бос орындардан тазартылып, бас әріпке келеді;
     * бос bik/account жіберілмейді (Flutter withdraw_page _submit).
     */
    fun withdraw(
        amount: Double,
        iban: String,
        bankName: String,
        bik: String?,
        account: String?,
    ): JsonObject = buildJsonObject {
        put("amount", amount)
        put(
            "bank_details",
            buildJsonObject {
                put("iban", iban)
                put("bank_name", bankName)
                bik?.takeIf { it.isNotBlank() }?.let { put("bik", it) }
                account?.takeIf { it.isNotBlank() }?.let { put("account", it) }
            },
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object WalletApiModule {

    @Provides
    @Singleton
    fun provideWalletApi(retrofit: Retrofit): WalletApi = retrofit.create(WalletApi::class.java)
}