package com.agroland.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * URL-дың толық/салыстырмалы формасын шешетін жалғыз орын (spec: "URL completion in one resolver").
 * Медиа URL-дары кейде absolute, кейде path түрінде келеді.
 */
object MediaUrlResolver {
    var baseMediaUrl: String = ""

    /** Бос/нөлдік мәнді → толық URL-ға айналдырады. CR/LF тазартылады (backend bug). */
    fun resolve(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val cleaned = url
            .replace("\n", "")
            .replace("\r", "")
            .trim()
        if (cleaned.isBlank()) return null
        return if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            cleaned
        } else {
            val base = baseMediaUrl.trimEnd('/')
            if (base.isBlank()) cleaned else "$base/${cleaned.trimStart('/')}"
        }
    }
}

/**
 * Coil 3 + кештелген сурет — CachedImage баламасы.
 * Плейсхолдер — сұр түспен, қате кезінде де жасырын қалады.
 */
@Composable
fun CachedImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val resolved = MediaUrlResolver.resolve(url)
    Box(modifier = modifier) {
        if (resolved != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(resolved)
                    .crossfade(true)
                    .build(),
                imageLoader = context.imageLoader,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}