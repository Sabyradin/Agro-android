package com.agroland.feature.demand.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.BottomActionContainer
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.ShimmerCard
import com.agroland.core.ui.theme.extendedColors
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Сұраныс деталы — толық мәлімет + әрекеттер: өңдеу, өшіру (растаумен),
 * белсендіру/тоқтату (спек §3.5 — Flutter-де бұл UI мүлде жоқ).
 */
@Composable
fun DemandDetailPage(
    onBack: () -> Unit,
    onOpenEdit: (Long) -> Unit,
    viewModel: DemandDetailViewModel = hiltViewModel(),
) {
    val demand by viewModel.demand.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val actionLoading by viewModel.actionLoading.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DemandEvent.ShowError -> snackbarHostState.showSnackbar(
                    event.error.displayText(
                        context.getString(L10nR.string.error_no_internet),
                        context.getString(L10nR.string.error_generic_message),
                    ),
                )
                DemandEvent.Deleted -> onBack()
                DemandEvent.Activated ->
                    snackbarHostState.showSnackbar(context.getString(L10nR.string.demand_activated))
                DemandEvent.Deactivated ->
                    snackbarHostState.showSnackbar(context.getString(L10nR.string.demand_deactivated))
                else -> Unit
            }
        }
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = demand?.title ?: "",
                onBack = onBack,
                actions = {
                    if (demand != null) {
                        IconButton(onClick = { onOpenEdit(viewModel.demandId) }) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = extendedColors().primaryText,
                            )
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (demand != null) {
                BottomActionContainer {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AgroButton(
                            text = stringResource(
                                if (demand!!.isActive) {
                                    L10nR.string.ad_action_deactivate
                                } else {
                                    L10nR.string.ad_action_activate
                                },
                            ),
                            onClick = {
                                if (demand!!.isActive) viewModel.deactivate() else viewModel.activate()
                            },
                            loading = actionLoading,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            enabled = !actionLoading,
                            modifier = Modifier
                                .width(64.dp)
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        },
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                loading -> Column {
                    ShimmerCard()
                    ShimmerCard()
                }
                error != null && demand == null -> CenteredContent(Modifier.fillMaxSize()) {
                    ErrorWithRetry(onRetry = viewModel::load)
                }
                demand == null -> EmptyView(
                    modifier = Modifier.fillMaxSize(),
                    title = stringResource(L10nR.string.nothing_found),
                )
                else -> {
                    val item = demand!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        val ext = extendedColors()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ext.primaryText,
                                modifier = Modifier.weight(1f),
                            )
                            DemandStatusChip(isActive = item.isActive)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ext.card)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (!item.description.isNullOrBlank()) {
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ext.primaryText,
                                )
                            }
                            if (item.maxPrice != null) {
                                DetailRow(
                                    label = stringResource(L10nR.string.demand_field_max_price),
                                    value = PriceFormatter.format(item.maxPrice, item.currency ?: "₸"),
                                )
                            }
                            if (item.quantity != null) {
                                DetailRow(
                                    label = stringResource(L10nR.string.demand_field_quantity),
                                    value = (item.quantity?.toInt()?.toString() ?: item.quantity.toString()) +
                                        (item.measurementUnit?.let { " $it" } ?: ""),
                                )
                            }
                            item.categoryName?.let {
                                DetailRow(label = stringResource(L10nR.string.filter_category), value = it)
                            }
                            item.subcategoryName?.let {
                                DetailRow(label = stringResource(L10nR.string.filter_subcategory), value = it)
                            }
                            DetailRow(
                                label = stringResource(L10nR.string.demand_created_at),
                                value = DateFormatter.formatDateTime(item.createdAt),
                            )
                        }
                    }
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete()
                }) {
                    Text(
                        text = stringResource(L10nR.string.common_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(L10nR.string.common_cancel))
                }
            },
            title = {
                Text(stringResource(L10nR.string.demand_delete_confirm))
            },
        )
    }
}

/** Атау — мән жолы. */
@Composable
private fun DetailRow(label: String, value: String) {
    val ext = extendedColors()
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ext.secondaryText,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = ext.primaryText,
        )
    }
}