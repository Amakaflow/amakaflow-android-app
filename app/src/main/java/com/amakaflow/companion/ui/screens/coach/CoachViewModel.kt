package com.amakaflow.companion.ui.screens.coach

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.AppEnvironment
import com.amakaflow.companion.data.model.CoachAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "CoachViewModel"

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val isStreaming: Boolean = false,
    val toolStatus: String? = null,
    val suggestions: List<String> = emptyList(),
    val actions: List<CoachAction> = emptyList()
)

data class CoachUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val sessionId: String? = null,
    val error: String? = null
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    @Named("coachSse") private val httpClient: OkHttpClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    init {
        // Start with a welcome message
        _uiState.update {
            it.copy(
                messages = listOf(
                    ChatMessage(
                        content = "Hi! I'm your AI training coach. Ask me anything about your training, recovery, or goals.",
                        isUser = false,
                        suggestions = listOf(
                            "How should I train this week?",
                            "Am I overtraining?",
                            "Suggest a recovery plan"
                        )
                    )
                )
            )
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        // Add user message
        val userMessage = ChatMessage(content = message, isUser = true)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseUrl = AppEnvironment.current.mapperApiUrl.trimEnd('/')
                val body = JSONObject().apply {
                    put("message", message)
                    _uiState.value.sessionId?.let { put("session_id", it) }
                    put("context", JSONObject.NULL)
                }

                val request = Request.Builder()
                    .url("$baseUrl/chat/stream")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .header("Accept", "text/event-stream")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val detail = response.body?.string()?.take(200) ?: ""
                        Log.e(TAG, "SSE error HTTP ${response.code}: $detail")
                        _uiState.update {
                            it.copy(
                                messages = it.messages.filterNot { msg -> msg.id == userMessage.id },
                                isLoading = false,
                                error = "Server error (${response.code})"
                            )
                        }
                        return@launch
                    }

                    val source = response.body?.source() ?: run {
                        _uiState.update {
                            it.copy(
                                messages = it.messages.filterNot { msg -> msg.id == userMessage.id },
                                isLoading = false,
                                error = "Empty response from coach"
                            )
                        }
                        return@launch
                    }

                    // Placeholder assistant message for streaming
                    val assistantMessageId = java.util.UUID.randomUUID().toString()
                    _uiState.update {
                        it.copy(
                            messages = it.messages + ChatMessage(
                                id = assistantMessageId,
                                content = "",
                                isUser = false,
                                isStreaming = true
                            )
                        )
                    }

                    var pendingEvent = ""
                    var pendingData = ""

                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        when {
                            line.startsWith("event: ") -> {
                                pendingEvent = line.removePrefix("event: ").trim()
                            }
                            line.startsWith("data: ") -> {
                                pendingData = line.removePrefix("data: ").trim()
                            }
                            line.isEmpty() -> {
                                // Blank line = dispatch the buffered event
                                val eventName = pendingEvent.ifEmpty { "message" }
                                if (pendingData.isNotEmpty()) {
                                    handleSseEvent(eventName, pendingData, assistantMessageId)
                                }
                                pendingEvent = ""
                                pendingData = ""
                            }
                        }
                    }

                    // Finalize: mark streaming done
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantMessageId) msg.copy(isStreaming = false, toolStatus = null)
                                else msg
                            },
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "SSE stream error", e)
                _uiState.update {
                    it.copy(
                        messages = it.messages.filterNot { msg -> msg.isStreaming },
                        isLoading = false,
                        error = e.message ?: "Connection error"
                    )
                }
            }
        }
    }

    private fun handleSseEvent(event: String, data: String, assistantMessageId: String) {
        try {
            when (event) {
                "message_start" -> {
                    // Capture session_id if present
                    val json = JSONObject(data)
                    val sessionId = json.optString("session_id").takeIf { it.isNotEmpty() }
                    if (sessionId != null) {
                        _uiState.update { it.copy(sessionId = sessionId) }
                    }
                }
                "content_delta" -> {
                    val json = JSONObject(data)
                    val delta = json.optString("text", "")
                    if (delta.isNotEmpty()) {
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages.map { msg ->
                                    if (msg.id == assistantMessageId) msg.copy(content = msg.content + delta)
                                    else msg
                                }
                            )
                        }
                    }
                }
                "function_call" -> {
                    val json = JSONObject(data)
                    val toolName = json.optString("name", "tool")
                    val status = toolStatusLabel(toolName)
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantMessageId) msg.copy(toolStatus = status)
                                else msg
                            }
                        )
                    }
                }
                "function_result" -> {
                    // Tool completed — clear the tool status indicator
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantMessageId) msg.copy(toolStatus = null)
                                else msg
                            }
                        )
                    }
                }
                "stage" -> {
                    val json = JSONObject(data)
                    val stage = json.optString("stage", "")
                    if (stage.isNotEmpty()) {
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages.map { msg ->
                                    if (msg.id == assistantMessageId) msg.copy(toolStatus = stage)
                                    else msg
                                }
                            )
                        }
                    }
                }
                "message_end" -> {
                    // Session may have been refreshed
                    val json = JSONObject(data)
                    val sessionId = json.optString("session_id").takeIf { it.isNotEmpty() }
                    if (sessionId != null) {
                        _uiState.update { it.copy(sessionId = sessionId) }
                    }
                }
                "error" -> {
                    val json = JSONObject(data)
                    val errorMsg = json.optString("message", "Unknown error from coach")
                    Log.e(TAG, "SSE error event: $errorMsg")
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantMessageId)
                                    msg.copy(
                                        content = msg.content.ifEmpty { "An error occurred. Please try again." },
                                        isStreaming = false,
                                        toolStatus = null
                                    )
                                else msg
                            },
                            isLoading = false,
                            error = errorMsg
                        )
                    }
                }
                // heartbeat / unknown events — ignore
                else -> {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse SSE event '$event': $data", e)
        }
    }

    private fun toolStatusLabel(toolName: String): String = when {
        toolName.contains("search", ignoreCase = true) -> "Searching..."
        toolName.contains("creat", ignoreCase = true) -> "Creating..."
        toolName.contains("schedul", ignoreCase = true) -> "Scheduling..."
        toolName.contains("analyz", ignoreCase = true) -> "Analyzing..."
        toolName.contains("fetch", ignoreCase = true) || toolName.contains("get", ignoreCase = true) -> "Fetching data..."
        else -> "Working..."
    }
}
