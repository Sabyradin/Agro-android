package com.agroland.core.common.phone

import org.junit.Assert.assertEquals
import org.junit.Test

/** Телефон нөмірін енгізу/жіберу пішіні (ISSUES #66). */
class CountryPhoneMaskTest {

    private val kz = CountryPhoneMask.KZ

    @Test
    fun `ulttyq 10 cifr ozgermeidi`() {
        assertEquals("7001112203", kz.normalizeInput("7001112203"))
    }

    @Test
    fun `segizdik prefiks kesiledi`() {
        assertEquals("7001112203", kz.normalizeInput("87001112203"))
    }

    @Test
    fun `el kody kesiledi`() {
        assertEquals("7001112203", kz.normalizeInput("+7 700 111 2203"))
        assertEquals("7001112203", kz.normalizeInput("77001112203"))
    }

    @Test
    fun `backend kutetin pishin`() {
        assertEquals("+77001112203", kz.toE164(kz.normalizeInput("8 (700) 111-22-03")))
    }

    @Test
    fun `maska formattaidy`() {
        assertEquals("(700) 111 2203", kz.format("7001112203"))
        assertEquals("(700) 111", kz.format("700111"))
    }

    @Test
    fun `tolyq nomirden el men ulttyq bolik bolinedi`() {
        val mask = CountryPhoneMask.fromE164("+998901234567")
        assertEquals("UZ", mask?.code)
        assertEquals("901234567", CountryPhoneMask.nationalDigits("+998901234567", mask!!))
    }

    @Test
    fun `ozbek nomiri 9 cifr`() {
        val uz = CountryPhoneMask.ALL.first { it.code == "UZ" }
        assertEquals(9, uz.digitCount)
        assertEquals("+998901234567", uz.toE164("901234567"))
    }
}
