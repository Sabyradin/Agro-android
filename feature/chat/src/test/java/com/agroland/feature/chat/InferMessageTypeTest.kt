package com.agroland.feature.chat

import com.agroland.feature.chat.data.ChatMessage
import com.agroland.feature.chat.domain.InferMessageType
import com.agroland.feature.chat.domain.InferredMessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** InferMessageType — CHAT_MESSAGE_RENDER_SPEC §2 шешу тәртібі. */
class InferMessageTypeTest {

    private fun message(
        messageType: String = "text",
        text: String = "",
        fileUrl: String? = null,
        audioUrl: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
    ) = ChatMessage(
        id = 1L,
        senderId = 2L,
        receiverId = 3L,
        senderName = "a",
        receiverName = "b",
        message = text,
        messageType = messageType,
        isRead = false,
        timestamp = 0L,
        senderAvatarUrl = null,
        receiverAvatarUrl = null,
        fileUrl = fileUrl,
        fileName = null,
        audioUrl = audioUrl,
        audioDuration = null,
        latitude = latitude,
        longitude = longitude,
        locationName = null,
        announcement = null,
        replyToId = null,
        replyToMessage = null,
        editedAt = null,
        isDeleted = false,
    )

    @Test
    fun `backend нақты типі text-тен басқа болса — соны сыйлаймыз`() {
        assertEquals(
            InferredMessageType.IMAGE,
            InferMessageType.infer(message(messageType = "image"), ""),
        )
        assertEquals(
            InferredMessageType.AUDIO,
            InferMessageType.infer(message(messageType = "audio"), ""),
        )
        assertEquals(
            InferredMessageType.VIDEO,
            InferMessageType.infer(message(messageType = "video"), ""),
        )
        assertEquals(
            InferredMessageType.LOCATION,
            InferMessageType.infer(message(messageType = "location"), ""),
        )
    }

    @Test
    fun `координат өрістері — message_type text болса да location`() {
        assertEquals(
            InferredMessageType.LOCATION,
            InferMessageType.infer(message(latitude = 43.2, longitude = 76.9), "hello"),
        )
    }

    @Test
    fun `мәтіндегі lat-lng жұбы — location`() {
        assertEquals(
            InferredMessageType.LOCATION,
            InferMessageType.infer(message(), "43.238, 76.889"),
        )
    }

    @Test
    fun `extension бойынша image`() {
        assertEquals(
            InferredMessageType.IMAGE,
            InferMessageType.infer(message(text = "https://cdn.kz/photo.jpg"), ""),
        )
    }

    @Test
    fun `extension жоқ URL — path substring бойынша`() {
        assertEquals(
            InferredMessageType.IMAGE,
            InferMessageType.infer(message(text = "https://cdn.kz/image/123"), ""),
        )
        assertEquals(
            InferredMessageType.AUDIO,
            InferMessageType.infer(message(text = "https://cdn.kz/voice/123"), ""),
        )
    }

    @Test
    fun `message бос — file_url кандидаты қаралады`() {
        assertEquals(
            InferredMessageType.FILE,
            InferMessageType.infer(
                message(fileUrl = "https://cdn.kz/doc.pdf"),
                "",
            ),
        )
    }

    @Test
    fun `танылмаған URL — text емес null (infer — TEXT fallback)`() {
        // URL-like, бірақ белгісіз path → infer TEXT (5-қадам fallback).
        assertEquals(
            InferredMessageType.TEXT,
            InferMessageType.infer(message(text = "https://cdn.kz/unknown/1"), ""),
        )
    }

    @Test
    fun `жай мәтін — TEXT`() {
        assertEquals(InferredMessageType.TEXT, InferMessageType.infer(message(), "Сәлем!"))
    }

    @Test
    fun `inferFromUrl — танылмаған пішім null`() {
        assertNull(InferMessageType.inferFromUrl("https://cdn.kz/unknown/1"))
        assertNull(InferMessageType.inferFromUrl("photo.jpg")) // URL-like емес
    }

    @Test
    fun `query-string пен fragment қиылады`() {
        assertEquals(
            InferredMessageType.IMAGE,
            InferMessageType.inferFromUrl("https://cdn.kz/a.PNG?w=100#top"),
        )
    }

    @Test
    fun `mediaUrlFor — бірінші бос емес кандидат`() {
        assertEquals(
            "https://cdn.kz/f.pdf",
            InferMessageType.mediaUrlFor(message(fileUrl = "https://cdn.kz/f.pdf", text = "x")),
        )
        assertEquals("x", InferMessageType.mediaUrlFor(message(text = "x")))
        assertNull(InferMessageType.mediaUrlFor(message()))
    }

    @Test
    fun `pathExtension`() {
        assertEquals("jpg", InferMessageType.pathExtension("https://a.kz/b/c.jpg?x=1"))
        assertNull(InferMessageType.pathExtension("https://a.kz/b/c"))
        assertNull(InferMessageType.pathExtension("https://a.kz/b/c."))
    }
}