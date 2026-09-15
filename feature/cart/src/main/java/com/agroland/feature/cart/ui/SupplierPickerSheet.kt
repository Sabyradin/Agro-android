package com.agroland.feature.cart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.theme.extendedColors

/**
 * SupplierPickerSheet — Flutter supplier_picker_sheet: себетте бірнеше бизнес
 * болса, төлем бір бизнеске ғана жүргізіледі (Halyk бір order_id алады).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierPickerSheet(
    groups: List<SupplierGroup>,
    onChoose: (SupplierGroup) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(L10nR.string.cart_supplier_title),
                style = MaterialTheme.typography.bodyMedium,
                color = extendedColors().primaryText,
            )
            Text(
                text = stringResource(L10nR.string.cart_supplier_hint),
                style = MaterialTheme.typography.labelMedium,
                color = extendedColors().secondaryText,
            )
            groups.forEachIndexed { index, group ->
                SupplierGroupCard(
                    number = index + 1,
                    group = group,
                    onChoose = { onChoose(group) },
                )
            }
        }
    }
}

@Composable
private fun SupplierGroupCard(
    number: Int,
    group: SupplierGroup,
    onChoose: () -> Unit,
) {
    val ext = extendedColors()
    val itemCount = group.items.size
    val subtotal = group.items.sumOf { item ->
        (item.announcement?.base?.price ?: 0.0) * item.quantity
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ext.card)
            .clickable(onClick = onChoose)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(L10nR.string.cart_supplier_number, number),
            style = MaterialTheme.typography.labelLarge,
            color = ext.primaryText,
        )
        Text(
            text = pluralStringResource(L10nR.plurals.cart_dealer_items, itemCount, itemCount),
            style = MaterialTheme.typography.labelMedium,
            color = ext.secondaryText,
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = PriceFormatter.formatPrecise(subtotal, "₸"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            AgroButton(
                text = stringResource(L10nR.string.cart_supplier_pay),
                onClick = onChoose,
                modifier = Modifier.height(44.dp),
            )
        }
    }
}