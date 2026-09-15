package com.agroland.feature.payment.data

import com.agroland.core.network.ApiResult
import com.agroland.core.network.NetworkModule.safeCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Төлем репозиторісі (Flutter PaymentRepository): баланспен төлеу, Halyk ePay
 * init, BCC legacy 3D Secure формасы (баланс толтыру, Фаза 10) және төлем
 * нәтижесін поллинг.
 */
@Singleton
class PaymentRepository @Inject constructor(
    private val paymentApi: PaymentApi,
) {

    suspend fun payFromBalance(orderId: Long): ApiResult<BalancePaymentResult> = safeCall {
        PaymentParser.parseBalanceResult(paymentApi.payFromBalance(PaymentRequests.orderId(orderId)))
            ?: throw IllegalStateException("Пустой ответ")
    }

    suspend fun initHalykPayment(orderId: Long): ApiResult<HalykPaymentInit> = safeCall {
        PaymentParser.parseHalykInit(paymentApi.initHalyk(PaymentRequests.orderId(orderId)))
            ?: throw IllegalStateException("Пустой ответ")
    }

    /** Баланс толтыру HTML формасы (BCC 3D Secure) — WebView-та көрсетіледі. */
    suspend fun generateBalanceTopUp(amount: Double): ApiResult<String> = safeCall {
        PaymentParser.parseGenerateHtml(paymentApi.generatePayment(PaymentRequests.generate(amount)))
            ?: throw IllegalStateException("Пустой ответ")
    }

    /** Төлем нәтижесін тексеру — PaymentResultPage поллингі (2с/60с). */
    suspend fun getOrderPaymentState(orderId: Long): ApiResult<OrderPaymentState> = safeCall {
        PaymentParser.parseOrderPaymentState(paymentApi.getOrder(orderId))
            ?: throw IllegalStateException("Пустой ответ")
    }
}