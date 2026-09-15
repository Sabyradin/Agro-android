package com.agroland.feature.payment.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.network.error.Failure
import com.agroland.feature.payment.data.BalancePaymentResult
import com.agroland.feature.payment.data.HalykPaymentInit

/**
 * UI-ға берілетін төлем қатесі: backend адам тіліндегі message, болмаса
 * локализацияланған generic мәтін. Шикі error_code ешқашан көрсетілмейді.
 */
data class PaymentError(
    val backendMessage: String? = null,
    val isNetwork: Boolean = false,
)

fun PaymentError.displayText(networkMessage: String, genericMessage: String): String =
    backendMessage ?: if (isNetwork) networkMessage else genericMessage

/** Қате мәтіні: backend хабарламасы > локализацияланған fallback. */
@Composable
fun PaymentError.displayText(): String = displayText(
    networkMessage = stringResource(L10nR.string.error_no_internet),
    genericMessage = stringResource(L10nR.string.error_generic_message),
)

fun Failure.toPaymentError(): PaymentError = when (this) {
    is Failure.Network -> PaymentError(isNetwork = true)
    else -> PaymentError(backendMessage = (this as? Failure.Http)?.error?.message)
}

/** Halyk төлемін ашу жүктемесі — MainActivity WebView/нәтижше бетіне айналдырады. */
data class HalykLaunch(
    val paymentUrl: String,
    val orderId: Long,
    /** DEV mock — WebView-сіз тікелей нәтижше бетіне (backend төленді деп белгілейді). */
    val mock: Boolean,
)

/** CheckoutPaymentViewModel-нің бір реттелген оқиғалары. */
sealed interface PaymentEvent {
    data class BalancePaid(val result: BalancePaymentResult) : PaymentEvent
    data class HalykReady(val result: HalykPaymentInit, val orderId: Long) : PaymentEvent
    data class ShowError(val error: PaymentError) : PaymentEvent
}