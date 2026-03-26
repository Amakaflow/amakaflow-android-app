package com.amakaflow.companion.ui.screens.rpefeedback

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.PlannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "RPEFeedbackVM"

/**
 * RPE difficulty option with emoji, label, and numeric RPE value.
 */
enum class RPEOption(val emoji: String, val label: String, val rpeValue: Int) {
    EASY("\uD83D\uDE0A", "Easy", 4),
    MODERATE("\uD83D\uDCAA", "Moderate", 6),
    HARD("\uD83D\uDD25", "Hard", 8),
    CRUSHED("\uD83D\uDC80", "Crushed", 10)
}

/**
 * Muscle groups available for soreness reporting.
 */
enum class MuscleGroup(val displayName: String, val apiValue: String) {
    CHEST("Chest", "chest"),
    BACK("Back", "back"),
    LEGS("Legs", "legs"),
    SHOULDERS("Shoulders", "shoulders"),
    ARMS("Arms", "arms"),
    CORE("Core", "core")
}

data class RPEFeedbackUiState(
    val selectedOption: RPEOption? = null,
    val selectedMuscles: Set<MuscleGroup> = emptySet(),
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val deloadRecommended: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RPEFeedbackViewModel @Inject constructor(
    private val plannerRepository: PlannerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RPEFeedbackUiState())
    val uiState: StateFlow<RPEFeedbackUiState> = _uiState.asStateFlow()

    fun selectOption(option: RPEOption) {
        _uiState.update { it.copy(selectedOption = option) }
    }

    fun toggleMuscle(muscle: MuscleGroup) {
        _uiState.update { state ->
            val updated = state.selectedMuscles.toMutableSet()
            if (updated.contains(muscle)) {
                updated.remove(muscle)
            } else {
                updated.add(muscle)
            }
            state.copy(selectedMuscles = updated)
        }
    }

    fun submit(workoutId: String, onComplete: () -> Unit) {
        val option = _uiState.value.selectedOption ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }

            val muscleSoreness = _uiState.value.selectedMuscles
                .takeIf { it.isNotEmpty() }
                ?.map { it.apiValue }

            val result = plannerRepository.submitRPEFeedback(
                workoutId = workoutId,
                rpe = option.rpeValue,
                muscleSoreness = muscleSoreness
            )

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isSubmitted = true,
                            deloadRecommended = result.data.deloadRecommended ?: false
                        )
                    }
                    // Brief delay to show success, then dismiss
                    kotlinx.coroutines.delay(1500)
                    onComplete()
                }
                is Result.Error -> {
                    Log.e(TAG, "Submit failed: ${result.message}")
                    _uiState.update {
                        it.copy(isSubmitting = false, error = "Could not save feedback")
                    }
                    // Still dismiss on error after brief delay
                    kotlinx.coroutines.delay(1000)
                    onComplete()
                }
                is Result.Loading -> { /* no-op */ }
            }
        }
    }
}
