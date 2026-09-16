package com.agroland.feature.chat.domain

/**
 * Қоңырау маркері `[voice_call:<status>:<duration_seconds>]`
 * (Flutter CallMessageBubble, MOBILE_VOICE_CALL_TASK.md §6.4).
 * status ∈ {completed, missed, declined}. Phase 13 (қоңырау) жібереді —
 * чат осында тарихтағы карточканы рендерлейді.
 */
object CallMessage {

    private val REGEX = Regex("^\\[voice_call:(completed|missed|declined):(\\d+)]$")

    enum class Status { COMPLETED, MISSED, DECLINED }

    fun isCallMessage(text: String): Boolean = REGEX.matches(text.trim())

    fun parse(text: String): Pair<Status, Int>? {
        val m = REGEX.find(text.trim()) ?: return null
        val status = when (m.groupValues[1]) {
            "completed" -> Status.COMPLETED
            "missed" -> Status.MISSED
            else -> Status.DECLINED
        }
        return status to (m.groupValues[2].toIntOrNull() ?: 0)
    }

    /** Тізім preview / reply жолағы үшін адам оқитын атау (label UI-дан беріледі). */
    fun statusLabelKey(text: String): Status? = parse(text)?.first
}