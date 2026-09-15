package com.agroland.feature.profile.data

import com.agroland.feature.location.data.SelectedLocation
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
              "locations": [{"address_id": 9, "user_location_id": 3, "house": "12/1",
                "street": "Достык", "country": "Қазақстан", "area": "Астана",
                "province": "Астана", "locality": "Есіл", "country_id": 1,
                "region_id": 10, "district_id": 100, "latitude": "51.1282", "longitude": "71.4307"}],
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
        val loc = profile.locations[0]
        assertEquals(3L, loc.id) // user_location_id — API әрекеттері осы id арқылы
        assertEquals(9L, loc.addressId)
        assertEquals("Есіл, Достык, 12/1", loc.title)
        assertEquals("Астана, Қазақстан", loc.subtitle)
        assertEquals("Қазақстан, Астана, Есіл, Достык, 12/1", loc.fullAddress)
        assertEquals(1, loc.countryId)
        assertEquals(10, loc.regionId)
        assertEquals(100, loc.districtId)
        assertEquals(51.1282, loc.latitude!!, 0.0001) // string координат та оқылады
        assertEquals(71.4307, loc.longitude!!, 0.0001)
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

    @Test
    fun `location body Flutter toMap пішімінде — Фаза 7`() {
        val body = ProfileRequests.location(
            street = "Достык",
            house = "12/1",
            catalog = SelectedLocation(
                countryId = 1, countryName = "Қазақстан",
                regionId = 10, regionName = "Астана",
                districtId = 100, districtName = "Есіл",
                latitude = 51.1282, longitude = 71.4307,
            ),
            latitude = 51.1282,
            longitude = 71.4307,
        )
        assertEquals("Достык", body["street"]?.toString()?.trim('"'))
        assertEquals("12/1", body["house"]?.toString()?.trim('"'))
        assertEquals("Қазақстан", body["country"]?.toString()?.trim('"'))
        // area мен province екеуі де өңір атауы (Flutter солай жібереді).
        assertEquals("Астана", body["area"]?.toString()?.trim('"'))
        assertEquals("Астана", body["province"]?.toString()?.trim('"'))
        assertEquals("Есіл", body["locality"]?.toString()?.trim('"'))
        assertEquals("1", body["country_id"]?.toString())
        assertEquals("10", body["region_id"]?.toString())
        assertEquals("100", body["district_id"]?.toString())
        assertEquals("51.1282", body["latitude"]?.toString())
        assertEquals("71.4307", body["longitude"]?.toString())

        // Бос көше/үй жіберілмейді; координат жоқ болса 0.0 (backend district_id-ды
        // өздігінен турындатады). title/address/city мүлде жоқ.
        val empty = ProfileRequests.location("", "", null, null, null)
        assertFalse("street" in empty)
        assertFalse("house" in empty)
        assertFalse("country" in empty)
        assertFalse("country_id" in empty)
        assertEquals("0.0", empty["latitude"]?.toString())
        assertEquals("0.0", empty["longitude"]?.toString())
    }
}