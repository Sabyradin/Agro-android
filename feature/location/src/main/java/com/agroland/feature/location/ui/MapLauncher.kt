package com.agroland.feature.location.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * map_launcher баламасы — сыртқы карта қосымшасын ашу (geo: URI, Android
 * стандартты тәсілі; Yandex/2GIS/Google Maps бәрі оны қабылдайды).
 */
fun openInExternalMaps(context: Context, latitude: Double, longitude: Double, label: String? = null): Boolean =
    try {
        val labelPart = label?.let { "(${Uri.encode(it)})" }.orEmpty()
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude$labelPart")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (_: Exception) {
        false
    }