package com.agroland.feature.marketplace.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroland.core.ui.theme.extendedColors

/*
 * Жасау беттерінің (жарнама / ұсыныс / жаппай жүктеу) iOS стиліндегі ортақ
 * құрылым-блоктары: топталған карточкалар, ішкі бөлгіштер, «Жабу» тақырыбы,
 * сегментті ауыстырғыш және төменгі әрекет панелі.
 */

/** Карточка ішіндегі жолдардың көлденең шегінісі — бөлгіш те осыдан басталады. */
internal val FormRowPadding = 16.dp

/** Бөлім: сол жақта атау (қалауынша оң жақта санауыш) + дөңгелектенген карточка. */
@Composable
internal fun FormSection(
    title: String?,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val ext = extendedColors()
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null || trailing != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title.orEmpty(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ext.secondaryText,
                    modifier = Modifier.weight(1f),
                )
                if (trailing != null) {
                    Text(text = trailing, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ext.secondaryText)
                }
            }
        }
        FormCard(content = content)
        if (footer != null) {
            Text(
                text = footer,
                fontSize = 12.sp,
                color = ext.secondaryText,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp),
            )
        }
    }
}

/** Атаусыз топтама карточкасы. */
@Composable
internal fun FormCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(extendedColors().card),
        content = content,
    )
}

/** Карточка ішіндегі бөлгіш (iOS: мәтін басталатын жерден). */
@Composable
internal fun FormDivider(start: Dp = FormRowPadding) {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = extendedColors().divider,
        modifier = Modifier.padding(start = start, end = FormRowPadding),
    )
}

/** Жиексіз мәтін өрісі: бос кезде сұр placeholder, қалауынша оң жақта белгі (₸) не батырма. */
@Composable
internal fun FormTextRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: Dp = 52.dp,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailing: (@Composable () -> Unit)? = null,
) {
    val ext = extendedColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .padding(horizontal = FormRowPadding, vertical = 14.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 16.sp,
                    color = if (isError) MaterialTheme.colorScheme.error else ext.secondaryText,
                    maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, color = ext.primaryText),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = keyboardOptions,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (singleLine) Modifier else Modifier.heightIn(min = minHeight - 28.dp)),
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/**
 * Таңдау жолы: (иконка) + атау, оң жақта мән жасыл түспен + шеврон немесе
 * ⇕ белгісі. [label] бос болса — мән жолдың бүкіл енін алады.
 */
@Composable
internal fun FormSelectRow(
    label: String?,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    valueIsPlaceholder: Boolean = true,
    updown: Boolean = false,
    isError: Boolean = false,
) {
    val ext = extendedColors()
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = FormRowPadding, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
        }
        if (label != null) {
            Text(
                text = label,
                fontSize = 16.sp,
                color = if (isError) MaterialTheme.colorScheme.error else ext.primaryText,
                maxLines = 1,
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = value,
            fontSize = 16.sp,
            color = when {
                isError && label == null -> MaterialTheme.colorScheme.error
                label == null && !valueIsPlaceholder -> ext.primaryText
                label == null -> ext.secondaryText
                valueIsPlaceholder -> primary.copy(alpha = 0.8f)
                else -> ext.secondaryText
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (label != null) TextAlign.End else TextAlign.Start,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = if (updown) Icons.Rounded.UnfoldMore else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = if (updown) primary else ext.secondaryText.copy(alpha = 0.6f),
            modifier = Modifier.size(if (updown) 18.dp else 20.dp),
        )
    }
}

/** Әрекет жолы: жасыл иконка + жасыл мәтін (мыс. «⊕ Тағы қосу», «Шаблонды жүктеп алу»). */
@Composable
internal fun FormActionRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val primary = MaterialTheme.colorScheme.primary
    val color = if (enabled) primary else extendedColors().secondaryText
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = FormRowPadding, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(color = primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(text = text, fontSize = 16.sp, color = color)
    }
}

/** Қосқыш жолы — атау + switch. */
@Composable
internal fun FormToggleRow(title: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = FormRowPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = extendedColors().primaryText,
            modifier = Modifier.weight(1f),
        )
        com.agroland.core.ui.components.AgroSwitch(checked = checked, onCheckedChange = onToggle)
    }
}

/** iOS сегментті ауыстырғышы: сұр трек + таңдалғаны ақшыл «таблетка». */
@Composable
internal fun FormSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
) {
    val ext = extendedColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(ext.grey)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(if (selected) ext.card else Color.Transparent, label = "segment")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(height - 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = ext.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

/**
 * Жасау бетінің тақырыбы: сол жақта «Жабу» таблеткасы, ортада атау, астында
 * (қалауынша) бөлімдер ауыстырғышы.
 */
@Composable
internal fun CreateHeader(
    title: String,
    closeLabel: String,
    onClose: () -> Unit,
    tabs: List<String> = emptyList(),
    selectedTab: Int = 0,
    onSelectTab: (Int) -> Unit = {},
) {
    val ext = extendedColors()
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ext.card)
            .statusBarsPadding()
            .padding(horizontal = 12.dp)
            .padding(top = 6.dp, bottom = 10.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(
                text = closeLabel,
                fontSize = 16.sp,
                color = primary,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(50))
                    .background(primary.copy(alpha = 0.12f))
                    .clickable(onClick = onClose)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = ext.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 88.dp),
            )
        }
        if (tabs.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FormSegmented(options = tabs, selectedIndex = selectedTab, onSelect = onSelectTab, height = 38.dp)
        }
    }
}

/** Төменгі әрекет панелі: бір немесе екі батырма (екіншісі — негізгі). */
@Composable
internal fun CreateBottomBar(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(extendedColors().card),
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = extendedColors().divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/** Төменгі панель батырмасы: негізгі (жасыл) немесе екінші дәрежелі (ақшыл-жасыл). */
@Composable
internal fun CreateBarButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val ext = extendedColors()
    val green = MaterialTheme.colorScheme.primary
    val container = when {
        !enabled -> ext.grey
        primary -> green
        else -> green.copy(alpha = 0.12f)
    }
    val content = when {
        !enabled -> ext.secondaryText
        primary -> Color.White
        else -> green
    }
    Row(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(color = content, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
