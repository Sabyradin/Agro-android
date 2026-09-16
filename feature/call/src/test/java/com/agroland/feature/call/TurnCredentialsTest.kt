package com.agroland.feature.call

import com.agroland.feature.call.data.TurnCredentials
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `GET /turn/credentials` жауабының парсингі (Flutter TurnCredentials.dart
 * паритеті): жарамды жауап, жоқ/бос urls, null түбір, ttl әдепкісі.
 */
class TurnCredentialsTest {

    private fun parse(raw: String): TurnCredentials? =
        TurnCredentials.fromBackendJson(Json.parseToJsonElement(raw))

    @Test
    fun `жарамды жауап — urls, username, credential, ttl оқылады`() {
        val creds = parse(
            """
            {"ttl": 1800, "turn": {"urls": ["turn:turn.agroland.kz:3478"],
              "username": "1730000000:42", "credential": "aGVsbG8="}}
            """.trimIndent(),
        )
        assertNotNull(creds)
        assertEquals(1800, creds!!.ttl)
        assertEquals(listOf("turn:turn.agroland.kz:3478"), creds.urls)
        assertEquals("1730000000:42", creds.username)
        assertEquals("aGVsbG8=", creds.credential)
    }

    @Test
    fun `urls бос тізім — null (STUN-only fallback)`() {
        assertNull(parse("""{"ttl": 3600, "turn": {"urls": [], "username": "u"}}"""))
    }

    @Test
    fun `turn нысаны жоқ — null`() {
        assertNull(parse("""{"ttl": 3600}"""))
    }

    @Test
    fun `түбір null — null`() {
        assertNull(TurnCredentials.fromBackendJson(null))
    }

    @Test
    fun `ttl жоқ — әдепкі 3600 (backend-test қайтпайды, бірақ келісім осы)`() {
        val creds = parse(
            """{"turn": {"urls": ["turn:t.kz:3478"], "username": "u", "credential": "c"}}""",
        )
        assertNotNull(creds)
        assertEquals(3600, creds!!.ttl)
    }

    @Test
    fun `urls бірнеше элементті — барлығы сақталады`() {
        val creds = parse(
            """
            {"ttl": 600, "turn": {"urls": ["turn:a.kz:3478", "turn:b.kz:3479"],
              "username": "u", "credential": "c"}}
            """.trimIndent(),
        )
        assertEquals(listOf("turn:a.kz:3478", "turn:b.kz:3479"), creds!!.urls)
    }
}