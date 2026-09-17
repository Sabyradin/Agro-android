package com.agroland.feature.chat.domain

import com.agroland.feature.chat.data.ChatMessage

/**
 * Web-парты `inferMessageType` алгоритмінен 1:1 порт
 * (CHAT_MESSAGE_RENDER_SPEC §2, Flutter infer_message_type.dart).
 *
 * Backend chat хабарламаны көбіне `message_type: "text"` деп сақтайды да,
 * типті message/file_url/audio_url ішіндегі URL-ден inference жасайды.
 *
 * Тәртіп (спек §1/§2):
 *  1. message_type != 'text'   → сол message_type
 *  2. latitude && longitude    → 'location'
 *  3. "lat, lng" координат мәтін → 'location'
 *  4. text/file_url/audio_url URL-like:
 *       a. path extension       → image/audio/video/file
 *       b. path substring       → соның біреуі (extension жоқ болса)
 *  5. басқасы                   → 'text'
 */
enum class InferredMessageType { IMAGE, AUDIO, VIDEO, FILE, LOCATION, TEXT, OTHER }

object InferMessageType {

    fun infer(msg: ChatMessage, text: String): InferredMessageType {
        // 1a. Сайт суретті/бейнені/аудионы «file» типімен жібереді (мыс. скриншот .png) —
        //     URL кеңейтімі медиа болса, «Файл» емес, сол медиа ретінде көрсетеміз.
        if (msg.messageType == "file") {
            for (candidate in listOf(msg.fileUrl, msg.message)) {
                if (candidate.isNullOrBlank()) continue
                when (inferFromUrl(candidate)) {
                    InferredMessageType.IMAGE -> return InferredMessageType.IMAGE
                    InferredMessageType.VIDEO -> return InferredMessageType.VIDEO
                    InferredMessageType.AUDIO -> return InferredMessageType.AUDIO
                    else -> Unit
                }
            }
        }

        // 1b. Backend нақты тип берген — соны сыйлау (text-тен басқасы).
        if (msg.messageType != "text") {
            return when (msg.messageType) {
                "image" -> InferredMessageType.IMAGE
                "audio" -> InferredMessageType.AUDIO
                "file" -> InferredMessageType.FILE
                "location" -> InferredMessageType.LOCATION
                "video" -> InferredMessageType.VIDEO
                else -> InferredMessageType.OTHER
            }
        }

        // 2. Координат өрістері бар → location (message_type text болса да).
        if (msg.latitude != null && msg.longitude != null) {
            return InferredMessageType.LOCATION
        }

        val t = text.trim()

        // 3. "lat, lng" мәтіні → location.
        if (COORDS_REGEX.matches(t)) {
            return InferredMessageType.LOCATION
        }

        // 4. URL inference — absolute немесе relative /...path. Кандидаттар:
        //    әуелі message мәтіні, кейін file_url, содан audio_url (сайт кейде
        //    message-ді бос жіберіп, URL-ді тек file_url-де қалдырады).
        //    msg.message де қоса жүреді — caller бос text берсек те қажет.
        for (candidate in listOf(t, msg.fileUrl, msg.audioUrl, msg.message)) {
            if (candidate.isNullOrBlank()) continue
            inferFromUrl(candidate)?.let { return it }
        }

        // 5. Әрқашан text (мыс. announcement сілтемесі — Linkify рендер).
        return InferredMessageType.TEXT
    }

    /** Жалғыз URL-ден медиа типін шығарады. Танылмаса null. */
    fun inferFromUrl(raw: String): InferredMessageType? {
        val lower = raw.trim().lowercase()
        val isUrlLike = lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("/")
        if (!isUrlLike) return null

        // 4a. Path extension (?/# кейіні қиылады, case-insensitive).
        pathExtension(lower)?.let { ext ->
            if (ext in IMAGE_EXT) return InferredMessageType.IMAGE
            if (ext in VIDEO_EXT) return InferredMessageType.VIDEO
            if (ext in AUDIO_EXT) return InferredMessageType.AUDIO
            if (ext in FILE_EXT) return InferredMessageType.FILE
        }
        // 4b. Path substring (extension жоқ болса).
        if ("/image/" in lower || "/photo/" in lower) return InferredMessageType.IMAGE
        if ("/video/" in lower || "/movie/" in lower) return InferredMessageType.VIDEO
        if ("/audio/" in lower || "/voice/" in lower) return InferredMessageType.AUDIO
        if ("/file/" in lower || "/document/" in lower) return InferredMessageType.FILE
        return null
    }

    /**
     * Хабарламадан рендер үшін media URL-ді алады (spec §3.5): image URL
     * message өрісінде жүреді; file_url — file/audio үшін қосымша.
     * Бос емес бірінші кандидат қайтарылады.
     */
    fun mediaUrlFor(msg: ChatMessage): String? {
        for (c in listOf(msg.fileUrl, msg.audioUrl, msg.message)) {
            if (!c.isNullOrBlank()) return c.trim()
        }
        return null
    }

    /**
     * Локация координаттары: latitude/longitude өрістері, болмаса `message` ішіндегі
     * «lat, lng» мәтіні (сайт локацияны тек мәтін ретінде жібереді).
     */
    fun coordinatesFor(msg: ChatMessage): Pair<Double, Double>? {
        val lat = msg.latitude
        val lng = msg.longitude
        if (lat != null && lng != null) return lat to lng
        val match = COORDS_CAPTURE_REGEX.matchEntire(msg.message.trim()) ?: return null
        val parsedLat = match.groupValues[1].toDoubleOrNull() ?: return null
        val parsedLng = match.groupValues[2].toDoubleOrNull() ?: return null
        return parsedLat to parsedLng
    }

    /**
     * Файлдың көрсетілетін аты: `file_name`, болмаса URL-дің соңғы сегменті —
     * %-декодталып, backend қосатын UUID префиксі («1b2c…-») алынып тасталады.
     */
    fun displayFileName(msg: ChatMessage): String? {
        msg.fileName?.takeIf { it.isNotBlank() }?.let { return it }
        val url = mediaUrlFor(msg) ?: return null
        val segment = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
        if (segment.isBlank()) return null
        val decoded = try {
            java.net.URLDecoder.decode(segment.replace("+", "%2B"), "UTF-8")
        } catch (_: Exception) {
            segment
        }
        return decoded.replace(UUID_PREFIX_REGEX, "").ifBlank { decoded }
    }

    fun pathExtension(lower: String): String? {
        val path = lower.substringBefore('?').substringBefore('#')
        // Соңғы path сегментін ғана қараймыз — әйтпесе хост ішіндегі нүкте
        // («a.kz/b/c») жалған «kz/b/c» extension береді.
        val segment = path.substringAfterLast('/')
        val dot = segment.lastIndexOf('.')
        if (dot < 0 || dot == segment.length - 1) return null
        return segment.substring(dot + 1)
    }

    /** Видеоны бөлек таныту үшін _fileExt-тен бөлек (Flutter сияқты). */
    val VIDEO_PATH_EXTS = setOf("mp4", "mov", "avi", "mkv")

    private val COORDS_REGEX = Regex("^-?\\d+\\.\\d+,\\s*-?\\d+\\.\\d+$")
    private val COORDS_CAPTURE_REGEX = Regex("^(-?\\d+\\.\\d+),\\s*(-?\\d+\\.\\d+)$")
    private val UUID_PREFIX_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}-")

    private val IMAGE_EXT = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "avif", "tiff", "tif",
    )
    private val AUDIO_EXT = setOf(
        "mp3", "wav", "ogg", "m4a", "aac", "flac", "wma", "opus", "webm",
    )
    private val FILE_EXT = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "zip", "7z", "txt", "csv", "ppt", "pptx",
    )
    private val VIDEO_EXT = VIDEO_PATH_EXTS
}