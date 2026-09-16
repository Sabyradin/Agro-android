package com.agroland.feature.china.data

import com.agroland.core.network.ApiResult
import com.agroland.core.network.NetworkModule.safeCall
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Қытай импорты API — MercuryX проксиі (Flutter ChinaCatalog/Cart/OrdersRepository, 1:1):
 *  - GET  /china/categories[?parent_id=]
 *  - GET  /china/products?category_id=&page=&per_page=&sort=
 *  - GET  /china/products/{product_id}  (productId — BigInteger int64!)
 *  - GET  /china/cart, POST /china/cart/items (upsert product+sku),
 *    PATCH/DELETE /china/cart/items/{id}
 *  - POST /china/orders (consent_accepted + phone 11d 77… + bin 12d → MercuryX forward),
 *    GET  /china/orders?limit=
 * Барлық жауап JsonElement — кешірімді парсинг ChinaParser-де.
 */
interface ChinaApi {

    @GET("china/categories")
    suspend fun getCategories(@Query("parent_id") parentId: Long?): JsonElement

    @GET("china/products")
    suspend fun getProducts(
        @Query("category_id") categoryId: Long,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): JsonElement

    @GET("china/products/{product_id}")
    suspend fun getProductDetail(@Path("product_id") productId: Long): JsonElement

    @GET("china/cart")
    suspend fun getCart(): JsonElement

    @POST("china/cart/items")
    suspend fun addCartItem(@Body body: JsonObject): JsonElement

    @PATCH("china/cart/items/{id}")
    suspend fun updateCartItem(
        @Path("id") id: Long,
        @Body body: JsonObject,
    ): JsonElement

    @DELETE("china/cart/items/{id}")
    suspend fun deleteCartItem(@Path("id") id: Long): JsonElement

    @POST("china/orders")
    suspend fun createOrder(@Body body: JsonObject): JsonElement

    @GET("china/orders")
    suspend fun getOrders(@Query("limit") limit: Int): JsonElement
}

@Module
@InstallIn(SingletonComponent::class)
object ChinaApiModule {

    @Provides
    @Singleton
    fun provideChinaApi(retrofit: Retrofit): ChinaApi =
        retrofit.create(ChinaApi::class.java)
}

/** Корзинаға қосу денесі — MercuryX snapshot өрістері толық жіберіледі (спек 2.2). */
data class ChinaAddCartItemRequest(
    val productId: Long,
    val skuId: Long?,
    val quantity: Int,
    val minQty: Int,
    val priceAtAdd: Double,
    val titleSnapshot: String,
    val imageUrl: String?,
)

/**
 * Қытай репозиторісі — барлық шақыру safeCall арқылы, парсинг ChinaParser.
 */
@Singleton
class ChinaRepository @Inject constructor(
    private val api: ChinaApi,
) {

    suspend fun getCategories(parentId: Long? = null): ApiResult<List<ChinaCategory>> =
        safeCall { ChinaParser.parseCategories(api.getCategories(parentId)) }

    suspend fun getProducts(
        categoryId: Long,
        page: Int,
        perPage: Int = 20,
    ): ApiResult<ChinaProductsPage> =
        safeCall { ChinaParser.parseProducts(api.getProducts(categoryId, page, perPage)) }

    suspend fun getProductDetail(productId: Long): ApiResult<ChinaProductDetail> =
        safeCall { ChinaParser.parseProductDetail(api.getProductDetail(productId)) }

    suspend fun getCart(): ApiResult<List<ChinaCartItem>> =
        safeCall { ChinaParser.parseCart(api.getCart()) }

    suspend fun addCartItem(item: ChinaAddCartItemRequest): ApiResult<ChinaCartItem> =
        safeCall { ChinaParser.parseCartItem(api.addCartItem(ChinaParser.buildAddCartBody(item))) }

    suspend fun updateCartItem(id: Long, quantity: Int): ApiResult<ChinaCartItem> =
        safeCall { ChinaParser.parseCartItem(api.updateCartItem(id, buildJsonObject { put("quantity", quantity) })) }

    suspend fun deleteCartItem(id: Long): ApiResult<Unit> =
        safeCall { api.deleteCartItem(id); Unit }

    suspend fun createOrder(phone: String, bin: String, cartItemIds: List<Long>? = null): ApiResult<ChinaOrder> =
        safeCall { ChinaParser.parseCreatedOrder(api.createOrder(ChinaParser.buildOrderBody(phone, bin, cartItemIds))) }

    suspend fun getOrders(limit: Int = 20): ApiResult<List<ChinaOrder>> =
        safeCall { ChinaParser.parseOrders(api.getOrders(limit)) }
}