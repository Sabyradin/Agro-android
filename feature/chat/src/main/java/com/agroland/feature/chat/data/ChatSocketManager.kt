package com.agroland.feature.chat.data

import com.agroland.core.common.settings.SettingsDataStore
import com.agroland.core.network.NetworkConfig
import com.agroland.core.network.auth.TokenStore
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.emitter.Emitter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.json.JSONArray
import org.json.JSONObject

/**
 * Socket.IO клиенті (Flutter socketProvider / OptionBuilder паритеті):
 *  - socketUrl = baseApiUrl минус /api/v1;
 *  - transports ['websocket'] ТОЛЫҚ (polling — қатесі, 2026-08-29 түзету);
 *  - шексіз қайта қосылым, delay 1000 / max 5000, timeout 10000;
 *  - auth {Authorization: Bearer, language: locale ?? 'kk'}.
 *
 * org.json exclude қылған (Android платформалық кітапханасын қолданады),
 * сондықтан event payload-тары org.json JSONObject арқылы келеді — олар
 * kotlinx JsonObject-ке айналдырылады (toJsonElement).
 */
@Singleton
class ChatSocketManager @Inject constructor(
    private val config: NetworkConfig,
    private val tokenStore: TokenStore,
    private val settings: SettingsDataStore,
) {

    enum class State { DISCONNECTED, CONNECTING, CONNECTED }

    /** Тіркелген тыңдаушыны алып тастау айқасы. */
    fun interface Subscription {
        fun cancel()
    }

    private val _state = MutableStateFlow(State.DISCONNECTED)
    val state: StateFlow<State> = _state.asStateFlow()

    var socket: Socket? = null
        private set

    val connected: Boolean get() = socket?.connected() == true

    /** Socket қосылған ба (субъективті күй — CONNECTED event болды ма). */
    val isConnected: Boolean get() = _state.value == State.CONNECTED

    /**
     * Жаңа socket жасап қосады (Flutter socketProvider әр auth кезінде
     * жаңартылады — ескі токенді socket ұстап тұрмауы үшін).
     */
    fun connect() {
        disconnect()
        val token = tokenStore.accessToken.value ?: return
        val url = config.baseUrl.substringBefore("/api/v1")

        val opts = IO.Options().apply {
            transports = arrayOf("websocket")
            reconnection = true
            reconnectionAttempts = Int.MAX_VALUE
            reconnectionDelay = 1000L
            reconnectionDelayMax = 5000L
            timeout = 10000L
            auth = mapOf(
                "Authorization" to "Bearer $token",
                "language" to language,
            )
        }

        val newSocket = IO.socket(url, opts)
        newSocket.on(Socket.EVENT_CONNECT) { _state.value = State.CONNECTED }
        newSocket.on(Socket.EVENT_DISCONNECT) { _state.value = State.DISCONNECTED }
        newSocket.on(Socket.EVENT_CONNECT_ERROR) { _state.value = State.DISCONNECTED }
        newSocket.io().on(Manager.EVENT_RECONNECT) { _state.value = State.CONNECTING }
        newSocket.io().on(Manager.EVENT_RECONNECT_ATTEMPT) { _state.value = State.CONNECTING }
        socket = newSocket
        _state.value = State.CONNECTING
        newSocket.connect()
    }

    /**
     * Тыңдаушы тіркеу — payload kotlinx JsonObject ретінде келеді.
     * socket болмаса — null (caller қайта тіркеуді кейін жасайды).
     */
    fun on(event: String, handler: (JsonElement?) -> Unit): Subscription? {
        val s = socket ?: return null
        var listener: Emitter.Listener? = null
        listener = Emitter.Listener { args -> handler(args.firstOrNull().let(::toJsonElement)) }
        s.on(event, listener)
        return Subscription { s.off(event, listener) }
    }

    fun emit(event: String, payload: JsonObject) {
        socket?.emit(event, JSONObject(payload.toString()))
    }

    fun emitAck(event: String, payload: JsonObject, ack: (JsonElement?) -> Unit) {
        socket?.emit(
            event,
            JSONObject(payload.toString()),
            io.socket.client.Ack { args -> ack(args.firstOrNull().let(::toJsonElement)) },
        )
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        _state.value = State.DISCONNECTED
    }

    /**
     * Socket қосылғанша күтеді (Flutter _emitChatMessageSafe үлгісі, 10с) —
     * уақыты өтсе false (queue / error branch).
     */
    suspend fun awaitConnected(timeoutMs: Long = 10_000): Boolean {
        if (connected) return true
        val s = socket ?: return false
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                var listener: Emitter.Listener? = null
                listener = Emitter.Listener {
                    if (!cont.isCompleted) {
                        listener?.let { s.off(Socket.EVENT_CONNECT, it) }
                        cont.resume(true)
                    }
                }
                s.on(Socket.EVENT_CONNECT, listener)
                cont.invokeOnCancellation { listener?.let { s.off(Socket.EVENT_CONNECT, it) } }
            }
        } ?: false
    }

    /** Socket жасалған сәттегі тіл (Flutter сияқты құрылғанда бекітіледі). */
    private var language: String = "kk"

    suspend fun refreshLanguageFromSettings() {
        language = settings.locale.first()
    }

    private fun toJsonElement(arg: Any?): JsonElement = when (arg) {
        null, JSONObject.NULL -> JsonNull
        is JSONObject -> arg.toElement()
        is JSONArray -> JsonArray(
            (0 until arg.length()).map { i -> toJsonElement(arg.opt(i)) },
        )
        is String -> JsonPrimitive(arg)
        is Boolean -> JsonPrimitive(arg)
        is Int -> JsonPrimitive(arg)
        is Long -> JsonPrimitive(arg)
        is Double -> JsonPrimitive(arg)
        is Float -> JsonPrimitive(arg)
        else -> JsonPrimitive(arg.toString())
    }

    private fun JSONObject.toElement(): JsonObject {
        val map = HashMap<String, JsonElement>()
        for (key in keys()) {
            map[key] = toJsonElement(opt(key))
        }
        return JsonObject(map)
    }
}