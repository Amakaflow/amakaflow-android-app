package com.amakaflow.companion.ui.screens.nutrition

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.nutrition.DailyNutritionSummary
import com.amakaflow.companion.data.nutrition.NutritionDisplayMode
import com.amakaflow.companion.data.nutrition.NutritionHealthConnectService
import com.amakaflow.companion.data.nutrition.NutritionLabel
import com.amakaflow.companion.data.nutrition.NutritionSettings
import com.amakaflow.companion.data.nutrition.NutritionSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "NutritionViewModel"

data class NutritionUiState(
    val isLoading: Boolean = true,
    val isEnabled: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val isHealthConnectAvailable: Boolean = false,
    val hasPermissions: Boolean = false,
    val displayMode: NutritionDisplayMode = NutritionDisplayMode.QUALITATIVE,
    val nutrition: DailyNutritionSummary = DailyNutritionSummary(),
    val nutritionLabel: NutritionLabel = NutritionLabel.NO_DATA,
    val proteinTargetGrams: Int = 120,
    val waterTargetMl: Int = 2500,
    val error: String? = null
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val healthConnectService: NutritionHealthConnectService,
    private val settingsRepository: NutritionSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        checkHealthConnect()
    }

    private fun loadSettings() {
        val settings = settingsRepository.getSettings()
        _uiState.update {
            it.copy(
                isEnabled = settings.isEnabled,
                hasCompletedOnboarding = settings.hasCompletedOnboarding,
                displayMode = settings.displayMode,
                proteinTargetGrams = settings.proteinTargetGrams,
                waterTargetMl = settings.waterTargetMl
            )
        }
    }

    private fun checkHealthConnect() {
        viewModelScope.launch {
            val available = healthConnectService.isAvailable()
            val hasPerms = if (available) healthConnectService.hasPermissions() else false
            _uiState.update {
                it.copy(
                    isHealthConnectAvailable = available,
                    hasPermissions = hasPerms,
                    isLoading = false
                )
            }
            if (hasPerms && _uiState.value.isEnabled) {
                refreshNutrition()
            }
        }
    }

    fun refreshNutrition() {
        viewModelScope.launch {
            try {
                val summary = healthConnectService.getDailyNutrition()
                val label = computeNutritionLabel(summary)
                _uiState.update {
                    it.copy(
                        nutrition = summary,
                        nutritionLabel = label,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing nutrition", e)
                _uiState.update { it.copy(error = "Failed to load nutrition data") }
            }
        }
    }

    fun addProtein(grams: Int) {
        viewModelScope.launch {
            val success = healthConnectService.addProtein(grams.toDouble())
            if (success) {
                refreshNutrition()
            } else {
                _uiState.update { it.copy(error = "Failed to log protein") }
            }
        }
    }

    fun addWater(ml: Int = 250) {
        viewModelScope.launch {
            val success = healthConnectService.addWater(ml.toDouble())
            if (success) {
                refreshNutrition()
            } else {
                _uiState.update { it.copy(error = "Failed to log water") }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        settingsRepository.setEnabled(enabled)
        _uiState.update { it.copy(isEnabled = enabled) }
        if (enabled && _uiState.value.hasPermissions) {
            refreshNutrition()
        }
    }

    fun completeOnboarding() {
        settingsRepository.setOnboardingComplete(true)
        settingsRepository.setEnabled(true)
        _uiState.update {
            it.copy(
                hasCompletedOnboarding = true,
                isEnabled = true
            )
        }
        if (_uiState.value.hasPermissions) {
            refreshNutrition()
        }
    }

    fun setDisplayMode(mode: NutritionDisplayMode) {
        settingsRepository.setDisplayMode(mode)
        _uiState.update { it.copy(displayMode = mode) }
    }

    fun setProteinTarget(grams: Int) {
        settingsRepository.setProteinTarget(grams)
        _uiState.update { it.copy(proteinTargetGrams = grams) }
    }

    fun setWaterTarget(ml: Int) {
        settingsRepository.setWaterTarget(ml)
        _uiState.update { it.copy(waterTargetMl = ml) }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            healthConnectService.deleteAllNutritionData()
            settingsRepository.deleteAllData()
            _uiState.update {
                NutritionUiState(
                    isLoading = false,
                    isHealthConnectAvailable = it.isHealthConnectAvailable
                )
            }
        }
    }

    fun onPermissionsGranted() {
        _uiState.update { it.copy(hasPermissions = true) }
        if (_uiState.value.isEnabled) {
            refreshNutrition()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        /**
         * Compute a qualitative label from nutrition data.
         * Used in QUALITATIVE display mode for privacy-first approach.
         */
        fun computeNutritionLabel(summary: DailyNutritionSummary): NutritionLabel {
            if (summary.calories == 0.0 && summary.proteinGrams == 0.0) {
                return NutritionLabel.NO_DATA
            }
            // Check protein adequacy (rough heuristic: >80g is decent for most athletes)
            val proteinAdequate = summary.proteinGrams >= 80.0
            // Check calorie adequacy (rough: >1200 kcal means they've eaten something meaningful)
            val caloriesAdequate = summary.calories >= 1200.0

            return when {
                proteinAdequate && caloriesAdequate -> NutritionLabel.WELL_FUELED
                !proteinAdequate && caloriesAdequate -> NutritionLabel.LOW_PROTEIN
                proteinAdequate && !caloriesAdequate -> NutritionLabel.ON_TRACK
                else -> NutritionLabel.UNDER_EATING
            }
        }
    }
}
