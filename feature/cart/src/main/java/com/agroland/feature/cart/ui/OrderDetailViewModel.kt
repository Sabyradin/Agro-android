package com.agroland.feature.cart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.cart.data.CartRepository
import com.agroland.feature.cart.data.Order
import com.agroland.feature.cart.data.OrderStatus
import com.agroland.feature.cart.data.OrderTracking
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * OrderDetailViewModel — тапсырыс деталы: толық тапсырыс + трекинг (үнсіз
 * қателермен), болдырмау/қабылдау/қайта тапсырыс әрекеттері.
 */
@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val repository: CartRepository,
) : ViewModel() {

    private val _order = MutableStateFlow<Order?>(null)
    val order: StateFlow<Order?> = _order

    private val _tracking = MutableStateFlow<OrderTracking?>(null)
    val tracking: StateFlow<OrderTracking?> = _tracking

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<CartError?>(null)
    val error: StateFlow<CartError?> = _error

    private val _events = MutableSharedFlow<CartEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<CartEvent> = _events

    private val _actionLoading = MutableStateFlow(false)
    val actionLoading: StateFlow<Boolean> = _actionLoading

    private var currentId: Long? = null

    fun load(id: Long) {
        if (currentId == id && _order.value != null) return
        currentId = id
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.getOrder(id)) {
                is ApiResult.Success -> {
                    _order.value = result.value
                    loadTracking(id)
                }
                is ApiResult.Error -> _error.value = result.failure.toCartError()
            }
            _loading.value = false
        }
    }

    /** Трекинг қатесі ешқашан бетті бұзпайды (Flutter тәртібі). */
    private suspend fun loadTracking(id: Long) {
        when (val result = repository.getOrderTracking(id)) {
            is ApiResult.Success -> _tracking.value = result.value
            is ApiResult.Error -> Unit
        }
    }

    /** Тапсырысты болдырмау — supplier_query/paid күйлерінде ғана. */
    fun cancelOrder() {
        val id = currentId ?: return
        if (_actionLoading.value) return
        viewModelScope.launch {
            _actionLoading.value = true
            when (val result = repository.updateOrderStatus(id, OrderStatus.CANCELLED.value)) {
                is ApiResult.Success -> {
                    _order.value = result.value
                    _events.emit(CartEvent.OrderCancelled)
                }
                is ApiResult.Error -> _events.emit(CartEvent.ShowError(result.failure.toCartError()))
            }
            _actionLoading.value = false
        }
    }

    /** «Тауарды қабылдау» — аралық статустар тізбегімен. */
    fun confirmReceipt() {
        val order = _order.value ?: return
        if (_actionLoading.value) return
        viewModelScope.launch {
            _actionLoading.value = true
            when (val result = repository.confirmDelivery(order.id, order.status)) {
                is ApiResult.Success -> {
                    _order.value = result.value
                    _events.emit(CartEvent.ReceiptConfirmed)
                }
                is ApiResult.Error -> _events.emit(CartEvent.ShowError(result.failure.toCartError()))
            }
            _actionLoading.value = false
        }
    }

    /** Қайта тапсырыс — себетке қайта салады. */
    fun reorder() {
        val order = _order.value ?: return
        viewModelScope.launch {
            when (val result = repository.reorder(order.id)) {
                is ApiResult.Success -> _events.emit(CartEvent.ReorderDone)
                is ApiResult.Error -> _events.emit(CartEvent.ShowError(result.failure.toCartError()))
            }
        }
    }
}