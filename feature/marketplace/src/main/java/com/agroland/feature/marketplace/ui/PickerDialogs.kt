package com.agroland.feature.marketplace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.MeasurementUnit

/**
 * ListPickerDialog — ортақ жалпы таңдау диалогы (өлшем бірлігі, мекенжай
 * т.б.). CreateAdPage пен MakeOfferPage қайта қолданады.
 */
@Composable
internal fun <T> ListPickerDialog(
    title: String,
    items: List<T>,
    label: @Composable (T) -> String,
    selected: T?,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val ext = extendedColors()
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ext.card)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = ext.primaryText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (items.isEmpty()) {
                Text(
                    text = stringResource(L10nR.string.create_no_locations),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.secondaryText,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.height(360.dp)) {
                    items(items) { item ->
                        val isSelected = item == selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) ext.grey else Color.Transparent)
                                .clickable { onSelect(item) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = label(item),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else ext.primaryText,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Өлшем бірлігінің локализатталған атауы — strings.xml (measurement_unit_*). */
@Composable
internal fun unitLabel(unit: MeasurementUnit): String = stringResource(
    when (unit) {
        MeasurementUnit.PIECE -> L10nR.string.measurement_unit_piece
        MeasurementUnit.KILOGRAM -> L10nR.string.measurement_unit_kilogram
        MeasurementUnit.TON -> L10nR.string.measurement_unit_ton
        MeasurementUnit.LITER -> L10nR.string.measurement_unit_liter
        MeasurementUnit.HECTARE -> L10nR.string.measurement_unit_hectare
        MeasurementUnit.SQUARE_METER -> L10nR.string.measurement_unit_square_meter
        MeasurementUnit.CUBIC_METER -> L10nR.string.measurement_unit_cubic_meter
        MeasurementUnit.BAG -> L10nR.string.measurement_unit_bag
        MeasurementUnit.CENTNER -> L10nR.string.measurement_unit_centner
        MeasurementUnit.HEAD -> L10nR.string.measurement_unit_head
        MeasurementUnit.PAIR -> L10nR.string.measurement_unit_pair
        MeasurementUnit.METER -> L10nR.string.measurement_unit_meter
        MeasurementUnit.BOX -> L10nR.string.measurement_unit_box
    },
)