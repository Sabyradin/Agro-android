package com.agroland.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agroland.core.ui.theme.AgroRadius
import com.agroland.core.ui.theme.AgroSize
import com.agroland.core.ui.theme.extendedColors

/** Негізгі (primary) батырма — PrimaryButton баламасы. Loading күйі бар. */
@Composable
fun AgroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val ext = extendedColors()
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.height(AgroSize.button),
        shape = AgroRadius.field,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            // Әдепкі M3 «өшірулі» түсі фонмен қосылып жоғалады — айқын сұр.
            disabledContainerColor = ext.grey,
            disabledContentColor = ext.secondaryText,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = contentColor,
                strokeWidth = 2.5.dp,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/** Екінші дәрежелі батырма — жиегі жасыл, іші мөлдір. */
@Composable
fun AgroOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(AgroSize.button),
        shape = AgroRadius.field,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (enabled) MaterialTheme.colorScheme.primary else extendedColors().divider,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}

/** Кішкентәй батырма — PrimarySmallBtn баламасы. */
@Composable
fun AgroSmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.height(AgroSize.buttonSmall),
        shape = AgroRadius.chip,
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = extendedColors().grey,
            disabledContentColor = extendedColors().secondaryText,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/** Мәтіндік батырма. */
@Composable
fun AgroTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = AgroSize.touchTarget),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Тек иконкалы батырма — тию аймағы 44dp, иконка өлшемі бөлек реттеледі. */
@Composable
fun AgroIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    size: Dp = 22.dp,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(AgroSize.touchTarget),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size),
        )
    }
}

/** Тұрақты орналасқан мәтін (көрсеткілер, бос орын толтырғыштар) үшін ыңғайлы Box. */
@Composable
fun CenterBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier, contentAlignment = Alignment.Center) { content() }
}
