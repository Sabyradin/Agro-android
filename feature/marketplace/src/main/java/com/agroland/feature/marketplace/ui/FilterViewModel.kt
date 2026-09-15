package com.agroland.feature.marketplace.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.marketplace.data.AnnouncementFilter
import com.agroland.feature.marketplace.data.MarketplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * FilterViewModel — FilterPage-тің тікелей эфирдегі нәтиже саны (Flutter FilterPage):
 * draft өзгерген сайын 500ms debounce-пен total сұралады, last-wins requestId.
 */
@HiltViewModel
class FilterViewModel @Inject constructor(
    private val repository: MarketplaceRepository,
) : ViewModel() {

    private val _count = MutableStateFlow<Int?>(null)
    val count: StateFlow<Int?> = _count

    private val _countLoading = MutableStateFlow(false)
    val countLoading: StateFlow<Boolean> = _countLoading

    private var job: Job? = null
    private var requestId = 0

    /** Draft сүзгі бойынша сәйкес жарнамалар саны (limit=1 — тек total керек). */
    fun updateCount(filter: AnnouncementFilter) {
        job?.cancel()
        job = viewModelScope.launch {
            delay(COUNT_DEBOUNCE_MS)
            val id = ++requestId
            _countLoading.value = true
            when (val result = repository.getAnnouncements(filter, page = 1, limit = 1)) {
                is ApiResult.Success -> if (id == requestId) _count.value = result.value.total
                is ApiResult.Error -> if (id == requestId) _count.value = null
            }
            if (id == requestId) _countLoading.value = false
        }
    }

    private companion object {
        const val COUNT_DEBOUNCE_MS = 500L
    }
}