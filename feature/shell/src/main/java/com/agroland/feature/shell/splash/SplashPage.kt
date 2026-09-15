package com.agroland.feature.shell.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.common.settings.SettingsDataStore
import com.agroland.core.ui.components.AppLogo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash шешімі: тіл таңдалды ма, өңір бекітілді ме — соған қарай келесі экран.
 * regionRedirectProvider баламасы: AppRegion null болса өңір орнатуға мәжбүрлейді.
 */
sealed interface SplashTarget {
    data object Language : SplashTarget
    data object RegionSetup : SplashTarget
    data object Main : SplashTarget
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val settings: SettingsDataStore,
) : ViewModel() {

    /**
     * Splash қақпасы: жалпы ұзақтығы ~3 сек (core-splashscreen жүйелік бөлігінен кейін).
     * Бірінші іске қосу — тіл таңдауына; тіл бар, өңір жоқ — өңір орнатуға;
     * қалғанда — басты экранға.
     */
    fun decide(onDecided: (SplashTarget) -> Unit) {
        viewModelScope.launch {
            val start = System.currentTimeMillis()
            val selected = settings.languageSelectedOnce()
            val regionSet = selected && settings.appRegionCountryIdOnce() != null
            val elapsed = System.currentTimeMillis() - start
            val remain = SPLASH_MIN_MS - elapsed
            if (remain > 0) delay(remain)
            onDecided(
                when {
                    !selected -> SplashTarget.Language
                    !regionSet -> SplashTarget.RegionSetup
                    else -> SplashTarget.Main
                },
            )
        }
    }

    private companion object {
        const val SPLASH_MIN_MS = 3000L
    }
}

@Composable
fun SplashPage(
    onDecided: (SplashTarget) -> Unit,
) {
    val viewModel: SplashViewModel = hiltViewModel()
    LaunchedEffect(Unit) {
        viewModel.decide { target -> onDecided(target) }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppLogo()
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(40.dp).padding(top = 24.dp),
        )
    }
}