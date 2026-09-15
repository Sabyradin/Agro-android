package com.agroland.app

import android.app.Application
import com.agroland.core.ui.components.MediaUrlResolver
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AgrolandApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Медиа URL негізі: {host}/api/v1 → {host}. Салыстырмалы media жолдары осыған жалғасады.
        MediaUrlResolver.baseMediaUrl = BuildConfig.BASE_API_URL.removeSuffix("/api/v1")
    }
}