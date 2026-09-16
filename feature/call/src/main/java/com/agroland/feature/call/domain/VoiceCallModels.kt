package com.agroland.feature.call.domain

/**
 * WebRTC 1:1 аудио қоңырау кезеңдері (Flutter voice_call_state.dart, 1:1).
 */
enum class CallPhase {
    /** Бос — қоңырау жоқ. */
    IDLE,

    /** Caller: `call_invite` жіберілді, `call:invite_ack` күтілуде/келді. */
    OUTGOING,

    /** Callee: `call:incoming` келді, қолданушы Accept/Decline басуы күтілуде. */
    INCOMING,

    /** SDP handshake барысында (offer/answer берілуі/қабылдануы). */
    CONNECTING,

    /** Caller: offer жіберілді, `call:peer_answer` күтілуде. */
    WAITING_ANSWER,

    /** Қоңырау қосылды, аудио алмасуда. */
    ACTIVE,

    /** Желі қысқа үзілді — ICE restart барысында, қоңырау сақталады. */
    RECONNECTING,
}

/** Звонок бағыты. */
enum class CallDirection { OUTGOING, INCOMING }

/** Қоңырау аяқталған себеп (UI-да l10n арқылы локализацияланады). */
enum class CallEndReason { DECLINED, NO_ANSWER, OFFLINE, MIC_DENIED, CONNECTION_ERROR }

/**
 * Бір қоңыраудың толық күйі (Flutter VoiceCallState — plain класы, 1:1).
 */
data class VoiceCallState(
    val phase: CallPhase = CallPhase.IDLE,
    val direction: CallDirection? = null,
    /** Backend жасайды (call:invite_ack / call:incoming арқылы келеді). */
    val callId: String? = null,
    val peerUserId: Long? = null,
    val peerName: String? = null,
    val peerAvatarUrl: String? = null,
    /** Активті қоңырау ұзақтығы (секунд). */
    val durationSeconds: Int = 0,
    val muted: Boolean = false,
    val speakerOn: Boolean = false,
    /** Аяқталған кезде көрсетілетін бір реттік себеп. Null — хабарлама жоқ. */
    val terminalReason: CallEndReason? = null,
)