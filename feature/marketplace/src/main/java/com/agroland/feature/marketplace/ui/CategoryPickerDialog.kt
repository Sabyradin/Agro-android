package com.agroland.feature.marketplace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.Category

/**
 * CategoryPickerDialog — ортақ категория таңдау диалогы (сүзгі, жарнама жасау,
 * сұраныс). 6 bucket + таңдалған категорияның сабкатегориялары.
 */
@Composable
fun CategoryPickerDialog(
    grouped: Map<String, List<Category>>,
    subcategories: List<Category>,
    subLoading: Boolean,
    localeTag: String?,
    selectedCategoryId: Int?,
    selectedSubcategoryId: Int?,
    onPickCategory: (Category) -> Unit,
    onPickSubcategory: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    val ext = extendedColors()
    val bucketTitles = mapOf(
        "crops" to L10nR.string.category_bucket_crops,
        "livestock" to L10nR.string.category_bucket_livestock,
        "products" to L10nR.string.category_bucket_products,
        "technology" to L10nR.string.category_bucket_technology,
        "services" to L10nR.string.category_bucket_services,
        "other" to L10nR.string.category_bucket_other,
    )
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ext.card)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = stringResource(L10nR.string.filter_category),
                style = MaterialTheme.typography.bodyMedium,
                color = ext.primaryText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            val hasAny = grouped.values.any { it.isNotEmpty() }
            if (!hasAny && subLoading) {
                LoadingWidget(Modifier.fillMaxWidth().padding(16.dp))
            } else if (!hasAny) {
                EmptyView(title = stringResource(L10nR.string.empty_generic_title))
            } else {
                LazyColumn(modifier = Modifier.height(420.dp)) {
                    grouped.forEach { (bucket, categories) ->
                        if (categories.isEmpty()) return@forEach
                        item(key = "header_$bucket") {
                            Text(
                                text = stringResource(bucketTitles[bucket] ?: L10nR.string.category_bucket_other),
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.secondaryText,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        items(categories, key = { "cat_${it.id}" }) { category ->
                            PickerDialogRow(
                                label = category.localizedName(localeTag),
                                selected = category.id == selectedCategoryId,
                                onClick = { onPickCategory(category) },
                            )
                        }
                    }
                    if (selectedCategoryId != null && subcategories.isNotEmpty()) {
                        item(key = "header_sub") {
                            Text(
                                text = stringResource(L10nR.string.filter_subcategory),
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.secondaryText,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        items(subcategories, key = { "sub_${it.id}" }) { sub ->
                            PickerDialogRow(
                                label = sub.localizedName(localeTag),
                                selected = sub.id == selectedSubcategoryId,
                                onClick = { onPickSubcategory(sub) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerDialogRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ext = extendedColors()
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) ext.grey else ext.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.primary else ext.primaryText,
        )
    }
}