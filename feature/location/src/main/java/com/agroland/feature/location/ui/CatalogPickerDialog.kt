package com.agroland.feature.location.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroSearchField
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.location.data.CatalogLocation

/**
 * Каталог таңдау диалогі (LocationPickerViewModel күйімен). Ел/облыс/аудан
 * деңгейлерінің бәрі осы диалог арқылы көрсетіледі — DistrictListPage/RegionFilterView
 * баламасы (Flutter парақтық пикерлерінің орнына M3 Dialog).
 */
@Composable
fun CatalogPickerDialog(
    title: String,
    state: LocationPickerViewModel.CatalogState,
    localeTag: String?,
    selectedId: Int?,
    onSelect: (CatalogLocation) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    var search by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(extendedColors().card)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = extendedColors().primaryText,
            )
            AgroSearchField(
                value = search,
                onValueChange = { search = it },
                hint = stringResource(L10nR.string.location_search_hint),
            )
            val visible = state.items.filter {
                search.isBlank() || it.localizedName(localeTag).contains(search.trim(), ignoreCase = true)
            }
            when {
                state.loading -> LoadingWidget()
                state.error -> ErrorWithRetry(onRetry = onRetry)
                visible.isEmpty() -> Text(
                    text = stringResource(L10nR.string.location_nothing_found),
                    style = MaterialTheme.typography.bodySmall,
                    color = extendedColors().secondaryText,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
                else -> LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    contentPadding = PaddingValues(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(visible, key = { it.id }) { item ->
                        val selected = item.id == selectedId
                        Text(
                            text = item.localizedName(localeTag),
                            style = MaterialTheme.typography.bodySmall,
                            color = extendedColors().primaryText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) extendedColors().grey else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { onSelect(item) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}