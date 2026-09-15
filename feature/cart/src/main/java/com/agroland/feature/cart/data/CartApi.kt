package com.agroland.feature.cart.data

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
 * Себет + тапсырыс API (Flutter CartRepository):
 * /cart CRUD, /cart/preview (Smart Calculator), /cart/checkout, /cart/reorder,
 * /orders/ (GET/POST status/PATCH), /orders/buy-now, /orders/{id}/tracking.
 * Жауаптар JsonObject — кешірімді парсинг CartParser-де.
 */
interface CartApi {

    /** Себет мазмұны — {items:[...]} немесе тікелей массив (екеуі де қолдауда). */
    @retrofit2.http.GET("cart")
    suspend fun getCart(): JsonObject

    @retrofit2.http.POST("cart/items")
    suspend fun addToCart(@retrofit2.http.Body body: JsonObject): JsonObject

    /** M6: quantity және/немесе жеткізу параметрлері (zone/district/address/pickup). */
    @retrofit2.http.PATCH("cart/items/{id}")
    suspend fun updateCartItem(
        @retrofit2.http.Path("id") id: Long,
        @retrofit2.http.Body body: JsonObject,
    ): JsonObject

    @retrofit2.http.DELETE("cart/items/{id}")
    suspend fun deleteCartItem(@retrofit2.http.Path("id") id: Long): Response<ResponseBody>

    /** Smart Calculator (M7): per-supplier breakdown — delivery_district_id/pickup опционал. */
    @retrofit2.http.GET("cart/preview")
    suspend fun getCartPreview(
        @retrofit2.http.Query("delivery_district_id") deliveryDistrictId: Int? = null,
        @retrofit2.http.Query("pickup") pickup: Boolean? = null,
    ): JsonObject

    /**
     * Атомарлы чекаут (M8): {delivery_district_id?, pickup?, delivery_address?,
     * pickup_address?, only_supplier_id?, items:[{cart_item_id, delivery_zone_id?}]}
     * → {order_ids}. Әр сатушы = бір тапсырыс.
     */
    @retrofit2.http.POST("cart/checkout")
    suspend fun checkout(@retrofit2.http.Body body: JsonObject): JsonObject

    /** Тапсырысты қайта себетке салу. */
    @retrofit2.http.POST("cart/reorder/{id}")
    suspend fun reorder(@retrofit2.http.Path("id") id: Long): JsonObject

    /** Тапсырыстар тізімі — role/status/payment_status (status: үтірмен бірнеше). */
    @retrofit2.http.GET("orders/")
    suspend fun getOrders(
        @retrofit2.http.Query("role") role: String? = null,
        @retrofit2.http.Query("status") status: String? = null,
        @retrofit2.http.Query("payment_status") paymentStatus: String? = null,
    ): JsonObject

    @retrofit2.http.GET("orders/{id}")
    suspend fun getOrder(@retrofit2.http.Path("id") id: Long): JsonObject

    /** Статус өтпесі — {status, note?}; жауап — толық тапсырыс. */
    @retrofit2.http.POST("orders/{id}/status")
    suspend fun updateOrderStatus(
        @retrofit2.http.Path("id") id: Long,
        @retrofit2.http.Body body: JsonObject,
    ): JsonObject

    /** PATCH /orders/{id} — pickup_address/delivery_address (buy-now 2-қадамы). */
    @retrofit2.http.PATCH("orders/{id}")
    suspend fun updateOrder(
        @retrofit2.http.Path("id") id: Long,
        @retrofit2.http.Body body: JsonObject,
    ): JsonObject

    /** Жүргізуші/көлік деректері — waybills[]; қате болса үнсіз қалдырылады. */
    @retrofit2.http.GET("orders/{id}/tracking")
    suspend fun getOrderTracking(@retrofit2.http.Path("id") id: Long): JsonObject

    /**
     * Себетсіз сатып алу (buy sheet): {announcement_id, quantity, measurement_unit?,
     * delivery_address?, delivery_zone_id?} → толық тапсырыс (backend a220e47).
     */
    @retrofit2.http.POST("orders/buy-now")
    suspend fun buyNow(@retrofit2.http.Body body: JsonObject): JsonObject
}

@Module
@InstallIn(SingletonComponent::class)
object CartApiModule {

    @Provides
    @Singleton
    fun provideCartApi(retrofit: Retrofit): CartApi = retrofit.create(CartApi::class.java)
}