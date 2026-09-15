package com.agroland.feature.push.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.agroland.core.l10n.R as L10nR

/**
 * Push каналдары (spec §11): чат, тапсырыс, қоңырау (full-screen), маркетинг.
 * FCM өзі көрсететін фондық notification-дар әдепкі каналға түседі — маркетинг.
 */
object PushChannels {

    const val CHAT = "agroland_chat"
    const val ORDERS = "agroland_orders"
    const val CALLS = "agroland_calls"
    const val MARKETING = "agroland_marketing"

    /** Каналдарды құрады (O+; 26-дан төменде канал ұғымы жоқ). Идемпотентті. */
    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val chat = NotificationChannel(
                CHAT,
                context.getString(L10nR.string.push_channel_chat),
                NotificationManager.IMPORTANCE_HIGH,
            )
            val orders = NotificationChannel(
                ORDERS,
                context.getString(L10nR.string.push_channel_orders),
                NotificationManager.IMPORTANCE_HIGH,
            )
            // Қоңырау — ең жоғары маңыздылық + full-screen intent (келген қоңырау
            // экранды ұстайды, spec §13-ке дайындық).
            val calls = NotificationChannel(
                CALLS,
                context.getString(L10nR.string.push_channel_calls),
                NotificationManager.IMPORTANCE_HIGH,
            )
            calls.lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            val marketing = NotificationChannel(
                MARKETING,
                context.getString(L10nR.string.push_channel_marketing),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            manager.createNotificationChannels(listOf(chat, orders, calls, marketing))
        }
        // FCM өзі шығаратын notification-дар (фонді күй) әдепкі каналы —
        // манифесттегі default_notification_channel_id meta-data (24.x API:
        // setDefaultNotificationChannelId жойылды). PushChannels.MARKETING.
    }

    /**
     * Payload → канал (Flutter-де бір ғана high_importance_channel болған;
     * spec §11 4 каналды талап етеді): чат → chat, қоңырау → calls,
     * тапсырыс/верификация/баланс → orders, қалғаны → marketing.
     */
    fun channelFor(data: Map<String, String>): String {
        val event = data["event"]
        return when {
            event == "chat" -> CHAT
            event == "call" || event?.startsWith("call_") == true || data.containsKey("call_id") -> CALLS
            event?.startsWith("verification") == true -> ORDERS
            event == "balance_credited" || event == "withdraw_approved" ||
                event == "withdraw_rejected" || event == "review_request" -> ORDERS
            data.containsKey("order_id") -> ORDERS
            else -> MARKETING
        }
    }
}