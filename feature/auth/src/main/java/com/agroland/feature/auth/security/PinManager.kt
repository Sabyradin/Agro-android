package com.agroland.feature.auth.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-lock PIN (AccessCodeService баламасы, spec фаза 3):
 * SHA-256(pin + salt), hash+salt шифрланған prefs-та.
 * Пайдаланушыға ешқашан қолжетімді — тек hash салыстырылады.
 */
@Singleton
class PinManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "agroland_secure_pin",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    val isPinSet: Boolean get() = prefs.contains(KEY_HASH)

    /** Биометриялық app-lock қосылған ба (Flutter: APP_BIOMETRIC_LOCK_KEY). */
    var biometricLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIO_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_BIO_ENABLED, value).apply()
        }

    /** PIN орнату — әрқашан жаңа salt. */
    fun setPin(pin: String) {
        val salt = newSalt()
        prefs.edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, hash(pin, salt))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val stored = prefs.getString(KEY_HASH, null) ?: return false
        return constantTimeEquals(hash(pin, salt), stored)
    }

    fun clearPin() {
        prefs.edit().clear().apply()
    }

    private fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + pin).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }

    private companion object {
        const val KEY_HASH = "pin_hash"
        const val KEY_SALT = "pin_salt"
        const val KEY_BIO_ENABLED = "biometric_lock_enabled"
    }
}