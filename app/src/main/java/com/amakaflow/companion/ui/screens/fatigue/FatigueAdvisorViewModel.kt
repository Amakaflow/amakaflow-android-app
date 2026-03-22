package com.amakaflow.companion.ui.screens.fatigue

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.model.FatigueAdvisorResponse
import com.amakaflow.companion.data.model.FatigueLevel
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.PlannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "FatigueAdvisorVM"

data class FatigueAdvisorUiState(
    val fatigueScore: Double = 0.0,
    val level: FatigueLevel = FatigueLevel.LOW,
    val advice: String? = null,
    val recommendations: List<String> = emptyList(),
    val contributingFactors: List<com.amakaflow.companion.data.model.FatigueFactor> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class FatigueAdvisorViewModel @Inject constructor(
    private val plannerRepository: PlannerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FatigueAdvisorUiState())
    val uiState: StateFlow<FatigueAdvisorUiState> = _uiState.asStateFlow()

    init {
        loadFatigueAdvice()
    }

    fun loadFatigueAdvice() {
        viewModelScope.launch {
            plannerRepository.getFatigueAdvice().collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> {
                        val data = result.data
                        _uiState.update {
                            it.copy(
                                fatigueScore = data.fatigueScore,
                                level = data.level,
                                advice = data.advice,
                                recommendations = data.recommendations,
                                contributingFactors = data.contributingFactors,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Fatigue advice error: ${result.message}")
                        _uiState.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                    }
                }
            }
        }
    }

    fun refresh() {
        loadFatigueAdvice()
    }
}
