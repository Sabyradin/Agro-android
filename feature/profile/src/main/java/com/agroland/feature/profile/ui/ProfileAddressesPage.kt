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
fun ProfileAddressesPage(onBack: () -> Unit, onEditLocation: (UserLocation?) -> Unit) {
    val viewModel = rememberProfileViewModel()
    val profile by viewModel.profile.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val snackbar = remember { SnackbarHostState() }

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
                ProfileEvent.Saved -> Unit // AddressEditPage өзі артқа шығады
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
                            onEdit = { onEditLocation(location) },
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
                    onClick = { onEditLocation(null) },
                    enabled = !saving,
                    loading = saving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
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
            // Flutter title/subtitle: locality, street, house / province, country.
            Text(
                text = location.title,
                style = MaterialTheme.typography.bodySmall,
                color = ext.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (location.subtitle.isNotEmpty()) {
                Text(
                    text = location.subtitle,
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
