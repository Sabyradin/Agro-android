package com.agroland.feature.call.domain

import android.content.Context
import com.agroland.core.network.json.JsonParser
import com.agroland.feature.call.data.TurnCredentialsRepository
import com.agroland.feature.call.platform.IncomingCallNotifier
import com.agroland.feature.chat.data.ChatSocketManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.webrtc.PeerConnection

/**
 * WebRTC 1:1 аудио қоңыраудың state machine-і (Flutter VoiceCallNotifier.dart,
 * 1:1 порт). Socket `call:*` event-терін тыңдап, [VoiceCallEngine]-ті басқарады.
 *
 * Socket — бірдей ChatSocketManager қосылымы (Flutter-де voice_call
 * socketProvider-ді қайта пайдаланады). Қайта қосылғанда listener-лер
 * қайта бекітіледі (CONNECTED state наблюдациясы арқылы).
 */
@Singleton
class VoiceCallManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val socketManager: ChatSocketManager,
    private val engine: VoiceCallEngine,
    private val turnRepository: TurnCredentialsRepository,
    private val incomingCallNotifier: IncomingCallNotifier,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(VoiceCallState())
    val state: StateFlow<VoiceCallState> = _state.asStateFlow()

    /** Қоңырау экраны көрінуде ме (MainActivity overlay хосты үшін). */
    val callActive: Boolean get() = _state.value.phase != CallPhase.IDLE

    private var socketSubscriptions = mutableListOf<ChatSocketManager.Subscription>()
    private var socketScopeJob: Job? = null

    private var durationJob: Job? = null
    private var noAnswerJob: Job? = null
    private var reconnectJob: Job? = null

    /** Бір қоңырау барысында ең көп бір ICE restart әрекеті (шексіз циклден сақ). */
    private var restartAttempted = false

    private var restTurnApplied = false

    /** REST TURN кэші — repository өзі кэштейді, бірақ «қолданылды» флагы осында. */
    private var pendingRestIceServers: List<PeerConnection.IceServer>? = null

    // ── Іске қосу / socket binding ────────────────────────────────────

    /**
     * Сессия ашылғанда шақырылады (MainActivity): socket CONNECTED болса
     * listener-лерді бекітеді және қайта қосылғанда қайта бекітеді.
     */
    fun start() {
        socketScopeJob?.cancel()
        socketScopeJob = scope.launch {
            socketManager.state.collect { s ->
                if (s == ChatSocketManager.State.CONNECTED) {
                    attachSocketListeners()
                }
            }
        }
        // Telecom ConnectionService + notification Accept/Decline көпірі.
        systemCallActions = object : SystemCallActions {
            override fun onSystemAnswer() {
                if (micGranted()) {
                    accept(micGranted = true)
                } else {
                    // Рұқсат жоқ — accept экранның өзінен өтеді (рұқсат диалогымен);
                    // мұнда тек қоңырау экраны ашылады.
                    openCallScreen()
                }
            }

            override fun onSystemReject() = reject()

            override fun onSystemDisconnect() = hangup()
        }
    }

    fun stop() {
        socketScopeJob?.cancel()
        socketScopeJob = null
        detachSocketListeners()
        systemCallActions = null
        hangup()
    }

    private fun micGranted(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.RECORD_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun openCallScreen() {
        val launch = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName) ?: return
        launch.addFlags(
            android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP,
        )
        appContext.startActivity(launch)
    }

    private fun detachSocketListeners() {
        socketSubscriptions.forEach { it.cancel() }
        socketSubscriptions.clear()
    }

    private fun attachSocketListeners() {
        detachSocketListeners()

        fun listen(event: String, handler: (JsonElement?) -> Unit) {
            socketManager.on(event, handler)?.let { socketSubscriptions.add(it) }
        }

        // ── Callee: кіріс қоңырау ────────────────────────────────────
        listen("call:incoming") { el ->
            val payload = (el as? JsonObject) ?: return@listen
            val callId = JsonParser.string(payload, "call_id")
            val fromUserId = JsonParser.long(payload, "from_user_id")
            // Backend TURN/STUN тізімін осында жіберуі мүмкін.
            applySignalIceServers(payload["ice_servers"])
            if (_state.value.phase != CallPhase.IDLE) {
                if (callId != null) emitCallReject(callId, "busy")
                return@listen
            }
            engine.enterCallAudioMode(appContext)
            _state.value = VoiceCallState(
                phase = CallPhase.INCOMING,
                direction = CallDirection.INCOMING,
                callId = callId,
                peerUserId = fromUserId,
                peerName = JsonParser.string(payload, "from_user_name"),
                peerAvatarUrl = JsonParser.string(payload, "from_user_avatar"),
            )
            // Фонда болса — full-screen intent; Telecom қоңырауды өзі тіркейді.
            incomingCallNotifier.showIncomingCall(_state.value)
        }

        // ── Caller: invite_ack ────────────────────────────────────────
        listen("call:invite_ack") { el ->
            val payload = (el as? JsonObject) ?: return@listen
            val st = _state.value
            if (st.phase != CallPhase.OUTGOING) return@listen
            applySignalIceServers(payload["ice_servers"])
            val callId = JsonParser.string(payload, "call_id")
            if (callId != null) {
                _state.value = st.copy(callId = callId)
            }
            val calleeOnline = JsonParser.bool(payload, "callee_online") == true
            if (!calleeOnline && JsonParser.bool(payload, "push_sent") != true) {
                endWithError(CallEndReason.OFFLINE)
            }
        }

        // ── Caller: callee қабылдады ──────────────────────────────────
        listen("call:accepted") { el ->
            val st = _state.value
            if (st.phase != CallPhase.OUTGOING) return@listen
            val callId = st.callId ?: return@listen
            _state.value = st.copy(phase = CallPhase.CONNECTING)
            scope.launch {
                try {
                    sendOffer(callId, iceRestart = false)
                    if (_state.value.phase == CallPhase.CONNECTING) {
                        _state.value = _state.value.copy(phase = CallPhase.WAITING_ANSWER)
                    }
                } catch (t: Throwable) {
                    endWithError(CallEndReason.CONNECTION_ERROR)
                }
            }
        }

        // ── Caller: callee қабылдамады ────────────────────────────────
        listen("call:rejected") { el ->
            val payload = (el as? JsonObject) ?: JsonObject(emptyMap())
            val reason = JsonParser.string(payload, "reason") ?: ""
            endWithError(
                if (isNoAnswerReason(reason)) CallEndReason.NO_ANSWER else CallEndReason.DECLINED,
            )
        }

        // ── Екі жақ: peer қоңырауды бітірді ────────────────────────────
        listen("call:hungup") { _ ->
            endSilent()
        }

        // ── Callee: caller offer-ы ─────────────────────────────────────
        listen("call:peer_offer") { el ->
            val payload = (el as? JsonObject) ?: return@listen
            val st = _state.value
            val callId = JsonParser.string(payload, "call_id") ?: st.callId ?: return@listen
            val sdpObj = payload["sdp"] as? JsonObject
            when (st.phase) {
                CallPhase.ACTIVE, CallPhase.RECONNECTING -> scope.launch {
                    // ICE restart re-offer — pc қайта құрылмайды, re-answer.
                    try {
                        handleOffer(callId, sdpObj)
                    } catch (t: Throwable) {
                        endWithError(CallEndReason.CONNECTION_ERROR)
                    }
                }

                CallPhase.CONNECTING, CallPhase.INCOMING -> {
                    _state.value = st.copy(phase = CallPhase.CONNECTING, callId = callId)
                    scope.launch {
                        try {
                            handleOffer(callId, sdpObj)
                            if (_state.value.phase == CallPhase.CONNECTING) {
                                _state.value = _state.value.copy(phase = CallPhase.ACTIVE)
                                startDurationTimer()
                            }
                        } catch (t: Throwable) {
                            endWithError(CallEndReason.CONNECTION_ERROR)
                        }
                    }
                }

                else -> Unit
            }
        }

        // ── Caller: callee answer-ы ────────────────────────────────────
        listen("call:peer_answer") { el ->
            val payload = (el as? JsonObject) ?: return@listen
            val st = _state.value
            if (st.phase != CallPhase.WAITING_ANSWER && st.phase != CallPhase.CONNECTING) return@listen
            val sdpObj = payload["sdp"] as? JsonObject
            scope.launch {
                try {
                    engine.handlePeerAnswer(
                        sdpType = (sdpObj?.get("type") as? JsonPrimitive)?.content,
                        sdp = (sdpObj?.get("sdp") as? JsonPrimitive)?.content,
                    )
                    if (_state.value.phase == CallPhase.WAITING_ANSWER ||
                        _state.value.phase == CallPhase.CONNECTING
                    ) {
                        _state.value = _state.value.copy(phase = CallPhase.ACTIVE)
                        startDurationTimer()
                    }
                } catch (t: Throwable) {
                    endWithError(CallEndReason.CONNECTION_ERROR)
                }
            }
        }

        // ── ICE candidate (екеуіне де) ─────────────────────────────────
        listen("call:peer_ice") { el ->
            val payload = (el as? JsonObject) ?: return@listen
            val candidate = payload["candidate"] as? JsonObject
            engine.handlePeerIce(
                candidate = (candidate?.get("candidate") as? JsonPrimitive)?.content,
                sdpMid = (candidate?.get("sdpMid") as? JsonPrimitive)?.content,
                sdpMLineIndex = JsonParser.int(candidate, "sdpMLineIndex"),
            )
        }

        // ── Қате ──────────────────────────────────────────────────────
        listen("call:error") { _ ->
            endWithError(CallEndReason.CONNECTION_ERROR)
        }
    }

    // ── Публік әрекеттер ──────────────────────────────────────────────

    /**
     * Caller қоңырау бастайды. [micGranted] — RECORD_AUDIO рұқсаты (UI
     * әрекеттеніп береді; Flutter-де probe+request миксі болған).
     */
    fun invite(peerUserId: Long, peerName: String?, peerAvatarUrl: String?, micGranted: Boolean) {
        if (_state.value.phase != CallPhase.IDLE) return
        if (!micGranted) {
            // Рұқсат жоқ — қоңырау экраны қысқа «Микрофон қолжетімсіз»-бен
            // көрсетіліп жабылады (Flutter VoiceCallHost idle+terminal жағдайы).
            _state.value = VoiceCallState(
                peerUserId = peerUserId,
                peerName = peerName,
                peerAvatarUrl = peerAvatarUrl,
                direction = CallDirection.OUTGOING,
                terminalReason = CallEndReason.MIC_DENIED,
            )
            return
        }
        if (!socketManager.connected) {
            // Socket қосылмаған — signaling жетпейді, тез «Желіде жоқ».
            socketManager.connect()
            _state.value = VoiceCallState(
                peerUserId = peerUserId,
                peerName = peerName,
                peerAvatarUrl = peerAvatarUrl,
                direction = CallDirection.OUTGOING,
                terminalReason = CallEndReason.OFFLINE,
            )
            return
        }
        _state.value = VoiceCallState(
            phase = CallPhase.OUTGOING,
            direction = CallDirection.OUTGOING,
            peerUserId = peerUserId,
            peerName = peerName,
            peerAvatarUrl = peerAvatarUrl,
        )
        wireEngineCallbacks()
        scope.launch {
            ensureRestTurnCredentials()
            emit(
                "call_invite",
                JsonObject(mapOf("to_user_id" to JsonPrimitive(peerUserId.toString()))),
            )
            startNoAnswerTimer()
        }
    }

    /** Callee қоңырауды қабылдайды (рұқсат UI-ден келеді). */
    fun accept(micGranted: Boolean) {
        val st = _state.value
        if (st.phase != CallPhase.INCOMING) return
        if (!micGranted) {
            st.callId?.let { emitCallReject(it, "mic_denied") }
            incomingCallNotifier.cancel()
            _state.value = VoiceCallState(terminalReason = CallEndReason.MIC_DENIED)
            engine.teardown()
            return
        }
        val callId = st.callId
        if (callId == null || !socketManager.connected) {
            endWithError(CallEndReason.CONNECTION_ERROR)
            return
        }
        incomingCallNotifier.cancel()
        wireEngineCallbacks()
        _state.value = st.copy(phase = CallPhase.CONNECTING)
        scope.launch {
            ensureRestTurnCredentials()
            emit("call_accept", JsonObject(mapOf("call_id" to JsonPrimitive(callId))))
        }
    }

    /** Callee қоңыраудан бас тартады. */
    fun reject() {
        val st = _state.value
        if (st.phase != CallPhase.INCOMING) return
        st.callId?.let { emitCallReject(it, "declined") }
        incomingCallNotifier.cancel()
        resetTransientState()
        engine.teardown()
        _state.value = VoiceCallState()
    }

    /** Кез келген жақ қоңырауды бітіреді (hangup). */
    fun hangup() {
        val st = _state.value
        if (st.phase == CallPhase.IDLE) return
        st.callId?.let { emit("call_hangup", JsonObject(mapOf("call_id" to JsonPrimitive(it)))) }
        incomingCallNotifier.cancel()
        resetTransientState()
        engine.teardown()
        _state.value = VoiceCallState()
    }

    fun toggleMute() {
        val st = _state.value
        val muted = !st.muted
        engine.setMuted(muted)
        _state.value = st.copy(muted = muted)
    }

    fun toggleSpeaker() {
        val st = _state.value
        val on = !st.speakerOn
        engine.setSpeaker(appContext, on)
        _state.value = st.copy(speakerOn = on)
    }

    /** UI terminal хабарламасын көрсетіп болған соң шақырады. */
    fun clearTerminal() {
        val st = _state.value
        if (st.terminalReason != null) {
            _state.value = st.copy(terminalReason = null)
        }
    }

    // ── Engine → state callback-тері ──────────────────────────────────

    private fun wireEngineCallbacks() {
        engine.onConnected = {
            val st = _state.value
            if (st.phase != CallPhase.ACTIVE && st.phase != CallPhase.RECONNECTING) {
                _state.value = st.copy(phase = CallPhase.ACTIVE)
                startDurationTimer()
            }
        }
        engine.onConnectionStateChanged = { s ->
            when (s) {
                PeerConnection.PeerConnectionState.CONNECTED -> {
                    reconnectJob?.cancel()
                    reconnectJob = null
                    if (_state.value.phase == CallPhase.RECONNECTING) {
                        _state.value = _state.value.copy(phase = CallPhase.ACTIVE)
                    }
                }

                PeerConnection.PeerConnectionState.DISCONNECTED -> {
                    if (_state.value.phase == CallPhase.ACTIVE) {
                        _state.value = _state.value.copy(phase = CallPhase.RECONNECTING)
                        startReconnectFallback()
                    } else if (_state.value.phase == CallPhase.RECONNECTING) {
                        startReconnectFallback()
                    }
                }

                PeerConnection.PeerConnectionState.FAILED -> {
                    if (!restartAttempted) {
                        restartAttempted = true
                        val callId = _state.value.callId
                        if (callId != null) {
                            scope.launch {
                                val ok = engine.restartIce(appContext, callId) { type, sdp ->
                                    emitOffer(callId, type, sdp)
                                }
                                if (ok) {
                                    _state.value = _state.value.copy(phase = CallPhase.RECONNECTING)
                                    startReconnectFallback()
                                } else {
                                    endWithError(CallEndReason.CONNECTION_ERROR)
                                }
                            }
                        } else {
                            endWithError(CallEndReason.CONNECTION_ERROR)
                        }
                    } else {
                        endWithError(CallEndReason.CONNECTION_ERROR)
                    }
                }

                PeerConnection.PeerConnectionState.CLOSED ->
                    endWithError(CallEndReason.CONNECTION_ERROR)

                else -> Unit
            }
        }
        engine.onIceCandidate = { candidate, sdpMid, sdpMLineIndex ->
            val callId = _state.value.callId
            if (callId != null) {
                emit(
                    "call_ice_candidate",
                    JsonObject(
                        mapOf(
                            "call_id" to JsonPrimitive(callId),
                            "candidate" to JsonObject(
                                mapOf(
                                    "candidate" to JsonPrimitive(candidate),
                                    "sdpMid" to (sdpMid?.let { JsonPrimitive(it) } ?: JsonNull),
                                    "sdpMLineIndex" to (sdpMLineIndex?.let { JsonPrimitive(it) }
                                        ?: JsonNull),
                                ),
                            ),
                        ),
                    ),
                )
            }
        }
    }

    // ── Offer/answer өңдеу ────────────────────────────────────────────

    private suspend fun sendOffer(callId: String, iceRestart: Boolean) {
        engine.createAndSendOffer(appContext, callId, iceRestart) { type, sdp ->
            emitOffer(callId, type, sdp)
        }
    }

    private fun emitOffer(callId: String, type: String, sdp: String) {
        emit(
            "call_offer",
            JsonObject(
                mapOf(
                    "call_id" to JsonPrimitive(callId),
                    "sdp" to JsonObject(
                        mapOf("type" to JsonPrimitive(type), "sdp" to JsonPrimitive(sdp)),
                    ),
                ),
            ),
        )
    }

    private suspend fun handleOffer(callId: String, sdpObj: JsonObject?) {
        engine.handlePeerOffer(
            context = appContext,
            callId = callId,
            sdpType = (sdpObj?.get("type") as? JsonPrimitive)?.content,
            sdp = (sdpObj?.get("sdp") as? JsonPrimitive)?.content,
        ) { type, sdp ->
            emit(
                "call_answer",
                JsonObject(
                    mapOf(
                        "call_id" to JsonPrimitive(callId),
                        "sdp" to JsonObject(
                            mapOf("type" to JsonPrimitive(type), "sdp" to JsonPrimitive(sdp)),
                        ),
                    ),
                ),
            )
        }
    }

    private fun emitCallReject(callId: String, reason: String) {
        emit(
            "call_reject",
            JsonObject(
                mapOf(
                    "call_id" to JsonPrimitive(callId),
                    "reason" to JsonPrimitive(reason),
                ),
            ),
        )
    }

    private fun emit(event: String, payload: JsonObject) {
        socketManager.emit(event, payload)
    }

    // ── ICE servers тізбегі ───────────────────────────────────────────

    private fun applySignalIceServers(raw: kotlinx.serialization.json.JsonElement?) {
        if (restTurnApplied) return
        val servers = parseIceServers(raw) ?: return
        if (servers.isEmpty()) return
        engine.setSignalIceServers(servers)
    }

    /** `[{urls:[...], username?, credential?}]` → IceServer тізімі. */
    private fun parseIceServers(raw: kotlinx.serialization.json.JsonElement?): List<PeerConnection.IceServer>? {
        val arr = raw as? JsonArray ?: return null
        val result = mutableListOf<PeerConnection.IceServer>()
        for (entry in arr) {
            val obj = entry as? JsonObject ?: continue
            val urls = JsonParser.arr(obj, "urls")
                ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotEmpty) }
                ?: emptyList()
            if (urls.isEmpty()) continue
            val builder = PeerConnection.IceServer.builder(urls)
            JsonParser.string(obj, "username")?.takeIf { it.isNotEmpty() }?.let(builder::setUsername)
            JsonParser.string(obj, "credential")?.takeIf { it.isNotEmpty() }?.let(builder::setPassword)
            result.add(builder.createIceServer())
        }
        return result
    }

    /** REST TURN: сәтсіз — silent fallback (signaling/STUN қалады). */
    private suspend fun ensureRestTurnCredentials() {
        val creds = turnRepository.fetch()
        if (creds != null) {
            val servers = listOf(creds.toIceServer())
            engine.setRestIceServers(servers)
            pendingRestIceServers = servers
            restTurnApplied = true
        } else {
            restTurnApplied = false
        }
    }

    // ── Таймерлер ──────────────────────────────────────────────────────

    private fun startDurationTimer() {
        durationJob?.cancel()
        stopNoAnswerTimer()
        _state.value = _state.value.copy(durationSeconds = 0)
        durationJob = scope.launch {
            while (true) {
                delay(1000)
                val st = _state.value
                if (st.phase == CallPhase.ACTIVE || st.phase == CallPhase.RECONNECTING) {
                    _state.value = st.copy(durationSeconds = st.durationSeconds + 1)
                } else {
                    break
                }
            }
        }
    }

    /** 35с: invite → active өтпесе «Жауап жоқ» (dev ортада voice_call жоқ болса да ілініп қалмайды). */
    private fun startNoAnswerTimer() {
        stopNoAnswerTimer()
        noAnswerJob = scope.launch {
            delay(NO_ANSWER_TIMEOUT_MS)
            val p = _state.value.phase
            if (p == CallPhase.OUTGOING || p == CallPhase.CONNECTING || p == CallPhase.WAITING_ANSWER) {
                endWithError(CallEndReason.NO_ANSWER)
            }
        }
    }

    private fun stopNoAnswerTimer() {
        noAnswerJob?.cancel()
        noAnswerJob = null
    }

    /** Reconnecting 20с ішінде қалпына келмесе — доғару (restart бір рет жасалды). */
    private fun startReconnectFallback() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(RECONNECT_TIMEOUT_MS)
            if (_state.value.phase == CallPhase.RECONNECTING) {
                endWithError(CallEndReason.CONNECTION_ERROR)
            }
        }
    }

    private fun resetTransientState() {
        durationJob?.cancel()
        durationJob = null
        stopNoAnswerTimer()
        reconnectJob?.cancel()
        reconnectJob = null
        restartAttempted = false
        restTurnApplied = false
    }

    // ── Аяқтау ────────────────────────────────────────────────────────

    private fun endWithError(reason: CallEndReason) {
        incomingCallNotifier.cancel()
        resetTransientState()
        engine.teardown()
        _state.value = VoiceCallState(terminalReason = reason)
    }

    private fun endSilent() {
        incomingCallNotifier.cancel()
        resetTransientState()
        engine.teardown()
        _state.value = VoiceCallState()
    }

    private fun isNoAnswerReason(reason: String): Boolean =
        reason.contains("no_answer") || reason.contains("timeout") ||
            reason.contains("no answer") || reason.contains("unanswered")

    companion object {
        private const val NO_ANSWER_TIMEOUT_MS = 35_000L
        private const val RECONNECT_TIMEOUT_MS = 20_000L

        /** ConnectionService (Telecom) жағынан answer/reject/disconnect шақырады. */
        @Volatile
        internal var systemCallActions: SystemCallActions? = null
    }

    /** Telecom ConnectionService → VoiceCallManager көпірі. */
    interface SystemCallActions {
        fun onSystemAnswer()
        fun onSystemReject()
        fun onSystemDisconnect()
    }
}