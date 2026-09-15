package com.agroland.core.common.formatters

import org.junit.Assert.assertEquals
import org.junit.Test

class PriceFormatterTest {

    @Test
    fun `formats millions with space grouping and tenge sign`() {
        assertEquals("1 250 000 ₸", PriceFormatter.format(1250000.0))
    }

    @Test
    fun `formats small price`() {
        assertEquals("42 ₸", PriceFormatter.format(42.0))
    }

    @Test
    fun `formats zero`() {
        assertEquals("0 ₸", PriceFormatter.format(0.0))
    }

    @Test
    fun `null and non-finite return empty`() {
        assertEquals("", PriceFormatter.format(null))
        assertEquals("", PriceFormatter.format(Double.NaN))
        assertEquals("", PriceFormatter.format(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `plain without currency`() {
        assertEquals("1 250 000", PriceFormatter.format(1250000.0, currency = ""))
    }

    @Test
    fun `plain long grouping`() {
        assertEquals("12 345", PriceFormatter.formatPlain(12345))
    }

    @Test
    fun `precise keeps fraction only when needed`() {
        assertEquals("1 234,5 ₸", PriceFormatter.formatPrecise(1234.5))
        assertEquals("1 234 ₸", PriceFormatter.formatPrecise(1234.0))
    }
}