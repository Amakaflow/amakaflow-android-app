package com.amakaflow.companion.ui.screens.aiimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.AppEnvironment
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class AIImportUiState(
    val isStreaming: Boolean = false,
    val events: List<String> = emptyList(),
    val isDone: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AIImportViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AIImportUiState())
    val uiState: StateFlow<AIImportUiState> = _uiState.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun startImport(message: String, sourceUrl: String?) {
        _uiState.value = AIImportUiState(isStreaming = true)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseUrl = AppEnvironment.current.mcpApiUrl
                val body = JSONObject().apply {
                    put("profile_id", "current_user")
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
                        _uiState.value = _uiState.value.copy(
                            isStreaming = false,
                            error = "HTTP ${response.code}",
                        )
                        return@launch
                    }

                    val source = response.body?.source() ?: run {
                        _uiState.value = _uiState.value.copy(isStreaming = false, error = "Empty response")
                        return@launch
                    }

                    var eventName = ""
                    var dataBuffer = ""

                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        when {
                            line.startsWith("event: ") -> {
                                eventName = line.removePrefix("event: ")
                                if (eventName == "done") break
                            }
                            line.startsWith("data: ") -> {
                                dataBuffer = line.removePrefix("data: ")
                                if (eventName.isNotEmpty() && dataBuffer.isNotEmpty()) {
                                    val display = "[$eventName] $dataBuffer"
                                    _uiState.value = _uiState.value.copy(
                                        events = _uiState.value.events + display,
                                    )
                                    eventName = ""
                                    dataBuffer = ""
                                }
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
