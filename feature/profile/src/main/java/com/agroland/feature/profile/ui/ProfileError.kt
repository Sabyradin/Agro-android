package com.agroland.feature.profile.ui

import com.agroland.core.network.error.Failure

/**
 * UI-ға берілетін қате: backend адам тіліндегі message, болмаса UI локализацияланған
 * generic мәтінді таңдайды. Шикі error_code ешқашан көрсетілмейді.
 */
data class ProfileError(
    val backendMessage: String? = null,
    val isNetwork: Boolean = false,
)

fun Failure.toProfileError(): ProfileError = when (this) {
    is Failure.Network -> ProfileError(isNetwork = true)
    else -> ProfileError(backendMessage = (this as? Failure.Http)?.error?.message)
}