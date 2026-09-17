package com.agroland.feature.cart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.cart.data.CartEntry
import com.agroland.feature.cart.data.CartRepository
import com.agroland.feature.cart.data.CartStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Лентадан бір басумен себетке салу («Себетке» батырмасы) және одан кейінгі
 * «+ / −» басқарғышы.
 *
 * [BuyBarViewModel] толық [com.agroland.feature.marketplace.data.FullAnnouncement]
 * талап етеді (деталь беті), ал лентада тек қысқа модель бар — сондықтан
 * бұл жеңіл ViewModel: id + өлшем бірлігі, бастапқы саны 1 (Flutter parity).
 * Саны себеттің ортақ күйінен ([CartStateHolder]) оқылады, сондықтан лента
 * мен себет беті әрқашан бір мәнді көрсетеді.
 */
@HiltViewModel
class QuickCartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    cartState: CartStateHolder,
) : ViewModel() {

    sealed interface Event {
        data object Added : Event
        data class Failed(val error: CartError) : Event
    }

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    /** announcementId → себеттегі жазба; карточка осыдан санын алады. */
    val entries: StateFlow<Map<Long, CartEntry>> = cartState.entries

    /** Авторизацияланған қолданушыда лента ашылғанда себет күйін бір рет оқимыз. */
    fun sync() {
        viewModelScope.launch { cartRepository.syncCartState() }
    }

    fun add(announcementId: Long, measurementUnit: String?) {
        viewModelScope.launch {
            when (val result = cartRepository.addToCart(announcementId, 1.0, measurementUnit)) {
                is ApiResult.Success -> _events.emit(Event.Added)
                is ApiResult.Error -> _events.emit(Event.Failed(result.failure.toCartError()))
            }
        }
    }

    /**
     * «+ / −». Саны 1-ден төмен түссе тауар себеттен мүлдем алынады —
     * карточка қайтадан «Себетке» батырмасын көрсетеді.
     */
    fun changeQuantity(announcementId: Long, delta: Double) {
        val entry = entries.value[announcementId] ?: return
        val target = entry.quantity + delta
        viewModelScope.launch {
            val result = if (target < 1.0) {
                cartRepository.deleteCartItem(entry.itemId)
            } else {
                cartRepository.updateQuantity(entry.itemId, target)
            }
            if (result is ApiResult.Error) _events.emit(Event.Failed(result.failure.toCartError()))
        }
    }
}
