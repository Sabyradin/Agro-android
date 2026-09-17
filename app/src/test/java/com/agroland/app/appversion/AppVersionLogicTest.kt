package com.agroland.app.appversion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Фаза 20: force-update шешім-логикасы — Flutter app_version_service
 * checkAndShowUpdateDialog қадамдары 1:1 (таза функциялар, JVM-тесттер).
 */
class AppVersionLogicTest {

    private fun model(
        version: String? = "1.1.0",
        minVersion: String? = null,
        forceUpdate: Boolean = false,
        updateRequired: Boolean = false,
    ) = AppVersionModel(
        platform = "android",
        version = version,
        minVersion = minVersion,
        forceUpdate = forceUpdate,
        updateMessage = "message",
        updateRequired = updateRequired,
        isActive = true,
    )

    // ── compareVersions (Flutter _compareVersions 1:1) ──

    @Test
    fun `версия салыстыру — тоқсан сандар, тең емес, тең`() {
        assertEquals(-1, compareVersions("1.0.31", "1.0.33"))
        assertEquals(1, compareVersions("1.0.33", "1.0.31"))
        assertEquals(0, compareVersions("1.0.33", "1.0.33"))
    }

    @Test
    fun `версия салыстыру — әртүрлі ұзындық нөлдермен теңестіріледі`() {
        assertEquals(-1, compareVersions("1.2", "1.2.1"))
        assertEquals(0, compareVersions("1.2", "1.2.0"))
        assertEquals(1, compareVersions("2", "1.9.9"))
    }

    @Test
    fun `версия салыстыру — бузау белгілері 0 болып есептеледі`() {
        assertEquals(-1, compareVersions("1.x.9", "1.1.0"))
        assertEquals(0, compareVersions("1.x", "1.0.0"))
    }

    // ── decideUpdate (Flutter шешім қадамдары) ──

    @Test
    fun `current жоғары target-тен — диалог жоқ`() {
        // 1.0.33 > target 1.1.0? Жоқ — алдымен нақты жоғары жағдай.
        assertNull(decideUpdate("1.2.0", model(version = "1.1.0"), lastShownVersion = null))
        assertNull(decideUpdate("1.1.1", model(version = "1.1.0"), lastShownVersion = "1.1.0"))
    }

    @Test
    fun `ескі нұсқа — update_required флагымен көрсетіледі`() {
        val decision = decideUpdate("1.0.0", model(version = "1.1.0", updateRequired = true), null)
        assertEquals("1.1.0", decision?.model?.version)
        assertEquals(false, decision?.isForceUpdate)
    }

    @Test
    fun `ескі нұсқа — force_update флагы міндетті етеді`() {
        val decision = decideUpdate(
            "1.0.0",
            model(version = "1.1.0", updateRequired = true, forceUpdate = true),
            lastShownVersion = null,
        )
        assertTrue(decision!!.isForceUpdate)
    }

    @Test
    fun `target тең + min_version қанағаттандырылады — диалог жоқ`() {
        // current 1.1.0 == target 1.1.0, min 1.0.0 → қанағаттанды.
        assertNull(decideUpdate("1.1.0", model(version = "1.1.0", minVersion = "1.0.0"), null))
    }

    @Test
    fun `target тең + current min_version-ден төмен — МІНДЕТТІ жаңарту`() {
        // Flutter: target нұсқасындағылар min_version-ді қанағаттандырмаса — force.
        val decision = decideUpdate(
            "1.1.0",
            model(version = "1.1.0", minVersion = "1.1.1"),
            lastShownVersion = null,
        )
        assertTrue(decision!!.isForceUpdate)
    }

    @Test
    fun `target тең + min берілмеген — сервер флагтары`() {
        assertNull(decideUpdate("1.1.0", model(version = "1.1.0", updateRequired = false), null))
        val d = decideUpdate("1.1.0", model(version = "1.1.0", updateRequired = true), null)
        assertEquals(false, d?.isForceUpdate)
        val forced = decideUpdate(
            "1.1.0",
            model(version = "1.1.0", updateRequired = true, forceUpdate = true),
            null,
        )
        assertTrue(forced!!.isForceUpdate)
    }

    @Test
    fun `version берілмеген — isTarget = update_required флагы`() {
        // version=null/empty → updateRequired флагы шешеді.
        assertNull(decideUpdate("1.0.0", model(version = null, updateRequired = false), null))
        assertEquals(false, decideUpdate("1.0.0", model(version = null, updateRequired = true), null)?.isForceUpdate)
    }

    @Test
    fun `міндетті емес жаңарту — last_shown_version дедупі`() {
        val m = model(version = "1.1.0", updateRequired = true)
        // Ешқашан көрсетілмеген → көрсетіледі.
        assertEquals(false, decideUpdate("1.0.0", m, lastShownVersion = null)?.isForceUpdate)
        // Бұрын көрсетілген нұсқа → үнсіз.
        assertNull(decideUpdate("1.0.0", m, lastShownVersion = "1.1.0"))
    }

    @Test
    fun `МІНДЕТТІ жаңарту — дедуп жоқ, қайта көрсетіледі`() {
        val m = model(version = "1.1.0", updateRequired = true, forceUpdate = true)
        val decision = decideUpdate("1.0.0", m, lastShownVersion = "1.1.0")
        assertTrue(decision!!.isForceUpdate)
    }

    // ── AppVersionParser (кешірімді парсинг) ──

    @Test
    fun `парсер — snake_case кілттер, null-өрістер, қаптамға tolerant`() {
        val json = kotlinx.serialization.json.Json.parseToJsonElement(
            """
            {"data": {"platform": "android", "version": "1.2.3",
             "min_version": "1.0.0", "force_update": true,
             "update_message": "msg", "update_required": true, "is_active": true}}
            """.trimIndent(),
        )
        val parsed = AppVersionParser.parse(json)!!
        assertEquals("android", parsed.platform)
        assertEquals("1.2.3", parsed.version)
        assertEquals("1.0.0", parsed.minVersion)
        assertTrue(parsed.forceUpdate)
        assertEquals("msg", parsed.updateMessage)
        assertTrue(parsed.updateRequired)
        assertTrue(parsed.isActive!!)
    }

    @Test
    fun `парсер — platform жоқ → null (диалог көрінбейді)`() {
        val json = kotlinx.serialization.json.Json.parseToJsonElement("""{"version": "1.0"}""")
        assertNull(AppVersionParser.parse(json))
    }

    @Test
    fun `парсер — минималды жауап (флагтар әдепкі false)`() {
        val json = kotlinx.serialization.json.Json.parseToJsonElement(
            """{"platform": "android"}""",
        )
        val parsed = AppVersionParser.parse(json)!!
        assertEquals("android", parsed.platform)
        assertNull(parsed.version)
        assertNull(parsed.minVersion)
        assertEquals(false, parsed.forceUpdate)
        assertEquals(false, parsed.updateRequired)
        assertNull(parsed.isActive)
    }
}