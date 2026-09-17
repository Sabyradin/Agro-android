package com.agroland.feature.chat.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Чат моделдері (Flutter ChatRoomModel / ChatMessageModel) — кешірімді
 * парсерлермен: backend типтері тұрақсыз (int/string/null аралас, CR/LF
 * бұзылған URL-дер), сондықтан барлық оқу JsonParser/ChatParser арқылы.
 */

/** URL-дегі literal CR/LF/TAB тазарту (backend upload-flow «жасыл блок» репорты). */
fun sanitizeUrl(url: String?): String? {
    if (url == null) return null
    val cleaned = url.trim().replace(Regex("[\\r\\n\\t]+"), "")
    return cleaned
}

/** Backend уақыты (ISO 8601) → epoch millis; оқылмаса — қазір. */
fun parseTime(raw: String?): Long = try {
    if (raw.isNullOrBlank()) System.currentTimeMillis() else java.time.Instant.parse(raw).toEpochMilli()
} catch (_: Exception) {
    try {
        java.time.OffsetDateTime.parse(raw).toInstant().toEpochMilli()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}

/** Nullable уақыт — оқылмаса null (last_online үшін). */
fun parseTimeOrNull(raw: String?): Long? = try {
    if (raw.isNullOrBlank()) null else java.time.Instant.parse(raw).toEpochMilli()
} catch (_: Exception) {
    try {
        java.time.OffsetDateTime.parse(raw).toInstant().toEpochMilli()
    } catch (_: Exception) {
        null
    }
}

/** Чат тізімі элементі (GET /chat/ + socket push) — Flutter ChatRoomModel. */
data class ChatRoom(
    val roomId: Long,
    val otherUserId: Long?,
    val senderId: Long?,
    val receiverId: Long?,
    val senderName: String,
    val receiverName: String,
    val message: String,
    val isRead: Boolean,
    val unreadCount: Int,
    val timestamp: Long,
    val announcementTitle: String,
    val announcementStatus: String,
    val announcementAuthorId: Long,
    val announcementId: Long?,
    val announcementLink: String?,
    val chatStatus: String,
    val otherUserName: String?,
    val otherUserAvatarUrl: String?,
    val senderAvatarUrl: String?,
    val receiverAvatarUrl: String?,
    val isOnline: Boolean,
    val isChecked: Boolean,
    val lastOnline: Long?,
)

/** Хабарлама (socket push / join_chat_success history) — Flutter ChatMessageModel. */
data class ChatMessage(
    val id: Long?,
    val senderId: Long,
    val receiverId: Long,
    val senderName: String,
    val receiverName: String,
    val message: String,
    val messageType: String,
    val isRead: Boolean,
    val timestamp: Long,
    val senderAvatarUrl: String?,
    val receiverAvatarUrl: String?,
    val fileUrl: String?,
    val fileName: String?,
    val audioUrl: String?,
    val audioDuration: Int?,
    val latitude: Double?,
    val longitude: Double?,
    val locationName: String?,
    val announcement: ChatMessageAnnouncement?,
    val replyToId: Long?,
    val replyToMessage: String?,
    val editedAt: Long?,
    val isDeleted: Boolean,
    // Local-only өрістер (optimistic send)
    val sendError: Boolean = false,
    val localId: String? = null,
) {
    /** Optimistic хабарлама ма (сервер растауын күтетін)? */
    val isOptimistic: Boolean get() = localId != null || id == null

    fun copyForEdit(message: String, editedAt: Long?): ChatMessage = copy(message = message, editedAt = editedAt)
}

/** Хабарлама деңгейіндегі дайын тауар дерегі (Option A, backend `announcement`). */
data class ChatMessageAnnouncement(
    val id: Long,
    val title: String,
    val price: Double,
    val mainImageUrl: String,
)

/**
 * Socket push толық емес келуі мүмкін (DEV flapping) — REPLACE емес MERGE
 * (Flutter ChatRoomModel.mergeRooms): push-та бар → жаңартылғанын қоямыз,
 * push-та жоқ → сақтаймыз, жаңа → қосамыз. chatStatus өзгерсе тізім
 * арасында жылжиды (active ↔ archived).
 */
object ChatRoomMerger {

    data class Merged(val active: List<ChatRoom>, val archived: List<ChatRoom>)

    fun merge(
        incoming: List<ChatRoom>,
        active: List<ChatRoom>,
        archived: List<ChatRoom>,
    ): Merged {
        val incomingById = incoming.associateBy { it.roomId }
        val existingIds = (active.map { it.roomId } + archived.map { it.roomId }).toSet()
        val newActive = mutableListOf<ChatRoom>()
        val newArchived = mutableListOf<ChatRoom>()

        fun place(room: ChatRoom) {
            if (room.chatStatus.equals("ARCHIVED", ignoreCase = true)) {
                newArchived.add(room)
            } else {
                newActive.add(room)
            }
        }

        for (r in active) incomingById[r.roomId]?.let(::place) ?: run { newActive.add(r) }
        for (r in archived) incomingById[r.roomId]?.let(::place) ?: run { newArchived.add(r) }
        for (r in incoming) if (r.roomId !in existingIds) place(r)
        return Merged(newActive, newArchived)
    }
}

/** Толерантты парсерлер — backend пішімі тұрақсыз (spec §4). */
object ChatParser {

    private fun str(obj: JsonObject, key: String): String =
        com.agroland.core.network.json.JsonParser.string(obj, key) ?: ""

    private fun strOrNull(obj: JsonObject, key: String): String? {
        val el = obj[key] ?: return null
        // JSON null → JsonNull, оның content-і "null" жолы: UI-да «null» болып
        // шықпас үшін нақты null қайтарамыз (reply_to_message т.б.).
        if (el is kotlinx.serialization.json.JsonNull) return null
        return (el as? JsonPrimitive)?.content?.takeIf { it != "null" }
    }

    private fun longOrNull(obj: JsonObject, key: String): Long? =
        com.agroland.core.network.json.JsonParser.long(obj, key)

    private fun doubleOrNull(obj: JsonObject, key: String): Double? =
        com.agroland.core.network.json.JsonParser.double(obj, key)

    private fun intOrNull(obj: JsonObject, key: String): Int? =
        com.agroland.core.network.json.JsonParser.int(obj, key)

    private fun boolOrFalse(obj: JsonObject, key: String): Boolean =
        com.agroland.core.network.json.JsonParser.bool(obj, key) ?: false

    /** roomJson → ChatRoom; қатесе null (чат тізімнен жоғалмау үшін). */
    fun parseRoom(obj: JsonObject): ChatRoom? {
        return try {
            ChatRoom(
                roomId = longOrNull(obj, "room_id") ?: return null,
            otherUserId = longOrNull(obj, "other_user_id"),
            senderId = longOrNull(obj, "sender_id"),
            receiverId = longOrNull(obj, "receiver_id"),
            senderName = str(obj, "sender_name"),
            receiverName = str(obj, "receiver_name"),
            message = str(obj, "message"),
            isRead = boolOrFalse(obj, "is_read"),
            unreadCount = intOrNull(obj, "unread_count") ?: 0,
            timestamp = parseTime(strOrNull(obj, "timestamp")),
            announcementTitle = str(obj, "announcement_title"),
            announcementStatus = str(obj, "announcement_status"),
            announcementAuthorId = longOrNull(obj, "announcement_author_id") ?: 0L,
            announcementId = longOrNull(obj, "announcement_id"),
            announcementLink = strOrNull(obj, "announcement_link"),
            chatStatus = strOrNull(obj, "chat_status") ?: "ACTIVE",
            otherUserName = strOrNull(obj, "other_user_name"),
            otherUserAvatarUrl = sanitizeUrl(strOrNull(obj, "other_user_avatar_url")),
            senderAvatarUrl = sanitizeUrl(strOrNull(obj, "sender_avatar_url")),
            receiverAvatarUrl = sanitizeUrl(strOrNull(obj, "receiver_avatar_url")),
            isOnline = boolOrFalse(obj, "is_online"),
            isChecked = boolOrFalse(obj, "is_checked"),
            lastOnline = parseTimeOrNull(strOrNull(obj, "last_online")),
        )
        } catch (_: Exception) {
            null
        }
    }

    /** messageJson → ChatMessage; қатесе null. */
    fun parseMessage(obj: JsonObject): ChatMessage? {
        return try {
        ChatMessage(
            id = longOrNull(obj, "id"),
            senderId = longOrNull(obj, "sender_id") ?: 0L,
            receiverId = longOrNull(obj, "receiver_id") ?: 0L,
            senderName = str(obj, "sender_name"),
            receiverName = str(obj, "receiver_name"),
            message = str(obj, "message"),
            messageType = strOrNull(obj, "message_type") ?: "text",
            isRead = boolOrFalse(obj, "is_read"),
            timestamp = parseTime(strOrNull(obj, "timestamp")),
            senderAvatarUrl = sanitizeUrl(strOrNull(obj, "sender_avatar_url")),
            receiverAvatarUrl = sanitizeUrl(strOrNull(obj, "receiver_avatar_url")),
            fileUrl = sanitizeUrl(strOrNull(obj, "file_url")),
            fileName = strOrNull(obj, "file_name"),
            audioUrl = sanitizeUrl(strOrNull(obj, "audio_url")),
            audioDuration = intOrNull(obj, "audio_duration"),
            latitude = doubleOrNull(obj, "latitude"),
            longitude = doubleOrNull(obj, "longitude"),
            locationName = strOrNull(obj, "location_name"),
            announcement = (obj["announcement"] as? JsonObject)?.let { parseAnnouncement(it) },
            replyToId = longOrNull(obj, "reply_to_id"),
            replyToMessage = strOrNull(obj, "reply_to_message"),
            editedAt = parseTimeOrNull(strOrNull(obj, "edited_at")),
            isDeleted = boolOrFalse(obj, "is_deleted"),
        )
        } catch (_: Exception) {
            null
        }
    }

    fun parseAnnouncement(obj: JsonObject): ChatMessageAnnouncement? {
        return try {
            ChatMessageAnnouncement(
                id = longOrNull(obj, "id") ?: return null,
                title = str(obj, "title"),
                price = doubleOrNull(obj, "price")
                    ?: (obj["price"] as? JsonPrimitive)?.content?.toDoubleOrNull()
                    ?: 0.0,
                mainImageUrl = sanitizeUrl(strOrNull(obj, "main_image_url")).orEmpty(),
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * GET /chat/ жауабы: `{chats: [...]}` / `{data: {chats}}` / түбірде тізім.
     */
    fun parseRoomList(root: JsonElement?): List<ChatRoom> {
        val list: JsonArray = when (root) {
            is JsonArray -> root
            is JsonObject -> {
                val data = root["data"] as? JsonObject
                (root["chats"] as? JsonArray)
                    ?: (data?.get("chats") as? JsonArray)
                    ?: (root["data"] as? JsonArray)
                    ?: return emptyList()
            }
            else -> return emptyList()
        }
        return list.filterIsInstance<JsonObject>()
            .mapNotNull(::parseRoom)
    }

    fun parseMessageList(list: JsonElement?): List<ChatMessage> {
        val arr = list as? JsonArray ?: return emptyList()
        return arr.filterIsInstance<JsonObject>().mapNotNull(::parseMessage)
    }
}