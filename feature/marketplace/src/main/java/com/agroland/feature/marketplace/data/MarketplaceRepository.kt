package com.agroland.feature.marketplace.data

import com.agroland.core.network.ApiResult
import com.agroland.core.network.NetworkModule
import com.agroland.core.network.error.Failure
import com.agroland.feature.location.data.LocationNameResolver
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MultipartBody

/**
 * Маркетплейс репозиторісі — сүзгіленген лента, деталь (FIFO кеш 50), таңдаулылар,
 * ұсыныстар, категориялар + жазу ағыны (create/edit/activate/delete, сұраныс, AI).
 * Барлық шақыру NetworkModule.safeCall арқылы.
 */
@Singleton
class MarketplaceRepository @Inject constructor(
    private val catalogApi: CatalogApi,
    private val writeApi: WriteApi,
    private val locationNames: LocationNameResolver,
) {

    /** FullAnnouncementNotifier-дің FIFO кеші (50) — сәтсіз fetch кезде fallback. */
    private val detailCache = object : LinkedHashMap<Long, FullAnnouncement>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, FullAnnouncement>): Boolean =
            size > DETAIL_CACHE_SIZE
    }

    suspend fun getAnnouncements(filter: AnnouncementFilter, page: Int, limit: Int = PAGE_SIZE): ApiResult<AnnouncementsPage> =
        safeCall { MarketplaceParser.parseAnnouncementPage(catalogApi.getAnnouncements(filter.toQueryMap(page, limit))) }
            .withPlaceNames()

    suspend fun getRecommended(page: Int, limit: Int = PAGE_SIZE): ApiResult<AnnouncementsPage> =
        safeCall { MarketplaceParser.parseAnnouncementPage(catalogApi.getRecommended(page, limit)) }
            .withPlaceNames()

    /** Деталь: сәтсіз болса кештегі соңғы нұсқа қайтарылады. */
    suspend fun getAnnouncement(id: Long): ApiResult<FullAnnouncement> {
        val result = safeCall {
            MarketplaceParser.parseFullAnnouncement(catalogApi.getAnnouncement(id))
                ?: throw IllegalStateException("Пустой ответ")
        }.withPlaceName()
        if (result is ApiResult.Success) {
            synchronized(detailCache) { detailCache[id] = result.value }
        }
        val cached = synchronized(detailCache) { detailCache[id] }
        return when {
            result is ApiResult.Success -> result
            cached != null -> ApiResult.Success(cached)
            else -> result
        }
    }

    suspend fun searchSuggestions(query: String, page: Int = 1, limit: Int = 10): ApiResult<List<Suggestion>> =
        safeCall { MarketplaceParser.parseSuggestions(catalogApi.searchSuggestions(query, page, limit)) }

    /**
     * Фаза 19: POST /search/log — fire-and-forget, қате ешқашан шақырушыға
     * жетпейді ( Flutter бұл endpoint-ті қолданбайды — MASTER_PLAN талабы).
     */
    suspend fun logSearchQuery(query: String, categoryId: Int? = null) {
        try {
            catalogApi.logSearchQuery(
                buildJsonObject {
                    put("query", query)
                    categoryId?.let { put("category_id", it) }
                },
            )
        } catch (_: Exception) {
        }
    }

    suspend fun getCategories(): ApiResult<List<Category>> =
        safeCall { MarketplaceParser.parseCategoryList(catalogApi.getCategories()) }

    suspend fun getGroupedCategories(): ApiResult<Map<String, List<Category>>> =
        safeCall { MarketplaceParser.parseGroupedCategories(catalogApi.getGroupedCategories()) }

    suspend fun getSubcategories(categoryId: Int): ApiResult<List<Category>> =
        safeCall { MarketplaceParser.parseCategoryList(catalogApi.getSubcategories(categoryId)) }

    suspend fun getFavorites(page: Int, limit: Int = PAGE_SIZE): ApiResult<AnnouncementsPage> =
        safeCall { MarketplaceParser.parseAnnouncementPage(catalogApi.getFavorites(page, limit)) }
            .withPlaceNames()

    suspend fun toggleFavorite(id: Long, add: Boolean): ApiResult<Unit> = safeCall {
        if (add) {
            catalogApi.addFavorite(id)
        } else {
            catalogApi.removeFavorite(id)
        }
        Unit
    }

    suspend fun getFavoriteStatus(id: Long): ApiResult<Boolean> =
        safeCall { MarketplaceParser.parseFavoriteStatus(catalogApi.getFavoriteStatus(id)) }

    // ---- Жазу ағыны (Фаза 6) ----

    /** Жарнама жасау — multipart (images + video + қайталанатын массивтер). */
    suspend fun createAnnouncement(
        draft: AdDraft,
        images: List<MultipartBody.Part>,
        video: MultipartBody.Part?,
        skipTariffDialog: Boolean = false,
    ): ApiResult<Long?> {
        val (fields, parts) = AdRequests.buildMultipart(draft, images, video, skipTariffDialog)
        return safeCall { WriteParser.parseAnnouncementId(writeApi.createAnnouncement(fields, parts)) }
    }

    /** Өңдеу — PATCH /announcement/{id}; backend статусты PENDING жасайды. */
    suspend fun updateAnnouncement(
        id: Long,
        draft: AdDraft,
        images: List<MultipartBody.Part>,
        video: MultipartBody.Part?,
    ): ApiResult<Long?> {
        val (fields, parts) = AdRequests.buildMultipart(draft, images, video)
        return safeCall { WriteParser.parseAnnouncementId(writeApi.updateAnnouncement(id, fields, parts)) }
    }

    suspend fun activateAnnouncement(id: Long): ApiResult<Unit> =
        safeCall { writeApi.activateAnnouncement(id); Unit }

    suspend fun deactivateAnnouncement(id: Long): ApiResult<Unit> =
        safeCall { writeApi.deactivateAnnouncement(id); Unit }

    suspend fun deleteAnnouncement(id: Long): ApiResult<Unit> =
        safeCall { writeApi.deleteAnnouncement(id); Unit }

    suspend fun getRejectMessage(id: Long): ApiResult<String?> =
        safeCall { WriteParser.parseRejectMessage(writeApi.getRejectMessage(id)) }

    /** Менің жарнамаларым — status: active|inactive|pending|rejected. */
    suspend fun getMyAnnouncements(status: String, page: Int, limit: Int = PAGE_SIZE): ApiResult<AnnouncementsPage> =
        safeCall { MarketplaceParser.parseAnnouncementPage(writeApi.getMyAnnouncements(status, page, limit)) }
            .withPlaceNames()

    /** AI мазмұн — {ad_title, lang_code}; DEV-те AI сервисі қазір 500 береді (ISSUES #12). */
    suspend fun generateAdContent(title: String, langCode: String): ApiResult<AdContent?> {
        val body = buildJsonObject {
            put("ad_title", title)
            put("lang_code", langCode)
        }
        return safeCall { WriteParser.parseAdContent(writeApi.generateAdContent(body)) }
    }

    /**
     * Сұраныс жасау (MakeOffer) — әдетте JSON; backend form күтсе (4xx) form
     * қайта әрекет. 4xx = сұраныс жасалмады, сондықтан қайталау дубликат болмайды.
     */
    suspend fun createDemand(draft: DemandDraft): ApiResult<Long?> {
        val primary = safeCall { WriteParser.parseDemandId(writeApi.createDemand(draft.toJsonObject())) }
        val http = (primary as? ApiResult.Error)?.failure as? Failure.Http ?: return primary
        if (http.error.httpStatus !in 400..422) return primary
        return safeCall { WriteParser.parseDemandId(writeApi.createDemandForm(draft.toPartMap())) }
    }

    /** Бизнес жеткізу аймақтары — {id, name/region_name, delivery_cost}. */
    suspend fun getDeliveryZones(): ApiResult<List<DeliveryZone>> =
        safeCall { WriteParser.parseDeliveryZones(writeApi.getDeliveryZones()) }

    /**
     * Smart Calculator (M12): сатушы бұл ауданға жеткізе ала ма.
     * {can_deliver, zone{...}, pickup_available, pickup_address}.
     */
    suspend fun deliveryCheck(announcementId: Long, districtId: Int): ApiResult<DeliveryCheckResult> =
        safeCall { MarketplaceParser.parseDeliveryCheck(catalogApi.deliveryCheck(announcementId, districtId)) }

    /** Bulk-upload нәтижесі. */
    suspend fun bulkUpload(file: MultipartBody.Part): ApiResult<BulkUploadResult> =
        safeCall { WriteParser.parseBulkResult(writeApi.bulkUpload(file)) }

    /** Excel үлгісі — бинарлы файл (ResponseBody); VM файлға жазады. */
    suspend fun downloadBulkTemplate(lang: String): ApiResult<okhttp3.ResponseBody> =
        safeCall {
            val response = writeApi.getBulkTemplate(lang)
            val body = response.body() ?: throw IllegalStateException("Пустой ответ")
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code()}")
            body
        }

    private suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> =
        NetworkModule.safeCall(block)

    /** Деталь бетіндегі орын атауы — лентадағы [withPlaceNames] баламасы. */
    private suspend fun ApiResult<FullAnnouncement>.withPlaceName(): ApiResult<FullAnnouncement> {
        val detail = (this as? ApiResult.Success)?.value ?: return this
        if (detail.base.placeLabel.isNotBlank()) return this
        val label = locationNames.label(detail.base.regionId, detail.base.districtId, short = false) ?: return this
        return ApiResult.Success(detail.copy(base = detail.base.copy(city = label)))
    }

    /**
     * Лентадағы жарнамаларға каталогтан алынған орын атауын қосады — бэк
     * `city`/`district` жібермей, тек `location` ID-лерін беретіндіктен.
     * Каталог қолжетімсіз болса тізім өзгеріссіз қалады.
     */
    private suspend fun ApiResult<AnnouncementsPage>.withPlaceNames(): ApiResult<AnnouncementsPage> {
        val page = (this as? ApiResult.Success)?.value ?: return this
        if (page.items.none { it.placeLabel.isBlank() && (it.regionId != null || it.districtId != null) }) return this
        val enriched = page.items.map { item ->
            if (item.placeLabel.isNotBlank()) {
                item
            } else {
                locationNames.label(item.regionId, item.districtId)
                    ?.let { item.copy(city = it) } ?: item
            }
        }
        return ApiResult.Success(page.copy(items = enriched))
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val DETAIL_CACHE_SIZE = 50
    }
}