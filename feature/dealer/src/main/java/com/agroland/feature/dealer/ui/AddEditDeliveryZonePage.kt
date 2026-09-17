package com.agroland.feature.dealer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroSwitch
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.BottomActionContainer
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.dealer.data.DeliveryZoneDraft
import com.agroland.feature.dealer.data.ZonePriceItem
import com.agroland.feature.dealer.data.displayText
import com.agroland.feature.dealer.ui.AddEditDeliveryZoneViewModel.Event
import com.agroland.feature.location.ui.CatalogPickerDialog
import com.agroland.feature.location.ui.LocationPickerViewModel
import kotlinx.coroutines.launch

/**
 * Жеткізу аймағын қосу/өңдеу — Flutter AddEditLogisticsPage (1:1):
 * атау, ел/облыс (міндетті), мин/макс күн, аптакүн таңдаушысы, уақыт
 * (TimePicker), аудан бағалары (district picker + баға диалогы), ескерту,
 * белсенді тумблері, астында Сақтау/Болдырмау.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDeliveryZonePage(
    onBack: () -> Unit,
    viewModel: AddEditDeliveryZoneViewModel = hiltViewModel(),
    pickerViewModel: LocationPickerViewModel = hiltViewModel(),
) {
    val editingZone by viewModel.editingZone.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val localeTag = LocalConfiguration.current.locales[0]?.toLanguageTag()

    // ── Форма күйі ──
    var name by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var daysMin by remember { mutableStateOf("") }
    var daysMax by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var timeStart by remember { mutableStateOf("09:00") }
    var timeEnd by remember { mutableStateOf("18:00") }
    var isActive by remember { mutableStateOf(true) }
    var countryId by remember { mutableStateOf<Int?>(null) }
    var regionId by remember { mutableStateOf<Int?>(null) }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var zonePrices by remember { mutableStateOf(listOf<ZonePriceItem>()) }
    var prefilled by remember { mutableStateOf(false) }

    // Каталог күйлері — LocationPickerViewModel кеші (id өзгерсе Flow қайта жүктеледі).
    val countries by pickerViewModel.countries.collectAsState()
    val regions by pickerViewModel.regions(countryId ?: -1)
        .collectAsState(initial = LocationPickerViewModel.CatalogState())
    val districts by pickerViewModel.districts(regionId ?: -1)
        .collectAsState(initial = LocationPickerViewModel.CatalogState())

    // Өңдеу prefill — зона жүктелгенде бір рет толтырылады.
    LaunchedEffect(editingZone) {
        val z = editingZone ?: return@LaunchedEffect
        if (!prefilled) {
            prefilled = true
            name = z.name.ifBlank { z.displayName }
            cost = z.deliveryCost.toInt().toString()
            daysMin = z.deliveryDaysMin?.toString().orEmpty()
            daysMax = z.deliveryDaysMax?.toString().orEmpty()
            note = z.note
            timeStart = z.timeStart ?: "09:00"
            timeEnd = z.timeEnd ?: "18:00"
            isActive = z.isActive
            countryId = z.countryId
            regionId = z.regionId
            selectedDays = z.availableDays.toSet()
            zonePrices = z.zonePrices
            pickerViewModel.ensureRegions(z.countryId)
        }
    }

    val networkError = stringResource(L10nR.string.error_no_internet)
    val genericError = stringResource(L10nR.string.error_generic_message)
    val validAmount = stringResource(L10nR.string.dealer_enter_valid_amount)
    val chooseCountry = stringResource(L10nR.string.location_choose_country)
    val chooseRegion = stringResource(L10nR.string.location_choose_region)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Диалог күйлері
    var openPicker by remember { mutableStateOf<String?>(null) } // country|region|district
    var timePickerFor by remember { mutableStateOf<String?>(null) } // start|end
    var priceDialogFor by remember { mutableStateOf<Int?>(null) } // zonePrices index
    var priceInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is Event.ShowError -> scope.launch {
                    snackbar.showSnackbar(event.error.displayText(networkError, genericError))
                }
                Event.Saved -> onBack()
            }
        }
    }

    val countryName = countries.items.firstOrNull { it.id == countryId }?.localizedName(localeTag) ?: ""
    val regionName = regions.items.firstOrNull { it.id == regionId }?.localizedName(localeTag) ?: ""

    fun trySave() {
        val costValue = cost.toDoubleOrNull()
        if (costValue == null || costValue <= 0.0) {
            scope.launch { snackbar.showSnackbar(validAmount) }
            return
        }
        if (countryId == null) {
            scope.launch { snackbar.showSnackbar(chooseCountry) }
            return
        }
        if (regionId == null) {
            scope.launch { snackbar.showSnackbar(chooseRegion) }
            return
        }
        viewModel.save(
            DeliveryZoneDraft(
                name = name,
                countryId = countryId!!,
                regionId = regionId!!,
                deliveryCost = costValue,
                deliveryDaysMin = daysMin.toIntOrNull(),
                deliveryDaysMax = daysMax.toIntOrNull(),
                isActive = isActive,
                note = note,
                timeStart = timeStart,
                timeEnd = timeEnd,
                zonePrices = zonePrices,
                availableDays = selectedDays,
            ),
        )
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(
                    if (viewModel.zoneId > 0) L10nR.string.dealer_edit_zone else L10nR.string.dealer_add_zone,
                ),
                onBack = onBack,
            )
        },
        bottomBar = {
            BottomActionContainer {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(stringResource(L10nR.string.common_cancel))
                    }
                    AgroButton(
                        text = stringResource(L10nR.string.common_save),
                        onClick = { trySave() },
                        enabled = !saving,
                        loading = saving,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    ) { modifier ->
        Box(modifier = modifier) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                FormSection(stringResource(L10nR.string.dealer_logistics_name)) {
                    AgroTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = stringResource(L10nR.string.dealer_zone_name_hint),
                    )
                }
                FormSection(stringResource(L10nR.string.location_choose_country)) {
                    DropdownField(
                        hint = stringResource(L10nR.string.location_choose_country),
                        value = countryName,
                        onClick = {
                            pickerViewModel.ensureCountries()
                            openPicker = "country"
                        },
                    )
                }
                FormSection(stringResource(L10nR.string.location_choose_region)) {
                    DropdownField(
                        hint = stringResource(L10nR.string.location_choose_region),
                        value = regionName,
                        onClick = {
                            if (countryId == null) {
                                scope.launch { snackbar.showSnackbar(chooseCountry) }
                            } else {
                                pickerViewModel.ensureRegions(countryId!!)
                                openPicker = "region"
                            }
                        },
                    )
                }
                FormSection(stringResource(L10nR.string.dealer_min_days)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AgroTextField(
                            value = daysMin,
                            onValueChange = { daysMin = it },
                            label = stringResource(L10nR.string.dealer_min_days),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        AgroTextField(
                            value = daysMax,
                            onValueChange = { daysMax = it },
                            label = stringResource(L10nR.string.dealer_max_days),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                FormSection(stringResource(L10nR.string.dealer_available_days)) {
                    WeekdaySelector(
                        selected = selectedDays,
                        onToggle = { day ->
                            selectedDays = if (day in selectedDays) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                        },
                    )
                }
                FormSection(stringResource(L10nR.string.dealer_delivery_time)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeField(
                            label = stringResource(L10nR.string.dealer_time_start),
                            value = timeStart,
                            onClick = { timePickerFor = "start" },
                            modifier = Modifier.weight(1f),
                        )
                        TimeField(
                            label = stringResource(L10nR.string.dealer_time_end),
                            value = timeEnd,
                            onClick = { timePickerFor = "end" },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                FormSection(stringResource(L10nR.string.dealer_zones_prices)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        zonePrices.forEachIndexed { index, item ->
                            ZonePriceRow(
                                item = item,
                                onRemove = { zonePrices = zonePrices.filterIndexed { i, _ -> i != index } },
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                if (regionId == null) {
                                    scope.launch { snackbar.showSnackbar(chooseRegion) }
                                } else {
                                    pickerViewModel.ensureDistricts(regionId!!)
                                    openPicker = "district"
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(L10nR.string.dealer_add_district),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                FormSection(stringResource(L10nR.string.dealer_note)) {
                    AgroTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = stringResource(L10nR.string.dealer_note),
                        singleLine = false,
                        maxLines = 3,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(L10nR.string.my_ads_status_active),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extendedColors().primaryText,
                        modifier = Modifier.weight(1f),
                    )
                    AgroSwitch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
            SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    // ── Каталог таңдау диалогтары ──
    when (openPicker) {
        "country" -> CatalogPickerDialog(
            title = stringResource(L10nR.string.location_choose_country),
            state = countries,
            localeTag = localeTag,
            selectedId = countryId,
            onSelect = { item ->
                // Ел өзгерсе — облыс таңдауы ыдырайды (облыстар елге байланысты).
                if (countryId != item.id) regionId = null
                countryId = item.id
                openPicker = null
            },
            onRetry = { pickerViewModel.ensureCountries() },
            onDismiss = { openPicker = null },
        )
        "region" -> CatalogPickerDialog(
            title = stringResource(L10nR.string.location_choose_region),
            state = regions,
            localeTag = localeTag,
            selectedId = regionId,
            onSelect = { item ->
                regionId = item.id
                openPicker = null
            },
            onRetry = { countryId?.let { pickerViewModel.ensureRegions(it) } },
            onDismiss = { openPicker = null },
        )
        "district" -> CatalogPickerDialog(
            title = stringResource(L10nR.string.location_choose_district),
            state = districts,
            localeTag = localeTag,
            selectedId = null,
            onSelect = { item ->
                zonePrices = zonePrices + ZonePriceItem(
                    districtId = item.id,
                    districtName = item.localizedName(localeTag),
                    price = 0.0,
                )
                priceInput = "0"
                priceDialogFor = zonePrices.lastIndex
                openPicker = null
            },
            onRetry = { regionId?.let { pickerViewModel.ensureDistricts(it) } },
            onDismiss = { openPicker = null },
        )
    }

    // ── Уақыт таңдау ──
    if (timePickerFor != null) {
        val initial = parseTime(if (timePickerFor == "start") timeStart else timeEnd)
        val timeState = rememberTimePickerState(
            initialHour = initial.first,
            initialMinute = initial.second,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { timePickerFor = null },
            title = {
                Text(
                    stringResource(
                        if (timePickerFor == "start") {
                            L10nR.string.dealer_time_start
                        } else {
                            L10nR.string.dealer_time_end
                        },
                    ),
                )
            },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val text = "%02d:%02d".format(timeState.hour, timeState.minute)
                        if (timePickerFor == "start") timeStart = text else timeEnd = text
                        timePickerFor = null
                    },
                ) { Text(stringResource(L10nR.string.dealer_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { timePickerFor = null }) {
                    Text(stringResource(L10nR.string.common_cancel))
                }
            },
        )
    }

    // ── Аудан бағасы диалогы ──
    priceDialogFor?.let { index ->
        val item = zonePrices.getOrNull(index) ?: return@let
        AlertDialog(
            onDismissRequest = { priceDialogFor = null },
            title = {
                Text(
                    stringResource(L10nR.string.dealer_delivery_cost) + " — " + item.districtName,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            text = {
                AgroTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = stringResource(L10nR.string.dealer_district_price),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val price = priceInput.toDoubleOrNull() ?: 0.0
                        zonePrices = zonePrices.mapIndexed { i, cur ->
                            if (i == index) cur.copy(price = price) else cur
                        }
                        priceDialogFor = null
                    },
                ) { Text(stringResource(L10nR.string.dealer_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { priceDialogFor = null }) {
                    Text(stringResource(L10nR.string.common_cancel))
                }
            },
        )
    }
}

// ── Кішкентәй көмекшілер ──

/** "09:00" → (9, 0). */
private fun parseTime(value: String): Pair<Int, Int> {
    val parts = value.split(":")
    return (parts.getOrNull(0)?.toIntOrNull() ?: 9) to (parts.getOrNull(1)?.toIntOrNull() ?: 0)
}

/** Бөлім тақырыбы + контент. */
@Composable
private fun FormSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.W600,
            color = extendedColors().primaryText,
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

/** Ел/облыс таңдау өрісі — басылатын контейнер + chevron. */
@Composable
private fun DropdownField(hint: String, value: String, onClick: () -> Unit) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(0.5.dp, ext.divider, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value.ifEmpty { hint },
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.isEmpty()) ext.secondaryText else ext.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = ext.secondaryText,
        )
    }
}

/** Уақыт өрісі — readOnly, TimePicker диалогын ашады. */
@Composable
private fun TimeField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ext = extendedColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(0.5.dp, ext.divider, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = ext.primaryText,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ext.secondaryText,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Rounded.Schedule,
            contentDescription = null,
            tint = ext.secondaryText,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Аптакүн таңдаушысы — 7 түйме (1=Дс..7=Жс), көк таңдалған. */
@Composable
private fun WeekdaySelector(selected: Set<Int>, onToggle: (Int) -> Unit) {
    val ext = extendedColors()
    val labels = listOf(
        L10nR.string.dealer_weekday_1,
        L10nR.string.dealer_weekday_2,
        L10nR.string.dealer_weekday_3,
        L10nR.string.dealer_weekday_4,
        L10nR.string.dealer_weekday_5,
        L10nR.string.dealer_weekday_6,
        L10nR.string.dealer_weekday_7,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { index, labelRes ->
            val day = index + 1
            val isSelected = day in selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else ext.card)
                    .border(
                        0.5.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else ext.divider,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { onToggle(day) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.W600 else FontWeight.Normal,
                    color = if (isSelected) ext.white else ext.secondaryText,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Аудан + баға жолы — атау, баға, жою X. */
@Composable
private fun ZonePriceRow(item: ZonePriceItem, onRemove: () -> Unit) {
    val ext = extendedColors()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(3f)
                .clip(RoundedCornerShape(8.dp))
                .border(0.5.dp, ext.divider, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = item.districtName.ifEmpty { stringResource(L10nR.string.location_choose_district) },
                style = MaterialTheme.typography.bodySmall,
                color = ext.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${item.price.toInt()} ₸",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.W600,
            color = ext.primaryText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.weight(2f),
        )
        TextButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}