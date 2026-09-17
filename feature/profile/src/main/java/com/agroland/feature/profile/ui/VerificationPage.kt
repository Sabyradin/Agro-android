package com.agroland.feature.profile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agroland.core.common.formatters.DateFormatter
import com.agroland.core.common.validators.Validators
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroIconButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroTextField
import com.agroland.core.ui.components.BottomActionContainer
import com.agroland.core.ui.theme.extendedColors
import com.agroland.feature.profile.data.MultipartHelper
import com.agroland.feature.profile.data.VerificationStatus
import kotlinx.coroutines.launch

/**
 * KYC верификация (VerificationPage):
 *  - Status banner: pending/approved/rejected (+ rejection_reason, submitted_at)
 *  - full_name + ЖСН (12 цифр), құжаттар 1..5 файл (≤10МБ, jpeg/png/webp/pdf)
 *  - POST /verification/submit (multipart documents[]) → статус refresh
 *  - Расталған/қаралуда — форма құлыпталады.
 */
@Composable
fun VerificationPage(onBack: () -> Unit) {
    val viewModel = rememberProfileViewModel()
    val status by viewModel.verification.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var fullName by remember { mutableStateOf("") }
    var iin by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var iinError by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }

    val genericError = stringResource(L10nR.string.error_generic_message)
    val networkError = stringResource(L10nR.string.error_no_internet)
    val nameEmpty = stringResource(L10nR.string.auth_name_error)
    val iinInvalid = stringResource(L10nR.string.verification_iin_error)
    val minFiles = stringResource(L10nR.string.verification_min_files)
    val maxFiles = stringResource(L10nR.string.verification_max_files)
    val fileError = stringResource(L10nR.string.verification_file_error)
    val submittedToast = stringResource(L10nR.string.verification_submitted_toast)

    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val current = files.toMutableList()
        for (uri in uris) {
            if (current.size >= MAX_DOCUMENTS) break
            if (current.none { it == uri }) current.add(uri)
        }
        files = current
    }

    LaunchedEffect(Unit) {
        viewModel.loadVerificationStatus()
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ShowError -> {
                    val text = event.error.backendMessage
                        ?: if (event.error.isNetwork) networkError else genericError
                    snackbar.showSnackbar(text)
                }
                ProfileEvent.VerificationSubmitted -> {
                    files = emptyList()
                    snackbar.showSnackbar(submittedToast)
                }
                else -> Unit
            }
        }
    }

    val locked = status?.isLocked == true

    AgroScaffold(
        topBar = {
            AgroAppBar(
                title = stringResource(L10nR.string.profile_verification),
                onBack = onBack,
            )
        },
    ) { modifier ->
        Box(modifier = modifier) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                status?.let { StatusBanner(it) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(extendedColors().card)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AgroTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            nameError = false
                        },
                        label = stringResource(L10nR.string.verification_full_name),
                        isError = nameError,
                        errorText = nameEmpty,
                        enabled = !locked,
                    )
                    AgroTextField(
                        value = iin,
                        onValueChange = {
                            iin = it.filter(Char::isDigit).take(12)
                            iinError = false
                        },
                        label = stringResource(L10nR.string.verification_iin),
                        isError = iinError,
                        errorText = iinInvalid,
                        enabled = !locked,
                    )

                    // Құжаттар тізімі.
                    Text(
                        text = stringResource(L10nR.string.verification_documents),
                        style = MaterialTheme.typography.labelMedium,
                        color = extendedColors().secondaryText,
                    )
                    files.forEachIndexed { index, uri ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(extendedColors().grey)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = uri.lastPathSegment ?: "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = extendedColors().primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (!locked) {
                                AgroIconButton(
                                    icon = Icons.Rounded.Cancel,
                                    contentDescription = stringResource(L10nR.string.common_delete),
                                    tint = extendedColors().secondaryText,
                                    onClick = { files = files.filterNot { it == uri } },
                                )
                            }
                        }
                    }
                    if (!locked) {
                        AgroButton(
                            text = stringResource(L10nR.string.verification_pick),
                            onClick = { documentPicker.launch(arrayOf("image/*", "application/pdf")) },
                            enabled = files.size < MAX_DOCUMENTS,
                            containerColor = extendedColors().grey,
                            contentColor = extendedColors().primaryText,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Box(modifier = Modifier.height(110.dp))
            }

            BottomActionContainer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (locked) {
                    Text(
                        text = stringResource(L10nR.string.verification_locked),
                        style = MaterialTheme.typography.labelMedium,
                        color = extendedColors().secondaryText,
                    )
                } else {
                    AgroButton(
                        text = stringResource(L10nR.string.verification_submit),
                        onClick = {
                            val name = fullName.trim()
                            val iinValue = iin.trim()
                            when {
                                name.isEmpty() -> nameError = true
                                !Validators.isValidIin(iinValue) -> iinError = true
                                files.isEmpty() -> scope.launch { snackbar.showSnackbar(minFiles) }
                                else -> scope.launch {
                                    val parts = files.mapNotNull { MultipartHelper.toPart(context, it, "documents") }
                                    if (parts.size != files.size) {
                                        snackbar.showSnackbar(fileError)
                                        return@launch
                                    }
                                    viewModel.submitVerification(name, iinValue, parts)
                                }
                            }
                        },
                        enabled = !saving,
                        loading = saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

/** Статус банері — pending/approved/rejected + себебі + уақыты. */
@Composable
private fun StatusBanner(status: VerificationStatus) {
    val ext = extendedColors()
    val (icon, tint, titleRes) = when {
        status.isApproved -> Triple(
            Icons.Rounded.CheckCircle,
            MaterialTheme.colorScheme.primary,
            L10nR.string.verification_status_approved,
        )
        status.isPending -> Triple(
            Icons.Rounded.Schedule,
            ext.secondaryText,
            L10nR.string.verification_status_pending,
        )
        status.isRejected -> Triple(
            Icons.Rounded.ErrorOutline,
            MaterialTheme.colorScheme.error,
            L10nR.string.verification_status_rejected,
        )
        else -> return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Column {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.labelLarge,
                color = tint,
            )
            if (status.isRejected && !status.rejectionReason.isNullOrBlank()) {
                Text(
                    text = stringResource(L10nR.string.verification_rejection_reason, status.rejectionReason!!),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.primaryText,
                )
            }
            val submitted = DateFormatter.formatDateTime(status.submittedAt)
            if (submitted.isNotBlank()) {
                Text(
                    text = submitted,
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.secondaryText,
                )
            }
        }
    }
}

private const val MAX_DOCUMENTS = 5