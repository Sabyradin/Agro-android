package com.agroland.core.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Analytics + Crashlytics wrapper — Flutter analytics_service.dart
 * parity: logEvent / logScreenView / setUserProperty / setUserId /
 * recordError. «Analytics must never break the app» — барлық шақыру
 * try/catch (google-services.json плейсхолдер кезінде де crash болмайды).
 */
@Singleton
class FirebaseAnalyticsService @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val analytics: FirebaseAnalytics? by lazy {
        try {
            FirebaseAnalytics.getInstance(context)
        } catch (_: Exception) {
            null
        }
    }

    private val crashlytics: FirebaseCrashlytics? by lazy {
        try {
            FirebaseCrashlytics.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    fun logEvent(name: String, params: Map<String, Any?> = emptyMap()) {
        try {
            analytics?.logEvent(name, params.toBundle())
        } catch (_: Exception) {
        }
    }

    /** SCREEN_VIEW стандарт оқиғасы (MonitoringRouteObserver ↔ route observer parity). */
    fun logScreenView(screenName: String, screenClass: String? = null) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass ?: screenName)
            }
            analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        } catch (_: Exception) {
        }
    }

    fun setUserProperty(name: String, value: String) {
        try {
            analytics?.setUserProperty(name, value)
        } catch (_: Exception) {
        }
    }

    fun setUserId(userId: String?) {
        try {
            analytics?.setUserId(userId)
            crashlytics?.setUserId(userId ?: "")
        } catch (_: Exception) {
        }
    }

    /** Flutter recordError parity — non-fatal exception. */
    fun recordError(throwable: Throwable) {
        try {
            crashlytics?.recordException(throwable)
        } catch (_: Exception) {
        }
    }

    fun log(message: String) {
        try {
            crashlytics?.log(message)
        } catch (_: Exception) {
        }
    }

    private fun Map<String, Any?>.toBundle(): Bundle = Bundle().apply {
        forEach { (k, v) ->
            when (v) {
                null -> Unit
                is String -> putString(k, v)
                is Int -> putInt(k, v)
                is Long -> putLong(k, v)
                is Double -> putDouble(k, v)
                is Boolean -> putBoolean(k, v)
                else -> putString(k, v.toString())
            }
        }
    }
}