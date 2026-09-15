package com.agroland.feature.profile.data

import com.agroland.core.network.json.JsonParser
import com.agroland.feature.location.data.SelectedLocation
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Профиль моделі — ProfileModel (Flutter): user, locations, announcements, company_info.
 * Барлық парсинг JsonParser көмекшілері арқылы (кешірімді, типтер тұрақсыз).
 */
data class UserProfile(
    val id: Long?,
    val name: String?,
    val phone: String?,
    val email: String?,
    val avatarUrl: String?,
    val userType: String?,
    val isVatPayer: Boolean,
    val isVerified: Boolean,
    val isVipSeller: Boolean,
    val dealerStatus: String?,
    val dealerTermsAccepted: Boolean,
    /** Әмиян балансы (₸) — PaymentMethodSheet жеткіліктігін тексереді (Фаза 9). */
    val balance: Double = 0.0,
    val company: CompanyInfo?,
    val locations: List<UserLocation>,
    val announcements: AnnouncementCounts?,
)

/** Компания туралы мәліметтер — user.company_info. */
data class CompanyInfo(
    val name: String? = null,
    val bin: String? = null,
    val description: String? = null,
    val logoUrl: String? = null,
    val bannerUrl: String? = null,
    val representativeName: String? = null,
    val representativePosition: String? = null,
    val about: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val telegram: String? = null,
    val whatsapp: String? = null,
)

/**
 * Пайдаланушы мекенжайы — GET /user/profile → locations[] (LocationModel пішіні,
 * Flutter mobile-app location_model.dart-пен бірдей):
 *  - address_id — Address кестесінің id-і;
 *  - user_location_id — UserLocation (user↔address) сілтеме жолының id-і;
 *    DELETE /user/location/{id} және жарнама user_location_id дәл осыны пайдаланады;
 *  - мәтіндік бөліктер: house/street/country/area/province/locality;
 *  - каталог ID-лері + координаталар (backend координат бойынша district_id-ды
 *    өздігінен турындатады — Location fix 3d78fa9).
 */
data class UserLocation(
    /** UserLocation сілтеме жолы — API әрекеттері осы id- арқылы. */
    val id: Long,
    /** Address кестесінің id-і (id-ден өзге, тек көрсету үшін). */
    val addressId: Long = 0L,
    val house: String? = null,
    val street: String? = null,
    val country: String? = null,
    val area: String? = null,
    val province: String? = null,
    val locality: String? = null,
    val countryId: Int? = null,
    val regionId: Int? = null,
    val districtId: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    /** Тізім тақтасының бас жолы — locality, street, house (Flutter title). */
    val title: String
        get() = listOfNotNull(locality, street, house)
            .filter { it.isNotBlank() }
            .joinToString(", ")

    /** Тізім тақтасының қосымша жолы — province, country (Flutter subtitle). */
    val subtitle: String
        get() = listOfNotNull(province, country)
            .filter { it.isNotBlank() }
            .joinToString(", ")

    /** Толық мекенжай — country, province, locality, street, house. */
    val fullAddress: String
        get() = listOfNotNull(country, province, locality, street, house)
            .filter { it.isNotBlank() }
            .joinToString(", ")
}

/** Жарнама саны — profile.announcements {counts}. */
data class AnnouncementCounts(
    val active: Int,
    val inactive: Int,
    val pending: Int,
    val rejected: Int,
)

/** KYC статусы — GET /verification/status. */
data class VerificationStatus(
    val status: String,
    val rejectionReason: String?,
    val submittedAt: String?,
    val documentCount: Int,
) {
    val isApproved: Boolean get() = status.equals("approved", ignoreCase = true)
    val isPending: Boolean get() = status.equals("pending", ignoreCase = true)
    val isRejected: Boolean get() = status.equals("rejected", ignoreCase = true)
    val isNotSubmitted: Boolean get() = status.equals("not_submitted", ignoreCase = true)
    /** Расталғаннан кейін форма құлыпталады. */
    val isLocked: Boolean get() = isApproved || isPending
}

/** Парсер — бір ғана орыннан шақырылады. */
object ProfileParser {

    fun parseProfile(root: JsonObject?): UserProfile? {
        if (root == null) return null
        val user = JsonParser.obj(root, "user") ?: root // кейде түбір тікелей user болуы мүмкін
        return UserProfile(
            id = JsonParser.long(user, "id"),
            name = JsonParser.string(user, "name"),
            phone = JsonParser.string(user, "phone_number") ?: JsonParser.string(user, "phone"),
            email = JsonParser.string(user, "email"),
            avatarUrl = JsonParser.string(user, "avatar_url") ?: JsonParser.string(user, "avatar"),
            userType = JsonParser.string(user, "user_type"),
            isVatPayer = JsonParser.bool(user, "is_vat_payer") ?: false,
            isVerified = JsonParser.bool(user, "is_verified") ?: false,
            isVipSeller = JsonParser.bool(user, "is_vip_seller") ?: false,
            dealerStatus = JsonParser.string(user, "dealer_status") ?: JsonParser.string(user, "business_status"),
            dealerTermsAccepted = JsonParser.bool(user, "dealer_terms_accepted")
                ?: JsonParser.bool(user, "business_terms_accepted") ?: false,
            balance = JsonParser.double(user, "balance") ?: 0.0,
            company = parseCompany(JsonParser.obj(user, "company_info")),
            locations = JsonParser.arrayOrSingle(root, "locations").mapNotNull { parseLocation(it as? JsonObject) },
            announcements = parseAnnouncementCounts(
                JsonParser.obj(root, "announcements") ?: JsonParser.obj(user, "announcements"),
            ),
        )
    }

    fun parseCompany(info: JsonObject?): CompanyInfo? {
        if (info == null) return null
        return CompanyInfo(
            name = JsonParser.string(info, "name"),
            bin = JsonParser.string(info, "bin") ?: JsonParser.string(info, "company_bin"),
            description = JsonParser.string(info, "description"),
            logoUrl = JsonParser.string(info, "logo_url") ?: JsonParser.string(info, "logo"),
            bannerUrl = JsonParser.string(info, "banner_url") ?: JsonParser.string(info, "banner"),
            representativeName = JsonParser.string(info, "representative_name"),
            representativePosition = JsonParser.string(info, "representative_position"),
            about = JsonParser.string(info, "about") ?: JsonParser.string(info, "text"),
            phone = JsonParser.string(info, "phone"),
            website = JsonParser.string(info, "website"),
            telegram = JsonParser.string(info, "telegram"),
            whatsapp = JsonParser.string(info, "whatsapp"),
        )
    }

    fun parseLocation(loc: JsonObject?): UserLocation? {
        if (loc == null) return null
        // Сілтеме жолының id-і — user_location_id, болмаса id (бірлік жауаптарда).
        val linkId = JsonParser.long(loc, "user_location_id") ?: JsonParser.long(loc, "id")
            ?: return null
        return UserLocation(
            id = linkId,
            addressId = JsonParser.long(loc, "address_id") ?: 0L,
            house = JsonParser.string(loc, "house"),
            street = JsonParser.string(loc, "street"),
            country = JsonParser.string(loc, "country"),
            area = JsonParser.string(loc, "area"),
            province = JsonParser.string(loc, "province"),
            locality = JsonParser.string(loc, "locality"),
            countryId = JsonParser.int(loc, "country_id"),
            regionId = JsonParser.int(loc, "region_id"),
            districtId = JsonParser.int(loc, "district_id"),
            latitude = JsonParser.double(loc, "latitude"),
            longitude = JsonParser.double(loc, "longitude"),
        )
    }

    private fun parseAnnouncementCounts(obj: JsonObject?): AnnouncementCounts? {
        if (obj == null) return null
        val counts = JsonParser.obj(obj, "counts") ?: obj
        fun count(vararg keys: String): Int = keys.firstNotNullOfOrNull { JsonParser.int(counts, it) } ?: 0
        return AnnouncementCounts(
            active = count("active", "published"),
            inactive = count("inactive", "draft", "deactivated"),
            pending = count("pending", "moderation"),
            rejected = count("rejected"),
        )
    }

    fun parseVerificationStatus(root: JsonObject?): VerificationStatus? {
        if (root == null) return null
        val status = JsonParser.string(root, "status") ?: return null
        return VerificationStatus(
            status = status,
            rejectionReason = JsonParser.string(root, "rejection_reason"),
            submittedAt = JsonParser.string(root, "submitted_at"),
            documentCount = JsonParser.arrayOrSingle(root, "documents").size,
        )
    }

    fun parseAvatarUrl(root: JsonObject?): String? =
        JsonParser.string(root, "avatar_url") ?: JsonParser.string(root, "avatar") ?: JsonParser.string(root, "url")
}

/** PATCH body жасаушылар — тек өзгерген өрістер жіберіледі (additive PATCH). */
object ProfileRequests {

    fun updateProfile(name: String?, email: String?): JsonObject = buildJsonObject {
        if (name != null) put("name", name)
        if (email != null) put("email", email) // "" → backend NULL (тазарту)
    }

    /**
     * Мекенжай денесі — Flutter LocationModel.toMap() пішіні (backend растаған):
     * house/street/country/area/province/locality (бос емесдері) + каталог
     * ID-лері + міндетті latitude/longitude (картадан таңдалмаса 0.0 —
     * backend district_id-ды өздігінен турындатады).
     */
    fun location(
        street: String,
        house: String,
        catalog: SelectedLocation?,
        latitude: Double?,
        longitude: Double?,
    ): JsonObject = buildJsonObject {
        if (street.isNotBlank()) put("street", street)
        if (house.isNotBlank()) put("house", house)
        catalog?.countryName?.takeIf { it.isNotBlank() }?.let { put("country", it) }
        // area мен province екеуі де өңір атауын алады (Flutter солай жібереді).
        catalog?.regionName?.takeIf { it.isNotBlank() }?.let { put("area", it) }
        catalog?.regionName?.takeIf { it.isNotBlank() }?.let { put("province", it) }
        catalog?.districtName?.takeIf { it.isNotBlank() }?.let { put("locality", it) }
        catalog?.countryId?.let { put("country_id", it) }
        catalog?.regionId?.let { put("region_id", it) }
        catalog?.districtId?.let { put("district_id", it) }
        put("latitude", latitude ?: 0.0)
        put("longitude", longitude ?: 0.0)
    }

    fun companyRepresentative(name: String, position: String): JsonObject = buildJsonObject {
        put("name", name)
        put("position", position)
    }

    fun companyAbout(text: String): JsonObject = buildJsonObject {
        put("text", text)
    }

    fun companyContacts(phone: String?, website: String?, telegram: String?, whatsapp: String?): JsonObject =
        buildJsonObject {
            if (phone != null) put("phone", phone)
            if (website != null) put("website", website)
            if (telegram != null) put("telegram", telegram)
            if (whatsapp != null) put("whatsapp", whatsapp)
        }

    fun email(email: String): JsonObject = buildJsonObject {
        put("email", email)
    }

    fun verification(fullName: String, iin: String): JsonObject = buildJsonObject {
        putJsonObject("meta") {
            put("full_name", fullName)
            put("iin", iin)
        }
    }
}