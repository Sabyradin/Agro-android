package com.agroland.feature.media.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * QrUrlParser — QR мазмұнынан жарнама id-ін алу
 * (Flutter QrScannerPage._extractAnnouncementId parity).
 */
class QrUrlParserTest {

    @Test
    fun `canonical announcement path`() {
        assertEquals(
            12345L,
            QrUrlParser.extractAnnouncementId("https://agroland.kz/announcement/12345"),
        )
    }

    @Test
    fun `trailing digits of alphanumeric segment`() {
        // ad-99 → 99 (соңғы цифрлар regex).
        assertEquals(
            99L,
            QrUrlParser.extractAnnouncementId("https://host/announcement/ad-99"),
        )
    }

    @Test
    fun `non numeric segment falls back to query parameter`() {
        assertEquals(
            42L,
            QrUrlParser.extractAnnouncementId("https://host/announcement/some?announcementId=42"),
        )
    }

    @Test
    fun `query parameter only`() {
        assertEquals(
            42L,
            QrUrlParser.extractAnnouncementId("https://agroland.kz/share?announcementId=42"),
        )
    }

    @Test
    fun `other query parameters ignored`() {
        assertEquals(
            77L,
            QrUrlParser.extractAnnouncementId("https://host/p?a=1&announcementId=77&b=2"),
        )
    }

    @Test
    fun `path wins over query`() {
        assertEquals(
            5L,
            QrUrlParser.extractAnnouncementId("https://host/announcement/5?announcementId=42"),
        )
    }

    @Test
    fun `relative announcement path`() {
        assertEquals(
            123L,
            QrUrlParser.extractAnnouncementId("announcement/123"),
        )
    }

    @Test
    fun `trailing slash without id gives null`() {
        assertNull(QrUrlParser.extractAnnouncementId("https://host/announcement/"))
    }

    @Test
    fun `garbage url gives null`() {
        assertNull(QrUrlParser.extractAnnouncementId("Менен шыққан QR емес"))
    }

    @Test
    fun `blank input gives null`() {
        assertNull(QrUrlParser.extractAnnouncementId("   "))
    }

    @Test
    fun `non announcement host path gives null`() {
        assertNull(QrUrlParser.extractAnnouncementId("https://example.com/catalog/12345"))
    }
}