package com.agroland.feature.dealer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroIconButton
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.dealer.data.DeliveryZone
import com.agroland.feature.dealer.data.displayText
import com.agroland.feature.dealer.ui.DeliveryZonesViewModel.Event
import kotlinx.coroutines.launch

/**
 * Логистика табы — Flutter DealerLogisticsTab (1:1): зона карточкалары
 * (атау + белсенді белгі + өңдеу/жою) және «Логистика қосу» FAB.
 * Жою — растау диалогы арқылы (404 tolerance).
 */
@Composable
fun DealerLogisticsTab(
    onAddZone: () -> Unit,
    onEditZone: (Long) -> Unit,
    viewModel: DeliveryZonesViewModel = hiltViewModel(),
) {
    val zones by viewModel.zones.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val deleteInProgress by viewModel.deleteInProgress.collectAsState()

    val networkError = stringResource(L10nR.string.error_no_internet)
    val genericError = stringResource(L10nR.string.error_generic_message)
    val deletedText = stringResource(L10nR.string.dealer_delete_zone)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var deleteCandidate by remember { mutableStateOf<DeliveryZone?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is Event.ShowError -> scope.launch {
                    snackbar.showSnackbar(event.error.displayText(networkError, genericError))
                }
                Event.ZoneDeleted -> scope.launch { snackbar.showSnackbar(deletedText) }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            loading -> LoadingWidget()
            error != null -> CenteredContent {
                ErrorWithRetry(onRetry = viewModel::load, message = error!!.displayText(networkError, genericError))
            }
            zones.isEmpty() -> CenteredContent {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Inbox,
                        contentDescription = null,
                        tint = extendedColors().secondaryText,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(L10nR.string.dealer_delivery_zones),
                        style = MaterialTheme.typography.titleMedium,
                        color = extendedColors().secondaryText,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(L10nR.string.dealer_add_zone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extendedColors().secondaryText,
                    )
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(zones, key = { it.id }) { zone ->
                    ZoneCard(
                        zone = zone,
                        deleting = deleteInProgress == zone.id,
                        onEdit = { onEditZone(zone.id) },
                        onDelete = { deleteCandidate = zone },
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAddZone,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = extendedColors().white,
                )
            },
            text = {
                Text(
                    text = stringResource(L10nR.string.dealer_add_logistics),
                    fontWeight = FontWeight.W600,
                    color = extendedColors().white,
                )
            },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    deleteCandidate?.let { zone ->
        ConfirmDialog(
            title = stringResource(L10nR.string.dealer_delete_zone),
            message = zone.displayName + "?",
            confirmLabel = stringResource(L10nR.string.dealer_confirm),
            onDismiss = { deleteCandidate = null },
            onConfirm = {
                viewModel.delete(zone.id)
                deleteCandidate = null
            },
        )
    }
}

/** Зона карточкасы — атау + күй белгісі + өңдеу/жою + баға/күндер (Flutter _ZoneCard). */
@Composable
private fun ZoneCard(
    zone: DeliveryZone,
    deleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val ext = extendedColors()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ext.card)
            .border(0.5.dp, ext.divider, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = zone.displayName.ifBlank {
                    stringResource(L10nR.string.dealer_zone_fallback, zone.id)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W600,
                color = ext.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            ZoneActiveBadge(isActive = zone.isActive)
            Spacer(Modifier.width(4.dp))
            AgroIconButton(
                icon = Icons.Rounded.Edit,
                contentDescription = stringResource(L10nR.string.dealer_edit_zone),
                onClick = onEdit,
                modifier = Modifier.size(36.dp),
            )
            AgroIconButton(
                icon = Icons.Rounded.Delete,
                contentDescription = stringResource(L10nR.string.dealer_delete_zone),
                onClick = onDelete,
                enabled = !deleting,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.CreditCard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = PriceFormatter.format(zone.deliveryCost),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.W600,
                color = ext.primaryText,
            )
            Spacer(Modifier.width(16.dp))
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = ext.secondaryText,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = zone.daysRange,
                style = MaterialTheme.typography.bodySmall,
                color = ext.secondaryText,
            )
        }
    }
}

/** Белсенді/өшірулі белгісі — жасыл / сұр. */
@Composable
private fun ZoneActiveBadge(isActive: Boolean) {
    val color = if (isActive) Color(0xFF4CAF50) else extendedColors().secondaryText
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = stringResource(
                if (isActive) L10nR.string.my_ads_status_active else L10nR.string.my_ads_status_inactive,
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.W600,
            color = color,
        )
    }
}