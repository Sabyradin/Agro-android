package com.agroland.core.analytics

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Фаза 19: мониторинг JSON кілттері — Flutter freezed toJson 1:1.
 * Барлық null өріс JsonNull ретінде жіберіледі (freezed json_serializable
 * default behavior), phone_number — ЖАЛҒЫЗ snake_case кілт.
 */
class MonitoringEventJsonTest {

    private fun event(
        eventType: MonitoringEventType = MonitoringEventType.pageView,
        eventName: String = "page_view",
        timestamp: String = "2026-09-16T00:00:00Z",
        userId: String? = "42",
        phoneNumber: String = "+77770001122",
        deviceId: String? = "abc123",
        pageName: String? = "home",
        searchQuery: String? = null,
        categoryId: String? = null,
        apiEndpoint: String? = null,
        httpMethod: String? = null,
        statusCode: Int? = null,
        errorMessage: String? = null,
        amount: Double? = null,
        currency: String? = null,
        success: Boolean? = null,
        error: String? = null,
    ): MonitoringEvent = MonitoringEvent(
        eventType = eventType,
        eventName = eventName,
        data = null,
        timestamp = timestamp,
        userId = userId,
        phoneNumber = phoneNumber,
        deviceId = deviceId,
        pageName = pageName,
        searchQuery = searchQuery,
        categoryId = categoryId,
        apiEndpoint = apiEndpoint,
        httpMethod = httpMethod,
        statusCode = statusCode,
        errorMessage = errorMessage,
        amount = amount,
        currency = currency,
        success = success,
        error = error,
    )

    @Test
    fun `кілттер дәл Flutter пара-пар ( eventType camelCase, phone_number snake_case )`() {
        val json = monitoringEventToJson(event()).let { Json.parseToJsonElement(it.toString()).jsonObject }
        val expected = listOf(
            "eventType", "eventName", "data", "timestamp", "userId", "phone_number",
            "deviceId", "pageName", "searchQuery", "categoryId", "apiEndpoint",
            "httpMethod", "statusCode", "errorMessage", "amount", "currency",
            "success", "error",
        )
        assertEquals(expected, json.keys.toList())
    }

    @Test
    fun `eventType enum атауы жол ретінде жіберіледі`() {
        val json = Json.parseToJsonElement(
            monitoringEventToJson(event(eventType = MonitoringEventType.apiRequestFailure)).toString(),
        ).jsonObject
        assertEquals("apiRequestFailure", json["eventType"]!!.toString().trim('"'))
    }

    @Test
    fun `null өрістер JsonNull болып шығады ( freezed parity )`() {
        val json = Json.parseToJsonElement(
            monitoringEventToJson(
                event(
                    userId = null, deviceId = null, pageName = null, searchQuery = null,
                    categoryId = null, apiEndpoint = null, httpMethod = null,
                    statusCode = null, errorMessage = null, amount = null,
                    currency = null, success = null, error = null,
                ),
            ).toString(),
        ).jsonObject
        for (key in listOf(
            "userId", "deviceId", "pageName", "searchQuery", "categoryId",
            "apiEndpoint", "httpMethod", "statusCode", "errorMessage",
            "amount", "currency", "success", "error", "data",
        )) {
            assertEquals(JsonNull, json[key])
        }
        // Толтырылған өрістер — primitives.
        assertEquals("\"pageView\"", json["eventType"].toString())
        assertEquals("\"page_view\"", json["eventName"].toString())
        assertEquals("\"+77770001122\"", json["phone_number"].toString())
        assertEquals("\"2026-09-16T00:00:00Z\"", json["timestamp"].toString())
    }

    @Test
    fun `толық оқиға — сандық және булевалық типтер сақталады`() {
        val json = Json.parseToJsonElement(
            monitoringEventToJson(
                event(
                    eventType = MonitoringEventType.apiRequestFailure,
                    eventName = "api_request_failure",
                    apiEndpoint = "/announcements",
                    httpMethod = "GET",
                    statusCode = 500,
                    errorMessage = "boom",
                    success = false,
                    amount = 1500.5,
                    currency = "KZT",
                ),
            ).toString(),
        ).jsonObject
        assertEquals("500", json["statusCode"].toString())
        assertEquals("false", json["success"].toString())
        assertEquals("1500.5", json["amount"].toString())
        assertFalse(json["success"].toString().toBoolean())
    }

    @Test
    fun `login оқиғасы — phone_number толтырылады, userId null болуы мүмкін`() {
        val json = Json.parseToJsonElement(
            monitoringEventToJson(
                event(
                    eventType = MonitoringEventType.login,
                    eventName = "login",
                    userId = null,
                    phoneNumber = "+77001234567",
                    success = true,
                ),
            ).toString(),
        ).jsonObject
        assertEquals("\"+77001234567\"", json["phone_number"].toString())
        assertEquals("null", json["userId"].toString())
    }
}

/** Кеш шегі: Flutter _maxPendingEvents=100 — ең ескісі ысырылады. */
class TrimPendingTest {

    private fun ev(n: Int) = event(eventName = "e$n")

    private fun event(eventName: String): MonitoringEvent = MonitoringEvent(
        eventType = MonitoringEventType.pageView,
        eventName = eventName,
        data = null,
        timestamp = "t",
        userId = null,
        phoneNumber = "",
        deviceId = null,
        pageName = null,
        searchQuery = null,
        categoryId = null,
        apiEndpoint = null,
        httpMethod = null,
        statusCode = null,
        errorMessage = null,
        amount = null,
        currency = null,
        success = null,
        error = null,
    )

    @Test
    fun `шектен аз — өзгеріссіз`() {
        val list = listOf(ev(1), ev(2), ev(3))
        assertEquals(list, trimPending(list, 100))
    }

    @Test
    fun `шектен асқанда — ең ескісі ысырылады`() {
        val list = (1..150).map { ev(it) }
        val trimmed = trimPending(list, 100)
        assertEquals(100, trimmed.size)
        assertEquals("e51", trimmed.first().eventName)
        assertEquals("e150", trimmed.last().eventName)
    }

    @Test
    fun `дәл шек — өзгеріссіз`() {
        val list = (1..100).map { ev(it) }
        assertEquals(list, trimPending(list, 100))
    }
}

/** TikTok оқиға картасы — iOS каналымен бірдеей атаулар. */
class TikTokEventMapTest {

    @Test
    fun `Dart оқиға атаулары TikTok standard атауларына аударылады`() {
        val map = TikTokAnalytics().eventMap
        assertEquals("Login", map["login"])
        assertEquals("CompleteRegistration", map["completeRegistration"])
        assertEquals("CompletePayment", map["purchase"])
        assertEquals("LaunchAPP", map["launchApp"])
    }

    @Test
    fun `картада небәре 4 конверсия бар`() {
        assertEquals(4, TikTokAnalytics().eventMap.size)
    }

    @Test
    fun `белгісіз оқиға атауы өзгеріссіз қалады ( trackEvent mapping арқылы )`() {
        val analytics = TikTokAnalytics()
        assertTrue(analytics.eventMap["custom_event"] == null)
    }
}