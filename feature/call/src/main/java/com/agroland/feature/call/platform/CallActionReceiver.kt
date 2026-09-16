package com.agroland.feature.call.platform

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.agroland.feature.call.domain.VoiceCallManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Notification accept/decline кнопкалары (CallStyle / addAction) →
 * VoiceCallManager. Микрофон рұқсаты әлі берілмесе — Accept тек қоңырау
 * экранын ашады (рұқсат диалогы экранның өз Accept батырмасынан шығады;
 * Flutter-дегідей — экран бірінші, рұқсат кейін).
 */
@AndroidEntryPoint
class CallActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var callManager: VoiceCallManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ANSWER -> {
                val micGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                if (micGranted) {
                    callManager.accept(micGranted = true)
                } else {
                    // Рұқсат жоқ — экранды ашамыз, қолданушы Accept басқанда
                    // рұқсат диалогы шығады (pending-accept рөлі экранда).
                    openCallScreen(context)
                }
            }

            ACTION_DECLINE ->
                callManager.reject()
        }
    }

    companion object {
        /** Notification accept/decline PendingIntent әрекеттері. */
        const val ACTION_ANSWER = "com.agroland.app.call.ANSWER"
        const val ACTION_DECLINE = "com.agroland.app.call.DECLINE"
    }

    private fun openCallScreen(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        launch.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP,
        )
        context.startActivity(launch)
    }
}