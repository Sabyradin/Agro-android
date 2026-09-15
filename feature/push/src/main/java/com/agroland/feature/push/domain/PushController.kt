package com.agroland.feature.push.domain

import com.agroland.core.network.ApiResult
import com.agroland.feature.push.data.PushRepository
import com.agroland.feature.push.data.PushRequests
import com.agroland.feature.push.data.PushTokenStore
import com.agroland.feature.push.device.DeviceDetailsProvider
import com.google.firebase.messaging.FirebaseMessaging
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Push оркестраторы (Flutter PushNotificationService):
 *  - FCM token алу (бос/жоқ токен ЕШҚАШАН POST етілмейді — backend
 *    EMPTY_FIREBASE_TOKEN-ді қабылдамайды) + сәтті жіберілген токен dedup;
 *  - тіл өзгерісінде кэштелген токенмен қайта тіркеу (dedup айналып өту);
 *  - шығу кезінде DELETE /device/{device_id};
 *  - push payload → [PushDestination] + 500 мс қайталау қорғаны;
 *  - өлі күй реплейі: [pendingDestination] UI дайын болғанда бір рет орындалады.
 *
 * Барлық FCM шақыруы қорғалған: плейсхолдер google-services.json жағдайында
 * push мүлде үнсіз (Flutter setupFlutterNotifications try/catch үлгісі).
 */
@Singleton
class PushController @Inject constructor(
    private val repository: PushRepository,
    private val tokenStore: PushTokenStore,
    private val deviceDetails: DeviceDetailsProvider,
) {
    private val duplicateGuard = DuplicateGuard()

    // onNewToken қызметтен (suspend емес контекст) келеді — өз scope-ымыз бар.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _pendingDestination = MutableStateFlow<PushDestination?>(null)

    /** Өлі күйден келген push межесі — UI бір навигация жасап consumePending() шақырады. */
    val pendingDestination: StateFlow<PushDestination?> = _pendingDestination

    /**
     * Кірістірілгенде шақырылады (сессия Authorized болғанда).
     * Токен алынбаса/бос болса — ештеңе жіберілмейді, келесі ашылуда қайта далпын.
     */
    suspend fun ensureRegistered(language: String) {
        val token = fetchFirebaseToken() ?: return
        registerToken(token, language)
    }

    /**
     * Тіл өзгергенде (Flutter reRegisterLanguage): token dedup мұны өткізіп
     * жіберетінінен тікелей POST — кэштелген токен + ЖАҢА тіл.
     */
    suspend fun onLanguageChanged(language: String) {
        val cached = tokenStore.sentToken ?: return
        if (tokenStore.sentLanguage == language) return
        doRegister(cached, language)
    }

    /** FCM токені жаңарғанда (dedup кэшін ыдыратып, қайта тіркеу). */
    fun onNewToken(token: String) {
        if (token.isBlank()) return
        scope.launch {
            try {
                doRegister(token, tokenStore.sentLanguage ?: DEFAULT_LANGUAGE)
            } catch (_: Exception) {
                // Push ешқашан қолданушыға қате көрсетпейді.
            }
        }
    }

    /** Шығу: DELETE /device/{device_id} (401 → сәтті, репозиторийде). */
    suspend fun unregister() {
        try {
            repository.unregister(deviceDetails.deviceId)
            tokenStore.clearSent()
        } catch (_: Exception) {
        }
    }

    /**
     * Push түймесі/хабарламасы басылғанда (MainActivity intent-інен):
     * бос payload → хабарламалар тізімі fallback (Flutter _onPushClick).
     */
    fun handlePushData(data: Map<String, String>) {
        if (data.isEmpty()) {
            _pendingDestination.value = PushDestination.Notifications(null, null)
            return
        }
        val id = PushPayloadRouter.payloadId(data)
        if (duplicateGuard.isDuplicate(id)) return
        _pendingDestination.value = PushPayloadRouter.route(data)
    }

    /** UI навигацияны орындап болғанда күйді тазартады. */
    fun consumePending() {
        _pendingDestination.value = null
    }

    private suspend fun registerToken(token: String, language: String) {
        try {
            // Бос токен жіберілмейді + сәтті жіберілген токен dedup.
            if (token.isBlank()) return
            if (tokenStore.sentToken == token) return
            doRegister(token, language)
        } catch (_: Exception) {
        }
    }

    private suspend fun doRegister(token: String, language: String) {
        val body = PushRequests.deviceRegistration(
            deviceId = deviceDetails.deviceId,
            firebaseToken = token,
            osVersion = deviceDetails.osVersion,
            appVersion = deviceDetails.appVersion,
            deviceModel = deviceDetails.deviceModel,
            language = language,
            timezone = TimeZone.getDefault().id,
        )
        when (repository.register(body)) {
            is ApiResult.Success -> tokenStore.save(token, language)
            // Сәтсіз POST → кэштелген токен өшіріледі (Flutter failure → prefs.remove).
            is ApiResult.Error -> tokenStore.clearSent()
        }
    }

    /**
     * FCM токенін алу — қауіпсіз (Flutter _getFcmTokenSafely): плейсхолдер
     * конфигта task сәтсіз аяқталуы мүмкін → null, ешқандай қате сыртқа шықпайды.
     */
    private suspend fun fetchFirebaseToken(): String? = try {
        withTimeoutOrNull(TOKEN_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                try {
                    FirebaseMessaging.getInstance().token
                        .addOnSuccessListener { token -> cont.resume(token) }
                        .addOnFailureListener { cont.resume(null) }
                        .addOnCanceledListener { cont.resume(null) }
                } catch (e: Exception) {
                    cont.resume(null)
                }
            }
        }?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val TOKEN_TIMEOUT_MS = 15_000L
        const val DEFAULT_LANGUAGE = "kk"
    }
}