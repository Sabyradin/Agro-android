package com.agroland.feature.marketplace.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.UploadFile
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.agroland.core.l10n.R as L10nR
import com.agroland.core.ui.components.AgroScaffold
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
    header: (@Composable () -> Unit)? = null,
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
            if (header != null) {
                header()
            } else {
                CreateHeader(
                    title = stringResource(L10nR.string.create_tab_bulk),
                    closeLabel = stringResource(L10nR.string.common_close),
                    onClose = onBack,
                )
            }
        },
    ) { inner ->
        Box(modifier = inner.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    // 1 — Excel шаблонын жүктеп алу.
                    FormCard {
                        BulkCardHeader(
                            icon = Icons.Rounded.TableChart,
                            title = stringResource(L10nR.string.bulk_template_title),
                            subtitle = stringResource(L10nR.string.bulk_template_subtitle),
                        )
                        FormDivider(start = 54.dp)
                        FormActionRow(
                            icon = Icons.Rounded.FileDownload,
                            text = stringResource(L10nR.string.bulk_template_action),
                            loading = templateLoading,
                            onClick = { viewModel.downloadTemplate(localeTag ?: "ru") },
                        )
                    }

                    // 2 — толтырылған файлды таңдау.
                    FormCard {
                        BulkCardHeader(
                            icon = Icons.Rounded.UploadFile,
                            title = stringResource(L10nR.string.bulk_file_title),
                            subtitle = stringResource(L10nR.string.bulk_file_subtitle),
                        )
                        FormDivider(start = 54.dp)
                        if (pickedFile == null) {
                            FormActionRow(
                                icon = Icons.AutoMirrored.Rounded.NoteAdd,
                                text = stringResource(L10nR.string.bulk_file_action),
                                onClick = { launchPicker() },
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { launchPicker() }
                                    .padding(horizontal = FormRowPadding, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.width(14.dp))
                                Text(
                                    text = fileName ?: stringResource(L10nR.string.bulk_file_picked),
                                    fontSize = 16.sp,
                                    color = extendedColors().primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = stringResource(L10nR.string.common_change),
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                CreateBottomBar {
                    CreateBarButton(
                        text = stringResource(L10nR.string.bulk_upload_server),
                        icon = Icons.Rounded.CloudUpload,
                        enabled = pickedFile != null,
                        loading = uploading,
                        onClick = viewModel::upload,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
            )
        }
    }
}

/** Жаппай жүктеу карточкасының басы: жасыл иконка + атау + сұр түсініктеме. */
@Composable
private fun BulkCardHeader(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FormRowPadding, vertical = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = extendedColors().primaryText)
            Spacer(Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 14.sp, color = extendedColors().secondaryText)
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