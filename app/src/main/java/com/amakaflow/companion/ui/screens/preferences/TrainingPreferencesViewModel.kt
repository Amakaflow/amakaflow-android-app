package com.amakaflow.companion.ui.screens.preferences

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.model.TrainingPreferences
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.PlannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "TrainingPrefsVM"

data class TrainingPreferencesUiState(
    val preferences: TrainingPreferences = TrainingPreferences(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TrainingPreferencesViewModel @Inject constructor(
    private val plannerRepository: PlannerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainingPreferencesUiState())
    val uiState: StateFlow<TrainingPreferencesUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            plannerRepository.getTrainingPreferences().collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                preferences = result.data,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Load preferences error: ${result.message}")
                        _uiState.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                    }
                }
            }
        }
    }

    fun updateDaysPerWeek(days: Int) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(daysPerWeek = days))
        }
    }

    fun updateMaxSessionMinutes(minutes: Int) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(maxSessionMinutes = minutes))
        }
    }

    fun updateFitnessLevel(level: String) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(fitnessLevel = level))
        }
    }

    fun updateGoal(goal: String) {
        _uiState.update {
            it.copy(preferences = it.preferences.copy(goal = goal))
        }
    }

    fun toggleSport(sport: String) {
        _uiState.update { state ->
            val current = state.preferences.preferredSports.toMutableList()
            if (current.contains(sport)) current.remove(sport) else current.add(sport)
            state.copy(preferences = state.preferences.copy(preferredSports = current))
        }
    }

    fun savePreferences() {
        _uiState.update { it.copy(isSaving = true, error = null, saveSuccess = false) }
        viewModelScope.launch {
            when (val result = plannerRepository.updateTrainingPreferences(_uiState.value.preferences)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            preferences = result.data,
                            isSaving = false,
                            saveSuccess = true,
                            error = null
                        )
                    }
                }
                is Result.Error -> {
                    Log.e(TAG, "Save preferences error: ${result.message}")
                    _uiState.update {
                        it.copy(isSaving = false, error = result.message)
                    }
                }
                is Result.Loading -> { /* won't happen for suspend */ }
            }
        }
    }
}
