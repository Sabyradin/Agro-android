package com.agroland.feature.push.data

import com.agroland.core.network.ApiResult
import com.agroland.core.network.NetworkModule.safeCall
import com.agroland.core.network.error.Failure
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Push репозиторийі: /device тіркеу/өшіру.
 * Шығу кезіндегі DELETE 401 қайтарса — СӘТТІ саналады (Flutter: токен жарамсыз
 * болғанда шыққан аккаунтты өшіру мұқтажы жоқ, 401 → success).
 */
@Singleton
class PushRepository @Inject constructor(
    private val api: PushApi,
) {

    suspend fun register(body: JsonObject): ApiResult<JsonElement> = safeCall {
        api.registerDevice(body)
    }

    suspend fun unregister(deviceId: String): ApiResult<Unit> =
        when (val result = safeCall { api.unregisterDevice(deviceId) }) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Error ->
                if (result.failure is Failure.Unauthorized) {
                    // 401 — токен өліп қалған: backend құрылғыны өшірген деуге болады.
                    ApiResult.success(Unit)
                } else {
                    result
                }
        }
}