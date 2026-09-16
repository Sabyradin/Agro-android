package com.agroland.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.agroland.app.navigation.AddressEditRoute
import com.agroland.app.navigation.AnnouncementFilterTypeMap
import com.agroland.app.navigation.AnnouncementDetailRoute
import com.agroland.app.navigation.AnnouncementsListRoute
import com.agroland.app.navigation.AuthRoute
import com.agroland.app.navigation.BulkUploadRoute
import com.agroland.app.navigation.CategoriesRoute
import com.agroland.app.navigation.CompanySectionRoute
import com.agroland.app.navigation.CompanySettingsRoute
import com.agroland.app.navigation.CreateAdRoute
import com.agroland.app.navigation.CreateOrOfferRoute
import com.agroland.app.navigation.CountryListRoute
import com.agroland.app.navigation.DealerTermsRoute
import com.agroland.app.navigation.EditAdRoute
import com.agroland.app.navigation.EditProfileRoute
import com.agroland.app.navigation.FavoritesRoute
import com.agroland.app.navigation.FilterRoute
import com.agroland.app.navigation.LanguageRoute
import com.agroland.app.navigation.LocationSelectionRoute
import com.agroland.app.navigation.MainShellRoute
import com.agroland.app.navigation.MakeOfferRoute
import com.agroland.app.navigation.MyAnnouncementsRoute
import com.agroland.app.navigation.ArchivedChatsRoute
import com.agroland.app.navigation.ChatRoomRoute
import com.agroland.app.navigation.NotificationsByTypeRoute
import com.agroland.app.navigation.NotificationsRoute
import com.agroland.app.navigation.PinSetupRoute
import com.agroland.app.navigation.SingleNotificationRoute
import com.agroland.app.navigation.ProfileAddressesRoute
import com.agroland.app.navigation.ProfileAnnouncementRoute
import com.agroland.app.navigation.RegionListRoute
import com.agroland.app.navigation.SplashRoute
import com.agroland.app.navigation.SubcategoriesRoute
import com.agroland.app.navigation.VerificationRoute
import com.agroland.app.navigation.OrderDetailRoute
import com.agroland.app.navigation.PaymentResultRoute
import com.agroland.app.navigation.WebViewRoute
import com.agroland.app.navigation.BalanceRoute
import com.agroland.app.navigation.TransactionHistoryRoute
import com.agroland.app.navigation.TopUpRoute
import com.agroland.app.navigation.WithdrawRoute
import com.agroland.app.navigation.AdvertiseAdRoute
import com.agroland.app.navigation.PromotedAnnouncementsRoute
import com.agroland.app.navigation.HotAnnouncementsRoute
import com.agroland.app.navigation.DealerProductsRoute
import com.agroland.app.navigation.DealerOrdersRoute
import com.agroland.app.navigation.TeamPoolRoute
import com.agroland.app.navigation.DealerEmployeesRoute
import com.agroland.app.navigation.OrderTrackingRoute
import com.agroland.app.navigation.AddEditDeliveryZoneRoute
import com.agroland.app.navigation.DealerSettingsRoute
import com.agroland.app.navigation.ChinaSubcategoriesRoute
import com.agroland.app.navigation.ChinaProductsRoute
import com.agroland.app.navigation.ChinaProductDetailRoute
import com.agroland.app.navigation.EgovServicesRoute
import com.agroland.app.navigation.DemandListRoute
import com.agroland.app.navigation.DemandDetailRoute
import com.agroland.app.navigation.CreateEditDemandRoute
import com.agroland.app.navigation.ProfileRoute
import com.agroland.app.navigation.AnnouncementReviewsRoute
import com.agroland.app.navigation.SendReviewRoute
import com.agroland.app.navigation.ProfileReviewsRoute
import com.agroland.app.navigation.SellerReviewsRoute
import com.agroland.app.navigation.MyReviewsRoute
import com.agroland.app.navigation.PhotoViewerRoute
import com.agroland.app.navigation.VideoViewerRoute
import com.agroland.app.navigation.PdfViewerRoute
import com.agroland.app.navigation.YouTubeViewerRoute
import com.agroland.app.navigation.QrScannerRoute
import com.agroland.core.common.settings.ThemeMode
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.theme.AgroTheme
import com.agroland.feature.auth.session.SessionState
import com.agroland.feature.auth.ui.AppLockGate
import com.agroland.feature.auth.ui.AuthFlowPage
import com.agroland.feature.auth.ui.PinSetupPage
import com.agroland.feature.cart.ui.CartPage
import com.agroland.feature.cart.ui.DetailBuyBar
import com.agroland.feature.cart.ui.GuestCartTab
import com.agroland.feature.chat.domain.ChatSocketService
import com.agroland.feature.chat.ui.ArchivedChatsPage
import com.agroland.feature.chat.ui.ChatListPage
import com.agroland.feature.chat.ui.ChatRoomPage
import com.agroland.feature.chat.ui.GuestChatTab
import com.agroland.feature.call.domain.CallPhase
import com.agroland.feature.call.domain.VoiceCallManager
import com.agroland.feature.call.ui.VoiceCallScreen
import com.agroland.feature.cart.ui.OrderDetailPage
import com.agroland.feature.location.data.LOCATION_RESULT_KEY
import com.agroland.feature.location.data.SelectedLocation
import com.agroland.feature.location.ui.CountryListPage
import com.agroland.feature.location.ui.LocationSelectionPage
import com.agroland.feature.location.ui.RegionListPage
import com.agroland.feature.marketplace.data.AnnouncementFilter
import com.agroland.feature.marketplace.ui.AnnouncementDetailPage
import com.agroland.feature.marketplace.ui.AnnouncementsListPage
import com.agroland.feature.marketplace.ui.BulkUploadPage
import com.agroland.feature.marketplace.ui.CategoriesPage
import com.agroland.feature.marketplace.ui.CreateAdPage
import com.agroland.feature.marketplace.ui.CreateOrOfferPage
import com.agroland.feature.marketplace.ui.FavoritesPage
import com.agroland.feature.marketplace.ui.FilterPage
import com.agroland.feature.marketplace.ui.HomeFeedPage
import com.agroland.feature.marketplace.ui.MakeOfferPage
import com.agroland.feature.marketplace.ui.MyAnnouncementsPage
import com.agroland.feature.marketplace.ui.OwnerAnnouncementPage
import com.agroland.feature.marketplace.ui.SubcategoriesPage
import com.agroland.feature.promo.ui.AdvertiseAdPage
import com.agroland.feature.promo.ui.PromotedAnnouncementsPage
import com.agroland.feature.payment.ui.HalykLaunch
import com.agroland.feature.payment.ui.PaymentResultPage
import com.agroland.feature.payment.ui.WebViewPage
import com.agroland.feature.notifications.data.NotificationType
import com.agroland.feature.notifications.ui.NotificationsByTypePage
import com.agroland.feature.notifications.ui.NotificationsPage
import com.agroland.feature.notifications.ui.SingleNotificationPage
import com.agroland.feature.push.domain.PushController
import com.agroland.feature.push.domain.PushDestination
import com.agroland.feature.push.service.PushNotificationShower
import com.agroland.feature.wallet.ui.BalancePage
import com.agroland.feature.wallet.ui.TopUpPage
import com.agroland.feature.wallet.ui.TransactionHistoryPage
import com.agroland.feature.wallet.ui.WithdrawPage
import com.agroland.feature.profile.ui.AddressEditPage
import com.agroland.feature.profile.ui.CompanySection
import com.agroland.feature.profile.ui.CompanySectionPage
import com.agroland.feature.profile.ui.CompanySettingsPage
import com.agroland.feature.profile.ui.DealerTermsPage
import com.agroland.feature.profile.ui.EditProfilePage
import com.agroland.feature.profile.ui.ProfileAddressesPage
import com.agroland.feature.profile.ui.ProfilePage
import com.agroland.feature.profile.ui.VerificationPage
import com.agroland.feature.dealer.ui.AddEditDeliveryZonePage
import com.agroland.feature.dealer.ui.DealerEmployeesPage
import com.agroland.feature.dealer.ui.DealerOrdersPage
import com.agroland.feature.dealer.ui.DealerProductsPage
import com.agroland.feature.dealer.ui.DealerSettingsPage
import com.agroland.feature.dealer.ui.OrderTrackingPage
import com.agroland.feature.dealer.ui.TeamPoolPage
import com.agroland.feature.shell.ShellViewModel
import com.agroland.feature.shell.language.LanguagePage
import com.agroland.feature.shell.main.MainShellPage
import com.agroland.feature.shell.splash.SplashPage
import com.agroland.feature.shell.splash.SplashTarget
import com.agroland.feature.china.ui.ChinaCatalogContent
import com.agroland.feature.china.ui.ChinaProductDetailPage
import com.agroland.feature.china.ui.ChinaProductsPage
import com.agroland.feature.china.ui.ChinaSubcategoriesPage
import com.agroland.feature.demand.ui.CreateEditDemandPage
import com.agroland.feature.demand.ui.DemandDetailPage
import com.agroland.feature.demand.ui.DemandListPage
import com.agroland.feature.services.ui.EgovServicesPage
import com.agroland.feature.services.ui.ServicesPage
import com.agroland.feature.reviews.ui.AnnouncementReviewsPage
import com.agroland.feature.reviews.ui.MyReviewsPage
import com.agroland.feature.reviews.ui.MyReviewsViewModel
import com.agroland.feature.reviews.ui.ProfileReviewsPage
import com.agroland.feature.reviews.ui.SellerReviewsPage
import com.agroland.feature.reviews.ui.SendReviewPage
import com.agroland.feature.media.ui.PhotoViewerPage
import com.agroland.feature.media.ui.PdfViewerPage
import com.agroland.feature.media.ui.QrScannerPage
import com.agroland.feature.media.ui.VideoViewerPage
import com.agroland.feature.media.ui.YouTubeViewerPage
import com.agroland.feature.stories.domain.StoryViewerStateHolder
import com.agroland.feature.stories.ui.StoryViewerScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var pushController: PushController

    @Inject
    lateinit var chatSocketService: ChatSocketService

    /** Фаза 13: WebRTC дауыстық қоңырау state machine-і (singleton overlay). */
    @Inject
    lateinit var voiceCallManager: VoiceCallManager

    /** Фаза 14: толықэкран сторис-viewer сессиясы (singleton overlay). */
    @Inject
    lateinit var storyViewerStateHolder: StoryViewerStateHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Push хабарламасынан ашылғанда (өлі күй реплейі де осы арқылы):
        // payload pending межеге сақталып, сессия дайын болғанда навигацияланады.
        handlePushIntent(intent)
        setContent {
            val shellViewModel: ShellViewModel = hiltViewModel()
            val appViewModel: AppViewModel = hiltViewModel()
            val themeMode by shellViewModel.themeMode.collectAsState()
            val localeTag by shellViewModel.localeTag.collectAsState()
            val session by appViewModel.session.collectAsState()

            // Push (Фаза 11): POST_NOTIFICATIONS — API 33+ рұқсат сұрауы
            // (берілмесе хабарлама көрінбейді, тіркеу жалғасады).
            val notifPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // Push тіркеу/өшіру (Flutter root_page auth-listener):
            // Authorized → POST /device (тек токен жаңа болса), Guest → DELETE.
            LaunchedEffect(session) {
                when (session) {
                    SessionState.Authorized -> {
                        pushController.ensureRegistered(localeTag ?: "kk")
                        // Socket.IO чат қосылымы (Flutter ChatSocketService.initialize).
                        chatSocketService.start()
                        // Дауыстық қоңырау listener-лері сол socket-та (Фаза 13).
                        voiceCallManager.start()
                    }
                    SessionState.Guest -> {
                        pushController.unregister()
                        chatSocketService.stop()
                        voiceCallManager.stop()
                    }
                    SessionState.Loading -> Unit
                }
            }

            // Тіл өзгерсе — кэштелген токенмен қайта тіркеу (push локализациясы).
            LaunchedEffect(localeTag) {
                localeTag?.let { pushController.onLanguageChanged(it) }
            }

            // Пер-апп локальдар (API 33-тен төменде де жұмыс істейді).
            val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (localeTag != null && currentTags != localeTag) {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(localeTag),
                )
            }

            // App-lock: құлыптаулы сессия + PIN орнатылған → кіру қақпасы.
            var locked by remember { mutableStateOf(false) }
            LaunchedEffect(session) {
                locked = session == SessionState.Authorized && appViewModel.pinIsSet
            }

            AgroTheme(
                darkTheme = themeMode == ThemeMode.DARK ||
                    (themeMode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme()),
            ) {
                Box(Modifier.fillMaxSize()) {
                    if (locked) {
                        AppLockGate(
                            pinManager = appViewModel.pinManager,
                            activity = this@MainActivity,
                            biometricAuthenticator = appViewModel.biometricAuthenticator,
                            onUnlocked = { locked = false },
                        )
                    } else {
                        AppNavHost(
                            isAuthorized = session == SessionState.Authorized,
                            themeMode = themeMode,
                            localeTag = localeTag,
                            appViewModel = appViewModel,
                            pushController = pushController,
                            chatSocketService = chatSocketService,
                            voiceCallManager = voiceCallManager,
                            onThemeChange = { shellViewModel.setThemeMode(it) },
                            onAppRegionSelected = { countryId, regionId ->
                                shellViewModel.setAppRegion(countryId, regionId)
                            },
                        )
                    }

                    // Фаза 13: қоңырау overlay-і (Flutter VoiceCallHost) —
                    // root үстінде, құлыптаулы күйде де (қоңырау қабылдауға
                    // болады, PIN экраны одан кейін түседі).
                    val callState by voiceCallManager.state.collectAsState()
                    if (callState.phase != CallPhase.IDLE || callState.terminalReason != null) {
                        VoiceCallScreen(
                            state = callState,
                            onAccept = { micGranted ->
                                voiceCallManager.accept(micGranted)
                            },
                            onDecline = { voiceCallManager.reject() },
                            onHangup = { voiceCallManager.hangup() },
                            onToggleMute = { voiceCallManager.toggleMute() },
                            onToggleSpeaker = { voiceCallManager.toggleSpeaker() },
                            onClearTerminal = { voiceCallManager.clearTerminal() },
                        )
                    }

                    // Фаза 14: сторис-viewer overlay-і (Flutter BannerStoryViewerPage
                    // + storyViewerOpenProvider — navbar табиғи жасырылады).
                    val viewerSession by storyViewerStateHolder.session.collectAsState()
                    if (viewerSession != null) {
                        StoryViewerScreen(holder = storyViewerStateHolder)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Push хабарламасы қосымша АШЫҚ күйінде басылды (Flutter onActionReceivedMethod).
        handlePushIntent(intent)
    }

    /**
     * Intent extras-тан push payload шығарады (екі дереккөз: біздің PendingIntent
     * маркермен; FCM өзі көрсеткен хабарлама — google.* кілттері жанындағы data).
     * Оқылған extras тазартылады — config change кезінде қайта ойналмайды.
     */
    private fun handlePushIntent(intent: Intent?) {
        if (intent == null) return
        val extras = intent.extras ?: return
        val data = HashMap<String, String>()
        var marked = false
        for (key in extras.keySet()) {
            if (key == PushNotificationShower.EXTRA_PUSH_MARKER) {
                marked = true
                continue
            }
            if (key.startsWith("google.") || key.startsWith("android.") ||
                key.startsWith("gcm.")
            ) {
                continue
            }
            (extras.get(key) as? String)?.let { value -> data[key] = value }
        }
        if (!marked && data.isEmpty()) return
        pushController.handlePushData(data)
        for (key in extras.keySet().toList()) {
            intent.removeExtra(key)
        }
    }
}

@Composable
private fun AppNavHost(
    isAuthorized: Boolean,
    themeMode: ThemeMode,
    localeTag: String?,
    appViewModel: AppViewModel,
    pushController: PushController,
    chatSocketService: ChatSocketService,
    voiceCallManager: VoiceCallManager,
    onThemeChange: (ThemeMode) -> Unit,
    onAppRegionSelected: (Int, Int) -> Unit,
) {
    val navController = rememberNavController()

    // Чат қойындысының бейджі — Socket.IO totalUnread (Flutter bottom_navbar).
    val chatBadge by chatSocketService.totalUnread.collectAsState()

    // Фаза 18: «Менің пікірлерім» — activity-scoped VM: чат тізімінің бейджі
    // мен MyReviewsPage бір күйді бөліседі (spec §10 клиент агрегациясы).
    val myReviewsViewModel: MyReviewsViewModel = hiltViewModel(
        LocalContext.current as androidx.activity.ComponentActivity,
    )

    // Push межесі (өлі күй реплейі де осыған келеді): сессия дайын болғанда
    // БІР рет навигация жасап, pending күйді тазартамыз.
    val pendingPush by pushController.pendingDestination.collectAsState()
    LaunchedEffect(pendingPush, isAuthorized) {
        val destination = pendingPush ?: return@LaunchedEffect
        if (!isAuthorized) return@LaunchedEffect
        when (destination) {
            is PushDestination.OrderDetail ->
                navController.navigate(OrderDetailRoute(destination.orderId))
            is PushDestination.Announcement ->
                navController.navigate(AnnouncementDetailRoute(destination.announcementId))
            PushDestination.Verification -> navController.navigate(VerificationRoute)
            PushDestination.Balance -> navController.navigate(BalanceRoute)
            is PushDestination.ChatRoom -> navController.navigate(
                ChatRoomRoute(
                    roomId = destination.roomId,
                    otherUserId = destination.senderId,
                    username = destination.senderName,
                    isSystemChat = destination.isSystemChat,
                ),
            )
            is PushDestination.Notifications -> navController.navigate(
                NotificationsByTypeRoute(destination.type ?: "service"),
            )
        }
        pushController.consumePending()
    }

    // Суық старт кезінде үзілген Halyk төлемін қалпына келтіру (бір рет).
    var resumedPendingPayment by remember { mutableStateOf(false) }
    LaunchedEffect(isAuthorized) {
        if (isAuthorized && !resumedPendingPayment) {
            resumedPendingPayment = true
            appViewModel.pendingPaymentOrderId()?.let { orderId ->
                navController.navigate(PaymentResultRoute(orderId))
            }
        }
    }

    // Halyk төлемін ашу: mock — тікелей нәтижше бетіне; әйтпесе WebView,
    // ол жабылғанда нәтижше бетіне өтеді (Flutter payment_method_sheet).
    val paymentResultTitle = stringResource(L10nR.string.payment_result)
    val openHalyk: (HalykLaunch) -> Unit = { launch ->
        if (launch.mock) {
            navController.navigate(PaymentResultRoute(launch.orderId))
        } else {
            navController.navigate(
                WebViewRoute(
                    url = launch.paymentUrl,
                    title = paymentResultTitle,
                    exitRedirectUrl = HALYK_EXIT_HOST,
                    paymentResultOrderId = launch.orderId,
                ),
            )
        }
    }

    // Әмиянды толтыру: BCC 3D Secure HTML формасы WebView-қа ашылады,
    // exit-хостқа (agroland.kz) жеткенде жабылады (Flutter payment_page).
    val topUpTitle = stringResource(L10nR.string.wallet_replenish)
    val openTopUpWebView: (String) -> Unit = { html ->
        navController.navigate(
            WebViewRoute(html = html, title = topUpTitle, exitRedirectUrl = HALYK_EXIT_HOST),
        )
    }

    NavHost(
        navController = navController,
        startDestination = SplashRoute,
    ) {
        composable<SplashRoute> {
            SplashPage(
                onDecided = { target ->
                    val route = when (target) {
                        SplashTarget.Language -> LanguageRoute
                        SplashTarget.RegionSetup -> CountryListRoute
                        SplashTarget.Main -> MainShellRoute
                    }
                    navController.navigate(route) {
                        popUpTo(SplashRoute) { inclusive = true }
                    }
                },
            )
        }
        composable<LanguageRoute> {
            LanguagePage(
                onSelected = {
                    // regionRedirectProvider баламасы: тілден кейін өңір таңдауға
                    // бағыттаймыз (appRegion әлі жоқ — splash Main-ға жібермейді).
                    navController.navigate(CountryListRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable<MainShellRoute> {
            MainShellPage(
                onCreateClick = {
                    if (isAuthorized) {
                        navController.navigate(CreateOrOfferRoute)
                    } else {
                        navController.navigate(AuthRoute)
                    }
                },
                chatBadge = chatBadge,
                homeContent = {
                    HomeFeedPage(
                        onOpenDetail = { navController.navigate(AnnouncementDetailRoute(it)) },
                        onOpenAll = { query ->
                            navController.navigate(
                                AnnouncementsListRoute(
                                    AnnouncementFilter(query = query.takeIf { it.isNotBlank() }),
                                ),
                            )
                        },
                        onOpenFilter = { navController.navigate(FilterRoute(AnnouncementFilter())) },
                        onOpenFavorites = { navController.navigate(FavoritesRoute) },
                        onOpenCategories = { navController.navigate(CategoriesRoute) },
                        onOpenNotifications = { navController.navigate(NotificationsRoute) },
                        // Stories статик промо-карточкалары (Фаза 14/15/17):
                        // құру — CreateAd; жарнама — өз жарнамаларынан таңдау
                        // (Flutter-де контекстсіз advertise модельді талап етеді
                        // — ISSUES #33); көтерілгендер — промо тізімі;
                        // Қытай каталогы — Фаза 17 кезінде.
                        onCreateAnnouncement = {
                            if (isAuthorized) {
                                navController.navigate(CreateAdRoute)
                            } else {
                                navController.navigate(AuthRoute)
                            }
                        },
                        onOpenAdvertise = {
                            if (isAuthorized) {
                                navController.navigate(MyAnnouncementsRoute("active"))
                            } else {
                                navController.navigate(AuthRoute)
                            }
                        },
                        onOpenPromoted = {
                            if (isAuthorized) {
                                navController.navigate(PromotedAnnouncementsRoute)
                            } else {
                                navController.navigate(AuthRoute)
                            }
                        },
                        // Аватар → профил беті (Фаза 17: SERVICES қойындысы
                        // Сервистерге берілді, профиль осында көшті).
                        onOpenProfile = { navController.navigate(ProfileRoute) },
                        // Фаза 18: QR сканер — іздеу жолағындағы иконка.
                        onOpenQrScanner = { navController.navigate(QrScannerRoute) },
                        // CHINA қойындысы — MercuryX каталогы (Фаза 17).
                        chinaContent = {
                            ChinaCatalogContent(
                                onOpenSubcategories = { parentId, title ->
                                    navController.navigate(ChinaSubcategoriesRoute(parentId, title))
                                },
                                onOpenProducts = { categoryId, title ->
                                    navController.navigate(ChinaProductsRoute(categoryId, title))
                                },
                                onOpenProduct = { productId ->
                                    navController.navigate(ChinaProductDetailRoute(productId))
                                },
                            )
                        },
                    )
                },
                chatContent = {
                    if (isAuthorized) {
                        // Чат қойындысы ашылды — pending пікірлерді жаңартамыз
                        // (MyReviewsViewModel.refreshIfNeeded — 30с debounce).
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            myReviewsViewModel.refreshIfNeeded()
                        }
                        val myReviewsBadge by myReviewsViewModel.pendingCount.collectAsState()
                        ChatListPage(
                            onOpenRoom = { room, username, otherUserId, isSystemChat, announcementId ->
                                navController.navigate(
                                    ChatRoomRoute(
                                        roomId = room.roomId,
                                        otherUserId = otherUserId,
                                        username = username,
                                        isSystemChat = isSystemChat,
                                        announcementId = announcementId?.toLongOrNull(),
                                        roomAnnouncementId = room.announcementId,
                                    ),
                                )
                            },
                            onOpenArchived = { navController.navigate(ArchivedChatsRoute) },
                            myReviewsBadge = myReviewsBadge,
                            onOpenMyReviews = { navController.navigate(MyReviewsRoute) },
                        )
                    } else {
                        GuestChatTab(onLoginClick = { navController.navigate(AuthRoute) })
                    }
                },
                cartContent = {
                    if (isAuthorized) {
                        CartPage(
                            onOpenOrder = { navController.navigate(OrderDetailRoute(it)) },
                            onOpenHalyk = openHalyk,
                        )
                    } else {
                        GuestCartTab(onLoginClick = { navController.navigate(AuthRoute) })
                    }
                },
                servicesContent = {
                    // Фаза 17: SERVICES = Сервистер (11 тақта + әріптестер);
                    // профиль — ProfileRoute (Home аватары ашады).
                    ServicesPage(
                        onOpenEgov = { navController.navigate(EgovServicesRoute) },
                    )
                },
            )
        }
        // ---- Чат (Фаза 12) ----
        composable<ChatRoomRoute> {
            // Фаза 13: қоңырау шалу — микрофон рұқсаты берілмесе, алдымен
            // сұралады, содан кейін invite жіберіледі (pending үлгісі).
            var pendingCall by remember {
                mutableStateOf<Triple<Long, String?, String?>?>(null)
            }
            val callMicLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                val pending = pendingCall
                pendingCall = null
                if (granted && pending != null) {
                    voiceCallManager.invite(
                        peerUserId = pending.first,
                        peerName = pending.second,
                        peerAvatarUrl = pending.third,
                        micGranted = true,
                    )
                }
            }
            val callState by voiceCallManager.state.collectAsState()
            val activityContext = androidx.compose.ui.platform.LocalContext.current
            ChatRoomPage(
                onBack = { navController.popBackStack() },
                onOpenAnnouncement = { navController.navigate(AnnouncementDetailRoute(it)) },
                // Фаза 18: сурет/бейне/PDF — ішкі көрсеткіштер (zoom/Exo/PdfRenderer).
                onOpenPhotoViewer = { url ->
                    navController.navigate(PhotoViewerRoute(listOf(url)))
                },
                onOpenVideoViewer = { url ->
                    navController.navigate(VideoViewerRoute(url))
                },
                onOpenPdfViewer = { url ->
                    navController.navigate(PdfViewerRoute(url))
                },
                callActive = callState.phase != CallPhase.IDLE,
                onVoiceCall = { peerId, peerName, peerAvatarUrl ->
                    val micGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                        activityContext,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (micGranted) {
                        voiceCallManager.invite(peerId, peerName, peerAvatarUrl, micGranted = true)
                    } else {
                        pendingCall = Triple(peerId, peerName, peerAvatarUrl)
                        callMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
            )
        }
        composable<ArchivedChatsRoute> {
            ArchivedChatsPage(
                onBack = { navController.popBackStack() },
                onOpenRoom = { room, username, otherUserId, isSystemChat, announcementId ->
                    navController.navigate(
                        ChatRoomRoute(
                            roomId = room.roomId,
                            otherUserId = otherUserId,
                            username = username,
                            isSystemChat = isSystemChat,
                            announcementId = announcementId?.toLongOrNull(),
                            roomAnnouncementId = room.announcementId,
                        ),
                    )
                },
            )
        }

        // ---- Хабарламалар (Фаза 12) ----
        composable<NotificationsRoute> {
            NotificationsPage(
                onBack = { navController.popBackStack() },
                onOpenType = { type ->
                    navController.navigate(NotificationsByTypeRoute(type.path))
                },
            )
        }
        composable<NotificationsByTypeRoute> {
            NotificationsByTypePage(
                onBack = { navController.popBackStack() },
                onOpenItem = { item, type ->
                    navController.navigate(SingleNotificationRoute(item, type.path))
                },
            )
        }
        composable<SingleNotificationRoute> { entry ->
            val route = entry.toRoute<SingleNotificationRoute>()
            SingleNotificationPage(
                item = route.item,
                type = NotificationType.fromString(route.type),
                onBack = { navController.popBackStack() },
            )
        }
        composable<AuthRoute> {
            Box(modifier = Modifier.fillMaxSize()) {
                AuthFlowPage(
                    onAuthorized = { offerPinSetup ->
                        if (offerPinSetup) {
                            navController.navigate(PinSetupRoute) {
                                popUpTo(AuthRoute) { inclusive = true }
                            }
                        } else {
                            navController.navigate(MainShellRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onClosed = { navController.popBackStack() },
                )
            }
        }
        composable<PinSetupRoute> {
            val appViewModel: AppViewModel = hiltViewModel()
            PinSetupPage(
                pinManager = appViewModel.pinManager,
                onDone = {
                    navController.navigate(MainShellRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSkipped = {
                    navController.navigate(MainShellRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // ---- Профиль (Phase 4) ----
        composable<EditProfileRoute> {
            EditProfilePage(onBack = { navController.popBackStack() })
        }
        composable<ProfileAddressesRoute> {
            ProfileAddressesPage(
                onBack = { navController.popBackStack() },
                onEditLocation = { location ->
                    navController.navigate(AddressEditRoute(location?.id ?: 0L))
                },
            )
        }
        composable<CompanySettingsRoute> {
            CompanySettingsPage(
                onBack = { navController.popBackStack() },
                onOpenSection = { section ->
                    navController.navigate(CompanySectionRoute(section))
                },
            )
        }
        composable<CompanySectionRoute> { entry ->
            CompanySectionPage(
                section = entry.toRoute<CompanySectionRoute>().section,
                onBack = { navController.popBackStack() },
            )
        }
        composable<VerificationRoute> {
            VerificationPage(onBack = { navController.popBackStack() })
        }
        composable<DealerTermsRoute> {
            DealerTermsPage(
                standalone = true,
                onBack = { navController.popBackStack() },
            )
        }

        // ---- Маркетплейс (Phase 5) ----
        composable<AnnouncementsListRoute>(typeMap = AnnouncementFilterTypeMap) { entry ->
            val route = entry.toRoute<AnnouncementsListRoute>()
            // FilterPage нәтижесі осы entry-дің savedStateHandle-ына жазылады.
            val filter by entry.savedStateHandle
                .getStateFlow(FILTER_RESULT_KEY, route.filter)
                .collectAsState()
            AnnouncementsListPage(
                filter = filter,
                onBack = { navController.popBackStack() },
                onOpenDetail = { navController.navigate(AnnouncementDetailRoute(it)) },
                onOpenFilter = { navController.navigate(FilterRoute(filter)) },
            )
        }
        composable<FilterRoute>(typeMap = AnnouncementFilterTypeMap) { entry ->
            val route = entry.toRoute<FilterRoute>()
            FilterPage(
                initialFilter = route.filter,
                onBack = { navController.popBackStack() },
                onApply = { newFilter ->
                    val previous = navController.previousBackStackEntry
                    if (previous?.destination?.route?.contains("AnnouncementsListRoute") == true) {
                        // Лентадан ашылды — нәтижені сол экранға қайтарып, артқа шығамыз.
                        previous.savedStateHandle[FILTER_RESULT_KEY] = newFilter
                        navController.popBackStack()
                    } else {
                        // Home-дан ашылды — сүзгімен лента экранына тікелей кіреміз.
                        navController.navigate(AnnouncementsListRoute(newFilter)) {
                            popUpTo(FilterRoute::class) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable<AnnouncementDetailRoute> { entry ->
            AnnouncementDetailPage(
                announcementId = entry.toRoute<AnnouncementDetailRoute>().id,
                onBack = { navController.popBackStack() },
                onOpenDetail = { navController.navigate(AnnouncementDetailRoute(it)) },
                // Фаза 12: сатушымен чат. Гость — авторизация арқылы.
                onOpenChat = if (isAuthorized) {
                    { otherUserId, username, annId ->
                        navController.navigate(
                            ChatRoomRoute(
                                otherUserId = otherUserId,
                                username = username,
                                announcementId = annId,
                            ),
                        )
                    }
                } else {
                    { _, _, _ -> navController.navigate(AuthRoute) }
                },
                // Фаза 18: галерея — фото көрсеткіш; пікірлер бөлімі; сатушы пікірлері.
                onOpenPhotoViewer = { images, index ->
                    navController.navigate(PhotoViewerRoute(images, index))
                },
                onOpenReviews = { id ->
                    navController.navigate(AnnouncementReviewsRoute(id))
                },
                onOpenSellerReviews = { userId ->
                    navController.navigate(SellerReviewsRoute(userId))
                },
                bottomBar = {
                    if (isAuthorized) {
                        DetailBuyBar(
                            detailViewModel = hiltViewModel(viewModelStoreOwner = entry),
                            onOpenOrder = { navController.navigate(OrderDetailRoute(it)) },
                        )
                    }
                },
            )
        }
        composable<CategoriesRoute> {
            CategoriesPage(
                onBack = { navController.popBackStack() },
                onOpenSubcategories = { navController.navigate(SubcategoriesRoute(it)) },
            )
        }
        composable<SubcategoriesRoute> { entry ->
            SubcategoriesPage(
                categoryId = entry.toRoute<SubcategoriesRoute>().categoryId,
                onBack = { navController.popBackStack() },
                onOpenFeed = { categoryId, subcategoryId ->
                    navController.navigate(
                        AnnouncementsListRoute(
                            AnnouncementFilter(categoryId = categoryId, subcategoryId = subcategoryId),
                        ),
                    )
                },
            )
        }
        composable<FavoritesRoute> {
            FavoritesPage(
                onBack = { navController.popBackStack() },
                onOpenDetail = { navController.navigate(AnnouncementDetailRoute(it)) },
            )
        }

        // ---- Маркетплейс: жазу режимі (Phase 6) ----
        composable<CreateOrOfferRoute> {
            CreateOrOfferPage(
                onBack = { navController.popBackStack() },
                onCreateAd = { navController.navigate(CreateAdRoute) },
                onMakeOffer = { navController.navigate(MakeOfferRoute) },
                onOpenBulkUpload = { navController.navigate(BulkUploadRoute) },
            )
        }
        composable<CreateAdRoute> { entry ->
            val mapSelection by entry.savedStateHandle
                .getStateFlow<SelectedLocation?>(LOCATION_RESULT_KEY, null)
                .collectAsState()
            CreateAdPage(
                onBack = { navController.popBackStack() },
                onSubmitted = { navController.popBackStack() },
                onOpenBulkUpload = { navController.navigate(BulkUploadRoute) },
                mapSelection = mapSelection,
                onMapSelectionConsumed = { entry.savedStateHandle[LOCATION_RESULT_KEY] = null },
                onOpenMapPicker = { prefill ->
                    navController.navigate(LocationSelectionRoute(prefill))
                },
            )
        }
        composable<EditAdRoute> { entry ->
            val mapSelection by entry.savedStateHandle
                .getStateFlow<SelectedLocation?>(LOCATION_RESULT_KEY, null)
                .collectAsState()
            CreateAdPage(
                onBack = { navController.popBackStack() },
                onSubmitted = { navController.popBackStack() },
                onOpenBulkUpload = { navController.navigate(BulkUploadRoute) },
                mapSelection = mapSelection,
                onMapSelectionConsumed = { entry.savedStateHandle[LOCATION_RESULT_KEY] = null },
                onOpenMapPicker = { prefill ->
                    navController.navigate(LocationSelectionRoute(prefill))
                },
                viewModel = hiltViewModel(viewModelStoreOwner = entry),
            )
        }
        composable<MakeOfferRoute> {
            MakeOfferPage(
                onBack = { navController.popBackStack() },
                onSubmitted = { navController.popBackStack() },
            )
        }
        composable<BulkUploadRoute> {
            BulkUploadPage(onBack = { navController.popBackStack() })
        }
        composable<MyAnnouncementsRoute> { entry ->
            MyAnnouncementsPage(
                onBack = { navController.popBackStack() },
                onOpenStatus = { status ->
                    navController.navigate(MyAnnouncementsRoute(status)) {
                        // Статус беттері шынжыр болмайды — әрқайсысы жаңа стек құрады.
                    }
                },
                onOpenAnnouncement = { navController.navigate(ProfileAnnouncementRoute(it)) },
                onEditAnnouncement = { navController.navigate(EditAdRoute(it)) },
                viewModel = hiltViewModel(viewModelStoreOwner = entry),
            )
        }
        composable<ProfileAnnouncementRoute> { entry ->
            val id = entry.toRoute<ProfileAnnouncementRoute>().id
            OwnerAnnouncementPage(
                announcementId = id,
                onBack = { navController.popBackStack() },
                onOpenDetail = { navController.navigate(AnnouncementDetailRoute(it)) },
                onEdit = { navController.navigate(EditAdRoute(it)) },
                onPromote = { navController.navigate(AdvertiseAdRoute(it)) },
            )
        }

        // ---- Локация (Phase 7) ----
        // Бірінші іске қосу: өңір орнату (country → region → MainShell).
        composable<CountryListRoute> {
            CountryListPage(
                onPickCountry = { country ->
                    navController.navigate(
                        RegionListRoute(country.id, country.localizedName(localeTag)),
                    )
                },
            )
        }
        composable<RegionListRoute> { entry ->
            val route = entry.toRoute<RegionListRoute>()
            RegionListPage(
                countryName = route.countryName.takeIf { it.isNotBlank() },
                onPickRegion = { region ->
                    // AppRegion persist: countryId + regionId (/cities DEV-те 404 →
                    // cityId := region id).
                    onAppRegionSelected(route.countryId, region.id)
                    navController.navigate(MainShellRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable<AddressEditRoute> { entry ->
            val mapSelection by entry.savedStateHandle
                .getStateFlow<SelectedLocation?>(LOCATION_RESULT_KEY, null)
                .collectAsState()
            AddressEditPage(
                locationId = entry.toRoute<AddressEditRoute>().locationId,
                mapSelection = mapSelection,
                onConsumeMapSelection = { entry.savedStateHandle[LOCATION_RESULT_KEY] = null },
                onOpenMapPicker = { prefill ->
                    navController.navigate(LocationSelectionRoute(prefill))
                },
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable<LocationSelectionRoute> { entry ->
            LocationSelectionPage(
                prefill = entry.toRoute<LocationSelectionRoute>().prefill,
                onBack = { navController.popBackStack() },
                onConfirm = { selected ->
                    // Нәтиже шақырған экранның savedStateHandle-ына қайтады.
                    navController.previousBackStackEntry?.savedStateHandle?.set(LOCATION_RESULT_KEY, selected)
                    navController.popBackStack()
                },
            )
        }

        // ---- Себет / тапсырыстар (Phase 8) ----
        composable<OrderDetailRoute> { entry ->
            OrderDetailPage(
                orderId = entry.toRoute<OrderDetailRoute>().orderId,
                onBack = { navController.popBackStack() },
                onOpenHalyk = openHalyk,
            )
        }

        // ---- Төлемдер (Phase 9) ----
        composable<PaymentResultRoute> { entry ->
            PaymentResultPage(
                orderId = entry.toRoute<PaymentResultRoute>().orderId,
                onBack = { navController.popBackStack() },
            )
        }
        composable<WebViewRoute> { entry ->
            val route = entry.toRoute<WebViewRoute>()
            WebViewPage(
                title = route.title,
                url = route.url,
                html = route.html,
                exitRedirectUrl = route.exitRedirectUrl,
                onFinished = {
                    navController.popBackStack()
                    route.paymentResultOrderId?.let { orderId ->
                        navController.navigate(PaymentResultRoute(orderId))
                    }
                },
            )
        }

        // ---- Әмиян (Phase 10) ----
        composable<BalanceRoute> {
            BalancePage(
                onBack = { navController.popBackStack() },
                onWithdraw = { available ->
                    navController.navigate(WithdrawRoute(available))
                },
            )
        }
        composable<WithdrawRoute> { entry ->
            WithdrawPage(
                availableBalance = entry.toRoute<WithdrawRoute>().availableBalance,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
        composable<TransactionHistoryRoute> {
            TransactionHistoryPage(onBack = { navController.popBackStack() })
        }
        composable<TopUpRoute> {
            TopUpPage(
                onBack = { navController.popBackStack() },
                onOpenWebView = openTopUpWebView,
            )
        }

        // ---- Промо v2 (Phase 15) ----
        composable<AdvertiseAdRoute> { entry ->
            AdvertiseAdPage(
                announcementId = entry.toRoute<AdvertiseAdRoute>().announcementId,
                onBack = { navController.popBackStack() },
                onTopUp = { navController.navigate(TopUpRoute) },
            )
        }
        composable<PromotedAnnouncementsRoute> {
            PromotedAnnouncementsPage(
                onBack = { navController.popBackStack() },
                onOpenAnnouncement = { navController.navigate(ProfileAnnouncementRoute(it)) },
            )
        }
        composable<HotAnnouncementsRoute> {
            // Flutter HotAnnouncementsPage: GET /announcements type_ad=vip.
            // Hot лентасы — бір турлі лента, сүзгі жолағы жоқ.
            AnnouncementsListPage(
                filter = AnnouncementFilter(typeAd = "vip"),
                onBack = { navController.popBackStack() },
                onOpenDetail = { navController.navigate(AnnouncementDetailRoute(it)) },
                onOpenFilter = {},
                titleOverride = stringResource(L10nR.string.hot_announcements),
                emptyMessageOverride = stringResource(L10nR.string.no_hot_announcements),
                showFilterControls = false,
            )
        }

        // ---- Дилер консолі (Фаза 16) ----
        composable<DealerProductsRoute> { entry ->
            val route = entry.toRoute<DealerProductsRoute>()
            DealerProductsPage(
                initialTab = route.initialTab,
                onBack = { navController.popBackStack() },
                onEditProduct = { navController.navigate(EditAdRoute(it)) },
                onAddZone = { navController.navigate(AddEditDeliveryZoneRoute()) },
                onEditZone = { navController.navigate(AddEditDeliveryZoneRoute(it)) },
                // Жарнама пакетін таңдау — активті жарнамалар тізімі арқылы
                // → onPromote → AdvertiseAdPage (Фаза 15 wiring-ты қайта пайдаланады).
                onPickAnnouncement = { navController.navigate(MyAnnouncementsRoute("active")) },
            )
        }
        composable<DealerOrdersRoute> { entry ->
            val route = entry.toRoute<DealerOrdersRoute>()
            DealerOrdersPage(
                initialTab = route.initialTab,
                onBack = { navController.popBackStack() },
                onOpenTeamPool = { navController.navigate(TeamPoolRoute) },
                onOpenTracking = { navController.navigate(OrderTrackingRoute(it)) },
            )
        }
        composable<TeamPoolRoute> {
            TeamPoolPage(
                onBack = { navController.popBackStack() },
                onOpenEmployees = { navController.navigate(DealerEmployeesRoute) },
            )
        }
        composable<DealerEmployeesRoute> {
            DealerEmployeesPage(onBack = { navController.popBackStack() })
        }
        composable<OrderTrackingRoute> {
            OrderTrackingPage(onBack = { navController.popBackStack() })
        }
        composable<AddEditDeliveryZoneRoute> {
            AddEditDeliveryZonePage(onBack = { navController.popBackStack() })
        }
        composable<DealerSettingsRoute> {
            DealerSettingsPage(
                onBack = { navController.popBackStack() },
                onOpenWallet = { navController.navigate(BalanceRoute) },
            )
        }

        // ---- Профиль (Фаза 17: Home аватары ашатын дербес маршрут) ----
        composable<ProfileRoute> {
            ProfilePage(
                isAuthorized = isAuthorized,
                themeMode = themeMode,
                onThemeChange = onThemeChange,
                onLoginClick = { navController.navigate(AuthRoute) },
                onEditProfile = { navController.navigate(EditProfileRoute) },
                onAddresses = { navController.navigate(ProfileAddressesRoute) },
                onCompanySettings = { navController.navigate(CompanySettingsRoute) },
                onVerification = { navController.navigate(VerificationRoute) },
                onOpenWallet = { navController.navigate(BalanceRoute) },
                onOpenTransactions = { navController.navigate(TransactionHistoryRoute) },
                onOpenTopUp = { navController.navigate(TopUpRoute) },
                onMyAnnouncements = { status ->
                    navController.navigate(MyAnnouncementsRoute(status))
                },
                // «Сұраныстарым» — feature:demand тізімі (Фаза 17).
                onMyDemands = {
                    if (isAuthorized) {
                        navController.navigate(DemandListRoute)
                    } else {
                        navController.navigate(AuthRoute)
                    }
                },
                // «Менің пікірлерім» — spec §10 (Фаза 18).
                onOpenMyReviews = {
                    if (isAuthorized) {
                        navController.navigate(MyReviewsRoute)
                    } else {
                        navController.navigate(AuthRoute)
                    }
                },
                // Фаза 12: қолдау чаты — жүйелік қолданушы 31 (kSupportUserId).
                onOpenSupportChat = {
                    navController.navigate(
                        ChatRoomRoute(otherUserId = 31L, isSystemChat = true),
                    )
                },
                // Фаза 16: дилер консолі — профильден кіру.
                onDealerProducts = { tab ->
                    navController.navigate(DealerProductsRoute(tab))
                },
                onDealerOrders = { tab ->
                    navController.navigate(DealerOrdersRoute(tab))
                },
                onDealerSettings = { navController.navigate(DealerSettingsRoute) },
            )
        }

        // ---- Қытай (MercuryX) — Фаза 17 ----
        composable<ChinaSubcategoriesRoute> { entry ->
            val route = entry.toRoute<ChinaSubcategoriesRoute>()
            ChinaSubcategoriesPage(
                title = route.title,
                onBack = { navController.popBackStack() },
                onOpenSubcategories = { parentId, title ->
                    navController.navigate(ChinaSubcategoriesRoute(parentId, title))
                },
                onOpenProducts = { categoryId, title ->
                    navController.navigate(ChinaProductsRoute(categoryId, title))
                },
            )
        }
        composable<ChinaProductsRoute> { entry ->
            val route = entry.toRoute<ChinaProductsRoute>()
            ChinaProductsPage(
                title = route.title,
                onBack = { navController.popBackStack() },
                onOpenProduct = { productId ->
                    navController.navigate(ChinaProductDetailRoute(productId))
                },
            )
        }
        composable<ChinaProductDetailRoute> { entry ->
            ChinaProductDetailPage(
                onBack = { navController.popBackStack() },
            )
        }

        // ---- EGOV — Фаза 17 ----
        composable<EgovServicesRoute> {
            EgovServicesPage(onBack = { navController.popBackStack() })
        }

        // ---- Сұраныстар — Фаза 17 (спек қосымшасы) ----
        composable<DemandListRoute> {
            DemandListPage(
                onBack = { navController.popBackStack() },
                onOpenDetail = { id ->
                    navController.navigate(DemandDetailRoute(id))
                },
                onOpenCreate = {
                    navController.navigate(CreateEditDemandRoute())
                },
            )
        }
        composable<DemandDetailRoute> { entry ->
            DemandDetailPage(
                onBack = { navController.popBackStack() },
                onOpenEdit = { id ->
                    navController.navigate(CreateEditDemandRoute(id))
                },
            )
        }
        composable<CreateEditDemandRoute> {
            CreateEditDemandPage(onBack = { navController.popBackStack() })
        }

        // ---- Пікірлер (Фаза 18) ----
        composable<AnnouncementReviewsRoute> { entry ->
            val route = entry.toRoute<AnnouncementReviewsRoute>()
            AnnouncementReviewsPage(
                announcementId = route.announcementId,
                onBack = { navController.popBackStack() },
                viewModel = hiltViewModel(viewModelStoreOwner = entry),
            )
        }
        composable<SendReviewRoute> { entry ->
            val route = entry.toRoute<SendReviewRoute>()
            SendReviewPage(
                announcementId = route.announcementId,
                onBack = { navController.popBackStack() },
                viewModel = hiltViewModel(viewModelStoreOwner = entry),
            )
        }
        composable<ProfileReviewsRoute> { entry ->
            val route = entry.toRoute<ProfileReviewsRoute>()
            ProfileReviewsPage(
                onBack = { navController.popBackStack() },
                onOpenAnnouncementReviews = { id ->
                    navController.navigate(AnnouncementReviewsRoute(id))
                },
                viewModel = hiltViewModel(viewModelStoreOwner = entry),
            )
        }
        composable<SellerReviewsRoute> { entry ->
            SellerReviewsPage(
                onBack = { navController.popBackStack() },
                viewModel = hiltViewModel(viewModelStoreOwner = entry),
            )
        }
        composable<MyReviewsRoute> {
            // VM — activity-scoped: чат бейджімен бір күй.
            MyReviewsPage(
                viewModel = myReviewsViewModel,
                onBack = { navController.popBackStack() },
                onOpenSendReview = { id ->
                    navController.navigate(SendReviewRoute(id))
                },
            )
        }

        // ---- Медиа көрсеткіштері (Фаза 18, ISSUES #27) ----
        composable<PhotoViewerRoute> { entry ->
            val route = entry.toRoute<PhotoViewerRoute>()
            PhotoViewerPage(
                images = route.images,
                initialIndex = route.initialIndex,
                onBack = { navController.popBackStack() },
            )
        }
        composable<VideoViewerRoute> { entry ->
            val route = entry.toRoute<VideoViewerRoute>()
            VideoViewerPage(
                url = route.url,
                title = route.title,
                onBack = { navController.popBackStack() },
            )
        }
        composable<PdfViewerRoute> { entry ->
            val route = entry.toRoute<PdfViewerRoute>()
            PdfViewerPage(
                url = route.url,
                onBack = { navController.popBackStack() },
            )
        }
        composable<YouTubeViewerRoute> { entry ->
            val route = entry.toRoute<YouTubeViewerRoute>()
            YouTubeViewerPage(
                url = route.url,
                onBack = { navController.popBackStack() },
            )
        }

        // ---- QR сканер (Фаза 18) ----
        composable<QrScannerRoute> {
            QrScannerPage(
                onBack = { navController.popBackStack() },
                onAnnouncementScanned = { id ->
                    navController.popBackStack()
                    navController.navigate(AnnouncementDetailRoute(id))
                },
            )
        }
    }
}

/** Halyk төлемі аяқталғанда redirect жасайтын хост (Flutter exitRedirectUrl). */
private const val HALYK_EXIT_HOST = "agroland.kz"

/** FilterPage → AnnouncementsListPage нәтиже кілті. */
private const val FILTER_RESULT_KEY = "filter"