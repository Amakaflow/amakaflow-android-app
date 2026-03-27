package com.amakaflow.companion.ui.screens.nutrition

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.nutrition.BarcodeProductResponse
import com.amakaflow.companion.data.nutrition.FoodLoggingRepository
import com.amakaflow.companion.data.nutrition.NutritionHealthConnectService
import com.amakaflow.companion.data.nutrition.ParseTextResponse
import com.amakaflow.companion.data.nutrition.PhotoAnalysisResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "FoodLoggingVM"

/**
 * Active entry mode for the food logging screen.
 */
enum class FoodLoggingMode {
    PHOTO,
    BARCODE,
    TEXT
}

data class FoodLoggingUiState(
    val mode: FoodLoggingMode = FoodLoggingMode.TEXT,
    val isAnalyzing: Boolean = false,
    val photoResult: PhotoAnalysisResponse? = null,
    val barcodeResult: BarcodeProductResponse? = null,
    val textResult: ParseTextResponse? = null,
    val textInput: String = "",
    val addedToDaily: Boolean = false,
    val error: String? = null
)

/**
 * AMA-1294: ViewModel managing all three food logging flows:
 * 1. AI meal photo analysis (Claude Vision)
 * 2. Barcode scanning (Open Food Facts)
 * 3. NLP text entry ("I ate 2 eggs and toast")
 */
@HiltViewModel
class FoodLoggingViewModel @Inject constructor(
    private val foodLoggingRepository: FoodLoggingRepository,
    private val healthConnectService: NutritionHealthConnectService
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodLoggingUiState())
    val uiState: StateFlow<FoodLoggingUiState> = _uiState.asStateFlow()

    fun setMode(mode: FoodLoggingMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                photoResult = null,
                barcodeResult = null,
                textResult = null,
                addedToDaily = false,
                error = null
            )
        }
    }

    fun setTextInput(text: String) {
        _uiState.update { it.copy(textInput = text) }
    }

    // --- Photo Analysis ---

    fun analyzePhoto(imageBase64: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null, photoResult = null) }
            try {
                val result = foodLoggingRepository.analyzePhoto(imageBase64)
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        photoResult = result,
                        error = if (result == null) "Could not analyze photo" else null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Photo analysis failed", e)
                _uiState.update {
                    it.copy(isAnalyzing = false, error = "Photo analysis failed")
                }
            }
        }
    }

    // --- Barcode Lookup ---

    fun lookupBarcode(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null, barcodeResult = null) }
            try {
                val result = foodLoggingRepository.lookupBarcode(code)
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        barcodeResult = result,
                        error = if (result == null) "Product not found" else null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Barcode lookup failed", e)
                _uiState.update {
                    it.copy(isAnalyzing = false, error = "Barcode lookup failed")
                }
            }
        }
    }

    // --- NLP Text Entry ---

    fun parseText() {
        val text = _uiState.value.textInput.trim()
        if (text.isBlank()) {
            _uiState.update { it.copy(error = "Enter what you ate") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, error = null, textResult = null) }
            try {
                val result = foodLoggingRepository.parseText(text)
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        textResult = result,
                        error = if (result == null) "Could not parse food description" else null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Text parsing failed", e)
                _uiState.update {
                    it.copy(isAnalyzing = false, error = "Text parsing failed")
                }
            }
        }
    }

    // --- Add to Daily Totals ---

    /**
     * Add the current result's macros to Health Connect as a nutrition record.
     * Works for any of the three entry modes.
     */
    fun addToDaily() {
        viewModelScope.launch {
            val state = _uiState.value
            val (calories, protein, carbs, fat) = extractMacros(state) ?: run {
                _uiState.update { it.copy(error = "No nutrition data to add") }
                return@launch
            }

            _uiState.update { it.copy(isAnalyzing = true, error = null) }
            try {
                val success = healthConnectService.addNutritionRecord(
                    calories = calories,
                    proteinGrams = protein,
                    carbsGrams = carbs,
                    fatGrams = fat
                )
                if (success) {
                    _uiState.update {
                        it.copy(isAnalyzing = false, addedToDaily = true)
                    }
                } else {
                    _uiState.update {
                        it.copy(isAnalyzing = false, error = "Failed to save to Health Connect")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add to daily", e)
                _uiState.update {
                    it.copy(isAnalyzing = false, error = "Failed to save nutrition data")
                }
            }
        }
    }

    /**
     * Extract macros from whichever result is currently populated.
     * Returns (calories, protein, carbs, fat) or null.
     */
    private fun extractMacros(state: FoodLoggingUiState): MacroData? {
        state.photoResult?.let { r ->
            val avgCalories = r.caloriesRange.average()
            return MacroData(avgCalories, r.proteinG, r.carbsG, r.fatG)
        }
        state.barcodeResult?.let { r ->
            return MacroData(r.calories.toDouble(), r.protein, r.carbs, r.fat)
        }
        state.textResult?.let { r ->
            return MacroData(
                r.total.calories.toDouble(),
                r.total.protein,
                r.total.carbs,
                r.total.fat
            )
        }
        return null
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun reset() {
        _uiState.update {
            FoodLoggingUiState(mode = it.mode)
        }
    }

    private data class MacroData(
        val calories: Double,
        val protein: Double,
        val carbs: Double,
        val fat: Double
    )
}
