package com.agroland.feature.marketplace.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit

/**
 * Маркетплейс API (spec: GET /announcements, /announcements/recommended,
 * /announcement/{id}, /search-suggestions, /categories[+/grouped], /subcategories,
 * /favorites CRUD). Жауаптар JsonObject — кешірімді парсинг MarketplaceModels-те.
 */
interface CatalogApi {

    /**
     * Пагинацияланған лента: q/category_id/subcategory_id/is_vip/type_ad(country_id...)/
     * min_price/max_price/negotiable/sort_by_date/sort_by_price/order_random/page/limit.
     * Жауап: items+total+page+pages.
     */
    @retrofit2.http.GET("announcements")
    suspend fun getAnnouncements(@retrofit2.http.QueryMap filters: Map<String, String>): JsonObject

    /** Ұсынылатын лента: Flutter сияқты type_ad=rec, order_random=true, country_id=4 жібереді. */
    @retrofit2.http.GET("announcements/recommended")
    suspend fun getRecommended(
        @retrofit2.http.Query("page") page: Int,
        @retrofit2.http.Query("limit") limit: Int,
        @retrofit2.http.Query("type_ad") typeAd: String = "rec",
        @retrofit2.http.Query("order_random") orderRandom: Boolean = true,
        @retrofit2.http.Query("country_id") countryId: Int = 4,
    ): JsonObject

    /** Толық деталь: images, seller, contacts, characteristics, additional, similar; иесі болмаса views_count++. */
    @retrofit2.http.GET("announcement/{id}")
    suspend fun getAnnouncement(@retrofit2.http.Path("id") id: Long): JsonObject

    /** Іздеу ұсыныстары: {title, category, categoryId, subCategory, subCategoryId, location}. */
    @retrofit2.http.GET("search-suggestions")
    suspend fun searchSuggestions(
        @retrofit2.http.Query("q") query: String,
        @retrofit2.http.Query("page") page: Int,
        @retrofit2.http.Query("limit") limit: Int,
    ): JsonObject

    /** Жазық категория ағашы + subcategories + announcement_count. */
    @retrofit2.http.GET("categories")
    suspend fun getCategories(): JsonObject

    /** 6 bucket (crops/livestock/products/technology/services/other). */
    @retrofit2.http.GET("categories/grouped")
    suspend fun getGroupedCategories(): JsonObject

    @retrofit2.http.GET("subcategories")
    suspend fun getSubcategories(@retrofit2.http.Query("category_id") categoryId: Int): JsonObject

    /** Таңдаулылар — пагинацияланған, dedup by id, барлық item is_favorite=true. */
    @retrofit2.http.GET("favorites")
    suspend fun getFavorites(
        @retrofit2.http.Query("page") page: Int,
        @retrofit2.http.Query("limit") limit: Int,
    ): JsonObject

    @retrofit2.http.POST("favorites/{id}")
    suspend fun addFavorite(@retrofit2.http.Path("id") id: Long): JsonObject

    @retrofit2.http.DELETE("favorites/{id}")
    suspend fun removeFavorite(@retrofit2.http.Path("id") id: Long): Response<ResponseBody>

    @retrofit2.http.GET("favorites/{id}/status")
    suspend fun getFavoriteStatus(@retrofit2.http.Path("id") id: Long): JsonObject
}

@Module
@InstallIn(SingletonComponent::class)
object CatalogApiModule {

    @Provides
    @Singleton
    fun provideCatalogApi(retrofit: Retrofit): CatalogApi = retrofit.create(CatalogApi::class.java)
}