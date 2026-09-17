package com.agroland.feature.auth.data

import com.agroland.core.network.ApiResult
import com.agroland.core.network.NetworkModule.safeCall
import com.agroland.core.network.auth.TokenStore
import com.agroland.core.network.json.JsonParser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * HATEOAS auth ағынының күй машинасы (spec §MFA):
 *  1) phone → POST /auth/login → task_id + links
 *  2) GET /auth/mfa/{task_id} → SMS жіберілді
 *  3) POST /auth/mfa/{task_id} {sms_code} → код дұрыс
 *  4) POST /auth/login, header: task_id → токендер
 * Signup сол ағынмен, бірақ /auth/signup арқылы.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val tokenStore: TokenStore,
) {

    /** Login 1-қадам: task_id алу. */
    suspend fun startLogin(phone: String): ApiResult<LoginStart> = safeCall {
        val root = api.login(buildJsonObject { put("phone_number", phone) })
        LoginStart(taskId = extractTaskId(root), maskedEmail = JsonParser.string(root, "email"))
    }

    /** Signup 1-қадам: тіркелу деректерін жіберу → task_id. */
    suspend fun startSignup(
        phone: String,
        name: String,
        userType: String,
        companyBin: String?,
        email: String?,
    ): ApiResult<LoginStart> = safeCall {
        val root = api.signup(
            buildJsonObject {
                put("phone_number", phone)
                put("name", name)
                put("user_type", userType)
                if (!companyBin.isNullOrBlank()) put("company_bin", companyBin)
                if (!email.isNullOrBlank()) put("email", email)
            },
        )
        LoginStart(taskId = extractTaskId(root), maskedEmail = JsonParser.string(root, "email"))
    }

    /** SMS кодты жіберу (MFA challenge басталады). */
    suspend fun requestSms(taskId: String): ApiResult<Unit> = safeCall {
        api.requestSms(taskId)
        Unit
    }

    /** Email арқылы қайта жіберу. */
    suspend fun sendCodeByEmail(taskId: String): ApiResult<Unit> = safeCall {
        api.sendCodeByEmail(taskId)
        Unit
    }

    /** Код тексеру + login аяқтау → токендер сақталады. */
    suspend fun confirmOtpAndLogin(taskId: String, smsCode: String): ApiResult<SessionTokens> = safeCall {
        api.submitSmsCode(taskId, buildJsonObject { put("sms_code", smsCode) })
        val root = api.login(buildJsonObject { put("task_id", taskId) }, taskId)
        val tokens = parseTokens(root)
        persist(tokens)
        tokens
    }

    /** Signup аяқтау → токендер. */
    suspend fun confirmOtpAndSignup(taskId: String, smsCode: String): ApiResult<SessionTokens> = safeCall {
        api.submitSmsCode(taskId, buildJsonObject { put("sms_code", smsCode) })
        val root = api.signup(buildJsonObject { put("task_id", taskId) }, taskId)
        val tokens = parseTokens(root)
        persist(tokens)
        tokens
    }

    /** Биометриялық token-мен кіру (backend 404 бере алады — best-effort, ISSUES.md #2). */
    suspend fun loginWithBiometricToken(biometricToken: String): ApiResult<SessionTokens> = safeCall {
        val root = api.biometricLogin(buildJsonObject { put("biometric_token", biometricToken) })
        val tokens = parseTokens(root)
        persist(tokens)
        tokens
    }

    private fun persist(tokens: SessionTokens) {
        tokenStore.saveSession(
            access = tokens.access.takeIf { it.isNotBlank() },
            refresh = tokens.refresh.takeIf { it.isNotBlank() },
            userId = null,
            biometric = tokens.biometric,
        )
    }

    /**
     * HATEOAS жауаптан task_id шығарады:
     *  - тура «task_id» өрісінен, болмаса
     *  - links ішіндегі /auth/mfa/{task_id} href-інен.
     */
    internal fun extractTaskId(root: JsonObject?): String? =
        AuthTaskIdParser.extract(root)

    /** {access_token, refresh_token, biometric_token} — кешірімді парсинг. */
    internal fun parseTokens(root: JsonObject?): SessionTokens {
        return SessionTokens(
            access = JsonParser.string(root, "access_token") ?: "",
            refresh = JsonParser.string(root, "refresh_token") ?: "",
            biometric = JsonParser.string(root, "biometric_token"),
        )
    }
}

/** Login/Signup бірінші қадамының нәтижесі. */
data class LoginStart(
    val taskId: String?,
    val maskedEmail: String?,
)

/** Токен үштігі. */
data class SessionTokens(
    val access: String,
    val refresh: String,
    val biometric: String?,
)