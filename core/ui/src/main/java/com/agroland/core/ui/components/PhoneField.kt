package com.agroland.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.agroland.core.common.phone.CountryPhoneMask
import com.agroland.core.ui.theme.AgroRadius
import com.agroland.core.ui.theme.AgroSpacing
import com.agroland.core.ui.theme.extendedColors

/**
 * Телефон өрісі — iOS нұсқасымен бірдей: сол жақта ту + ел коды (таңдалады),
 * тік бөлгіш, содан кейін маска бойынша форматталатын нөмір.
 *
 * Өріс тек ЦИФРЛАРДЫ ұстайды; backend-ке жіберетін толық пішінді
 * [CountryPhoneMask.toE164] береді.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgroPhoneField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    country: CountryPhoneMask,
    onCountryChange: (CountryPhoneMask) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
    enabled: Boolean = true,
) {
    val ext = extendedColors()
    var pickerVisible by remember { mutableStateOf(false) }
    val borderColor = if (isError) MaterialTheme.colorScheme.error else ext.divider

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(AgroRadius.field)
                .background(ext.grey)
                .border(1.dp, borderColor, AgroRadius.field),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(AgroRadius.field)
                    .clickable(enabled = enabled) { pickerVisible = true }
                    .padding(start = 14.dp, end = AgroSpacing.sm, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = country.flag, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "+${country.phoneCode}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ext.primaryText,
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = ext.secondaryText,
                    modifier = Modifier.size(18.dp),
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(ext.divider),
            )
            BasicTextField(
                value = digits,
                onValueChange = { raw -> onDigitsChange(country.normalizeInput(raw)) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AgroSpacing.md),
                enabled = enabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = ext.primaryText),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                visualTransformation = remember(country) { PhoneMaskTransformation(country) },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (digits.isEmpty()) {
                            Text(
                                text = country.mask.replace('0', '0'),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ext.secondaryText,
                            )
                        }
                        inner()
                    }
                },
            )
        }
        if (isError && !errorText.isNullOrBlank()) {
            Spacer(Modifier.height(AgroSpacing.xs))
            Text(
                text = errorText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = AgroSpacing.xs),
            )
        }
    }

    if (pickerVisible) {
        ModalBottomSheet(
            onDismissRequest = { pickerVisible = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = ext.card,
        ) {
            Column(modifier = Modifier.padding(bottom = AgroSpacing.xl)) {
                CountryPhoneMask.ALL.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCountryChange(item)
                                onDigitsChange("")
                                pickerVisible = false
                            }
                            .padding(horizontal = AgroSpacing.lg, vertical = AgroSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AgroSpacing.md),
                    ) {
                        Text(text = item.flag, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = item.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.primaryText,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "+${item.phoneCode}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.secondaryText,
                        )
                        if (item.code == country.code) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Тек цифрлардан тұратын мәнді маска бойынша көрсетеді: `7011234567`
 * → `(701) 123 4567`. Курсор орны [OffsetMapping] арқылы дұрыс есептеледі.
 */
private class PhoneMaskTransformation(
    private val country: CountryPhoneMask,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(country.digitCount)
        val formatted = country.format(digits)

        // digit индексі → форматталған жолдағы позиция.
        val digitPositions = ArrayList<Int>(digits.length)
        var d = 0
        for ((index, ch) in formatted.withIndex()) {
            if (ch.isDigit()) {
                digitPositions.add(index)
                d++
                if (d >= digits.length) break
            }
        }

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 0 -> 0
                offset > digitPositions.size -> formatted.length
                else -> digitPositions[offset - 1] + 1
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                var count = 0
                for (i in 0 until minOf(offset, formatted.length)) {
                    if (formatted[i].isDigit()) count++
                }
                return count
            }
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}
