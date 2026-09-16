package com.agroland.feature.chat

import com.agroland.feature.chat.data.ChatParser
import com.agroland.feature.chat.data.ChatRoom
import com.agroland.feature.chat.data.ChatRoomMerger
import com.agroland.feature.chat.data.parseTime
import com.agroland.feature.chat.data.sanitizeUrl
import com.agroland.feature.chat.domain.parseRoomIdOrNull
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** ChatParser / ChatRoomMerger / sanitizeUrl / parseTime — кешірімді парсинг. */
class ChatModelsTest {

    private fun obj(json: String) =
        Json.parseToJsonElement(json).let { it as kotlinx.serialization.json.JsonObject }

    // ---- parseRoomId (payload пішімі тұрақсыз) ----

    @Test
    fun `parseRoomIdOrNull — 123, chat_123, бос аралықтар`() {
        assertEquals(123L, parseRoomIdOrNull("123"))
        assertEquals(123L, parseRoomIdOrNull("chat_123"))
        assertEquals(123L, parseRoomIdOrNull(" 123 "))
        assertNull(parseRoomIdOrNull("abc"))
        assertNull(parseRoomIdOrNull(""))
    }

    // ---- sanitizeUrl ----

    @Test
    fun `sanitizeUrl — CR LF TAB тазарту`() {
        assertEquals("https://a.kz/x.jpg", sanitizeUrl("https://a.kz/x.jpg\r\n"))
        assertEquals("https://a.kz/x.jpg", sanitizeUrl(" https://a.kz/x.jpg\t"))
        assertNull(sanitizeUrl(null))
    }

    // ---- parseTime ----

    @Test
    fun `parseTime — ISO 8601 және OffsetDateTime`() {
        val iso = parseTime("2026-09-15T10:00:00Z")
        assertTrue(iso > 0)
        val offset = parseTime("2026-09-15T10:00:00+06:00")
        assertTrue(offset > 0)
        // Бос/жарамсыз — қазір (жарылмайды).
        assertTrue(parseTime(null) > 0)
        assertTrue(parseTime("not-a-date") > 0)
    }

    // ---- ChatParser.parseRoom ----

    @Test
    fun `parseRoom — толық пішім әрі тип араласуына төзімді`() {
        val room = ChatParser.parseRoom(
            obj(
                """
                {"room_id": "42", "other_user_id": 7, "sender_id": 1,
                 "sender_name": "A", "receiver_name": "B",
                 "message": "salam", "is_read": 1, "unread_count": "3",
                 "timestamp": "2026-09-15T10:00:00Z",
                 "announcement_title": "Tractor", "announcement_status": "ACTIVE",
                 "announcement_author_id": 5, "announcement_id": 9,
                 "chat_status": "ARCHIVED", "other_user_name": "Dealer",
                 "is_online": true, "last_online": null}
                """.trimIndent(),
            ),
        )
        assertNotNull(room)
        assertEquals(42L, room!!.roomId)
        assertEquals(7L, room.otherUserId)
        assertEquals(true, room.isRead)
        assertEquals(3, room.unreadCount)
        assertEquals("ARCHIVED", room.chatStatus)
        assertEquals(true, room.isOnline)
        assertNull(room.lastOnline)
    }

    @Test
    fun `parseRoom — room_id жоқ болса null (тізімнен жоғалмайды)`() {
        assertNull(ChatParser.parseRoom(obj("""{"sender_name": "A"}""")))
    }

    @Test
    fun `parseRoom — толығымен бос пішімде қақпау`() {
        val room = ChatParser.parseRoom(obj("""{"room_id": 1, "sender_id": null}"""))
        assertNotNull(room)
        assertEquals(1L, room!!.roomId)
        assertNull(room.senderId)
        assertEquals("", room.senderName)
        assertEquals(0, room.unreadCount)
    }

    // ---- ChatParser.parseMessage ----

    @Test
    fun `parseMessage — announcement ішкі объектімен`() {
        val msg = ChatParser.parseMessage(
            obj(
                """
                {"id": 5, "sender_id": 1, "receiver_id": 2,
                 "sender_name": "A", "receiver_name": "B",
                 "message": "hi", "message_type": "text", "is_read": false,
                 "timestamp": "2026-09-15T10:00:00Z",
                 "file_url": null, "latitude": 43.2, "longitude": 76.9,
                 "announcement": {"id": 3, "title": "Tractor", "price": 1000,
                                   "main_image_url": "https://a.kz/i.jpg"}}
                """.trimIndent(),
            ),
        )
        assertNotNull(msg)
        assertEquals(43.2, msg!!.latitude!!, 0.001)
        assertNotNull(msg.announcement)
        assertEquals(3L, msg.announcement!!.id)
        assertEquals(1000.0, msg.announcement!!.price, 0.001)
        assertEquals(1L, msg.senderId)
    }

    @Test
    fun `parseMessage — price жол ретінде келсе де оқылады`() {
        val msg = ChatParser.parseMessage(
            obj(
                """
                {"id": 5, "sender_id": 1, "receiver_id": 2,
                 "sender_name": "A", "receiver_name": "B", "message": "x",
                 "timestamp": "2026-09-15T10:00:00Z",
                 "announcement": {"id": 3, "title": "T", "price": "1500", "main_image_url": null}}
                """.trimIndent(),
            ),
        )
        assertNotNull(msg)
        assertEquals(1500.0, msg!!.announcement!!.price, 0.001)
    }

    // ---- ChatRoomMerger (push толық емес келуі мүмкін — MERGE) ----

    private fun room(id: Long, chatStatus: String = "ACTIVE", unread: Int = 0) = ChatRoom(
        roomId = id,
        otherUserId = id + 100,
        senderId = 1,
        receiverId = id + 100,
        senderName = "A",
        receiverName = "B",
        message = "m",
        isRead = false,
        unreadCount = unread,
        timestamp = 0L,
        announcementTitle = "",
        announcementStatus = "",
        announcementAuthorId = 0,
        announcementId = null,
        announcementLink = null,
        chatStatus = chatStatus,
        otherUserName = null,
        otherUserAvatarUrl = null,
        senderAvatarUrl = null,
        receiverAvatarUrl = null,
        isOnline = false,
        isChecked = false,
        lastOnline = null,
    )

    @Test
    fun `merge — push жаңартады, жоғалтпайды, жаңаны қосады`() {
        val active = listOf(room(1, unread = 1), room(2))
        val archived = listOf(room(3, chatStatus = "ARCHIVED"))

        // push: room 1 жаңартылады (unread → 5), room 99 жаңа.
        val push = listOf(room(1, unread = 5), room(99))
        val merged = ChatRoomMerger.merge(push, active, archived)

        assertEquals(listOf(1L, 2L, 99L), merged.active.map { it.roomId })
        assertEquals(listOf(3L), merged.archived.map { it.roomId })
        assertEquals(5, merged.active.first { it.roomId == 1L }.unreadCount)
    }

    @Test
    fun `merge — chatStatus өзгерсе аралықта жылжиды (ACTIVE → ARCHIVED)`() {
        val active = listOf(room(1))
        val push = listOf(room(1, chatStatus = "ARCHIVED"))
        val merged = ChatRoomMerger.merge(push, active, emptyList())
        assertEquals(emptyList<Long>(), merged.active.map { it.roomId })
        assertEquals(listOf(1L), merged.archived.map { it.roomId })
    }

    @Test
    fun `parseRoomList — chats, data-chats, түбірлік тізім`() {
        assertEquals(1, ChatParser.parseRoomList(obj("""{"chats": [{"room_id": 1}]}""")).size)
        assertEquals(
            1,
            ChatParser.parseRoomList(obj("""{"data": {"chats": [{"room_id": 1}]}}""")).size,
        )
        assertEquals(
            1,
            ChatParser.parseRoomList(
                Json.parseToJsonElement("""[{"room_id": 1}]"""),
            ).size,
        )
        assertEquals(0, ChatParser.parseRoomList(obj("""{"data": 5}""")).size)
    }
}