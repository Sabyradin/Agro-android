package com.agroland.feature.call.domain

import android.content.Context
import android.media.AudioManager
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * WebRTC peer-connection + SDP/ICE handshake инкапсуляциясы (Flutter
 * VoiceCallController.dart, 1:1 порт; flutter_webrtc → stream-webrtc-android).
 *
 * Socket signaling осы классқа кірмейді — VoiceCallManager-нен келген
 * әрекеттерді орындайды, ICE candidate-терін [onIceCandidate] арқылы өткізеді.
 * SDP observer-лері suspend орамдарына оралынды (сәтсіздік — exception,
 * VoiceCallManager оны CONNECTION_ERROR терминалына айналдырады).
 */
@Singleton
class VoiceCallEngine @Inject constructor() {

    /** Локалды ICE candidate (signal арқылы жіберіледі). */
    @Volatile
    var onIceCandidate: ((candidate: String, sdpMid: String?, sdpMLineIndex: Int?) -> Unit)? = null

    /** Remote аудио келді — active күйге өту үшін (аудио автоматты ойнайды). */
    @Volatile
    var onConnected: (() -> Unit)? = null

    /** ICE қосылым күйі — reconnecting фазасы + ICE restart (handoff §6.9). */
    @Volatile
    var onConnectionStateChanged: ((PeerConnection.PeerConnectionState) -> Unit)? = null

    private var factory: PeerConnectionFactory? = null
    private var pc: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var remoteDescriptionSet = false
    private val pendingCandidates = mutableListOf<IceCandidate>()

    /** Осы қоңырауда біз offerer па — ICE restart тек offerer жасай алады. */
    private var isOfferer = false

    private var audioManager: AudioManager? = null

    private val eglBase: EglBase by lazy { EglBase.create() }

    /** REST TURN жауабы (ең жоғары басымдылық). */
    @Volatile
    private var iceServersOverride: List<PeerConnection.IceServer>? = null

    /** Signaling payload ice_servers (REST жоқ болса қолданылады). */
    @Volatile
    private var iceServersFromSignal: List<PeerConnection.IceServer>? = null

    // ── Initialization / ICE servers ─────────────────────────────────

    private fun ensureFactory(context: Context): PeerConnectionFactory {
        factory?.let { return it }
        synchronized(this) {
            factory?.let { return it }
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                    .createInitializationOptions(),
            )
            val f = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
                .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
                .createPeerConnectionFactory()
            factory = f
            return f
        }
    }

    /**
     * TURN басымдылық тізбегі (spec §7): REST iceServers → signaling ice_servers
     * → STUN-only. Flutter-дегі AppConfig TURN орта буыны Android портында жоқ
     * (env-конфиг енгізілмеген).
     */
    fun setRestIceServers(servers: List<PeerConnection.IceServer>?) {
        iceServersOverride = servers?.takeIf { it.isNotEmpty() }
    }

    fun setSignalIceServers(servers: List<PeerConnection.IceServer>?) {
        // REST TURN қолданылған болса — signaling мәнін баспаймыз (Flutter паритеті).
        if (iceServersOverride != null) return
        iceServersFromSignal = servers?.takeIf { it.isNotEmpty() }
    }

    /** Тест үшін: қолданыстағы ICE серверлері (REST → signaling тізбегі). */
    internal fun activeIceServersForTest(): List<PeerConnection.IceServer>? =
        iceServersOverride ?: iceServersFromSignal

    // ── PeerConnection lifecycle ────────────────────────────────────

    private fun newPeerConnection(context: Context) {
        disposePeerConnection()
        remoteDescriptionSet = false
        pendingCandidates.clear()

        val stunOnly = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        )
        val servers = iceServersOverride ?: iceServersFromSignal ?: stunOnly
        val config = PeerConnection.RTCConfiguration(servers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        pc = ensureFactory(context).createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                onIceCandidate?.invoke(candidate.sdp, candidate.sdpMid, candidate.sdpMLineIndex)
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                // Remote аудио WebRTC audio device module арқылы автоматты ойнайды.
                onConnected?.invoke()
            }

            override fun onAddStream(stream: MediaStream?) {
                onConnected?.invoke()
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                onConnectionStateChanged?.invoke(newState)
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) = Unit
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) = Unit
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
            override fun onRemoveStream(stream: MediaStream?) = Unit
            override fun onDataChannel(channel: DataChannel?) = Unit
            override fun onRenegotiationNeeded() = Unit
            override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
        })
    }

    /** Локалды микрофон трегі (RECORD_AUDIO рұқсаты әрі берілген болуы керек). */
    private fun ensureLocalAudio(context: Context) {
        if (audioTrack != null) return
        val f = ensureFactory(context)
        val source = f.createAudioSource(MediaConstraints())
        val track = f.createAudioTrack("agroland_audio", source)
        audioSource = source
        audioTrack = track
        pc?.addTrack(track, listOf("agroland_stream"))
    }

    // ── Caller: offer ────────────────────────────────────────────────

    /**
     * Offer құрып жібереді. `iceRestart = true` — ICE restart re-offer
     * (pc қайта құрылмайды; 1:1 Flutter createAndSendOffer).
     */
    suspend fun createAndSendOffer(
        context: Context,
        callId: String,
        iceRestart: Boolean = false,
        sendOffer: (sdpType: String, sdp: String) -> Unit,
    ) {
        if (pc == null) {
            newPeerConnection(context)
            ensureLocalAudio(context)
            isOfferer = true
        }
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            if (iceRestart) mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
        }
        val offer = createSdp { observer -> pc?.createOffer(observer, constraints) }
        setLocalDescriptionSuspend(offer)
        sendOffer(offer.type.canonicalForm(), offer.description)
    }

    /** ICE restart — тек offerer жасай алады (handoff §6.9). true — re-offer жіберілді. */
    suspend fun restartIce(
        context: Context,
        callId: String,
        sendOffer: (sdpType: String, sdp: String) -> Unit,
    ): Boolean {
        if (pc == null || !isOfferer) return false
        createAndSendOffer(context, callId, iceRestart = true, sendOffer = sendOffer)
        return true
    }

    // ── Callee: келген offer-ды өңдеу + answer ────────────────────────

    suspend fun handlePeerOffer(
        context: Context,
        callId: String,
        sdpType: String?,
        sdp: String?,
        sendAnswer: (sdpType: String, sdp: String) -> Unit,
    ) {
        if (pc == null) {
            newPeerConnection(context)
            ensureLocalAudio(context)
        }
        setRemoteDescriptionSuspend(SessionDescription(typeOf(sdpType), sdp ?: ""))
        remoteDescriptionSet = true
        flushPendingCandidates()
        val answer = createSdp { observer -> pc?.createAnswer(observer, MediaConstraints()) }
        setLocalDescriptionSuspend(answer)
        sendAnswer(answer.type.canonicalForm(), answer.description)
    }

    // ── Caller: келген answer ──────────────────────────────────────────

    suspend fun handlePeerAnswer(sdpType: String?, sdp: String?) {
        if (pc == null) return
        setRemoteDescriptionSuspend(SessionDescription(typeOf(sdpType), sdp ?: ""))
        remoteDescriptionSet = true
        flushPendingCandidates()
    }

    // ── ICE candidate (екеуіне де) ────────────────────────────────────

    fun handlePeerIce(candidate: String?, sdpMid: String?, sdpMLineIndex: Int?) {
        val ice = candidate?.let { IceCandidate(sdpMid, sdpMLineIndex ?: 0, it) } ?: return
        val connection = pc
        if (connection == null || !remoteDescriptionSet) {
            pendingCandidates.add(ice)
            return
        }
        connection.addIceCandidate(ice)
    }

    private fun flushPendingCandidates() {
        val connection = pc ?: return
        pendingCandidates.forEach { connection.addIceCandidate(it) }
        pendingCandidates.clear()
    }

    // ── Аудио басқару ────────────────────────────────────────────────

    fun setMuted(muted: Boolean) {
        audioTrack?.setEnabled(!muted)
    }

    /** Динамик/құлаққап маршруты + MODE_IN_COMMUNICATION (spec §7). */
    fun setSpeaker(context: Context, on: Boolean) {
        val am = audioManager ?: context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager = am
        am.isSpeakerphoneOn = on
    }

    fun enterCallAudioMode(context: Context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager = am
        am.mode = AudioManager.MODE_IN_COMMUNICATION
    }

    fun leaveCallAudioMode() {
        audioManager?.mode = AudioManager.MODE_NORMAL
        audioManager = null
    }

    // ── Teardown ─────────────────────────────────────────────────────

    fun teardown() {
        disposePeerConnection()
        audioTrack?.dispose()
        audioSource?.dispose()
        audioTrack = null
        audioSource = null
        iceServersOverride = null
        iceServersFromSignal = null
        isOfferer = false
        onIceCandidate = null
        onConnected = null
        onConnectionStateChanged = null
        leaveCallAudioMode()
    }

    private fun disposePeerConnection() {
        pc?.close()
        pc = null
        remoteDescriptionSet = false
        pendingCandidates.clear()
    }

    private fun typeOf(sdpType: String?): SessionDescription.Type =
        if (sdpType?.trim()?.lowercase() == SessionDescription.Type.OFFER.canonicalForm()) {
            SessionDescription.Type.OFFER
        } else {
            SessionDescription.Type.ANSWER
        }

    // ── suspend SdpObserver орамдары ────────────────────────────────

    /** createOffer/createAnswer → SessionDescription (onCreateSuccess). */
    private suspend fun createSdp(
        create: (SdpObserver) -> Unit,
    ): SessionDescription = suspendCancellableCoroutine { cont ->
        val observer = object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (cont.isActive && sdp != null) cont.resume(sdp)
            }

            override fun onCreateFailure(error: String?) {
                if (cont.isActive) cont.resumeWithException(IllegalStateException(error ?: "sdp error"))
            }
        }
        try {
            create(observer)
        } catch (t: Throwable) {
            if (cont.isActive) cont.resumeWithException(t)
        }
    }

    private suspend fun setLocalDescriptionSuspend(sdp: SessionDescription) {
        val connection = pc ?: return
        suspendCancellableCoroutine { cont ->
            connection.setLocalDescription(
                setDescriptionObserver(
                    onSetSuccess = { if (cont.isActive) cont.resume(Unit) },
                    onSetFailure = { err ->
                        if (cont.isActive) {
                            cont.resumeWithException(IllegalStateException(err ?: "setLocal failed"))
                        }
                    },
                ),
                sdp,
            )
        }
    }

    private suspend fun setRemoteDescriptionSuspend(sdp: SessionDescription) {
        val connection = pc ?: throw IllegalStateException("pc null")
        suspendCancellableCoroutine { cont ->
            connection.setRemoteDescription(
                setDescriptionObserver(
                    onSetSuccess = { if (cont.isActive) cont.resume(Unit) },
                    onSetFailure = { err ->
                        if (cont.isActive) {
                            cont.resumeWithException(IllegalStateException(err ?: "setRemote failed"))
                        }
                    },
                ),
                sdp,
            )
        }
    }

    private fun setDescriptionObserver(
        onSetSuccess: () -> Unit,
        onSetFailure: (String?) -> Unit,
    ) = object : SdpObserverAdapter() {
        override fun onSetSuccess() = onSetSuccess()
        override fun onSetFailure(error: String?) = onSetFailure(error)
    }

    private open class SdpObserverAdapter : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) = Unit
        override fun onSetFailure(error: String?) = Unit
    }
}