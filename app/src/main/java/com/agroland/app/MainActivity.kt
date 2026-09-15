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
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.agroland.app.navigation.AnnouncementDetailRoute
import com.agroland.app.navigation.AnnouncementsListRoute
import com.agroland.app.navigation.AuthRoute
import com.agroland.app.navigation.CategoriesRoute
import com.agroland.app.navigation.CompanySectionRoute
import com.agroland.app.navigation.CompanySettingsRoute
import com.agroland.app.navigation.DealerTermsRoute
import com.agroland.app.navigation.EditProfileRoute
import com.agroland.app.navigation.FavoritesRoute
import com.agroland.app.navigation.FilterRoute
import com.agroland.app.navigation.LanguageRoute
import com.agroland.app.navigation.MainShellRoute
import com.agroland.app.navigation.PinSetupRoute
import com.agroland.app.navigation.ProfileAddressesRoute
import com.agroland.app.navigation.SplashRoute
import com.agroland.app.navigation.SubcategoriesRoute
import com.agroland.app.navigation.VerificationRoute
import com.agroland.core.common.settings.ThemeMode
import com.agroland.core.ui.theme.AgroTheme
import com.agroland.feature.auth.session.SessionState
import com.agroland.feature.auth.ui.AppLockGate
import com.agroland.feature.auth.ui.AuthFlowPage
import com.agroland.feature.auth.ui.PinSetupPage
import com.agroland.feature.marketplace.data.AnnouncementFilter
import com.agroland.feature.marketplace.ui.AnnouncementDetailPage
import com.agroland.feature.marketplace.ui.AnnouncementsListPage
import com.agroland.feature.marketplace.ui.CategoriesPage
import com.agroland.feature.marketplace.ui.FavoritesPage
import com.agroland.feature.marketplace.ui.FilterPage
import com.agroland.feature.marketplace.ui.HomeFeedPage
import com.agroland.feature.marketplace.ui.SubcategoriesPage
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
                        onThemeChange = { shellViewModel.setThemeMode(it) },
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
    onThemeChange: (ThemeMode) -> Unit,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = SplashRoute,
    ) {
        composable<SplashRoute> {
            SplashPage(
                onDecided = { target ->
                    val route = when (target) {
                        SplashTarget.Language -> LanguageRoute
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
                    navController.navigate(MainShellRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable<MainShellRoute> {
            MainShellPage(
                onCreateClick = {
                    if (isAuthorized) {
                        // Жарнама жасау — фаза 6 (auth қақпасымен).
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
            ProfileAddressesPage(onBack = { navController.popBackStack() })
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
        composable<AnnouncementsListRoute> { entry ->
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
        composable<FilterRoute> { entry ->
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
    }
}

/** FilterPage → AnnouncementsListPage нәтиже кілті. */
private const val FILTER_RESULT_KEY = "filter"