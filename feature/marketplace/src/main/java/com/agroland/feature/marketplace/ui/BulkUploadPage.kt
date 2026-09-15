package com.agroland.feature.marketplace.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroAppBar
import com.agroland.core.ui.components.AgroButton
import com.agroland.core.ui.components.AgroScaffold
import com.agroland.core.ui.components.AgroTextButton
import com.agroland.core.ui.theme.extendedColors
import java.io.File

/**
 * BulkUploadPage — топтап жүктеу (bulk_upload_page.dart): Excel үлгісін
 * жүктеу, кестелі файлды таңдау және POST /announcements/bulk-upload.
 * Үлгі FileProvider арқылы share intent-пен беріледі.
 */
@Composable
fun BulkUploadPage(
    onBack: () -> Unit,
    viewModel: BulkUploadViewModel = hiltViewModel(),
) {
    val templateLoading by viewModel.templateLoading.collectAsState()
    val pickedFile by viewModel.pickedFile.collectAsState()
    val uploading by viewModel.uploading.collectAsState()

    val context = LocalContext.current
    val localeTag = LocalConfiguration.current.locales[0]?.toLanguageTag()

    val snackbar = remember { SnackbarHostState() }
    val noInternetText = stringResource(L10nR.string.error_no_internet)
    val genericErrorText = stringResource(L10nR.string.error_generic_message)
    val successText = stringResource(L10nR.string.bulk_upload_success)
    val errorsText = stringResource(L10nR.string.bulk_upload_errors)

    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> viewModel.pickFile(uri) }

    /** Медиатека емес — DocumentsUI (Excel/CSV/TSV). */
    fun launchPicker() = pickFile.launch(
        arrayOf(
            "application/*",
            "text/*",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
        ),
    )

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BulkUploadViewModel.Event.TemplateDownloaded -> shareTemplate(context, event.file)
                is BulkUploadViewModel.Event.Uploaded -> {
                    val result = event.result
                    val created = result.createdCount
                    val errors = result.errorCount
                    val parts = buildList {
                        if (created != null) add(successText.format(created))
                        if (errors != null) add(errorsText.format(errors))
                        result.message?.let { add(it) }
                    }
                    snackbar.showSnackbar(parts.joinToString(". ").ifBlank { successText.format(0) })
                }
                is BulkUploadViewModel.Event.ShowError ->
                    snackbar.showSnackbar(event.error.displayText(noInternetText, genericErrorText))
            }
        }
    }

    val fileName = remember(pickedFile) { pickedFile?.let(context::queryDisplayName) }

    AgroScaffold(
        topBar = {
            AgroAppBar(title = stringResource(L10nR.string.bulk_upload_title), onBack = onBack)
        },
    ) { inner ->
        Box(modifier = inner.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(L10nR.string.bulk_upload_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = extendedColors().secondaryText,
                )

                // 1-қадам — үлгіні жүктеу.
                Text(
                    text = stringResource(L10nR.string.bulk_step_template),
                    style = MaterialTheme.typography.bodyMedium,
                    color = extendedColors().primaryText,
                )
                AgroButton(
                    text = stringResource(L10nR.string.bulk_template_download),
                    onClick = { viewModel.downloadTemplate(localeTag ?: "ru") },
                    loading = templateLoading,
                )

                // 2-қадам — кестелі файлды таңдау.
                Text(
                    text = stringResource(L10nR.string.bulk_step_pick),
                    style = MaterialTheme.typography.bodyMedium,
                    color = extendedColors().primaryText,
                )
                if (pickedFile == null) {
                    AgroButton(
                        text = stringResource(L10nR.string.bulk_pick_file),
                        onClick = { launchPicker() },
                        containerColor = extendedColors().grey,
                        contentColor = extendedColors().primaryText,
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fileName
                                    ?: stringResource(L10nR.string.bulk_file_picked),
                                style = MaterialTheme.typography.bodySmall,
                                color = extendedColors().primaryText,
                            )
                        }
                        AgroTextButton(
                            text = stringResource(L10nR.string.common_change),
                            onClick = { launchPicker() },
                        )
                    }
                }

                // 3-қадам — жүктеу.
                AgroButton(
                    text = stringResource(L10nR.string.bulk_upload_button),
                    onClick = viewModel::upload,
                    enabled = pickedFile != null,
                    loading = uploading,
                )
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** Үлгі файлын share intent арқылы пайдаланушыға беру (FileProvider). */
private fun shareTemplate(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(share, file.name))
}

/** ContentResolver DISPLAY_NAME — таңдалған файлдың атауы. */
private fun android.content.Context.queryDisplayName(uri: android.net.Uri): String? =
    runCatching {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()