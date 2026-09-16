package com.agroland.feature.dealer.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.dealer.data.AnalyticsPoint
import com.agroland.feature.dealer.data.DealerAnalytics
import com.agroland.feature.dealer.data.DealerEmployee
import com.agroland.feature.dealer.data.DealerError
import com.agroland.feature.dealer.data.DealerOrder
import com.agroland.feature.dealer.data.DealerProduct
import com.agroland.feature.dealer.data.DealerRepository
import com.agroland.feature.dealer.data.DeliveryZone
import com.agroland.feature.dealer.data.DeliveryZoneDraft
import com.agroland.feature.dealer.data.OrderTracking
import com.agroland.feature.dealer.data.TariffConditions
import com.agroland.feature.dealer.data.TeamPoolOrder
import com.agroland.feature.dealer.data.toDealerError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * DealerProductsViewModel — DealerProductsNotifier + Өнімдер табы: статус
 * чиптері (active/pending/inactive/rejected) + іздеу (debounce 500ms) +
 * activate/deactivate/delete + reject-message.
 */
@HiltViewModel
class DealerProductsViewModel @Inject constructor(
    private val repository: DealerRepository,
) : ViewModel() {

    sealed interface Event {
        data class ShowError(val error: DealerError) : Event
        /** Бас тарту себебі — диалогта көрсетіледі (жүктеу қатесі де мәтін ретінде). */
        data class ShowRejectMessage(val title: Long, val message: String) : Event
        data object ProductDeleted : Event
    }

    private val _status = MutableStateFlow("active")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    private val _items = MutableStateFlow<List<DealerProduct>>(emptyList())
    val items: StateFlow<List<DealerProduct>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loading.asStateFlow()

    private val _exhausted = MutableStateFlow(false)
    val exhausted: StateFlow<Boolean> = _exhausted.asStateFlow()

    private val _error = MutableStateFlow<DealerError?>(null)
    val error: StateFlow<DealerError?> = _error.asStateFlow()

    private val _actionInProgress = MutableStateFlow<Long?>(null)
    val actionInProgress: StateFlow<Long?> = _actionInProgress.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private var page = 1
    private var seenIds = mutableSetOf<Long>()
    private var searchJob: Job? = null

    init {
        refresh()
    }

    fun setStatus(value: String) {
        if (_status.value == value) return
        _status.value = value
        refresh()
    }

    /** Іздеу — debounce 500ms (Flutter provider key өзгерісімен бірдей). */
    fun setSearch(value: String) {
        _search.value = value
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            page = 1
            seenIds = mutableSetOf()
            val result = repository.getProducts(
                status = _status.value,
                search = _search.value.takeIf { it.isNotEmpty() },
                page = 1,
            )
            when (result) {
                is ApiResult.Success -> {
                    _items.value = result.value.items.distinctBy { it.id }
                    seenIds = _items.value.map { it.id }.toMutableSet()
                    _exhausted.value = !result.value.canLoadMore
                }
                is ApiResult.Error -> _error.value = result.failure.toDealerError()
            }
            _loading.value = false
        }
    }

    fun loadMore() {
        if (_loading.value || _loadingMore.value || _exhausted.value) return
        viewModelScope.launch {
            _loadingMore.value = true
            when (
                val result = repository.getProducts(
                    status = _status.value,
                    search = _search.value.takeIf { it.isNotEmpty() },
                    page = page + 1,
                )
            ) {
                is ApiResult.Success -> {
                    page += 1
                    val fresh = result.value.items.filter { seenIds.add(it.id) }
                    _items.value = _items.value + fresh
                    _exhausted.value = !result.value.canLoadMore
                }
                is ApiResult.Error -> _events.emit(Event.ShowError(result.failure.toDealerError()))
            }
            _loadingMore.value = false
        }
    }

    fun activate(id: Long) = runAction(id, action = { repository.activateProduct(id) })

    fun deactivate(id: Long) = runAction(id, action = { repository.deactivateProduct(id) })

    fun delete(id: Long) = runAction(id, action = { repository.deleteProduct(id) }, emitDeleted = true)

    private fun runAction(
        id: Long,
        action: suspend () -> ApiResult<Unit>,
        emitDeleted: Boolean = false,
    ) {
        if (_actionInProgress.value != null) return
        viewModelScope.launch {
            _actionInProgress.value = id
            when (val result = action()) {
                is ApiResult.Success -> {
                    _items.value = _items.value.filter { it.id != id }
                    if (emitDeleted) _events.emit(Event.ProductDeleted)
                }
                is ApiResult.Error ->
                    _events.emit(Event.ShowError(result.failure.toDealerError()))
            }
            _actionInProgress.value = null
        }
    }

    /**
     * Бас тарту себебін жүктеп, диалогта көрсетеді. Flutter сияқты жүктеу
     * қатесі де «табылмады» емес, қате мәтіні ретінде көрсетіледі.
     */
    fun showRejectReason(id: Long) {
        viewModelScope.launch {
            _actionInProgress.value = id
            when (val result = repository.getRejectMessage(id)) {
                is ApiResult.Success -> _events.emit(Event.ShowRejectMessage(id, result.value))
                is ApiResult.Error ->
                    _events.emit(Event.ShowError(result.failure.toDealerError()))
            }
            _actionInProgress.value = null
        }
    }
}

/** DeliveryZonesViewModel — DealerDeliveryZonesNotifier: тізім + жою (404 tolerance). */
@HiltViewModel
class DeliveryZonesViewModel @Inject constructor(
    private val repository: DealerRepository,
) : ViewModel() {

    sealed interface Event {
        data class ShowError(val error: DealerError) : Event
        data object ZoneDeleted : Event
    }

    private val _zones = MutableStateFlow<List<DeliveryZone>>(emptyList())
    val zones: StateFlow<List<DeliveryZone>> = _zones.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<DealerError?>(null)
    val error: StateFlow<DealerError?> = _error.asStateFlow()

    private val _deleteInProgress = MutableStateFlow<Long?>(null)
    val deleteInProgress: StateFlow<Long?> = _deleteInProgress.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.getDeliveryZones()) {
                is ApiResult.Success -> _zones.value = result.value
                is ApiResult.Error -> _error.value = result.failure.toDealerError()
            }
            _loading.value = false
        }
    }

    fun delete(id: Long) {
        if (_deleteInProgress.value != null) return
        viewModelScope.launch {
            _deleteInProgress.value = id
            when (val result = repository.deleteDeliveryZone(id)) {
                is ApiResult.Success -> {
                    _zones.value = _zones.value.filter { it.id != id }
                    _events.emit(Event.ZoneDeleted)
                }
                is ApiResult.Error ->
                    _events.emit(Event.ShowError(result.failure.toDealerError()))
            }
            _deleteInProgress.value = null
        }
    }
}

/**
 * AddEditDeliveryZoneViewModel — AddEditLogisticsPage: зона жүктеу (өңдеу)
 * + сақтау. Country/Region/District таңдауыштары ортақ LocationPickerViewModel
 * арқылы (кештелген каталогтар, CatalogPickerDialog).
 * Өңдеу кезінде зона id Route-тан келеді (-1 = жаңа).
 */
@HiltViewModel
class AddEditDeliveryZoneViewModel @Inject constructor(
    private val repository: DealerRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    sealed interface Event {
        data class ShowError(val error: DealerError) : Event
        /** Сақтау сәтті — бет жабылады, тізім қайта жүктеледі. */
        data object Saved : Event
    }

    /** Route параметрі; -1 = жаңа зона. */
    val zoneId: Long = savedStateHandle["zoneId"] ?: -1L

    private val _editingZone = MutableStateFlow<DeliveryZone?>(null)
    val editingZone: StateFlow<DeliveryZone?> = _editingZone.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    init {
        if (zoneId > 0) {
            viewModelScope.launch {
                when (val result = repository.getDeliveryZones()) {
                    is ApiResult.Success ->
                        _editingZone.value = result.value.firstOrNull { it.id == zoneId }
                    is ApiResult.Error ->
                        _events.emit(Event.ShowError(result.failure.toDealerError()))
                }
            }
        }
    }

    /**
     * Сақтау — клиент жағынан тексерістер UI-де (сома > 0, ел/облыс
     * міндетті); сәттілікте Saved, қатеде ShowError (snackbar).
     */
    fun save(draft: DeliveryZoneDraft) {
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            val result = if (zoneId > 0) {
                repository.updateDeliveryZone(zoneId, draft)
            } else {
                repository.createDeliveryZone(draft)
            }
            when (result) {
                is ApiResult.Success -> _events.emit(Event.Saved)
                is ApiResult.Error ->
                    _events.emit(Event.ShowError(result.failure.toDealerError()))
            }
            _saving.value = false
        }
    }
}

/**
 * DealerAnalyticsViewModel — DealerAnalyticsNotifier + CurrentTariff гейті:
 * bucket day→30d / week→84d / month→365d; locked → placeholder (қате емес).
 */
@HiltViewModel
class DealerAnalyticsViewModel @Inject constructor(
    private val repository: DealerRepository,
) : ViewModel() {

    sealed interface Event {
        data class ShowError(val error: DealerError) : Event
    }

    private val _bucket = MutableStateFlow("day")
    val bucket: StateFlow<String> = _bucket.asStateFlow()

    private val _conditions = MutableStateFlow(TariffConditions.FALLBACK)
    val conditions: StateFlow<TariffConditions> = _conditions.asStateFlow()

    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private val _analytics = MutableStateFlow<DealerAnalytics?>(null)
    val analytics: StateFlow<DealerAnalytics?> = _analytics.asStateFlow()

    private val _points = MutableStateFlow<List<AnalyticsPoint>>(emptyList())
    val points: StateFlow<List<AnalyticsPoint>> = _points.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _chartLoading = MutableStateFlow(true)
    val chartLoading: StateFlow<Boolean> = _chartLoading.asStateFlow()

    private val _error = MutableStateFlow<DealerError?>(null)
    val error: StateFlow<DealerError?> = _error.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    init {
        load()
    }

    /** bucket → periodDays: day=30, week=84 (~12 апта), month=365. */
    private fun periodDays(): Int = when (_bucket.value) {
        "week" -> 84
        "month" -> 365
        else -> 30
    }

    fun setBucket(value: String) {
        if (_bucket.value == value) return
        _bucket.value = value
        load()
    }

    fun load() {
        viewModelScope.launch {
            // Тариф гейті ешқашан қате көрсетпейді — парсинг сәтсіз болса
            // fallback шарттарымен жалғасады (Flutter CurrentTariffNotifier).
            when (val result = repository.getTariffConditions()) {
                is ApiResult.Success -> _conditions.value = result.value
                is ApiResult.Error -> _conditions.value = TariffConditions.FALLBACK
            }
            _locked.value = !_conditions.value.hasFeature("analytics_enabled")
            if (_locked.value) {
                _loading.value = false
                _chartLoading.value = false
                return@launch
            }

            _loading.value = true
            _chartLoading.value = true
            _error.value = null
            val period = periodDays()

            when (val result = repository.getAnalytics(period)) {
                is ApiResult.Success -> _analytics.value = result.value
                is ApiResult.Error -> _error.value = result.failure.toDealerError()
            }
            _loading.value = false

            when (val ts = repository.getAnalyticsTimeseries(period, _bucket.value)) {
                is ApiResult.Success -> _points.value = ts.value
                // Timeseries — декоратив: қате жағдайда бос график, қате UI-ға шықпайды.
                is ApiResult.Error -> _points.value = emptyList()
            }
            _chartLoading.value = false
        }
    }
}

/**
 * DealerOrdersViewModel — DealerOrdersNotifier family: әр таб үшін жеке күй
 * (Flutter family provider), tab_counts 'new' жауабынан; accept/reject/ship
 * тізімнен жояды және санды түзетеді.
 */
@HiltViewModel
class DealerOrdersViewModel @Inject constructor(
    private val repository: DealerRepository,
) : ViewModel() {

    sealed interface Event {
        data class ShowError(val error: DealerError) : Event
        data object OrderAccepted : Event
        data object OrderShipped : Event
    }

    data class TabState(
        val items: List<DealerOrder> = emptyList(),
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val exhausted: Boolean = false,
        val error: DealerError? = null,
        val loaded: Boolean = false,
    )

    /** Flutter таб атаулары (backend tab param). */
    val tabs: List<String> = listOf("new", "confirmed", "in_transit", "delivered", "cancelled")

    private val _states = MutableStateFlow<Map<String, TabState>>(tabs.associateWith { TabState() })
    val states: StateFlow<Map<String, TabState>> = _states.asStateFlow()

    private val _tabCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val tabCounts: StateFlow<Map<String, Int>> = _tabCounts.asStateFlow()

    private val _actionInProgress = MutableStateFlow<Long?>(null)
    val actionInProgress: StateFlow<Long?> = _actionInProgress.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private var pages = mutableMapOf<String, Int>()

    init {
        // Бірінші таб ('new') — badge сандары үшін де жүктеледі.
        load("new")
    }

    fun load(tab: String) {
        viewModelScope.launch {
            update(tab) { it.copy(loading = true, error = null, loaded = true) }
            pages[tab] = 1
            when (val result = repository.getDealerOrders(tab, 1)) {
                is ApiResult.Success -> {
                    _tabCounts.value = result.value.tabCounts
                    update(tab) {
                        it.copy(
                            items = result.value.items.distinctBy { o -> o.id },
                            exhausted = !result.value.canLoadMore,
                        )
                    }
                }
                is ApiResult.Error ->
                    update(tab) { it.copy(error = result.failure.toDealerError()) }
            }
            update(tab) { it.copy(loading = false) }
        }
    }

    fun loadMore(tab: String) {
        val state = _states.value[tab] ?: return
        if (state.loading || state.loadingMore || state.exhausted) return
        viewModelScope.launch {
            update(tab) { it.copy(loadingMore = true) }
            val nextPage = (pages[tab] ?: 1) + 1
            when (val result = repository.getDealerOrders(tab, nextPage)) {
                is ApiResult.Success -> {
                    pages[tab] = nextPage
                    val existing = _states.value[tab]?.items ?: emptyList()
                    val seen = existing.map { it.id }.toMutableSet()
                    val fresh = result.value.items.filter { seen.add(it.id) }
                    update(tab) {
                        it.copy(
                            items = existing + fresh,
                            exhausted = !result.value.canLoadMore,
                        )
                    }
                }
                is ApiResult.Error ->
                    _events.emit(Event.ShowError(result.failure.toDealerError()))
            }
            update(tab) { it.copy(loadingMore = false) }
        }
    }

    fun accept(orderId: Long, tab: String) = runOrderAction(
        orderId,
        tab,
        action = { repository.acceptOrder(orderId) },
        success = Event.OrderAccepted,
    )

    fun reject(orderId: Long, reason: String?, tab: String) = runOrderAction(orderId, tab, {
        repository.rejectOrder(orderId, reason?.takeIf { it.isNotEmpty() })
    })

    fun ship(orderId: Long, tab: String) = runOrderAction(
        orderId,
        tab,
        action = { repository.shipOrder(orderId) },
        success = Event.OrderShipped,
    )

    private fun runOrderAction(
        orderId: Long,
        tab: String,
        action: suspend () -> ApiResult<Unit>,
        success: Event? = null,
    ) {
        if (_actionInProgress.value != null) return
        viewModelScope.launch {
            _actionInProgress.value = orderId
            when (val result = action()) {
                is ApiResult.Success -> {
                    removeLocal(orderId, tab)
                    if (success != null) _events.emit(success)
                }
                is ApiResult.Error ->
                    _events.emit(Event.ShowError(result.failure.toDealerError()))
            }
            _actionInProgress.value = null
        }
    }

    private fun removeLocal(orderId: Long, tab: String) {
        update(tab) { it.copy(items = it.items.filter { o -> o.id != orderId }) }
        _tabCounts.value = _tabCounts.value.toMutableMap().apply {
            put(tab, (this[tab] ?: 0).coerceAtLeast(1) - 1)
        }
    }

    private fun update(tab: String, transform: (TabState) -> TabState) {
        _states.value = _states.value.toMutableMap().apply {
            put(tab, transform(this[tab] ?: TabState()))
        }
    }
}

/** TeamPoolViewModel — TeamPoolNotifier: bare list + claim (тізімнен жояды). */
@HiltViewModel
class TeamPoolViewModel @Inject constructor(
    private val repository: DealerRepository,
) : ViewModel() {

    sealed interface Event {
        data class ShowError(val error: DealerError) : Event
        data class Claimed(val orderId: Long) : Event
    }

    private val _orders = MutableStateFlow<List<TeamPoolOrder>>(emptyList())
    val orders: StateFlow<List<TeamPoolOrder>> = _orders.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<DealerError?>(null)
    val error: StateFlow<DealerError?> = _error.asStateFlow()

    private val _claimInProgress = MutableStateFlow<Long?>(null)
    val claimInProgress: StateFlow<Long?> = _claimInProgress.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.getTeamPool()) {
                is ApiResult.Success -> _orders.value = result.value
                is ApiResult.Error -> _error.value = result.failure.toDealerError()
            }
            _loading.value = false
        }
    }

    /** Claim сәтті — тізімнен жойылып, snackbar «#id қабылданды». */
    fun claim(orderId: Long) {
        if (_claimInProgress.value != null) return
        viewModelScope.launch {
            _claimInProgress.value = orderId
            when (val result = repository.claimOrder(orderId)) {
                is ApiResult.Success -> {
                    _orders.value = _orders.value.filter { it.id != orderId }
                    _events.emit(Event.Claimed(orderId))
                }
                is ApiResult.Error ->
                    _events.emit(Event.ShowError(result.failure.toDealerError()))
            }
            _claimInProgress.value = null
        }
    }
}

/** DealerEmployeesViewModel — DealerEmployeesNotifier: тізім + қосу + жою. */
@HiltViewModel
class DealerEmployeesViewModel @Inject constructor(
    private val repository: DealerRepository,
) : ViewModel() {

    sealed interface Event {
        data class ShowError(val error: DealerError) : Event
        data class EmployeeAdded(val name: String) : Event
        data class EmployeeDeleted(val name: String) : Event
    }

    private val _employees = MutableStateFlow<List<DealerEmployee>>(emptyList())
    val employees: StateFlow<List<DealerEmployee>> = _employees.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<DealerError?>(null)
    val error: StateFlow<DealerError?> = _error.asStateFlow()

    private val _addInProgress = MutableStateFlow(false)
    val addInProgress: StateFlow<Boolean> = _addInProgress.asStateFlow()

    private val _deleteInProgress = MutableStateFlow<Long?>(null)
    val deleteInProgress: StateFlow<Long?> = _deleteInProgress.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.getEmployees()) {
                is ApiResult.Success -> _employees.value = result.value
                is ApiResult.Error -> _error.value = result.failure.toDealerError()
            }
            _loading.value = false
        }
    }

    /** Клиент валидациясы (аты, телефон 11 цифр, ЖСН 12) UI-де; қате — dialog. */
    fun add(name: String, phone: String, dealerRole: String, iin: String) {
        if (_addInProgress.value) return
        viewModelScope.launch {
            _addInProgress.value = true
            when (val result = repository.addEmployee(name, phone, dealerRole, iin)) {
                is ApiResult.Success -> {
                    _employees.value = _employees.value + result.value
                    _events.emit(Event.EmployeeAdded(name))
                }
                is ApiResult.Error ->
                    _events.emit(Event.ShowError(result.failure.toDealerError()))
            }
            _addInProgress.value = false
        }
    }

    fun delete(employee: DealerEmployee) {
        if (_deleteInProgress.value != null) return
        viewModelScope.launch {
            _deleteInProgress.value = employee.id
            when (val result = repository.deleteEmployee(employee.id)) {
                is ApiResult.Success -> {
                    _employees.value = _employees.value.filter { it.id != employee.id }
                    _events.emit(Event.EmployeeDeleted(employee.name))
                }
                is ApiResult.Error ->
                    _events.emit(Event.ShowError(result.failure.toDealerError()))
            }
            _deleteInProgress.value = null
        }
    }
}

/** OrderTrackingViewModel — GET /orders/{id}/tracking (orderId Route-тан). */
@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    private val repository: DealerRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _tracking = MutableStateFlow<OrderTracking?>(null)
    val tracking: StateFlow<OrderTracking?> = _tracking.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<DealerError?>(null)
    val error: StateFlow<DealerError?> = _error.asStateFlow()

    private val orderId: Long = savedStateHandle["orderId"] ?: -1L

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.getOrderTracking(orderId)) {
                is ApiResult.Success -> _tracking.value = result.value
                is ApiResult.Error -> _error.value = result.failure.toDealerError()
            }
            _loading.value = false
        }
    }
}

/**
 * Дилер рөлінің қақпалары — UserProfile.dealer_role (Фаза 16):
 * TeamPool иконкасы (manager|director) және Қызметкерлер басқаруы
 * (director). Flutter профиль провайдерінен оқыған сияқты — беттер
 * өздері жүктеп, MainActivity параметр жібермейді.
 */
@HiltViewModel
class DealerAccessViewModel @Inject constructor(
    private val profileRepository: com.agroland.feature.profile.data.ProfileRepository,
) : ViewModel() {

    private val _profile = MutableStateFlow<com.agroland.feature.profile.data.UserProfile?>(null)
    val profile: StateFlow<com.agroland.feature.profile.data.UserProfile?> = _profile.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = profileRepository.getProfile()) {
                is ApiResult.Success -> _profile.value = result.value
                is ApiResult.Error -> Unit // Гейт қаупсіз: profile == null → екі мүмкіндік те жабық.
            }
        }
    }
}

/**
 * Дилер баптаулары — Flutter DealerSettingsPage: баланс плиткасы (әмиянға
 * ашады) + VAT тумблері (PATCH /user/profile {is_vat_payer}) және
 * «Деректер жаңартылды» toast.
 */
@HiltViewModel
class DealerSettingsViewModel @Inject constructor(
    private val profileRepository: com.agroland.feature.profile.data.ProfileRepository,
) : ViewModel() {

    sealed interface Event {
        data class ShowError(val error: DealerError) : Event
        data object DataUpdated : Event
    }

    private val _profile = MutableStateFlow<com.agroland.feature.profile.data.UserProfile?>(null)
    val profile: StateFlow<com.agroland.feature.profile.data.UserProfile?> = _profile.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            when (val result = profileRepository.getProfile()) {
                is ApiResult.Success -> _profile.value = result.value
                is ApiResult.Error ->
                    _events.emit(Event.ShowError(result.failure.toDealerError()))
            }
            _loading.value = false
        }
    }

    /** VAT тумблері — сәтті болса профиль жаңартылып, toast көрсетіледі. */
    fun setVatPayer(value: Boolean) {
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            when (val result = profileRepository.updateVatPayer(value)) {
                is ApiResult.Success -> {
                    _profile.value = result.value
                    _events.emit(Event.DataUpdated)
                }
                is ApiResult.Error ->
                    _events.emit(Event.ShowError(result.failure.toDealerError()))
            }
            _saving.value = false
        }
    }
}