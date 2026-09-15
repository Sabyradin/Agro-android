package com.agroland.feature.wallet.data

import com.agroland.core.network.ApiResult
import com.agroland.core.network.NetworkModule.safeCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Әмиян репозиторісі (Flutter BalanceRepository): баланс, транзакциялар
 * ledger-і және қаражат шығару сұрауы. Шығару кезінде баланс admin
 * мақұлдағанда ғана кемиді — клиент тек сұраныс жібереді.
 */
@Singleton
class WalletRepository @Inject constructor(
    private val walletApi: WalletApi,
) {

    suspend fun getBalance(): ApiResult<WalletBalance> = safeCall {
        WalletParser.parseBalance(walletApi.getBalance())
            ?: throw IllegalStateException("Пустой ответ")
    }

    suspend fun getTransactions(type: String? = null): ApiResult<List<WalletTransaction>> = safeCall {
        WalletParser.parseTransactions(walletApi.getTransactions(type))
    }

    suspend fun requestWithdraw(
        amount: Double,
        iban: String,
        bankName: String,
        bik: String?,
        account: String?,
    ): ApiResult<Unit> = safeCall {
        walletApi.withdraw(WalletRequests.withdraw(amount, iban, bankName, bik, account))
        Unit
    }
}