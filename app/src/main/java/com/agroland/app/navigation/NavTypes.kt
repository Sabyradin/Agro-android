package com.agroland.app.navigation

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import com.agroland.feature.marketplace.data.AnnouncementFilter
import com.agroland.feature.notifications.data.NotificationItem
import com.agroland.feature.location.data.SelectedLocation
import kotlin.reflect.typeOf
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** AnnouncementFilter маршрут аргументі — JSON ретінде сериализацияланады. */
private val AnnouncementFilterNavType = object : NavType<AnnouncementFilter>(isNullableAllowed = false) {
    private val json = Json { ignoreUnknownKeys = true }

    override fun get(bundle: Bundle, key: String): AnnouncementFilter? =
        bundle.getString(key)?.let { parseValue(it) }

    override fun put(bundle: Bundle, key: String, value: AnnouncementFilter) =
        bundle.putString(key, json.encodeToString(AnnouncementFilter.serializer(), value))

    override fun parseValue(value: String): AnnouncementFilter =
        json.decodeFromString(AnnouncementFilter.serializer(), Uri.decode(value))

    override fun serializeAsValue(value: AnnouncementFilter): String =
        Uri.encode(json.encodeToString(AnnouncementFilter.serializer(), value))
}

/** AnnouncementsListRoute / FilterRoute үшін typeMap. */
val AnnouncementFilterTypeMap = mapOf(typeOf<AnnouncementFilter>() to AnnouncementFilterNavType)

private val filterResultJson = Json { ignoreUnknownKeys = true }

/**
 * Сүзгі нәтижесі savedStateHandle арқылы қайтады, ал ол тек Bundle-ге
 * сыятын түрлерді қабылдайды (AnnouncementFilter — Parcelable емес, сондықтан
 * тікелей жазу IllegalArgumentException береді). Сол себепті JSON жол күйінде
 * сақталады.
 */
fun encodeFilterResult(filter: AnnouncementFilter): String =
    filterResultJson.encodeToString(AnnouncementFilter.serializer(), filter)

/** [encodeFilterResult] жазған жолды қайта оқу; бүлінген мән — null. */
fun decodeFilterResult(value: String?): AnnouncementFilter? {
    if (value.isNullOrBlank()) return null
    return runCatching { filterResultJson.decodeFromString(AnnouncementFilter.serializer(), value) }.getOrNull()
}

/** NotificationItem маршрут аргументі — JSON ретінде сериализацияланады. */
private val NotificationItemNavType = object : NavType<NotificationItem>(isNullableAllowed = false) {
    private val json = Json { ignoreUnknownKeys = true }

    override fun get(bundle: Bundle, key: String): NotificationItem? =
        bundle.getString(key)?.let { parseValue(it) }

    override fun put(bundle: Bundle, key: String, value: NotificationItem) =
        bundle.putString(key, json.encodeToString(NotificationItem.serializer(), value))

    override fun parseValue(value: String): NotificationItem =
        json.decodeFromString(NotificationItem.serializer(), Uri.decode(value))

    override fun serializeAsValue(value: NotificationItem): String =
        Uri.encode(json.encodeToString(NotificationItem.serializer(), value))
}

/** SingleNotificationRoute үшін typeMap. */
val NotificationItemTypeMap = mapOf(typeOf<NotificationItem>() to NotificationItemNavType)

/** SelectedLocation маршрут аргументі (nullable) — JSON ретінде сериализацияланады. */
private val SelectedLocationNavType = object : NavType<SelectedLocation?>(isNullableAllowed = true) {
    private val json = Json { ignoreUnknownKeys = true }

    override fun get(bundle: Bundle, key: String): SelectedLocation? =
        bundle.getString(key)?.let { parseValue(it) }

    override fun put(bundle: Bundle, key: String, value: SelectedLocation?) {
        if (value != null) {
            bundle.putString(key, json.encodeToString(SelectedLocation.serializer(), value))
        }
    }

    override fun parseValue(value: String): SelectedLocation? =
        if (value.isEmpty()) null
        else json.decodeFromString(SelectedLocation.serializer(), Uri.decode(value))

    override fun serializeAsValue(value: SelectedLocation?): String =
        if (value == null) "" else Uri.encode(json.encodeToString(SelectedLocation.serializer(), value))
}

/** LocationSelectionRoute үшін typeMap. */
val SelectedLocationTypeMap = mapOf(typeOf<SelectedLocation?>() to SelectedLocationNavType)

/** List<String> маршрут аргументі (PhotoViewerRoute) — JSON массиві ретінде. */
private val StringListNavType = object : NavType<List<String>>(isNullableAllowed = false) {
    private val json = Json
    private val listSerializer = ListSerializer(String.serializer())

    override fun get(bundle: Bundle, key: String): List<String>? =
        bundle.getString(key)?.let { parseValue(it) }

    override fun put(bundle: Bundle, key: String, value: List<String>) =
        bundle.putString(key, json.encodeToString(listSerializer, value))

    override fun parseValue(value: String): List<String> =
        json.decodeFromString(listSerializer, Uri.decode(value))

    override fun serializeAsValue(value: List<String>): String =
        Uri.encode(json.encodeToString(listSerializer, value))
}

/** PhotoViewerRoute үшін typeMap. */
val StringListTypeMap = mapOf(typeOf<List<String>>() to StringListNavType)
