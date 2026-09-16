package com.agroland.feature.promo.data

import com.agroland.core.network.json.JsonParser
import com.agroland.feature.marketplace.data.MarketplaceParser
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Промо v2 каталог элементі (Flutter PromoCatalogItem, 1:1):
 * GET /promo-v2/catalog → {items: [...]}. «Жарнама жылжыту» UI-інің
 * жалғыз дереккөзі — атаулар/бағалар/эффекттер түгел backend-тен.
 */
data class PromoCatalogItem(
    val sku: String,
    val kind: String, // boost | vip | banner | auto_renew
    val target: String, // announcement | user | main_page
    val durationDays: Int,
    val effects: Map<String, String>,
    val titles: Map<String, String>,
    val descriptions: Map<String, String>,
    val price: Double,
    val currency: String,
    val activateEndpoint: PromoActivateEndpoint?,
) {
    /** Белсендіру жарнама id-ін талап ете ме (boost/vip_item/auto_renew). */
    val requiresAnnouncement: Boolean get() = target == "announcement"

    fun localizedTitle(localeTag: String?): String = localized(titles, localeTag)

    fun localizedDescription(localeTag: String?): String = localized(descriptions, localeTag)

    companion object {
        fun fromJson(root: JsonElement?): PromoCatalogItem? {
            val obj = root as? JsonObject ?: return null
            val sku = JsonParser.string(obj, "sku") ?: return null
            return PromoCatalogItem(
                sku = sku,
                kind = JsonParser.string(obj, "kind") ?: "",
                target = JsonParser.string(obj, "target") ?: "",
                durationDays = JsonParser.int(obj, "duration_days") ?: 0,
                effects = stringMap(obj, "effects"),
                titles = stringMap(obj, "titles"),
                descriptions = stringMap(obj, "descriptions"),
                price = JsonParser.double(obj, "price") ?: 0.0,
                currency = JsonParser.string(obj, "currency") ?: "KZT",
                activateEndpoint = PromoActivateEndpoint.fromJson(obj["activate_endpoint"]),
            )
        }

        fun listFromJson(root: JsonElement?): List<PromoCatalogItem> {
            val obj = root as? JsonObject ?: return emptyList()
            val arr = JsonParser.arr(obj, "items") ?: return emptyList()
            // Бір элемент қате болса — қалғандары қалыпта оқылады.
            return arr.mapNotNull { fromJson(it) }
        }

        private fun stringMap(obj: JsonObject, key: String): Map<String, String> =
            ((obj[key] as? JsonObject)?.entries ?: emptySet()).mapNotNull { (k, v) ->
                (v as? JsonPrimitive)?.contentOrNull?.let { k to it }
            }.toMap()

        /** zh → cn, тікелей сәйкес, ru fallback, бірінші мән, соңында ''. */
        fun localized(map: Map<String, String>, localeTag: String?): String {
            if (localeTag == "zh" && !map["cn"].isNullOrEmpty()) return map["cn"]!!
            map[localeTag]?.takeIf { it.isNotEmpty() }?.let { return it }
            map["ru"]?.takeIf { it.isNotEmpty() }?.let { return it }
            return map.values.firstOrNull() ?: ""
        }
    }
}

/**
 * SKU-ға тән белсендіру эндпоинты (Flutter PromoActivateEndpoint):
 * path ішінде {announcement_id} плейсхолдері болуы мүмкін.
 */
data class PromoActivateEndpoint(
    val method: String,
    val path: String,
    val body: JsonObject?,
) {
    /** {announcement_id} алмастырылған және baseUrl-ге сәйкес path (бастапқы / жоқ). */
    fun resolvedPath(announcementId: Long?): String =
        path.replace("{announcement_id}", announcementId?.toString() ?: "").trimStart('/')

    companion object {
        fun fromJson(root: JsonElement?): PromoActivateEndpoint? {
            val obj = root as? JsonObject ?: return null
            val path = JsonParser.string(obj, "path") ?: return null
            return PromoActivateEndpoint(
                method = JsonParser.string(obj, "method")?.uppercase() ?: "POST",
                path = path,
                body = obj["body"] as? JsonObject,
            )
        }
    }
}

/**
 * Кез келген промо-v2 белсендірудің бірыңғай нәтижесі (Flutter
 * PromoActivateResult). Әр SKU түрі өрістердің өз жиынын толтырады —
 * барлығы nullable, сондықтан бір модель boost/vip/banner/auto_renew-ді
 * қамтиды.
 */
data class PromoActivateResult(
    val announcementId: Long?,
    val boostMultiplier: Double?,
    val boostExpiresAt: String?,
    val isVipSeller: Boolean?,
    val vipSellerExpiresAt: String?,
    val bannerId: Long?,
    val bannerStatus: String?,
    val bannerStartsAt: String?,
    val bannerExpiresAt: String?,
    val balance: Double?,
) {
    companion object {
        fun fromJson(obj: JsonObject?): PromoActivateResult? {
            obj ?: return null
            return PromoActivateResult(
                announcementId = JsonParser.long(obj, "announcement_id"),
                boostMultiplier = JsonParser.double(obj, "boost_multiplier"),
                boostExpiresAt = JsonParser.string(obj, "boost_expires_at"),
                isVipSeller = JsonParser.bool(obj, "is_vip_seller"),
                vipSellerExpiresAt = JsonParser.string(obj, "vip_seller_expires_at"),
                bannerId = JsonParser.long(obj, "id"),
                bannerStatus = JsonParser.string(obj, "status"),
                bannerStartsAt = JsonParser.string(obj, "starts_at"),
                bannerExpiresAt = JsonParser.string(obj, "expires_at"),
                balance = JsonParser.double(obj, "balance"),
            )
        }
    }
}

/**
 * Мұрағат промо (Flutter PromotionModel, 1:1): GET /announcement/{id}/promotion
 * → {has_promotion, promotion} және /user/announcements/promotions.
 */
data class Promotion(
    val id: Long,
    val packageType: String,
    val status: String, // active | pending | expired
    val maxViews: Int,
    val startViews: Int,
    val currentViews: Int,
    val viewsUsed: Int,
    val remainingViews: Int,
    val progressPercentage: Int,
    val remainingTimeSeconds: Long?,
    val remainingTimeDays: Double?,
    val autoRenewal: Boolean,
) {
    companion object {
        fun fromJson(root: JsonElement?): Promotion? {
            val obj = root as? JsonObject ?: return null
            val id = JsonParser.long(obj, "id") ?: return null
            return Promotion(
                id = id,
                packageType = (JsonParser.string(obj, "package_type") ?: "minimal").lowercase(),
                status = (JsonParser.string(obj, "status") ?: "pending").lowercase(),
                maxViews = JsonParser.int(obj, "max_views") ?: 0,
                startViews = JsonParser.int(obj, "start_views") ?: 0,
                currentViews = JsonParser.int(obj, "current_views") ?: 0,
                viewsUsed = JsonParser.int(obj, "views_used") ?: 0,
                remainingViews = JsonParser.int(obj, "remaining_views") ?: 0,
                progressPercentage = (JsonParser.int(obj, "progress_percentage") ?: 0)
                    .coerceIn(0, 100),
                remainingTimeSeconds = JsonParser.long(obj, "remaining_time_seconds"),
                remainingTimeDays = JsonParser.double(obj, "remaining_time_days"),
                autoRenewal = JsonParser.bool(obj, "auto_renewal") == true,
            )
        }

        /** GET /announcement/{id}/promotion → {has_promotion, promotion}. */
        fun fromAnnouncementResponse(root: JsonElement?): Promotion? {
            val obj = root as? JsonObject ?: return null
            if (JsonParser.bool(obj, "has_promotion") != true) return null
            return fromJson(obj["promotion"])
        }
    }
}

/** Пайдаланушы жарнамасы + оған байланысты промо (Flutter AnnouncementPromotionModel). */
data class AnnouncementPromotion(
    val announcementId: Long,
    val announcementTitle: String,
    val announcementImageUrl: String?,
    val announcementPrice: Double?,
    val announcementCurrency: String?,
    val announcementCity: String?,
    val announcementStatus: String?,
    val hasPromotion: Boolean,
    val promotion: Promotion?,
) {
    companion object {
        fun fromJson(root: JsonElement?): AnnouncementPromotion? {
            val obj = root as? JsonObject ?: return null
            val announcement = MarketplaceParser.parseAnnouncement(
                obj["announcement"] as? JsonObject,
            ) ?: return null
            return AnnouncementPromotion(
                announcementId = announcement.id,
                announcementTitle = announcement.title,
                announcementImageUrl = announcement.imageUrl,
                announcementPrice = announcement.price,
                announcementCurrency = announcement.currency,
                announcementCity = announcement.city,
                announcementStatus = announcement.status,
                hasPromotion = JsonParser.bool(obj, "has_promotion") == true,
                promotion = Promotion.fromJson(obj["promotion"]),
            )
        }

        fun listFromResponse(root: JsonElement?): List<AnnouncementPromotion> {
            val obj = root as? JsonObject ?: return emptyList()
            val arr = JsonParser.arr(obj, "announcements") ?: return emptyList()
            return arr.mapNotNull { fromJson(it) }
        }
    }
}