package com.agroland.app

import android.os.Bundle
import androidx.activity.compose.setContent
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
import com.agroland.app.navigation.PinSetupRoute
import com.agroland.app.navigation.ProfileAddressesRoute
import com.agroland.app.navigation.ProfileAnnouncementRoute
import com.agroland.app.navigation.RegionListRoute
import com.agroland.app.navigation.SplashRoute
import com.agroland.app.navigation.SubcategoriesRoute
import com.agroland.app.navigation.VerificationRoute
import com.agroland.app.navigation.OrderDetailRoute
import com.agroland.app.navigation.PaymentResultRoute
import com.agroland.app.navigation.WebViewRoute
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

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val shellViewModel: ShellViewModel = hiltViewModel()
            val appViewModel: AppViewModel = hiltViewModel()
            val themeMode by shellViewModel.themeMode.collectAsState()
            val localeTag by shellViewModel.localeTag.collectAsState()
            val session by appViewModel.session.collectAsState()

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
                        onThemeChange = { shellViewModel.setThemeMode(it) },
                        onAppRegionSelected = { countryId, regionId ->
                            shellViewModel.setAppRegion(countryId, regionId)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(
    isAuthorized: Boolean,
    themeMode: ThemeMode,
    localeTag: String?,
    appViewModel: AppViewModel,
    onThemeChange: (ThemeMode) -> Unit,
    onAppRegionSelected: (Int, Int) -> Unit,
) {
    val navController = rememberNavController()

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
                    )
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
                        onMyAnnouncements = { status ->
                            navController.navigate(MyAnnouncementsRoute(status))
                        },
                    )
                },
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
    }
}

/** Halyk төлемі аяқталғанда redirect жасайтын хост (Flutter exitRedirectUrl). */
private const val HALYK_EXIT_HOST = "agroland.kz"

/** FilterPage → AnnouncementsListPage нәтиже кілті. */
private const val FILTER_RESULT_KEY = "filter"