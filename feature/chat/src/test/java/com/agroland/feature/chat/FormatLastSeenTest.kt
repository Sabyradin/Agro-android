package com.agroland.feature.chat

import com.agroland.feature.chat.domain.FormatLastSeen
import org.junit.Assert.assertEquals
import org.junit.Test

/** FormatLastSeen — Flutter format_last_seen.dart / web formatLastSeen. */
class FormatLastSeenTest {

    private val labels = FormatLastSeen.Labels(
        online = "Желіде",
        offline = "Желіден шыққан",
        prefix = "соңғы рет",
        justNow = "жаңа ғана",
        minutesAgo = { "$it мин бұрын" },
        hoursAgo = { "$it сағ бұрын" },
        yesterday = "кеше",
        daysAgo = { "$it күн бұрын" },
        dateFormatter = { FormatLastSeen.formatDate(it) },
    )

    private val now = 1_800_000_000_000L // тұрақты «қазір»

    @Test
    fun `онлайн — «Желіде»`() {
        assertEquals("Желіде", FormatLastSeen.format(labels, isOnline = true, lastOnline = null, peerId = 5L, now = now))
    }

    @Test
    fun `онлайн — lastOnline-ға қарамайды`() {
        assertEquals(
            "Желіде",
            FormatLastSeen.format(labels, isOnline = true, lastOnline = now - 100_000, peerId = 5L, now = now),
        )
    }

    @Test
    fun `сервис аккаунт — бос жол (31, 1001-1005)`() {
        for (id in listOf(31L, 1001L, 1002L, 1003L, 1004L, 1005L)) {
            assertEquals(
                "",
                FormatLastSeen.format(labels, isOnline = false, lastOnline = now, peerId = id, now = now),
            )
        }
    }

    @Test
    fun `деректік әуелгі координаттарсыз — офлайн`() {
        assertEquals(
            "Желіден шыққан",
            FormatLastSeen.format(labels, isOnline = false, lastOnline = null, peerId = 5L, now = now),
        )
    }

    @Test
    fun `бір минуттан аз — жаңа ғана`() {
        assertEquals(
            "соңғы рет жаңа ғана",
            FormatLastSeen.format(labels, isOnline = false, lastOnline = now - 30_000, peerId = 5L, now = now),
        )
    }

    @Test
    fun `минуттар`() {
        assertEquals(
            "соңғы рет 5 мин бұрын",
            FormatLastSeen.format(labels, isOnline = false, lastOnline = now - 5 * 60_000, peerId = 5L, now = now),
        )
    }

    @Test
    fun `сағаттар`() {
        assertEquals(
            "соңғы рет 3 сағ бұрын",
            FormatLastSeen.format(labels, isOnline = false, lastOnline = now - 3 * 3_600_000, peerId = 5L, now = now),
        )
    }

    @Test
    fun `кеше`() {
        assertEquals(
            "соңғы рет кеше",
            FormatLastSeen.format(labels, isOnline = false, lastOnline = now - 24 * 3_600_000, peerId = 5L, now = now),
        )
    }

    @Test
    fun `күндер`() {
        assertEquals(
            "соңғы рет 3 күн бұрын",
            FormatLastSeen.format(labels, isOnline = false, lastOnline = now - 3L * 86_400_000, peerId = 5L, now = now),
        )
    }

    @Test
    fun `жеті күннен ескі — толық дата`() {
        assertEquals(
            "соңғы рет ${FormatLastSeen.formatDate(now - 8L * 86_400_000)}",
            FormatLastSeen.format(labels, isOnline = false, lastOnline = now - 8L * 86_400_000, peerId = 5L, now = now),
        )
    }

    @Test
    fun `formatDate — ddMMyyyy пішімі`() {
        val formatted = FormatLastSeen.formatDate(System.currentTimeMillis())
        assert(Regex("\\d{2}\\.\\d{2}\\.\\d{4}").matches(formatted))
    }

    private fun assert(b: Boolean) {
        org.junit.Assert.assertTrue(b)
    }
}