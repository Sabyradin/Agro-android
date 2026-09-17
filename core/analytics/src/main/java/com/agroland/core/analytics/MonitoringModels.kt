package com.agroland.core.analytics

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Мониторинг оқиғасы — Flutter monitoring_event.dart 1:1 (Фаза 19).
 * JSON кілттері freezed json_serializable шығарымымен тең: phone_number,
 * timestamp, қалғандары camelCase; eventType enum атауы string ретінде.
 */
data class MonitoringEvent(
    val eventType: MonitoringEventType,
    val eventName: String,
    val data: Map<String, JsonElement>? = null,
    val timestamp: String,
    val userId: String? = null,
    val phoneNumber: String,
    val deviceId: String? = null,
    val pageName: String? = null,
    val searchQuery: String? = null,
    val categoryId: String? = null,
    val apiEndpoint: String? = null,
    val httpMethod: String? = null,
    val statusCode: Int? = null,
    val errorMessage: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val success: Boolean? = null,
    val error: String? = null,
)

/** Оқиға түрлері — Flutter MonitoringEventType enum 1:1. */
enum class MonitoringEventType {
    pageView,
    search,
    categoryView,
    apiRequestSuccess,
    apiRequestFailure,
    login,
    logout,
    registration,
    advertise,
    balanceTopUp,
    appOpen,
    appClose,
}

/**
 * Event → JSON (таза функция — MonitoringEventJsonTest-пен қапталады).
 * Flutter toJson шығарымымен бір кілт бір мән: {eventType, eventName, data,
 * timestamp, userId, phone_number, deviceId, pageName, searchQuery,
 * categoryId, apiEndpoint, httpMethod, statusCode, errorMessage, amount,
 * currency, success, error} — null өрістер JsonNull (freezed parity).
 */
fun monitoringEventToJson(event: MonitoringEvent): JsonObject = buildJsonObject {
    put("eventType", event.eventType.name)
    put("eventName", event.eventName)
    put("data", event.data?.let { JsonObject(it) } ?: JsonNull)
    put("timestamp", event.timestamp)
    put("userId", event.userId.json())
    put("phone_number", event.phoneNumber)
    put("deviceId", event.deviceId.json())
    put("pageName", event.pageName.json())
    put("searchQuery", event.searchQuery.json())
    put("categoryId", event.categoryId.json())
    put("apiEndpoint", event.apiEndpoint.json())
    put("httpMethod", event.httpMethod.json())
    put("statusCode", event.statusCode.json())
    put("errorMessage", event.errorMessage.json())
    put("amount", event.amount.json())
    put("currency", event.currency.json())
    put("success", event.success.json())
    put("error", event.error.json())
}

private fun String?.json(): JsonElement = this?.let { JsonPrimitive(it) } ?: JsonNull

private fun Int?.json(): JsonElement = this?.let { JsonPrimitive(it) } ?: JsonNull

private fun Double?.json(): JsonElement = this?.let { JsonPrimitive(it) } ?: JsonNull

private fun Boolean?.json(): JsonElement = this?.let { JsonPrimitive(it) } ?: JsonNull