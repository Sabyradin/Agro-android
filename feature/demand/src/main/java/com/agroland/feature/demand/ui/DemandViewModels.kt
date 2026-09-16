package com.agroland.feature.demand.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.core.network.error.Failure
import com.agroland.feature.demand.data.DemandDraft
import com.agroland.feature.demand.data.DemandItem
import com.agroland.feature.demand.data.DemandRepository
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
 * UI-ға берiлетін қате: backend адам тіліндегі message > generic
 * (спек §4 — шикі error_code ешқашан көрсетілмейді).
 */
data class DemandError(
    val backendMessage: String? = null,
    val isNetwork: Boolean = false,
)

fun DemandError.displayText(networkMessage: String, genericMessage: String): String =
    backendMessage ?: if (isNetwork) networkMessage else genericMessage

fun Failure.toDemandError(): DemandError = when (this) {
    is Failure.Network -> DemandError(isNetwork = true)
    else -> DemandError(backendMessage = (this as? Failure.Http)?.error?.message)
}

/** Сұраныс экрандарының бір реттелген оқиғалары. */
sealed interface DemandEvent {
    data class ShowError(val error: DemandError) : DemandEvent
    /** Жаңа сұраныс жарияланды. */
    data object Created : DemandEvent
    data object Updated : DemandEvent
    data object Deleted : DemandEvent
    data object Activated : DemandEvent
    data object Deactivated : DemandEvent
}

/** Сұраныстар парақталынған тізімі (DemandListPage). */
@HiltViewModel
class DemandListViewModel @Inject constructor(
    private val repository: DemandRepository,
) : ViewModel() {

    private val _items = MutableStateFlow<List<DemandItem>>(emptyList())
    val items = _items.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore = _loadingMore.asStateFlow()

    private val _exhausted = MutableStateFlow(false)
    val exhausted = _exhausted.asStateFlow()

    private val _error = MutableStateFlow<Failure?>(null)
    val error = _error.asStateFlow()

    private val _events = MutableSharedFlow<DemandEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<DemandEvent> = _events.asSharedFlow()

    private var page = 1

    init {
        loadFirst()
    }

    fun loadFirst() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            page = 1
            when (val result = repository.getDemands(page = 1)) {
                is ApiResult.Success -> {
                    _items.value = result.value.items
                    _exhausted.value = !result.value.canLoadMore
                    page = 2
                }
                is ApiResult.Error -> _error.value = result.failure
            }
            _loading.value = false
        }
    }

    fun loadMore() {
        if (_loadingMore.value || _exhausted.value || _items.value.isEmpty()) return
        viewModelScope.launch {
            _loadingMore.value = true
            when (val result = repository.getDemands(page = page)) {
                is ApiResult.Success -> {
                    val existing = _items.value.map { it.id }.toSet()
                    val fresh = result.value.items.filter { it.id !in existing }
                    if (fresh.isEmpty()) {
                        _exhausted.value = true
                    } else {
                        _items.value = _items.value + fresh
                        page++
                        _exhausted.value = !result.value.canLoadMore
                    }
                }
                is ApiResult.Error ->
                    _events.tryEmit(DemandEvent.ShowError(result.failure.toDemandError()))
            }
            _loadingMore.value = false
        }
    }

    /** Тізімнен жою (detail беті де қолданады) — оптимистті жаңарту. */
    fun removeLocally(id: Long) {
        _items.value = _items.value.filter { it.id != id }
    }
}

/** Жеке сұраныс (DemandDetailPage) — деталь + әрекеттер. */
@HiltViewModel
class DemandDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DemandRepository,
) : ViewModel() {

    val demandId: Long = savedStateHandle.get<Long>("demandId") ?: -1L

    private val _demand = MutableStateFlow<DemandItem?>(null)
    val demand = _demand.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<Failure?>(null)
    val error = _error.asStateFlow()

    private val _actionLoading = MutableStateFlow(false)
    val actionLoading = _actionLoading.asStateFlow()

    private val _events = MutableSharedFlow<DemandEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<DemandEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.getDemand(demandId)) {
                is ApiResult.Success -> _demand.value = result.value
                is ApiResult.Error -> _error.value = result.failure
            }
            _loading.value = false
        }
    }

    fun delete() {
        if (_actionLoading.value) return
        viewModelScope.launch {
            _actionLoading.value = true
            when (val result = repository.deleteDemand(demandId)) {
                is ApiResult.Success -> _events.tryEmit(DemandEvent.Deleted)
                is ApiResult.Error ->
                    _events.tryEmit(DemandEvent.ShowError(result.failure.toDemandError()))
            }
            _actionLoading.value = false
        }
    }

    fun activate() {
        if (_actionLoading.value) return
        viewModelScope.launch {
            _actionLoading.value = true
            when (val result = repository.activateDemand(demandId)) {
                is ApiResult.Success -> {
                    _demand.value = _demand.value?.copy(status = "active")
                    _events.tryEmit(DemandEvent.Activated)
                }
                is ApiResult.Error ->
                    _events.tryEmit(DemandEvent.ShowError(result.failure.toDemandError()))
            }
            _actionLoading.value = false
        }
    }

    fun deactivate() {
        if (_actionLoading.value) return
        viewModelScope.launch {
            _actionLoading.value = true
            when (val result = repository.deactivateDemand(demandId)) {
                is ApiResult.Success -> {
                    _demand.value = _demand.value?.copy(status = "inactive")
                    _events.tryEmit(DemandEvent.Deactivated)
                }
                is ApiResult.Error ->
                    _events.tryEmit(DemandEvent.ShowError(result.failure.toDemandError()))
            }
            _actionLoading.value = false
        }
    }
}

/** Сұраныс формасының күйі (CreateEditDemandPage). */
data class DemandFormState(
    val demandId: Long = -1,
    val title: String = "",
    val description: String = "",
    val maxPrice: String = "",
    val currency: String = "KZT",
    val measurementUnit: String = "",
    val quantity: String = "",
    val categoryId: Int? = null,
    val subcategoryId: Int? = null,
    val categoryLabel: String? = null,
    val subcategoryLabel: String? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val loaded: Boolean = false,
)

/** Жаңа/өзгерту сұраныс формасы (CreateEditDemandRoute, demandId = -1 → жаңа). */
@HiltViewModel
class CreateEditDemandViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DemandRepository,
) : ViewModel() {

    val demandId: Long = savedStateHandle.get<Long>("demandId") ?: -1L

    private val _state = MutableStateFlow(DemandFormState(demandId = demandId))
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<DemandEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<DemandEvent> = _events.asSharedFlow()

    init {
        if (demandId > 0) loadExisting()
    }

    private fun loadExisting() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            when (val result = repository.getDemand(demandId)) {
                is ApiResult.Success -> {
                    val demand = result.value
                    _state.value = _state.value.copy(
                        title = demand?.title ?: "",
                        description = demand?.description ?: "",
                        maxPrice = demand?.maxPrice?.let { trimZero(it) } ?: "",
                        currency = demand?.currency ?: "KZT",
                        measurementUnit = demand?.measurementUnit ?: "",
                        quantity = demand?.quantity?.let { trimZero(it) } ?: "",
                        categoryId = demand?.categoryId,
                        subcategoryId = demand?.subcategoryId,
                        categoryLabel = demand?.categoryName,
                        subcategoryLabel = demand?.subcategoryName,
                        loading = false,
                        loaded = true,
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(loading = false, loaded = true)
                    _events.tryEmit(DemandEvent.ShowError(result.failure.toDemandError()))
                }
            }
        }
    }

    private fun trimZero(value: Double): String =
        if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }

    fun update(transform: (DemandFormState) -> DemandFormState) {
        _state.value = transform(_state.value)
    }

    /** Сақтау — клиент валидациясы (тақырып міндетті), сосын create не update. */
    fun save() {
        val current = _state.value
        if (current.saving) return
        if (current.title.isBlank()) return
        viewModelScope.launch {
            _state.value = current.copy(saving = true)
            val draft = DemandDraft(
                title = current.title.trim(),
                description = current.description.trim().takeIf { it.isNotEmpty() },
                currency = current.currency.trim().takeIf { it.isNotEmpty() },
                categoryId = current.categoryId,
                subcategoryId = current.subcategoryId,
                maxPrice = current.maxPrice.toDoubleOrNull(),
                measurementUnit = current.measurementUnit.trim().takeIf { it.isNotEmpty() },
                quantity = current.quantity.toDoubleOrNull(),
            )
            val result = if (demandId > 0) {
                repository.updateDemand(demandId, draft)
            } else {
                repository.createDemand(draft)
            }
            when (result) {
                is ApiResult.Success ->
                    _events.tryEmit(
                        if (demandId > 0) DemandEvent.Updated else DemandEvent.Created,
                    )
                is ApiResult.Error ->
                    _events.tryEmit(DemandEvent.ShowError(result.failure.toDemandError()))
            }
            _state.value = _state.value.copy(saving = false)
        }
    }
}