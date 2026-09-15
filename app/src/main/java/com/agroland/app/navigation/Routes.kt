package com.agroland.app.navigation

import com.agroland.feature.location.data.SelectedLocation
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

// ---- Маркетплейс: жазу режимі (Phase 6) ----

/** «+» табының таңдау экраны: жарнама / сұраныс / топтап жүктеу. */
@Serializable
data object CreateOrOfferRoute

/** Жарнама құру (multipart images+video, AI мазмұн, preview). */
@Serializable
data object CreateAdRoute

/** Жарнаманы өңдеу (FullAnnouncement prefill). */
@Serializable
data class EditAdRoute(val id: Long)

/** Сұраныс құру — POST /demands (MakeOffer). */
@Serializable
data object MakeOfferRoute

/** Топтап жүктеу (.xlsx/.csv шаблон + upload). */
@Serializable
data object BulkUploadRoute

/** Менің жарнамаларым — статус беттері (active|pending|inactive|rejected). */
@Serializable
data class MyAnnouncementsRoute(val status: String = "active")

/** Өз жарнамасының деталы + ие әрекеттері. */
@Serializable
data class ProfileAnnouncementRoute(val id: Long)

// ---- Локация (Phase 7) ----

/** Бірінші іске қосу: ел таңдау (app-region орнату ағыны). */
@Serializable
data object CountryListRoute

/** Бірінші іске қосу: елдің өңірлерін таңдау. */
@Serializable
data class RegionListRoute(val countryId: Int, val countryName: String = "")

/** Мекенжай қосу (locationId = 0) / өңдеу. */
@Serializable
data class AddressEditRoute(val locationId: Long = 0L)

/** Карта арқылы локация таңдау — нәтиже caller-дің savedStateHandle-ына қайтады. */
@Serializable
data class LocationSelectionRoute(val prefill: SelectedLocation? = null)

// ---- Себет / тапсырыстар (Phase 8) ----

/** Тапсырыс деталы (себет/чекаут/buy-now жасаған тапсырыстар). */
@Serializable
data class OrderDetailRoute(val orderId: Long)

// ---- Төлемдер (Phase 9) ----

/** Halyk ePay нәтижесі — поллинг 2с/60с, WebView-тан қайтқанда ашылады. */
@Serializable
data class PaymentResultRoute(val orderId: Long)

/**
 * WebView: Halyk payment_url немесе BCC legacy 3D Secure HTML формасы.
 * exitRedirectUrl-ға (agroland.kz) жеткенде/артқа шыққанда жабылады;
 * paymentResultOrderId толтырылса — жабылғаннан кейін нәтижше бетіне өтеді.
 */
@Serializable
data class WebViewRoute(
    val url: String = "",
    val html: String? = null,
    val title: String? = null,
    val exitRedirectUrl: String? = null,
    val paymentResultOrderId: Long? = null,
)