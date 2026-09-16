package com.agroland.feature.dealer.data

import com.agroland.core.network.json.JsonParser
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Дилер өнімі — Flutter DealerProductModel (1:1): GET /dealer/products →
 * {items, total, page, size, pages}. Барлық өріс JsonParser арқылы кешірімді
 * оқылады (типтер тұрақсыз: backend кейде string/num аралас жібереді).
 */
data class DealerProduct(
    val id: Long,
    val title: String,
    val price: Double,
    val currency: String,
    val sku: String?,
    val stockQuantity: Double?,
    val status: String,
    val viewsCount: Int,
    val callsCount: Int,
    val favoritesCount: Int,
    val mainImageUrl: String,
    val isVip: Boolean,
    val deliveryAvailable: Boolean,
    val pickupAvailable: Boolean,
    val createdAt: String?,
)

data class DealerProductsResponse(
    val items: List<DealerProduct>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
    val pages: Int,
    val canLoadMore: Boolean,
)

/** Жеткізу аймағы — Flutter DeliveryZoneModel (1:1): GET/POST/PATCH /dealer/delivery-zones. */
data class DeliveryZone(
    val id: Long,
    val countryId: Int,
    val regionId: Int,
    val districtId: Int?,
    val deliveryCost: Double,
    val deliveryDaysMin: Int?,
    val deliveryDaysMax: Int?,
    val isActive: Boolean,
    val note: String,
    val name: String,
    val availableDays: List<Int>, // 1=Дс .. 7=Жс
    val timeStart: String?, // "09:00"
    val timeEnd: String?, // "18:00"
    val countryName: String,
    val regionName: String,
    val districtName: String,
    val zonePrices: List<ZonePriceItem>,
) {
    /** displayName: name → «region — district» → «Zone #id». */
    val displayName: String
        get() {
            if (name.isNotEmpty()) return name
            val parts = listOf(regionName, districtName).filter { it.isNotBlank() }
            return parts.joinToString(" — ")
        }

    /** daysRange: «1–3», «≥1», «≤3», «-». */
    val daysRange: String
        get() = when {
            deliveryDaysMin != null && deliveryDaysMax != null -> "$deliveryDaysMin–$deliveryDaysMax"
            deliveryDaysMin != null -> "≥$deliveryDaysMin"
            deliveryDaysMax != null -> "≤$deliveryDaysMax"
            else -> "-"
        }
}

/** Аудан + баға (zone_prices[]) — district.name_ru оқылады. */
data class ZonePriceItem(
    val districtId: Int?,
    val districtName: String,
    val price: Double,
)

/** Дилер тапсырысы — Flutter DealerOrderItem (1:1): GET /dealer/orders. */
data class DealerOrder(
    val id: Long,
    val status: String,
    val buyerId: Long?,
    val sellerId: Long?,
    val announcementId: Long?,
    val totalAmount: Double?,
    val agreedPrice: Double?,
    val paymentStatus: String?,
    val pickupAddress: String?,
    val deliveryAddress: String?,
    val loadingDate: String?,
    val deliveryDate: String?,
    val paidAt: String?,
    val createdAt: String?,
    val buyerName: String?,
    val buyerPhone: String?,
    val notes: String?,
    val title: String?,
    val mainImageUrl: String?,
    val quantity: Double,
    val measurementUnit: String?,
    val deliveryZones: List<OrderDeliveryZone>,
)

/** Тапсырыс ішіндегі жеткізу зонасы (delivery_zones[]). */
data class OrderDeliveryZone(
    val id: Long,
    val name: String?,
    val region: String?,
    val district: String?,
    val deliveryDaysMin: Int?,
    val deliveryDaysMax: Int?,
    val deliveryCost: Double?,
) {
    val displayName: String
        get() = if (!name.isNullOrBlank()) name
        else listOfNotNull(region, district).filter { it.isNotBlank() }.joinToString(" — ")
}

data class DealerOrdersResponse(
    val tab: String,
    val items: List<DealerOrder>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
    val pages: Int,
    val canLoadMore: Boolean,
    val tabCounts: Map<String, Int>,
)

/** Тапсырыс трекингі — Flutter OrderTrackingModel (1:1): GET /orders/{id}/tracking. */
data class OrderTracking(
    val orderId: Long,
    val status: String,
    val deliveryDate: String?,
    val loadingDate: String?,
    val pickupAddress: String?,
    val deliveryAddress: String?,
    val waybills: List<Waybill>,
    val timeline: List<TrackingEvent>,
)

data class Waybill(
    val id: Long,
    val waybillNumber: String,
    val vehicleNumber: String?,
    val driverName: String?,
    val shipmentDate: String?,
    val loadingPoint: String?,
    val deliveryPoint: String?,
    val signedBySeller: Boolean,
    val signedByBuyer: Boolean,
    val qrCodeUrl: String?,
)

data class TrackingEvent(
    val status: String,
    val note: String?,
    val createdAt: String?,
)

/** Team pool — бөлінбеген тапсырыс (GET /orders/team-pool, bare list). */
data class TeamPoolOrder(
    val id: Long,
    val buyerId: Long?,
    val announcementId: Long?,
    val status: String?,
    val quantity: Double?,
    val agreedPrice: Double?,
    val totalAmount: Double?,
    val createdAt: String?,
)

/** POST /orders/{id}/claim нәтижесі. */
data class ClaimResult(
    val orderId: Long,
    val assignedUserId: Long?,
    val status: String?,
)

/** Команда мүшесі — Flutter DealerEmployee (1:1); profilePicture camelCase!. */
data class DealerEmployee(
    val id: Long,
    val name: String,
    val dealerRole: String?,
    val iin: String?,
    val phone: String?,
    val profilePicture: String?,
)

/** Аналитика жиындықтары — Flutter DealerAnalyticsTotals (1:1). */
data class DealerAnalyticsTotals(
    val products: Int,
    val views: Int,
    val clicks: Int,
    val favorites: Int,
    val orders: Int,
    val completedOrders: Int,
    val conversion: Double,
    val revenue: Double,
    val activePromotions: Int,
)

data class TopAnnouncement(
    val announcementId: Long,
    val title: String,
    val views: Int,
    val clicks: Int,
    val favorites: Int,
    val orders: Int,
    val revenue: Double,
)

data class DealerAnalytics(
    val periodDays: Int,
    val totals: DealerAnalyticsTotals,
    val topAnnouncements: List<TopAnnouncement>,
)

/** Timeseries нүктесі — GET /dealer/analytics/timeseries (points[]). */
data class AnalyticsPoint(
    val date: String,
    val revenue: Double,
    val orders: Int,
)

/**
 * Тариф шарттары — Flutter TariffConditions (1:1): GET /subscription/current →
 * {data} → top-level `conditions` ?? `plan.conditions`, үстінен fallback
 * (жоқ жазылымда: 3 жарнама, 5 фото, аналитика өшірулі). Аналитика гейті
 * hasFeature("analytics_enabled") арқылы оқылады.
 */
data class TariffConditions(
    val maxActiveAnnouncements: Int?,
    val maxPhotosPerAd: Int?,
    val analyticsEnabled: Boolean?,
    val personalManager: Boolean?,
    val topPlacementSlots: Int?,
    val brandShowcase: Boolean?,
    val chinaImportAccess: Boolean?,
    val searchBoostMultiplier: Double?,
    val supportPriority: String?,
) {
    fun hasFeature(key: String): Boolean = when (key) {
        "analytics_enabled" -> analyticsEnabled == true
        "personal_manager" -> personalManager == true
        "brand_showcase" -> brandShowcase == true
        "china_import_access" -> chinaImportAccess == true
        else -> false
    }

    companion object {
        /** Жоқ жазылымның әдепкілері (auth-less fallback). */
        val FALLBACK = TariffConditions(
            maxActiveAnnouncements = 3,
            maxPhotosPerAd = 5,
            analyticsEnabled = false,
            personalManager = false,
            topPlacementSlots = 0,
            brandShowcase = false,
            chinaImportAccess = false,
            searchBoostMultiplier = 1.0,
            supportPriority = "standard",
        )

        fun fromJson(obj: JsonObject?): TariffConditions? {
            obj ?: return null
            return TariffConditions(
                maxActiveAnnouncements = JsonParser.int(obj, "max_active_announcements"),
                maxPhotosPerAd = JsonParser.int(obj, "max_photos_per_ad"),
                analyticsEnabled = JsonParser.bool(obj, "analytics_enabled"),
                personalManager = JsonParser.bool(obj, "personal_manager"),
                topPlacementSlots = JsonParser.int(obj, "top_placement_slots"),
                brandShowcase = JsonParser.bool(obj, "brand_showcase"),
                chinaImportAccess = JsonParser.bool(obj, "china_import_access"),
                searchBoostMultiplier = JsonParser.double(obj, "search_boost_multiplier"),
                supportPriority = JsonParser.string(obj, "support_priority"),
            )
        }

        /** Пайдаланушы жауабынан effektive шарттар: conditions ?? plan.conditions → fallback үстіне. */
        fun effectiveFrom(root: JsonObject?): TariffConditions {
            val data = JsonParser.obj(root, "data") ?: root ?: return FALLBACK
            val parsed = JsonParser.obj(data, "conditions")
                ?: JsonParser.obj(JsonParser.obj(data, "plan"), "conditions")
                ?: return FALLBACK
            val f = FALLBACK
            return TariffConditions(
                maxActiveAnnouncements = JsonParser.int(parsed, "max_active_announcements")
                    ?: f.maxActiveAnnouncements,
                maxPhotosPerAd = JsonParser.int(parsed, "max_photos_per_ad") ?: f.maxPhotosPerAd,
                analyticsEnabled = JsonParser.bool(parsed, "analytics_enabled")
                    ?: f.analyticsEnabled,
                personalManager = JsonParser.bool(parsed, "personal_manager") ?: f.personalManager,
                topPlacementSlots = JsonParser.int(parsed, "top_placement_slots")
                    ?: f.topPlacementSlots,
                brandShowcase = JsonParser.bool(parsed, "brand_showcase") ?: f.brandShowcase,
                chinaImportAccess = JsonParser.bool(parsed, "china_import_access")
                    ?: f.chinaImportAccess,
                searchBoostMultiplier = JsonParser.double(parsed, "search_boost_multiplier")
                    ?: f.searchBoostMultiplier,
                supportPriority = JsonParser.string(parsed, "support_priority") ?: f.supportPriority,
            )
        }
    }
}

/**
 * Дилер парсері — барлық парсинг бір жерде (ProfileParser үлгісі).
 * Толтырылмайтын объект қате емес, null қайтарылады (тізім элементтері
 * mapNotNull арқылы алынады).
 */
object DealerParser {

    fun parseProducts(root: JsonElement?): DealerProductsResponse {
        val obj = root as? JsonObject ?: return DealerProductsResponse(emptyList(), 0, 1, 0, 0, false)
        val items = JsonParser.arr(obj, "items")?.mapNotNull { parseProduct(it as? JsonObject) }
            ?: emptyList()
        val page = JsonParser.int(obj, "page") ?: 1
        val pages = JsonParser.int(obj, "pages") ?: 1
        return DealerProductsResponse(
            items = items,
            total = JsonParser.int(obj, "total") ?: items.size,
            page = page,
            pageSize = JsonParser.int(obj, "size") ?: items.size,
            pages = pages,
            canLoadMore = page < pages,
        )
    }

    private fun parseProduct(obj: JsonObject?): DealerProduct? {
        obj ?: return null
        val id = JsonParser.long(obj, "id") ?: return null
        return DealerProduct(
            id = id,
            title = JsonParser.string(obj, "title") ?: "",
            price = JsonParser.double(obj, "price") ?: 0.0,
            currency = JsonParser.string(obj, "currency") ?: "KZT",
            sku = JsonParser.string(obj, "sku"),
            stockQuantity = JsonParser.double(obj, "stock_quantity"),
            status = (JsonParser.string(obj, "status") ?: "active").lowercase(),
            viewsCount = JsonParser.int(obj, "views_count") ?: 0,
            callsCount = JsonParser.int(obj, "calls_count") ?: 0,
            favoritesCount = JsonParser.int(obj, "favorites_count") ?: 0,
            mainImageUrl = JsonParser.string(obj, "main_image_url") ?: "",
            isVip = JsonParser.bool(obj, "is_vip") == true,
            deliveryAvailable = JsonParser.bool(obj, "delivery_available") == true,
            pickupAvailable = JsonParser.bool(obj, "pickup_available") == true,
            createdAt = JsonParser.string(obj, "created_at"),
        )
    }

    fun parseZones(root: JsonElement?): List<DeliveryZone> {
        // Bare list жауабы (Flutter is List check); бір объект — POST/PATCH
        // нәтижесі (id бар); қаптама келсе — {items:[]} tolerance.
        return when (root) {
            is kotlinx.serialization.json.JsonArray ->
                root.mapNotNull { parseZone(it as? JsonObject) }
            is JsonObject -> {
                val single = parseZone(root)
                if (single != null) {
                    listOf(single)
                } else {
                    JsonParser.arrayOrSingle(root, "items")
                        .mapNotNull { parseZone(it as? JsonObject) }
                }
            }
            else -> emptyList()
        }
    }

    private fun parseZone(obj: JsonObject?): DeliveryZone? {
        obj ?: return null
        val id = JsonParser.long(obj, "id") ?: return null
        return DeliveryZone(
            id = id,
            countryId = JsonParser.int(obj, "country_id") ?: 0,
            regionId = JsonParser.int(obj, "region_id") ?: 0,
            districtId = JsonParser.int(obj, "district_id"),
            deliveryCost = JsonParser.double(obj, "delivery_cost") ?: 0.0,
            deliveryDaysMin = JsonParser.int(obj, "delivery_days_min"),
            deliveryDaysMax = JsonParser.int(obj, "delivery_days_max"),
            isActive = JsonParser.bool(obj, "is_active") ?: true,
            note = JsonParser.string(obj, "note") ?: "",
            name = JsonParser.string(obj, "name") ?: "",
            availableDays = JsonParser.arrayOrSingle(obj, "available_days")
                .mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.toIntOrNull() },
            timeStart = JsonParser.string(obj, "time_start"),
            timeEnd = JsonParser.string(obj, "time_end"),
            countryName = JsonParser.string(obj, "country_name") ?: "",
            regionName = JsonParser.string(obj, "region_name") ?: "",
            districtName = JsonParser.string(obj, "district_name") ?: "",
            zonePrices = parseZonePrices(obj["zone_prices"]),
        )
    }

    private fun parseZonePrices(root: JsonElement?): List<ZonePriceItem> {
        val arr = root as? kotlinx.serialization.json.JsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val district = JsonParser.obj(obj, "district")
            ZonePriceItem(
                districtId = JsonParser.int(obj, "district_id"),
                districtName = JsonParser.string(district, "name_ru")
                    ?: JsonParser.string(district, "name") ?: "",
                price = JsonParser.double(obj, "delivery_cost") ?: 0.0,
            )
        }
    }

    fun parseOrders(root: JsonElement?): DealerOrdersResponse {
        val obj = root as? JsonObject
            ?: return DealerOrdersResponse("", emptyList(), 0, 1, 0, 0, false, emptyMap())
        val items = JsonParser.arr(obj, "items")?.mapNotNull { parseOrder(it as? JsonObject) }
            ?: emptyList()
        val page = JsonParser.int(obj, "page") ?: 1
        val pages = JsonParser.int(obj, "pages") ?: 1
        return DealerOrdersResponse(
            tab = JsonParser.string(obj, "tab") ?: "",
            items = items,
            total = JsonParser.int(obj, "total") ?: items.size,
            page = page,
            pageSize = JsonParser.int(obj, "size") ?: items.size,
            pages = pages,
            canLoadMore = page < pages,
            tabCounts = parseTabCounts(obj["tab_counts"]),
        )
    }

    /** TabCountsConverter (1:1): жарамсыз пішін ешқашан crash жасамайды → {}. */
    private fun parseTabCounts(root: JsonElement?): Map<String, Int> {
        val obj = root as? JsonObject ?: return emptyMap()
        val result = mutableMapOf<String, Int>()
        obj.forEach { (key, value) ->
            val parsed = when (value) {
                is kotlinx.serialization.json.JsonPrimitive ->
                    value.contentOrNull?.toDoubleOrNull()?.toInt()
                else -> null
            }
            if (parsed != null) result[key] = parsed
        }
        return result
    }

    private fun parseOrder(obj: JsonObject?): DealerOrder? {
        obj ?: return null
        val id = JsonParser.long(obj, "id") ?: return null
        return DealerOrder(
            id = id,
            status = (JsonParser.string(obj, "status") ?: "").lowercase(),
            buyerId = JsonParser.long(obj, "buyer_id"),
            sellerId = JsonParser.long(obj, "seller_id"),
            announcementId = JsonParser.long(obj, "announcement_id"),
            totalAmount = JsonParser.double(obj, "total_amount"),
            agreedPrice = JsonParser.double(obj, "agreed_price"),
            paymentStatus = JsonParser.string(obj, "payment_status"),
            pickupAddress = JsonParser.string(obj, "pickup_address"),
            deliveryAddress = JsonParser.string(obj, "delivery_address"),
            loadingDate = JsonParser.string(obj, "loading_date"),
            deliveryDate = JsonParser.string(obj, "delivery_date"),
            paidAt = JsonParser.string(obj, "paid_at"),
            createdAt = JsonParser.string(obj, "created_at"),
            buyerName = JsonParser.string(obj, "buyer_name"),
            buyerPhone = JsonParser.string(obj, "buyer_phone"),
            notes = JsonParser.string(obj, "notes"),
            title = JsonParser.string(obj, "title"),
            mainImageUrl = JsonParser.string(obj, "main_image_url"),
            quantity = JsonParser.double(obj, "quantity") ?: 1.0,
            measurementUnit = JsonParser.string(obj, "measurement_unit"),
            deliveryZones = JsonParser.arrayOrSingle(obj, "delivery_zones")
                .mapNotNull { parseOrderZone(it as? JsonObject) },
        )
    }

    private fun parseOrderZone(obj: JsonObject?): OrderDeliveryZone? {
        obj ?: return null
        return OrderDeliveryZone(
            id = JsonParser.long(obj, "id") ?: 0L,
            name = JsonParser.string(obj, "name"),
            region = JsonParser.string(obj, "region"),
            district = JsonParser.string(obj, "district"),
            deliveryDaysMin = JsonParser.int(obj, "delivery_days_min"),
            deliveryDaysMax = JsonParser.int(obj, "delivery_days_max"),
            deliveryCost = JsonParser.double(obj, "delivery_cost"),
        )
    }

    fun parseTeamPool(root: JsonElement?): List<TeamPoolOrder> {
        val arr = root as? kotlinx.serialization.json.JsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val id = JsonParser.long(obj, "id") ?: return@mapNotNull null
            TeamPoolOrder(
                id = id,
                buyerId = JsonParser.long(obj, "buyer_id"),
                announcementId = JsonParser.long(obj, "announcement_id"),
                status = JsonParser.string(obj, "status"),
                quantity = JsonParser.double(obj, "quantity"),
                agreedPrice = JsonParser.double(obj, "agreed_price"),
                totalAmount = JsonParser.double(obj, "total_amount"),
                createdAt = JsonParser.string(obj, "created_at"),
            )
        }
    }

    fun parseClaim(root: JsonElement?): ClaimResult? {
        val obj = root as? JsonObject ?: return null
        val orderId = JsonParser.long(obj, "order_id") ?: return null
        return ClaimResult(
            orderId = orderId,
            assignedUserId = JsonParser.long(obj, "assigned_user_id"),
            status = JsonParser.string(obj, "status"),
        )
    }

    fun parseEmployees(root: JsonElement?): List<DealerEmployee> {
        val obj = root as? JsonObject ?: return emptyList()
        return JsonParser.arr(obj, "employees")?.mapNotNull { el ->
            val e = el as? JsonObject ?: return@mapNotNull null
            val id = JsonParser.long(e, "id") ?: return@mapNotNull null
            DealerEmployee(
                id = id,
                name = JsonParser.string(e, "name") ?: "",
                dealerRole = JsonParser.string(e, "dealer_role"),
                iin = JsonParser.string(e, "iin"),
                phone = JsonParser.string(e, "phone"),
                profilePicture = JsonParser.string(e, "profilePicture")
                    ?: JsonParser.string(e, "profile_picture"),
            )
        } ?: emptyList()
    }

    fun parseAnalytics(root: JsonElement?): DealerAnalytics? {
        val obj = root as? JsonObject ?: return null
        val totalsObj = JsonParser.obj(obj, "totals")
        return DealerAnalytics(
            periodDays = JsonParser.int(obj, "period_days") ?: 30,
            totals = DealerAnalyticsTotals(
                products = JsonParser.int(totalsObj, "products") ?: 0,
                views = JsonParser.int(totalsObj, "views") ?: 0,
                clicks = JsonParser.int(totalsObj, "clicks") ?: 0,
                favorites = JsonParser.int(totalsObj, "favorites") ?: 0,
                orders = JsonParser.int(totalsObj, "orders") ?: 0,
                completedOrders = JsonParser.int(totalsObj, "completed_orders") ?: 0,
                conversion = JsonParser.double(totalsObj, "conversion") ?: 0.0,
                revenue = JsonParser.double(totalsObj, "revenue") ?: 0.0,
                activePromotions = JsonParser.int(totalsObj, "active_promotions") ?: 0,
            ),
            topAnnouncements = JsonParser.arrayOrSingle(obj, "top_announcements")
                .mapNotNull { el ->
                    val t = el as? JsonObject ?: return@mapNotNull null
                    val aid = JsonParser.long(t, "announcement_id") ?: return@mapNotNull null
                    TopAnnouncement(
                        announcementId = aid,
                        title = JsonParser.string(t, "title") ?: "",
                        views = JsonParser.int(t, "views") ?: 0,
                        clicks = JsonParser.int(t, "clicks") ?: 0,
                        favorites = JsonParser.int(t, "favorites") ?: 0,
                        orders = JsonParser.int(t, "orders") ?: 0,
                        revenue = JsonParser.double(t, "revenue") ?: 0.0,
                    )
                },
        )
    }

    fun parseTimeseries(root: JsonElement?): List<AnalyticsPoint> {
        // {data:{points:[...]}} қаптамасында келуі мүмкін (Flutter сияқты);
        // бұзылған жауап — қате емес, бос тізім (аналитика табы ешқашан құламайды).
        val obj = try {
            (root as? JsonObject)?.let {
                (it["data"] as? JsonObject) ?: it
            }
        } catch (_: Exception) {
            null
        } ?: return emptyList()
        return JsonParser.arrayOrSingle(obj, "points").mapNotNull { el ->
            val p = el as? JsonObject ?: return@mapNotNull null
            AnalyticsPoint(
                date = JsonParser.string(p, "date") ?: "",
                revenue = JsonParser.double(p, "revenue") ?: 0.0,
                orders = JsonParser.int(p, "orders") ?: 0,
            )
        }
    }

    fun parseTracking(root: JsonElement?): OrderTracking? {
        val obj = root as? JsonObject ?: return null
        val orderId = JsonParser.long(obj, "order_id") ?: return null
        return OrderTracking(
            orderId = orderId,
            status = JsonParser.string(obj, "status") ?: "",
            deliveryDate = JsonParser.string(obj, "delivery_date"),
            loadingDate = JsonParser.string(obj, "loading_date"),
            pickupAddress = JsonParser.string(obj, "pickup_address"),
            deliveryAddress = JsonParser.string(obj, "delivery_address"),
            waybills = JsonParser.arrayOrSingle(obj, "waybills").mapNotNull { el ->
                val w = el as? JsonObject ?: return@mapNotNull null
                Waybill(
                    id = JsonParser.long(w, "id") ?: 0L,
                    waybillNumber = JsonParser.string(w, "waybill_number") ?: "",
                    vehicleNumber = JsonParser.string(w, "vehicle_number"),
                    driverName = JsonParser.string(w, "driver_name"),
                    shipmentDate = JsonParser.string(w, "shipment_date"),
                    loadingPoint = JsonParser.string(w, "loading_point"),
                    deliveryPoint = JsonParser.string(w, "delivery_point"),
                    signedBySeller = JsonParser.bool(w, "signed_by_seller") == true,
                    signedByBuyer = JsonParser.bool(w, "signed_by_buyer") == true,
                    qrCodeUrl = JsonParser.string(w, "qr_code_url"),
                )
            },
            timeline = JsonParser.arrayOrSingle(obj, "timeline").mapNotNull { el ->
                val t = el as? JsonObject ?: return@mapNotNull null
                TrackingEvent(
                    status = JsonParser.string(t, "status") ?: "",
                    note = JsonParser.string(t, "note"),
                    createdAt = JsonParser.string(t, "created_at"),
                )
            },
        )
    }

    /** GET /announcement/reject-message/{id} → {message}. */
    fun parseRejectMessage(root: JsonElement?): String {
        val obj = root as? JsonObject ?: return ""
        return JsonParser.string(obj, "message") ?: ""
    }
}