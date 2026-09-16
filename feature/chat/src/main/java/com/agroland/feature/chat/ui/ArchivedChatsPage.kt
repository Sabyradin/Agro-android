package com.agroland.feature.chat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.EmptyView
import com.agroland.feature.chat.data.ChatRoom

/**
 * Мұрағатталған чаттар парағы (Flutter ArchivedChatsPage паритеті):
 * 3 свайп-әрекет — Үнсіз / Жою / Мұрағаттан шығару (толық свайп — шығару).
 * Деректер — @Singleton ChatSocketService-тен (ChatListViewModel арқылы),
 * сондықтан бұл беттің өз VM инстансы бір бұрынғы тізіммен синхронда.
 */
@Composable
fun ArchivedChatsPage(
    onBack: () -> Unit,
    onOpenRoom: (room: ChatRoom, username: String, otherUserId: Long?, isSystemChat: Boolean, announcementId: String?) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val me by viewModel.me.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<ChatRoom?>(null) }

    val unarchivedMsg = stringResource(L10nR.string.chat_unarchived)
    val archivedMsg = stringResource(L10nR.string.chat_archived)
    val deletedMsg = stringResource(L10nR.string.chat_deleted)
    val title = stringResource(L10nR.string.archived_chats)
    val emptyTitle = stringResource(L10nR.string.no_archived_chats)
    val deleteConfirmTitle = stringResource(L10nR.string.delete_chat)
    val deleteConfirmText = stringResource(L10nR.string.clear_chat_history_confirm)
    val cancelLabel = stringResource(L10nR.string.cancel)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatListEvent.Message -> snackbar.showSnackbar(
                    when (event.text) {
                        ChatListMessage.ARCHIVED -> archivedMsg
                        ChatListMessage.UNARCHIVED -> unarchivedMsg
                        ChatListMessage.DELETED -> deletedMsg
                        else -> ""
                    },
                )
            }
        }
    }

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = "$title (${state.archivedCount})",
                onBack = onBack,
            )
        },
    ) { modifier ->
        Box(modifier = modifier) {
            if (state.archivedRows.isEmpty()) {
                EmptyView(
                    modifier = Modifier.align(Alignment.Center),
                    icon = Icons.Outlined.Unarchive,
                    title = emptyTitle,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(state.archivedRows, key = { _, row -> row.room.roomId }) { index, row ->
                        SwipeActionRow(
                            actions = listOf(
                                SwipeAction(
                                    icon = if (row.muted) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                                    label = "",
                                    color = MaterialTheme.colorScheme.primary,
                                    onClick = { viewModel.toggleMute(row.room) },
                                ),
                                SwipeAction(
                                    icon = Icons.Outlined.Delete,
                                    label = "",
                                    color = Color(0xFFE53935),
                                    destructive = true,
                                    onClick = { pendingDelete = row.room },
                                ),
                                SwipeAction(
                                    icon = Icons.Outlined.Unarchive,
                                    label = "",
                                    color = Color(0xFF616161),
                                    isPrimary = true,
                                    onClick = { viewModel.setArchived(row.room, archived = false) },
                                ),
                            ),
                            isOpen = state.openSwipeRoomId == row.room.roomId,
                            onOpenChange = { open ->
                                viewModel.setOpenSwipe(if (open) row.room.roomId else null)
                            },
                        ) {
                            ChatTile(
                                room = row.room,
                                pinned = row.pinned,
                                meId = me?.id,
                                peerName = viewModel.peerName(row.room, me?.id),
                                showDivider = index != state.archivedRows.lastIndex,
                                onClick = {
                                    val otherUserId = row.room.otherUserId
                                        ?: (if (row.room.senderId == me?.id) row.room.receiverId else row.room.senderId)
                                    onOpenRoom(
                                        row.room,
                                        viewModel.peerName(row.room, me?.id),
                                        otherUserId,
                                        row.room.announcementAuthorId == 0L,
                                        row.room.announcementId?.toString(),
                                    )
                                },
                            )
                        }
                    }
                }
            }
            SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    // Жою растау диалогі (Flutter DeleteActionPane confirm).
    pendingDelete?.let { room ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(deleteConfirmTitle, style = MaterialTheme.typography.titleMedium) },
            text = { Text(deleteConfirmText, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteChat(room)
                    viewModel.setOpenSwipe(null)
                    pendingDelete = null
                }) {
                    Text(deleteConfirmTitle, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(cancelLabel) }
            },
        )
    }
}