package com.agroland.feature.notifications.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.notifications.data.NotificationItem
import com.agroland.feature.notifications.data.NotificationType
import com.agroland.feature.notifications.data.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Бөлім хабарламалары тізімі (Flutter notificationsNotifier):
 * GET /notifications/{type}?page=1&limit=20 → карточкалар.
 */
@HiltViewModel
class NotificationsByTypeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NotificationsRepository,
) : ViewModel() {

    val type: NotificationType = NotificationType.fromString(
        savedStateHandle["type"] ?: "service",
    )

    data class UiState(
        val loading: Boolean = true,
        val error: Boolean = false,
        val items: List<NotificationItem> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = false) }
        viewModelScope.launch {
            when (val result = repository.getByType(type)) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, error = false, items = result.value)
                }
                is ApiResult.Error -> _state.update {
                    it.copy(loading = false, error = true)
                }
            }
        }
    }
}