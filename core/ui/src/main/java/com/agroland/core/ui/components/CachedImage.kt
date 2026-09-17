package com.agroland.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.agroland.core.ui.theme.extendedColors

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
 *
 * Сурет жүктелмегенде (URL жоқ, интернет жоқ, 404) орны БОС қалмайды:
 * астында әрқашан бейтарап плейсхолдер тұрады, сурет соның үстіне түседі.
 * Бұрын бос Box қалып, карточкалар «сынған» болып көрінетін.
 */
@Composable
fun CachedImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderIcon: ImageVector = Icons.Rounded.Image,
    showPlaceholder: Boolean = true,
) {
    val context = LocalContext.current
    val resolved = MediaUrlResolver.resolve(url)
    Box(modifier = modifier) {
        if (showPlaceholder) {
            ImagePlaceholder(icon = placeholderIcon)
        }
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

/** Бейтарап плейсхолдер: сұр фон + өлшемге қарай масштабталған иконка. */
@Composable
private fun ImagePlaceholder(icon: ImageVector) {
    val ext = extendedColors()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ext.grey),
        contentAlignment = Alignment.Center,
    ) {
        // Кішкентай аватарларда иконка блокты толтырып кетпеуі керек.
        val iconSize = (minOf(maxWidth, maxHeight) * 0.34f).coerceIn(14.dp, 40.dp)
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ext.divider,
            modifier = Modifier.size(iconSize),
        )
    }
}
