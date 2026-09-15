package com.agroland.core.common.formatters

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Күн форматтаушы. Backend ISO-8601 («2026-09-15T10:30:00Z», кейде fractional seconds
 * немесе timezone-сыз) қайтарады — парсер кешірімді болуы керек.
 */
object DateFormatter {

    private val dateTimeFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /** Кез келген ISO-8601 варианттан LocalDateTime жасайды (UTC → жергілікті аймақ). */
    fun parseOrNull(raw: String?): LocalDateTime? {
        if (raw.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(raw).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        } catch (e: Exception) {
            try {
                Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDateTime()
            } catch (e2: Exception) {
                try {
                    LocalDateTime.parse(raw) // timezone-сыз
                } catch (e3: Exception) {
                    try {
                        LocalDate.parse(raw).atStartOfDay() // тек күн
                    } catch (e4: Exception) {
                        null
                    }
                }
            }
        }
    }

    fun formatDateTime(raw: String?): String =
        parseOrNull(raw)?.format(dateTimeFormat) ?: ""

    fun formatDate(raw: String?): String =
        parseOrNull(raw)?.format(dateFormat) ?: ""

    fun formatDateTime(value: LocalDateTime?): String =
        value?.format(dateTimeFormat) ?: ""

    fun formatDate(value: LocalDate?): String =
        value?.format(dateFormat) ?: ""
}