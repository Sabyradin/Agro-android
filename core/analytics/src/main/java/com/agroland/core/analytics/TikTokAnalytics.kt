package com.agroland.core.analytics

import android.content.Context
import android.provider.Settings
import com.tiktok.TikTokBusinessSdk
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject

/**
 * TikTok Business SDK wrapper — Flutter TikTokService + Android
 * TikTokBusinessChannel.kt біріктірілген нұсқасы (native portта method
 * channel қажет емес). Android SDK TTConfig appId + ttAppId ғана қабылдайды
 * — accessToken/secret клиент жағында қолданылмайды (iOS HMAC iOS-only).
 *
 * Event map iOS каналімен БІРДЕЙ (Events Manager біріктіріп көрсетеді):
 * login→Login, completeRegistration→CompleteRegistration,
 * purchase→CompletePayment, launchApp→LaunchAPP.
 *
 * Барлық шақыру try/catch + non-fatal: аналитика ешқашан app flow-н үзбейді.
 */
@Singleton
class TikTokAnalytics @Inject constructor() {

    @Volatile
    private var initialized = false

    /** Dart оқиға атаулары → TikTok standard event атаулары (iOS mirror 1:1). */
    val eventMap: Map<String, String> = mapOf(
        "login" to "Login",
        "completeRegistration" to "CompleteRegistration",
        "purchase" to "CompletePayment",
        "launchApp" to "LaunchAPP",
    )

    fun init(context: Context, appId: String, ttAppId: String): Boolean {
        if (appId.isEmpty() || ttAppId.isEmpty()) {
            android.util.Log.w(TAG, "init skipped — missing credentials (appId/ttAppId)")
            return false
        }
        return try {
            val config = TikTokBusinessSdk.TTConfig(context.applicationContext)
                .setAppId(appId)
                .setTTAppId(ttAppId)
                .setLogLevel(TikTokBusinessSdk.LogLevel.DEBUG)
            // Өндірістік конвейер — debug/test-event режим жоқ (Flutter parity).
            TikTokBusinessSdk.initializeSdk(config)
            initialized = true
            android.util.Log.i(TAG, "SDK initialized: appId=$appId ttAppId=$ttAppId")
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "init error: ${e.message}")
            false
        }
    }

    fun trackEvent(name: String, props: JsonObject? = null, eventId: String? = null) {
        if (!initialized || name.isEmpty()) return
        try {
            val ttName = eventMap[name] ?: name
            val json = org.json.JSONObject()
            props?.forEach { (k, v) ->
                when (v) {
                    is JsonPrimitive -> json.put(k, v.content)
                    else -> json.put(k, v.toString())
                }
            }
            eventId?.let { json.put("event_id", it) }
            TikTokBusinessSdk.trackEvent(ttName, json)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "trackEvent $name error: ${e.message}")
        }
    }

    /** identify(externalId, externalUserName, phoneNumber, email) — Flutter parity. */
    fun identify(externalId: String?, phone: String?, email: String?) {
        if (!initialized) return
        if (externalId == null && phone == null && email == null) return
        try {
            TikTokBusinessSdk.identify(externalId, null, phone, email)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "identify error: ${e.message}")
        }
    }

    fun logout() {
        if (!initialized) return
        try {
            TikTokBusinessSdk.logout()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "logout error: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "TikTok"
    }
}

/**
 * DeviceId — Flutter android_id parity (DeviceDetails.deviceId):
 * Settings.Secure.ANDROID_ID (құрылғы/жүктеу бойынша тұрақты, 64-bit hex).
 */
@Singleton
class DeviceIdProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val deviceId: String by lazy {
        try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } catch (_: Exception) {
            ""
        }
    }
}