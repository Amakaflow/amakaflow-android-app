package com.amakaflow.companion.ui.screens.coach

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.model.CoachAction
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.PlannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CoachViewModel"

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val suggestions: List<String> = emptyList(),
    val actions: List<CoachAction> = emptyList()
)

data class CoachUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val conversationId: String? = null,
    val error: String? = null
)

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val plannerRepository: PlannerRepository
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

        viewModelScope.launch {
            plannerRepository.sendCoachMessage(
                message = message,
                conversationId = _uiState.value.conversationId
            ).collect { result ->
                when (result) {
                    is Result.Loading -> { /* already showing loading */ }
                    is Result.Success -> {
                        val response = result.data
                        val coachMessage = ChatMessage(
                            content = response.reply ?: "I'm not sure how to respond to that.",
                            isUser = false,
                            suggestions = response.suggestions,
                            actions = response.actions
                        )
                        _uiState.update {
                            it.copy(
                                messages = it.messages + coachMessage,
                                isLoading = false,
                                conversationId = response.conversationId ?: it.conversationId,
                                error = null
                            )
                        }
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Coach message error: ${result.message}")
                        _uiState.update {
                            it.copy(
                                messages = it.messages.filterNot { msg -> msg.id == userMessage.id },
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }
}
