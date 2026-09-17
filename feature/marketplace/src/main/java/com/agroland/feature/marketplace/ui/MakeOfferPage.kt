package com.agroland.feature.marketplace.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.PinDrop
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.AdDraft
import com.agroland.feature.marketplace.data.MeasurementUnit
import kotlinx.coroutines.launch

/**
 * MakeOfferPage — сұраныс құру (POST /demands), iOS «Ұсыныс жасау» макеті:
 * байланыс ақпараты (мекенжай) → сұраныс (тақырып, сипаттама, санат) →
 * баға шегі, саны, өлшем бірлігі, кілт сөздер → «Сұраныс жариялау».
 *
 * [header] берілсе (CreateHubPage) — қойындылары бар тақырып көрсетіледі.
 */
@Composable
fun MakeOfferPage(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    header: (@Composable () -> Unit)? = null,
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
    var addressPickerVisible by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }
    LaunchedEffect(draft.categoryId) {
        draft.categoryId?.let { categoriesViewModel.loadSubcategories(it) }
    }

    val invalidField = if (showErrors) draft.validate() else null
    val addressLabel = locations.firstOrNull { it.id == draft.userLocationId }?.fullAddress?.takeIf { it.isNotBlank() }

    AgroScaffold(
        topBar = {
            if (header != null) {
                header()
            } else {
                CreateHeader(
                    title = stringResource(L10nR.string.create_tab_offer),
                    closeLabel = stringResource(L10nR.string.common_close),
                    onClose = onBack,
                )
            }
        },
    ) { inner ->
        Box(modifier = inner.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    FormSection(title = stringResource(L10nR.string.create_section_contact)) {
                        FormSelectRow(
                            label = stringResource(L10nR.string.create_address),
                            value = addressLabel ?: stringResource(L10nR.string.create_specify_address),
                            leadingIcon = Icons.Outlined.PinDrop,
                            valueIsPlaceholder = true,
                            updown = true,
                            isError = invalidField == AdDraft.FIELD_LOCATION,
                            onClick = { addressPickerVisible = true },
                        )
                    }

                    FormSection(title = stringResource(L10nR.string.offer_section_request)) {
                        FormTextRow(
                            value = draft.title,
                            onValueChange = { value -> viewModel.updateDraft { it.copy(title = value.take(120)) } },
                            placeholder = stringResource(L10nR.string.offer_title_hint),
                            isError = invalidField == AdDraft.FIELD_TITLE,
                        )
                        FormDivider()
                        FormTextRow(
                            value = draft.description,
                            onValueChange = { value ->
                                viewModel.updateDraft { it.copy(description = value.take(1000)) }
                            },
                            placeholder = stringResource(L10nR.string.offer_description_hint),
                            singleLine = false,
                            minHeight = 110.dp,
                            isError = invalidField == AdDraft.FIELD_DESCRIPTION,
                        )
                        FormDivider()
                        FormSelectRow(
                            label = stringResource(L10nR.string.create_category),
                            value = categoryName(grouped, draft.categoryId, localeTag)?.let { category ->
                                subcategories.firstOrNull { it.id == draft.subcategoryId }
                                    ?.localizedName(localeTag)
                                    ?.let { "$category · $it" } ?: category
                            } ?: stringResource(L10nR.string.create_choose_category),
                            valueIsPlaceholder = draft.categoryId == null,
                            isError = invalidField == AdDraft.FIELD_CATEGORY ||
                                invalidField == AdDraft.FIELD_SUBCATEGORY,
                            onClick = { categoryPickerVisible = true },
                        )
                    }

                    FormCard {
                        FormTextRow(
                            value = draft.maxPrice,
                            onValueChange = { value ->
                                viewModel.updateDraft { it.copy(maxPrice = value.filter(Char::isDigit).take(12)) }
                            },
                            placeholder = stringResource(L10nR.string.offer_max_price_hint),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailing = {
                                Text(text = "₸", fontSize = 18.sp, color = extendedColors().secondaryText)
                            },
                        )
                        FormDivider()
                        FormTextRow(
                            value = draft.quantity,
                            onValueChange = { value ->
                                viewModel.updateDraft { it.copy(quantity = value.filter(Char::isDigit).take(9)) }
                            },
                            placeholder = stringResource(L10nR.string.offer_quantity_hint),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        FormDivider()
                        FormSelectRow(
                            label = stringResource(L10nR.string.create_unit),
                            value = draft.measurementUnit?.let { unitLabel(it) }
                                ?: stringResource(L10nR.string.create_choose),
                            valueIsPlaceholder = true,
                            updown = true,
                            onClick = { unitPickerVisible = true },
                        )
                        FormDivider()
                        OfferKeywordsRow(
                            keywords = draft.keywords,
                            onChange = { keywords -> viewModel.updateDraft { it.copy(keywords = keywords) } },
                        )
                    }
                }

                CreateBottomBar {
                    CreateBarButton(
                        text = stringResource(L10nR.string.offer_publish),
                        icon = Icons.Outlined.Campaign,
                        loading = submitting,
                        onClick = {
                            val field = draft.validate()
                            if (field == null) {
                                viewModel.submit()
                            } else {
                                showErrors = true
                                validationMessages[field]?.let { message ->
                                    scope.launch { snackbar.showSnackbar(message) }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
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

    if (addressPickerVisible) {
        // Сұраныс тек сақталған мекенжаймен жіберіледі (user_location_id) — карта жоқ.
        AddressPickerSheet(
            locations = locations,
            selectedId = draft.userLocationId,
            onPickSaved = { location ->
                viewModel.updateDraft { it.copy(userLocationId = location.id) }
                addressPickerVisible = false
            },
            onPickMap = null,
            onDismiss = { addressPickerVisible = false },
        )
    }
}

/** Кілт сөздер — үтір арқылы бір жолда (жергілікті мәтін + таза тізім). */
@Composable
private fun OfferKeywordsRow(keywords: List<String>, onChange: (List<String>) -> Unit) {
    var text by remember { mutableStateOf(keywords.joinToString(", ")) }
    FormTextRow(
        value = text,
        onValueChange = { value ->
            text = value.take(300)
            onChange(text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct())
        },
        placeholder = stringResource(L10nR.string.offer_keywords_hint),
    )
}
