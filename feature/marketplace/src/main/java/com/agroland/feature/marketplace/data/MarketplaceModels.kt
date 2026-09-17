package com.agroland.feature.marketplace.data

import com.agroland.core.network.json.JsonParser
import com.agroland.feature.location.data.SelectedLocation
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Жарнама карточкасы — AnnouncementModel (Flutter): лента/таңдаулылар/ұқсас.
 */
data class Announcement(
    val id: Long,
    val title: String,
    val price: Double?,
    val currency: String?,
    val city: String?,
    val district: String?,
    val createdAt: String?,
    val imageUrl: String?,
    val imageUrls: List<String>,
    val isFavorite: Boolean,
    val authorId: Long?,
    val status: String?,
    val measurementUnit: String?,
    val viewsCount: Int,
    val callsCount: Int,
    val favoritesCount: Int,
    val messagesCount: Int,
    val rating: Double?,
    val reviewsCount: Int,
    val negotiable: Boolean,
    val isVip: Boolean,
    val isHot: Boolean,
    val boostMultiplier: Int?,
    val autoRenewEnabled: Boolean,
    val allowCart: Boolean,
    val isMarketplace: Boolean,
    val typeAd: String?,
    val deliveryAvailable: Boolean,
    val pickupAvailable: Boolean,
    val pickupAddress: String?,
    /**
     * Лента жауабы қала атауын жібермейді — тек `location` ішіндегі каталог
     * ID-лері келеді. Атау LocationNameResolver арқылы шешіледі; шешілмесе
     * карточкада локация жолы мүлдем көрсетілмейді (иесіз пин болмас үшін).
     */
    val regionId: Int? = null,
    val districtId: Int? = null,
) {
    /** Карточкада көрсетілетін орын — бос болса жол жасырылады. */
    val placeLabel: String
        get() = listOfNotNull(
            city?.takeIf { it.isNotBlank() },
            district?.takeIf { it.isNotBlank() },
        ).distinct().joinToString(", ")
}

/**
 * Жарнаманың жеткізу зонасы — AnnouncementDeliveryZoneInfo (Flutter):
 * себет/чекаудтағы зона таңдауы осы пішімді оқиды (delivery_zones өрісі).
 */
data class AnnouncementDeliveryZone(
    val id: Long,
    val deliveryCost: Double,
    val regionId: Int?,
    val regionName: String?,
    val name: String?,
    val deliveryDaysMin: Int?,
    val deliveryDaysMax: Int?,
) {
    /** Атау: name → region_name → «Зона #id». */
    val displayName: String
        get() = name?.takeIf { it.isNotEmpty() }
            ?: regionName?.takeIf { it.isNotEmpty() }
            ?: "Zone #$id"

    /** Күндер диапазоны бірліксіз («3–5», «≥3», «») — бірлікте caller қосады. */
    val daysRangeRaw: String
        get() = when {
            deliveryDaysMin != null && deliveryDaysMax != null -> "$deliveryDaysMin–$deliveryDaysMax"
            deliveryDaysMin != null -> "≥$deliveryDaysMin"
            else -> ""
        }
}

/** GET /announcements/{id}/delivery-check нәтижесі (buy sheet жеткізу тексеруі). */
data class DeliveryCheckResult(
    val canDeliver: Boolean,
    val zone: AnnouncementDeliveryZone?,
    val pickupAvailable: Boolean,
    val pickupAddress: String?,
)

/** Толық деталь — FullAnnouncementModel: + description, seller, contacts, similar. */
data class FullAnnouncement(
    val base: Announcement,
    val description: String?,
    val priceIncludesVat: Boolean,
    val videoUrl: String?,
    val seller: Seller?,
    val contactNumbers: List<String>,
    val categoryId: Int?,
    val subcategoryId: Int?,
    val userLocationId: Long?,
    val keywords: List<String>,
    val sku: String?,
    val stockQuantity: Int?,
    val additional: List<Announcement>,
    val similar: List<Announcement>,
    /** Dealer жарнамаларының жеткізу зоналары (Фаза 8: себет/чекаут). */
    val deliveryZones: List<AnnouncementDeliveryZone> = emptyList(),
)

data class Seller(
    val id: Long?,
    val name: String?,
    val memberSince: String?,
    val rating: Double?,
    val phoneNumber: String?,
    val isVipSeller: Boolean,
    val avatarUrl: String?,
)

/** Категория — nameLocalized (kk/ru/en/zh; backend өрістері kz/ru/en/ch). */
data class Category(
    val id: Int,
    val nameKk: String?,
    val nameRu: String?,
    val nameEn: String?,
    val nameZh: String?,
    val fallbackName: String?,
    val iconUrl: String?,
    val announcementCount: Int,
    val subcategories: List<Category>,
) {
    fun localizedName(localeTag: String?): String {
        val lang = localeTag?.substringBefore('-')?.lowercase()
        return when (lang) {
            "ru" -> nameRu ?: fallbackName ?: ""
            "en" -> nameEn ?: fallbackName ?: ""
            "zh" -> nameZh ?: fallbackName ?: ""
            else -> nameKk ?: fallbackName ?: ""
        }
    }
}

/** 6 bucket кілті: crops/livestock/products/technology/services/other (UI атауын string-тен алады). */

/** Іздеу ұсынысы — SuggestionModel. */
data class Suggestion(
    val title: String,
    val category: String?,
    val categoryId: Int?,
    val subCategory: String?,
    val subCategoryId: Int?,
    val location: String?,
)

/** Лента беті — items+total+page+pages. */
data class AnnouncementsPage(
    val items: List<Announcement>,
    val total: Int,
    val page: Int,
    val pages: Int,
) {
    val hasMore: Boolean get() = page < pages
}

/** Сұрып түрі — FilterSortType (Flutter): default/dateDesc/dateAsc/priceAsc/priceDesc. */
@Serializable
enum class FilterSort(val queryKey: String?, val queryValue: String?) {
    DEFAULT(null, null),
    DATE_DESC("sort_by_date", "desc"),
    DATE_ASC("sort_by_date", "asc"),
    PRICE_ASC("sort_by_price", "asc"),
    PRICE_DESC("sort_by_price", "desc"),
}

/**
 * Қолданыстағы сүзгі — FilterModel: query, price range, category, negotiable, sort,
 * локация (Фаза 7: country_id/region_id/district_id каталог ID-лері).
 * @Serializable — навигация маршрут параметрі ретінде жіберіледі.
 */
@Serializable
data class AnnouncementFilter(
    val query: String? = null,
    val categoryId: Int? = null,
    val subcategoryId: Int? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val negotiable: Boolean? = null,
    val sort: FilterSort = FilterSort.DEFAULT,
    val orderRandom: Boolean = false,
    val typeAd: String? = null,
    val isVip: Boolean? = null,
    val location: SelectedLocation? = null,
) {
    /** Backend query map — тек толтырылған өрістер. Әдепкі баға диапазоны жіберілмейді. */
    fun toQueryMap(page: Int, limit: Int): Map<String, String> = buildMap {
        query?.takeIf { it.isNotBlank() }?.let { put("q", it.trim()) }
        categoryId?.let { put("category_id", it.toString()) }
        subcategoryId?.let { put("subcategory_id", it.toString()) }
        minPrice?.let { put("min_price", it.toString()) }
        maxPrice?.let { put("max_price", it.toString()) }
        negotiable?.let { put("negotiable", it.toString()) }
        if (orderRandom) put("order_random", "true")
        typeAd?.let { put("type_ad", it) }
        isVip?.let { put("is_vip", it.toString()) }
        location?.let { loc ->
            loc.countryId?.let { put("country_id", it.toString()) }
            loc.regionId?.let { put("region_id", it.toString()) }
            loc.districtId?.let { put("district_id", it.toString()) }
        }
        sort.queryKey?.let { key -> put(key, sort.queryValue ?: "desc") }
        put("page", page.toString())
        put("limit", limit.toString())
    }

    /** Басты беттегі «Сүзгі» жолағында көрсетілетін белсенді шарттар саны. */
    fun activeCount(): Int = listOfNotNull(
        query?.takeIf { it.isNotBlank() },
        categoryId,
        subcategoryId,
        minPrice,
        maxPrice,
        negotiable,
        isVip,
        typeAd,
        location?.takeIf { it.isNotEmpty },
        sort.takeIf { it != FilterSort.DEFAULT },
    ).size

    fun isDefault(): Boolean =
        query.isNullOrBlank() && categoryId == null && subcategoryId == null &&
            minPrice == null && maxPrice == null && negotiable == null &&
            sort == FilterSort.DEFAULT && typeAd == null && location == null
}

/** Парсер — бір ғана орыннан. */
object MarketplaceParser {

    fun parseAnnouncementPage(root: JsonObject?): AnnouncementsPage {
        val items = JsonParser.arrayOrSingle(root, "items")
            .mapNotNull { parseAnnouncement(it as? JsonObject) }
        return AnnouncementsPage(
            items = items,
            total = JsonParser.int(root, "total") ?: items.size,
            page = JsonParser.int(root, "page") ?: 1,
            pages = JsonParser.int(root, "pages")
                ?: if (items.isEmpty()) 0 else 1,
        )
    }

    fun parseAnnouncement(obj: JsonObject?): Announcement? {
        if (obj == null) return null
        val id = JsonParser.long(obj, "id") ?: return null
        val location = JsonParser.obj(obj, "location")
        val images = JsonParser.arrayOrSingle(obj, "image_urls")
            .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
            .filter { it.isNotBlank() }
        return Announcement(
            id = id,
            title = JsonParser.string(obj, "title") ?: "",
            price = JsonParser.double(obj, "price"),
            currency = JsonParser.string(obj, "currency") ?: "₸",
            city = JsonParser.string(obj, "city") ?: JsonParser.string(obj, "city_name"),
            district = JsonParser.string(obj, "district") ?: JsonParser.string(obj, "district_name"),
            createdAt = JsonParser.string(obj, "created_at") ?: JsonParser.string(obj, "createdAt"),
            // Бэк негізгі суретті main_image_url деп жібереді (Flutter
            // @JsonKey('main_image_url')); image_url — толық деталь жауабында.
            imageUrl = JsonParser.string(obj, "main_image_url")
                ?: JsonParser.string(obj, "image_url")
                ?: images.firstOrNull(),
            imageUrls = images,
            isFavorite = JsonParser.bool(obj, "is_favorite") ?: JsonParser.bool(obj, "isFavorite") ?: false,
            authorId = JsonParser.long(obj, "author_id") ?: JsonParser.long(obj, "user_id"),
            status = JsonParser.string(obj, "status"),
            measurementUnit = JsonParser.string(obj, "measurement_unit"),
            // Лентада views_count, толық деталь жауабында views (Flutter
            // AnnouncementModel vs FullAnnouncementModel @JsonKey айырмасы).
            viewsCount = JsonParser.int(obj, "views_count") ?: JsonParser.int(obj, "views") ?: 0,
            callsCount = JsonParser.int(obj, "calls_count") ?: 0,
            favoritesCount = JsonParser.int(obj, "favorites_count") ?: 0,
            messagesCount = JsonParser.int(obj, "messages_count") ?: 0,
            rating = JsonParser.double(obj, "rating"),
            reviewsCount = JsonParser.int(obj, "reviews_count") ?: 0,
            negotiable = JsonParser.bool(obj, "negotiable") ?: false,
            isVip = JsonParser.bool(obj, "is_vip") ?: false,
            isHot = JsonParser.bool(obj, "is_hot")
                ?: JsonParser.bool(obj, "hot") ?: false,
            boostMultiplier = JsonParser.int(obj, "boost_multiplier"),
            autoRenewEnabled = JsonParser.bool(obj, "auto_renew_enabled") ?: false,
            allowCart = JsonParser.bool(obj, "allow_cart") ?: false,
            isMarketplace = JsonParser.bool(obj, "is_marketplace") ?: false,
            typeAd = JsonParser.string(obj, "type_ad"),
            deliveryAvailable = JsonParser.bool(obj, "delivery_available") ?: false,
            pickupAvailable = JsonParser.bool(obj, "pickup_available") ?: false,
            pickupAddress = JsonParser.string(obj, "pickup_address"),
            regionId = JsonParser.int(obj, "region_id") ?: JsonParser.int(location, "region_id"),
            districtId = JsonParser.int(obj, "district_id") ?: JsonParser.int(location, "district_id"),
        )
    }

    /**
     * Бэк деталь жауабын `{"announcement": {...}}` деп орайды (Flutter
     * announcements_repository.getAnnouncement: `(r as Map)['announcement']`).
     * Орам болмаса — түбірдің өзін оқимыз (тесттер/кеш үшін кешірімді).
     */
    fun parseFullAnnouncement(response: JsonObject?): FullAnnouncement? {
        val root = JsonParser.obj(response, "announcement") ?: response
        val parsed = parseAnnouncement(root) ?: return null
        val seller = parseSeller(JsonParser.obj(root, "seller"))
        // Деталь жауабында author_id жоқ — сатушы тек `seller` нысанында келеді.
        // Чат пен «сатушының пікірлері» осы id-ге сүйенеді, сондықтан толтырамыз.
        val base = if (parsed.authorId == null) parsed.copy(authorId = seller?.id) else parsed
        return FullAnnouncement(
            base = base,
            description = JsonParser.string(root, "description"),
            priceIncludesVat = JsonParser.bool(root, "price_includes_vat") ?: false,
            videoUrl = JsonParser.string(root, "video_url"),
            seller = seller,
            contactNumbers = JsonParser.arrayOrSingle(root, "contact_numbers").mapNotNull { el ->
                when (el) {
                    is kotlinx.serialization.json.JsonPrimitive -> el.content
                    is JsonObject -> JsonParser.string(el, "phone_number") ?: JsonParser.string(el, "phone")
                    else -> null
                }
            },
            categoryId = JsonParser.int(root, "category_id"),
            subcategoryId = JsonParser.int(root, "subcategory_id"),
            userLocationId = JsonParser.long(root, "user_location_id"),
            keywords = JsonParser.arrayOrSingle(root, "keywords")
                .plus(JsonParser.arrayOrSingle(root, "tags"))
                .mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                .filter { it.isNotBlank() },
            sku = JsonParser.string(root, "sku"),
            stockQuantity = JsonParser.int(root, "stock_quantity"),
            additional = JsonParser.arrayOrSingle(root, "additional_announcements")
                .mapNotNull { parseAnnouncement(it as? JsonObject) },
            similar = JsonParser.arrayOrSingle(root, "similar_announcements")
                .mapNotNull { parseAnnouncement(it as? JsonObject) },
            deliveryZones = parseDeliveryZoneInfos(root),
        )
    }

    /** Жарнама жауабындағы delivery_zones — [{id, name, region_name, delivery_cost, delivery_days_min/max}]. */
    fun parseDeliveryZoneInfos(root: JsonObject?): List<AnnouncementDeliveryZone> =
        JsonParser.arrayOrSingle(root, "delivery_zones").mapNotNull { parseDeliveryZone(it as? JsonObject) }

    /** Бір зона нысаны — id немесе delivery_zone_id (екеуі де келеді). */
    fun parseDeliveryZone(obj: JsonObject?): AnnouncementDeliveryZone? {
        if (obj == null) return null
        val id = JsonParser.long(obj, "id") ?: JsonParser.long(obj, "delivery_zone_id") ?: return null
        return AnnouncementDeliveryZone(
            id = id,
            deliveryCost = JsonParser.double(obj, "delivery_cost") ?: 0.0,
            regionId = JsonParser.int(obj, "region_id"),
            regionName = JsonParser.string(obj, "region_name"),
            name = JsonParser.string(obj, "name"),
            deliveryDaysMin = JsonParser.int(obj, "delivery_days_min"),
            deliveryDaysMax = JsonParser.int(obj, "delivery_days_max"),
        )
    }

    /** delivery-check жауабы — {can_deliver, zone, pickup_available, pickup_address}. */
    fun parseDeliveryCheck(root: JsonObject?): DeliveryCheckResult = DeliveryCheckResult(
        canDeliver = JsonParser.bool(root, "can_deliver") ?: false,
        zone = parseDeliveryZone(JsonParser.obj(root, "zone")),
        pickupAvailable = JsonParser.bool(root, "pickup_available") ?: false,
        pickupAddress = JsonParser.string(root, "pickup_address"),
    )

    fun parseSeller(obj: JsonObject?): Seller? {
        if (obj == null) return null
        return Seller(
            id = JsonParser.long(obj, "id"),
            name = JsonParser.string(obj, "name"),
            memberSince = JsonParser.string(obj, "member_since") ?: JsonParser.string(obj, "created_at"),
            rating = JsonParser.double(obj, "rating"),
            phoneNumber = JsonParser.string(obj, "phone_number") ?: JsonParser.string(obj, "phone"),
            isVipSeller = JsonParser.bool(obj, "is_vip_seller") ?: false,
            avatarUrl = JsonParser.string(obj, "avatar_url"),
        )
    }

    fun parseCategory(obj: JsonObject?): Category? {
        if (obj == null) return null
        val id = JsonParser.int(obj, "id") ?: return null
        return Category(
            id = id,
            nameKk = JsonParser.string(obj, "name_kz") ?: JsonParser.string(obj, "name_kk"),
            nameRu = JsonParser.string(obj, "name_ru"),
            nameEn = JsonParser.string(obj, "name_en"),
            nameZh = JsonParser.string(obj, "name_ch") ?: JsonParser.string(obj, "name_zh"),
            fallbackName = JsonParser.string(obj, "name"),
            iconUrl = JsonParser.string(obj, "icon_url") ?: JsonParser.string(obj, "image_url"),
            announcementCount = JsonParser.int(obj, "announcement_count") ?: 0,
            subcategories = JsonParser.arrayOrSingle(obj, "subcategories")
                .mapNotNull { parseCategory(it as? JsonObject) },
        )
    }

    /** Жазық тізім: {items:[...]} немесе түбірде {categories:[...]}. */
    fun parseCategoryList(root: JsonObject?): List<Category> =
        JsonParser.arrayOrSingle(root, "items")
            .plus(JsonParser.arrayOrSingle(root, "categories"))
            .mapNotNull { parseCategory(it as? JsonObject) }

    /** Grouped: {crops:[...], livestock:[...], products:[...], technology:[...], services:[...], other:[...]}. */
    fun parseGroupedCategories(root: JsonObject?): Map<String, List<Category>> {
        if (root == null) return emptyMap()
        return listOf("crops", "livestock", "products", "technology", "services", "other").associateWith { key ->
            JsonParser.arrayOrSingle(root, key).mapNotNull { parseCategory(it as? JsonObject) }
        }
    }

    fun parseSuggestions(root: JsonObject?): List<Suggestion> =
        JsonParser.arrayOrSingle(root, "items")
            .plus(JsonParser.arrayOrSingle(root, "suggestions"))
            .mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                Suggestion(
                    title = JsonParser.string(obj, "title") ?: JsonParser.string(obj, "name") ?: return@mapNotNull null,
                    category = JsonParser.string(obj, "category"),
                    categoryId = JsonParser.int(obj, "category_id") ?: JsonParser.int(obj, "categoryId"),
                    subCategory = JsonParser.string(obj, "sub_category") ?: JsonParser.string(obj, "subCategory"),
                    subCategoryId = JsonParser.int(obj, "subcategory_id")
                        ?: JsonParser.int(obj, "sub_category_id")
                        ?: JsonParser.int(obj, "subCategoryId"),
                    location = JsonParser.string(obj, "location"),
                )
            }

    fun parseFavoriteStatus(root: JsonObject?): Boolean =
        JsonParser.bool(root, "is_favorite") ?: JsonParser.bool(root, "isFavorite")
            ?: JsonParser.bool(root, "favorite") ?: false
}