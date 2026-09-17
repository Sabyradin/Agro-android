package com.agroland.feature.dealer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Receipt
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.common.formatters.PriceFormatter
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroSwitch
import com.agroland.core.ui.components.CenteredContent
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.dealer.data.displayText
import com.agroland.feature.dealer.ui.DealerSettingsViewModel.Event

/**
 * Бизнес баптаулары — Flutter DealerSettingsPage (1:1): баланс плиткасы
 * (әмиян бетіне ашады) + ҚҚС төлеуші тумблері (PATCH /user/profile
 * is_vat_payer). Сәтті сақтау — «Деректер сәтті жаңартылды» snackbar.
 */
@Composable
fun DealerSettingsPage(
    onBack: () -> Unit,
    onOpenWallet: () -> Unit,
    viewModel: DealerSettingsViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val saving by viewModel.saving.collectAsState()

    val networkError = stringResource(L10nR.string.error_no_internet)
    val genericError = stringResource(L10nR.string.error_generic_message)
    val updatedText = stringResource(L10nR.string.dealer_data_updated)
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is Event.ShowError ->
                    snackbar.showSnackbar(event.error.displayText(networkError, genericError))
                Event.DataUpdated -> snackbar.showSnackbar(updatedText)
            }
        }
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.dealer_settings),
                onBack = onBack,
            )
        },
    ) { modifier ->
        Box(modifier = modifier) {
            when {
                loading -> LoadingWidget()
                profile == null -> CenteredContent {
                    ErrorWithRetry(onRetry = viewModel::load, message = genericError)
                }
                profile != null -> Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val ext = extendedColors()
                    // ── Баланс плиткасы — әмиянға ашады ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .border(0.5.dp, ext.divider, RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenWallet)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(L10nR.string.dealer_balance_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.secondaryText,
                            )
                            Text(
                                text = PriceFormatter.format(profile!!.balance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ext.primaryText,
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Receipt,
                            contentDescription = null,
                            tint = ext.secondaryText,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // ── ҚҚС төлеуші тумблері ──
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ext.card)
                            .border(0.5.dp, ext.divider, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(L10nR.string.dealer_vat_toggle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.W600,
                                    color = ext.primaryText,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(L10nR.string.dealer_vat_description),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ext.secondaryText,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            AgroSwitch(
                                checked = profile!!.isVatPayer,
                                onCheckedChange = { viewModel.setVatPayer(it) },
                                enabled = !saving,
                            )
                        }
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