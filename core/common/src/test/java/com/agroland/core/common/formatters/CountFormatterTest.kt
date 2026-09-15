package com.agroland.core.common.formatters

import org.junit.Assert.assertEquals
import org.junit.Test

class CountFormatterTest {

    @Test
    fun `small counts stay as is`() {
        assertEquals("0", CountFormatter.formatCompact(0))
        assertEquals("42", CountFormatter.formatCompact(42))
        assertEquals("999", CountFormatter.formatCompact(999))
    }

    @Test
    fun `thousands use K with comma`() {
        assertEquals("1K", CountFormatter.formatCompact(1000))
        assertEquals("1,2K", CountFormatter.formatCompact(1234))
        assertEquals("999,9K", CountFormatter.formatCompact(999_999))
    }

    @Test
    fun `millions use M with comma`() {
        assertEquals("2,5M", CountFormatter.formatCompact(2_500_000))
        assertEquals("1M", CountFormatter.formatCompact(1_000_000))
    }

    @Test
    fun `null becomes zero`() {
        assertEquals("0", CountFormatter.formatCompact(null))
    }
}