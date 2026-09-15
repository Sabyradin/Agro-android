package com.agroland.app.navigation

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import com.agroland.feature.marketplace.data.AnnouncementFilter
import kotlin.reflect.typeOf
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
