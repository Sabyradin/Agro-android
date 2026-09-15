package com.agroland.feature.cart.data

import com.agroland.core.network.ApiResult
import com.agroland.core.network.NetworkModule.safeCall
import com.agroland.feature.marketplace.data.MarketplaceRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

/**
 * Себет/тапсырыс репозиторісі (Flutter CartRepository):
 * оптимистік жаңартулардың сервер жағы + чекауттың ERROR_500 қайталауы (1с,
 * 1 рет) + confirmDelivery статустрізбегі + buy-now (PATCH pickup_address).
 */
@Singleton
class CartRepository @Inject constructor(
    private val cartApi: CartApi,
    private val marketplaceRepository: MarketplaceRepository,
) {

    /**
     * Себет мазмұны + байыту: announcement жоқ немесе deliveryAvailable бірақ
     * zones бос болса — жарнама деталы қайта оқылады (CartItemsNotifier).
     */
    suspend fun getCart(): ApiResult<List<CartItem>> {
        val result = safeCall { CartParser.parseCart(cartApi.getCart()) }
        if (result !is ApiResult.Success) return result
        val enriched = result.value.map { item ->
            val needsEnrich = item.announcement == null ||
                (item.announcement.base.deliveryAvailable && item.announcement.deliveryZones.isEmpty())
            if (!needsEnrich) return@map item
            val detail = marketplaceRepository.getAnnouncement(item.announcementId).getOrNull()
                ?: return@map item
            item.copy(
                announcement = CartAnnouncement(
                    base = detail.base,
                    deliveryZones = detail.deliveryZones,
                ),
            )
        }
        return ApiResult.Success(enriched)
    }

    suspend fun addToCart(announcementId: Long, quantity: Double, measurementUnit: String?): ApiResult<Unit> =
        safeCall { cartApi.addToCart(CartRequests.addToCart(announcementId, quantity, measurementUnit)); Unit }

    suspend fun updateQuantity(itemId: Long, quantity: Double): ApiResult<Unit> =
        safeCall { cartApi.updateCartItem(itemId, CartRequests.updateQuantity(quantity)); Unit }

    suspend fun deleteCartItem(itemId: Long): ApiResult<Unit> =
        safeCall { cartApi.deleteCartItem(itemId); Unit }

    /** Smart Calculator: preview — аудан (0 = pickup/таңдалмады) өзгерсе қайта оқиды caller. */
    suspend fun getCartPreview(deliveryDistrictId: Int? = null): ApiResult<CartPreview> =
        safeCall {
            CartParser.parseCartPreview(cartApi.getCartPreview(deliveryDistrictId))
                ?: throw IllegalStateException("Пустой ответ")
        }

    /**
     * Атомарлы чекаут. ERROR_500 келсе 1с күтіп бір рет қайталанады (Flutter
     * мінез-құлығы — backend-тің уақытша ішкі қатесі).
     */
    suspend fun checkout(
        deliveryDistrictId: Int? = null,
        pickup: Boolean? = null,
        deliveryAddress: String? = null,
        pickupAddress: String? = null,
        onlySupplierId: Long? = null,
        lines: List<CheckoutLine> = emptyList(),
    ): ApiResult<CheckoutResult> {
        val body = CartRequests.checkout(
            deliveryDistrictId, pickup, deliveryAddress, pickupAddress, onlySupplierId, lines,
        )
        var result = safeCall { CartParser.parseCheckoutResult(cartApi.checkout(body)) }
        if (result is ApiResult.Error && isServerError500(result.failure)) {
            delay(ERROR_500_RETRY_DELAY_MS)
            result = safeCall { CartParser.parseCheckoutResult(cartApi.checkout(body)) }
        }
        return result
    }

    private fun isServerError500(failure: com.agroland.core.network.error.Failure): Boolean =
        failure is com.agroland.core.network.error.Failure.Http &&
            failure.error.message == "ERROR_500"

    suspend fun reorder(orderId: Long): ApiResult<Unit> =
        safeCall { cartApi.reorder(orderId); Unit }

    suspend fun getOrders(query: OrdersQuery): ApiResult<List<Order>> =
        safeCall { CartParser.parseOrders(cartApi.getOrders(query.role, query.status, query.paymentStatus)) }

    suspend fun getOrder(orderId: Long): ApiResult<Order> = safeCall {
        CartParser.parseOrder(cartApi.getOrder(orderId)) ?: throw IllegalStateException("Пустой ответ")
    }

    /** Статус өтпесі — жауап толық тапсырыс. */
    suspend fun updateOrderStatus(orderId: Long, status: String, note: String? = null): ApiResult<Order> =
        safeCall {
            CartParser.parseOrder(cartApi.updateOrderStatus(orderId, CartRequests.updateOrderStatus(status, note)))
                ?: throw IllegalStateException("Пустой ответ")
        }

    /** PATCH /orders/{id} — pickup_address/delivery_address. */
    suspend fun updateOrder(orderId: Long, pickupAddress: String? = null, deliveryAddress: String? = null): ApiResult<Order> =
        safeCall {
            CartParser.parseOrder(cartApi.updateOrder(orderId, CartRequests.updateOrder(pickupAddress, deliveryAddress)))
                ?: throw IllegalStateException("Пустой ответ")
        }

    /**
     * «Тауарды қабылдау» — аралық статустарды тізбектей өтеді:
     * logistics_enroute → loading → loaded → delivered (Flutter confirmDelivery).
     */
    /** «Тауарды қабылдау» — аралық статустарды тізбектей өтеді: logistics_enroute → loading → loaded → delivered. */
    suspend fun confirmDelivery(orderId: Long, currentStatus: String): ApiResult<Order> {
        val steps = confirmDeliverySteps(currentStatus)
        if (steps.isEmpty()) return getOrder(orderId)
        var last: ApiResult<Order>? = null
        for (step in steps) {
            last = updateOrderStatus(orderId, step)
            if (last is ApiResult.Error) return last
        }
        return last ?: getOrder(orderId)
    }

    /** Трекинг қатесі ешқашан бетті бұзбайды — caller null-ге ыдырайды. */
    suspend fun getOrderTracking(orderId: Long): ApiResult<OrderTracking> =
        safeCall { CartParser.parseOrderTracking(cartApi.getOrderTracking(orderId)) }

    /**
     * Себетсіз сатып алу (buy sheet): buy-now толық тапсырыс қайтарады;
     * pickup_address берілсе — 2-қадам PATCH (buy-now денесінде тек delivery).
     */
    suspend fun buyNow(
        announcementId: Long,
        quantity: Double,
        measurementUnit: String? = null,
        pickupAddress: String? = null,
        deliveryAddress: String? = null,
        deliveryZoneId: Long? = null,
    ): ApiResult<Order> {
        val body = CartRequests.buyNow(announcementId, quantity, measurementUnit, deliveryAddress, deliveryZoneId)
        val created = safeCall {
            CartParser.parseOrder(cartApi.buyNow(body)) ?: throw IllegalStateException("Пустой ответ")
        }
        if (created is ApiResult.Error) return created
        val order = (created as ApiResult.Success<Order>).value
        if (pickupAddress != null) {
            return updateOrder(order.id, pickupAddress = pickupAddress)
        }
        return created
    }

    private companion object {
        const val ERROR_500_RETRY_DELAY_MS = 1000L
    }
}