package com.agroland.feature.call

import com.agroland.feature.call.domain.CallDirection
import com.agroland.feature.call.domain.CallEndReason
import com.agroland.feature.call.domain.CallPhase
import com.agroland.feature.call.domain.VoiceCallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Қоңырау күйінің өзгерістері (Flutter VoiceCallState, 1:1):
 * фаза/terminalReason copy-семантикасы және overlay-дің көріну шарты.
 */
class VoiceCallModelsTest {

    @Test
    fun `әдепкі күй — idle және бос`() {
        val state = VoiceCallState()
        assertEquals(CallPhase.IDLE, state.phase)
        assertNull(state.direction)
        assertNull(state.callId)
        assertNull(state.peerUserId)
        assertNull(state.terminalReason)
        assertEquals(0, state.durationSeconds)
    }

    @Test
    fun `incoming күйі — phase + direction + peer деректері`() {
        val state = VoiceCallState(
            phase = CallPhase.INCOMING,
            direction = CallDirection.INCOMING,
            callId = "c42",
            peerUserId = 31L,
            peerName = "Agroland",
        )
        assertEquals(CallPhase.INCOMING, state.phase)
        assertEquals(CallDirection.INCOMING, state.direction)
        assertEquals("c42", state.callId)
        assertEquals(31L, state.peerUserId)
    }

    @Test
    fun `copy — басқа өрістерге тиіспейді`() {
        val incoming = VoiceCallState(
            phase = CallPhase.INCOMING,
            direction = CallDirection.INCOMING,
            callId = "c42",
            peerName = "Ayan",
        )
        val connecting = incoming.copy(phase = CallPhase.CONNECTING)
        assertEquals(CallPhase.CONNECTING, connecting.phase)
        assertEquals("c42", connecting.callId)
        assertEquals("Ayan", connecting.peerName)
        assertEquals(CallDirection.INCOMING, connecting.direction)
    }

    @Test
    fun `terminal күй — себеп сақталады, басқа өрістер түсіп қалады`() {
        val ended = VoiceCallState(terminalReason = CallEndReason.DECLINED)
        assertEquals(CallPhase.IDLE, ended.phase)
        assertEquals(CallEndReason.DECLINED, ended.terminalReason)
    }

    @Test
    fun `clearTerminal — себеп тазартылады, overlay жабылады`() {
        val ended = VoiceCallState(terminalReason = CallEndReason.NO_ANSWER)
        val cleared = ended.copy(terminalReason = null)
        assertNull(cleared.terminalReason)
        assertTrue(cleared.phase == CallPhase.IDLE)
    }

    @Test
    fun `қоңырау күйі әр түрлі себептермен аяқталады — ешқайсысы шатаспайды`() {
        val reasons = CallEndReason.entries
        assertEquals(5, reasons.size)
        reasons.forEach { reason ->
            assertNotEquals(null, VoiceCallState(terminalReason = reason).terminalReason)
        }
    }
}