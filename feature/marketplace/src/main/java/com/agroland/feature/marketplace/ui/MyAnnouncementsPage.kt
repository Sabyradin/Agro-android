package com.agroland.feature.marketplace.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroTextButton
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.marketplace.data.Announcement

/**
 * MyAnnouncementsPage — менің жарнамаларым статус бойынша
 * (announcements_by_status_page.dart): 4 таб, әрқайсысы бөлек бетте
 * (GET /user/announcements/{status}), әрекеттер — активация/өшіру/жою,
 * қайтарылғандар үшін — себебі диалогы.
 */
@Composable
fun MyAnnouncementsPage(
    onBack: () -> Unit,
    onOpenStatus: (String) -> Unit,
    onOpenAnnouncement: (Long) -> Unit,
    onEditAnnouncement: (Long) -> Unit,
    viewModel: MyAnnouncementsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val loadingMore by viewModel.loadingMore.collectAsState()
    val error by viewModel.error.collectAsState()
    val rejectMessage by viewModel.rejectMessage.collectAsState()
    val actionInProgress by viewModel.actionInProgress.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val noInternetText = stringResource(L10nR.string.error_no_internet)
    val genericErrorText = stringResource(L10nR.string.error_generic_message)
    val statusChangedText = stringResource(L10nR.string.ad_status_changed_toast)
    val deletedText = stringResource(L10nR.string.ad_deleted_toast)

    var deleteTarget by remember { mutableStateOf<Announcement?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MyAnnouncementsViewModel.Event.ShowError ->
                    snackbar.showSnackbar(event.error.displayText(noInternetText, genericErrorText))
                is MyAnnouncementsViewModel.Event.StatusChanged ->
                    snackbar.showSnackbar(statusChangedText)
                is MyAnnouncementsViewModel.Event.Deleted ->
                    snackbar.showSnackbar(deletedText)
            }
        }
    }

    val tabIndex = MyAnnouncementsViewModel.STATUS_TABS.indexOfFirst { it == viewModel.status }
        .takeIf { it >= 0 } ?: 0
    val tabTitles = listOf(
        L10nR.string.my_ads_status_active,
        L10nR.string.my_ads_status_pending,
        L10nR.string.my_ads_status_inactive,
        L10nR.string.my_ads_status_rejected,
    )

    AgroScaffold(
        topBar = {
            AgroAppBar(title = stringResource(L10nR.string.my_ads_title), onBack = onBack)
        },
    ) { inner ->
        Column(modifier = inner.fillMaxSize()) {
            TabRow(selectedTabIndex = tabIndex) {
                MyAnnouncementsViewModel.STATUS_TABS.forEachIndexed { index, statusKey ->
                    Tab(
                        selected = index == tabIndex,
                        onClick = { onOpenStatus(statusKey) },
                        text = { Text(stringResource(tabTitles[index])) },
                    )
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    loading -> LoadingWidget(Modifier.fillMaxSize())
                    error != null -> ErrorWithRetry(
                        onRetry = viewModel::refresh,
                        modifier = Modifier.fillMaxSize(),
                    )
                    items.isEmpty() -> EmptyView(
                        title = stringResource(L10nR.string.my_ads_empty_title),
                        message = stringResource(L10nR.string.my_ads_empty_message),
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> {
                        val listState = rememberLazyListState()
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                                if (index >= items.size - 3) viewModel.loadMore()
                                MyAdRow(
                                    item = item,
                                    actionInProgress = actionInProgress == item.id,
                                    onOpen = { onOpenAnnouncement(item.id) },
                                    onEdit = { onEditAnnouncement(item.id) },
                                    onToggle = { viewModel.toggleActive(item) },
                                    onDelete = { deleteTarget = item },
                                    onShowRejectReason = { viewModel.showRejectMessage(item.id) },
                                )
                            }
                            if (loadingMore) {
                                item(key = "loading_more") {
                                    LoadingWidget(Modifier.fillMaxWidth().padding(16.dp))
                                }
                            }
                        }
                    }
                }
                SnackbarHost(
                    hostState = snackbar,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(L10nR.string.ad_delete_confirm_title)) },
            text = { Text(stringResource(L10nR.string.ad_delete_confirm, target.title)) },
            confirmButton = {
                AgroTextButton(
                    text = stringResource(L10nR.string.ad_action_delete),
                    onClick = {
                        viewModel.delete(target)
                        deleteTarget = null
                    },
                )
            },
            dismissButton = {
                AgroTextButton(
                    text = stringResource(L10nR.string.common_cancel),
                    onClick = { deleteTarget = null },
                )
            },
        )
    }

    rejectMessage?.let { (_, message) ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRejectMessage,
            title = { Text(stringResource(L10nR.string.reject_reason_title)) },
            text = { Text(message) },
            confirmButton = {
                AgroTextButton(
                    text = stringResource(L10nR.string.common_ok),
                    onClick = viewModel::dismissRejectMessage,
                )
            },
        )
    }
}

/** Бір жарнама жолы — карточка + әрекеттер жолағы (Flutter list tile actions). */
@Composable
private fun MyAdRow(
    item: Announcement,
    actionInProgress: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onShowRejectReason: () -> Unit,
) {
    val ext = extendedColors()
    Column(modifier = Modifier.fillMaxWidth()) {
        AnnouncementCard(item = item, onClick = onOpen)
        if (actionInProgress) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .size(20.dp),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val isActive = item.status.equals("active", ignoreCase = true)
                AgroTextButton(
                    text = stringResource(
                        if (isActive) L10nR.string.ad_action_deactivate
                        else L10nR.string.ad_action_activate,
                    ),
                    onClick = onToggle,
                )
                AgroTextButton(
                    text = stringResource(L10nR.string.ad_action_edit),
                    onClick = onEdit,
                )
                if (item.status.equals("rejected", ignoreCase = true)) {
                    AgroTextButton(
                        text = stringResource(L10nR.string.ad_action_reject_reason),
                        onClick = onShowRejectReason,
                    )
                }
                AgroTextButton(
                    text = stringResource(L10nR.string.ad_action_delete),
                    onClick = onDelete,
                )
            }
        }
    }
}