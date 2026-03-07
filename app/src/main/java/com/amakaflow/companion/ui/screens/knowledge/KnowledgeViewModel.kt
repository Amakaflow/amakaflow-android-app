package com.amakaflow.companion.ui.screens.knowledge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.api.KnowledgeApi
import com.amakaflow.companion.data.model.KnowledgeCard
import com.amakaflow.companion.data.model.KnowledgeIngestRequest
import com.amakaflow.companion.data.model.KnowledgeSearchRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KnowledgeUiState(
    val cards: List<KnowledgeCard> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSaving: Boolean = false,
    val savedSuccess: Boolean = false,
)

@HiltViewModel
class KnowledgeViewModel @Inject constructor(
    private val api: KnowledgeApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(KnowledgeUiState())
    val uiState: StateFlow<KnowledgeUiState> = _uiState.asStateFlow()

    fun loadCards() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.listCards()
                _uiState.update { it.copy(isLoading = false, cards = response.items) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.searchCards(KnowledgeSearchRequest(query = query))
                _uiState.update { it.copy(isLoading = false, cards = response.items) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun ingest(url: String? = null, text: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, savedSuccess = false) }
            try {
                val request = KnowledgeIngestRequest(
                    sourceType = if (url != null) "url" else "text",
                    sourceUrl = url,
                    rawContent = text,
                )
                api.ingest(request)
                _uiState.update { it.copy(isSaving = false, savedSuccess = true) }
                loadCards()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun resetSavedSuccess() {
        _uiState.update { it.copy(savedSuccess = false) }
    }
}
