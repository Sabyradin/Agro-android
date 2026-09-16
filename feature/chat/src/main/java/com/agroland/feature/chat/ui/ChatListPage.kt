package com.agroland.feature.chat.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.CachedImage
import com.agroland.core.ui.components.EmptyView
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.chat.data.ChatRoom
import com.agroland.feature.chat.data.sanitizeUrl
import com.agroland.feature.chat.domain.CallMessage
import com.agroland.feature.chat.domain.FormatLastSeen

/** Чат тізімі парағы (Flutter ChatPage паритеті + Material 3 полиші). */
@Composable
fun ChatListPage(
    onOpenRoom: (room: ChatRoom, username: String, otherUserId: Long?, isSystemChat: Boolean, announcementId: String?) -> Unit,
    onOpenArchived: (count: Int) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val searchInput by viewModel.searchInput.collectAsState()
    val me by viewModel.me.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<ChatRoom?>(null) }

    val archivedMsg = stringResource(L10nR.string.chat_archived)
    val unarchivedMsg = stringResource(L10nR.string.chat_unarchived)
    val deletedMsg = stringResource(L10nR.string.chat_deleted)
    val pinnedMsg = stringResource(L10nR.string.chat_pinned)
    val unpinnedMsg = stringResource(L10nR.string.chat_unpinned)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatListEvent.Message -> snackbar.showSnackbar(
                    when (event.text) {
                        ChatListMessage.ARCHIVED -> archivedMsg
                        ChatListMessage.UNARCHIVED -> unarchivedMsg
                        ChatListMessage.DELETED -> deletedMsg
                        ChatListMessage.PINNED -> pinnedMsg
                        ChatListMessage.UNPINNED -> unpinnedMsg
                    },
                )
            }
        }
    }

    // Swipe-әрекеттердің жауаптары.
    val deleteConfirmTitle = stringResource(L10nR.string.delete_chat)
    val deleteConfirmText = stringResource(L10nR.string.clear_chat_history_confirm)
    val cancelLabel = stringResource(L10nR.string.cancel)

    Column(modifier = Modifier.fillMaxSize()) {
        ChatSearchAppBar(
            query = searchInput,
            onQueryChange = viewModel::onSearchChange,
            onClose = viewModel::clearSearch,
            onOpenSearch = { viewModel.onSearchChange("") },
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.loading -> LoadingWidget(Modifier.align(Alignment.Center))
                state.loadFailed -> Box(Modifier.align(Alignment.Center).padding(horizontal = 32.dp)) {
                    ErrorWithRetry(onRetry = viewModel::retry)
                }
                state.rows.isEmpty() && state.archivedCount == 0 -> Box(Modifier.align(Alignment.Center)) {
                    EmptyView(
                        icon = Icons.Outlined.ChatBubble,
                        title = stringResource(L10nR.string.nothing_found),
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
                ) {
                    // Мұрағат жолы — Telegram стилінде, іздеу кезінде жасырылады.
                    if (state.archivedCount > 0 && state.searchQuery.isEmpty()) {
                        item("archived") {
                            ArchivedSectionHeader(
                                count = state.archivedCount,
                                preview = state.archivedPreview,
                                onClick = { onOpenArchived(state.archivedCount) },
                            )
                        }
                    }
                    itemsIndexed(state.rows, key = { _, row -> row.room.roomId }) { index, row ->
                        val isLast = index == state.rows.lastIndex
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
                                    icon = Icons.Outlined.Archive,
                                    label = "",
                                    color = Color(0xFF616161),
                                    isPrimary = true,
                                    onClick = { viewModel.setArchived(row.room, archived = true) },
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
                                showDivider = !isLast,
                                onClick = {
                                    val otherUserId = row.room.otherUserId
                                        ?: (if (row.room.senderId == me?.id) row.room.receiverId else row.room.senderId)
                                    val isSystemChat = row.room.announcementAuthorId == 0L
                                    onOpenRoom(
                                        row.room,
                                        viewModel.peerName(row.room, me?.id),
                                        otherUserId,
                                        isSystemChat,
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

/** ІздеуAppBar — атау ↔ TextField (Flutter _ChatSearchAppBar 350мс анимациясы). */
@Composable
private fun ChatSearchAppBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    var searchOpen by remember { mutableStateOf(query.isNotEmpty()) }
    // Егер query босатылса — режим жабылады.
    LaunchedEffect(query) { if (query.isEmpty()) searchOpen = false }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(extendedColors().card)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = searchOpen,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
            label = "chatSearchBar",
        ) { open ->
            if (open) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(extendedColors().grey)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = extendedColors().secondaryText,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                stringResource(L10nR.string.search_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = extendedColors().secondaryText,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                                tint = extendedColors().secondaryText,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(L10nR.string.chat),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors().primaryText,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
        if (!searchOpen) {
            IconButton(onClick = {
                onOpenSearch()
                searchOpen = true
            }) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(L10nR.string.search),
                    tint = extendedColors().primaryText,
                )
            }
        }
    }
}

/** Мұрағат секциясының бас жолы (Telegram стилі — мөлдір, свайп жоқ). */
@Composable
private fun ArchivedSectionHeader(
    count: Int,
    preview: String,
    onClick: () -> Unit,
) {
    val ext = extendedColors()
    val chevronSize by animateDpAsState(18.dp, label = "chevron")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Archive,
            contentDescription = null,
            tint = ext.secondaryText,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${stringResource(L10nR.string.archived_chats)} ($count)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = ext.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (preview.isNotBlank()) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = ext.secondaryText,
            modifier = Modifier.size(chevronSize),
        )
    }
}

/** Жүйелі чат иконкалары (Flutter _systemChatIcons): id → иконка + түс. */
internal fun systemChatIcon(peerId: Long?): Pair<ImageVector, Color>? = when (peerId) {
    31L -> Icons.AutoMirrored.Outlined.Help to Color(0xFFFF9500)
    1001L -> Icons.Outlined.Sell to Color(0xFFFF3B30)
    1002L -> Icons.Outlined.Agriculture to Color(0xFF34C759)
    1003L -> Icons.AutoMirrored.Outlined.MenuBook to Color(0xFF30B0C7)
    1004L -> Icons.Outlined.Description to Color(0xFF007AFF)
    1005L -> Icons.Outlined.ShoppingBag to Color(0xFFAF52DE)
    else -> null
}

/** Аватар: сурет/инициал + онлайн нүктесі (0xFF34C759, оң жақ астында). */
@Composable
internal fun ChatAvatar(
    avatarUrl: String?,
    name: String,
    isOnline: Boolean,
    isSystemChat: Boolean,
    peerId: Long?,
    size: Int = 52,
) {
    Box {
        val iconSpec = if (isSystemChat && avatarUrl.isNullOrBlank()) systemChatIcon(peerId) else null
        if (iconSpec != null) {
            // Жүйелі чат — түсті фон + иконка (15% фон).
            val (icon, color) = iconSpec
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size((size * 0.46f).dp),
                )
            }
        } else if (!avatarUrl.isNullOrBlank()) {
            CachedImage(
                url = avatarUrl,
                contentDescription = name,
                modifier = Modifier.size(size.dp).clip(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.trim().firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = (size * 0.42f).sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size * 0.28f).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34C759)),
            )
        }
    }
}

/** Preview мәтіні (Flutter _previewText паритеті). */
@Composable
internal fun chatPreviewText(room: ChatRoom, meId: Long?): String {
    val sharedLocation = stringResource(L10nR.string.shared_location)
    val youPrefix = stringResource(L10nR.string.chat_list_you_prefix)
    val callCompleted = stringResource(L10nR.string.call_msg_completed)
    val callMissed = stringResource(L10nR.string.call_msg_missed)
    val callDeclined = stringResource(L10nR.string.call_msg_declined)
    val callNoAnswer = stringResource(L10nR.string.call_msg_no_answer)

    val callStatus = CallMessage.parse(room.message)?.first
    if (callStatus != null) {
        return when (callStatus) {
            CallMessage.Status.COMPLETED -> callCompleted
            CallMessage.Status.MISSED -> callMissed
            CallMessage.Status.DECLINED -> callDeclined
        }
    }
    var display = room.message.replace(Regex("\\s*\\[[^]]+]"), "").trim()
    val coordsMatch = Regex("^-?\\d+\\.\\d+,\\s*-?\\d+\\.\\d+$")
    if (coordsMatch.matches(display)) {
        display = "📍 $sharedLocation"
    } else if (display.startsWith("http://") || display.startsWith("https://")) {
        display = sanitizeUrl(display) ?: display
    }
    return if (meId != null && room.senderId == meId) "$youPrefix: $display" else display
}

/** Бір чат жолы (Flutter ChatViewItem паритеті). */
@Composable
internal fun ChatTile(
    room: ChatRoom,
    pinned: Boolean,
    meId: Long?,
    peerName: String,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    val ext = extendedColors()
    val yesterdayLabel = stringResource(L10nR.string.yesterday)
    val preview = chatPreviewText(room, meId)
    val unread = room.unreadCount

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val avatarUrl = room.otherUserAvatarUrl
                ?: (if (room.senderId == meId) room.receiverAvatarUrl else room.senderAvatarUrl)
            ChatAvatar(
                avatarUrl = avatarUrl,
                name = peerName,
                isOnline = room.isOnline,
                isSystemChat = room.announcementAuthorId == 0L,
                peerId = room.otherUserId ?: (if (room.senderId == meId) room.receiverId else room.senderId),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = peerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Medium,
                        color = ext.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (room.isChecked) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (unread > 0) ext.primaryText else ext.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = FormatLastSeen.formatRelative(room.timestamp, yesterdayLabel),
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.secondaryText,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pinned) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = null,
                            tint = ext.secondaryText,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    if (unread > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (unread > 99) "99+" else unread.toString(),
                                color = ext.white,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = ext.divider,
                modifier = Modifier.padding(start = 80.dp, end = 16.dp),
            )
        }
    }
}
/** Қонақ (кірмеген) үшін чат қойындысы — кіруге шақыру (Flutter guest parity). */
@Composable
fun GuestChatTab(onLoginClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubble,
                contentDescription = null,
                tint = extendedColors().divider,
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(L10nR.string.chat_login_prompt),
                style = MaterialTheme.typography.bodySmall,
                color = extendedColors().secondaryText,
            )
            Spacer(Modifier.height(16.dp))
            com.agroland.core.ui.components.AgroButton(
                text = stringResource(L10nR.string.auth_login_title),
                onClick = onLoginClick,
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .fillMaxWidth(),
            )
        }
    }
}
