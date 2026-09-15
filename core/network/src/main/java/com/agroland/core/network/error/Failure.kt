package com.agroland.core.network.error

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Backend қатесінің қаңқасы: {error_code, message} (+ кейде fields/detail).
 * UI-да ЕШҚАШАН error_code көрсетілмейді — тек адам тіліндегі message.
 */
data class ApiError(
    val code: String?,
    val message: String?,
    val httpStatus: Int?,
    val fields: Map<String, String> = emptyMap(),
)

/** Failure иерархиясы — UI-ның адам тіліндегі хабарламалар жасауы үшін. */
sealed class Failure {
    /** HTTP қатесі — backend хабарламасымен (message толық адам тілінде). */
    data class Http(val error: ApiError) : Failure()

    /** Желі жоқ / timeout / DNS. */
    data class Network(val cause: Throwable) : Failure()

    /** JSON/кодтау қатесі — пайдаланушыға жалпы хабарлама. */
    data class Parsing(val cause: Throwable) : Failure()

    /** Белгісіз. */
    data class Unknown(val cause: Throwable) : Failure()

    /** Тариф шегі (403 TARIFF_LIMIT_*) — UI-де тариф жаңарту диалогын көрсетеді. */
    data class TariffLimit(val error: ApiError) : Failure()

    /** Token жарамсыз — logout талап етіледі. */
    data object Unauthorized : Failure()
}

object ApiErrorParser {

    /** Response body-дан ApiError шығарады; бітпеген формада болса жалпылау. */
    fun parse(httpCode: Int, body: String?): ApiError {
        val root: JsonObject? = try {
            body?.let {
                kotlinx.serialization.json.Json.parseToJsonElement(it) as? JsonObject
            }
        } catch (e: Exception) {
            null
        }
        val code = root?.get("error_code")?.jsonPrimitive?.content
        val message = root?.get("message")?.jsonPrimitive?.content
            ?: root?.get("detail")?.jsonPrimitive?.content
        val fields = (root?.get("fields") as? JsonObject)?.let { f ->
            f.entries.mapNotNull { (k, v) ->
                (v as? kotlinx.serialization.json.JsonPrimitive)?.content?.let { k to it }
            }.toMap()
        }
        return ApiError(
            code = code,
            message = message,
            httpStatus = httpCode,
            fields = fields ?: emptyMap(),
        )
    }
}