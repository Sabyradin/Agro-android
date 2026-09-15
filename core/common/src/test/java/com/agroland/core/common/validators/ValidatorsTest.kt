package com.agroland.core.common.validators

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    // ---- BIN / IIN ----

    @Test
    fun `valid bin is 12 digits`() {
        assertTrue(Validators.isValidBin("123456789012"))
        assertFalse(Validators.isValidBin("12345678901"))
        assertFalse(Validators.isValidBin("1234567890123"))
        assertFalse(Validators.isValidBin("12345678901a"))
    }

    @Test
    fun `valid iin is 12 digits`() {
        assertTrue(Validators.isValidIin("010101123456"))
    }

    // ---- VIN ----

    @Test
    fun `valid vin has 17 chars without IOQ`() {
        assertTrue(Validators.isValidVin("1HGCM82633A004352"))
        assertFalse(Validators.isValidVin("1HGCM82633A00435I")) // I жоқ болуы керек
        assertFalse(Validators.isValidVin("1HGCM82633A0043"))    // қысқа
    }

    @Test
    fun `vin lowercase is normalized`() {
        assertTrue(Validators.isValidVin("1hgcm82633a004352"))
    }

    // ---- China phone ----

    @Test
    fun `china phone is 11 digits starting 77`() {
        assertTrue(Validators.isValidChinaPhone("77123456789"))
        assertFalse(Validators.isValidChinaPhone("70123456789"))
        assertFalse(Validators.isValidChinaPhone("7712345678"))
    }

    // ---- Email ----

    @Test
    fun `email format`() {
        assertTrue(Validators.isValidEmailFormat("user@gmail.com"))
        assertFalse(Validators.isValidEmailFormat("usergmail.com"))
        assertFalse(Validators.isValidEmailFormat("user@"))
    }

    @Test
    fun `email domain whitelist`() {
        assertTrue(Validators.isEmailDomainAllowed("user@gmail.com"))
        assertTrue(Validators.isEmailDomainAllowed("user@icloud.com"))
        assertTrue(Validators.isEmailDomainAllowed("user@mail.ru"))
        assertFalse(Validators.isEmailDomainAllowed("user@yahoo.com"))
    }

    // ---- Contact phone ----

    @Test
    fun `contact phone digit count 10 or 11`() {
        assertTrue(Validators.isValidContactPhone("+7 701 234 56 78"))
        assertTrue(Validators.isValidContactPhone("87012345678"))
        assertFalse(Validators.isValidContactPhone("70123"))
    }

    // ---- Price ----

    @Test
    fun `price must be positive finite`() {
        assertTrue(Validators.isValidPrice(100.0))
        assertFalse(Validators.isValidPrice(0.0))
        assertFalse(Validators.isValidPrice(-5.0))
        assertFalse(Validators.isValidPrice(null))
    }
}