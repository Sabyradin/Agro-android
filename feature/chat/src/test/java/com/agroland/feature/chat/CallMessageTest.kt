package com.agroland.feature.chat

import com.agroland.feature.chat.domain.CallMessage
import com.agroland.feature.chat.domain.CallMessage.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** [voice_call:status:duration] маркерлері — CallMessage. */
class CallMessageTest {

    @Test
    fun `дұрыс маркерлер`() {
        assertTrue(CallMessage.isCallMessage("[voice_call:completed:120]"))
        assertTrue(CallMessage.isCallMessage("[voice_call:missed:0]"))
        assertTrue(CallMessage.isCallMessage(" [voice_call:declined:15] "))
    }

    @Test
    fun `жарамсыз маркерлер`() {
        assertFalse(CallMessage.isCallMessage("hello"))
        assertFalse(CallMessage.isCallMessage("[voice_call:unknown:10]"))
        assertFalse(CallMessage.isCallMessage("[voice_call:completed]"))
    }

    @Test
    fun `parse — статус пен ұзақтық`() {
        assertEquals(Status.COMPLETED to 120, CallMessage.parse("[voice_call:completed:120]"))
        assertEquals(Status.MISSED to 0, CallMessage.parse("[voice_call:missed:0]"))
        assertEquals(Status.DECLINED to 15, CallMessage.parse("[voice_call:declined:15]"))
        assertNull(CallMessage.parse("not a call"))
    }

    @Test
    fun `statusLabelKey`() {
        assertEquals(Status.COMPLETED, CallMessage.statusLabelKey("[voice_call:completed:1]"))
        assertNull(CallMessage.statusLabelKey("text"))
    }
}