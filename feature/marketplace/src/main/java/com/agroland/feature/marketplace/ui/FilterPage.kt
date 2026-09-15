package com.agroland.feature.marketplace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroCheckbox
import com.agroland.core.ui.components.AgroChip
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.AgroTextButton
import com.agroland.core.ui.components.BottomActionContainer
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.AnnouncementFilter
import com.agroland.feature.marketplace.data.Category
import com.agroland.feature.marketplace.data.FilterSort

/**
 * FilterPage — draft сүзгі редакторы (Flutter FilterPage): сұрып, баға диапазоны,
 * келісуге болады, категория/сабкатегория + тікелей нәтиже саны.
 * «Қолдану» — нәтижені caller-ге қайтарады (savedStateHandle арқылы).
 */
@Composable
fun FilterPage(
    initialFilter: AnnouncementFilter,
    onBack: () -> Unit,
    onApply: (AnnouncementFilter) -> Unit,
    filterViewModel: FilterViewModel = hiltViewModel(),
    categoriesViewModel: CategoriesViewModel = rememberCategoriesViewModel(),
) {
    val localeTag = LocalConfiguration.current.locales[0]?.toLanguageTag()
    val grouped by categoriesViewModel.grouped.collectAsState()
    val subcategories by categoriesViewModel.subcategories.collectAsState()
    val subLoading by categoriesViewModel.subLoading.collectAsState()
    val count by filterViewModel.count.collectAsState()
    val countLoading by filterViewModel.countLoading.collectAsState()

    // Draft күйі — «Қолдану» басылғанда ғана caller-ге беріледі.
    var sort by remember { mutableStateOf(initialFilter.sort) }
    var minPrice by remember { mutableStateOf(initialFilter.minPrice?.toLong()?.toString() ?: "") }
    var maxPrice by remember { mutableStateOf(initialFilter.maxPrice?.toLong()?.toString() ?: "") }
    var negotiable by remember { mutableStateOf(initialFilter.negotiable ?: false) }
    var negotiableEnabled by remember { mutableStateOf(initialFilter.negotiable != null) }
    var categoryId by remember { mutableStateOf(initialFilter.categoryId) }
    var subcategoryId by remember { mutableStateOf(initialFilter.subcategoryId) }

    fun currentDraft(): AnnouncementFilter = AnnouncementFilter(
        query = initialFilter.query,
        categoryId = categoryId,
        subcategoryId = subcategoryId,
        minPrice = minPrice.toLongOrNull()?.toDouble(),
        maxPrice = maxPrice.toLongOrNull()?.toDouble(),
        negotiable = if (negotiableEnabled) negotiable else null,
        sort = sort,
    )

    // Draft өзгерісі — тікелей эфирдегі санды жаңартады.
    var pickerVisible by remember { mutableStateOf(false) }
    val draft = currentDraft()
    LaunchedEffect(
        draft.categoryId, draft.subcategoryId, draft.minPrice, draft.maxPrice,
        draft.negotiable, draft.sort,
    ) {
        filterViewModel.updateCount(draft)
    }

    var pickedCategory by remember { mutableStateOf<Category?>(null) }
    LaunchedEffect(categoryId) {
        if (categoryId != null) {
            pickedCategory = categoriesViewModel.findCategory(categoryId!!) ?: pickedCategory
            categoriesViewModel.loadSubcategories(categoryId!!)
        } else {
            pickedCategory = null
        }
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(title = stringResource(L10nR.string.filter_title), onBack = onBack)
        },
    ) { inner ->
        Column(modifier = inner.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Сұрыптау.
                SectionTitle(stringResource(L10nR.string.filter_sort))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SortChip(FilterSort.DEFAULT, sort) { sort = it }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SortChip(FilterSort.DATE_DESC, sort) { sort = it }
                        SortChip(FilterSort.PRICE_ASC, sort) { sort = it }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        SortChip(FilterSort.DATE_ASC, sort) { sort = it }
                        SortChip(FilterSort.PRICE_DESC, sort) { sort = it }
                    }
                }

                // Баға диапазоны.
                SectionTitle(stringResource(L10nR.string.filter_price))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AgroTextField(
                        value = minPrice,
                        onValueChange = { minPrice = it.filter(Char::isDigit).take(9) },
                        label = stringResource(L10nR.string.filter_price_from),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    AgroTextField(
                        value = maxPrice,
                        onValueChange = { maxPrice = it.filter(Char::isDigit).take(9) },
                        label = stringResource(L10nR.string.filter_price_to),
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }

                // Баға келісуге болады.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(extendedColors().card)
                        .clickable {
                            if (!negotiableEnabled) {
                                negotiableEnabled = true
                                negotiable = true
                            } else if (negotiable) {
                                negotiable = false
                            } else {
                                negotiableEnabled = false
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(L10nR.string.filter_negotiable),
                            style = MaterialTheme.typography.bodySmall,
                            color = extendedColors().primaryText,
                        )
                        if (!negotiableEnabled) {
                            Text(
                                text = stringResource(L10nR.string.filter_negotiable_any),
                                style = MaterialTheme.typography.labelMedium,
                                color = extendedColors().secondaryText,
                            )
                        }
                    }
                    AgroCheckbox(
                        checked = negotiableEnabled && negotiable,
                        onCheckedChange = { checked ->
                            if (checked) {
                                negotiableEnabled = true
                                negotiable = true
                            } else if (negotiable) {
                                negotiable = false
                            } else {
                                negotiableEnabled = false
                            }
                        },
                    )
                }

                // Категория / сабкатегория.
                SectionTitle(stringResource(L10nR.string.filter_category))
                PickerRow(
                    label = pickedCategory?.localizedName(localeTag)
                        ?: stringResource(L10nR.string.filter_any_category),
                    clearable = categoryId != null,
                    onClear = {
                        categoryId = null
                        subcategoryId = null
                        pickedCategory = null
                    },
                    onClick = { pickerVisible = true },
                )
                subcategoryId?.let { subId ->
                    val sub = subcategories.firstOrNull { it.id == subId }
                    PickerRow(
                        label = sub?.localizedName(localeTag)
                            ?: stringResource(L10nR.string.filter_subcategory),
                        clearable = true,
                        onClear = { subcategoryId = null },
                        onClick = { pickerVisible = true },
                    )
                }
            }

            // Тікелей эфирдегі нәтиже саны + Қолдану.
            BottomActionContainer {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when {
                            countLoading -> stringResource(L10nR.string.common_loading)
                            count != null -> stringResource(L10nR.string.filter_result_count, count!!)
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = extendedColors().secondaryText,
                        modifier = Modifier.weight(1f),
                    )
                    AgroTextButton(
                        text = stringResource(L10nR.string.filter_reset),
                        onClick = {
                            sort = FilterSort.DEFAULT
                            minPrice = ""
                            maxPrice = ""
                            negotiableEnabled = false
                            negotiable = false
                            categoryId = null
                            subcategoryId = null
                            pickedCategory = null
                        },
                    )
                }
                AgroButton(
                    text = stringResource(L10nR.string.filter_apply),
                    onClick = { onApply(currentDraft()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                )
            }
        }
    }

    if (pickerVisible) {
        CategoryPickerDialog(
            grouped = grouped,
            subcategories = subcategories,
            subLoading = subLoading,
            localeTag = localeTag,
            selectedCategoryId = categoryId,
            selectedSubcategoryId = subcategoryId,
            onPickCategory = { category ->
                if (category.id != categoryId) {
                    categoryId = category.id
                    subcategoryId = null
                    pickedCategory = category
                }
                pickerVisible = false
            },
            onPickSubcategory = { sub ->
                subcategoryId = sub.id
                pickerVisible = false
            },
            onDismiss = { pickerVisible = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = extendedColors().primaryText,
    )
}

@Composable
private fun SortChip(
    sort: FilterSort,
    selected: FilterSort,
    onSelect: (FilterSort) -> Unit,
) {
    val labels = mapOf(
        FilterSort.DEFAULT to L10nR.string.filter_sort_default,
        FilterSort.DATE_DESC to L10nR.string.filter_sort_date_desc,
        FilterSort.DATE_ASC to L10nR.string.filter_sort_date_asc,
        FilterSort.PRICE_ASC to L10nR.string.filter_sort_price_asc,
        FilterSort.PRICE_DESC to L10nR.string.filter_sort_price_desc,
    )
    AgroChip(
        text = stringResource(labels.getValue(sort)),
        selected = sort == selected,
        onClick = { onSelect(sort) },
        modifier = Modifier,
    )
}

/** Таңдалған мәнді көрсететін жол — түймені басу диалогды ашады. */
@Composable
private fun PickerRow(
    label: String,
    clearable: Boolean,
    onClear: () -> Unit,
    onClick: () -> Unit,
) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ext.primaryText,
            modifier = Modifier.weight(1f),
        )
        if (clearable) {
            androidx.compose.material3.IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Outlined.Cancel,
                    contentDescription = stringResource(L10nR.string.common_delete),
                    tint = ext.secondaryText,
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = ext.secondaryText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Категориялар диалогы — 6 bucket + таңдалған категорияның сабкатегориялары. */
@Composable
private fun CategoryPickerDialog(
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
    Row(
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