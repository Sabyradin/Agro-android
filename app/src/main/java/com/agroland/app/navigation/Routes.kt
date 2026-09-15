package com.agroland.app.navigation

import kotlinx.serialization.Serializable

/** Type-safe навигация маршруттары (Navigation Compose @Serializable). */
object Routes {
    const val SPLASH = "splash"
    const val LANGUAGE = "language"
    const val MAIN = "main"
}

@Serializable
data object SplashRoute

@Serializable
data object LanguageRoute

@Serializable
data object MainShellRoute

/** Auth ағыны (телефон → OTP → тіркелу). */
@Serializable
data object AuthRoute

/** PIN орнату (auth сәтті аяқталғаннан кейін ұсынылады). */
@Serializable
data object PinSetupRoute