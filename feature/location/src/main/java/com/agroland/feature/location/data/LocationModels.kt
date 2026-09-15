package com.agroland.feature.location.data

import com.agroland.core.network.json.JsonParser
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Локация моделдері — LocationCountryModel/LocationRegionModel/LocationDistrictModel +
 * ReverseGeocodeResult (Flutter → Android порты, Фаза 7).
 *
 * DEV шектелген жауап пішімдері (нақты тексерілді):
 *  - GET /countries → {"countries":[{id, name_kz, name_en, name_zh, name_ru, name_ky, name_uz}]}
 *  - GET /regions?country_id= → {"regions":[{id, country_id, name_*, latitude, longitude}]}
 *  - GET /districts?region_id=|country_id= → {"districts":[{id, region_id, name_*, latitude, longitude}]}
 *  - GET /location/reverse?lat=&lng= → {country_id, country_ru, region_id, region_ru,
 *    district_id, district_ru, nominatim_raw}
 */

/** Каталог жолы — ел, облыс немесе аудан (бірдей пішім, өріс аты ғана ерекше). */
data class CatalogLocation(
    val id: Int,
    val parentId: Int?,
    val nameKk: String?,
    val nameRu: String?,
    val nameEn: String?,
    val nameZh: String?,
    val latitude: Double?,
    val longitude: Double?,
) {
    /** Ағымдағы тілге сай атау — Category.localizedName үлгісімен бірдей. */
    fun localizedName(localeTag: String?): String {
        val lang = localeTag?.substringBefore('-')?.lowercase()
        return when (lang) {
            "ru" -> nameRu ?: nameKk ?: ""
            "en" -> nameEn ?: nameKk ?: ""
            "zh" -> nameZh ?: nameKk ?: ""
            else -> nameKk ?: nameRu ?: nameEn ?: nameZh ?: ""
        }
    }
}

/**
 * Reverse geocode нәтижесі — каталог ID-лері + орысша атаулар (backend Nominatim
 * проксисі fuzzy match арқылы Region/District кестелерімен сәйкестендіреді).
 */
data class ReverseGeocodeResult(
    val countryId: Int?,
    val regionId: Int?,
    val districtId: Int?,
    val countryName: String?,
    val regionName: String?,
    val districtName: String?,
) {
    val hasCatalogMatch: Boolean get() = countryId != null || regionId != null
}

/**
 * Таңдалған локация — навигация нәтижесі ретінде жіберіледі (LocationSelectionPage,
 * CountryListPage/RegionListPage/DistrictListPage → caller). Кілт — төмендегі
 * LOCATION_RESULT_KEY; savedStateHandle арқылы өтеді, сондықтан java.io.Serializable.
 */
@Serializable
data class SelectedLocation(
    val countryId: Int? = null,
    val countryName: String? = null,
    val regionId: Int? = null,
    val regionName: String? = null,
    val districtId: Int? = null,
    val districtName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
) : java.io.Serializable {
    val isNotEmpty: Boolean get() = countryId != null || regionId != null || districtId != null

    /** Сүзгі жолындағы көрсетілетін мәтін: «Облыс · Аудан». */
    fun displayLabel(): String = listOfNotNull(regionName?.takeIf { it.isNotBlank() }, districtName?.takeIf { it.isNotBlank() })
        .joinToString(" · ")
        .ifBlank { countryName.orEmpty() }
}

/** Парсер — бір ғана орыннан. */
object LocationParser {

    /** {countries:[...]} — country_id өрісі жоқ (parentId = null). */
    fun parseCountries(root: JsonObject?): List<CatalogLocation> =
        parseList(root, "countries", parentKey = null)

    /** {regions:[...]} — parentKey = country_id. */
    fun parseRegions(root: JsonObject?): List<CatalogLocation> =
        parseList(root, "regions", parentKey = "country_id")

    /** {districts:[...]} — parentKey = region_id. */
    fun parseDistricts(root: JsonObject?): List<CatalogLocation> =
        parseList(root, "districts", parentKey = "region_id")

    private fun parseList(root: JsonObject?, key: String, parentKey: String?): List<CatalogLocation> {
        val elements = JsonParser.arrayOrSingle(root, key)
        return elements.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val id = JsonParser.int(obj, "id") ?: return@mapNotNull null
            CatalogLocation(
                id = id,
                parentId = parentKey?.let { JsonParser.int(obj, it) },
                nameKk = JsonParser.string(obj, "name_kz") ?: JsonParser.string(obj, "name_kk"),
                nameRu = JsonParser.string(obj, "name_ru"),
                nameEn = JsonParser.string(obj, "name_en"),
                nameZh = JsonParser.string(obj, "name_zh") ?: JsonParser.string(obj, "name_ch"),
                latitude = JsonParser.double(obj, "latitude"),
                longitude = JsonParser.double(obj, "longitude"),
            )
        }
    }

    /** /location/reverse жауабы — country_id/region_id/district_id + *_ru атаулары. */
    fun parseReverseGeocode(root: JsonObject?): ReverseGeocodeResult? {
        if (root == null) return null
        val inner = JsonParser.obj(root, "data") ?: root
        return ReverseGeocodeResult(
            countryId = JsonParser.int(inner, "country_id"),
            regionId = JsonParser.int(inner, "region_id"),
            districtId = JsonParser.int(inner, "district_id"),
            countryName = JsonParser.string(inner, "country_ru") ?: JsonParser.string(inner, "country"),
            regionName = JsonParser.string(inner, "region_ru") ?: JsonParser.string(inner, "region"),
            districtName = JsonParser.string(inner, "district_ru") ?: JsonParser.string(inner, "district"),
        )
    }
}

/** Нәтиже кілті — LocationSelectionPage/каталог беттері caller-ге осылай жауап береді. */
const val LOCATION_RESULT_KEY = "selected_location"