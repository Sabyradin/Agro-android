package com.agroland.app.appversion

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.agroland.core.network.ApiResult
import com.agroland.core.network.NetworkModule.safeCall
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Force-update тексерушісі (Фаза 20) — Flutter AppVersionService.checkAndShowUpdateDialog
 * 1:1. 24 сағаттық throttle (forceCheck өткізіп жібереді), last_shown_version
 * дедупликациясы, шешім — [decideUpdate] таза функциясында.
 *
 * Flutter сияқты БҮР ҚАТЕ лақтырмайды: версияхана жетімсіз болса қосымша
 * әдеттегідей жұмыс істей береді (checkAndShowUpdateDialog try/catch қаптамасы).
 */
@Singleton
class AppVersionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: AppVersionApi,
    private val dataStore: DataStore<Preferences>,
) {

    /** Диалог көрсетілуі керек болса — шешім; әйтпесе null. */
    suspend fun check(forceCheck: Boolean): UpdateDecision? {
        try {
            // 1. Троттлинг: 24 сағатта бір рет (Flutter _lastVersionCheckKey).
            if (!forceCheck && isThrottled()) return null

            val currentVersion = currentAppVersion()

            // 2. Серверден модель (public эндпоинт).
            val model = when (
                val result = safeCall { api.getAppVersion(PLATFORM, currentVersion) }
            ) {
                is ApiResult.Success -> AppVersionParser.parse(result.value) ?: return null
                else -> return null
            }

            // 3. Тексеру уақытын сақтау (сәтті/сәтсіз болсын — Flutter parity).
            saveLastCheckTime()

            // 4. Шешім (dedupe — last_shown_version).
            val lastShown = dataStore.data.map { it[LAST_SHOWN_KEY] }.first()
            val decision = decideUpdate(currentVersion, model, lastShown) ?: return null

            // 5. Көрсетілген нұсқаны сақтау (міндетті/міндетті емес — бәрібір;
            //    дедуп тек міндетті емеске қолданылады).
            model.version?.let { version ->
                dataStore.edit { it[LAST_SHOWN_KEY] = version }
            }
            return decision
        } catch (_: Exception) {
            return null
        }
    }

    /** Әдепкі версия — PackageManager.versionName (Flutter PackageInfo.version). */
    private fun currentAppVersion(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (_: Exception) {
        "1.0.0"
    }

    private suspend fun isThrottled(): Boolean {
        val lastCheckMillis = dataStore.data.map { it[LAST_CHECK_KEY] }.first() ?: return false
        val sinceLastCheck = Duration.between(
            Instant.ofEpochMilli(lastCheckMillis),
            Instant.now(),
        )
        return sinceLastCheck < CHECK_INTERVAL
    }

    private suspend fun saveLastCheckTime() {
        try {
            dataStore.edit { it[LAST_CHECK_KEY] = System.currentTimeMillis() }
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val PLATFORM = "android"
        val CHECK_INTERVAL: Duration = Duration.ofHours(24)
        val LAST_CHECK_KEY = longPreferencesKey("last_version_check")
        val LAST_SHOWN_KEY = stringPreferencesKey("last_shown_version")
    }
}