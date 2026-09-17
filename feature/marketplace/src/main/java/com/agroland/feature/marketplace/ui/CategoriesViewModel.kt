package com.agroland.feature.marketplace.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.analytics.MonitoringService
import com.agroland.core.network.ApiResult
import com.agroland.feature.marketplace.data.Category
import com.agroland.feature.marketplace.data.MarketplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * CategoriesViewModel — grouped категориялар (6 bucket) + сабкатегориялар.
 * Activity scope: CategoriesPage → SubcategoriesPage бір кешті көреді,
 * сабкатегория беті атауды қайта сұрамау үшін.
 */
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: MarketplaceRepository,
    private val monitoringService: MonitoringService,
) : ViewModel() {

    /** Bucket кілті → категориялар. Кілттер: crops/livestock/products/technology/services/other. */
    private val _grouped = MutableStateFlow<Map<String, List<Category>>>(emptyMap())
    val grouped: StateFlow<Map<String, List<Category>>> = _grouped

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<MarketplaceError?>(null)
    val error: StateFlow<MarketplaceError?> = _error

    private val _events = MutableSharedFlow<MarketplaceEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<MarketplaceEvent> = _events

    private val _subcategories = MutableStateFlow<List<Category>>(emptyList())
    val subcategories: StateFlow<List<Category>> = _subcategories

    private val _subLoading = MutableStateFlow(false)
    val subLoading: StateFlow<Boolean> = _subLoading

    private val _subError = MutableStateFlow<MarketplaceError?>(null)
    val subError: StateFlow<MarketplaceError?> = _subError

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            when (val result = repository.getGroupedCategories()) {
                is ApiResult.Success -> _grouped.value = result.value
                is ApiResult.Error -> _error.value = result.failure.toMarketplaceError()
            }
            _loading.value = false
        }
    }

    /** Grouped жауаптан категорияны id бойынша табу (сабкатегория бетінің атауы). */
    fun findCategory(categoryId: Int): Category? =
        _grouped.value.values.flatten().firstOrNull { it.id == categoryId }

    /** Фаза 19 (Flutter MainCategoryItemView parity): категория басылды. */
    fun trackCategoryView(categoryId: Int, categoryName: String?) =
        monitoringService.trackCategoryView(categoryId.toString(), categoryName)

    fun loadSubcategories(categoryId: Int) {
        viewModelScope.launch {
            _subLoading.value = true
            _subError.value = null
            when (val result = repository.getSubcategories(categoryId)) {
                is ApiResult.Success -> _subcategories.value = result.value
                is ApiResult.Error -> _subError.value = result.failure.toMarketplaceError()
            }
            _subLoading.value = false
        }
    }
}