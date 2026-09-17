package com.agroland.feature.cart.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Себеттегі бір жазба — жарнама бойынша itemId мен саны. */
data class CartEntry(val itemId: Long, val quantity: Double)

/**
 * Себеттің процесс бойындағы ортақ күйі: announcementId → [CartEntry].
 *
 * Лента карточкасы себетте бар тауарды «+ / −» басқарғышымен көрсету үшін
 * себет мазмұнын білуі керек, бірақ әр карточка сұрау жібере алмайды —
 * сондықтан күй бір жерде сақталып, [CartRepository] оны жаңартып отырады.
 * Оптимистік өзгерістер бірден көрінеді, сервер қатесінде қайтарылады.
 */
@Singleton
class CartStateHolder @Inject constructor() {

    private val _entries = MutableStateFlow<Map<Long, CartEntry>>(emptyMap())
    val entries: StateFlow<Map<Long, CartEntry>> = _entries

    /** Себеттегі тауарлар саны — төменгі панельдің белгісі үшін. */
    val itemCount: Int get() = _entries.value.size

    fun replaceAll(items: List<CartItem>) {
        _entries.value = items.associate { it.announcementId to CartEntry(it.id, it.quantity) }
    }

    fun put(announcementId: Long, entry: CartEntry) {
        _entries.value = _entries.value + (announcementId to entry)
    }

    fun setQuantity(announcementId: Long, quantity: Double) {
        val current = _entries.value[announcementId] ?: return
        _entries.value = _entries.value + (announcementId to current.copy(quantity = quantity))
    }

    /** Себет бетінен өзгерген сан — лента карточкасына да көрінуі керек. */
    fun setQuantityByItemId(itemId: Long, quantity: Double) {
        val key = _entries.value.entries.firstOrNull { it.value.itemId == itemId }?.key ?: return
        _entries.value = _entries.value + (key to CartEntry(itemId, quantity))
    }

    fun remove(announcementId: Long) {
        _entries.value = _entries.value - announcementId
    }

    fun removeByItemId(itemId: Long) {
        _entries.value = _entries.value.filterValues { it.itemId != itemId }
    }

    fun clear() {
        _entries.value = emptyMap()
    }
}
