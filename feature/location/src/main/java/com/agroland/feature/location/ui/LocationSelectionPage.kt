package com.agroland.feature.location.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.BottomActionContainer
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.location.data.SelectedLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

/**
 * Локацияны карта арқылы таңдау (LocationSelectionPage / MapsPage, Фаза 7):
 * Google Maps пині + GPS (FusedLocationProviderClient, 10 с шегі) +
 * reverse geocode → каталог ID-лері + қолмен каталог пикерлері.
 *
 * Maps API кілті жоқ болса карта плиткасы орнына хабар картасы көрсетіледі —
 * GPS пен каталог толық жұмыс істейді (функционалдық stub емес).
 */
@Composable
fun LocationSelectionPage(
    prefill: SelectedLocation?,
    onBack: () -> Unit,
    onConfirm: (SelectedLocation) -> Unit,
    viewModel: LocationSelectionViewModel = hiltViewModel(),
    pickerViewModel: LocationPickerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val localeTag = LocalConfiguration.current.locales[0]?.toLanguageTag()
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    val selection = state.selection

    LaunchedEffect(prefill) { viewModel.resetWith(prefill) }

    val mapsKey = remember { context.mapsApiKey() }
    var pin by remember { mutableStateOf<LatLng?>(null) }
    var hasGpsPermission by remember {
        mutableStateOf(
            context.hasLocationPermission(),
        )
    }

    fun locate() {
        viewModel.onGpsStarted()
        scope.launch {
            val location = try {
                withTimeout(GPS_TIMEOUT_MS) {
                    LocationServices.getFusedLocationProviderClient(context).awaitCurrentLocation()
                }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                null
            } catch (_: Exception) {
                null
            }
            if (location != null) {
                pin = LatLng(location.latitude, location.longitude)
                viewModel.onGpsFix(location.latitude, location.longitude)
            } else {
                viewModel.onGpsFailed()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        hasGpsPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasGpsPermission) locate()
    }

    // ---- Каталог пикерлері ----
    var dialog by remember { mutableStateOf<String?>(null) }
    val countries by pickerViewModel.countries.collectAsState()
    val regions by remember(selection.countryId) {
        pickerViewModel.regions(selection.countryId ?: 0)
    }.collectAsState(initial = LocationPickerViewModel.CatalogState())
    val districts by remember(selection.regionId) {
        pickerViewModel.districts(selection.regionId ?: 0)
    }.collectAsState(initial = LocationPickerViewModel.CatalogState())

    fun openDialog(which: String) {
        dialog = which
        when (which) {
            DIALOG_COUNTRY -> pickerViewModel.ensureCountries()
            DIALOG_REGION -> selection.countryId?.let { pickerViewModel.ensureRegions(it) }
            DIALOG_DISTRICT -> selection.regionId?.let { pickerViewModel.ensureDistricts(it) }
        }
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(title = stringResource(L10nR.string.location_title), onBack = onBack)
        },
    ) { inner ->
        Column(modifier = inner.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ---- Карта ----
                val mapModifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                if (mapsKey != null) {
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(
                            pin ?: LatLng(51.1282, 71.4307), // Астана — әдепкі орталық
                            10f,
                        )
                    }
                    LaunchedEffect(pin) {
                        pin?.let {
                            cameraPositionState.animate(
                                com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(it, 13f),
                            )
                        }
                    }
                    GoogleMap(
                        modifier = mapModifier,
                        cameraPositionState = cameraPositionState,
                        onMapClick = { latLng ->
                            pin = latLng
                            viewModel.onPinPlaced(latLng.latitude, latLng.longitude)
                        },
                        properties = MapProperties(isMyLocationEnabled = hasGpsPermission),
                    ) {
                        pin?.let { pinPosition ->
                            Marker(state = rememberMarkerState(position = pinPosition))
                        }
                    }
                } else {
                    Row(
                        modifier = mapModifier.background(extendedColors().card).padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Map,
                            contentDescription = null,
                            tint = extendedColors().secondaryText,
                        )
                        Text(
                            text = stringResource(L10nR.string.location_map_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = extendedColors().secondaryText,
                        )
                    }
                }

                // ---- Пин/гпс статусы ----
                when {
                    state.locating -> StatusText(stringResource(L10nR.string.location_locating))
                    state.gpsFailed -> StatusText(stringResource(L10nR.string.location_gps_failed))
                    state.reverseLoading -> StatusText(stringResource(L10nR.string.location_locating))
                    state.reverseFailed -> StatusText(stringResource(L10nR.string.location_reverse_failed))
                    selection.isNotEmpty -> StatusText(selection.displayLabel())
                    else -> StatusText(stringResource(L10nR.string.location_pin_hint))
                }
                pin?.let {
                    StatusText(
                        stringResource(
                            L10nR.string.location_selected_coords,
                            "%.5f".format(it.latitude),
                            "%.5f".format(it.longitude),
                        ),
                    )
                }

                // ---- Каталогтан таңдау ----
                Text(
                    text = stringResource(L10nR.string.location_catalog_section),
                    style = MaterialTheme.typography.bodySmall,
                    color = extendedColors().primaryText,
                )
                CatalogRow(
                    label = selection.countryName ?: stringResource(L10nR.string.location_choose_country),
                    filled = selection.countryId != null,
                    onClick = { openDialog(DIALOG_COUNTRY) },
                )
                if (selection.countryId != null) {
                    CatalogRow(
                        label = selection.regionName ?: stringResource(L10nR.string.location_choose_region),
                        filled = selection.regionId != null,
                        onClick = { openDialog(DIALOG_REGION) },
                    )
                }
                if (selection.regionId != null) {
                    CatalogRow(
                        label = selection.districtName ?: stringResource(L10nR.string.location_choose_district),
                        filled = selection.districtId != null,
                        onClick = { openDialog(DIALOG_DISTRICT) },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AgroButton(
                        text = stringResource(L10nR.string.location_use_gps),
                        onClick = {
                            if (hasGpsPermission) {
                                locate()
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            }
                        },
                        loading = state.locating,
                        containerColor = extendedColors().grey,
                        contentColor = extendedColors().primaryText,
                        modifier = Modifier.weight(1f),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        pin?.let { current ->
                            AgroButton(
                                text = stringResource(L10nR.string.location_open_in_maps),
                                onClick = { openInExternalMaps(context, current.latitude, current.longitude) },
                                containerColor = extendedColors().grey,
                                contentColor = extendedColors().primaryText,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            BottomActionContainer {
                AgroButton(
                    text = stringResource(L10nR.string.location_select),
                    enabled = state.canConfirm,
                    onClick = { onConfirm(state.selection) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
            }
        }
    }

    val activeDialog = dialog
    if (activeDialog != null) {
        val title = when (activeDialog) {
            DIALOG_COUNTRY -> stringResource(L10nR.string.location_choose_country)
            DIALOG_REGION -> stringResource(L10nR.string.location_choose_region)
            else -> stringResource(L10nR.string.location_choose_district)
        }
        val dialogState = when (activeDialog) {
            DIALOG_COUNTRY -> countries
            DIALOG_REGION -> regions
            else -> districts
        }
        val selectedId = when (activeDialog) {
            DIALOG_COUNTRY -> selection.countryId
            DIALOG_REGION -> selection.regionId
            else -> selection.districtId
        }
        CatalogPickerDialog(
            title = title,
            state = dialogState,
            localeTag = localeTag,
            selectedId = selectedId,
            onSelect = { item ->
                val name = item.localizedName(localeTag)
                when (activeDialog) {
                    DIALOG_COUNTRY -> viewModel.selectCountry(item.id, name)
                    DIALOG_REGION -> viewModel.selectRegion(item.id, name)
                    else -> viewModel.selectDistrict(item.id, name)
                }
                dialog = null
            },
            onRetry = {
                when (activeDialog) {
                    DIALOG_COUNTRY -> pickerViewModel.retryCountries()
                    DIALOG_REGION -> selection.countryId?.let { pickerViewModel.retryRegions(it) }
                    else -> selection.regionId?.let { pickerViewModel.retryDistricts(it) }
                }
            },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = extendedColors().secondaryText,
    )
}

@Composable
private fun CatalogRow(label: String, filled: Boolean, onClick: () -> Unit) {
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
            imageVector = Icons.Rounded.LocationOn,
            contentDescription = null,
            tint = if (filled) MaterialTheme.colorScheme.primary else extendedColors().secondaryText,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (filled) extendedColors().primaryText else extendedColors().secondaryText,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---- Android plumbing ----

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/** Maps API кілті Manifest-тен — бос/шаблон болса null (карта режимі өшіреді). */
private fun Context.mapsApiKey(): String? = try {
    val info = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    info.metaData?.getString("com.google.android.geo.API_KEY")
        ?.takeIf { it.isNotBlank() && !it.contains('$') }
} catch (_: Exception) {
    null
}

/** getCurrentLocation Task-ын suspend ету — 10 с шегі caller жағында. */
@SuppressLint("MissingPermission")
private suspend fun com.google.android.gms.location.FusedLocationProviderClient.awaitCurrentLocation(): Location? =
    suspendCancellableCoroutine { cont ->
        val cancellation = com.google.android.gms.tasks.CancellationTokenSource()
        val task = getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
        task.addOnSuccessListener { location -> if (cont.isActive) cont.resumeWith(Result.success(location)) }
        task.addOnFailureListener { if (cont.isActive) cont.resumeWith(Result.success(null)) }
        cont.invokeOnCancellation { cancellation.cancel() }
    }

private const val GPS_TIMEOUT_MS = 10_000L
private const val DIALOG_COUNTRY = "country"
private const val DIALOG_REGION = "region"
private const val DIALOG_DISTRICT = "district"