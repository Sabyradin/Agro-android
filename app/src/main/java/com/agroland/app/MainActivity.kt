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
import com.agroland.app.navigation.AuthRoute
import com.agroland.app.navigation.LanguageRoute
import com.agroland.app.navigation.MainShellRoute
import com.agroland.app.navigation.PinSetupRoute
import com.agroland.app.navigation.SplashRoute
import com.agroland.core.common.settings.ThemeMode
import com.agroland.core.ui.theme.AgroTheme
import com.agroland.feature.auth.session.SessionState
import com.agroland.feature.auth.ui.AppLockGate
import com.agroland.feature.auth.ui.AuthFlowPage
import com.agroland.feature.auth.ui.PinSetupPage
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
                    AppNavHost(isAuthorized = session == SessionState.Authorized)
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(isAuthorized: Boolean) {
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
    }
}