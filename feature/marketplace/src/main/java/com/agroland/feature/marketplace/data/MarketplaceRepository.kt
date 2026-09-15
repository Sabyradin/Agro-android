package com.agroland.feature.marketplace.data

import com.agroland.core.network.ApiResult
import com.agroland.core.network.NetworkModule
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Маркетплейс репозиторісі — сүзгіленген лента, деталь (FIFO кеш 50), таңдаулылар,
 * ұсыныстар, категориялар. Барлық шақыру NetworkModule.safeCall арқылы.
 */
@Singleton
class MarketplaceRepository @Inject constructor(
    private val catalogApi: CatalogApi,
) {

    /** FullAnnouncementNotifier-дің FIFO кеші (50) — сәтсіз fetch кезде fallback. */
    private val detailCache = object : LinkedHashMap<Long, FullAnnouncement>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, FullAnnouncement>): Boolean =
            size > DETAIL_CACHE_SIZE
    }

    suspend fun getAnnouncements(filter: AnnouncementFilter, page: Int, limit: Int = PAGE_SIZE): ApiResult<AnnouncementsPage> =
        safeCall { MarketplaceParser.parseAnnouncementPage(catalogApi.getAnnouncements(filter.toQueryMap(page, limit))) }

    suspend fun getRecommended(page: Int, limit: Int = PAGE_SIZE): ApiResult<AnnouncementsPage> =
        safeCall { MarketplaceParser.parseAnnouncementPage(catalogApi.getRecommended(page, limit)) }

    /** Деталь: сәтсіз болса кештегі соңғы нұсқа қайтарылады. */
    suspend fun getAnnouncement(id: Long): ApiResult<FullAnnouncement> {
        val result = safeCall {
            MarketplaceParser.parseFullAnnouncement(catalogApi.getAnnouncement(id))
                ?: throw IllegalStateException("Пустой ответ")
        }
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

    suspend fun getCategories(): ApiResult<List<Category>> =
        safeCall { MarketplaceParser.parseCategoryList(catalogApi.getCategories()) }

    suspend fun getGroupedCategories(): ApiResult<Map<String, List<Category>>> =
        safeCall { MarketplaceParser.parseGroupedCategories(catalogApi.getGroupedCategories()) }

    suspend fun getSubcategories(categoryId: Int): ApiResult<List<Category>> =
        safeCall { MarketplaceParser.parseCategoryList(catalogApi.getSubcategories(categoryId)) }

    suspend fun getFavorites(page: Int, limit: Int = PAGE_SIZE): ApiResult<AnnouncementsPage> =
        safeCall { MarketplaceParser.parseAnnouncementPage(catalogApi.getFavorites(page, limit)) }

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

    private suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> =
        NetworkModule.safeCall(block)

    private companion object {
        const val PAGE_SIZE = 20
        const val DETAIL_CACHE_SIZE = 50
    }
}