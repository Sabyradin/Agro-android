package com.agroland.feature.call

import com.agroland.feature.call.domain.VoiceCallEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.webrtc.PeerConnection

/**
 * TURN басымдылық тізбегі (spec §7): REST iceServers → signaling ice_servers.
 * REST орнатылған соң signaling мәні ешқашан оны баспайды (Flutter паритеті).
 */
class VoiceCallEngineIcePriorityTest {

    private val restServer: PeerConnection.IceServer =
        PeerConnection.IceServer.builder("turn:rest.kz:3478")
            .setUsername("u")
            .setPassword("p")
            .createIceServer()

    private val signalServer: PeerConnection.IceServer =
        PeerConnection.IceServer.builder("turn:signal.kz:3478").createIceServer()

    @Test
    fun `REST алдымен — signaling мәні еленбейді`() {
        val engine = VoiceCallEngine()
        engine.setRestIceServers(listOf(restServer))
        engine.setSignalIceServers(listOf(signalServer))
        val active = engine.activeIceServersForTest()
        assertNotNull(active)
        assertEquals(1, active!!.size)
        assertEquals("turn:rest.kz:3478", active.first().uri)
    }

    @Test
    fun `signaling алдымен — REST келгенде REST басып шығады`() {
        val engine = VoiceCallEngine()
        engine.setSignalIceServers(listOf(signalServer))
        assertEquals("turn:signal.kz:3478", engine.activeIceServersForTest()!!.first().uri)
        engine.setRestIceServers(listOf(restServer))
        assertEquals("turn:rest.kz:3478", engine.activeIceServersForTest()!!.first().uri)
    }

    @Test
    fun `REST жоқ — signaling қалады (backend-test TURN 404 жағдайы)`() {
        val engine = VoiceCallEngine()
        engine.setRestIceServers(null)
        engine.setSignalIceServers(listOf(signalServer))
        assertEquals("turn:signal.kz:3478", engine.activeIceServersForTest()!!.first().uri)
    }

    @Test
    fun `екеуі де бос — active null (STUN-only fallback)`() {
        val engine = VoiceCallEngine()
        engine.setRestIceServers(emptyList())
        engine.setSignalIceServers(emptyList())
        assertNull(engine.activeIceServersForTest())
    }
}