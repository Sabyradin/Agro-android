package com.agroland.core.network.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiErrorParserTest {

    @Test
    fun `parses error_code and message`() {
        val body = """{"error_code":"INVALID_PHONE_FORMAT","message":"Телефон форматы дұрыс емес"}"""
        val error = ApiErrorParser.parse(400, body)
        assertEquals("INVALID_PHONE_FORMAT", error.code)
        assertEquals("Телефон форматы дұрыс емес", error.message)
        assertEquals(400, error.httpStatus)
    }

    @Test
    fun `falls back to detail when message missing`() {
        val body = """{"detail":"Not found"}"""
        val error = ApiErrorParser.parse(404, body)
        assertEquals("Not found", error.message)
    }

    @Test
    fun `parses field errors`() {
        val body = """{"error_code":"VALIDATION_ERROR","message":"Проверьте поля","fields":{"bin":"Должен быть 12 цифр"}}"""
        val error = ApiErrorParser.parse(422, body)
        assertEquals(1, error.fields.size)
        assertEquals("Должен быть 12 цифр", error.fields["bin"])
    }

    @Test
    fun `garbage body yields code null`() {
        val error = ApiErrorParser.parse(500, "<html>server error</html>")
        assertEquals(null, error.code)
        assertEquals(500, error.httpStatus)
    }

    @Test
    fun `tariff code detectable`() {
        val error = ApiErrorParser.parse(403, """{"error_code":"TARIFF_LIMIT_ANNOUNCEMENTS","message":"Лимит тарифа"}""")
        assertTrue(error.code!!.startsWith("TARIFF_LIMIT_"))
    }
}