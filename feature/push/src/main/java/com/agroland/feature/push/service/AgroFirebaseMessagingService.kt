package com.agroland.feature.push.service

import com.agroland.feature.push.domain.PushController
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * FCM қызметі (Flutter FirebaseMessaging.onMessage / onTokenRefresh):
 *  - onMessageReceived (foreground + барлық data-хабарламалар) → жергілікті
 *    notification көрсету ғана, автоматты навигация ЖОҚ;
 *  - onNewToken → PushController.onNewToken (қайта тіркеу).
 */
@AndroidEntryPoint
class AgroFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var controller: PushController

    override fun onMessageReceived(message: RemoteMessage) {
        try {
            val data = message.data
            val title = message.notification?.title ?: data["title"]
            val body = message.notification?.body ?: data["body"]
            // Деректері жоқ хабарлама — көрсететін ештеңе жоқ (Flutter:
            // notification != null ғана createNotification жасайды).
            if (title != null || body != null) {
                PushNotificationShower.show(this, data, title, body)
            }
        } catch (_: Exception) {
            // Push қатесі ешқашан қосымшаны құлатпауы керек.
        }
    }

    override fun onNewToken(token: String) {
        try {
            controller.onNewToken(token)
        } catch (_: Exception) {
        }
    }
}