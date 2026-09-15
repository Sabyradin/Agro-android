package com.agroland.feature.marketplace.domain

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * FavoriteSync — глобалдың изабранның жергілікті күйі (Flutter FavoritesNotifier үлгісі).
 * Деталь бетіндегі toggle бүкіл приложение бойынша бірден көрінеді: әр экран өз
 * тізіміндегі isFavorite мәнін осы override-пен қосып көрсетеді.
 */
@Singleton
class FavoriteSync @Inject constructor() {

    private val _overrides = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val overrides: StateFlow<Map<Long, Boolean>> = _overrides

    /** Сәтті toggle/backend-тен келген нақты мән. */
    fun apply(id: Long, favorite: Boolean) {
        _overrides.update { it + (id to favorite) }
    }

    fun applyAll(fresh: Map<Long, Boolean>) {
        _overrides.update { it + fresh }
    }
}