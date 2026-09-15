package com.agroland.feature.cart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.network.ApiResult
import com.agroland.feature.cart.data.CartItem
import com.agroland.feature.cart.data.CartPreview
import com.agroland.feature.cart.data.CartRepository
import com.agroland.feature.cart.data.CartSection
import com.agroland.feature.cart.data.CheckoutLine
import com.agroland.feature.cart.data.Order
import com.agroland.feature.cart.data.sectionClientFilter
import com.agroland.feature.cart.data.sectionQuery
import com.agroland.feature.profile.data.ProfileRepository
import com.agroland.feature.profile.data.UserLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Чекаут парағындағы бір тауар: жеткізу/өзі алу + таңдалған зона. */
data class CheckoutItemUi(
    val item: CartItem,
    val isDelivery: Boolean,
    val deliveryZoneId: Long? = null,
)

/** SupplierPicker-дегі бір бизнес топтамасы. */
data class SupplierGroup(
    val supplierId: Long,
    val items: List<CartItem>,
)

/** CheckoutSheet күйі. */
data class CheckoutUiState(
    val visible: Boolean = false,
    /** Multi-supplier режимде таңдалған бизнес (only_supplier_id). */
    val supplierId: Long? = null,
    val items: List<CheckoutItemUi> = emptyList(),
    val addresses: List<UserLocation> = emptyList(),
    val selectedAddressId: Long? = null,
    val preview: CartPreview? = null,
    val previewLoading: Boolean = false,
    val submitting: Boolean = false,
) {
    /** Ешбір тауар жеткізілмейтін болса — тек өзі алу режимі. */
    val pickupOnly: Boolean get() = items.isNotEmpty() && items.none { it.isDelivery }

    /** Жеткізу режимі бар ма (кем дегенде бір тауар жеткізіледі)? */
    val anyDelivery: Boolean get() = items.any { it.isDelivery }

    /** Өзі алатын тауарлардың сатушы мекенжайы (чекаут pickup_address). */
    val pickupAddress: String?
        get() = items.firstOrNull { !it.isDelivery }?.item?.announcement?.base?.pickupAddress

    /** Таңдалған мекенжай. */
    val selectedAddress: UserLocation?
        get() = addresses.firstOrNull { it.id == selectedAddressId }

    /** Барлық жеткізілетін тауарлдың зонасы таңдалған ба? */
    val allDeliveryZonesSelected: Boolean
        get() = items.filter { it.isDelivery }.all { it.deliveryZoneId != null }

    /** Тапсырыс сұрауы дайын ба? */
    val canSubmit: Boolean
        get() = items.isNotEmpty() && !submitting &&
            (!anyDelivery || (selectedAddress?.districtId != null && allDeliveryZonesSelected))

    /** Жеткізу сомасы preview-дан (Smart Calculator grand_total). */
    val grandTotal: Double get() = preview?.grandTotal ?: 0.0
}

/**
 * CartViewModel — Flutter cart_notifier + cart_items_notifier + checkout_bottom_sheet
 * state-терінің бірігуі: 5 бөлім, оптимистік сан/өшіру, SupplierPicker, чекаут.
 */
@HiltViewModel
class CartViewModel @Inject constructor(
    private val repository: CartRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<CartError?>(null)
    val error: StateFlow<CartError?> = _error.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _section = MutableStateFlow(CartSection.BASKET_ITEMS)
    val section: StateFlow<CartSection> = _section.asStateFlow()

    /** Әр бөлімнің тапсырыстары — refresh кезінде барлығы жүктеледі (Flutter _loadAll). */
    private val _sectionOrders = MutableStateFlow<Map<CartSection, List<Order>>>(emptyMap())
    val sectionOrders: StateFlow<Map<CartSection, List<Order>>> = _sectionOrders.asStateFlow()

    private val _ordersLoading = MutableStateFlow(false)
    val ordersLoading: StateFlow<Boolean> = _ordersLoading.asStateFlow()

    private val _supplierPickerVisible = MutableStateFlow(false)
    val supplierPickerVisible: StateFlow<Boolean> = _supplierPickerVisible.asStateFlow()

    private val _supplierGroups = MutableStateFlow<List<SupplierGroup>>(emptyList())
    val supplierGroups: StateFlow<List<SupplierGroup>> = _supplierGroups.asStateFlow()

    private val _checkout = MutableStateFlow(CheckoutUiState())
    val checkout: StateFlow<CheckoutUiState> = _checkout.asStateFlow()

    /** Пайдаланушы балансы — төлем парағының жеткіліктігін тексереді (Фаза 9). */
    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance.asStateFlow()

    private val _events = MutableSharedFlow<CartEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<CartEvent> = _events

    private var busy = mutableSetOf<Long>()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.getCart()) {
                is ApiResult.Success -> {
                    _items.value = result.value
                    // Flutter: себет жаңартылғанда барлығы таңдалып тұрады.
                    _selectedIds.value = result.value.map { it.id }.toSet()
                }
                is ApiResult.Error -> _error.value = result.failure.toCartError()
            }
            _loading.value = false
            loadOrderSections()
        }
    }

    /** 4 тапсырыс бөлімі параллель жүктеледі — клиенттік сүзгі қолданылады. */
    private suspend fun loadOrderSections() {
        _ordersLoading.value = true
        coroutineScope {
            listOf(
                CartSection.PAID_PENDING,
                CartSection.IN_PROGRESS,
                CartSection.DELIVERED,
                CartSection.ORDER_HISTORY,
            ).forEach { section ->
                launch {
                    val query = sectionQuery(section) ?: return@launch
                    when (val result = repository.getOrders(query)) {
                        is ApiResult.Success ->
                            _sectionOrders.update { it + (section to sectionClientFilter(section, result.value)) }
                        is ApiResult.Error -> {
                            // Тапсырыс бөлімінің қатесі бүкіл бетті бұзпайды — бөлім бос қалады.
                        }
                    }
                }
            }
        }
        _ordersLoading.value = false
    }

    fun setSection(section: CartSection) {
        _section.value = section
    }

    fun toggleSelected(id: Long) {
        _selectedIds.update { if (id in it) it - id else it + id }
    }

    fun toggleSelectAll() {
        val all = _selectedIds.value == _items.value.map { it.id }.toSet()
        _selectedIds.value = if (all) emptySet() else _items.value.map { it.id }.toSet()
    }

    /** Оптимистік сан өзгерту — қате болса getCart арқылы қайтару. */
    fun changeQuantity(item: CartItem, newQuantity: Double) {
        if (item.id in busy) return
        val clamped = newQuantity.coerceAtLeast(MIN_QTY)
        if (clamped == item.quantity) return
        busy += item.id
        _items.value = _items.value.map {
            if (it.id == item.id) it.copy(quantity = clamped) else it
        }
        viewModelScope.launch {
            when (val result = repository.updateQuantity(item.id, clamped)) {
                is ApiResult.Error -> {
                    revertCart()
                    _events.emit(CartEvent.ShowError(result.failure.toCartError()))
                }
                is ApiResult.Success -> Unit
            }
            busy -= item.id
        }
    }

    /** Оптимистік өшіру — қате болса қайтару. */
    fun deleteItem(item: CartItem) {
        if (item.id in busy) return
        busy += item.id
        _items.value = _items.value.filterNot { it.id == item.id }
        _selectedIds.update { it - item.id }
        viewModelScope.launch {
            when (val result = repository.deleteCartItem(item.id)) {
                is ApiResult.Error -> {
                    revertCart()
                    _events.emit(CartEvent.ShowError(result.failure.toCartError()))
                }
                is ApiResult.Success -> Unit
            }
            busy -= item.id
        }
    }

    private suspend fun revertCart() {
        when (val result = repository.getCart()) {
            is ApiResult.Success -> {
                _items.value = result.value
                _selectedIds.value = _selectedIds.value intersect result.value.map { it.id }.toSet()
            }
            is ApiResult.Error -> Unit
        }
    }

    /** «Төлемге өту»: бір бизнес → тікелей чекаут; бірнеше → SupplierPicker. */
    fun startCheckout() {
        val selected = _items.value.filter { it.id in _selectedIds.value }
        if (selected.isEmpty()) return
        val groups = selected
            .groupBy { it.authorId }
            .map { (supplierId, items) -> SupplierGroup(supplierId ?: 0L, items) }
        if (groups.size > 1) {
            _supplierGroups.value = groups
            _supplierPickerVisible.value = true
        } else {
            openCheckout(groups.firstOrNull())
        }
    }

    /** SupplierPicker-ден бизнес таңдалды. */
    fun chooseSupplier(group: SupplierGroup) {
        _supplierPickerVisible.value = false
        openCheckout(group)
    }

    fun dismissSupplierPicker() {
        _supplierPickerVisible.value = false
    }

    private fun openCheckout(group: SupplierGroup?) {
        val items = group?.items ?: _items.value.filter { it.id in _selectedIds.value }
        if (items.isEmpty()) return
        _checkout.value = CheckoutUiState(
            visible = true,
            supplierId = group?.supplierId?.takeIf { it != 0L },
            items = items.map { item ->
                val base = item.announcement?.base
                // Flutter CheckoutItem.isDelivery: жеткізу қолжетімді және зоналар бар.
                val isDelivery = base?.deliveryAvailable == true &&
                    item.announcement?.deliveryZones?.isNotEmpty() == true
                CheckoutItemUi(item = item, isDelivery = isDelivery)
            },
        )
        loadAddresses()
    }

    /** Профиль мекенжайлары — delivery_address/delivery_district_id көздері. */
    private fun loadAddresses() {
        viewModelScope.launch {
            when (val result = profileRepository.getProfile()) {
                is ApiResult.Success -> {
                    val locations = result.value.locations
                    _checkout.update { state ->
                        state.copy(
                            addresses = locations,
                            selectedAddressId = state.selectedAddressId
                                ?: locations.firstOrNull { it.districtId != null }?.id
                                ?: locations.firstOrNull()?.id,
                        )
                    }
                    loadPreview()
                }
                is ApiResult.Error ->
                    _events.emit(CartEvent.ShowError(result.failure.toCartError()))
            }
        }
    }

    fun dismissCheckout() {
        _checkout.update { it.copy(visible = false) }
    }

    /** Төлем парағы ашылар алдында баланс қайта оқылады. */
    fun loadBalance() {
        viewModelScope.launch {
            when (val result = profileRepository.getProfile()) {
                is ApiResult.Success -> _balance.value = result.value.balance
                is ApiResult.Error -> Unit // баланс оқылмаса — парақ 0 ₸ көрсетеді
            }
        }
    }

    /** Тауар режимін ауыстыру: жеткізуге болатын тауарлар ғана (pickupOnly құлыптайды). */
    fun toggleItemDelivery(cartItemId: Long) {
        _checkout.update { state ->
            if (state.pickupOnly) return@update state
            state.copy(
                items = state.items.map {
                    if (it.item.id == cartItemId) it.copy(isDelivery = !it.isDelivery) else it
                },
            )
        }
        loadPreview()
    }

    fun setItemZone(cartItemId: Long, zoneId: Long) {
        _checkout.update { state ->
            state.copy(
                items = state.items.map {
                    if (it.item.id == cartItemId) it.copy(deliveryZoneId = zoneId) else it
                },
            )
        }
    }

    fun selectAddress(id: Long) {
        _checkout.update { it.copy(selectedAddressId = id) }
        loadPreview()
    }

    /** Preview — аудан өзгерсе қайта оқылады (Smart Calculator). */
    private fun loadPreview() {
        val state = _checkout.value
        if (!state.visible) return
        val districtId = state.selectedAddress?.districtId
        _checkout.update { it.copy(previewLoading = true, preview = null) }
        viewModelScope.launch {
            val result = repository.getCartPreview(districtId?.takeIf { state.anyDelivery })
            _checkout.update { prev ->
                when (result) {
                    is ApiResult.Success -> prev.copy(preview = result.value, previewLoading = false)
                    is ApiResult.Error -> {
                        // Preview қатесі чекаутты блоктамайды — серверден сома келмей ғана қалады.
                        prev.copy(previewLoading = false)
                    }
                }
            }
        }
    }

    /** Чекаутты жіберу: pickup = !anyDelivery, delivery_address/lines зоналармен. */
    fun submitCheckout() {
        val state = _checkout.value
        if (!state.canSubmit) return
        _checkout.update { it.copy(submitting = true) }
        viewModelScope.launch {
            val address = state.selectedAddress
            val result = repository.checkout(
                deliveryDistrictId = if (state.anyDelivery) address?.districtId else null,
                pickup = !state.anyDelivery,
                deliveryAddress = if (state.anyDelivery) address?.fullAddress else null,
                pickupAddress = if (!state.anyDelivery) state.pickupAddress else null,
                onlySupplierId = state.supplierId,
                lines = state.items.map { CheckoutLine(it.item.id, it.deliveryZoneId) },
            )
            _checkout.update { it.copy(submitting = false) }
            when (result) {
                is ApiResult.Success -> {
                    // Flutter: totalAmount = Σ таңдалған тауарлар price × quantity
                    // (жеткізу құнысынсыз — PaymentMethodSheet көрсетуі үшін).
                    val totalAmount = state.items.sumOf { line ->
                        (line.item.announcement?.base?.price ?: 0.0) * line.item.quantity
                    }
                    _checkout.update { CheckoutUiState() }
                    _events.emit(CartEvent.CheckoutDone(result.value.orderIds, totalAmount))
                    loadBalance()
                    refresh()
                }
                is ApiResult.Error -> {
                    val cartError = result.failure.toCartError()
                    if (cartError.isMultiSupplier) {
                        // 409 MULTI_SUPPLIER_CART — чекаут жабылып, бизнес таңдауы ашылады.
                        _checkout.update { CheckoutUiState() }
                        startCheckout()
                    } else {
                        _events.emit(CartEvent.ShowError(cartError))
                    }
                }
            }
        }
    }

    /** «Тауарды қабылдау» — статус тізбегін серверден өтеді. */
    fun confirmReceipt(order: Order) {
        viewModelScope.launch {
            when (val result = repository.confirmDelivery(order.id, order.status)) {
                is ApiResult.Success -> {
                    _events.emit(CartEvent.ReceiptConfirmed)
                    loadOrderSections()
                }
                is ApiResult.Error ->
                    _events.emit(CartEvent.ShowError(result.failure.toCartError()))
            }
        }
    }

    fun reorder(order: Order) {
        viewModelScope.launch {
            when (val result = repository.reorder(order.id)) {
                is ApiResult.Success -> {
                    _events.emit(CartEvent.ReorderDone)
                    refresh()
                }
                is ApiResult.Error ->
                    _events.emit(CartEvent.ShowError(result.failure.toCartError()))
            }
        }
    }

    /** Себетті мәтінмен бөлісу (Flutter share): таңдалған тауарлар тізімі. */
    fun buildShareText(header: String): String {
        val selected = _items.value.filter { it.id in _selectedIds.value }
        if (selected.isEmpty()) return ""
        return buildString {
            append(header)
            append("\n\n")
            selected.forEach { item ->
                val base = item.announcement?.base
                val title = base?.title?.takeIf { it.isNotBlank() } ?: "#${item.announcementId}"
                val qty = item.quantity.trimTrailingZero()
                append("• $title")
                if (base?.price != null) append(" — ${PriceFormatter.format(base.price, base.currency ?: "₸")}")
                append(" ×$qty")
                append("\n")
            }
        }
    }

    private fun Double.trimTrailingZero(): String =
        if (this == this.toLong().toDouble()) this.toLong().toString() else toString()

    private companion object {
        const val MIN_QTY = 1.0
    }
}