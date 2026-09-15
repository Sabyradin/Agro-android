package com.agroland.feature.push.domain

/**
 * Push payload → навигация межесі (Flutter _handlePayload тармақтауы).
 * ChatRoom мен Notifications межелері Фаза 12-де (чат + хабарламалар тізімі
 * экрандары) қосылатын маршруттарға байланысты — әзірге MainActivity оларды
 * күтілуде деп белгілейді (ISSUES.md #24).
 */
sealed interface PushDestination {

    /** verification_approved / verification_rejected → KYC статусы. */
    data object Verification : PushDestination

    /** balance_credited / withdraw_approved / withdraw_rejected → әмиян. */
    data object Balance : PushDestination

    /** Тапсырыс туралы хабарлама → тапсырыс деталы (review_request қоса). */
    data class OrderDetail(val orderId: Long) : PushDestination

    /** Чат хабарламасы → сол бөлме (Фаза 12 экраны). */
    data class ChatRoom(
        val senderId: Long,
        val senderName: String?,
        val roomId: Long?,
        val isSystemChat: Boolean,
    ) : PushDestination

    /** Жарнама туралы → деталь беті. */
    data class Announcement(val announcementId: Long) : PushDestination

    /** Жүйелік хабарлама → хабарламалар тізімі (Фаза 12 экраны). */
    data class Notifications(
        val notificationId: Long?,
        val type: String?,
    ) : PushDestination
}

/**
 * Payload талдауышы — ТАЗА функция (бірлік-тесттермен қамтылған).
 * Flutter push_notification_service._handlePayload дәйек реті:
 * verification → balance → review_request+order_id → order_id → chat →
 * announcement → notification_id+type → fallback (хабарламалар тізімі).
 */
object PushPayloadRouter {

    fun route(data: Map<String, String>): PushDestination {
        val event = data["event"]

        // Platform-features (Verification): мақұлдау/қайта қарау → статус беті.
        if (event == "verification_approved" || event == "verification_rejected") {
            return PushDestination.Verification
        }

        // Platform-features (Dealer balance): есептеу / шығару нәтижесі.
        if (event == "balance_credited" ||
            event == "withdraw_approved" ||
            event == "withdraw_rejected"
        ) {
            return PushDestination.Balance
        }

        // Seller review: жеткізуден кейінгі бағалау сұрауы → тапсырыс деталы
        // (авто-ашылатын бағалау парағымен, Фаза 18).
        if (event == "review_request") {
            data["order_id"]?.toLongOrNull()?.let { return PushDestination.OrderDetail(it) }
        }

        // Тапсырыс хабарламалары → OrderDetail.
        data["order_id"]?.toLongOrNull()?.let { return PushDestination.OrderDetail(it) }

        // Чат: тізімге емес, СОЛ бөлмеге өтеміз (sender_id join арқылы).
        if (event == "chat") {
            data["sender_id"]?.toLongOrNull()?.let { senderId ->
                return PushDestination.ChatRoom(
                    senderId = senderId,
                    senderName = data["sender_name"],
                    roomId = data["room_id"]?.toLongOrNull()
                        ?: data["chat_id"]?.toLongOrNull(),
                    isSystemChat = data["is_system_chat"] == "true",
                )
            }
        }

        // Жарнама → деталь.
        data["announcement_id"]?.toLongOrNull()?.let { return PushDestination.Announcement(it) }

        // Жүйелік хабарлама → тиісті тип тізімі.
        if (data.containsKey("notification_id") && data.containsKey("notification_type")) {
            return PushDestination.Notifications(
                notificationId = data["notification_id"]?.toLongOrNull(),
                type = data["notification_type"],
            )
        }

        // Fallback — жалпы хабарламалар тізімі.
        return PushDestination.Notifications(notificationId = null, type = null)
    }

    /** Payload-тан бірегей id (Flutter _generateIdFromPayload: кілттер сортталған). */
    fun payloadId(data: Map<String, String>): String {
        val buffer = StringBuilder()
        for (key in data.keys.sorted()) {
            buffer.append(key).append(data[key])
        }
        return buffer.toString()
    }
}

/**
 * Қайталанған push-навигация қорғаны (Flutter _isDuplicateNotification):
 * бірдей payload id 500 мс ішінде келсе — ештеңе жасалмайды.
 */
class DuplicateGuard(
    private val cooldownMs: Long = NAVIGATION_COOLDOWN_MS,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var lastId: String? = null
    private var lastTime = 0L

    fun isDuplicate(id: String): Boolean {
        val time = now()
        if (id == lastId && time - lastTime < cooldownMs) return true
        lastId = id
        lastTime = time
        return false
    }

    companion object {
        const val NAVIGATION_COOLDOWN_MS = 500L
    }
}