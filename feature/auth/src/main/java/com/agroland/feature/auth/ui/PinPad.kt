package com.agroland.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroTextButton
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.auth.security.PinManager

/**
 * PIN пернетақтасы — ортақ компонент:
 *  - App lock (AppLockGate) — verify PIN
 *  - PIN орнату — create + confirm
 */
@Composable
fun PinPad(
    title: String,
    onPinComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    var pin by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = extendedColors().primaryText,
        )
        Text(
            text = errorText ?: "",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 6.dp),
        )
        // Индикатор дөңгелектері.
        Row(
            modifier = Modifier.padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            repeat(PIN_LENGTH) { i ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                if (i < pin.length) MaterialTheme.colorScheme.primary
                                else extendedColors().divider,
                            ),
                    )
                }
            }
        }
        // Сандық пернетақта 3×4.
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "DEL"),
            )
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    row.forEach { key ->
                        when (key) {
                            "" -> Box(modifier = Modifier.size(68.dp))
                            "DEL" -> PinKey(
                                display = null,
                                icon = Icons.AutoMirrored.Outlined.Backspace,
                                onClick = { pin = pin.dropLast(1) },
                            )
                            else -> PinKey(
                                display = key,
                                icon = null,
                                onClick = {
                                    if (pin.length < PIN_LENGTH) {
                                        pin += key
                                        if (pin.length == PIN_LENGTH) {
                                            val completed = pin
                                            onPinComplete(completed)
                                            pin = ""
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
        footer?.invoke()
    }
}

@Composable
private fun PinKey(
    display: String?,
    icon: ImageVector?,
    onClick: () -> Unit,
) {
    val ext = extendedColors()
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(ext.card)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (display != null) {
            Text(
                text = display,
                style = MaterialTheme.typography.bodyLarge,
                color = ext.primaryText,
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ext.secondaryText,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private const val PIN_LENGTH = 4

/** App-lock қақпасы: PIN немесе биометрия. [onUnlocked] — ішкі мәтін ашылады. */
@Composable
fun AppLockGate(
    pinManager: PinManager,
    activity: androidx.fragment.app.FragmentActivity,
    biometricAuthenticator: com.agroland.feature.auth.security.BiometricAuthenticator,
    onUnlocked: () -> Unit,
) {
    var error by remember { mutableStateOf<String?>(null) }

    // stringResource — композбл контекстінде оқылады, ламбдада емес.
    val promptTitle = stringResource(L10nR.string.auth_biometric_prompt)
    val negativeText = stringResource(L10nR.string.common_cancel)
    val biometricErrorText = stringResource(L10nR.string.auth_biometric_error)
    val wrongPinText = stringResource(L10nR.string.pin_wrong)
    val unlockBiometricText = stringResource(L10nR.string.pin_unlock_biometric)

    PinPad(
        title = stringResource(L10nR.string.pin_enter_title),
        errorText = error,
        onPinComplete = { pin ->
            if (pinManager.verifyPin(pin)) {
                onUnlocked()
            } else {
                error = wrongPinText
            }
        },
        footer = {
            if (pinManager.biometricLockEnabled && biometricAuthenticator.isAvailable(activity)) {
                AgroTextButton(
                    text = unlockBiometricText,
                    onClick = {
                        biometricAuthenticator.authenticate(
                            activity = activity,
                            title = promptTitle,
                            negativeButtonText = negativeText,
                            onSuccess = onUnlocked,
                            onError = { error = biometricErrorText },
                        )
                    },
                )
            }
        },
    )
}

/** PIN орнату: create → confirm → done. [onDone]/[onSkipped]. */
@Composable
fun PinSetupPage(
    pinManager: PinManager,
    onDone: () -> Unit,
    onSkipped: () -> Unit,
) {
    var stage by rememberSaveable { mutableIntStateOf(0) } // 0=create, 1=confirm
    var first by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val mismatchText = stringResource(L10nR.string.pin_mismatch)
    val titleRes = if (stage == 0) L10nR.string.pin_setup_title else L10nR.string.pin_confirm_title

    PinPad(
        title = stringResource(titleRes),
        errorText = error,
        onPinComplete = { pin ->
            if (stage == 0) {
                first = pin
                stage = 1
                error = null
            } else {
                if (pin == first) {
                    pinManager.setPin(pin)
                    onDone()
                } else {
                    error = mismatchText
                    stage = 0
                }
            }
        },
        footer = {
            AgroTextButton(
                text = stringResource(L10nR.string.common_cancel),
                onClick = onSkipped,
            )
        },
    )
}