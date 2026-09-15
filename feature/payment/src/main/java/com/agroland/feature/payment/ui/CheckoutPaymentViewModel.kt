package com.agroland.feature.payment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.payment.data.PaymentRepository
import com.agroland.feature.payment.data.PendingPaymentStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * CheckoutPaymentViewModel — Flutter CheckoutPaymentNotifier: баланспен төлеу
 * және Halyk init. Halyk жолында WebView ашылмай тұрып pending id сақталады —
 * қосымша суық старт болса нәтиже беті соны қалпына келтіреді.
 */
@HiltViewModel
class CheckoutPaymentViewModel @Inject constructor(
    private val repository: PaymentRepository,
    private val pendingPaymentStore: PendingPaymentStore,
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _events = MutableSharedFlow<PaymentEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<PaymentEvent> = _events

    fun payFromBalance(orderId: Long) {
        if (_loading.value) return
        viewModelScope.launch {
            _loading.value = true
            when (val result = repository.payFromBalance(orderId)) {
                is ApiResult.Success -> _events.emit(PaymentEvent.BalancePaid(result.value))
                is ApiResult.Error -> _events.emit(PaymentEvent.ShowError(result.failure.toPaymentError()))
            }
            _loading.value = false
        }
    }

    fun payWithHalyk(orderId: Long) {
        if (_loading.value) return
        viewModelScope.launch {
            _loading.value = true
            when (val result = repository.initHalykPayment(orderId)) {
                is ApiResult.Success -> {
                    pendingPaymentStore.savePendingOrderId(orderId)
                    _events.emit(PaymentEvent.HalykReady(result.value, orderId))
                }
                is ApiResult.Error -> _events.emit(PaymentEvent.ShowError(result.failure.toPaymentError()))
            }
            _loading.value = false
        }
    }

    /** Парақ қайта ашылғанда күй тазаланады (Flutter initState reset). */
    fun reset() {
        _loading.value = false
    }
}