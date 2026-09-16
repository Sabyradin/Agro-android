package com.agroland.feature.chat.data

import com.agroland.core.network.ApiResult
import com.agroland.feature.location.data.LocationRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Reverse geocode (Flutter NominatimGeocoder): GET nominatim.openstreetmap.org
 * /reverse (format=jsonv2, zoom=18, addressdetails=1, accept-language,
 * User-Agent 'AgroLandApp/1.0 (mobile)'), 4с/5с timeout, 5 dp координат
 * кэші. Сәтсіз болса — backend GET /location/reverse (region+district)
 * fallback, одан да сәтсіз — null (чatta "lat, lng" көрінеді).
 */
@Singleton
class ChatGeocoder @Inject constructor(
    private val locationRepository: LocationRepository,
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val cache = HashMap<String, String>()

    /** Шығу/аккаунт ауысу кезінде кэшті тазарту. */
    fun clearCache() = cache.clear()

    /** Координат → адам оқитын мекенжай атауы; табылмаса null. */
    suspend fun reverseGeocode(lat: Double, lng: Double, language: String): String? =
        withContext(Dispatchers.IO) {
            val key = "%.5f,%.5f".format(lat, lng)
            cache[key]?.let { return@withContext it }

            val result = nominatim(lat, lng, language) ?: backendFallback(lat, lng)
            if (result != null) {
                cache[key] = result
                if (cache.size > 100) cache.remove(cache.keys.first())
            }
            result
        }

    private fun nominatim(lat: Double, lng: Double, language: String): String? = try {
        val url = "https://nominatim.openstreetmap.org/reverse" +
            "?format=jsonv2&lat=$lat&lon=$lng&zoom=18&addressdetails=1" +
            "&accept-language=${encodeURIComponent(language)}"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val root = com.agroland.core.network.json.JsonParser
                .json.parseToJsonElement(body).jsonObject
            val address = (root["address"] as? kotlinx.serialization.json.JsonObject) ?: return null
            val parts = buildList {
                address.str("road")?.let(::add)
                address.str("pedestrian")?.let(::add)
                address.str("footway")?.let(::add)
                address.str("street")?.let(::add)
                address.str("house_number")?.let(::add)
                listOf("city", "town", "village", "hamlet", "suburb", "municipality")
                    .firstNotNullOfOrNull { address.str(it) }
                    ?.let(::add)
            }
            // Road/house/city тізбегі бос болса — name өрісі.
            parts.filter { it.isNotBlank() }.joinToString(", ")
                .ifEmpty { address.str("name") }
                .takeIf { !it.isNullOrBlank() }
        }
    } catch (_: Exception) {
        null
    }

    private suspend fun backendFallback(lat: Double, lng: Double): String? = try {
        when (val result = locationRepository.reverseGeocode(lat, lng)) {
            is ApiResult.Success -> {
                val value = result.value ?: return null
                listOfNotNull(value.regionName, value.districtName)
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                    .takeIf { it.isNotEmpty() }
            }
            is ApiResult.Error -> null
        }
    } catch (_: Exception) {
        null
    }

    private fun kotlinx.serialization.json.JsonObject.str(key: String): String? =
        (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.content

    private fun encodeURIComponent(s: String): String =
        java.net.URLEncoder.encode(s, "UTF-8")

    private companion object {
        const val USER_AGENT = "AgroLandApp/1.0 (mobile)"
    }
}