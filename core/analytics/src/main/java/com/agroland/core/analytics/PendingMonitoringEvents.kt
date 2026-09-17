package com.agroland.core.analytics

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Желі жоқ кездегі оқиға кеші — Flutter _pendingEventsKey 1:1: StringList
 * орнына бір JSON-массив жолы (DataStore preferences set-і жоқ). 100-ден
 * асқанда ең ескісі ысырылады (Flutter _maxPendingEvents).
 */
@Singleton
class PendingMonitoringEvents @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    suspend fun load(): List<MonitoringEvent> = dataStore.data.map { prefs ->
        prefs[KEY].orEmpty()
    }.first().let { raw ->
        raw.split('\n').filter { it.isNotBlank() }.mapNotNull { line ->
            try {
                eventFromJson(Json.parseToJsonElement(line).jsonObject)
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun save(events: List<MonitoringEvent>) {
        dataStore.edit { prefs ->
            prefs[KEY] = events.joinToString("\n") { monitoringEventToJson(it).toString() }
        }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(KEY) }
    }

    private fun eventFromJson(obj: JsonObject): MonitoringEvent? {
        val eventType = obj["eventType"]?.let { el ->
            el.toString().trim('"').let { name -> MonitoringEventType.entries.firstOrNull { it.name == name } }
        } ?: return null
        val eventName = obj["eventName"]?.toString()?.trim('"').orEmpty()
        if (eventName.isEmpty()) return null
        return MonitoringEvent(
            eventType = eventType,
            eventName = eventName,
            data = (obj["data"] as? JsonObject)?.let { it.let { map -> map.entries.associate { (k, v) -> k to v } } },
            timestamp = obj["timestamp"]?.toString()?.trim('"') ?: "",
            userId = obj["userId"]?.takeNullString(),
            phoneNumber = obj["phone_number"]?.takeNullString() ?: "",
            deviceId = obj["deviceId"]?.takeNullString(),
            pageName = obj["pageName"]?.takeNullString(),
            searchQuery = obj["searchQuery"]?.takeNullString(),
            categoryId = obj["categoryId"]?.takeNullString(),
            apiEndpoint = obj["apiEndpoint"]?.takeNullString(),
            httpMethod = obj["httpMethod"]?.takeNullString(),
            statusCode = obj["statusCode"]?.takeNullInt(),
            errorMessage = obj["errorMessage"]?.takeNullString(),
            amount = obj["amount"]?.takeNullDouble(),
            currency = obj["currency"]?.takeNullString(),
            success = obj["success"]?.takeNullBoolean(),
            error = obj["error"]?.takeNullString(),
        )
    }

    private fun kotlinx.serialization.json.JsonElement.takeNullString(): String? =
        (this as? kotlinx.serialization.json.JsonPrimitive)
            ?.takeIf { it !is kotlinx.serialization.json.JsonNull }
            ?.content

    private fun kotlinx.serialization.json.JsonElement.takeNullInt(): Int? =
        takeNullString()?.toIntOrNull()

    private fun kotlinx.serialization.json.JsonElement.takeNullDouble(): Double? =
        takeNullString()?.toDoubleOrNull()

    private fun kotlinx.serialization.json.JsonElement.takeNullBoolean(): Boolean? =
        takeNullString()?.toBooleanStrictOrNull()

    private companion object {
        val KEY = stringPreferencesKey("monitoring_pending_events")
    }
}