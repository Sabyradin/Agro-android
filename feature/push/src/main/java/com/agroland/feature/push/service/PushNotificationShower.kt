package com.agroland.feature.push.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.agroland.core.l10n.R as L10nR
import com.agroland.feature.push.R

/**
 * Жергілікті push-хабарламаны көрсету (Flutter showAppNotification →
 * AwesomeNotifications.createNotification). Фонда көрсету ғана — автоматты
 * навигация ЖОҚ; навигация хабарлама БАСЫЛҒАНДА intent арқылы болады.
 *
 * PendingIntent — launch intent: қосымша MainActivity класына тәуелсіз
 * (feature модулі app-ты білмейді), payload extras-тан беріледі.
 */
object PushNotificationShower {

    /** Біздің хабарламаларымызды FCM system-tray intent-терінен айыратын маркер. */
    const val EXTRA_PUSH_MARKER = "agroland_push"

    fun show(context: Context, data: Map<String, String>, title: String?, body: String?) {
        if (title == null && body == null) return
        PushChannels.ensureCreated(context)

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val notificationId = data.hashCode()
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().apply { setPackage(context.packageName) }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(EXTRA_PUSH_MARKER, true)
        data.forEach { (key, value) -> intent.putExtra(key, value) }

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val channel = PushChannels.channelFor(data)
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_push_notification)
            .setContentTitle(title ?: context.getString(L10nR.string.app_name))
            .setContentText(body ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body ?: ""))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        // Қоңырау каналы — full-screen intent (spec §11/§13): келген қоңырау
        // экранды тікелей ұстайды.
        if (channel == PushChannels.CALLS) {
            builder.setCategory(NotificationCompat.CATEGORY_CALL)
            builder.setFullScreenIntent(contentIntent, true)
        } else {
            builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
        }

        try {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            manager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Рұқсат берілмеген — хабарлама жүктелмейді, ештеңе сыналмайды.
        }
    }
}