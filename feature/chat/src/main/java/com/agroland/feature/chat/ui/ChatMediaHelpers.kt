package com.agroland.feature.chat.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Файл хабарламасын ашу (Flutter _openFile паритеті):
 * кеш каталогына жүктеп алып → FileProvider → ACTION_VIEW.
 * Дауыстық/бейне/пайдаланушы таңдаған MIME — extension бойынша.
 */
object ChatFileOpener {

    private val client by lazy { OkHttpClient() }

    /** URL → кештегі файл (OkHttp, ChatRepository-дің шегіне дейін). */
    suspend fun downloadToCache(context: Context, url: String, fileName: String): File? {
        return try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val response = client.newCall(Request.Builder().url(url).build()).execute()
                val body = response.body ?: return@withContext null
                val safeName = fileName.ifBlank { "file_${System.currentTimeMillis()}" }
                val output = File(context.cacheDir, "chat_files")
                output.mkdirs()
                val target = File(output, safeName)
                body.byteStream().use { input ->
                    target.outputStream().use { out -> input.copyTo(out) }
                }
                target
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Файлды сыртқы қосымшамен ашу (FileProvider). */
    fun open(context: Context, file: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val mime = mimeFor(file.name)
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Extension → MIME (беймәлім — application/octet-stream). */
    fun mimeFor(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: "application/octet-stream"
    }
}

/**
 * Дауыстық жазба (Flutter _startRecording паритеті): AAC m4a, макс 120 сек.
 * Кез келген уақытта stop() → (файл, ұзақтық), cancel() → жою.
 */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt: Long = 0

    val isActive: Boolean get() = recorder != null

    /** Жазуды бастау — сәтсіз болса false. */
    fun start(): Boolean {
        if (recorder != null) return true
        return try {
            val dir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
            val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64_000)
            r.setAudioSamplingRate(44_100)
            r.setMaxDuration(MAX_DURATION_MS)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            outputFile = file
            startedAt = System.currentTimeMillis()
            true
        } catch (_: Exception) {
            cleanup()
            false
        }
    }

    /** Жазуды аяқтап, файл + ұзақтығын береді. */
    fun stop(): Pair<File, Int>? {
        val r = recorder ?: return null
        val file = outputFile ?: return null
        return try {
            r.stop()
            val duration = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
            Pair(file, duration)
        } catch (_: Exception) {
            file.delete()
            null
        } finally {
            cleanup()
        }
    }

    /** Жазудан бас тарту — файл жойылады. */
    fun cancel() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
        }
        outputFile?.delete()
        cleanup()
    }

    /** Анимация үшін дыбыс деңгейі (0..1). */
    fun amplitude(): Float = try {
        (recorder?.maxAmplitude ?: 0) / 32767f
    } catch (_: Exception) {
        0f
    }

    private fun cleanup() {
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        outputFile = null
    }

    companion object {
        const val MAX_DURATION_MS = 120_000
    }
}

/** Локация алу құқығы тексерілімі. */
fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
}