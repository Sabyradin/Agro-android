package com.agroland.feature.marketplace.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroSmallButton
import com.agroland.core.ui.components.AgroTextButton
import com.agroland.core.ui.theme.extendedColors
import kotlinx.coroutines.delay

/**
 * OwnerAnnouncementPage — ProfileAnnouncementPage: өз жарнамасының деталы +
 * ие әрекеттері (activate/deactivate/delete/edit). Детал — AnnouncementDetailPage
 * қайта қолданылады, bottomBar-ға ие панелі қосылады.
 */
@Composable
fun OwnerAnnouncementPage(
    announcementId: Long,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    // Фаза 15: промо v2 — «Жарнама жылжыту» (AdvertiseAdPage) өтуі.
    onPromote: (Long) -> Unit = {},
    detailViewModel: AnnouncementDetailViewModel = hiltViewModel(),
    actionsViewModel: AnnouncementActionsViewModel = hiltViewModel(),
) {
    val detail by detailViewModel.detail.collectAsState()
    val actionInProgress by actionsViewModel.actionInProgress.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val noInternetText = stringResource(L10nR.string.error_no_internet)
    val genericErrorText = stringResource(L10nR.string.error_generic_message)
    val statusChangedText = stringResource(L10nR.string.ad_status_changed_toast)
    val deletedText = stringResource(L10nR.string.ad_deleted_toast)

    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        actionsViewModel.events.collect { event ->
            when (event) {
                is AnnouncementActionsViewModel.Event.ShowError ->
                    snackbar.showSnackbar(event.error.displayText(noInternetText, genericErrorText))
                is AnnouncementActionsViewModel.Event.StatusChanged -> {
                    snackbar.showSnackbar(statusChangedText)
                    detailViewModel.load(announcementId)
                }
                is AnnouncementActionsViewModel.Event.Deleted -> {
                    snackbar.showSnackbar(deletedText)
                    delay(900)
                    onBack()
                }
            }
        }
    }

    AnnouncementDetailPage(
        announcementId = announcementId,
        onBack = onBack,
        onOpenDetail = onOpenDetail,
        bottomBar = {
            val ext = extendedColors()
            val isActive = detail?.base?.status.equals("active", ignoreCase = true)
            Column(modifier = Modifier.navigationBarsPadding()) {
                SnackbarHost(hostState = snackbar)
                Surface(color = ext.card) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AgroSmallButton(
                                text = stringResource(
                                    if (isActive) L10nR.string.ad_action_deactivate
                                    else L10nR.string.ad_action_activate,
                                ),
                                onClick = {
                                    if (isActive) {
                                        actionsViewModel.deactivate(announcementId)
                                    } else {
                                        actionsViewModel.activate(announcementId)
                                    }
                                },
                                enabled = !actionInProgress,
                                loading = actionInProgress,
                                modifier = Modifier.weight(1f),
                            )
                            AgroTextButton(
                                text = stringResource(L10nR.string.ad_action_edit),
                                onClick = { onEdit(announcementId) },
                                enabled = !actionInProgress,
                                modifier = Modifier.weight(1f),
                            )
                            AgroTextButton(
                                text = stringResource(L10nR.string.ad_action_delete),
                                onClick = { confirmDelete = true },
                                enabled = !actionInProgress,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        // Фаза 15: активті жарнаманы жылжыту — промо v2 каталогы
                        // (Flutter profile_announcement_actions «Advertise»).
                        if (isActive) {
                            AgroSmallButton(
                                text = stringResource(L10nR.string.advertise),
                                onClick = { onPromote(announcementId) },
                                enabled = !actionInProgress,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        viewModel = detailViewModel,
    )

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(L10nR.string.ad_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        L10nR.string.ad_delete_confirm,
                        detail?.base?.title ?: "",
                    ),
                )
            },
            confirmButton = {
                AgroTextButton(
                    text = stringResource(L10nR.string.ad_action_delete),
                    onClick = {
                        actionsViewModel.delete(announcementId)
                        confirmDelete = false
                    },
                )
            },
            dismissButton = {
                AgroTextButton(
                    text = stringResource(L10nR.string.common_cancel),
                    onClick = { confirmDelete = false },
                )
            },
        )
    }
}