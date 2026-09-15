package com.agroland.feature.push.device

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Құрылғы параметрлері (Flutter device_details.dart Android тармағы):
 * deviceId — android_id (SSAID), systemVersion — Build.VERSION.RELEASE,
 * deviceName — Build.MODEL, appVersion — PackageManager.
 */
@Singleton
class DeviceDetailsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val deviceId: String
        get() = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (_: Exception) {
            "unknown"
        }

    val osVersion: String
        get() = Build.VERSION.RELEASE ?: ""

    val deviceModel: String
        get() = Build.MODEL ?: ""

    val appVersion: String
        get() = try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: ""
        } catch (_: Exception) {
            ""
        }
}