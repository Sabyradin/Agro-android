package com.agroland.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agroland.core.ui.theme.AgroRadius
import com.agroland.core.ui.theme.AgroSpacing
import com.agroland.core.ui.theme.extendedColors

/** Көлеңке түсі — қара емес, жұмсақ сұр-жасыл (жиегі көрінбейді). */
private val ShadowTint = Color(0x14121212)

/**
 * Карточка беті — ашық темада жеңіл көлеңке, қараңғыда тек фон айырмасы.
 * `shadow` clip-тен БҰРЫН қойылады, әйтпесе көлеңке қиылып қалады.
 */
@Composable
fun AgroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = AgroRadius.card,
    elevation: Dp = 2.dp,
    containerColor: Color = extendedColors().card,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(AgroSpacing.md),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .shadow(elevation = elevation, shape = shape, ambientColor = ShadowTint, spotColor = ShadowTint)
            .clip(shape)
            .background(containerColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * Секция тақырыбы — «Барлығын көру» сілтемесімен.
 * Тізім беттерінде блоктарды бөліп тұрады.
 */
@Composable
fun AgroSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AgroSpacing.screen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = extendedColors().primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (actionText != null && onActionClick != null) {
            AgroTextButton(text = actionText, onClick = onActionClick)
        }
    }
}

/** Жіңішке бөлгіш сызық — тізім элементтерінің арасы. */
@Composable
fun AgroDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 0.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(1.dp)
            .background(extendedColors().divider),
    )
}

/**
 * Дөңгелек фондағы иконка — сервис плиткалары, бос күй суреттері,
 * тізім жолдарының leading элементі.
 */
@Composable
fun AgroIconCircle(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = size * 0.5f,
    containerColor: Color = extendedColors().backgroundLight,
    tint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}
