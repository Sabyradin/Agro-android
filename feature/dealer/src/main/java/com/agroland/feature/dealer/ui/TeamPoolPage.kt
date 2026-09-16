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
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroIconButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.dealer.data.TeamPoolOrder
import com.agroland.feature.dealer.data.displayText
import com.agroland.feature.dealer.ui.TeamPoolViewModel.Event

/**
 * Team Pool — Flutter TeamPoolPage (1:1): бөлінбеген тапсырыстар
 * («барлығының көрінісі»), claim батырмасымен. Қызметкерлер иконкасы
 * тек director рөліне (DealerAccessViewModel арқылы гейт).
 */
@Composable
fun TeamPoolPage(
    onBack: () -> Unit,
    onOpenEmployees: () -> Unit,
    viewModel: TeamPoolViewModel = hiltViewModel(),
    accessViewModel: DealerAccessViewModel = hiltViewModel(),
) {
    val orders by viewModel.orders.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val claimInProgress by viewModel.claimInProgress.collectAsState()
    val profile by accessViewModel.profile.collectAsState()

    val networkError = stringResource(L10nR.string.error_no_internet)
    val genericError = stringResource(L10nR.string.error_generic_message)
    val claimedTemplate = stringResource(L10nR.string.dealer_claimed)
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is Event.ShowError ->
                    snackbar.showSnackbar(event.error.displayText(networkError, genericError))
                is Event.Claimed ->
                    snackbar.showSnackbar(claimedTemplate.format(event.orderId))
            }
        }
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.dealer_team_pool),
                onBack = onBack,
                actions = {
                    if (profile?.canManageEmployees == true) {
                        AgroIconButton(
                            icon = Icons.Outlined.Person,
                            contentDescription = stringResource(L10nR.string.dealer_employees),
                            onClick = onOpenEmployees,
                        )
                    }
                },
            )
        },
    ) { modifier ->
        Box(modifier = modifier) {
            when {
                loading -> LoadingWidget()
                error != null -> CenteredContent {
                    ErrorWithRetry(
                        onRetry = viewModel::load,
                        message = error!!.displayText(networkError, genericError),
                    )
                }
                orders.isEmpty() -> EmptyView(
                    icon = Icons.Outlined.WorkHistory,
                    title = stringResource(L10nR.string.dealer_team_pool_empty),
                    message = stringResource(L10nR.string.dealer_team_pool_empty_hint),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(orders, key = { it.id }) { order ->
                        TeamPoolCard(
                            order = order,
                            claimInProgress = claimInProgress == order.id,
                            onClaim = { viewModel.claim(order.id) },
                        )
                    }
                }
            }
            androidx.compose.material3.SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** Team pool карточкасы — №id + статус + саны/келісім баға/сома/күн + claim. */
@Composable
private fun TeamPoolCard(
    order: TeamPoolOrder,
    claimInProgress: Boolean,
    onClaim: () -> Unit,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "№" + order.id,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ext.primaryText,
            )
            order.status?.let { status ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(dealerOrderStatusColor(status).copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = stringResource(dealerOrderStatusLabelRes(status)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.W600,
                        color = dealerOrderStatusColor(status),
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        order.quantity?.let { quantity ->
            InfoRow(stringResource(L10nR.string.dealer_quantity), formatDouble(quantity))
        }
        order.agreedPrice?.let { price ->
            InfoRow(stringResource(L10nR.string.dealer_agreed_price), PriceFormatter.format(price))
        }
        order.totalAmount?.let { total ->
            InfoRow(stringResource(L10nR.string.dealer_total), PriceFormatter.format(total))
        }
        order.createdAt?.let { date ->
            InfoRow(stringResource(L10nR.string.dealer_created), DateFormatter.formatDateTime(date))
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onClaim,
            enabled = !claimInProgress,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = ext.white,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (claimInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = ext.white,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(L10nR.string.dealer_claim),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W600,
                )
            }
        }
    }
}

/** Атау-мән жолы — сол жақта сұр белгі, оң жақта мән. */
@Composable
private fun InfoRow(label: String, value: String) {
    val ext = extendedColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = ext.secondaryText,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.W600,
            color = ext.primaryText,
        )
    }
}

/** Double → «2,5» пішімі (бүтін болса «.0» жоқ). */
private fun formatDouble(value: Double): String =
    if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        value.toString().replace('.', ',')
    }