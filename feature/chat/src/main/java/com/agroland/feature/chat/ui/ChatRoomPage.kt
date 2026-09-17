package com.agroland.feature.chat.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.ErrorWithRetry
import com.agroland.core.ui.components.LoadingWidget
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.chat.data.ChatMessage
import com.agroland.feature.chat.data.ChatSocketManager
import com.agroland.feature.chat.data.sanitizeUrl
import com.agroland.feature.chat.domain.FormatLastSeen
import com.agroland.feature.chat.domain.InferMessageType
import com.agroland.feature.chat.domain.InferredMessageType
import java.io.File
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Тізім қатары: күн ажыратқышы НЕ хабарлама. */
private sealed interface RoomRow {
    data class Day(val millis: Long, val label: String) : RoomRow
    data class Msg(val message: ChatMessage) : RoomRow
}

/**
 * Чат бөлмесі (Flutter ChatRoomPage паритеті, көрнекті дизайн):
 *  - join күйі: connecting banner / error retry / grace (хабарлама бар болса);
 *  - күн ажыратқыштары + көпіршектер (мәтін/сурет/бейне/дауыс/файл/локация/
 *    қоңырау/жеткізу карточкалары/жарнама баннері);
 *  - load_older + prepend скролл сақтауы + төменге FAB;
 *  - ұзын басу → мәтінмән меню (жауап/өңдеу/көшіру/жою);
 *  - typing көрсеткіші + last seen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatRoomPage(
    onBack: () -> Unit,
    onOpenAnnouncement: (Long) -> Unit,
    /** Фаза 13: дауыстық қоңырау (peer-чаттерде ғана, жүйелік чатта null). */
    onVoiceCall: ((peerId: Long, peerName: String?, peerAvatarUrl: String?) -> Unit)? = null,
    /** Қоңырау жүріп жатқанда шалу батырмасы disabled. */
    callActive: Boolean = false,
    /** Фаза 18: сурет — ішкі pinch-zoom фото көрсеткіші (null — локальды диалог). */
    onOpenPhotoViewer: ((url: String) -> Unit)? = null,
    /** Фаза 18: бейне — ішкі ExoPlayer көрсеткіші (null — сыртқы intent). */
    onOpenVideoViewer: ((url: String) -> Unit)? = null,
    /** Фаза 18: PDF — ішкі PdfRenderer көрсеткіші (null — сыртқы intent). */
    onOpenPdfViewer: ((url: String) -> Unit)? = null,
    viewModel: ChatRoomViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val peerRoom by viewModel.peerRoom.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val colors = extendedColors()

    // --- Строка-деректер (эффектісіз мәтіндер) ---
    val typingLabel = stringResource(L10nR.string.typing)
    val yesLabel = stringResource(L10nR.string.delivery_quick_reply_yes)
    val noLabel = stringResource(L10nR.string.delivery_quick_reply_no)
    val sumLabel = stringResource(L10nR.string.delivery_sum_label)
    val clearHistoryLabel = stringResource(L10nR.string.clear_chat_history)
    val clearHistoryConfirmLabel = stringResource(L10nR.string.clear_chat_history_confirm)
    val archiveChatLabel = stringResource(L10nR.string.archive_chat)
    val cancelLabel = stringResource(L10nR.string.cancel)
    val deleteLabel = stringResource(L10nR.string.delete)
    val chatLabel = stringResource(L10nR.string.chat)

    // Чип мәтіндері — VM талдауы үшін (delivery flow).
    LaunchedEffect(Unit) {
        viewModel.setDeliveryReplyStrings(yesLabel, noLabel, sumLabel)
    }

    // --- Snackbar/toast оқиғалары ---
    val queuedMsg = stringResource(L10nR.string.chat_message_queued)
    val connLostMsg = stringResource(L10nR.string.chat_connection_lost)
    val sendErrorMsg = stringResource(L10nR.string.chat_send_error)
    val copiedMsg = stringResource(L10nR.string.copied)
    val fileLargeMsg = stringResource(L10nR.string.chat_file_too_large)
    val wrongMsg = stringResource(L10nR.string.something_went_wrong)
    val clearedMsg = stringResource(L10nR.string.chat_cleared)
    val voiceShortMsg = stringResource(L10nR.string.voice_message_too_short)
    val recordErrorMsg = stringResource(L10nR.string.voice_record_error)
    val fileOpenErrorMsg = stringResource(L10nR.string.chat_file_open_error)
    val audioPlayErrorMsg = stringResource(L10nR.string.chat_audio_play_error)
    val loadFailedMsg = stringResource(L10nR.string.chat_history_load_failed)
    val locationErrorMsg = stringResource(L10nR.string.location_error)
    val cameraUnavailableMsg = stringResource(L10nR.string.camera_unavailable)
    val photoPickErrorMsg = stringResource(L10nR.string.photo_pick_error)
    val fileSelectErrorMsg = stringResource(L10nR.string.file_select_error)
    val videoPreparingMsg = stringResource(L10nR.string.video_preparing)
    val deliverySentMsg = stringResource(L10nR.string.delivery_request_sent)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatRoomEvent.OpenAnnouncement -> onOpenAnnouncement(event.announcementId)
                ChatRoomEvent.PopRoom -> onBack()
                is ChatRoomEvent.Toast -> snackbar.showSnackbar(
                    when (event) {
                        ChatRoomEvent.Toast.QUEUED -> queuedMsg
                        ChatRoomEvent.Toast.CONNECTION_LOST -> connLostMsg
                        ChatRoomEvent.Toast.SEND_ERROR -> sendErrorMsg
                        ChatRoomEvent.Toast.COPIED -> copiedMsg
                        ChatRoomEvent.Toast.FILE_TOO_LARGE -> fileLargeMsg
                        ChatRoomEvent.Toast.VIDEO_PREPARING -> videoPreparingMsg
                        ChatRoomEvent.Toast.SOMETHING_WRONG -> wrongMsg
                        ChatRoomEvent.Toast.DELIVERY_REQUEST_SENT -> deliverySentMsg
                        ChatRoomEvent.Toast.CHAT_CLEARED -> clearedMsg
                        ChatRoomEvent.Toast.VOICE_TOO_SHORT -> voiceShortMsg
                        ChatRoomEvent.Toast.RECORD_ERROR -> recordErrorMsg
                        ChatRoomEvent.Toast.FILE_OPEN_ERROR -> fileOpenErrorMsg
                        ChatRoomEvent.Toast.AUDIO_PLAY_ERROR -> audioPlayErrorMsg
                        ChatRoomEvent.Toast.LOAD_FAILED -> loadFailedMsg
                        ChatRoomEvent.Toast.LOCATION_ERROR -> locationErrorMsg
                        ChatRoomEvent.Toast.CAMERA_UNAVAILABLE -> cameraUnavailableMsg
                        ChatRoomEvent.Toast.PHOTO_PICK_ERROR -> photoPickErrorMsg
                        ChatRoomEvent.Toast.FILE_SELECT_ERROR -> fileSelectErrorMsg
                    },
                )
            }
        }
    }

    // --- Media picker launchers ---
    fun displayNameOf(uri: Uri): String? = context.displayName(uri)

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.sendMedia(uri, UploadKind.IMAGE, displayNameOf(uri))
        }
    }
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.sendMedia(uri, UploadKind.FILE, displayNameOf(uri))
        }
    }
    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.sendMedia(uri, UploadKind.AUDIO, displayNameOf(uri))
        }
    }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val micDeniedMsg = stringResource(L10nR.string.chat_mic_permission_denied)
    val cameraDeniedMsg = stringResource(L10nR.string.chat_camera_permission_denied)
    val holdHintMsg = stringResource(L10nR.string.voice_hold_to_record)
    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok ->
        val uri = cameraUri
        if (ok && uri != null) {
            viewModel.sendMedia(uri, UploadKind.IMAGE, context.displayName(uri))
        } else if (!ok) {
            uri?.let { File(it.path ?: "").delete() }
        }
    }
    val recordVideo = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo(),
    ) { ok ->
        val uri = cameraUri
        if (ok && uri != null) {
            viewModel.sendMedia(uri, UploadKind.VIDEO, context.displayName(uri))
        }
    }

    // Камера: манифестте CAMERA рұқсаты жарияланғандықтан, ACTION_IMAGE_CAPTURE
    // рұқсатсыз SecurityException лақтырады — алдымен рұқсат сұраймыз.
    var pendingCameraVideo by remember { mutableStateOf<Boolean?>(null) }
    fun launchCamera(video: Boolean) {
        try {
            val uri = context.newCameraUri(video = video)
            cameraUri = uri
            if (video) recordVideo.launch(uri) else takePhoto.launch(uri)
        } catch (_: Exception) {
            scope.launch { snackbar.showSnackbar(cameraUnavailableMsg) }
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val video = pendingCameraVideo
        pendingCameraVideo = null
        if (granted && video != null) {
            launchCamera(video)
        } else if (!granted) {
            scope.launch { snackbar.showSnackbar(cameraDeniedMsg) }
        }
    }
    fun openCamera(video: Boolean) {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchCamera(video)
        } else {
            pendingCameraVideo = video
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        scope.launch { snackbar.showSnackbar(if (granted) holdHintMsg else micDeniedMsg) }
    }

    @SuppressLint("MissingPermission")
    fun fetchLocation() {
        try {
            val client = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(context)
            client.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        viewModel.sendLocation(loc.latitude, loc.longitude)
                    } else {
                        // Кеште соңғы орын жоқ — нақты орынды сұраймыз.
                        client.getCurrentLocation(
                            com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                            null,
                        ).addOnSuccessListener { current ->
                            if (current != null) {
                                viewModel.sendLocation(current.latitude, current.longitude)
                            } else {
                                scope.launch { snackbar.showSnackbar(locationErrorMsg) }
                            }
                        }.addOnFailureListener {
                            scope.launch { snackbar.showSnackbar(locationErrorMsg) }
                        }
                    }
                }
                .addOnFailureListener {
                    scope.launch { snackbar.showSnackbar(locationErrorMsg) }
                }
        } catch (_: Exception) {
            scope.launch { snackbar.showSnackbar(locationErrorMsg) }
        }
    }
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) fetchLocation()
        else scope.launch { snackbar.showSnackbar(locationErrorMsg) }
    }
    fun pickLocation() {
        if (!hasLocationPermission(context)) {
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        fetchLocation()
    }

    // --- Хабарлама менюі / диалогтар / қарау режимі ---
    var menuMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var deleteTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var deleteForEveryone by remember { mutableStateOf(false) }
    var viewerUrl by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var clearConfirmOpen by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    // --- Тізім модельі: күн ажыратқыштарымен ---
    val yesterdayLabel = stringResource(L10nR.string.yesterday)
    val todayLabel = stringResource(L10nR.string.chat_day_today)
    val rows: List<RoomRow> = remember(state.messages) {
        buildRows(state.messages, todayLabel, yesterdayLabel)
    }

    // WhatsApp-тағыдай тізім астынан басталады: reverseLayout + кері реттегі
    // қатарлар. Ашылғанда соңғы хабарлама бірден көрінеді, пернетақта
    // ашылғанда тізім бірге көтеріледі, ескі хабарламалар жоғарыдан
    // қосылғанда орын секірмейді.
    val reversedRows = remember(rows) { rows.asReversed() }
    val listState = rememberLazyListState()
    // Жаңа хабарлама → төменге жылжу.
    LaunchedEffect(state.scrollToBottom) {
        if (state.scrollToBottom > 0 && reversedRows.isNotEmpty()) {
            if (listState.firstVisibleItemIndex <= 3) {
                listState.animateScrollToItem(0)
            } else {
                listState.scrollToItem(0)
            }
        }
    }
    // Жоғарғы шетке (тізімнің соңына) жақындағанда — ескі хабарламаларды жүктеу.
    LaunchedEffect(state.roomId) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
            .distinctUntilChanged()
            .collect { nearTop ->
                if (nearTop) viewModel.loadOlderMessages()
            }
    }
    val showScrollDown by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 2 }
    }

    // --- Header деректері ---
    val meId = state.me?.id
    val peerId = peerRoom?.otherUserId ?: peerRoom?.senderId
    // Шаблондарды алдын ала оқымыз — Labels lambda-лары composable емес.
    val minutesAgoTpl = stringResource(L10nR.string.chat_last_seen_minutes_ago)
    val hoursAgoTpl = stringResource(L10nR.string.chat_last_seen_hours_ago)
    val daysAgoTpl = stringResource(L10nR.string.chat_last_seen_days_ago)
    val onlineLabels = FormatLastSeen.Labels(
        online = stringResource(L10nR.string.chat_online),
        offline = stringResource(L10nR.string.chat_offline),
        prefix = stringResource(L10nR.string.chat_last_seen_prefix),
        justNow = stringResource(L10nR.string.chat_last_seen_just_now),
        minutesAgo = { String.format(minutesAgoTpl, it) },
        hoursAgo = { String.format(hoursAgoTpl, it) },
        yesterday = stringResource(L10nR.string.chat_last_seen_yesterday),
        daysAgo = { String.format(daysAgoTpl, it) },
        dateFormatter = { FormatLastSeen.formatDate(it) },
    )
    val roomTitle = peerRoom?.otherUserName?.takeIf { it.isNotBlank() }
        ?: peerRoom?.let { if (it.senderId == meId) it.receiverName else it.senderName }
        ?: chatLabel
    val subtitle = when {
        state.peerTyping -> typingLabel
        peerRoom != null -> FormatLastSeen.format(
            labels = onlineLabels,
            isOnline = peerRoom?.isOnline == true,
            lastOnline = peerRoom?.lastOnline,
            peerId = peerRoom?.otherUserId,
        )
        else -> ""
    }
    val connected = state.connectionState == ChatSocketManager.State.CONNECTED
    val showBanner = !state.joinResponded && !state.loadTimedOut ||
        (!connected && !state.loadTimedOut && state.messages.isNotEmpty())

    // ================= UI =================
    val palette = chatPalette()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            // Пернетақта ашылса — енгізу жолағы оның үстіне көтеріледі;
            // жабық кезде — жүйелік навигация жолағының үстінде тұрады.
            .windowInsetsPadding(
                androidx.compose.foundation.layout.WindowInsets.ime
                    .union(androidx.compose.foundation.layout.WindowInsets.navigationBars)
                    .only(androidx.compose.foundation.layout.WindowInsetsSides.Bottom),
            ),
    ) {
        ChatRoomAppBar(
            title = roomTitle,
            subtitle = subtitle,
            typing = state.peerTyping,
            avatarUrl = peerRoom?.otherUserAvatarUrl,
            isOnline = peerRoom?.isOnline == true,
            isSystemChat = peerRoom?.announcementAuthorId == 0L,
            peerId = peerId,
            onVoiceCall = if (onVoiceCall != null && peerRoom?.announcementAuthorId != 0L) {
                {
                    onVoiceCall(
                        peerRoom?.otherUserId ?: 0L,
                        peerRoom?.otherUserName ?: roomTitle,
                        peerRoom?.otherUserAvatarUrl,
                    )
                }
            } else {
                null
            },
            callActive = callActive,
            menuOpen = menuOpen,
            onMenuOpenChange = { menuOpen = it },
            onClearHistory = { clearConfirmOpen = true },
            onArchive = { viewModel.archiveChat() },
            onBack = onBack,
        )

        // fillMaxWidth — спиннер мен «Қосылуда…» банері экран ортасында тұруы үшін.
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                // Graceful timeout + хабарлама жоқ → қайта жүктеу.
                state.loadTimedOut && state.messages.isEmpty() -> {
                    ErrorWithRetry(
                        modifier = Modifier.align(Alignment.Center),
                        onRetry = { viewModel.retryLoadHistory() },
                    )
                }
                // Join күтілуде + хабарламалар жоқ → spinner.
                !state.joinResponded && state.messages.isEmpty() -> {
                    LoadingWidget(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 10.dp,
                            vertical = 8.dp,
                        ),
                    ) {
                        itemsIndexed(reversedRows, key = { index, row -> rowKey(row, index) }) { _, row ->
                            when (row) {
                                is RoomRow.Day -> DaySeparator(row.label)
                                is RoomRow.Msg -> MessageRow(
                                    message = row.message,
                                    isMine = row.message.senderId == meId,
                                    state = state,
                                    viewModel = viewModel,
                                    meId = meId,
                                    onLongPress = { menuMessage = row.message },
                                    onSwipeReply = { viewModel.setReplyTo(row.message) },
                                    onOpenImage = { url ->
                                        val openPhoto = onOpenPhotoViewer
                                        if (openPhoto != null) {
                                            openPhoto(url)
                                        } else {
                                            viewerUrl = url
                                        }
                                    },
                                    onPlayVideo = { url ->
                                        val openVideo = onOpenVideoViewer
                                        if (openVideo != null) {
                                            openVideo(url)
                                        } else {
                                            playVideoExternally(context, url)
                                        }
                                    },
                                    onOpenFile = { url, name ->
                                        val openPdf = onOpenPdfViewer
                                        if (openPdf != null && name.endsWith(".pdf", ignoreCase = true)) {
                                            openPdf(url)
                                        } else {
                                            scope.launch {
                                                snackbar.showSnackbar(
                                                    context.getString(L10nR.string.chat_file_opening),
                                                )
                                                val file = ChatFileOpener.downloadToCache(context, url, name)
                                                if (file == null || !ChatFileOpener.open(context, file)) {
                                                    snackbar.showSnackbar(fileOpenErrorMsg)
                                                }
                                            }
                                        }
                                    },
                                    onOpenMap = { lat, lng -> openLocationInMap(context, lat, lng) },
                                    onOpenAnnouncement = onOpenAnnouncement,
                                    onAudioError = {
                                        scope.launch { snackbar.showSnackbar(audioPlayErrorMsg) }
                                    },
                                )
                            }
                        }
                    }

                    // Төменге тез жылжу батырмасы.
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showScrollDown,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            shadowElevation = 4.dp,
                            color = colors.card,
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        scope.launch {
                                            if (reversedRows.isNotEmpty()) listState.animateScrollToItem(0)
                                        }
                                    }
                                    .padding(8.dp),
                            )
                        }
                    }
                }
            }

            // Байланыс банері (Flutter _buildConnectionBanner паритеті).
            androidx.compose.animation.AnimatedVisibility(
                visible = showBanner,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                ConnectionBanner(
                    connecting = !state.loadTimedOut,
                    onRetry = { viewModel.retryLoadHistory() },
                )
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
            )
        }

        // Енгізу жолағы.
        ChatInputBar(
            input = state.input,
            onInputChange = viewModel::setInput,
            onSend = viewModel::sendMessage,
            replyTo = state.replyTo,
            onClearReply = viewModel::clearReply,
            editing = state.editing,
            onClearEditing = viewModel::clearEditing,
            readOnly = state.readOnly,
            canRecord = state.roomId != null || state.joinResponded,
            upload = state.upload,
            showDeliveryPrice = state.dealerDeliveryReplyYes,
            deliveryPrice = state.deliveryPrice,
            onDeliveryPriceChange = viewModel::setDeliveryPrice,
            onPickImage = {
                imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                )
            },
            onPickFile = { filePicker.launch(arrayOf("*/*")) },
            onPickAudio = { audioPicker.launch(arrayOf("audio/*")) },
            onPickLocation = { pickLocation() },
            onTakePhoto = { openCamera(video = false) },
            onRecordVideo = { openCamera(video = true) },
            hasMicPermission = {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            },
            onRequestMicPermission = { micPermission.launch(Manifest.permission.RECORD_AUDIO) },
            onHoldHint = { scope.launch { snackbar.showSnackbar(holdHintMsg) } },
            replyAuthor = state.replyTo?.let { target ->
                if (target.senderId == meId) target.senderName else roomTitle
            },
            onSendVoice = { path, duration -> viewModel.sendAudioRecording(path, duration) },
            onVoiceError = { scope.launch { snackbar.showSnackbar(recordErrorMsg) } },
            onVoiceTooShort = { scope.launch { snackbar.showSnackbar(voiceShortMsg) } },
        )
    }

    // --- Хабарлама әрекеттерінің парағы ---
    menuMessage?.let { target ->
        MessageActionSheet(
            message = target,
            isMine = target.senderId == meId,
            onDismiss = { menuMessage = null },
            onReply = {
                viewModel.setReplyTo(target)
                menuMessage = null
            },
            onEdit = {
                viewModel.startEditing(target)
                menuMessage = null
            },
            onCopy = {
                val raw = target.displayText()
                val text = if (raw.startsWith("http")) (sanitizeUrl(raw) ?: raw) else raw
                clipboard.setText(AnnotatedString(text))
                scope.launch { snackbar.showSnackbar(copiedMsg) }
                menuMessage = null
            },
            onDeleteForMe = {
                deleteTarget = target
                deleteForEveryone = false
                menuMessage = null
            },
            onDeleteForEveryone = {
                deleteTarget = target
                deleteForEveryone = true
                menuMessage = null
            },
        )
    }

    // --- Жою растауы ---
    deleteTarget?.let { target ->
        val confirmText = stringResource(
            if (deleteForEveryone) {
                L10nR.string.delete_message_confirm_own
            } else {
                L10nR.string.delete_message_confirm_other
            },
        )
        val deleteTitle = stringResource(L10nR.string.delete_message)
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(deleteTitle, style = MaterialTheme.typography.titleMedium) },
            text = { Text(confirmText, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMessage(target, deleteForEveryone)
                    deleteTarget = null
                }) {
                    Text(deleteLabel, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(cancelLabel) }
            },
        )
    }

    // --- Тарихты тазалау растауы ---
    if (clearConfirmOpen) {
        AlertDialog(
            onDismissRequest = { clearConfirmOpen = false },
            title = { Text(clearHistoryLabel, style = MaterialTheme.typography.titleMedium) },
            text = { Text(clearHistoryConfirmLabel, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = {
                    clearConfirmOpen = false
                    viewModel.clearHistory()
                }) {
                    Text(deleteLabel, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { clearConfirmOpen = false }) { Text(cancelLabel) }
            },
        )
    }

    // --- Толық экранды сурет қарау ---
    viewerUrl?.let { url ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { viewerUrl = null },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { viewerUrl = null },
                contentAlignment = Alignment.Center,
            ) {
                coil3.compose.AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = cancelLabel,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(28.dp)
                        .clickable { viewerUrl = null },
                )
            }
        }
    }
}

// ================= Бөлшектер =================

/** Күн ажыратқыштарымен тізім қатарлары. */
private fun buildRows(messages: List<ChatMessage>, todayLabel: String, yesterdayLabel: String): List<RoomRow> {
    val todayStart = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        clear(java.util.Calendar.MINUTE)
        clear(java.util.Calendar.SECOND)
        clear(java.util.Calendar.MILLISECOND)
    }.timeInMillis
    val yesterdayStart = todayStart - 24L * 60 * 60 * 1000
    val result = mutableListOf<RoomRow>()
    var lastDay = -1L
    for (message in messages) {
        val day = java.util.Calendar.getInstance().apply {
            timeInMillis = message.timestamp
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            clear(java.util.Calendar.MINUTE)
            clear(java.util.Calendar.SECOND)
            clear(java.util.Calendar.MILLISECOND)
        }.timeInMillis
        if (day != lastDay) {
            // Күн чипі: «Бүгін» / «Кеше» / күні (бұрын әр күнге уақыт жазылатын).
            val label = when {
                day >= todayStart -> todayLabel
                day >= yesterdayStart -> yesterdayLabel
                else -> FormatLastSeen.formatDate(day)
            }
            result.add(RoomRow.Day(day, label))
            lastDay = day
        }
        result.add(RoomRow.Msg(message))
    }
    return result
}

private fun rowKey(row: RoomRow, index: Int): String = when (row) {
    is RoomRow.Day -> "day_${row.millis}"
    is RoomRow.Msg -> row.message.localId?.let { "local_$it" }
        ?: row.message.id?.let { "msg_$it" }
        ?: "idx_$index"
}

/** Бөлме тақырып жолағы (WhatsApp): ‹ + аватар + атау/күй + қоңырау + мәзір. */
@Composable
private fun ChatRoomAppBar(
    title: String,
    subtitle: String,
    typing: Boolean,
    avatarUrl: String?,
    isOnline: Boolean,
    isSystemChat: Boolean,
    peerId: Long?,
    /** Дауыстық қоңырау батырмасы (peer-чаттерде ғана; null — жүйелік чат). */
    onVoiceCall: (() -> Unit)?,
    callActive: Boolean,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onArchive: () -> Unit,
    onBack: () -> Unit,
) {
    val palette = chatPalette()
    Surface(color = palette.inputBar, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBackIos,
                    contentDescription = stringResource(L10nR.string.common_cancel),
                    tint = palette.accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            ChatAvatar(
                avatarUrl = avatarUrl,
                name = title,
                isOnline = false,
                isSystemChat = isSystemChat,
                peerId = peerId,
                size = 40,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.text,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        fontSize = 12.5.sp,
                        color = if (typing || isOnline) palette.accent else palette.meta,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
            if (onVoiceCall != null) {
                IconButton(
                    onClick = onVoiceCall,
                    enabled = !callActive,
                ) {
                    Icon(
                        Icons.Outlined.Call,
                        contentDescription = stringResource(L10nR.string.call_incoming_title),
                        tint = if (callActive) palette.meta.copy(alpha = 0.45f) else palette.accent,
                        modifier = Modifier.size(25.dp),
                    )
                }
            }
            Box {
                IconButton(onClick = { onMenuOpenChange(true) }) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = null,
                        tint = palette.accent,
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { onMenuOpenChange(false) },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(L10nR.string.clear_chat_history)) },
                        leadingIcon = { Icon(Icons.Outlined.ClearAll, contentDescription = null) },
                        onClick = {
                            onMenuOpenChange(false)
                            onClearHistory()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(L10nR.string.archive_chat)) },
                        leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
                        onClick = {
                            onMenuOpenChange(false)
                            onArchive()
                        },
                    )
                }
            }
        }
    }
}

/** Байланыс банері: connecting → spinner; timeout → retry. */
@Composable
private fun ConnectionBanner(connecting: Boolean, onRetry: () -> Unit) {
    val palette = chatPalette()
    Surface(
        modifier = Modifier.padding(top = 12.dp),
        shape = RoundedCornerShape(50),
        color = palette.dayChip,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (connecting) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = palette.accent,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(L10nR.string.chat_connecting),
                    fontSize = 13.sp,
                    color = palette.meta,
                )
            } else {
                Icon(
                    Icons.Outlined.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(L10nR.string.chat_connection_lost_short),
                    fontSize = 13.sp,
                    color = palette.text,
                )
                TextButton(onClick = onRetry) {
                    Text(stringResource(L10nR.string.retry), color = palette.accent)
                }
            }
        }
    }
}

/** Күн чипі — WhatsApp-тағыдай ақ, жұмсақ көлеңкелі. */
@Composable
private fun DaySeparator(label: String) {
    val palette = chatPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = palette.dayChip,
            shadowElevation = 0.5.dp,
        ) {
            Text(
                label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = palette.meta,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

/**
 * Бір хабарлама қатары: тип бойынша көпіршек + (болса) жарнама карточкасы.
 * WhatsApp мінезі: оңға сырғыту → жауап беру (жебе иконкасы шығып, діріл),
 * ұзақ басу → әрекеттер парағы.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    message: ChatMessage,
    isMine: Boolean,
    state: ChatRoomUiState,
    viewModel: ChatRoomViewModel,
    meId: Long?,
    onLongPress: () -> Unit,
    onSwipeReply: () -> Unit,
    onOpenImage: (String) -> Unit,
    onPlayVideo: (String) -> Unit,
    onOpenFile: (url: String, name: String) -> Unit,
    onOpenMap: (Double, Double) -> Unit,
    onOpenAnnouncement: (Long) -> Unit,
    onAudioError: () -> Unit,
) {
    val palette = chatPalette()
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val triggerPx = with(density) { 64.dp.toPx() }
    val swipe = remember { androidx.compose.animation.core.Animatable(0f) }
    val dragValue = remember { floatArrayOf(0f) }
    val armed = remember { booleanArrayOf(false) }
    val rowScope = rememberCoroutineScope()
    val currentReply by androidx.compose.runtime.rememberUpdatedState(onSwipeReply)

    fun release() {
        if (armed[0]) currentReply()
        armed[0] = false
        dragValue[0] = 0f
        rowScope.launch {
            swipe.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 500f))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { release() },
                    onDragCancel = { release() },
                ) { change, dragAmount ->
                    val next = (dragValue[0] + dragAmount * 0.6f).coerceIn(0f, triggerPx * 1.3f)
                    if (next > 0f || dragValue[0] > 0f) change.consume()
                    dragValue[0] = next
                    rowScope.launch { swipe.snapTo(next) }
                    val nowArmed = next >= triggerPx
                    if (nowArmed && !armed[0]) {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                    armed[0] = nowArmed
                }
            },
    ) {
        // Сырғытқанда сол жақта шығатын жауап иконкасы.
        val progress = (swipe.value / triggerPx).coerceIn(0f, 1f)
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .size(34.dp)
                    .graphicsLayer {
                        alpha = progress
                        scaleX = 0.6f + 0.4f * progress
                        scaleY = 0.6f + 0.4f * progress
                    }
                    .clip(CircleShape)
                    .background(palette.dayChip),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Reply,
                    contentDescription = null,
                    tint = palette.meta,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = swipe.value }
                .combinedClickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onLongPress()
                    },
                )
                .padding(
                    start = if (isMine) 48.dp else 0.dp,
                    end = if (isMine) 0.dp else 48.dp,
                    top = 2.dp,
                    bottom = 2.dp,
                ),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        ) {
            MessageContent(
                message = message,
                isMine = isMine,
                state = state,
                viewModel = viewModel,
                onOpenImage = onOpenImage,
                onPlayVideo = onPlayVideo,
                onOpenFile = onOpenFile,
                onOpenMap = onOpenMap,
                onOpenAnnouncement = onOpenAnnouncement,
                onAudioError = onAudioError,
            )
        }
    }
}

/** Хабарлама типі бойынша көпіршек. */
@Composable
private fun MessageContent(
    message: ChatMessage,
    isMine: Boolean,
    state: ChatRoomUiState,
    viewModel: ChatRoomViewModel,
    onOpenImage: (String) -> Unit,
    onPlayVideo: (String) -> Unit,
    onOpenFile: (url: String, name: String) -> Unit,
    onOpenMap: (Double, Double) -> Unit,
    onOpenAnnouncement: (Long) -> Unit,
    onAudioError: () -> Unit,
) {
    val display = message.displayText()
    val deliveryRequest = viewModel.isDeliveryRequestMessage(message)
    val isCall = com.agroland.feature.chat.domain.CallMessage.isCallMessage(message.message)
    var cardShownInline = false
    when {
        isCall -> CallBubble(message = message, isMine = isMine)
        deliveryRequest && !isMine -> DeliveryRequestCard(
            message = message,
            answered = viewModel.isAnsweredDeliveryRequest(message),
            isDealer = true,
            onQuickReply = { yes -> viewModel.onDeliveryQuickReply(message, yes) },
        )
        deliveryRequest && isMine -> {
            if (viewModel.shouldShowDeliveryAnswer(message)) {
                DeliveryAnswerCard(
                    requestMessage = message,
                    chosen = state.buyerChosenIds.contains(message.id),
                    onChoose = { choice -> viewModel.onBuyerChoiceForRequest(message, choice) },
                )
            } else {
                DeliveryRequestCard(
                    message = message,
                    answered = false,
                    isDealer = false,
                    onQuickReply = {},
                )
            }
        }
        else -> when (InferMessageType.infer(message, display)) {
            InferredMessageType.IMAGE -> ImageBubble(
                url = InferMessageType.mediaUrlFor(message),
                timestamp = message.timestamp,
                isMine = isMine,
                message = message,
                onOpenImage = onOpenImage,
            )
            InferredMessageType.VIDEO -> VideoBubble(
                url = InferMessageType.mediaUrlFor(message),
                timestamp = message.timestamp,
                isMine = isMine,
                message = message,
                onPlayVideo = onPlayVideo,
            )
            InferredMessageType.AUDIO -> VoiceBubble(
                audioUrl = message.audioUrl ?: InferMessageType.mediaUrlFor(message),
                durationSec = message.audioDuration,
                isMine = isMine,
                message = message,
                onError = onAudioError,
            )
            InferredMessageType.FILE -> FileBubble(
                // file_name жоқ болса (сайт) — атын URL-ден аламыз.
                fileName = InferMessageType.displayFileName(message),
                url = InferMessageType.mediaUrlFor(message),
                isMine = isMine,
                message = message,
                onOpen = {
                    val url = InferMessageType.mediaUrlFor(message)
                    if (url != null) onOpenFile(url, InferMessageType.displayFileName(message) ?: "file")
                },
            )
            InferredMessageType.LOCATION -> LocationBubble(
                message = message,
                isMine = isMine,
                onOpenMap = {
                    InferMessageType.coordinatesFor(message)?.let { (lat, lng) -> onOpenMap(lat, lng) }
                },
            )
            else -> {
                // Хабарламада жарнама сілтемесі болса — превью көпіршек ішінде.
                val card = viewModel.announcementFor(message)
                cardShownInline = card != null && display.contains("agroland.kz/announcement", ignoreCase = true)
                TextBubble(
                    message = message,
                    isMine = isMine,
                    replyAuthor = null,
                    linkPreview = if (cardShownInline) card else null,
                    onOpenLinkPreview = onOpenAnnouncement,
                    onResend = { viewModel.resendMessage(message) },
                )
            }
        }
    }
    // Жарнама карточкасы — хабарлама астында бөлек элемент (тег арқылы келгенде).
    if (!deliveryRequest && !isCall && !cardShownInline) {
        viewModel.announcementFor(message)?.takeIf { card ->
            // Сол жарнаманың сілтеме-превьюі чатта бар болса — қайталамаймыз.
            state.messages.none { other ->
                other !== message &&
                    other.message.contains("agroland.kz/announcement", ignoreCase = true) &&
                    viewModel.announcementFor(other)?.id == card.id
            }
        }?.let { card ->
            Spacer(Modifier.height(4.dp))
            AnnouncementBanner(card = card) { onOpenAnnouncement(card.id) }
        }
    }
}

/** Хабарлама әрекеттерінің парағы (жауап/өңдеу/көшіру/жою). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActionSheet(
    message: ChatMessage,
    isMine: Boolean,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
) {
    val canEdit = isMine && message.messageType == "text" && message.id != null
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ActionSheetOption(
            icon = Icons.AutoMirrored.Outlined.Reply,
            label = stringResource(L10nR.string.reply),
            onClick = onReply,
        )
        if (canEdit) {
            ActionSheetOption(
                icon = Icons.Outlined.Edit,
                label = stringResource(L10nR.string.edit),
                onClick = onEdit,
            )
        }
        ActionSheetOption(
            icon = Icons.Outlined.ContentCopy,
            label = stringResource(L10nR.string.copy),
            onClick = onCopy,
        )
        ActionSheetOption(
            icon = Icons.Outlined.Delete,
            label = stringResource(L10nR.string.delete_for_me),
            onClick = onDeleteForMe,
            destructive = true,
        )
        if (isMine && message.id != null) {
            ActionSheetOption(
                icon = Icons.Outlined.Delete,
                label = stringResource(L10nR.string.delete_for_everyone),
                onClick = onDeleteForEveryone,
                destructive = true,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Парақтағы бір опция. */
@Composable
private fun ActionSheetOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 16.sp, color = tint)
    }
}

// ================= Көмекшілер =================

/** CONTENT_URI → DISPLAY_NAME. */
private fun android.content.Context.displayName(uri: Uri): String? = try {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
    }
} catch (_: Exception) {
    uri.lastPathSegment
}

/** Камера үшін FileProvider URI. */
private fun android.content.Context.newCameraUri(video: Boolean): Uri {
    val dir = File(cacheDir, "camera").apply { mkdirs() }
    val name = if (video) "video_${System.currentTimeMillis()}.mp4" else "photo_${System.currentTimeMillis()}.jpg"
    val file = File(dir, name)
    return androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
}

/** Бейне — сыртқы ойнатқыш (intent). */
private fun playVideoExternally(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(android.net.Uri.parse(url), "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}