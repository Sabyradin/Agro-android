package com.agroland.feature.marketplace.ui

import com.agroland.core.network.error.Failure

/**
 * UI-ға берілетін қате: backend адам тіліндегі message, болмаса UI локализацияланған
 * generic мәтінді таңдайды. Шикі error_code ешқашан көрсетілмейді.
 */
data class MarketplaceError(
    val backendMessage: String? = null,
    val isNetwork: Boolean = false,
)

/** Marketplace экрандарының бір реттелген оқиғалары. */
sealed interface MarketplaceEvent {
    data class ShowError(val error: MarketplaceError) : MarketplaceEvent
    data object FavoriteFailed : MarketplaceEvent
}

/**
 * Қате мәтіні: backend хабарламасы > берілген локализацияланған fallback.
 * stringResource-ты Composable контексттен алдын ала оқып, осы функцияға беріңіз.
 */
fun MarketplaceError.displayText(networkMessage: String, genericMessage: String): String =
    backendMessage ?: if (isNetwork) networkMessage else genericMessage

fun Failure.toMarketplaceError(): MarketplaceError = when (this) {
    is Failure.Network -> MarketplaceError(isNetwork = true)
    else -> MarketplaceError(backendMessage = (this as? Failure.Http)?.error?.message)
}