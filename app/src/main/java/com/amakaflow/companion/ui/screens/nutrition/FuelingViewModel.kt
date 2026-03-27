package com.amakaflow.companion.ui.screens.nutrition

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.nutrition.FuelingLevel
import com.amakaflow.companion.data.nutrition.FuelingRepository
import com.amakaflow.companion.data.nutrition.FuelingStatusResponse
import com.amakaflow.companion.data.nutrition.ProteinNudgeService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "FuelingViewModel"

data class FuelingUiState(
    val isLoading: Boolean = true,
    val fuelingStatus: FuelingStatusResponse? = null,
    val error: String? = null
)

/**
 * AMA-1293: ViewModel for the fueling status card.
 * Loads fueling status from the backend and exposes it to the UI.
 * Also triggers the protein nudge check after workout completion.
 */
@HiltViewModel
class FuelingViewModel @Inject constructor(
    private val fuelingRepository: FuelingRepository,
    private val proteinNudgeService: ProteinNudgeService
) : ViewModel() {

    private val _uiState = MutableStateFlow(FuelingUiState())
    val uiState: StateFlow<FuelingUiState> = _uiState.asStateFlow()

    init {
        loadFuelingStatus()
    }

    fun loadFuelingStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val status = fuelingRepository.getFuelingStatus()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        fuelingStatus = status,
                        error = if (status == null) "Unable to load fueling status" else null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading fueling status", e)
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to load fueling status")
                }
            }
        }
    }

    /**
     * Called after a workout completes to schedule the protein nudge.
     */
    fun onWorkoutCompleted() {
        viewModelScope.launch {
            proteinNudgeService.schedulePostWorkoutCheck(
                ProteinNudgeService.DELAY_MINUTES_DEFAULT
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
