package com.agroland.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Place
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroIconButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.BottomActionContainer
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.profile.data.UserLocation

/**
 * Мекенжайлар (ProfileAddressesPage): POST/PATCH/DELETE /user/location.
 * Тізім + «қосу» түймесі, өңдеу/өшіру әрекеттері.
 */
@Composable
fun ProfileAddressesPage(onBack: () -> Unit) {
    val viewModel = rememberProfileViewModel()
    val profile by viewModel.profile.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var editing by remember { mutableStateOf<UserLocation?>(null) }
    var dialogOpen by remember { mutableStateOf(false) }

    val genericError = stringResource(L10nR.string.error_generic_message)
    val networkError = stringResource(L10nR.string.error_no_internet)
    val deletedToast = stringResource(L10nR.string.address_deleted_toast)

    LaunchedEffect(Unit) {
        if (profile == null) viewModel.refresh()
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ShowError -> {
                    val text = event.error.backendMessage
                        ?: if (event.error.isNetwork) networkError else genericError
                    snackbar.showSnackbar(text)
                }
                ProfileEvent.Saved -> dialogOpen = false
                ProfileEvent.LocationDeleted -> snackbar.showSnackbar(deletedToast)
                else -> Unit
            }
        }
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.profile_addresses),
                onBack = onBack,
            )
        },
    ) { modifier ->
        Box(modifier = modifier) {
            val locations = profile?.locations.orEmpty()
            when {
                loading && profile == null -> LoadingWidget()
                locations.isEmpty() -> CenteredContent {
                    EmptyView(
                        icon = Icons.Outlined.LocationOn,
                        title = stringResource(L10nR.string.address_empty_title),
                        message = stringResource(L10nR.string.address_empty_message),
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(locations, key = { it.id }) { location ->
                        LocationTile(
                            location = location,
                            onEdit = {
                                editing = location
                                dialogOpen = true
                            },
                            onDelete = { viewModel.deleteLocation(location.id) },
                        )
                    }
                }
            }

            BottomActionContainer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                com.agroland.core.ui.components.AgroButton(
                    text = stringResource(L10nR.string.address_add),
                    onClick = {
                        editing = null
                        dialogOpen = true
                    },
                    enabled = !saving,
                    loading = saving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    if (dialogOpen) {
        LocationDialog(
            existing = editing,
            saving = saving,
            onSave = { title, address, city ->
                viewModel.saveLocation(editing, title, address, city)
            },
            onDismiss = { dialogOpen = false },
        )
    }
}

@Composable
private fun LocationTile(
    location: UserLocation,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ext.card)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = location.title ?: location.address ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = ext.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = listOfNotNull(location.city, location.address)
                .filter { it.isNotBlank() }
                .joinToString(", ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        AgroIconButton(
            icon = Icons.Outlined.Edit,
            contentDescription = stringResource(L10nR.string.common_edit),
            tint = ext.secondaryText,
            onClick = onEdit,
        )
        AgroIconButton(
            icon = Icons.Outlined.Delete,
            contentDescription = stringResource(L10nR.string.common_delete),
            tint = MaterialTheme.colorScheme.error,
            onClick = onDelete,
        )
    }
}

/** Қосу/өңдеу диалогі — атау, мекенжай, қала. */
@Composable
private fun LocationDialog(
    existing: UserLocation?,
    saving: Boolean,
    onSave: (title: String, address: String, city: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var address by remember { mutableStateOf(existing?.address ?: "") }
    var city by remember { mutableStateOf(existing?.city ?: "") }
    var titleError by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf(false) }

    val titleEmpty = stringResource(L10nR.string.address_title_error)
    val addressEmpty = stringResource(L10nR.string.address_address_error)

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
                text = stringResource(
                    if (existing == null) L10nR.string.address_add else L10nR.string.address_edit,
                ),
                style = MaterialTheme.typography.titleSmall,
                color = extendedColors().primaryText,
            )
            com.agroland.core.ui.components.AgroTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = false
                },
                label = stringResource(L10nR.string.address_title_hint),
                isError = titleError,
                errorText = titleEmpty,
            )
            com.agroland.core.ui.components.AgroTextField(
                value = address,
                onValueChange = {
                    address = it
                    addressError = false
                },
                label = stringResource(L10nR.string.address_address_hint),
                isError = addressError,
                errorText = addressEmpty,
            )
            com.agroland.core.ui.components.AgroTextField(
                value = city,
                onValueChange = { city = it },
                label = stringResource(L10nR.string.address_city_hint),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    com.agroland.core.ui.components.AgroButton(
                        text = stringResource(L10nR.string.common_cancel),
                        onClick = onDismiss,
                        containerColor = extendedColors().grey,
                        contentColor = extendedColors().primaryText,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    com.agroland.core.ui.components.AgroButton(
                        text = stringResource(L10nR.string.common_save),
                        enabled = !saving,
                        loading = saving,
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                            } else if (address.isBlank()) {
                                addressError = true
                            } else {
                                onSave(title.trim(), address.trim(), city.trim().takeIf { it.isNotEmpty() })
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}