package com.agroland.core.common.formatters

/**
 * Сан форматтаушы: 1234 -> "1,2K", 2_500_000 -> "2,5M", 42 -> "42".
 * Көру/шақыру/пікір сандары үшін.
 */
object CountFormatter {

    fun formatCompact(count: Int?): String {
        if (count == null) return "0"
        return when {
            count < 1_000 -> count.toString()
            count < 1_000_000 -> {
                val v = count / 1000.0
                trimZero(v) + "K"
            }
            else -> {
                val v = count / 1_000_000.0
                trimZero(v) + "M"
            }
        }
    }

    private fun trimZero(value: Double): String {
        val rounded = (value * 10).toLong() / 10.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString().replace('.', ',')
        }
    }
}