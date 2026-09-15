package com.agroland.app.navigation

import com.agroland.feature.marketplace.data.AnnouncementFilter
import com.agroland.feature.profile.ui.CompanySection
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

// ---- Профиль (Phase 4) ----

/** Профильді өңдеу (avatar + аты + email). */
@Serializable
data object EditProfileRoute

/** Мекенжайлар (CRUD). */
@Serializable
data object ProfileAddressesRoute

/** Компания параметрлері хабы. */
@Serializable
data object CompanySettingsRoute

/** Компания саб-беті (representative|about|contacts|decor). */
@Serializable
data class CompanySectionRoute(val section: CompanySection)

/** KYC верификация. */
@Serializable
data object VerificationRoute

/** Бизнес шарттар — standalone режим (403 DEALER_TERMS_NOT_ACCEPTED). */
@Serializable
data object DealerTermsRoute

// ---- Маркетплейс (Phase 5) ----

/** Сүзгіленген жарнама лентасы (сүзгі route параметрі). */
@Serializable
data class AnnouncementsListRoute(val filter: AnnouncementFilter)

/** Жарнама деталы (id). */
@Serializable
data class AnnouncementDetailRoute(val id: Long)

/** Категориялар — 6 bucket. */
@Serializable
data object CategoriesRoute

/** Категория сабкатегориялары. */
@Serializable
data class SubcategoriesRoute(val categoryId: Int)

/** Таңдаулылар. */
@Serializable
data object FavoritesRoute

/** Сүзгі редакторы — нәтиже caller-дің savedStateHandle-ына қайтады. */
@Serializable
data class FilterRoute(val filter: AnnouncementFilter)