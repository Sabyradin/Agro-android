package com.agroland.feature.notifications.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.notifications.data.NotificationCounter
import com.agroland.feature.notifications.data.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Хабарламалар хабы ViewModel (Flutter notificationCounterNotifier):
 * үш бөлімнің оқылмаған санағын жүктейді, ортасынан жаңартады.
 */
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationsRepository,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val error: Boolean = false,
        val counter: NotificationCounter? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = false) }
        viewModelScope.launch {
            when (val result = repository.getCounter()) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, error = false, counter = result.value)
                }
                is ApiResult.Error -> _state.update {
                    it.copy(loading = false, error = true)
                }
            }
        }
    }
}