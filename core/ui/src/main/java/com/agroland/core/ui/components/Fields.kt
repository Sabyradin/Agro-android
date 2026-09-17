package com.agroland.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroland.core.ui.theme.AgroRadius
import com.agroland.core.ui.theme.AgroSize
import com.agroland.core.ui.theme.AgroSpacing
import com.agroland.core.ui.theme.extendedColors

/** Негізгі мәтіндік өріс — этикетка, қате мәтіні, оптикалық параметрлермен. */
@Composable
fun AgroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false,
    errorText: String? = null,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    prefix: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailing: @Composable () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        maxLines = maxLines,
        isError = isError,
        shape = AgroRadius.field,
        textStyle = MaterialTheme.typography.bodyMedium,
        prefix = prefix?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extendedColors().primaryText,
                )
            }
        },
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        trailingIcon = trailing,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = extendedColors().divider,
            focusedContainerColor = extendedColors().card,
            unfocusedContainerColor = extendedColors().card,
            errorBorderColor = MaterialTheme.colorScheme.error,
        ),
    )
    val helper = errorText?.takeIf { isError } ?: supportingText
    if (!helper.isNullOrBlank()) {
        Text(
            text = helper,
            style = MaterialTheme.typography.labelMedium,
            color = if (isError) MaterialTheme.colorScheme.error else extendedColors().secondaryText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AgroSpacing.screen, top = AgroSpacing.xs),
        )
    }
}

/**
 * Іздеу өрісі — 48dp биіктіктегі толтырылған «пилл».
 *
 * Material [OutlinedTextField] мұнда жарамайды: оның ең кіші биіктігі 56dp,
 * мәжбүрлі `.height(46.dp)` мәтінді тігінен қиып тастайтын (кириллицаның
 * астыңғы жағы көрінбейтін). Сондықтан [BasicTextField] + өз контейнері.
 */
@Composable
fun AgroSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    enabled: Boolean = true,
    /** Тазарту түймесінің орнына (өріс бос кезде) көрсетілетін қосымша әрекет. */
    trailing: (@Composable () -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
) {
    val ext = extendedColors()
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (focused) MaterialTheme.colorScheme.primary else ext.divider

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(AgroSize.field),
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = ext.primaryText),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AgroSize.field)
                    .clip(AgroRadius.pill)
                    // `card` ЕМЕС: өріс көбіне картаның (ақ/қою сұр) үстінде
                    // тұрады да, бірдей түспен фонға сіңіп жоғалатын.
                    .background(ext.grey)
                    .border(1.dp, borderColor, AgroRadius.pill)
                    .padding(start = 14.dp, end = AgroSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = ext.secondaryText,
                    modifier = Modifier.size(20.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = AgroSpacing.sm),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && hint.isNotBlank()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    AgroIconButton(
                        icon = Icons.Rounded.Clear,
                        contentDescription = null,
                        onClick = { onValueChange("") },
                        tint = ext.secondaryText,
                        size = 18.dp,
                    )
                } else {
                    trailing?.invoke()
                }
            }
        },
    )
}
