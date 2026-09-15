package com.agroland.core.l10n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLocaleTest {

    @Test
    fun `all four locales exist`() {
        assertEquals(4, AppLocale.entries.size)
    }

    @Test
    fun `tags match android conventions`() {
        assertEquals("kk", AppLocale.KK.tag)
        assertEquals("ru", AppLocale.RU.tag)
        assertEquals("en", AppLocale.EN.tag)
        assertEquals("zh", AppLocale.ZH.tag)
    }

    @Test
    fun `backend keys are remapped kk to kz and zh to ch`() {
        assertEquals("kz", AppLocale.KK.backendKey)
        assertEquals("ru", AppLocale.RU.backendKey)
        assertEquals("en", AppLocale.EN.backendKey)
        assertEquals("ch", AppLocale.ZH.backendKey)
    }

    @Test
    fun `fromTag parses known tags case-insensitively`() {
        assertEquals(AppLocale.KK, AppLocale.fromTag("kk"))
        assertEquals(AppLocale.RU, AppLocale.fromTag("RU"))
        assertEquals(AppLocale.ZH, AppLocale.fromTag(" zh "))
    }

    @Test
    fun `unknown tag falls back to KK`() {
        assertEquals(AppLocale.KK, AppLocale.fromTag("fr"))
        assertEquals(AppLocale.KK, AppLocale.fromTag(null))
    }

    @Test
    fun `fromTagOrNull returns null for unknown`() {
        assertNull(AppLocale.fromTagOrNull("de"))
        assertNull(AppLocale.fromTagOrNull(null))
    }
}