package com.agroland.feature.push

import com.agroland.feature.push.data.PushRequests
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** POST /device дене пішіні (Flutter sendPushToken). */
class PushRequestsTest {

    @Test
    fun `registration body has all required fields`() {
        val body: JsonObject = PushRequests.deviceRegistration(
            deviceId = "ssaid-123",
            firebaseToken = "fcm-token",
            osVersion = "15",
            appVersion = "1.0.0",
            deviceModel = "Pixel 8",
            language = "kk",
            timezone = "Asia/Almaty",
        )
        assertEquals("ssaid-123", body["device_id"]!!.jsonPrimitive.content)
        assertEquals("fcm-token", body["firebase_token"]!!.jsonPrimitive.content)
        assertEquals("android", body["platform"]!!.jsonPrimitive.content)
        assertEquals("15", body["os_version"]!!.jsonPrimitive.content)
        assertEquals("1.0.0", body["app_version"]!!.jsonPrimitive.content)
        assertEquals("Pixel 8", body["device_model"]!!.jsonPrimitive.content)
        assertEquals("kk", body["language"]!!.jsonPrimitive.content)
        assertEquals("Asia/Almaty", body["timezone"]!!.jsonPrimitive.content)
    }

    @Test
    fun `optional timezone and location are omitted when absent`() {
        val body = PushRequests.deviceRegistration(
            deviceId = "d", firebaseToken = "t", osVersion = "o",
            appVersion = "a", deviceModel = "m", language = "kk",
            timezone = null,
        )
        assertFalse(body.containsKey("timezone"))
        assertFalse(body.containsKey("location"))
    }

    @Test
    fun `location object is included when both coordinates present`() {
        val body = PushRequests.deviceRegistration(
            deviceId = "d", firebaseToken = "t", osVersion = "o",
            appVersion = "a", deviceModel = "m", language = "kk",
            timezone = "Asia/Almaty",
            latitude = 43.25,
            longitude = 76.95,
        )
        assertTrue(body.containsKey("location"))
        assertTrue(body.toString().contains("\"latitude\""))
        assertTrue(body.toString().contains("\"longitude\""))
    }
}