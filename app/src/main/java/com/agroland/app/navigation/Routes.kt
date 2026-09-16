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

// ---- Әмиян (Phase 10) ----

/** Әмиян: баланс (қолжетімді/күтуде/ұсталған) + ledger (Flutter BalancePage). */
@Serializable
data object BalanceRoute

/** Қаражат шығару — қолжетімді баланс query параметрімен (Flutter withdraw). */
@Serializable
data class WithdrawRoute(val availableBalance: Double = 0.0)

/** Транзакциялар тарихы — профиль мәзірі ашады. */
@Serializable
data object TransactionHistoryRoute

/** Әмиянды толтыру — BCC 3DS HTML формасы WebView-қа ашылады. */
@Serializable
data object TopUpRoute
// ---- Чат (Phase 12) ----

/**
 * Чат бөлмесі. Тізімнен ашылғанда — roomId + otherUserId + username;
 * жарнамадан басталған сұхбат — announcementId + deliveryAddress;
 * push-тан — senderId арқылы басқарылатын межелер.
 */
@Serializable
data class ChatRoomRoute(
    val roomId: Long? = null,
    val otherUserId: Long? = null,
    val username: String? = null,
    val announcementId: Long? = null,
    val deliveryAddress: String? = null,
    val isSystemChat: Boolean = false,
    val roomAnnouncementId: Long? = null,
)

/** Мұрағатқа шығарылған чаттар (Flutter ArchivedChatsPage). */
@Serializable
data object ArchivedChatsRoute

// ---- Хабарламалар (Phase 12) ----

/** Хабарламалар хабы — үш бөлім (service/support/promotions). */
@Serializable
data object NotificationsRoute

/** Бөлім хабарламалары тізімі (type = service|support|promotions). */
@Serializable
data class NotificationsByTypeRoute(val type: String)

/** Толық хабарлама — модель навигация арқылы беріледі (Flutter model query). */
@Serializable
data class SingleNotificationRoute(
    val item: com.agroland.feature.notifications.data.NotificationItem,
    val type: String,
)

// ---- Промо v2 (Phase 15) ----

/** Өз жарнамасын жылжыту — промо v2 каталогы + ағымдағы промо күйі. */
@Serializable
data class AdvertiseAdRoute(val announcementId: Long)

/** Өз жарнамаларының промолары — GET /user/announcements/promotions. */
@Serializable
data object PromotedAnnouncementsRoute

/** «Жылдам жарнамалар» — type_ad=vip лентасы (Flutter HotAnnouncementsPage). */
@Serializable
data object HotAnnouncementsRoute

// ═══════════════ Фаза 16 — Дилер консолі ═══════════════

/** Дилер өнімдері хабы — 4 таб (products/logistics/promotion/analytics). */
@Serializable
data class DealerProductsRoute(val initialTab: Int = 0)

/** Дилер тапсырыстары — 5 таб (new/confirmed/in_transit/delivered/cancelled). */
@Serializable
data class DealerOrdersRoute(val initialTab: Int = 0)

/** Тапсырыстар пулы — бөлінбеген тапсырыстар + claim (manager|director). */
@Serializable
data object TeamPoolRoute

/** Қызметкертерді басқару — director ғана. */
@Serializable
data object DealerEmployeesRoute

/** Тапсырыс трекингі — GET /orders/{id}/tracking. */
@Serializable
data class OrderTrackingRoute(val orderId: Long)

/** Жеткізу зонасын қосу/өңдеу (-1 = жаңа). */
@Serializable
data class AddEditDeliveryZoneRoute(val zoneId: Long = -1)

/** Бизнес баптаулары — баланс + ҚҚС төлеуші тумблері. */
@Serializable
data object DealerSettingsRoute

// ═══════════════ Фаза 17 — Қытай (MercuryX) / EGOV / Сұраныстар ═══════════════

/** Қытай категориясының сабкатегориялары (parentId — savedStateHandle кілті). */
@Serializable
data class ChinaSubcategoriesRoute(val parentId: Long, val title: String = "")

/** Қытай категория тауарлары — парақталған тор (categoryId). */
@Serializable
data class ChinaProductsRoute(val categoryId: Long, val title: String = "")

/** Қытай тауарының деталы (productId — MercuryX int64 id). */
@Serializable
data class ChinaProductDetailRoute(val productId: Long)

/** Egov сервистері — статикалық каталог + VIN іздестіру. */
@Serializable
data object EgovServicesRoute

/** «Сұраныстарым» — пайдаланушының сұраныстары (спек қосымшасы, Flutter-де жоқ). */
@Serializable
data object DemandListRoute

/** Сұраныс деталы (demandId). */
@Serializable
data class DemandDetailRoute(val demandId: Long)

/** Сұраныс құру/өңдеу (demandId = -1 → жаңа). */
@Serializable
data class CreateEditDemandRoute(val demandId: Long = -1L)

/** Профиль беті — Home аватары ашады (SERVICES қойындысы Сервистерге ауысты). */
@Serializable
data object ProfileRoute

// ═══════════════ Фаза 18 — Пікірлер / Медиа / QR ═══════════════

/** Жарнама пікірлері — толық тізім (деталь бетінің превьюінен ашылады). */
@Serializable
data class AnnouncementReviewsRoute(val announcementId: Long)

/** Пікір қалдыру (жұлдыздар + мәтін ≤1000). */
@Serializable
data class SendReviewRoute(val announcementId: Long)

/** GET /user/{id}/reviews — жарнама бойынша пікір жиынтықтары. */
@Serializable
data class ProfileReviewsRoute(val userId: Long)

/** GET /seller/{id}/reviews — сатушы пікірлері + like. */
@Serializable
data class SellerReviewsRoute(val userId: Long)

/** «Менің пікірлерім» — клиент жағынан агрегация, 2 қойынды (spec §10). */
@Serializable
data object MyReviewsRoute

/** Суреттер галереясы — pinch zoom + парақтау (деталь/чат/тапсырыс). */
@Serializable
data class PhotoViewerRoute(val images: List<String>, val initialIndex: Int = 0)

/** Бейне көрсеткі — ExoPlayer (mp4/әмбебап). */
@Serializable
data class VideoViewerRoute(val url: String, val title: String? = null)

/** PDF көрсеткі — ішінде рендер (ISSUES #27). */
@Serializable
data class PdfViewerRoute(val url: String)

/** YouTube IFrame көрсеткі — videoId шықпаса VideoViewer fallback. */
@Serializable
data class YouTubeViewerRoute(val url: String)

/** QR сканер — Home іздеу жолағынан ашылады. */
@Serializable
data object QrScannerRoute
