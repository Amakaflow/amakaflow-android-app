package com.amakaflow.companion.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.model.WeeklySummary
import com.amakaflow.companion.data.model.Workout
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.usecase.completion.GetCompletionHistoryUseCase
import com.amakaflow.companion.domain.usecase.pairing.LoadPairingStateUseCase
import com.amakaflow.companion.domain.usecase.workout.GetPushedWorkoutsUseCase
import com.amakaflow.companion.simulation.SimulationSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "HomeViewModel"

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String? = null,
    val todayWorkouts: List<Workout> = emptyList(),
    val upcomingWorkouts: List<Workout> = emptyList(),
    val weeklyStats: WeeklySummary = WeeklySummary(0, 0, 0),
    val error: String? = null,
    // XP + Level (AMA-1285)
    val xpTotal: Int = 0,
    val currentLevel: Int = 1,
    val levelName: String = "Newcomer",
    val xpToNextLevel: Int = 500,
    val xpToday: Int = 0,
    val dailyCap: Int = 300
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getPushedWorkouts: GetPushedWorkoutsUseCase,
    private val getCompletionHistory: GetCompletionHistoryUseCase,
    private val loadPairingState: LoadPairingStateUseCase,
    val simulationSettings: SimulationSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var localWorkoutsJob: Job? = null

    init {
        loadData()
        loadWeeklyStats()
        observeUserProfile()
        observeLocalWorkouts()
    }

    /**
     * AMA-320: Observe local workouts from Room database.
     * This ensures workouts persist even after server filters synced workouts.
     */
    private fun observeLocalWorkouts() {
        localWorkoutsJob?.cancel()
        localWorkoutsJob = viewModelScope.launch {
            getPushedWorkouts.getLocal().collect { localWorkouts ->
                Log.d(TAG, "observeLocalWorkouts: ${localWorkouts.size} local workouts available")
                // Only update if we currently have no workouts (API returned empty)
                if (_uiState.value.todayWorkouts.isEmpty() && localWorkouts.isNotEmpty()) {
                    Log.d(TAG, "observeLocalWorkouts: Using local workouts since API returned empty")
                    _uiState.update {
                        it.copy(
                            todayWorkouts = localWorkouts,
                            upcomingWorkouts = localWorkouts
                        )
                    }
                }
            }
        }
    }

    private fun observeUserProfile() {
        viewModelScope.launch {
            loadPairingState.userProfile.collect { profile ->
                _uiState.update { it.copy(userName = profile?.name) }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            Log.d(TAG, "loadData: Starting to fetch pushed workouts")
            // Load pushed workouts from android-companion endpoint
            getPushedWorkouts().collect { result ->
                Log.d(TAG, "loadData: Got result: $result")
                when (result) {
                    is Result.Loading -> {
                        Log.d(TAG, "loadData: Loading...")
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is Result.Success -> {
                        Log.d(TAG, "loadData: Success! Found ${result.data.size} workouts from API: ${result.data.map { it.name }}")

                        // AMA-320: If API returns empty (server filtered synced workouts),
                        // fall back to local storage
                        val workoutsToShow = if (result.data.isEmpty()) {
                            Log.d(TAG, "loadData: API returned empty, checking local storage")
                            val localWorkouts = getPushedWorkouts.getLocalSync()
                            Log.d(TAG, "loadData: Found ${localWorkouts.size} local workouts")
                            localWorkouts
                        } else {
                            result.data
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                todayWorkouts = workoutsToShow,
                                upcomingWorkouts = workoutsToShow,
                                error = null
                            )
                        }
                        Log.d(TAG, "loadData: Updated state, todayWorkouts=${_uiState.value.todayWorkouts.size}")
                    }
                    is Result.Error -> {
                        Log.e(TAG, "loadData: Error - ${result.message}")
                        // AMA-320: On error, try to show local workouts
                        val localWorkouts = getPushedWorkouts.getLocalSync()
                        Log.d(TAG, "loadData: Error occurred, falling back to ${localWorkouts.size} local workouts")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                todayWorkouts = localWorkouts,
                                upcomingWorkouts = localWorkouts,
                                error = if (localWorkouts.isEmpty()) result.message else null
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadWeeklyStats() {
        viewModelScope.launch {
            getCompletionHistory(limit = 50, offset = 0).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val weeklyStats = WeeklySummary.fromCompletions(result.data.completions)
                        _uiState.update { it.copy(weeklyStats = weeklyStats) }
                    }
                    else -> { /* Keep default stats on error */ }
                }
            }
        }
    }

    fun refresh() {
        loadData()
        loadWeeklyStats()
    }
}
