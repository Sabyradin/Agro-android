package com.agroland.feature.media.data

/**
 * QR мәнінен жарнама id-ін алу (Flutter QrScannerPage._extractAnnouncementId
 * 1:1). Таза функция — android.net.Uri қолданбайды (JVM unit-тестте
 * қапталады), path/query қолмен талданады.
 *
 * Танылатын пішіндер:
 *  - https://agroland.kz/announcement/12345      → 12345
 *  - https://host/announcement/ad-99             → 99 (соңғы цифрлар)
 *  - https://host/path?announcementId=42          → 42
 *  - announcement/123                             → 123 (салыстырмалы path)
 */
object QrUrlParser {

    private val trailingDigits = Regex("""(\d+)$""")

    /** Жарнама id табылмаса null. Бірінші path, сосын query параметр. */
    fun extractAnnouncementId(url: String): Long? {
        val cleaned = url.trim()
        if (cleaned.isEmpty()) return null

        // Query бөлігін ажыратамыз (фрагмент '#…' еленбейді).
        val queryPart: String?
        val beforeQuery: String
        val queryIndex = cleaned.indexOf('?')
        if (queryIndex >= 0) {
            beforeQuery = cleaned.substring(0, queryIndex)
            queryPart = cleaned.substring(queryIndex + 1).substringBefore('#')
        } else {
            beforeQuery = cleaned
            queryPart = null
        }

        // Path: scheme://authority кесіп тастап, қалғаны бүтін path.
        // «://» жоқ болса — салыстырмалы path, толығымен қалады.
        val path = if (beforeQuery.contains("://")) {
            val authorityAndPath = beforeQuery.substringAfter("://")
            val slash = authorityAndPath.indexOf('/')
            if (slash < 0) "" else authorityAndPath.substring(slash)
        } else {
            beforeQuery
        }
        val segments = path.split('/').filter { it.isNotEmpty() }

        var i = 0
        while (i < segments.size) {
            if (segments[i] == "announcement" && i + 1 < segments.size) {
                val segment = segments[i + 1]
                val candidate = trailingDigits.find(segment)?.groupValues?.getOrNull(1) ?: segment
                candidate.toLongOrNull()?.let { return it }
                // сегмент санға айналмаса — query параметрді де тексереміз
                break
            }
            i++
        }

        queryPart?.split('&')?.forEach { pair ->
            val eq = pair.indexOf('=')
            val key = if (eq >= 0) pair.substring(0, eq) else pair
            if (key == "announcementId") {
                val value = if (eq >= 0) pair.substring(eq + 1) else ""
                value.toLongOrNull()?.let { return it }
            }
        }
        return null
    }
}