package com.agroland.feature.call.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.agroland.core.l10n.R as L10nR
import com.agroland.feature.call.R
import com.agroland.feature.call.domain.VoiceCallState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Кіріс қоңырауды жүйеге хабарлау (Flutter-де бұл FCM push + VoiceCallHost
 * overlay болған; Android портында — CallStyle notification + full-screen
 * intent + self-managed Telecom, spec §13).
 *
 * Дыбыс (ringtone) осында ЖОҚ — оны VoiceCallScreen MediaPlayer-мен ойнатады
 * (алдыңғы жоспарда екі дыбыс қатар жүрмеуі керек). Notification — фонда
 * болғанда heads-up/full-screen ретінде.
 */
@Singleton
class IncomingCallNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val telecomManager: TelecomManager
        get() = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

    fun showIncomingCall(state: VoiceCallState) {
        ensureChannel()
        registerTelecomIncomingCall()
        if (!areNotificationsEnabled()) return
        val notification = buildNotification(state)
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS берілмеген — full-screen intent те шықпайды;
            // қоңырау экраны (overlay) әлі де көрінеді.
        }
    }

    fun cancel() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        AgroCallConnectionService.reportSystemEnded()
    }

    // ── Notification ──────────────────────────────────────────────────

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(L10nR.string.call_notification_channel),
                NotificationManager.IMPORTANCE_HIGH,
            )
            channel.lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun areNotificationsEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun buildNotification(state: VoiceCallState): android.app.Notification {
        val callerName = state.peerName?.takeIf { it.isNotBlank() }
            ?: context.getString(L10nR.string.call_incoming_title)

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent().apply { setPackage(context.packageName) }
        launch.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP,
        )

        val contentPending = PendingIntent.getActivity(
            context,
            REQUEST_CONTENT,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_notification)
            .setContentTitle(callerName)
            .setContentText(context.getString(L10nR.string.call_incoming_title))
            .setContentIntent(contentPending)
            .setFullScreenIntent(contentPending, true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: CallStyle — жүйелік қоңырау түрі (accept/decline кнопкалары).
            val caller = Person.Builder()
                .setName(callerName)
                .setImportant(true)
                .build()
            builder.setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    caller,
                    actionPending(CallActionReceiver.ACTION_DECLINE, REQUEST_DECLINE),
                    actionPending(CallActionReceiver.ACTION_ANSWER, REQUEST_ANSWER),
                ),
            )
        } else {
            builder.addAction(
                NotificationCompat.Action.Builder(
                    null,
                    context.getString(L10nR.string.call_decline),
                    actionPending(CallActionReceiver.ACTION_DECLINE, REQUEST_DECLINE),
                ).build(),
            )
            builder.addAction(
                NotificationCompat.Action.Builder(
                    null,
                    context.getString(L10nR.string.call_accept),
                    actionPending(CallActionReceiver.ACTION_ANSWER, REQUEST_ANSWER),
                ).build(),
            )
        }
        return builder.build()
    }

    private fun actionPending(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, CallActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    // ── Telecom (self-managed) ────────────────────────────────────────

    /**
     * Self-managed ConnectionService: жүйе қоңырау күйін біледі, аудио фокусы/
     * экранды ұстау оңайлаяды. Құрылғы/эмулятор қолдамаса — silent degradation,
     * notification өзі жеткілікті.
     */
    private fun registerTelecomIncomingCall() {
        try {
            val handle = PhoneAccountHandle(
                ComponentName(context, AgroCallConnectionService::class.java),
                PHONE_ACCOUNT_ID,
            )
            val account = PhoneAccount.builder(
                handle,
                context.getString(L10nR.string.call_notification_channel),
            )
                .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
                .build()
            telecomManager.registerPhoneAccount(account)
            if (telecomManager.getPhoneAccount(handle) != null) {
                telecomManager.addNewIncomingCall(handle, Bundle())
            }
        } catch (_: Throwable) {
            // Telecom қолжетімсіз — деградация, қоңырау жұмысын тоқтатпайды.
        }
    }

    companion object {
        /** PushChannels.CALLS дегенмен бірдей id — бір канал қайта құрылмайды. */
        private const val CHANNEL_ID = "agroland_calls"
        private const val NOTIFICATION_ID = 4213
        private const val PHONE_ACCOUNT_ID = "agroland_voice"

        private const val REQUEST_CONTENT = 42130
        private const val REQUEST_ANSWER = 42131
        private const val REQUEST_DECLINE = 42132
    }
}