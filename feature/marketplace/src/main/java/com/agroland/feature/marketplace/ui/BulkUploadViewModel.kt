package com.agroland.feature.marketplace.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agroland.core.network.ApiResult
import com.agroland.feature.marketplace.data.BulkUploadResult
import com.agroland.feature.marketplace.data.MarketplaceRepository
import com.agroland.feature.profile.data.MultipartHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BulkUploadViewModel — үлгі жүктеу (.xlsx) + кестелі файлды топтап жүктеу.
 * Шаблон cache/templates ішіне жазылып, share intent арқылы пайдаланушыға беріледі.
 */
@HiltViewModel
class BulkUploadViewModel @Inject constructor(
    private val repository: MarketplaceRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    sealed interface Event {
        /** Үлгі дайын — File share intent үшін. */
        data class TemplateDownloaded(val file: File) : Event
        data class Uploaded(val result: BulkUploadResult) : Event
        data class ShowError(val error: MarketplaceError) : Event
    }

    private val _templateLoading = MutableStateFlow(false)
    val templateLoading: StateFlow<Boolean> = _templateLoading.asStateFlow()

    private val _pickedFile = MutableStateFlow<Uri?>(null)
    val pickedFile: StateFlow<Uri?> = _pickedFile.asStateFlow()

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    /** GET /announcements/bulk-upload/template?lang=… → cache/templates/bulk_template.xlsx. */
    fun downloadTemplate(langTag: String) {
        if (_templateLoading.value) return
        viewModelScope.launch {
            _templateLoading.value = true
            when (val result = repository.downloadBulkTemplate(langTag)) {
                is ApiResult.Success -> {
                    val file = withContext(Dispatchers.IO) {
                        val dir = File(appContext.cacheDir, "templates").apply { mkdirs() }
                        val target = File(dir, "bulk_template.xlsx")
                        result.value.byteStream().use { input ->
                            target.outputStream().use { output -> input.copyTo(output) }
                        }
                        target
                    }
                    _events.emit(Event.TemplateDownloaded(file))
                }
                is ApiResult.Error -> _events.emit(Event.ShowError(result.failure.toMarketplaceError()))
            }
            _templateLoading.value = false
        }
    }

    fun pickFile(uri: Uri?) {
        _pickedFile.value = uri
    }

    fun upload() {
        val uri = _pickedFile.value ?: return
        if (_uploading.value) return
        viewModelScope.launch {
            _uploading.value = true
            val part = MultipartHelper.toPart(appContext, uri, "file")
            if (part == null) {
                _events.emit(Event.ShowError(MarketplaceError()))
            } else {
                when (val result = repository.bulkUpload(part)) {
                    is ApiResult.Success -> {
                        _pickedFile.value = null
                        _events.emit(Event.Uploaded(result.value))
                    }
                    is ApiResult.Error -> _events.emit(Event.ShowError(result.failure.toMarketplaceError()))
                }
            }
            _uploading.value = false
        }
    }
}