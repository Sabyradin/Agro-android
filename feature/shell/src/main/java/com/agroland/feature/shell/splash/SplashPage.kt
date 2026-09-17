package com.agroland.feature.shell.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.common.settings.SettingsDataStore
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.R as CoreUiR
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

/**
 * Splash беті — жалғыз логотип: жүйелік splash иконкасы әдейі бос
 * (drawable/ic_splash_empty.xml), сондықтан логотип бірден осында, толық өлшемде
 * ақырын пайда болады (fade + сәл үлкею), кейін «тыныстайды»; астынан слоган мен индикатор.
 */
@Composable
fun SplashPage(
    onDecided: (SplashTarget) -> Unit,
) {
    val viewModel: SplashViewModel = hiltViewModel()
    LaunchedEffect(Unit) {
        viewModel.decide { target -> onDecided(target) }
    }
    val logoSize = 160.dp

    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(durationMillis = 650, easing = FastOutSlowInEasing))
    }
    val pulse = rememberInfiniteTransition(label = "splashPulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "splashScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Image(
            painter = painterResource(CoreUiR.drawable.ic_agroland_logo_splash),
            contentDescription = stringResource(L10nR.string.app_name),
            modifier = Modifier
                .align(Alignment.Center)
                .size(logoSize)
                .graphicsLayer {
                    // 0.85 → 1.0 үлкейіп пайда болады, кейін баяу пульсация қосылады.
                    val s = (0.85f + 0.15f * appear.value) * (1f + (scale - 1f) * appear.value)
                    scaleX = s
                    scaleY = s
                    alpha = appear.value
                },
        )
        Text(
            text = stringResource(L10nR.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .offset(y = logoSize / 2 + 24.dp)
                .graphicsLayer {
                    alpha = appear.value
                    translationY = (1f - appear.value) * 12.dp.toPx()
                },
        )
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.5.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 72.dp)
                .size(28.dp)
                .graphicsLayer { alpha = appear.value },
        )
    }
}
