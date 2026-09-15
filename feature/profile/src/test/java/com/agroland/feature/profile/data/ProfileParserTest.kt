package com.agroland.feature.profile.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ProfileParser кешірімді парсингі — backend тип тұрақсыздығына төзімділік. */
class ProfileParserTest {

    private fun obj(raw: String) = Json.parseToJsonElement(raw).jsonObject

    @Test
    fun `парсинг толық профилін оқиды`() {
        val root = obj(
            """
            {
              "user": {
                "id": 77,
                "name": "Асан",
                "phone_number": "+77770001122",
                "email": "asan@gmail.com",
                "avatar_url": "/media/avatars/a.png",
                "user_type": "dealer",
                "is_vat_payer": true,
                "is_verified": true,
                "is_vip_seller": false,
                "dealer_status": "active",
                "dealer_terms_accepted": true,
                "company_info": {
                  "name": "Agro Ltd",
                  "bin": "123456789012",
                  "representative_name": "Асан Әсенов",
                  "representative_position": "Директор"
                }
              },
              "locations": [{"id": 3, "title": "Үй", "address": "Достык 5", "is_primary": true}],
              "announcements": {"counts": {"active": "4", "pending": 1, "rejected": 0}}
            }
            """.trimIndent(),
        )
        val profile = ProfileParser.parseProfile(root)!!

        assertEquals(77L, profile.id)
        assertEquals("Асан", profile.name)
        assertEquals("+77770001122", profile.phone)
        assertEquals("dealer", profile.userType)
        assertTrue(profile.isVatPayer)
        assertTrue(profile.isVerified)
        assertTrue(profile.dealerTermsAccepted)
        assertEquals("Agro Ltd", profile.company?.name)
        assertEquals("Асан Әсенов", profile.company?.representativeName)
        assertEquals(1, profile.locations.size)
        assertEquals("Үй", profile.locations[0].title)
        assertTrue(profile.locations[0].isPrimary)
        // int string түрінде келсе де дұрыс оқылады.
        assertEquals(4, profile.announcements?.active)
        assertEquals(1, profile.announcements?.pending)
    }

    @Test
    fun `бос өрістер null-ге шыдайды және дефолттарды береді`() {
        val root = obj("""{"user": {"id": 1, "name": null, "user_type": "individual"}}""")
        val profile = ProfileParser.parseProfile(root)!!

        assertEquals(1L, profile.id)
        assertNull(profile.name)
        assertNull(profile.phone)
        assertFalse(profile.isVatPayer)
        assertFalse(profile.dealerTermsAccepted)
        assertNull(profile.company)
        assertTrue(profile.locations.isEmpty())
    }

    @Test
    fun `verification статусын оқиды және күйлерін есептейді`() {
        val root = obj(
            """
            {"status": "rejected", "rejection_reason": "Сурет бұлыңғыр",
             "submitted_at": "2026-09-01T10:00:00", "documents": [{"id": 1}, {"id": 2}]}
            """.trimIndent(),
        )
        val status = ProfileParser.parseVerificationStatus(root)!!

        assertTrue(status.isRejected)
        assertFalse(status.isApproved)
        assertFalse(status.isLocked)
        assertEquals("Сурет бұлыңғыр", status.rejectionReason)
        assertEquals(2, status.documentCount)

        val pending = ProfileParser.parseVerificationStatus(obj("""{"status": "pending"}"""))!!
        assertTrue(pending.isLocked)
        assertFalse(pending.isNotSubmitted)
    }

    @Test
    fun `avatar url балама кілттерінен оқылады`() {
        assertEquals("/m/a.png", ProfileParser.parseAvatarUrl(obj("""{"avatar_url": "/m/a.png"}""")))
        assertEquals("/m/b.png", ProfileParser.parseAvatarUrl(obj("""{"avatar": "/m/b.png"}""")))
        assertNull(ProfileParser.parseAvatarUrl(obj("""{"other": 1}""")))
    }

    @Test
    fun `PATCH body тек өзгерген өрістерді қосады`() {
        val body = ProfileRequests.updateProfile(name = "Жаңа", email = null)
        assertTrue("name" in body)
        assertFalse("email" in body)

        val clearEmail = ProfileRequests.updateProfile(name = null, email = "")
        assertTrue("email" in clearEmail) // "" → backend NULL
        assertFalse("name" in clearEmail)
    }

    @Test
    fun `company contacts body нақты мәндер жібереді`() {
        val body = ProfileRequests.companyContacts("+7777", null, "@tg", "")
        assertTrue("phone" in body)
        assertFalse("website" in body)
        assertTrue("telegram" in body)
        assertTrue("whatsapp" in body) // бос string де жіберіледі (тазарту үшін)
    }
}