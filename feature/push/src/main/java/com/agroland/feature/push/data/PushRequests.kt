package com.agroland.feature.push.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * POST /device денесі (Flutter sendPushToken):
 * {device_id, firebase_token, platform, os_version, app_version, device_model,
 *  language, timezone?, location?{latitude, longitude}}.
 *
 * `language` — backend push-ты осыған сүйеніп локализациялайды (kk/ru/en/zh тікелей;
 * Flutter-де де APP_LOCALE_KEY мәні жіберіледі). `platform` — кіші әріп «android».
 */
object PushRequests {

    fun deviceRegistration(
        deviceId: String,
        firebaseToken: String,
        osVersion: String,
        appVersion: String,
        deviceModel: String,
        language: String,
        timezone: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
    ): JsonObject = buildJsonObject {
        put("device_id", deviceId)
        put("firebase_token", firebaseToken)
        put("platform", PLATFORM_ANDROID)
        put("os_version", osVersion)
        put("app_version", appVersion)
        put("device_model", deviceModel)
        put("language", language)
        timezone?.takeIf { it.isNotBlank() }?.let { put("timezone", it) }
        if (latitude != null && longitude != null) {
            put(
                "location",
                buildJsonObject {
                    put("latitude", latitude)
                    put("longitude", longitude)
                },
            )
        }
    }

    const val PLATFORM_ANDROID = "android"
}