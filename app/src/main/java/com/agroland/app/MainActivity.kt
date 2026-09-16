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
import com.agroland.feature.shell.ShellViewModel
import com.agroland.feature.shell.language.LanguagePage
import com.agroland.feature.shell.main.MainShellPage
import com.agroland.feature.shell.splash.SplashPage
import com.agroland.feature.shell.splash.SplashTarget
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var pushController: PushController

    @Inject
    lateinit var chatSocketService: ChatSocketService

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
                    }
                    SessionState.Guest -> {
                        pushController.unregister()
                        chatSocketService.stop()
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
                if (locked) {
                    AppLockGate(
                        pinManager = appViewModel.pinManager,
                        activity = this,
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
                        onThemeChange = { shellViewModel.setThemeMode(it) },
                        onAppRegionSelected = { countryId, regionId ->
                            shellViewModel.setAppRegion(countryId, regionId)
                        },
                    )
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
    onThemeChange: (ThemeMode) -> Unit,
    onAppRegionSelected: (Int, Int) -> Unit,
) {
    val navController = rememberNavController()

    // Чат қойындысының бейджі — Socket.IO totalUnread (Flutter bottom_navbar).
    val chatBadge by chatSocketService.totalUnread.collectAsState()

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
                    )
                },
                chatContent = {
                    if (isAuthorized) {
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
                        // Фаза 12: қолдау чаты — жүйелік қолданушы 31 (kSupportUserId).
                        onOpenSupportChat = {
                            navController.navigate(
                                ChatRoomRoute(otherUserId = 31L, isSystemChat = true),
                            )
                        },
                    )
                },
            )
        }
        // ---- Чат (Фаза 12) ----
        composable<ChatRoomRoute> {
            ChatRoomPage(
                onBack = { navController.popBackStack() },
                onOpenAnnouncement = { navController.navigate(AnnouncementDetailRoute(it)) },
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
    }
}

/** Halyk төлемі аяқталғанда redirect жасайтын хост (Flutter exitRedirectUrl). */
private const val HALYK_EXIT_HOST = "agroland.kz"

/** FilterPage → AnnouncementsListPage нәтиже кілті. */
private const val FILTER_RESULT_KEY = "filter"