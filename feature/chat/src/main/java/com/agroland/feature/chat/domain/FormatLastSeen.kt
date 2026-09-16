package com.agroland.feature.chat.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * «Last seen» мәтінін құрушы (Flutter format_last_seen.dart, web formatLastSeen):
 *   онлайн → «Желіде»; офлайн: <1 мин «әзір ғана», <60 мин «N мин бұрын»,
 *   <24 сағ «N сағ бұрын», 1 күн «кеше», <7 күн «N күн бұрын», ескісі — дата.
 * Сервис аккаунттар (31, 1001–1005) үшін бос строка — бұл чаттарда last seen жоқ.
 */
object FormatLastSeen {

    /** Жүйелік/сервис чат id-лері (Оператор, Гид, Агроланд және т.б.). */
    val SPECIAL_SERVICE_IDS = setOf(31L, 1001L, 1002L, 1003L, 1004L, 1005L)

    data class Labels(
        val online: String,
        val offline: String,
        val prefix: String,
        val justNow: String,
        val minutesAgo: (Int) -> String,
        val hoursAgo: (Int) -> String,
        val yesterday: String,
        val daysAgo: (Int) -> String,
        val dateFormatter: (Long) -> String,
    )

    fun format(
        labels: Labels,
        isOnline: Boolean,
        lastOnline: Long?,
        peerId: Long?,
        now: Long = System.currentTimeMillis(),
    ): String {
        // Сервис чаттарда last seen жоқ (web isSpecialService паритеті).
        if (peerId != null && peerId in SPECIAL_SERVICE_IDS) return ""
        if (isOnline) return labels.online
        if (lastOnline == null) return labels.offline

        val diffMinutes = ChronoUnit.MINUTES.between(
            Instant.ofEpochMilli(lastOnline),
            Instant.ofEpochMilli(now),
        )
        if (diffMinutes < 1) return "${labels.prefix} ${labels.justNow}"
        if (diffMinutes < 60) return "${labels.prefix} ${labels.minutesAgo(diffMinutes.toInt())}"
        val diffHours = ChronoUnit.HOURS.between(
            Instant.ofEpochMilli(lastOnline),
            Instant.ofEpochMilli(now),
        )
        if (diffHours < 24) return "${labels.prefix} ${labels.hoursAgo(diffHours.toInt())}"
        val diffDays = ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(lastOnline),
            Instant.ofEpochMilli(now),
        )
        if (diffDays == 1L) return "${labels.prefix} ${labels.yesterday}"
        if (diffDays < 7) return "${labels.prefix} ${labels.daysAgo(diffDays.toInt())}"
        // 7 күннен ескі — толық күн (чат басындағы дата пішімімен).
        return "${labels.prefix} ${labels.dateFormatter(lastOnline)}"
    }

    /** «12.09.2026» пішімі (UiUtils.dateFormatter). */
    fun formatDate(millis: Long): String {
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        return "%02d.%02d.%04d".format(date.dayOfMonth, date.monthValue, date.year)
    }

    /** dayOfYear парақтау — тізім элементіндегі салыстырмалы уақыт. */
    fun formatRelative(millis: Long, yesterdayLabel: String, now: Long = System.currentTimeMillis()): String {
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        return when (date) {
            today -> "%02d:%02d".format(
                Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).hour,
                Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).minute,
            )
            today.minusDays(1) -> yesterdayLabel
            else -> formatDate(millis)
        }
    }

    fun dayKey(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}