package com.agroland.feature.location.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * LocationParser тесттері — DEV backend жауап пішіндерімен (curl арқылы
 * тексерілген) сәйкестігі: countries/regions/districts + reverse geocode.
 */
class LocationParserTest {

    private fun obj(raw: String) = Json.parseToJsonElement(raw).jsonObject

    @Test
    fun `countries тізімін оқиды — country_id жоқ, parentId = null`() {
        val root = obj(
            """
            {"countries":[
              {"id":1,"name_kz":"Қазақстан","name_ru":"Казахстан","name_en":"Kazakhstan","name_zh":"哈萨克斯坦","name_ky":"Кыргыз"},
              {"id":2,"name_kz":"Қытай","name_ru":"Китай","name_en":"China","name_zh":"中国"}
            ]}
            """.trimIndent(),
        )
        val countries = LocationParser.parseCountries(root)
        assertEquals(2, countries.size)
        val kz = countries.first()
        assertEquals(1, kz.id)
        assertNull(kz.parentId)
        assertEquals("Қазақстан", kz.nameKk)
        assertEquals("Казахстан", kz.nameRu)
        assertEquals("Kazakhstan", kz.nameEn)
        assertEquals("哈萨克斯坦", kz.nameZh)
    }

    @Test
    fun `regions country_id және координаталарды оқиды`() {
        val root = obj(
            """
            {"regions":[
              {"id":10,"country_id":1,"name_kz":"Астана","name_ru":"Астана","latitude":51.1282,"longitude":71.4307}
            ]}
            """.trimIndent(),
        )
        val regions = LocationParser.parseRegions(root)
        assertEquals(1, regions.size)
        val r = regions.first()
        assertEquals(10, r.id)
        assertEquals(1, r.parentId)
        assertEquals(51.1282, r.latitude)
        assertEquals(71.4307, r.longitude)
    }

    @Test
    fun `districts region_id оқиды және name_ch fallback жұмыс істейді`() {
        val root = obj(
            """
            {"districts":[
              {"id":100,"region_id":10,"name_kz":"Есіл","name_ch":"叶西尔"}
            ]}
            """.trimIndent(),
        )
        val districts = LocationParser.parseDistricts(root)
        val d = districts.single()
        assertEquals(100, d.id)
        assertEquals(10, d.parentId)
        assertEquals("Есіл", d.nameKk)
        assertEquals("叶西尔", d.nameZh)
    }

    @Test
    fun `id жоқ жазулар қағылады`() {
        val root = obj("""{"countries":[{"name_kz":"Жоқ id"}]}""")
        assertTrue(LocationParser.parseCountries(root).isEmpty())
    }

    @Test
    fun `reverse geocode data ішінен каталог ID-лерін оқиды`() {
        val root = obj(
            """
            {"data":{"country_id":1,"country_ru":"Казахстан","region_id":10,
             "region_ru":"Астана","district_id":100,"district_ru":"Есіл",
             "nominatim_raw":{"display_name":"test"}}}
            """.trimIndent(),
        )
        val rev = LocationParser.parseReverseGeocode(root)!!
        assertEquals(1, rev.countryId)
        assertEquals(10, rev.regionId)
        assertEquals(100, rev.districtId)
        assertEquals("Казахстан", rev.countryName)
        assertEquals("Астана", rev.regionName)
        assertEquals("Есіл", rev.districtName)
        assertTrue(rev.hasCatalogMatch)
    }

    @Test
    fun `reverse geocode түбірде де (data жоқ) оқылады, сәйкестік жоқ`() {
        val root = obj("""{"country_id":null,"region_id":null,"district_id":null}""")
        val rev = LocationParser.parseReverseGeocode(root)!!
        assertNull(rev.countryId)
        assertNull(rev.regionId)
        assertFalse(rev.hasCatalogMatch)
    }

    @Test
    fun `localizedName тілге сай атауды таңдайды, fallbacks істейді`() {
        val loc = CatalogLocation(
            id = 1,
            parentId = null,
            nameKk = "Қазақстан",
            nameRu = "Казахстан",
            nameEn = null,
            nameZh = null,
            latitude = null,
            longitude = null,
        )
        assertEquals("Казахстан", loc.localizedName("ru"))
        assertEquals("Казахстан", loc.localizedName("ru-RU"))
        assertEquals("Қазақстан", loc.localizedName("en")) // en → nameKk fallback
        assertEquals("Қазақстан", loc.localizedName("kk"))
        assertEquals("Қазақстан", loc.localizedName(null))
    }

    @Test
    fun `displayLabel «Облыс · Аудан» пішімін құрады, бос болса ел атауы`() {
        val full = SelectedLocation(
            countryId = 1,
            countryName = "Қазақстан",
            regionId = 10,
            regionName = "Астана",
            districtId = 100,
            districtName = "Есіл",
        )
        assertEquals("Астана · Есіл", full.displayLabel())
        assertTrue(full.isNotEmpty)

        val onlyCountry = SelectedLocation(countryId = 1, countryName = "Қазақстан")
        assertEquals("Қазақстан", onlyCountry.displayLabel())
        assertTrue(onlyCountry.isNotEmpty)

        val empty = SelectedLocation()
        assertEquals("", empty.displayLabel())
        assertFalse(empty.isNotEmpty)
    }

    @Test
    fun `SelectedLocation java Serializable — savedStateHandle Bundle үшін`() {
        val loc = SelectedLocation(countryId = 1, regionId = 10)
        assertTrue(loc is java.io.Serializable)
    }
}