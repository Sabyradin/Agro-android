package com.agroland.feature.payment.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Halyk картасымен төлеу үшін «үздік төлем» id-інің персистентті сақтауы
 * (Flutter PaymentStorage, SharedPreferences → DataStore). WebView ашылмай
 * тұрып сақталады; қосымша суық старт болса нәтиже беті қалпына келтіреді.
 */
class PendingPaymentStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    suspend fun savePendingOrderId(orderId: Long) {
        dataStore.edit { it[ORDER_ID] = orderId }
    }

    suspend fun pendingOrderIdOnce(): Long? = dataStore.data.first()[ORDER_ID]

    suspend fun clearPendingOrderId() {
        dataStore.edit { it.remove(ORDER_ID) }
    }

    private companion object {
        val ORDER_ID = longPreferencesKey("pending_payment_order_id")
    }
}