package com.agroland.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.svg.SvgDecoder
import com.agroland.core.analytics.FirebaseAnalyticsService
import com.agroland.core.analytics.MonitoringService
import com.agroland.core.analytics.TikTokAnalytics
import com.agroland.core.ui.components.MediaUrlResolver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AgrolandApplication : Application(), SingletonImageLoader.Factory {

    /** Фаза 19: мониторинг fan-out (monitoring API + TikTok + Firebase). */
    @Inject
    lateinit var monitoringService: MonitoringService

    @Inject
    lateinit var tikTokAnalytics: TikTokAnalytics

    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalyticsService

    override fun onCreate() {
        super.onCreate()
        // Медиа URL негізі: {host}/api/v1 → {host}. Салыстырмалы media жолдары осыған жалғасады.
        MediaUrlResolver.baseMediaUrl = BuildConfig.BASE_API_URL.removeSuffix("/api/v1")

        // Фаза 19 (Flutter app_bootstrap parity): TikTok SDK init — босағада,
        // sync (жергілікті TTConfig дайындау ғана; credential бос болса skip).
        tikTokAnalytics.init(this, BuildConfig.TIKTOK_APP_ID, BuildConfig.TIKTOK_TT_APP_ID)

        // trackAppOpen — fire-and-forget (Flutter: unawaited — monitoring
        // Cloud Run scale-to-zero cold start UI-ды блоктауы мүмкін).
        monitoringService.trackAppOpen()
    }

    /**
     * Coil singleton ImageLoader — SVG декодерімен (Фаза 17: MercuryX категория
     * суреттері SVG форматында келеді). Network fetcher ServiceLoader арқылы
     * өзі қосылады (coil-network-okhttp). Coil 3.2: Application классы
     * SingletonImageLoader.Factory жүзеге асырады.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
}