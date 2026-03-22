package com.amakaflow.companion.ui.screens.calendar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.model.DayState
import com.amakaflow.companion.data.model.DayStatus
import com.amakaflow.companion.data.model.ScheduleConflict
import com.amakaflow.companion.data.model.WeekPlan
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.PlannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "CalendarViewModel"

data class CalendarUiState(
    val isLoading: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val dayStates: Map<String, DayState> = emptyMap(),
    val conflicts: List<ScheduleConflict> = emptyList(),
    val weekPlan: WeekPlan? = null,
    val isGeneratingPlan: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val plannerRepository: PlannerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        loadWeekState(LocalDate.now())
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadDayState(date)
        loadConflicts(date)
    }

    fun navigateWeek(forward: Boolean) {
        val newDate = if (forward) {
            _uiState.value.selectedDate.plusWeeks(1)
        } else {
            _uiState.value.selectedDate.minusWeeks(1)
        }
        _uiState.update { it.copy(selectedDate = newDate) }
        loadWeekState(newDate)
    }

    fun generateWeekPlan(goalDescription: String? = null) {
        val startDate = _uiState.value.selectedDate.format(dateFormatter)
        _uiState.update { it.copy(isGeneratingPlan = true, error = null) }

        viewModelScope.launch {
            plannerRepository.generateWeek(startDate, goalDescription).collect { result ->
                when (result) {
                    is Result.Loading -> { /* already set */ }
                    is Result.Success -> {
                        Log.d(TAG, "Week plan generated successfully")
                        _uiState.update {
                            it.copy(
                                isGeneratingPlan = false,
                                weekPlan = result.data,
                                error = null
                            )
                        }
                        // Refresh week state to show new workouts
                        loadWeekState(_uiState.value.selectedDate)
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Generate week error: ${result.message}")
                        _uiState.update {
                            it.copy(isGeneratingPlan = false, error = result.message)
                        }
                    }
                }
            }
        }
    }

    private fun loadWeekState(date: LocalDate) {
        val startDate = date.format(dateFormatter)
        viewModelScope.launch {
            plannerRepository.getWeekState(startDate).collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> {
                        val dayMap = result.data.associateBy { it.date }
                        _uiState.update {
                            it.copy(isLoading = false, dayStates = it.dayStates + dayMap, error = null)
                        }
                    }
                    is Result.Error -> {
                        Log.e(TAG, "loadWeekState error: ${result.message}")
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    private fun loadDayState(date: LocalDate) {
        val dateStr = date.format(dateFormatter)
        viewModelScope.launch {
            plannerRepository.getDayState(dateStr).collect { result ->
                if (result is Result.Success) {
                    _uiState.update {
                        it.copy(dayStates = it.dayStates + (dateStr to result.data))
                    }
                }
            }
        }
    }

    private fun loadConflicts(date: LocalDate) {
        val dateStr = date.format(dateFormatter)
        viewModelScope.launch {
            plannerRepository.getConflicts(dateStr).collect { result ->
                if (result is Result.Success) {
                    _uiState.update { it.copy(conflicts = result.data) }
                }
            }
        }
    }

    fun getDayStatus(date: LocalDate): DayStatus {
        val dateStr = date.format(dateFormatter)
        return _uiState.value.dayStates[dateStr]?.status ?: DayStatus.REST
    }

    fun getDayWorkoutCount(date: LocalDate): Int {
        val dateStr = date.format(dateFormatter)
        return _uiState.value.dayStates[dateStr]?.workouts?.size ?: 0
    }
}
