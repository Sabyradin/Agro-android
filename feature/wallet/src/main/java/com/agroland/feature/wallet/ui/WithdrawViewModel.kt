package com.agroland.feature.wallet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.wallet.data.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Шығару сұрауы күйі (Flutter WithdrawNotifier). */
@HiltViewModel
class WithdrawViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _events = MutableSharedFlow<WalletEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<WalletEvent> = _events.asSharedFlow()

    fun request(
        amount: Double,
        iban: String,
        bankName: String,
        bik: String?,
        account: String?,
    ) {
        if (_loading.value) return
        _loading.value = true
        viewModelScope.launch {
            when (
                val result = walletRepository.requestWithdraw(amount, iban, bankName, bik, account)
            ) {
                is ApiResult.Error -> _events.emit(WalletEvent.ShowError(result.failure.toWalletError()))
                is ApiResult.Success -> _events.emit(WalletEvent.WithdrawDone)
            }
            _loading.value = false
        }
    }
}