package com.amakaflow.companion.ui.screens.aiimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.AppEnvironment
import com.amakaflow.companion.data.TestConfig
import com.amakaflow.companion.data.local.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Named

data class AIImportUiState(
    val isStreaming: Boolean = false,
    val events: List<String> = emptyList(),
    val isDone: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AIImportViewModel @Inject constructor(
    @Named("mcp") private val httpClient: OkHttpClient,
    private val secureStorage: SecureStorage,
    private val testConfig: TestConfig,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIImportUiState())
    val uiState: StateFlow<AIImportUiState> = _uiState.asStateFlow()

    private fun resolveProfileId(): String {
        if (testConfig.isTestModeEnabled) {
            testConfig.testUserId?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        // Parse user profile JSON stored in SecureStorage to extract the id field.
        return try {
            secureStorage.getUserProfile()
                ?.let { JSONObject(it).optString("id", "") }
                ?.takeIf { it.isNotEmpty() }
                ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }

    fun startImport(message: String, sourceUrl: String?) {
        _uiState.value = AIImportUiState(isStreaming = true)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseUrl = AppEnvironment.current.mcpApiUrl
                val body = JSONObject().apply {
                    put("profile_id", resolveProfileId())
                    put("message", message)
                    if (!sourceUrl.isNullOrBlank()) put("source_url", sourceUrl)
                }

                val request = Request.Builder()
                    .url("$baseUrl/api/v1/ai/import")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .header("Accept", "text/event-stream")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val detail = response.body?.string()?.take(200) ?: ""
                        _uiState.value = _uiState.value.copy(
                            isStreaming = false,
                            error = "HTTP ${response.code}: $detail",
                        )
                        return@launch
                    }

                    val source = response.body?.source() ?: run {
                        _uiState.value = _uiState.value.copy(isStreaming = false, error = "Empty response")
                        return@launch
                    }

                    // SSE parsing: buffer event name and data, flush on blank line.
                    var pendingEvent = ""
                    var pendingData = ""

                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        when {
                            line.startsWith("event: ") -> {
                                pendingEvent = line.removePrefix("event: ")
                                if (pendingEvent == "done") break
                            }
                            line.startsWith("data: ") -> {
                                pendingData = line.removePrefix("data: ")
                            }
                            line.isEmpty() -> {
                                // Blank line = dispatch the buffered event
                                val eventName = pendingEvent.ifEmpty { "message" }
                                if (pendingData.isNotEmpty()) {
                                    val display = "[$eventName] $pendingData"
                                    _uiState.value = _uiState.value.copy(
                                        events = _uiState.value.events + display,
                                    )
                                }
                                pendingEvent = ""
                                pendingData = ""
                            }
                        }
                    }

                    _uiState.value = _uiState.value.copy(isStreaming = false, isDone = true)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isStreaming = false,
                    error = e.message ?: "Unknown error",
                )
            }
        }
    }
}
