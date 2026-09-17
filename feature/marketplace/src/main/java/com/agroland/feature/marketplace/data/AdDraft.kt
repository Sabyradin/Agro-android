package com.agroland.feature.marketplace.data

import com.agroland.core.network.json.JsonParser
import com.agroland.feature.location.data.SelectedLocation
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Жарнама жасау/өңдеу draft-ы — CreateAdState (Flutter CreateAdNotifier).
 * @Serializable емес: экран ішінде сақталады, навигациядан өтпейді.
 */
data class AdDraft(
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val currency: String = DEFAULT_CURRENCY,
    val negotiable: Boolean = false,
    val priceIncludesVat: Boolean = false,
    val categoryId: Int? = null,
    val subcategoryId: Int? = null,
    val measurementUnit: MeasurementUnit? = null,
    val userLocationId: Long? = null,
    val contactNumbers: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val deliveryAvailable: Boolean = false,
    val pickupAvailable: Boolean = false,
    val pickupAddress: String = "",
    val deliveryZoneIds: List<Long> = emptyList(),
    val sku: String = "",
    val stockQuantity: String = "",
    val allowCart: Boolean = false,
    val isMarketplace: Boolean = false,
    /**
     * Каталог локациясы (Фаза 7) — сақталған мекенжай болмағанда карта/каталог
     * арқылы таңдалады: country_id/region_id/district_id (+ lat/lng).
     * Backend: user_location_id OR country/region/district (spec POST /announcements).
     */
    val location: SelectedLocation? = null,
    /** Тек өңдеу режимінде: қолданылған суреттерді сақтау (жою үшін). */
    val images: List<String> = emptyList(),
    /** YouTube бейне сілтемесі (Flutter/iOS: video_url) — бос болуы мүмкін. */
    val videoLink: String = "",
) {
    val priceValue: Double? get() = price.toDoubleOrNull()

    /**
     * CreateAdNotifier.validate() — бірінші жaramсыз өрістің кілтін қайтарады
     * (null = бәрі дұрыс). UI кілт арқылы тиісті өрісті қызыл етіп көрсетеді.
     */
    fun validate(): String? = when {
        title.isBlank() -> FIELD_TITLE
        description.isBlank() -> FIELD_DESCRIPTION
        priceValue == null || priceValue!! <= 0.0 -> FIELD_PRICE
        categoryId == null -> FIELD_CATEGORY
        subcategoryId == null -> FIELD_SUBCATEGORY
        contactNumbers.none { it.isNotBlank() } -> FIELD_PHONES
        userLocationId == null && location?.countryId == null -> FIELD_LOCATION
        pickupAvailable && pickupAddress.isBlank() -> FIELD_PICKUP_ADDRESS
        videoLink.isNotBlank() && !isYouTubeLink(videoLink) -> FIELD_VIDEO
        else -> null
    }

    fun isValid(): Boolean = validate() == null

    companion object {
        const val DEFAULT_CURRENCY = "₸"
        const val FIELD_TITLE = "title"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_PRICE = "price"
        const val FIELD_CATEGORY = "category"
        const val FIELD_SUBCATEGORY = "subcategory"
        const val FIELD_PHONES = "phones"
        const val FIELD_LOCATION = "location"
        const val FIELD_PICKUP_ADDRESS = "pickup_address"
        const val FIELD_VIDEO = "video"

        private val YOUTUBE_REGEX = Regex(
            "^(https?://)?(www\\.|m\\.)?(youtube\\.com/(watch\\?v=|shorts/|embed/)|youtu\\.be/)[\\w-]{6,}.*$",
            RegexOption.IGNORE_CASE,
        )

        /** Flutter `isYouTubeVideoLink` баламасы: watch / shorts / embed / youtu.be. */
        fun isYouTubeLink(link: String): Boolean = YOUTUBE_REGEX.matches(link.trim())
    }
}

/**
 * Өлшем бірліктері — backend MeasurementUnitEnum: piece, kilogram, ton, liter,
 * hectare, square_meter, cubic_meter, bag, centner, head, pair, meter, box.
 * UI атауы — strings.xml (measurement_unit_*).
 */
@Serializable
enum class MeasurementUnit(val queryKey: String) {
    PIECE("piece"),
    KILOGRAM("kilogram"),
    TON("ton"),
    LITER("liter"),
    HECTARE("hectare"),
    SQUARE_METER("square_meter"),
    CUBIC_METER("cubic_meter"),
    BAG("bag"),
    CENTNER("centner"),
    HEAD("head"),
    PAIR("pair"),
    METER("meter"),
    BOX("box");

    companion object {
        /** Backend жіберген строкадан enum — танымал емес болса null. */
        fun fromKey(key: String?): MeasurementUnit? =
            key?.trim()?.lowercase()?.let { k -> entries.firstOrNull { it.queryKey == k } }
    }
}

/** Multipart мәтіндік өріс — text/plain RequestBody. */
private fun text(value: String): RequestBody = value.toRequestBody("text/plain".toMediaType())

/** Сұраныс (MakeOffer) draft-ы — POST /demands. */
data class DemandDraft(
    val title: String = "",
    val description: String = "",
    val maxPrice: String = "",
    val categoryId: Int? = null,
    val subcategoryId: Int? = null,
    val measurementUnit: MeasurementUnit? = null,
    val quantity: String = "",
    val userLocationId: Long? = null,
    /** Кілт сөздер (iOS «Кілт сөздерді енгізіңіз») — бос болса жіберілмейді. */
    val keywords: List<String> = emptyList(),
) {
    val maxPriceValue: Double? get() = maxPrice.toDoubleOrNull()

    fun validate(): String? = when {
        title.isBlank() -> AdDraft.FIELD_TITLE
        description.isBlank() -> AdDraft.FIELD_DESCRIPTION
        categoryId == null -> AdDraft.FIELD_CATEGORY
        subcategoryId == null -> AdDraft.FIELD_SUBCATEGORY
        userLocationId == null -> AdDraft.FIELD_LOCATION
        else -> null
    }

    fun toJsonObject(): JsonObject = buildJsonObject {
        put("title", title.trim())
        put("description", description.trim())
        maxPriceValue?.let { put("max_price", it) }
        categoryId?.let { put("category_id", it) }
        subcategoryId?.let { put("subcategory_id", it) }
        measurementUnit?.let { put("measurement_unit", it.queryKey) }
        quantity.toDoubleOrNull()?.let { put("quantity", it) }
        userLocationId?.let { put("user_location_id", it) }
        if (keywords.isNotEmpty()) {
            put("keywords", kotlinx.serialization.json.JsonArray(keywords.map { kotlinx.serialization.json.JsonPrimitive(it) }))
        }
    }

    fun toPartMap(): Map<String, RequestBody> = buildMap {
        put("title", text(title.trim()))
        put("description", text(description.trim()))
        maxPriceValue?.let { put("max_price", text(it.toString())) }
        categoryId?.let { put("category_id", text(it.toString())) }
        subcategoryId?.let { put("subcategory_id", text(it.toString())) }
        measurementUnit?.let { put("measurement_unit", text(it.queryKey)) }
        quantity.toDoubleOrNull()?.let { put("quantity", text(it.toString())) }
        userLocationId?.let { put("user_location_id", text(it.toString())) }
    }
}

/** AI ұсынған мазмұн — POST /ai/ad-content жауабы (иілімді парсинг). */
data class AdContent(
    val description: String?,
    val categoryId: Int?,
    val subcategoryId: Int?,
    val title: String?,
)

/** Жеткізу аймағы — business/delivery-zones (create формасындағы тізім). */
data class DeliveryZone(
    val id: Long,
    val name: String?,
    val regionName: String?,
    val deliveryCost: Double?,
)

/** Bulk-upload нәтижесі — created/errors сандары иілімді алынады. */
data class BulkUploadResult(
    val createdCount: Int?,
    val errorCount: Int?,
    val message: String?,
)

/** Multipart өрістерін жасаушы — CreateAdNotifier.toMap() баламасы. */
object AdRequests {

    /**
     * Мәтіндік өрістер (PartMap) + файл/массив Part тізімі (images, video,
     * contact_numbers, keywords, delivery_zone_ids — қайталанатын кілттер).
     * [skipTariffDialog] — inline 403 (тариф шегі) үшін extra жалауша.
     */
    fun buildMultipart(
        draft: AdDraft,
        images: List<MultipartBody.Part>,
        video: MultipartBody.Part?,
        skipTariffDialog: Boolean = false,
    ): Pair<Map<String, RequestBody>, List<MultipartBody.Part>> {
        val fields = buildMap<String, RequestBody> {
            put("title", text(draft.title.trim()))
            put("description", text(draft.description.trim()))
            draft.priceValue?.let { put("price", text(it.toString())) }
            put("currency", text(draft.currency))
            put("negotiable", text(draft.negotiable.toString()))
            put("price_includes_vat", text(draft.priceIncludesVat.toString()))
            draft.categoryId?.let { put("category_id", text(it.toString())) }
            draft.subcategoryId?.let { put("subcategory_id", text(it.toString())) }
            draft.measurementUnit?.let { put("measurement_unit", text(it.queryKey)) }
            draft.userLocationId?.let { put("user_location_id", text(it.toString())) }
            // Каталог локациясы — сақталған мекенжай болмағанда (Фаза 7).
            if (draft.userLocationId == null) {
                draft.location?.let { loc ->
                    loc.countryId?.let { put("country_id", text(it.toString())) }
                    loc.regionId?.let { put("region_id", text(it.toString())) }
                    loc.districtId?.let { put("district_id", text(it.toString())) }
                    loc.latitude?.let { put("latitude", text(it.toString())) }
                    loc.longitude?.let { put("longitude", text(it.toString())) }
                }
            }
            put("delivery_available", text(draft.deliveryAvailable.toString()))
            put("pickup_available", text(draft.pickupAvailable.toString()))
            if (draft.pickupAvailable) put("pickup_address", text(draft.pickupAddress.trim()))
            put("allow_cart", text(draft.allowCart.toString()))
            put("is_marketplace", text(draft.isMarketplace.toString()))
            if (draft.sku.isNotBlank()) put("sku", text(draft.sku.trim()))
            draft.stockQuantity.toIntOrNull()?.let { put("stock_quantity", text(it.toString())) }
            // Қолданылған (өңдеу режиміндегі) суреттерді backend URL арқылы сақтайды.
            if (draft.images.isNotEmpty()) put("existing_images", text(draft.images.joinToString(",")))
            if (draft.videoLink.isNotBlank()) put("video_url", text(draft.videoLink.trim()))
            if (skipTariffDialog) put("skipTariffDialog", text("true"))
        }
        val parts = buildList {
            addAll(images)
            video?.let { add(it) }
            draft.contactNumbers.filter { it.isNotBlank() }.forEach { phone ->
                add(MultipartBody.Part.createFormData("contact_numbers", null, text(phone.trim())))
            }
            draft.keywords.filter { it.isNotBlank() }.forEach { keyword ->
                add(MultipartBody.Part.createFormData("keywords", null, text(keyword.trim())))
            }
            draft.deliveryZoneIds.forEach { zoneId ->
                add(MultipartBody.Part.createFormData("delivery_zone_ids", null, text(zoneId.toString())))
            }
        }
        return fields to parts
    }
}

/** Жазу ағынының парсерлері. */
object WriteParser {

    /** Жауап жарнаманың өзі болуы мүмкін (id) немесе {announcement_id}/{announcement:{id}}. */
    fun parseAnnouncementId(root: JsonObject?): Long? {
        if (root == null) return null
        JsonParser.long(root, "id")?.let { return it }
        JsonParser.long(root, "announcement_id")?.let { return it }
        JsonParser.obj(root, "announcement")?.let { ann ->
            JsonParser.long(ann, "id")?.let { return it }
        }
        JsonParser.obj(root, "data")?.let { data ->
            JsonParser.long(data, "id")?.let { return it }
            JsonParser.long(data, "announcement_id")?.let { return it }
        }
        return null
    }

    /** Сұраныс id-і — жарнама id-іне ұқсас пішімдер. */
    fun parseDemandId(root: JsonObject?): Long? {
        if (root == null) return null
        JsonParser.long(root, "id")?.let { return it }
        JsonParser.long(root, "demand_id")?.let { return it }
        JsonParser.obj(root, "demand")?.let { JsonParser.long(it, "id")?.let { id -> return id } }
        JsonParser.obj(root, "data")?.let { JsonParser.long(it, "id")?.let { id -> return id } }
        return null
    }

    /** Қайтару себебі — message/reason/reject_message/text өрістері. */
    fun parseRejectMessage(root: JsonObject?): String? {
        if (root == null) return null
        return JsonParser.string(root, "message")
            ?: JsonParser.string(root, "reason")
            ?: JsonParser.string(root, "reject_message")
            ?: JsonParser.string(root, "text")
            ?: JsonParser.string(root, "detail")
    }

    /** AI мазмұн — description/content/ad_text + category_id/subcategory_id. */
    fun parseAdContent(root: JsonObject?): AdContent? {
        val obj = root ?: return null
        val inner = JsonParser.obj(obj, "data") ?: obj
        val description = JsonParser.string(inner, "description")
            ?: JsonParser.string(inner, "content")
            ?: JsonParser.string(inner, "ad_text")
            ?: JsonParser.string(inner, "text")
        val categoryId = JsonParser.int(inner, "category_id")
            ?: JsonParser.int(inner, "categoryId")
        val subcategoryId = JsonParser.int(inner, "subcategory_id")
            ?: JsonParser.int(inner, "sub_category_id")
            ?: JsonParser.int(inner, "subcategoryId")
        val title = JsonParser.string(inner, "title")
            ?: JsonParser.string(inner, "ad_title")
        if (description == null && categoryId == null && subcategoryId == null && title == null) {
            return null
        }
        return AdContent(
            description = description,
            categoryId = categoryId,
            subcategoryId = subcategoryId,
            title = title,
        )
    }

    /** Bulk-upload жауабы — created_count/success_count + errors. */
    fun parseBulkResult(root: JsonObject?): BulkUploadResult {
        if (root == null) return BulkUploadResult(null, null, null)
        val inner = JsonParser.obj(root, "data") ?: root
        fun count(vararg keys: String): Int? =
            keys.firstNotNullOfOrNull { JsonParser.int(inner, it) }
        val message = JsonParser.string(inner, "message")
            ?: JsonParser.string(root, "detail")
        return BulkUploadResult(
            createdCount = count("created_count", "created", "success_count", "total_created"),
            errorCount = count("error_count", "errors_count", "failed_count", "errors"),
            message = message,
        )
    }

    /** Жеткізу аймақтары — {items:[{id, name, region_name, delivery_cost}]}. */
    fun parseDeliveryZones(root: JsonObject?): List<DeliveryZone> {
        if (root == null) return emptyList()
        val array = JsonParser.arrayOrSingle(root, "items")
            .plus(JsonParser.arrayOrSingle(root, "zones"))
            .plus(JsonParser.arrayOrSingle(root, "delivery_zones"))
        return array.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val id = JsonParser.long(obj, "id") ?: return@mapNotNull null
            DeliveryZone(
                id = id,
                name = JsonParser.string(obj, "name"),
                regionName = JsonParser.string(obj, "region_name") ?: JsonParser.string(obj, "region"),
                deliveryCost = JsonParser.double(obj, "delivery_cost"),
            )
        }
    }
}