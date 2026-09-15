package com.agroland.feature.wallet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.wallet.data.WalletBalance
import com.agroland.feature.wallet.data.WalletRepository
import com.agroland.feature.wallet.data.WalletTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Баланс + транзакциялар күйі (Flutter BalanceNotifier +
 * balanceTransactionsProvider бірге): BalancePage және TransactionHistoryPage
 * осы ViewModel-ді пайдаланады.
 */
@HiltViewModel
class BalanceViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
) : ViewModel() {

    private val _balance = MutableStateFlow<WalletBalance?>(null)
    val balance: StateFlow<WalletBalance?> = _balance.asStateFlow()

    private val _transactions = MutableStateFlow<List<WalletTransaction>?>(null)
    val transactions: StateFlow<List<WalletTransaction>?> = _transactions.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<WalletError?>(null)
    val error: StateFlow<WalletError?> = _error.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (_loading.value) return
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            when (val result = walletRepository.getBalance()) {
                is ApiResult.Success -> _balance.value = result.value
                is ApiResult.Error -> _error.value = result.failure.toWalletError()
            }
            when (val result = walletRepository.getTransactions()) {
                is ApiResult.Success -> _transactions.value = result.value
                is ApiResult.Error -> if (_error.value == null) {
                    _error.value = result.failure.toWalletError()
                }
            }
            _loading.value = false
        }
    }

    fun refresh() = load()
}