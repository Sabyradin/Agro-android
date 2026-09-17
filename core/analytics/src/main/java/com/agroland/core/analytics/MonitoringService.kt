package com.agroland.core.analytics

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.agroland.core.network.interceptors.ApiEventReporter
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * MonitoringService — Flutter monitoring_service.dart 1:1 (Фаза 19).
 * Fan-out: monitoring API (fire-and-forget) + TikTok + Firebase.
 *
 * Барлық track* әдісі өз SupervisorJob scope-ында fire-and-forget
 * жұмыс істейді — шақырушы ешқашан блокталмайды, monitoring cold start
 * (scale-to-zero Cloud Run) UI-ды тоқтата алмайды (Flutter bootstrap
 * комментарииндегі «pages don't open» сабағы). Сәтсіз жіберілген оқиға
 * PendingMonitoringEvents кешіне түседі (max 100), келесі сәтті
 * жіберу кезінде қайта жіберіледі (Flutter TODO-flush орнына нақты flush).
 */
@Singleton
class MonitoringService @Inject constructor(
    private val repository: MonitoringRepository,
    private val pendingEvents: PendingMonitoringEvents,
    private val deviceIdProvider: DeviceIdProvider,
    private val tikTok: TikTokAnalytics,
    private val firebase: FirebaseAnalyticsService,
    private val dataStore: DataStore<Preferences>,
) : ApiEventReporter {

    /** OkHttp ApiMonitoringInterceptor осы арқылы қосылды (core:network ↔ analytics). */
    override fun apiRequestSuccess(endpoint: String, httpMethod: String, statusCode: Int) =
        trackApiRequestSuccess(endpoint, httpMethod, statusCode)

    override fun apiRequestFailure(
        endpoint: String,
        httpMethod: String,
        statusCode: Int?,
        errorMessage: String?,
    ) = trackApiRequestFailure(endpoint, httpMethod, statusCode, errorMessage)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var userId: String? = null

    @Volatile
    private var phoneNumber: String? = null

    /** Auth кезінде орнатылады (UserNotifier parity) — TikTok identify fan-out. */
    fun setUserId(userId: String?) {
        this.userId = userId
        firebase.setUserId(userId)
        if (userId != null) {
            tikTok.identify(externalId = userId, phone = phoneNumber, email = null)
        }
    }

    fun setPhoneNumber(phone: String?) {
        phoneNumber = phone
        phone?.let { firebase.setUserProperty("phone_number_hash", it.hashCode().toString()) }
    }

    private fun createEvent(
        eventType: MonitoringEventType,
        eventName: String,
        data: Map<String, kotlinx.serialization.json.JsonElement>? = null,
        pageName: String? = null,
        searchQuery: String? = null,
        categoryId: String? = null,
        apiEndpoint: String? = null,
        httpMethod: String? = null,
        statusCode: Int? = null,
        errorMessage: String? = null,
        amount: Double? = null,
        currency: String? = null,
        phoneNumberOverride: String? = null,
        success: Boolean? = null,
        error: String? = null,
    ): MonitoringEvent = MonitoringEvent(
        eventType = eventType,
        eventName = eventName,
        data = data,
        timestamp = Instant.now().toString(),
        userId = userId,
        phoneNumber = phoneNumberOverride ?: phoneNumber ?: "",
        deviceId = deviceIdProvider.deviceId.ifEmpty { null },
        pageName = pageName,
        searchQuery = searchQuery,
        categoryId = categoryId,
        apiEndpoint = apiEndpoint,
        httpMethod = httpMethod,
        statusCode = statusCode,
        errorMessage = errorMessage,
        amount = amount,
        currency = currency,
        success = success,
        error = error,
    )

    private fun sendEvent(event: MonitoringEvent) {
        scope.launch {
            if (repository.sendEvent(event)) {
                flushPending()
            } else {
                savePending(event)
            }
        }
    }

    private suspend fun savePending(event: MonitoringEvent) {
        try {
            val current = pendingEvents.load()
            pendingEvents.save(trimPending(current + event, MAX_PENDING))
        } catch (_: Exception) {
        }
    }

    private suspend fun flushPending() {
        try {
            val pending = pendingEvents.load()
            if (pending.isEmpty()) return
            val remaining = pending.filterNot { repository.sendEvent(it) }
            if (remaining.isEmpty()) pendingEvents.clear() else pendingEvents.save(remaining)
        } catch (_: Exception) {
        }
    }

    // ── Оқиғалар (Flutter 1:1 атауларымен) ──

    fun trackPageView(pageName: String) {
        sendEvent(
            createEvent(MonitoringEventType.pageView, "page_view", pageName = pageName),
        )
        firebase.logScreenView(pageName)
    }

    fun trackSearch(query: String, categoryId: String? = null) {
        sendEvent(
            createEvent(MonitoringEventType.search, "search", searchQuery = query, categoryId = categoryId),
        )
        firebase.logEvent("search", mapOf("query" to query))
    }

    fun trackCategoryView(categoryId: String, categoryName: String? = null) {
        sendEvent(
            createEvent(
                MonitoringEventType.categoryView,
                "category_view",
                categoryId = categoryId,
                data = categoryName?.let {
                    mapOf("categoryName" to kotlinx.serialization.json.JsonPrimitive(it))
                },
            ),
        )
        firebase.logEvent("category_view", mapOf("category_id" to categoryId))
    }

    fun trackApiRequestSuccess(endpoint: String, httpMethod: String, statusCode: Int) {
        sendEvent(
            createEvent(
                MonitoringEventType.apiRequestSuccess,
                "api_request_success",
                apiEndpoint = endpoint,
                httpMethod = httpMethod,
                statusCode = statusCode,
            ),
        )
    }

    fun trackApiRequestFailure(
        endpoint: String,
        httpMethod: String,
        statusCode: Int? = null,
        errorMessage: String? = null,
    ) {
        sendEvent(
            createEvent(
                MonitoringEventType.apiRequestFailure,
                "api_request_failure",
                apiEndpoint = endpoint,
                httpMethod = httpMethod,
                statusCode = statusCode,
                errorMessage = errorMessage,
            ),
        )
    }

    fun trackLogin(phone: String, success: Boolean, errorMessage: String? = null) {
        sendEvent(
            createEvent(
                MonitoringEventType.login,
                "login",
                phoneNumberOverride = phone,
                success = success,
                error = errorMessage,
            ),
        )
        if (success) {
            phoneNumber = phone
            tikTok.trackEvent("login")
            firebase.logEvent("login", mapOf("success" to true))
        }
    }

    fun trackLogout() {
        sendEvent(createEvent(MonitoringEventType.logout, "logout"))
        userId = null
        tikTok.logout()
        firebase.setUserId(null)
    }

    fun trackRegistration(phone: String, success: Boolean, errorMessage: String? = null) {
        sendEvent(
            createEvent(
                MonitoringEventType.registration,
                "registration",
                phoneNumberOverride = phone,
                success = success,
                error = errorMessage,
            ),
        )
        if (success) {
            phoneNumber = phone
            tikTok.trackEvent("completeRegistration")
            firebase.logEvent("sign_up", mapOf("success" to true))
        }
    }

    /** Жарнама/хабарландыру жасау — Flutter data: {categoryId, subcategoryId}. */
    fun trackAdvertise(
        adId: String,
        success: Boolean,
        errorMessage: String? = null,
        data: Map<String, kotlinx.serialization.json.JsonElement>? = null,
    ) {
        sendEvent(
            createEvent(
                MonitoringEventType.advertise,
                "advertise",
                success = success,
                error = errorMessage,
                data = buildMap {
                    put("adId", kotlinx.serialization.json.JsonPrimitive(adId))
                    put("success", kotlinx.serialization.json.JsonPrimitive(success))
                    data?.forEach { (k, v) -> put(k, v) }
                    errorMessage?.let { put("error", kotlinx.serialization.json.JsonPrimitive(it)) }
                },
            ),
        )
    }

    fun trackBalanceTopUp(amount: Double, currency: String, success: Boolean, errorMessage: String? = null) {
        sendEvent(
            createEvent(
                MonitoringEventType.balanceTopUp,
                "balance_top_up",
                amount = amount,
                currency = currency,
                success = success,
                error = errorMessage,
            ),
        )
        firebase.logEvent(
            "balance_top_up",
            mapOf("amount" to amount, "currency" to currency, "success" to success),
        )
    }

    /**
     * Тапсырыс төлемі сәтті — TikTok `purchase` conversion (Flutter parity:
     * backend MonitoringEventType-та purchase жоқ, сондықтан тек TikTok-қа).
     */
    fun trackPurchase(orderId: String, amount: Double?, currency: String?) {
        val props = buildMap {
            put("orderId", kotlinx.serialization.json.JsonPrimitive(orderId))
            amount?.let { put("amount", kotlinx.serialization.json.JsonPrimitive(it)) }
            currency?.let { put("currency", kotlinx.serialization.json.JsonPrimitive(it)) }
        }
        tikTok.trackEvent("purchase", kotlinx.serialization.json.JsonObject(props), eventId = orderId)
        firebase.logEvent(
            "purchase",
            buildMap {
                put("transaction_id", orderId)
                amount?.let { put("value", it) }
                currency?.let { put("currency", it) }
            },
        )
    }

    fun trackAppOpen() {
        sendEvent(createEvent(MonitoringEventType.appOpen, "app_open"))
        tikTok.trackEvent("launchApp")
        firebase.logEvent("app_open", emptyMap())
        // Соңғы кіру уақытын сақтау (Flutter last_app_open_time parity).
        scope.launch {
            try {
                dataStore.edit { it[LAST_OPEN_KEY] = Instant.now().toString() }
            } catch (_: Exception) {
            }
        }
    }

    fun trackAppClose() {
        sendEvent(createEvent(MonitoringEventType.appClose, "app_close"))
    }

    private companion object {
        const val MAX_PENDING = 100
        val LAST_OPEN_KEY = stringPreferencesKey("last_app_open_time")
    }
}

/** Кеш шегі — таза функция (MonitoringServiceTest қапталады). */
fun trimPending(events: List<MonitoringEvent>, maxSize: Int): List<MonitoringEvent> =
    if (events.size <= maxSize) events else events.drop(events.size - maxSize)