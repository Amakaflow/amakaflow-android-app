package com.amakaflow.companion.ui.screens.shareimport

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.api.IngestorApi
import com.amakaflow.companion.data.model.UrlImportRequest
import com.amakaflow.companion.data.worker.ImportWorker
import com.amakaflow.companion.util.PlatformDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShareImportUiState(
    val urls: List<ShareUrlItem> = emptyList(),
    val isImporting: Boolean = false,
    val importResult: ImportResult? = null,
    val error: String? = null,
)

data class ShareUrlItem(
    val url: String,
    val platform: PlatformDetector.Platform,
)

sealed class ImportResult {
    data class Success(val workoutName: String, val workoutId: String) : ImportResult()
    data class BackgroundEnqueued(val count: Int) : ImportResult()
}

/**
 * AMA-1258: ViewModel for the share intent import preview screen.
 * AMA-1259: Also used for deep link import — same UI, different entry point.
 */
@HiltViewModel
class ShareImportViewModel @Inject constructor(
    private val ingestorApi: IngestorApi,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareImportUiState())
    val uiState: StateFlow<ShareImportUiState> = _uiState.asStateFlow()

    /**
     * Parse incoming shared text and populate the URL list.
     */
    fun handleSharedText(sharedText: String?) {
        if (sharedText.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(error = "No content received")
            return
        }

        val urls = PlatformDetector.extractAllUrls(sharedText)
        if (urls.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "No URL found in shared content")
            return
        }

        val items = urls.map { url ->
            ShareUrlItem(url = url, platform = PlatformDetector.detectPlatform(url))
        }

        _uiState.value = _uiState.value.copy(urls = items, error = null)
    }

    /**
     * Handle multiple shared texts (ACTION_SEND_MULTIPLE).
     */
    fun handleMultipleSharedTexts(texts: List<String>) {
        val allUrls = texts.flatMap { PlatformDetector.extractAllUrls(it) }
        if (allUrls.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "No URLs found in shared content")
            return
        }

        val items = allUrls.map { url ->
            ShareUrlItem(url = url, platform = PlatformDetector.detectPlatform(url))
        }

        _uiState.value = _uiState.value.copy(urls = items, error = null)
    }

    /**
     * AMA-1259: Handle deep link error (missing param, unrecognized link).
     */
    fun handleDeepLinkError(message: String) {
        _uiState.value = _uiState.value.copy(error = message, urls = emptyList())
    }

    /**
     * Import a single URL immediately (foreground — used when the share sheet is visible).
     * For a single URL, we do the API call in the ViewModel so the user gets instant feedback.
     */
    fun importSingleUrl() {
        val items = _uiState.value.urls
        if (items.isEmpty()) return

        val item = items.first()
        _uiState.value = _uiState.value.copy(isImporting = true, error = null, importResult = null)

        viewModelScope.launch {
            try {
                val request = UrlImportRequest(url = item.url)
                val response = ingestorApi.importUrl(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.workout != null) {
                        _uiState.value = _uiState.value.copy(
                            isImporting = false,
                            importResult = ImportResult.Success(
                                workoutName = body.workout.name,
                                workoutId = body.workout.id,
                            ),
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isImporting = false,
                            error = body?.message ?: "Import failed: no workout returned",
                        )
                    }
                } else {
                    val detail = response.errorBody()?.string()?.take(200) ?: ""
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        error = "HTTP ${response.code()}: $detail",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    error = e.message ?: "Unknown error",
                )
            }
        }
    }

    /**
     * Enqueue all URLs for background import via WorkManager.
     * Used for bulk imports or when user wants to import in the background.
     */
    fun importInBackground() {
        val urls = _uiState.value.urls.map { it.url }
        if (urls.isEmpty()) return

        ImportWorker.enqueueBatch(appContext, urls)
        _uiState.value = _uiState.value.copy(
            importResult = ImportResult.BackgroundEnqueued(count = urls.size),
        )
    }
}
