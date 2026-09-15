package com.agroland.feature.profile.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Content URI → MultipartBody.Part (avatar, KYC құжаттары, компания decor).
 * KYC шектеуі: 1..5 файл, әрқайсысы ≤10MB, jpeg/png/webp/pdf (backend валидациясы).
 */
object MultipartHelper {

    private const val MAX_FILE_BYTES = 10L * 1024 * 1024

    suspend fun toPart(context: Context, uri: Uri, partName: String): MultipartBody.Part? =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val mime = resolver.getType(uri) ?: "application/octet-stream"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
                if (bytes.isEmpty() || bytes.size > MAX_FILE_BYTES) return@withContext null
                val fileName = queryDisplayName(context, uri) ?: "upload"
                val body = bytes.toRequestBody(mime.toMediaType())
                MultipartBody.Part.createFormData(partName, fileName, body)
            } catch (e: Exception) {
                Log.w("MultipartHelper", "URI оқу сәтсіз: $uri", e)
                null
            }
        }

    private fun queryDisplayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}