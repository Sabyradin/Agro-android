package com.agroland.app.appversion

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.theme.extendedColors

/**
 * Force-update диалогі (Фаза 20) — Flutter _UpdateDialog 1:1, Material 3
 * тұрғысында әсемделген: градиент шеңбер + жүктеу иконкасы, дөңгелек
 * бұрыштар, міндетті күйде артқа/сыртқы түртуге жабылмайды.
 */
@Composable
fun UpdateDialog(
    isForceUpdate: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val title = stringResource(L10nR.string.update_available)
    // Сервер update_message мәтіні бір тілде ғана — қосымша тіліндегі
    // мәтін қолданылады (Flutter комментарииндегі шешім).
    val message = stringResource(L10nR.string.update_message)
    val updateLabel = stringResource(L10nR.string.update_action)
    val cancelLabel = stringResource(L10nR.string.cancel)
    val cannotOpen = stringResource(L10nR.string.cannot_open_link)

    BackHandler(enabled = isForceUpdate) { /* міндетті жаңарту — артқа жабылмайды */ }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = !isForceUpdate,
            dismissOnBackPress = !isForceUpdate,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Градиентті шеңбер + жүктеу иконкасы (Flutter parity, әсемдеу:
                // жұмсақ сақина + градиент primary → primaryLight).
                UpdateBadge()

                Spacer(Modifier.height(24.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors().primaryText,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extendedColors().secondaryText,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25f,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(32.dp))

                if (!isForceUpdate) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text(
                                text = cancelLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        UpdateButton(
                            label = updateLabel,
                            modifier = Modifier.weight(1f),
                            onClick = { openPlayStore(context, cannotOpen) },
                        )
                    }
                } else {
                    UpdateButton(
                        label = updateLabel,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { openPlayStore(context, cannotOpen) },
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateBadge(modifier: Modifier = Modifier) {
    val ext = extendedColors()
    Box(
        modifier = modifier
            .size(96.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, ext.primaryLight),
                ),
                shape = CircleShape,
            )
            .border(
                width = 10.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Download,
            contentDescription = null,
            tint = ext.white,
            modifier = Modifier.size(46.dp),
        )
    }
}

@Composable
private fun UpdateButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = extendedColors().white,
        ),
    ) {
        Icon(
            imageVector = Icons.Rounded.Download,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Play Store бетін сыртқы браузерде ашу; ашылмаса — адам тіліндегі toast. */
private fun openPlayStore(context: android.content.Context, errorText: String) {
    var opened = false
    try {
        val intent = Intent(
            Intent.ACTION_VIEW,
            android.net.Uri.parse(PLAY_STORE_URL),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        opened = true
    } catch (_: Exception) {
    }
    if (!opened) Toast.makeText(context, errorText, Toast.LENGTH_SHORT).show()
}

/** Flutter _openAppStore URL-і (Android нұсқасы). */
private const val PLAY_STORE_URL =
    "https://play.google.com/store/apps/details?id=com.agroland.app&hl=en"