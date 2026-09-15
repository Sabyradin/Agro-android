package com.agroland.core.network.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Token сақтау қоймасы — EncryptedSharedPreferences.
 * Refresh token single-use (spec §4): жаңартылғаннан кейін бірден алмастырылады.
 * biometric_token — биометриямен кіру үшін (backend маршрут әзірге тіркелмеген — ISSUES.md #2).
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "agroland_secure_tokens",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken

    private val _refreshToken = MutableStateFlow<String?>(null)
    val refreshToken: StateFlow<String?> = _refreshToken

    private val _biometricToken = MutableStateFlow<String?>(null)
    val biometricToken: StateFlow<String?> = _biometricToken

    private val _userId = MutableStateFlow<Int?>(null)
    val userId: StateFlow<Int?> = _userId

    init {
        _accessToken.value = prefs.getString(KEY_ACCESS, null)
        _refreshToken.value = prefs.getString(KEY_REFRESH, null)
        _biometricToken.value = prefs.getString(KEY_BIOMETRIC, null)
        _userId.value = prefs.getString(KEY_USER_ID, null)?.toIntOrNull()
    }

    /** Login/signup кезінде толық сессия жазылады (барлығы бірге). */
    @Synchronized
    fun saveSession(access: String?, refresh: String?, userId: Int?, biometric: String?) {
        prefs.edit().apply {
            if (access != null) putString(KEY_ACCESS, access) else remove(KEY_ACCESS)
            if (refresh != null) putString(KEY_REFRESH, refresh) else remove(KEY_REFRESH)
            if (biometric != null) putString(KEY_BIOMETRIC, biometric) else remove(KEY_BIOMETRIC)
            if (userId != null) putString(KEY_USER_ID, userId.toString()) else remove(KEY_USER_ID)
            apply()
        }
        _accessToken.value = access
        _refreshToken.value = refresh
        _biometricToken.value = biometric
        _userId.value = userId
    }

    /** Single-flight refresh нәтижесі — тек access/refresh, қалғанды сақтаймыз. */
    @Synchronized
    fun saveRefreshedTokens(access: String, refresh: String, biometric: String?) {
        prefs.edit().apply {
            putString(KEY_ACCESS, access)
            putString(KEY_REFRESH, refresh)
            if (biometric != null) putString(KEY_BIOMETRIC, biometric)
            apply()
        }
        _accessToken.value = access
        _refreshToken.value = refresh
        if (biometric != null) _biometricToken.value = biometric
    }

    /** Тек biometric token жаңарту. */
    @Synchronized
    fun saveBiometric(token: String) {
        prefs.edit().putString(KEY_BIOMETRIC, token).apply()
        _biometricToken.value = token
    }

    /** Logout / unauthorized. */
    @Synchronized
    fun clear() {
        prefs.edit().clear().apply()
        _accessToken.value = null
        _refreshToken.value = null
        _biometricToken.value = null
        _userId.value = null
    }

    val isAuthorized: Boolean get() = _accessToken.value != null

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_BIOMETRIC = "biometric_token"
        const val KEY_USER_ID = "user_id"
    }
}