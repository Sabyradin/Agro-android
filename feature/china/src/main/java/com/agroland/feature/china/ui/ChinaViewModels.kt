package com.agroland.feature.china.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.core.network.error.Failure
import com.agroland.feature.china.data.ChinaCartItem
import com.agroland.feature.china.data.ChinaCategory
import com.agroland.feature.china.data.ChinaOrder
import com.agroland.feature.china.data.ChinaProduct
import com.agroland.feature.china.data.ChinaProductDetail
import com.agroland.feature.china.data.ChinaProductsPage
import com.agroland.feature.china.data.ChinaRepository
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
 * UI-ға берiлетін қате: backend адам тіліндегі message > generic.
 * Шикі error_code ешқашан көрсетілмейді (спек §4).
 */
data class ChinaError(
    val backendMessage: String? = null,
    val isNetwork: Boolean = false,
)

fun ChinaError.displayText(networkMessage: String, genericMessage: String): String =
    backendMessage ?: if (isNetwork) networkMessage else genericMessage

fun Failure.toChinaError(): ChinaError = when (this) {
    is Failure.Network -> ChinaError(isNetwork = true)
    else -> ChinaError(backendMessage = (this as? Failure.Http)?.error?.message)
}

/** China экрандарының бір реттелген оқиғалары. */
sealed interface ChinaEvent {
    data class ShowError(val error: ChinaError) : ChinaEvent
    /** Корзинаға қосылды. */
    data object ItemAdded : ChinaEvent
    /** Тапсырыс MercuryX-ке жөнелтілді (меркуриX нөмірі опционал). */
    data class OrderForwarded(val mercuryxNumber: String?) : ChinaEvent
}

/** Қытай каталог күйлерінің ортақ жинасы. */
data class CatalogState(
    val categories: List<ChinaCategory> = emptyList(),
    val categoriesLoading: Boolean = true,
    val categoriesError: Failure? = null,
    val popular: List<ChinaProduct> = emptyList(),
    val popularLoading: Boolean = true,
)

/**
 * «Товары из Китая» басты мазмұны — категория жолы + «Танымал тауарлар» каруселі.
 * MercuryX-те нақты popular эндпоинті жоқ: алғашқы 3 root категорияның 1-бетін
 * қатар алып біріктіреміз (Flutter chinaPopularProductsProvider).
 */
@HiltViewModel
class ChinaCatalogViewModel @Inject constructor(
    private val repository: ChinaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CatalogState())
    val state = _state.asStateFlow()

    val events = MutableSharedFlow<ChinaEvent>(extraBufferCapacity = 8)

    init {
        load()
    }

    fun load() {
        loadCategories()
        loadPopular()
    }

    fun retryCategories() = loadCategories()

    fun retryPopular() = loadPopular()

    private fun loadCategories() {
        viewModelScope.launch {
            _state.value = _state.value.copy(categoriesLoading = true, categoriesError = null)
            when (val result = repository.getCategories()) {
                is ApiResult.Success -> _state.value =
                    _state.value.copy(categories = result.value, categoriesLoading = false)
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        categoriesLoading = false,
                        categoriesError = result.failure,
                    )
                    events.tryEmit(ChinaEvent.ShowError(result.failure.toChinaError()))
                }
            }
        }
    }

    private fun loadPopular() {
        viewModelScope.launch {
            _state.value = _state.value.copy(popularLoading = true)
            // Алғашқы 3 root категорияның 1-беті, dedupe, 12 шектеу.
            val cats = (repository.getCategories() as? ApiResult.Success)?.value ?: emptyList()
            val merged = LinkedHashMap<Long, ChinaProduct>()
            for (category in cats.take(3)) {
                if (merged.size >= 12) break
                val page = (repository.getProducts(category.id, page = 1, perPage = 6)
                    as? ApiResult.Success)?.value ?: continue
                for (product in page.products) {
                    merged.putIfAbsent(product.productId, product)
                    if (merged.size >= 12) break
                }
            }
            _state.value = _state.value.copy(popular = merged.values.toList(), popularLoading = false)
        }
    }
}

/** Категория балалары (ChinaSubcategoriesPage, parent_id аргументі). */
@HiltViewModel
class ChinaSubcategoriesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChinaRepository,
) : ViewModel() {

    val parentId: Long = savedStateHandle.get<Long>("parentId") ?: -1L

    private val _categories = MutableStateFlow<List<ChinaCategory>>(emptyList())
    val categories = _categories.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<Failure?>(null)
    val error = _error.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.getCategories(parentId.takeIf { it > 0 })) {
                is ApiResult.Success -> _categories.value = result.value
                is ApiResult.Error -> _error.value = result.failure
            }
            _loading.value = false
        }
    }
}

/** Категория тауарларының пагинациялы күйі (Flutter ChinaProductsNotifier). */
@HiltViewModel
class ChinaProductsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChinaRepository,
) : ViewModel() {

    val categoryId: Long = savedStateHandle.get<Long>("categoryId") ?: 0L

    private val _items = MutableStateFlow<List<ChinaProduct>>(emptyList())
    val items = _items.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore = _loadingMore.asStateFlow()

    private val _exhausted = MutableStateFlow(false)
    val exhausted = _exhausted.asStateFlow()

    private val _error = MutableStateFlow<Failure?>(null)
    val error = _error.asStateFlow()

    private val events = MutableSharedFlow<ChinaEvent>(extraBufferCapacity = 8)

    private var page = 1

    init {
        loadFirst()
    }

    fun loadFirst() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            page = 1
            when (val result = repository.getProducts(categoryId, page = 1)) {
                is ApiResult.Success -> {
                    _items.value = result.value.products
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
            when (val result = repository.getProducts(categoryId, page = page)) {
                is ApiResult.Success -> {
                    val existing = _items.value.map { it.productId }.toSet()
                    val fresh = result.value.products.filter { it.productId !in existing }
                    if (fresh.isEmpty()) {
                        _exhausted.value = true
                    } else {
                        _items.value = _items.value + fresh
                        page++
                        _exhausted.value = !result.value.canLoadMore
                    }
                }
                is ApiResult.Error -> events.tryEmit(ChinaEvent.ShowError(result.failure.toChinaError()))
            }
            _loadingMore.value = false
        }
    }
}

/** Толық карточка (ChinaProductDetailPage). */
@HiltViewModel
class ChinaProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChinaRepository,
) : ViewModel() {

    val productId: Long = savedStateHandle.get<Long>("productId") ?: -1L

    private val _detail = MutableStateFlow<ChinaProductDetail?>(null)
    val detail = _detail.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<Failure?>(null)
    val error = _error.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.getProductDetail(productId)) {
                is ApiResult.Success -> _detail.value = result.value
                is ApiResult.Error -> _error.value = result.failure
            }
            _loading.value = false
        }
    }
}

/** Қытай корзинасы — optimistic update/delete (Flutter ChinaCartItemsNotifier). */
@HiltViewModel
class ChinaCartViewModel @Inject constructor(
    private val repository: ChinaRepository,
) : ViewModel() {

    private val _items = MutableStateFlow<List<ChinaCartItem>>(emptyList())
    val items = _items.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<Failure?>(null)
    val error = _error.asStateFlow()

    private val _events = MutableSharedFlow<ChinaEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<ChinaEvent> = _events.asSharedFlow()

    private val _adding = MutableStateFlow(false)
    val adding = _adding.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.getCart()) {
                is ApiResult.Success -> _items.value = result.value
                is ApiResult.Error -> _error.value = result.failure
            }
            _loading.value = false
        }
    }

    /** Корзинаға қосу — сәтті болса ItemAdded, қате болса ShowError. */
    fun addItem(
        productId: Long,
        skuId: Long?,
        quantity: Int,
        minQty: Int,
        price: Double,
        titleSnapshot: String,
        imageUrl: String?,
    ) {
        if (_adding.value) return
        viewModelScope.launch {
            _adding.value = true
            when (
                val result = repository.addCartItem(
                    com.agroland.feature.china.data.ChinaAddCartItemRequest(
                        productId = productId,
                        skuId = skuId,
                        quantity = quantity,
                        minQty = minQty,
                        priceAtAdd = price,
                        titleSnapshot = titleSnapshot,
                        imageUrl = imageUrl,
                    ),
                )
            ) {
                is ApiResult.Success -> {
                    refresh()
                    _events.tryEmit(ChinaEvent.ItemAdded)
                }
                is ApiResult.Error ->
                    _events.tryEmit(ChinaEvent.ShowError(result.failure.toChinaError()))
            }
            _adding.value = false
        }
    }

    /** Санын өзгерту — optimistic, қате болса қайта жүктеп, себебін көрсетеміз. */
    fun updateQuantity(itemId: Long, quantity: Int) {
        val prev = _items.value
        _items.value = prev.map { if (it.id == itemId) it.copy(quantity = quantity) else it }
        viewModelScope.launch {
            when (val result = repository.updateCartItem(itemId, quantity)) {
                is ApiResult.Error -> {
                    _events.tryEmit(ChinaEvent.ShowError(result.failure.toChinaError()))
                    refresh()
                }
                else -> Unit
            }
        }
    }

    /** Жою — optimistic, қате болса қайта жүктеп, себебін көрсетеміз. */
    fun deleteItem(itemId: Long) {
        val prev = _items.value
        _items.value = prev.filter { it.id != itemId }
        viewModelScope.launch {
            when (val result = repository.deleteCartItem(itemId)) {
                is ApiResult.Error -> {
                    _events.tryEmit(ChinaEvent.ShowError(result.failure.toChinaError()))
                    refresh()
                }
                else -> Unit
            }
        }
    }
}

/** Қытай тапсырыстары — тарих + жаңа тапсырыс жасау (MercuryX forward). */
@HiltViewModel
class ChinaOrdersViewModel @Inject constructor(
    private val repository: ChinaRepository,
) : ViewModel() {

    private val _orders = MutableStateFlow<List<ChinaOrder>>(emptyList())
    val orders = _orders.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<Failure?>(null)
    val error = _error.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting = _submitting.asStateFlow()

    private val _events = MutableSharedFlow<ChinaEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<ChinaEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.getOrders()) {
                is ApiResult.Success -> _orders.value = result.value
                is ApiResult.Error -> _error.value = result.failure
            }
            _loading.value = false
        }
    }

    /**
     * Тапсырыс жасау (phone 11d 77…, bin 12d — клиент тексереді, consent UI-да).
     * Сәтті болса OrderForwarded(mercuryxNumber) + тарих/корзина жаңартылады.
     */
    fun createOrder(phone: String, bin: String, cartItemIds: List<Long>) {
        if (_submitting.value) return
        viewModelScope.launch {
            _submitting.value = true
            when (val result = repository.createOrder(phone, bin, cartItemIds)) {
                is ApiResult.Success -> {
                    load()
                    _events.tryEmit(ChinaEvent.OrderForwarded(result.value.mercuryxNumber))
                }
                is ApiResult.Error ->
                    _events.tryEmit(ChinaEvent.ShowError(result.failure.toChinaError()))
            }
            _submitting.value = false
        }
    }
}