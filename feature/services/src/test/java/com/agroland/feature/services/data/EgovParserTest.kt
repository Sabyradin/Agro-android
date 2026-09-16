package com.agroland.feature.services.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * EgovParser + статикалық каталог — /agri-machinery/{vin} конверті
 * (`{status, code, data, message}`) мен Flutter egovServices көшірмесі.
 */
class EgovParserTest {

    private fun obj(raw: String) = Json.parseToJsonElement(raw).jsonObject

    @Test
    fun `SCSE002 қате конверті NotFound + backend message береді`() {
        val result = EgovParser.parseMachineryLookup(
            obj(
                """{"status": "error", "code": "SCSE002",
                    "message": "По указанному VIN запись не найдена", "data": null}""",
            ),
        )
        assertTrue(result is EgovMachineryLookup.NotFound)
        assertEquals(
            "По указанному VIN запись не найдена",
            (result as EgovMachineryLookup.NotFound).message,
        )
    }

    @Test
    fun `status error data-мен де NotFound береді`() {
        val result = EgovParser.parseMachineryLookup(
            obj("""{"status": "error", "message": "Деректер қолжетімсіз", "data": {"x": "1"}}"""),
        )
        assertTrue(result is EgovMachineryLookup.NotFound)
    }

    @Test
    fun `data primitive жолдары key-value тізіміне айналады`() {
        val result = EgovParser.parseMachineryLookup(
            obj(
                """{"status": "ok", "data": {
                    "vin": "XTA12345678901234", "mark": "МТЗ", "model": "80",
                    "year": "1985", "empty": "",
                    "owner": {"name": "АШ"}, "list": [1, 2]
                }}""",
            ),
        )
        assertTrue(result is EgovMachineryLookup.Found)
        val fields = (result as EgovMachineryLookup.Found).fields
        // Бос primitive түспейді, объект JSON строкасына айналады, массив түспейді.
        assertEquals(
            listOf(
                "vin" to "XTA12345678901234",
                "mark" to "МТЗ",
                "model" to "80",
                "year" to "1985",
                "owner" to """{"name":"АШ"}""",
            ),
            fields,
        )
    }

    @Test
    fun `data конвертсіз тамыр объекті де өңделеді`() {
        val result = EgovParser.parseMachineryLookup(obj("""{"data": {"vin": "V1"}}"""))
        assertTrue(result is EgovMachineryLookup.Found)
    }

    @Test
    fun `egovServices каталогы Flutter-мен сайткес 83 жазба`() {
        assertEquals(83, egovServices.size)
        // Барлық code бірегей.
        assertEquals(egovServices.size, egovServices.map { it.code }.toSet().size)
        // Flutter egov_service_model.dart ішіндегі белгілі жазбалар 1:1 тұр.
        assertTrue(egovServices.any { it.code == "GRST_TI" && it.category == "МСХ РК" })
        assertTrue(egovServices.any { it.code == "epts_green" && it.category == "ЭПТС" })
    }
}