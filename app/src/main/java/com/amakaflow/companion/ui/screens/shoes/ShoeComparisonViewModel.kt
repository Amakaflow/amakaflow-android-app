package com.amakaflow.companion.ui.screens.shoes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.model.ShoeComparison
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.PlannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ShoeComparisonVM"

data class ShoeComparisonUiState(
    val shoeInput: String = "",
    val sport: String? = null,
    val comparison: ShoeComparison? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ShoeComparisonViewModel @Inject constructor(
    private val plannerRepository: PlannerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoeComparisonUiState())
    val uiState: StateFlow<ShoeComparisonUiState> = _uiState.asStateFlow()

    fun updateShoeInput(input: String) {
        _uiState.update { it.copy(shoeInput = input) }
    }

    fun updateSport(sport: String?) {
        _uiState.update { it.copy(sport = sport) }
    }

    fun compare() {
        val shoes = _uiState.value.shoeInput
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (shoes.size < 2) {
            _uiState.update { it.copy(error = "Enter at least 2 shoes separated by commas") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            plannerRepository.compareShoes(shoes, _uiState.value.sport).collect { result ->
                when (result) {
                    is Result.Loading -> { /* already loading */ }
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(comparison = result.data, isLoading = false, error = null)
                        }
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Shoe comparison error: ${result.message}")
                        _uiState.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                    }
                }
            }
        }
    }
}
