package com.agroland.feature.chat.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.agroland.core.l10n.R as L10nR
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * Статикалық карта (OSM тайлдары, Web Mercator) — Flutter StaticMapView паритеті.
 * Тап → 2GIS (geo: intent, fallback https://2gis.kz/search/lat,lng).
 * «© OpenStreetMap» атрибуциясы — OSM политикасының талабы.
 */
@Composable
fun StaticMapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
    zoom: Int = 15,
    onOpenMap: () -> Unit,
) {
    val openInLabel = stringResource(L10nR.string.open_in2gis)

    // Web Mercator: центр тайл координаттары (fractional).
    val n = 1 shl zoom
    val centerX = (longitude + 180.0) / 360.0 * n
    val centerY = (1.0 - ln(tan(Math.toRadians(latitude)) + 1 / Math.cos(Math.toRadians(latitude))) / Math.PI) / 2.0 * n
    val tile = 256

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onOpenMap),
    ) {
        // 3×3 тайл торы — картаның кішкентай статикалық көрінісі.
        val baseX = centerX.roundToInt()
        val baseY = centerY.roundToInt()
        for (dx in -1..1) {
            for (dy in -1..1) {
                val tx = baseX + dx
                val ty = baseY + dy
                if (ty < 0 || ty >= n) continue
                val wrapped = ((tx % n) + n) % n
                val offsetX = ((tx - centerX) * tile).roundToInt()
                val offsetY = ((ty - centerY) * tile).roundToInt()
                AsyncImage(
                    model = "https://tile.openstreetmap.org/$zoom/$wrapped/$ty.png",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .offset { IntOffset(offsetX, offsetY) }
                        .size(tile.dp)
                        .align(Alignment.Center),
                )
            }
        }

        // Центрдегі пин.
        Icon(
            imageVector = Icons.Rounded.LocationOn,
            contentDescription = openInLabel,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-10).dp)
                .size(40.dp),
        )

        // «© OpenStreetMap» атрибуциясы + 2GIS сілтемесі.
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.45f),
        ) {
            Text(
                text = "© OpenStreetMap",
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** 2GIS/Карталар ашу (geo: intent, fallback https://2gis.kz). */
fun openLocationInMap(context: android.content.Context, latitude: Double, longitude: Double) {
    val geo = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, geo))
        return
    } catch (_: Exception) {
    }
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://2gis.kz/search/$latitude,$longitude")),
        )
    } catch (_: Exception) {
    }
}