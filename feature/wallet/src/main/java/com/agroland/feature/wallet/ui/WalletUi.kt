package com.agroland.feature.wallet.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.network.error.Failure

/**
 * UI-ға берілетін әмиян қатесі: backend адам тіліндегі message, болмаса
 * локализацияланған generic мәтін. Шикі error_code ешқашан көрсетілмейді.
 */
data class WalletError(
    val backendMessage: String? = null,
    val isNetwork: Boolean = false,
)

fun WalletError.displayText(networkMessage: String, genericMessage: String): String =
    backendMessage ?: if (isNetwork) networkMessage else genericMessage

/** Қате мәтіні: backend хабарламасы > локализацияланған fallback. */
@Composable
fun WalletError.displayText(): String = displayText(
    networkMessage = stringResource(L10nR.string.error_no_internet),
    genericMessage = stringResource(L10nR.string.error_generic_message),
)

fun Failure.toWalletError(): WalletError = when (this) {
    is Failure.Network -> WalletError(isNetwork = true)
    else -> WalletError(backendMessage = (this as? Failure.Http)?.error?.message)
}

/** Әмиян оқиғалары — топтық түрде біріктірілген. */
sealed interface WalletEvent {
    /** Шығару сұрауы қабылданды — сәттілік диалогы + тізімдер жаңартылады. */
    data object WithdrawDone : WalletEvent
    /** Баланс толтыру HTML формасы дайын — WebView-қа ашылады. */
    data class TopUpHtmlReady(val html: String) : WalletEvent
    data class ShowError(val error: WalletError) : WalletEvent
}