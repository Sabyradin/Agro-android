package com.agroland.core.common.formatters

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Баға форматтаушы. Backend Float бағаны қайтарады — Double ретінде оқып,
 * «1 250 000 ₸» стилінде (үтір-нүктесіз, орын бөлгіш — ғарыш) көрсетеміз.
 */
object PriceFormatter {

    private val symbols = DecimalFormatSymbols(Locale("ru", "RU")).apply {
        groupingSeparator = ' '   // ғарыш бөлгіш
        decimalSeparator = ','    // қалдық оңай көрсету үшін
    }

    private val grouped = DecimalFormat("#,##0", symbols)

    /** 1250000.0 -> "1 250 000 ₸" (валюта коды көрсетілсе). */
    fun format(price: Double?, currency: String = "₸"): String {
        if (price == null || price.isNaN() || price.isInfinite()) return ""
        return grouped.format(price.toLong()) + if (currency.isBlank()) "" else " $currency"
    }

    /** Дөңгелектеусіз топтастырылған сан: 1250000 -> "1 250 000". */
    fun formatPlain(value: Long): String = grouped.format(value)

    /** Тапсырыс сандары: қалдықпен, қажет болса ғана (1234.5 -> "1 234,5"). */
    fun formatPrecise(price: Double?, currency: String = "₸"): String {
        if (price == null || price.isNaN() || price.isInfinite()) return ""
        val hasFraction = price != price.toLong().toDouble()
        val pattern = if (hasFraction) "#,##0.##" else "#,##0"
        return DecimalFormat(pattern, symbols).format(price) +
            if (currency.isBlank()) "" else " $currency"
    }
}