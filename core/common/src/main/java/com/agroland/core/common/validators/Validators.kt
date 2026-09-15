package com.agroland.core.common.validators

/**
 * Валидаторлар. Барлық клиент жағынан тексерілетін форма ережелері осында.
 */
object Validators {

    /** Бизнес-идентификация нөмірі: 12 цифр. */
    private val BIN_REGEX = Regex("^\\d{12}$")

    /** ЖСН (ЖҚҚАҚ ілік номері): 12 цифр. */
    private val IIN_REGEX = Regex("^\\d{12}$")

    /** Agri-machinery VIN: I, O, Q жоқ, 17 таңба. */
    private val VIN_REGEX = Regex("^[A-HJ-NPR-Z0-9]{17}$")

    /** Қытай тапсырысы телефоны: 11 цифр, 77 префиксімен басталады. */
    private val CHINA_PHONE_REGEX = Regex("^77\\d{9}$")

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /** Email доменінен тізімі (backend PUT /user/email ережесімен сайкес). */
    private val EMAIL_DOMAIN_WHITELIST = setOf("gmail.com", "icloud.com", "mail.ru")

    fun isValidBin(bin: String): Boolean = BIN_REGEX.matches(bin.trim())

    fun isValidIin(iin: String): Boolean = IIN_REGEX.matches(iin.trim())

    fun isValidVin(vin: String): Boolean = VIN_REGEX.matches(vin.trim().uppercase())

    fun isValidChinaPhone(phone: String): Boolean = CHINA_PHONE_REGEX.matches(phone.trim())

    fun isValidEmailFormat(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    fun isEmailDomainAllowed(email: String): Boolean {
        val domain = email.trim().substringAfterLast('@', missingDelimiterValue = "").lowercase()
        return EMAIL_DOMAIN_WHITELIST.contains(domain)
    }

    /** Байланыс телефоны: +7 (70X XXX XX XX) стилі — цифрлер саны 10–11. */
    fun isValidContactPhone(phone: String): Boolean {
        val digits = phone.filter { it.isDigit() }
        return digits.length in 10..11
    }

    /** Баға: > 0 және ақылға қонымы. */
    fun isValidPrice(price: Double?): Boolean = price != null && price > 0.0 && price.isFinite()
}