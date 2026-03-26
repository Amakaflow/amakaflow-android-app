package com.amakaflow.companion.ui.screens.prhistory

import androidx.lifecycle.ViewModel
import com.amakaflow.companion.domain.usecase.pr.PRDetectionService
import com.amakaflow.companion.domain.usecase.pr.PersonalRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PRHistoryUiState(
    val groupedPRs: List<Pair<String, List<PersonalRecord>>> = emptyList()
)

@HiltViewModel
class PRHistoryViewModel @Inject constructor(
    private val prDetectionService: PRDetectionService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PRHistoryUiState())
    val uiState: StateFlow<PRHistoryUiState> = _uiState.asStateFlow()

    init {
        loadPRs()
    }

    private fun loadPRs() {
        _uiState.update {
            it.copy(groupedPRs = prDetectionService.prsByExercise())
        }
    }
}
