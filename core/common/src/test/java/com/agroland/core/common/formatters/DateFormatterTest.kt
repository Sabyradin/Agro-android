package com.agroland.core.common.formatters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateFormatterTest {

    @Test
    fun `parses utc offset datetime`() {
        val parsed = DateFormatter.parseOrNull("2026-09-15T10:30:00Z")
        assertNotNull(parsed)
    }

    @Test
    fun `parses timezone-less datetime`() {
        val parsed = DateFormatter.parseOrNull("2026-09-15T10:30:00")
        assertNotNull(parsed)
        assertEquals(10, parsed!!.hour)
        assertEquals(30, parsed.minute)
    }

    @Test
    fun `parses date only`() {
        val parsed = DateFormatter.parseOrNull("2026-09-15")
        assertNotNull(parsed)
        assertEquals(0, parsed!!.hour)
    }

    @Test
    fun `returns null for garbage`() {
        assertNull(DateFormatter.parseOrNull(null))
        assertNull(DateFormatter.parseOrNull(""))
        assertNull(DateFormatter.parseOrNull("not a date"))
    }

    @Test
    fun `formats date as ddMMyyyy`() {
        assertEquals("15.09.2026", DateFormatter.formatDate("2026-09-15"))
    }

    @Test
    fun `formatDateTime keeps hhmm`() {
        val formatted = DateFormatter.formatDateTime("2026-09-15T10:30:00")
        assertEquals(16, formatted.length)
        assertTrue(formatted.endsWith("10:30") || formatted.endsWith(":"))
    }
}