package com.agroland.feature.auth.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * HATEOAS `task_id` шығару регрессиясы.
 *
 * DEV backend `links`-ті МАССИВ түрінде береді — бұрын код оны тек объект
 * ретінде оқитын да, кіру бірінші қадамнан әрі жүрмейтін (ISSUES #73).
 */
class AuthTaskIdTest {

    private fun json(raw: String): JsonObject =
        Json.parseToJsonElement(raw) as JsonObject

    @Test
    fun `links massив bolganda task_id href-ten alynady`() {
        val root = json(
            """
            {"links":[
              {"name":"mfaSmsRequest","href":"/auth/mfa/e731e770-9555-4474-b217-c9835815ba0c","verb":"GET"},
              {"name":"mfaSmsSubmit","href":"/auth/mfa/e731e770-9555-4474-b217-c9835815ba0c","verb":"POST"}
            ]}
            """.trimIndent(),
        )
        assertEquals(
            "e731e770-9555-4474-b217-c9835815ba0c",
            AuthTaskIdParser.extract(root),
        )
    }

    @Test
    fun `tikelei task_id orisi basymdyqqa ie`() {
        val root = json("""{"task_id":"abc","links":[{"href":"/auth/mfa/xyz"}]}""")
        assertEquals("abc", AuthTaskIdParser.extract(root))
    }

    @Test
    fun `links obekt bolganda da oqylady`() {
        val root = json("""{"links":{"mfaSmsRequest":{"href":"/auth/mfa/obj-task"}}}""")
        assertEquals("obj-task", AuthTaskIdParser.extract(root))
    }

    @Test
    fun `tolyq URL men query boligi kesiledi`() {
        val root = json(
            """{"links":[{"href":"https://api.example.com/api/v1/auth/mfa/task-42?lang=kk"}]}""",
        )
        assertEquals("task-42", AuthTaskIdParser.extract(root))
    }

    @Test
    fun `mfa siltemesi joq bolsa null`() {
        val root = json("""{"links":[{"name":"self","href":"/auth/login"}]}""")
        assertNull(AuthTaskIdParser.extract(root))
    }
}
