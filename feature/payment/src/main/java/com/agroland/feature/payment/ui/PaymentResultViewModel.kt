package com.agroland.feature.payment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.analytics.MonitoringService
import com.agroland.core.network.ApiResult
import com.agroland.feature.payment.data.PaymentRepository
import com.agroland.feature.payment.data.PendingPaymentStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** PaymentResultPage күйлері: тексеріледі / сәтті / қате / 60с уақыт өтті. */
sealed interface PaymentResultPhase {
    data object Verifying : PaymentResultPhase
    data object Success : PaymentResultPhase
    data class Failed(val error: PaymentError) : PaymentResultPhase
    data object TimedOut : PaymentResultPhase
}

/**
 * PaymentResultViewModel — Flutter payment_result_page поллингі:
 * GET /orders/{id} әр 2 секундте, 60 секундтан кейін «өңделуде» күйі.
 * Расталғанда/уақыт өткенде pending id тазаланады.
 */
@HiltViewModel
class PaymentResultViewModel @Inject constructor(
    private val repository: PaymentRepository,
    private val pendingPaymentStore: PendingPaymentStore,
    private val monitoringService: MonitoringService,
) : ViewModel() {

    private val _phase = MutableStateFlow<PaymentResultPhase>(PaymentResultPhase.Verifying)
    val phase: StateFlow<PaymentResultPhase> = _phase.asStateFlow()

    private var pollJob: Job? = null

    fun start(orderId: Long) {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            _phase.value = PaymentResultPhase.Verifying
            var elapsed = 0
            while (isActive) {
                elapsed += POLL_INTERVAL_SECONDS
                when (val result = repository.getOrderPaymentState(orderId)) {
                    is ApiResult.Error -> {
                        pendingPaymentStore.clearPendingOrderId()
                        _phase.value = PaymentResultPhase.Failed(result.failure.toPaymentError())
                        return@launch
                    }
                    is ApiResult.Success -> {
                        if (result.value.isPaid) {
                            // Фаза 19 (Flutter payment_result_page parity): TikTok
                            // `purchase` conversion — төлем расталған сәт.
                            monitoringService.trackPurchase(
                                orderId = result.value.orderId.toString(),
                                amount = result.value.totalAmount,
                                currency = result.value.currency,
                            )
                            pendingPaymentStore.clearPendingOrderId()
                            _phase.value = PaymentResultPhase.Success
                            return@launch
                        }
                        if (elapsed >= TIMEOUT_SECONDS) {
                            pendingPaymentStore.clearPendingOrderId()
                            _phase.value = PaymentResultPhase.TimedOut
                            return@launch
                        }
                    }
                }
                delay(POLL_INTERVAL_SECONDS * 1000L)
            }
        }
    }

    fun retry(orderId: Long) {
        pollJob?.cancel()
        start(orderId)
    }

    override fun onCleared() {
        pollJob?.cancel()
    }

    companion object {
        const val POLL_INTERVAL_SECONDS = 2
        const val TIMEOUT_SECONDS = 60
    }
}