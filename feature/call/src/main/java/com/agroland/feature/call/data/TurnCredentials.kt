package com.agroland.feature.call.data

import com.agroland.core.network.json.JsonParser
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.webrtc.PeerConnection

/**
 * Backend `GET /turn/credentials` жауабы (Flutter TurnCredentials.dart, 1:1):
 * `{ttl, turn:{urls, username, credential}}` — coturn use-auth-secret REST.
 * username = "<expiryEpoch>:<userId>", credential = base64 HMAC-SHA1.
 * urls бос/жоқ болса — null (STUN-only fallback-ке құлайды).
 */
data class TurnCredentials(
    val ttl: Int,
    val urls: List<String>,
    val username: String,
    val credential: String,
) {
    /** org.webrtc IceServer (бос емес credential-дерімен). */
    fun toIceServer(): PeerConnection.IceServer = PeerConnection.IceServer.builder(urls)
        .apply {
            if (username.isNotEmpty()) setUsername(username)
            if (credential.isNotEmpty()) setPassword(credential)
        }
        .createIceServer()

    companion object {
        fun fromBackendJson(root: JsonElement?): TurnCredentials? {
            val obj = (root as? JsonObject) ?: return null
            val turn = (obj["turn"] as? JsonObject) ?: return null
            val urls = JsonParser.arr(turn, "urls")
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotEmpty) }
                ?: emptyList()
            if (urls.isEmpty()) return null
            return TurnCredentials(
                ttl = JsonParser.int(obj, "ttl") ?: 3600,
                urls = urls,
                username = JsonParser.string(turn, "username") ?: "",
                credential = JsonParser.string(turn, "credential") ?: "",
            )
        }
    }
}