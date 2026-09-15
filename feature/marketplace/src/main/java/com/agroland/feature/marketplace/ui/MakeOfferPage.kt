package com.agroland.feature.marketplace.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.BottomActionContainer
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.AdDraft
import com.agroland.feature.marketplace.data.MeasurementUnit
import kotlinx.coroutines.launch

/**
 * MakeOfferPage — сұраныс құру (make_offer_page.dart):
 * «Мынаны іздеймін, баға шегі мынау» жариялауы — POST /demands.
 * Flutter-дегі CreateOrOfferPage-тің offer режимі.
 */
@Composable
fun MakeOfferPage(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    viewModel: MakeOfferViewModel = hiltViewModel(),
    categoriesViewModel: CategoriesViewModel = rememberCategoriesViewModel(),
) {
    val draft by viewModel.draft.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val submitting by viewModel.submitting.collectAsState()

    val localeTag = LocalConfiguration.current.locales[0]?.toLanguageTag()
    val grouped by categoriesViewModel.grouped.collectAsState()
    val subcategories by categoriesViewModel.subcategories.collectAsState()
    val subLoading by categoriesViewModel.subLoading.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val noInternetText = stringResource(L10nR.string.error_no_internet)
    val genericErrorText = stringResource(L10nR.string.error_generic_message)
    val validationMessages = mapOf(
        AdDraft.FIELD_TITLE to stringResource(L10nR.string.validation_title),
        AdDraft.FIELD_DESCRIPTION to stringResource(L10nR.string.validation_description),
        AdDraft.FIELD_CATEGORY to stringResource(L10nR.string.validation_category),
        AdDraft.FIELD_SUBCATEGORY to stringResource(L10nR.string.validation_subcategory),
        AdDraft.FIELD_LOCATION to stringResource(L10nR.string.validation_location),
    )
    val offerCreatedText = stringResource(L10nR.string.offer_created_toast)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MakeOfferViewModel.Event.Submitted -> {
                    snackbar.showSnackbar(offerCreatedText)
                    onSubmitted()
                }
                is MakeOfferViewModel.Event.ShowError ->
                    snackbar.showSnackbar(event.error.displayText(noInternetText, genericErrorText))
            }
        }
    }

    var categoryPickerVisible by remember { mutableStateOf(false) }
    var unitPickerVisible by remember { mutableStateOf(false) }
    var locationPickerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(draft.categoryId) {
        draft.categoryId?.let { categoriesViewModel.loadSubcategories(it) }
    }

    val invalidField = draft.validate()

    AgroScaffold(
        topBar = {
            AgroAppBar(title = stringResource(L10nR.string.make_offer_title), onBack = onBack)
        },
    ) { inner ->
        Box(modifier = inner.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AgroTextField(
                        value = draft.title,
                        onValueChange = { value -> viewModel.updateDraft { it.copy(title = value.take(120)) } },
                        label = stringResource(L10nR.string.create_field_title),
                        isError = invalidField == AdDraft.FIELD_TITLE,
                        singleLine = false,
                        maxLines = 2,
                    )
                    AgroTextField(
                        value = draft.description,
                        onValueChange = { value -> viewModel.updateDraft { it.copy(description = value.take(4000)) } },
                        label = stringResource(L10nR.string.create_field_description),
                        isError = invalidField == AdDraft.FIELD_DESCRIPTION,
                        singleLine = false,
                        maxLines = 6,
                    )
                    AgroTextField(
                        value = draft.maxPrice,
                        onValueChange = { value ->
                            viewModel.updateDraft { it.copy(maxPrice = value.filter(Char::isDigit).take(12)) }
                        },
                        label = stringResource(L10nR.string.offer_field_max_price),
                        supportingText = stringResource(L10nR.string.offer_field_max_price_hint),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    AgroTextField(
                        value = draft.quantity,
                        onValueChange = { value ->
                            viewModel.updateDraft { it.copy(quantity = value.filter(Char::isDigit).take(9)) }
                        },
                        label = stringResource(L10nR.string.offer_field_quantity),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )

                    OfferSelectRow(
                        icon = Icons.Outlined.Category,
                        label = grouped.values.flatten().firstOrNull { it.id == draft.categoryId }
                            ?.localizedName(localeTag)
                            ?: stringResource(L10nR.string.create_pick_category),
                        isError = invalidField == AdDraft.FIELD_CATEGORY,
                        onClick = { categoryPickerVisible = true },
                    )
                    OfferSelectRow(
                        icon = Icons.Outlined.Category,
                        label = subcategories.firstOrNull { it.id == draft.subcategoryId }
                            ?.localizedName(localeTag)
                            ?: stringResource(L10nR.string.create_pick_subcategory),
                        isError = invalidField == AdDraft.FIELD_SUBCATEGORY,
                        onClick = { categoryPickerVisible = true },
                    )
                    OfferSelectRow(
                        icon = Icons.Outlined.Straighten,
                        label = draft.measurementUnit?.let { unitLabel(it) }
                            ?: stringResource(L10nR.string.create_pick_unit),
                        onClick = { unitPickerVisible = true },
                    )
                    OfferSelectRow(
                        icon = Icons.Outlined.LocationOn,
                        label = locations.firstOrNull { it.id == draft.userLocationId }
                            ?.fullAddress
                            ?.takeIf { it.isNotBlank() }
                            ?: stringResource(L10nR.string.create_pick_location),
                        isError = invalidField == AdDraft.FIELD_LOCATION,
                        onClick = { locationPickerVisible = true },
                    )
                }

                BottomActionContainer {
                    AgroButton(
                        text = stringResource(L10nR.string.offer_submit),
                        onClick = {
                            val field = draft.validate()
                            if (field == null) {
                                viewModel.submit()
                            } else {
                                validationMessages[field]?.let { message ->
                                    scope.launch { snackbar.showSnackbar(message) }
                                }
                            }
                        },
                        loading = submitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                    )
                }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (categoryPickerVisible) {
        CategoryPickerDialog(
            grouped = grouped,
            subcategories = subcategories,
            subLoading = subLoading,
            localeTag = localeTag,
            selectedCategoryId = draft.categoryId,
            selectedSubcategoryId = draft.subcategoryId,
            onPickCategory = { category ->
                if (category.id != draft.categoryId) {
                    viewModel.updateDraft { it.copy(categoryId = category.id, subcategoryId = null) }
                }
                categoryPickerVisible = false
            },
            onPickSubcategory = { sub ->
                viewModel.updateDraft { it.copy(subcategoryId = sub.id) }
                categoryPickerVisible = false
            },
            onDismiss = { categoryPickerVisible = false },
        )
    }

    if (unitPickerVisible) {
        ListPickerDialog(
            title = stringResource(L10nR.string.create_pick_unit),
            items = MeasurementUnit.entries,
            label = { unitLabel(it) },
            selected = draft.measurementUnit,
            onSelect = { unit ->
                viewModel.updateDraft { it.copy(measurementUnit = unit) }
                unitPickerVisible = false
            },
            onDismiss = { unitPickerVisible = false },
        )
    }

    if (locationPickerVisible) {
        ListPickerDialog(
            title = stringResource(L10nR.string.create_pick_location),
            items = locations,
            label = { it.fullAddress },
            selected = locations.firstOrNull { it.id == draft.userLocationId },
            onSelect = { location ->
                viewModel.updateDraft { it.copy(userLocationId = location.id) }
                locationPickerVisible = false
            },
            onDismiss = { locationPickerVisible = false },
        )
    }
}

/** Таңдау жолы — иконка + мән, бос болса placeholder көк түспен. */
@Composable
private fun OfferSelectRow(
    icon: ImageVector,
    label: String,
    isError: Boolean = false,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isError) MaterialTheme.colorScheme.error else ext.secondaryText,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error else ext.primaryText,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
    }
}