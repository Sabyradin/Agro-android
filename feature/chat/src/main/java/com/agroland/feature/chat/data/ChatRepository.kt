package com.agroland.feature.chat.data

import android.content.Context
import android.net.Uri
import com.agroland.core.network.ApiResult
import com.agroland.core.network.NetworkModule.safeCall
import com.agroland.core.network.error.Failure
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Чат REST репозиторийі (Flutter ChatRemoteService): тізім fallback,
 * файл/дауыс жүктеу (жүктеу пайызы кері байланысымен), статус өзгерту,
 * хабарлама/чат жою, тарих тазарту.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val api: ChatApi,
    @ApplicationContext private val context: Context,
) {

    /** Чат файлы/дауыс шегі — 10 МБ (Flutter chatConstants MAX_FILE_SIZE). */
    val maxFileBytes: Long = MAX_FILE_BYTES

    /** Файл өлшемі (UI алдын-ала «тіпті үлкен» тексеруі үшін). */
    suspend fun fileSizeBytes(uri: Uri): Long = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    /** GET /chat/?chat_status=... — REST fallback (socket үзілгенде). */
    suspend fun getChats(chatStatus: String): ApiResult<List<ChatRoom>> = safeCall {
        ChatParser.parseRoomList(api.getChats(chatStatus))
    }

    /**
     * Файл жүктеу. [onProgress] — 0..100 пайыз (WhatsApp-үлгісінің прогресі).
     * Шегінен асса — [ApiResult.Error] ([ChatUploadFailure]).
     */
    suspend fun uploadFile(
        uri: Uri,
        onProgress: (Int) -> Unit = {},
    ): ApiResult<String> = uploadToEndpoint(
        uri = uri,
        call = { part -> api.uploadFile(part) },
        urlKey = "file_url",
        onProgress = onProgress,
    )

    /** Дауыс жүктеу (жазылған m4a немесе таңдалған аудио файл). */
    suspend fun uploadAudio(
        uri: Uri,
        onProgress: (Int) -> Unit = {},
    ): ApiResult<String> = uploadToEndpoint(
        uri = uri,
        call = { part -> api.uploadAudio(part) },
        urlKey = "audio_url",
        onProgress = onProgress,
    )

    suspend fun updateChatStatus(chatId: Long, status: String): ApiResult<Unit> = safeCall {
        api.updateChatStatus(chatId, status)
        Unit
    }

    suspend fun deleteMessage(messageId: Long): ApiResult<Unit> = safeCall {
        api.deleteMessage(messageId)
        Unit
    }

    suspend fun clearChatHistory(chatId: Long): ApiResult<Unit> = safeCall {
        api.clearChatHistory(chatId)
        Unit
    }

    suspend fun deleteChat(chatId: Long): ApiResult<Unit> = safeCall {
        api.deleteChat(chatId)
        Unit
    }

    private suspend fun uploadToEndpoint(
        uri: Uri,
        call: suspend (MultipartBody.Part) -> kotlinx.serialization.json.JsonElement,
        urlKey: String,
        onProgress: (Int) -> Unit,
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        } ?: return@withContext ApiResult.Error(Failure.Unknown(FileTooLargeException()))
        if (bytes.isEmpty() || bytes.size > MAX_FILE_BYTES) {
            return@withContext ApiResult.Error(Failure.Unknown(FileTooLargeException()))
        }
        val fileName = queryDisplayName(uri) ?: "upload"
        val mime = context.contentResolver.getType(uri) ?: guessMime(fileName)
        val body = ProgressRequestBody(bytes.toRequestBody(mime.toMediaType()), onProgress)
        val part = MultipartBody.Part.createFormData("file", fileName, body)
        safeCall {
            val root = call(part).jsonObject
            val url = sanitizeUrl((root[urlKey] as? kotlinx.serialization.json.JsonPrimitive)?.content)
                .takeIf { !it.isNullOrBlank() }
                ?: throw IllegalStateException("empty_$urlKey")
            url
        }
    }

    private fun queryDisplayName(uri: Uri): String? =
        context.contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null, null, null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private fun guessMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "mp4" -> "video/mp4"
        "mov" -> "video/quicktime"
        "m4a" -> "audio/mp4"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        "aac" -> "audio/aac"
        "flac" -> "audio/flac"
        "pdf" -> "application/pdf"
        else -> "application/octet-stream"
    }

    private companion object {
        const val MAX_FILE_BYTES = 10L * 1024 * 1024
    }
}

/** Файл 10МБ шегінен асты / оқылмады — UI бұны танип «файл тым үлкен» дейді. */
class FileTooLargeException : IllegalStateException("CHAT_FILE_TOO_LARGE")

/** Жүктеу пайызын хабарлайтын RequestBody (okhttp). */
private class ProgressRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (Int) -> Unit,
) : RequestBody() {

    override fun contentType() = delegate.contentType()
    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: okio.BufferedSink) {
        // delegate толығымен staging Buffer-ге оқылады, содан кейін кесіндермен
        // жіберіледі — әр кесінде onProgress шақырылады (okio-ның buffer()
        // helper-і модульде қолжетімсіз болғандықтан қолмен жүзеге асырылды).
        val staging = okio.Buffer()
        delegate.writeTo(staging)
        val size = staging.size
        if (size <= 0L) {
            onProgress(100)
            return
        }
        val step = maxOf(1L, size / 100L)
        var sent = 0L
        while (staging.size > 0L) {
            sink.write(staging, minOf(step, staging.size))
            sent += step
            onProgress(((minOf(sent, size)) * 100 / size).toInt())
        }
        sink.flush()
        onProgress(100)
    }
}