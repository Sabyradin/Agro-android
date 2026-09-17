package com.agroland.app.appversion

import com.agroland.core.network.json.JsonParser
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Force-update моделі (Фаза 20) — Flutter app_version_model.dart 1:1.
 * GET /app-version?platform=android&current_version=X.
 * Толық кешірімді парсинг (белгісіз өрістерге tolerant, backend-ті өзгертпейміз).
 */
data class AppVersionModel(
    val platform: String,
    val version: String? = null,
    val minVersion: String? = null,
    val forceUpdate: Boolean = false,
    val updateMessage: String? = null,
    val updateRequired: Boolean = false,
    val isActive: Boolean? = null,
)

object AppVersionParser {

    /** `{platform, version, min_version, force_update, update_message, update_required, is_active}`. */
    fun parse(root: JsonElement): AppVersionModel? {
        // Laravel-түрі қаптама: `{data}` — ішіндегісін ашамыз.
        var obj = root as? JsonObject ?: return null
        (obj["data"] as? JsonObject)?.let { obj = it }
        val platform = JsonParser.string(obj, "platform") ?: return null
        return AppVersionModel(
            platform = platform,
            version = JsonParser.string(obj, "version"),
            minVersion = JsonParser.string(obj, "min_version"),
            forceUpdate = JsonParser.bool(obj, "force_update") ?: false,
            updateMessage = JsonParser.string(obj, "update_message"),
            updateRequired = JsonParser.bool(obj, "update_required") ?: false,
            isActive = JsonParser.bool(obj, "is_active"),
        )
    }
}

/**
 * Версияларды салыстыру — Flutter _compareVersions 1:1.
 * «1.0.33» vs «1.0.31»: -1 — current < server, 0 — тең, 1 — current > server.
 * Ұзындықтары теңестіріледі («1.2» vs «1.2.1» → [1,2,0] vs [1,2,1]).
 */
fun compareVersions(current: String, server: String): Int {
    val currentParts = current.split('.').map { it.toIntOrNull() ?: 0 }.toMutableList()
    val serverParts = server.split('.').map { it.toIntOrNull() ?: 0 }.toMutableList()
    while (currentParts.size < serverParts.size) currentParts.add(0)
    while (serverParts.size < currentParts.size) serverParts.add(0)
    for (i in currentParts.indices) {
        if (currentParts[i] < serverParts[i]) return -1
        if (currentParts[i] > serverParts[i]) return 1
    }
    return 0
}

/** Диалог шешімі: көрсету керек пе, міндетті ме. */
data class UpdateDecision(
    val model: AppVersionModel,
    val isForceUpdate: Boolean,
)

/**
 * Flutter checkAndShowUpdateDialog шешім-логикасы 1:1 (таза функция — тесттер
 * AppVersionLogicTest-те):
 *
 * 1) version (target) берілген болса: current > target → диалог ЖОҚ;
 *    current < target → ескі нұсқа (флагтар серверден); current == target →
 *    min_version тексеріледі: current < min → МІНДЕТТІ жаңарту.
 * 2) version берілмесе — isTarget = update_required флаг-і.
 * 3) Ескі нұсқа (target-тен төмен) — флагтар: update_required/force_update.
 * 4) Міндетті емес жаңарту — lastShownVersion дедупликациясы (бір нұсқа
 *    үшін бір рет көрсетіледі); міндеттіде дедуп жоқ (әрдайым көрсетіледі).
 *
 * @param lastShownVersion соңғы көрсетілген сервер нұсқасы (dedupe).
 */
fun decideUpdate(
    currentVersion: String,
    model: AppVersionModel,
    lastShownVersion: String?,
): UpdateDecision? {
    // 1. Target version: тек осы нұсқадағыларға (және төмендегілерге) қатысты.
    val targetVersion = model.version?.takeIf { it.isNotEmpty() }
    val isTargetVersion: Boolean
    if (targetVersion != null) {
        when (compareVersions(currentVersion, targetVersion)) {
            1 -> return null // current > target — әлдеқашан жаңа, диалог жоқ
            0 -> isTargetVersion = true
            else -> isTargetVersion = false // ескі нұсқа — жаңарту көрсету керек
        }
    } else {
        isTargetVersion = model.updateRequired
    }

    var shouldShowUpdate: Boolean
    var isForceUpdate: Boolean
    if (isTargetVersion) {
        val minVersion = model.minVersion?.takeIf { it.isNotEmpty() }
        if (minVersion != null) {
            shouldShowUpdate = false
            isForceUpdate = false
            if (compareVersions(currentVersion, minVersion) < 0) {
                shouldShowUpdate = true // минималды талап қанағаттандырылмайды
                isForceUpdate = true
            }
        } else {
            shouldShowUpdate = model.updateRequired
            isForceUpdate = model.forceUpdate
        }
    } else {
        // Ескі нұсқа — сервер флагтары.
        shouldShowUpdate = model.updateRequired
        isForceUpdate = model.forceUpdate
    }

    if (!shouldShowUpdate) return null

    // Міндетті емес жаңарту — бұрын көрсетілген нұсқа үшін үнсіз қалдыру.
    if (!isForceUpdate && targetVersion != null && lastShownVersion == targetVersion) {
        return null
    }
    return UpdateDecision(model, isForceUpdate)
}