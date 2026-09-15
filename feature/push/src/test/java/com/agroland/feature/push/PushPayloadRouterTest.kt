package com.agroland.feature.push

import com.agroland.feature.push.domain.PushDestination
import com.agroland.feature.push.domain.PushPayloadRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Push payload → меже дәйек тізбегі (Flutter _handlePayload тармақтауын қайталайды).
 */
class PushPayloadRouterTest {

    @Test
    fun `verification_approved opens verification`() {
        val dest = PushPayloadRouter.route(mapOf("event" to "verification_approved"))
        assertEquals(PushDestination.Verification, dest)
    }

    @Test
    fun `verification_rejected opens verification`() {
        val dest = PushPayloadRouter.route(mapOf("event" to "verification_rejected"))
        assertEquals(PushDestination.Verification, dest)
    }

    @Test
    fun `balance events open balance`() {
        assertEquals(
            PushDestination.Balance,
            PushPayloadRouter.route(mapOf("event" to "balance_credited")),
        )
        assertEquals(
            PushDestination.Balance,
            PushPayloadRouter.route(mapOf("event" to "withdraw_approved")),
        )
        assertEquals(
            PushDestination.Balance,
            PushPayloadRouter.route(mapOf("event" to "withdraw_rejected")),
        )
    }

    @Test
    fun `review_request with order opens order detail`() {
        val dest = PushPayloadRouter.route(
            mapOf("event" to "review_request", "order_id" to "42"),
        )
        assertEquals(PushDestination.OrderDetail(42), dest)
    }

    @Test
    fun `review_request without order falls through`() {
        val dest = PushPayloadRouter.route(mapOf("event" to "review_request"))
        assertEquals(PushDestination.Notifications(null, null), dest)
    }

    @Test
    fun `order_id opens order detail`() {
        val dest = PushPayloadRouter.route(mapOf("order_id" to "7", "event" to "order_status"))
        assertEquals(PushDestination.OrderDetail(7), dest)
    }

    @Test
    fun `chat event with sender opens chat room with all optional fields`() {
        val dest = PushPayloadRouter.route(
            mapOf(
                "event" to "chat",
                "sender_id" to "15",
                "sender_name" to "Асхат",
                "room_id" to "99",
                "is_system_chat" to "true",
            ),
        )
        assertEquals(
            PushDestination.ChatRoom(senderId = 15, senderName = "Асхат", roomId = 99, isSystemChat = true),
            dest,
        )
    }

    @Test
    fun `chat event falls back to chat_id when room_id missing`() {
        val dest = PushPayloadRouter.route(
            mapOf("event" to "chat", "sender_id" to "3", "chat_id" to "12"),
        )
        assertEquals(
            PushDestination.ChatRoom(senderId = 3, senderName = null, roomId = 12, isSystemChat = false),
            dest,
        )
    }

    @Test
    fun `chat event without sender falls through to notifications`() {
        val dest = PushPayloadRouter.route(mapOf("event" to "chat"))
        assertEquals(PushDestination.Notifications(null, null), dest)
    }

    @Test
    fun `announcement_id opens announcement`() {
        val dest = PushPayloadRouter.route(mapOf("announcement_id" to "555"))
        assertEquals(PushDestination.Announcement(555), dest)
    }

    @Test
    fun `notification_id with type opens notifications by type`() {
        val dest = PushPayloadRouter.route(
            mapOf("notification_id" to "9", "notification_type" to "news"),
        )
        assertEquals(PushDestination.Notifications(notificationId = 9, type = "news"), dest)
    }

    @Test
    fun `notification_id without type falls back to notification list`() {
        val dest = PushPayloadRouter.route(mapOf("notification_id" to "9"))
        assertEquals(PushDestination.Notifications(null, null), dest)
    }

    @Test
    fun `empty payload falls back to notification list`() {
        val dest = PushPayloadRouter.route(emptyMap())
        assertEquals(PushDestination.Notifications(null, null), dest)
    }

    @Test
    fun `unknown payload falls back to notification list`() {
        val dest = PushPayloadRouter.route(mapOf("title" to "Сәлем", "body" to "Хабарлама"))
        assertEquals(PushDestination.Notifications(null, null), dest)
    }

    @Test
    fun `payload id is built from sorted keys regardless of insertion order`() {
        val a = PushPayloadRouter.payloadId(
            linkedMapOf("b" to "2", "a" to "1", "c" to "3"),
        )
        val b = PushPayloadRouter.payloadId(
            linkedMapOf("c" to "3", "b" to "2", "a" to "1"),
        )
        assertEquals("a1b2c3", a)
        assertEquals(a, b)
    }

    @Test
    fun `different payloads produce different ids`() {
        val a = PushPayloadRouter.payloadId(mapOf("order_id" to "1"))
        val b = PushPayloadRouter.payloadId(mapOf("order_id" to "2"))
        assertTrue(a != b)
    }
}