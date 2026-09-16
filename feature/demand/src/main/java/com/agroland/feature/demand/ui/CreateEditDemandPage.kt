package com.agroland.feature.demand.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.BottomActionContainer
import com.agroland.core.ui.components.ShimmerCard
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.ui.CategoryPickerDialog
import com.agroland.feature.marketplace.ui.CategoriesViewModel
import androidx.hilt.navigation.compose.hiltViewModel

/** Валюта таңдау чиптері — Flutter demands_service валюталары. */
private val DEMAND_CURRENCIES = listOf("KZT", "USD", "RUB", "CNY")

/**
 * Сұраныс жасау/өңдеу формасы (demandId = -1 → жаңа). Категория/сабкатегория
 * опционал — ортақ CategoryPickerDialog арқылы таңдалады.
 */
@Composable
fun CreateEditDemandPage(
    onBack: () -> Unit,
    viewModel: CreateEditDemandViewModel = hiltViewModel(),
    categoriesViewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val grouped by categoriesViewModel.grouped.collectAsStateWithLifecycle()
    val subcategories by categoriesViewModel.subcategories.collectAsStateWithLifecycle()
    val subLoading by categoriesViewModel.subLoading.collectAsStateWithLifecycle()
    val localeTag = LocalConfiguration.current.locales[0]?.toLanguageTag()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showValidation by remember { mutableStateOf(false) }
    var pickerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        categoriesViewModel.refresh()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DemandEvent.ShowError -> snackbarHostState.showSnackbar(
                    event.error.displayText(
                        context.getString(L10nR.string.error_no_internet),
                        context.getString(L10nR.string.error_generic_message),
                    ),
                )
                DemandEvent.Created -> {
                    snackbarHostState.showSnackbar(context.getString(L10nR.string.demand_created))
                    onBack()
                }
                DemandEvent.Updated -> {
                    snackbarHostState.showSnackbar(context.getString(L10nR.string.demand_updated))
                    onBack()
                }
                else -> Unit
            }
        }
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(
                    if (state.demandId > 0) {
                        L10nR.string.demand_edit_title
                    } else {
                        L10nR.string.demand_create_title
                    },
                ),
                onBack = onBack,
            )
        },
        bottomBar = {
            BottomActionContainer {
                AgroButton(
                    text = stringResource(L10nR.string.common_save),
                    onClick = {
                        if (state.title.isBlank()) {
                            showValidation = true
                        } else {
                            viewModel.save()
                        }
                    },
                    loading = state.saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        },
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.loading -> Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ShimmerCard()
                    ShimmerCard()
                    ShimmerCard()
                }
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val ext = extendedColors()

                    // ── Негізгі ақпарат ──
                    FormCard {
                        AgroTextField(
                            value = state.title,
                            onValueChange = { text -> viewModel.update { form -> form.copy(title = text) } },
                            label = stringResource(L10nR.string.demand_field_title),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = showValidation && state.title.isBlank(),
                            errorText = if (showValidation && state.title.isBlank()) {
                                stringResource(L10nR.string.demand_validation_title)
                            } else {
                                null
                            },
                        )
                        Spacer(Modifier.padding(top = 4.dp))
                        AgroTextField(
                            value = state.description,
                            onValueChange = { text -> viewModel.update { form -> form.copy(description = text) } },
                            label = stringResource(L10nR.string.demand_field_description_hint),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // ── Баға мен мөлшер ──
                    FormCard {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AgroTextField(
                                value = state.maxPrice,
                                onValueChange = { text ->
                                    viewModel.update {
                                        it.copy(maxPrice = text.filter { ch -> ch.isDigit() || ch == '.' })
                                    }
                                },
                                label = stringResource(L10nR.string.demand_field_max_price),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            )
                            AgroTextField(
                                value = state.quantity,
                                onValueChange = { text ->
                                    viewModel.update {
                                        it.copy(quantity = text.filter { ch -> ch.isDigit() || ch == '.' })
                                    }
                                },
                                label = stringResource(L10nR.string.demand_field_quantity),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            )
                        }
                        Spacer(Modifier.padding(top = 12.dp))
                        Text(
                            text = stringResource(L10nR.string.demand_field_currency),
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.secondaryText,
                        )
                        Spacer(Modifier.padding(top = 6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DEMAND_CURRENCIES.forEach { code ->
                                CurrencyChip(
                                    code = code,
                                    selected = state.currency == code,
                                    onClick = { viewModel.update { it.copy(currency = code) } },
                                )
                            }
                        }
                        Spacer(Modifier.padding(top = 12.dp))
                        AgroTextField(
                            value = state.measurementUnit,
                            onValueChange = { text -> viewModel.update { form -> form.copy(measurementUnit = text) } },
                            label = stringResource(L10nR.string.demand_field_measurement),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }

                    // ── Категория (опционал) ──
                    FormCard {
                        Text(
                            text = stringResource(L10nR.string.filter_category),
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.secondaryText,
                        )
                        Spacer(Modifier.padding(top = 8.dp))
                        PickRow(
                            label = state.categoryLabel
                                ?: stringResource(L10nR.string.create_pick_category),
                            hasValue = state.categoryId != null,
                            onClick = { pickerVisible = true },
                        )
                        state.subcategoryLabel?.let { label ->
                            Spacer(Modifier.padding(top = 8.dp))
                            Text(
                                text = stringResource(L10nR.string.filter_subcategory),
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.secondaryText,
                            )
                            Spacer(Modifier.padding(top = 8.dp))
                            PickRow(label = label, hasValue = true, onClick = { pickerVisible = true })
                        }
                    }
                    Spacer(Modifier.padding(bottom = 90.dp))
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp),
            )
        }
    }

    if (pickerVisible) {
        CategoryPickerDialog(
            grouped = grouped,
            subcategories = subcategories,
            subLoading = subLoading,
            localeTag = localeTag,
            selectedCategoryId = state.categoryId,
            selectedSubcategoryId = state.subcategoryId,
            onPickCategory = { category ->
                if (category.id != state.categoryId) {
                    viewModel.update {
                        it.copy(
                            categoryId = category.id,
                            subcategoryId = null,
                            subcategoryLabel = null,
                            categoryLabel = category.localizedName(localeTag),
                        )
                    }
                    categoriesViewModel.loadSubcategories(category.id)
                }
                pickerVisible = false
            },
            onPickSubcategory = { sub ->
                viewModel.update {
                    it.copy(subcategoryId = sub.id, subcategoryLabel = sub.localizedName(localeTag))
                }
                pickerVisible = false
            },
            onDismiss = { pickerVisible = false },
        )
    }
}

/** Форма бөлімі — дөңгелек карточка тұсындағы өрістер. */
@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(extendedColors().card)
            .padding(14.dp),
    ) {
        content()
    }
}

/** Таңдау жолы — карточка түріндегі түйме (атау + chevron). */
@Composable
private fun PickRow(label: String, hasValue: Boolean, onClick: () -> Unit) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (hasValue) ext.primaryLight else ext.backgroundLight)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (hasValue) FontWeight.SemiBold else FontWeight.Normal,
            color = if (hasValue) ext.primaryText else ext.secondaryText,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = ext.secondaryText,
        )
    }
}

/** Валюта чипі — KZT/USD/RUB/CNY. */
@Composable
private fun CurrencyChip(code: String, selected: Boolean, onClick: () -> Unit) {
    val ext = extendedColors()
    Text(
        text = code,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (selected) {
            androidx.compose.ui.graphics.Color.White
        } else {
            ext.secondaryText
        },
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else ext.backgroundLight,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}