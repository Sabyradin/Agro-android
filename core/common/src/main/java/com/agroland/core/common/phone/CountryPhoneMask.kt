package com.agroland.core.common.phone

/**
 * Ел коды + телефон маскасы — Flutter `assets/config/countries.json` 1:1.
 *
 * Бұл backend-ке ЖІБЕРІЛЕТІН нөмір пішінін анықтайды: Flutter/iOS
 * `'+' + phoneCode + digits` жібереді (мыс. `+77011234567`). Android порты
 * бұрын пайдаланушы терген жолды сол күйі жіберетін (`7011234567`) — сол
 * себепті backend аккаунтты таппай, кіру мүлдем жүрмейтін (ISSUES #66).
 */
data class CountryPhoneMask(
    /** ISO коды — тұрақты кілт (KZ/RU/UZ/CN/KG). */
    val code: String,
    /** Ел коды «+» белгісінсіз: 7, 998, 86, 996. */
    val phoneCode: String,
    /** Енгізу маскасы: «0» — цифр орны, қалғаны — көрсетілім таңбасы. */
    val mask: String,
    /** Тізімде көрсетілетін ту (эмодзи — SVG асcets қажет етпейді). */
    val flag: String,
    /** Ел атауы (тізім/таңдау парағы үшін). */
    val displayName: String,
) {
    /** Маскадағы цифр орындарының саны — толық нөмір осынша цифрдан тұрады. */
    val digitCount: Int get() = mask.count { it == '0' }

    /** Тек цифрларды маскаға салады: `7011234567` → `(701) 123 4567`. */
    fun format(digits: String): String {
        val sb = StringBuilder()
        var i = 0
        for (ch in mask) {
            if (ch == '0') {
                if (i >= digits.length) break
                sb.append(digits[i])
                i++
            } else {
                if (i >= digits.length) break
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Пайдаланушы енгізген кез келген пішінді ұлттық цифрларға келтіреді.
     *
     * Адамдар нөмірді әдетте `8 701 …` немесе `+7 701 …` деп тереді (немесе
     * буферден толық нөмірді қояды) — артық префикс кесілмесе, соңғы цифрлар
     * жоғалып, мүлдем басқа нөмір жіберілетін еді.
     */
    fun normalizeInput(raw: String): String {
        var digits = raw.filter { it.isDigit() }
        if (digits.length > digitCount) {
            digits = when {
                digits.startsWith(phoneCode) &&
                    digits.length - phoneCode.length == digitCount ->
                    digits.drop(phoneCode.length)
                // Тек «сегіздік» елдер (KZ/RU) үшін: 8 → ел коды.
                phoneCode == "7" && digits.startsWith("8") &&
                    digits.length == digitCount + 1 -> digits.drop(1)
                else -> digits
            }
        }
        return digits.take(digitCount)
    }

    /** Backend күтетін пішін: `+7` + ұлттық цифрлар. */
    fun toE164(digits: String): String = "+$phoneCode$digits"

    fun isComplete(digits: String): Boolean = digits.length == digitCount

    companion object {
        val KZ = CountryPhoneMask("KZ", "7", "(000) 000 0000", "🇰🇿", "Қазақстан")

        /** Flutter countries.json тәртібі сақталған. */
        val ALL = listOf(
            KZ,
            CountryPhoneMask("RU", "7", "(000) 000 0000", "🇷🇺", "Россия"),
            CountryPhoneMask("UZ", "998", "00 000 00 00", "🇺🇿", "Өзбекстан"),
            CountryPhoneMask("CN", "86", "000 0000 0000", "🇨🇳", "Қытай"),
            CountryPhoneMask("KG", "996", "000 000 000", "🇰🇬", "Қырғызстан"),
        )

        /** Толық нөмірден (`+77011234567`) елді табу — ең ұзын кодқа басымдық. */
        fun fromE164(phone: String): CountryPhoneMask? {
            val digits = phone.filter { it.isDigit() }
            return ALL.sortedByDescending { it.phoneCode.length }
                .firstOrNull { digits.startsWith(it.phoneCode) }
        }

        /** Толық нөмірден ұлттық бөлікті бөліп алу. */
        fun nationalDigits(phone: String, mask: CountryPhoneMask): String =
            phone.filter { it.isDigit() }
                .removePrefix(mask.phoneCode)
                .take(mask.digitCount)
    }
}
