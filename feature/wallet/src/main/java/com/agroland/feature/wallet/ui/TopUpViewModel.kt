package com.agroland.feature.wallet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.analytics.MonitoringService
import com.agroland.core.network.ApiResult
import com.agroland.feature.payment.data.PaymentRepository
import com.agroland.feature.profile.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Әмиянды толтыру күйі (Flutter PaymentNotifier + userNotifier): ағымдағы
 * балансты алып, POST /payment/generate HTML формасын дайындайды.
 */
@HiltViewModel
class TopUpViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val profileRepository: ProfileRepository,
    private val monitoringService: MonitoringService,
) : ViewModel() {

    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _events = MutableSharedFlow<WalletEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<WalletEvent> = _events.asSharedFlow()

    /** Профильден балансты жаңартады (WebView-тан оралғанда да шақырылады). */
    fun loadBalance() {
        viewModelScope.launch {
            when (val result = profileRepository.getProfile()) {
                is ApiResult.Success -> _balance.value = result.value.balance
                is ApiResult.Error -> Unit // Баланс көрсетімі үшін үнсіз қалдырылады.
            }
        }
    }

    fun makePayment(amount: Double) {
        if (_loading.value) return
        _loading.value = true
        viewModelScope.launch {
            when (val result = paymentRepository.generateBalanceTopUp(amount)) {
                is ApiResult.Error -> {
                    // Фаза 19 (Flutter PaymentNotifier.makePayment parity).
                    monitoringService.trackBalanceTopUp(
                        amount = amount,
                        currency = "KZT",
                        success = false,
                        errorMessage = result.failure.toString(),
                    )
                    _events.emit(WalletEvent.ShowError(result.failure.toWalletError()))
                }
                is ApiResult.Success -> {
                    monitoringService.trackBalanceTopUp(
                        amount = amount,
                        currency = "KZT",
                        success = true,
                    )
                    _events.emit(WalletEvent.TopUpHtmlReady(result.value))
                }
            }
            _loading.value = false
        }
    }
}