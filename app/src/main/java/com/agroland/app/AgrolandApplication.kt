package com.agroland.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.svg.SvgDecoder
import com.agroland.core.ui.components.MediaUrlResolver
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AgrolandApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        // Медиа URL негізі: {host}/api/v1 → {host}. Салыстырмалы media жолдары осыған жалғасады.
        MediaUrlResolver.baseMediaUrl = BuildConfig.BASE_API_URL.removeSuffix("/api/v1")
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