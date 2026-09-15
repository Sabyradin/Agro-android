package com.agroland.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.BottomActionContainer
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.location.data.CatalogLocation
import com.agroland.feature.location.data.SelectedLocation
import com.agroland.feature.location.ui.CatalogPickerDialog
import com.agroland.feature.location.ui.LocationPickerViewModel
import com.agroland.feature.profile.data.UserLocation

/**
 * Мекенжай қосу/өңдеу (AddressEditPage, Фаза 7) — Flutter LocationSelectionPage
 * адрес режимінің баламасы: ел/облыс/аудан каталогы + көше/үй мәтіндері
 * (+ карта арқылы таңдау — LocationSelectionPage → SelectedLocation).
 * Body: house/street/country/area/province/locality + каталог ID + координаталар.
 * Өңдеу = POST жаңа + DELETE ескі (PATCH жоқ); 3-тен асқаны өшіріледі.
 */
@Composable
fun AddressEditPage(
    locationId: Long,
    mapSelection: SelectedLocation?,
    onConsumeMapSelection: () -> Unit,
    onOpenMapPicker: (SelectedLocation?) -> Unit,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ProfileViewModel = rememberProfileViewModel(),
    locationViewModel: LocationPickerViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val localeTag = LocalConfiguration.current.locales[0]?.toLanguageTag()

    val existing: UserLocation? = profile?.locations?.firstOrNull { it.id == locationId }?.takeIf { locationId != 0L }

    var street by remember { mutableStateOf("") }
    var house by remember { mutableStateOf("") }
    var catalog by remember { mutableStateOf<SelectedLocation?>(null) }
    var initialized by remember { mutableStateOf(locationId == 0L) }

    // Profile кештен келген соң prefill (бір рет).
    LaunchedEffect(existing) {
        if (!initialized && existing != null) {
            street = existing.street.orEmpty()
            house = existing.house.orEmpty()
            catalog = existing.let {
                SelectedLocation(
                    countryId = it.countryId,
                    countryName = it.country,
                    regionId = it.regionId,
                    regionName = it.area ?: it.province,
                    districtId = it.districtId,
                    districtName = it.locality,
                    latitude = it.latitude,
                    longitude = it.longitude,
                )
            }
            initialized = true
        }
    }

    // Картамен таңдау нәтижесі.
    LaunchedEffect(mapSelection) {
        if (mapSelection != null) {
            catalog = mapSelection
            onConsumeMapSelection()
        }
    }

    val genericError = stringResource(L10nR.string.error_generic_message)
    val networkError = stringResource(L10nR.string.error_no_internet)
    val savedToast = stringResource(L10nR.string.profile_saved_toast)
    LaunchedEffect(Unit) {
        if (profile == null) viewModel.refresh()
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ShowError -> {
                    val text = event.error.backendMessage
                        ?: if (event.error.isNetwork) networkError else genericError
                    snackbar.showSnackbar(text)
                }
                ProfileEvent.Saved -> {
                    snackbar.showSnackbar(savedToast)
                    onSaved()
                }
                else -> Unit
            }
        }
    }

    var dialog by remember { mutableStateOf<String?>(null) }
    val countries by locationViewModel.countries.collectAsState()
    val regions by remember(catalog?.countryId) {
        locationViewModel.regions(catalog?.countryId ?: 0)
    }.collectAsState(initial = LocationPickerViewModel.CatalogState())
    val districts by remember(catalog?.regionId) {
        locationViewModel.districts(catalog?.regionId ?: 0)
    }.collectAsState(initial = LocationPickerViewModel.CatalogState())

    // Flutter _buildLocationModel: ел+облыс+аудан таңдалмайынша сақтау ашылмайды.
    val catalogComplete = catalog?.let { it.countryId != null && it.regionId != null && it.districtId != null } == true

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(
                    if (locationId == 0L) L10nR.string.address_add else L10nR.string.address_edit,
                ),
                onBack = onBack,
            )
        },
    ) { inner ->
        if (locationId != 0L && existing == null) {
            if (loading) {
                LoadingWidget()
            } else {
                // Профиль жүктелмеген болса — әрекет етуге мүмкіндік бер.
                EmptyBackContent(inner, onBack)
            }
        } else {
            Column(modifier = inner.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AgroTextField(
                        value = street,
                        onValueChange = { street = it },
                        label = stringResource(L10nR.string.address_street_hint),
                    )
                    AgroTextField(
                        value = house,
                        onValueChange = { house = it },
                        label = stringResource(L10nR.string.address_house_hint),
                    )

                    // ---- Ел / облыс / аудан (каталог) ----
                    CatalogFieldRow(
                        label = catalog?.countryName ?: stringResource(L10nR.string.location_choose_country),
                        filled = catalog?.countryId != null,
                        onClick = {
                            dialog = "country"
                            locationViewModel.ensureCountries()
                        },
                    )
                    if (catalog?.countryId != null) {
                        CatalogFieldRow(
                            label = catalog?.regionName ?: stringResource(L10nR.string.location_choose_region),
                            filled = catalog?.regionId != null,
                            onClick = {
                                dialog = "region"
                                catalog?.countryId?.let { locationViewModel.ensureRegions(it) }
                            },
                        )
                    }
                    if (catalog?.regionId != null) {
                        CatalogFieldRow(
                            label = catalog?.districtName ?: stringResource(L10nR.string.location_choose_district),
                            filled = catalog?.districtId != null,
                            onClick = {
                                dialog = "district"
                                catalog?.regionId?.let { locationViewModel.ensureDistricts(it) }
                            },
                        )
                    }

                    CatalogFieldRow(
                        label = stringResource(L10nR.string.create_pick_location_map),
                        filled = catalog?.latitude != null,
                        onClick = { onOpenMapPicker(catalog) },
                    )
                }
                BottomActionContainer {
                    AgroButton(
                        text = stringResource(L10nR.string.common_save),
                        enabled = !saving && catalogComplete,
                        loading = saving,
                        onClick = {
                            viewModel.saveLocation(
                                existing,
                                street.trim(),
                                house.trim(),
                                catalog,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }
                SnackbarHost(hostState = snackbar)
            }
        }
    }

    val activeDialog = dialog
    if (activeDialog != null) {
        val dialogState = when (activeDialog) {
            "country" -> countries
            "region" -> regions
            else -> districts
        }
        val dialogTitle = when (activeDialog) {
            "country" -> stringResource(L10nR.string.location_choose_country)
            "region" -> stringResource(L10nR.string.location_choose_region)
            else -> stringResource(L10nR.string.location_choose_district)
        }
        CatalogPickerDialog(
            title = dialogTitle,
            state = dialogState,
            localeTag = localeTag,
            selectedId = when (activeDialog) {
                "country" -> catalog?.countryId
                "region" -> catalog?.regionId
                else -> catalog?.districtId
            },
            onSelect = { item: CatalogLocation ->
                val name = item.localizedName(localeTag)
                catalog = when (activeDialog) {
                    "country" -> SelectedLocation(countryId = item.id, countryName = name)
                    "region" -> catalog?.copy(
                        regionId = item.id,
                        regionName = name,
                        districtId = null,
                        districtName = null,
                    )
                    else -> catalog?.copy(districtId = item.id, districtName = name)
                }
                dialog = null
            },
            onRetry = {
                when (activeDialog) {
                    "country" -> locationViewModel.retryCountries()
                    "region" -> catalog?.countryId?.let { locationViewModel.retryRegions(it) }
                    else -> catalog?.regionId?.let { locationViewModel.retryDistricts(it) }
                }
            },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun CatalogFieldRow(label: String, filled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(extendedColors().card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (filled) Icons.Outlined.LocationOn else Icons.Outlined.Map,
            contentDescription = null,
            tint = if (filled) MaterialTheme.colorScheme.primary else extendedColors().secondaryText,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (filled) extendedColors().primaryText else extendedColors().secondaryText,
        )
    }
}

@Composable
private fun EmptyBackContent(modifier: Modifier, onBack: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AgroButton(
            text = stringResource(L10nR.string.common_back),
            onClick = onBack,
        )
    }
}