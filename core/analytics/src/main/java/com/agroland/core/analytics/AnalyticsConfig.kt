package com.agroland.core.analytics

/**
 * Аналитика конфигурациясы — flavor-ға тәуелді (app AppConfigModule
 * BuildConfig-тан толтырады). Flutter assets/config/main{,_dev}.json
 * parity: MONITORING_API_URL + TIKTOK_*_ANDROID.
 */
interface AnalyticsConfig {
    /** Monitoring Cloud Run host (бос — monitoring API толығымен өшіріледі). */
    val monitoringApiUrl: String

    /** TikTok Business SDK Android appId (пакет атауы емес — TikTok Events Manager appId). */
    val tiktokAndroidAppId: String

    /** TikTok Events Manager ttAppId (Android app event source). */
    val tiktokAndroidTtAppId: String

    /** iOS HMAC signing parity үшін жіберіледі — Android SDK елемейді. */
    val tiktokAndroidAccessToken: String
}