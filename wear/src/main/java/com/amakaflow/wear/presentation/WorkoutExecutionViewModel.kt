package com.amakaflow.wear.presentation

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.shared.model.WearInterval
import com.amakaflow.shared.model.WearWorkout
import com.amakaflow.shared.model.WearWorkoutCompletion
import com.amakaflow.wear.data.repository.WearWorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Execution phases for the workout player.
 */
enum class ExecutionPhase {
    LOADING,
    COUNTDOWN,    // 3-2-1 before starting
    ACTIVE,       // Performing interval
    REST,         // Rest period between intervals
    PAUSED,
    COMPLETED,
    ERROR
}

/**
 * UI state for workout execution.
 */
data class WorkoutExecutionUiState(
    val phase: ExecutionPhase = ExecutionPhase.LOADING,
    val workout: WearWorkout? = null,
    val currentInterval: WearInterval? = null,
    val currentIntervalIndex: Int = 0,
    val totalIntervals: Int = 0,
    val remainingSeconds: Int = 0,
    val elapsedSeconds: Int = 0,
    val currentHeartRate: Int? = null,
    val countdownValue: Int = 3,
    val currentReps: Int = 0,
    val targetReps: Int = 0,
    val roundInfo: String? = null,
    val error: String? = null
) {
    val progressFraction: Float
        get() = if (totalIntervals > 0) {
            currentIntervalIndex.toFloat() / totalIntervals.toFloat()
        } else 0f

    val isTimedInterval: Boolean
        get() {
            val secs = currentInterval?.seconds
            return secs != null && secs > 0
        }

    val formattedElapsed: String
        get() {
            val minutes = elapsedSeconds / 60
            val seconds = elapsedSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }

    val formattedRemaining: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }
}

@HiltViewModel
class WorkoutExecutionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: WearWorkoutRepository,
    private val vibrator: Vibrator?
) : ViewModel() {

    private val workoutId: String = savedStateHandle["workoutId"] ?: ""

    private val _uiState = MutableStateFlow(WorkoutExecutionUiState())
    val uiState: StateFlow<WorkoutExecutionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var heartRateJob: Job? = null
    private var elapsedTimerJob: Job? = null
    private var flattenedIntervals: List<WearInterval> = emptyList()
    private var workoutStartTime: Long = 0L

    init {
        loadWorkout()
    }

    private fun loadWorkout() {
        viewModelScope.launch {
            try {
                val workout = repository.getWorkout(workoutId).first()
                if (workout != null) {
                    flattenedIntervals = flattenIntervals(workout.intervals)
                    _uiState.value = _uiState.value.copy(
                        workout = workout,
                        totalIntervals = flattenedIntervals.size,
                        phase = ExecutionPhase.COUNTDOWN,
                        countdownValue = 3
                    )
                    startCountdown()
                } else {
                    _uiState.value = _uiState.value.copy(
                        phase = ExecutionPhase.ERROR,
                        error = "Workout not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    phase = ExecutionPhase.ERROR,
                    error = e.message ?: "Failed to load workout"
                )
            }
        }
    }

    private fun startCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (i in 3 downTo 1) {
                _uiState.value = _uiState.value.copy(countdownValue = i)
                vibrateShort()
                delay(1000)
            }
            vibrateLong()
            startWorkout()
        }
    }

    private fun startWorkout() {
        workoutStartTime = System.currentTimeMillis()
        repository.resetHealthTracking()
        startHeartRateMonitoring()
        startElapsedTimer()
        startInterval(0)
    }

    private fun startInterval(index: Int) {
        if (index >= flattenedIntervals.size) {
            completeWorkout()
            return
        }

        val interval = flattenedIntervals[index]
        _uiState.value = _uiState.value.copy(
            phase = ExecutionPhase.ACTIVE,
            currentInterval = interval,
            currentIntervalIndex = index,
            remainingSeconds = interval.seconds ?: 0,
            targetReps = interval.reps ?: 0,
            currentReps = 0,
            roundInfo = interval.roundInfo
        )

        vibrateShort()

        val secs = interval.seconds
        if (secs != null && secs > 0) {
            startIntervalTimer(secs)
        }
        // For reps-based intervals, we wait for user to tap "done"
    }

    private fun startIntervalTimer(seconds: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                _uiState.value = _uiState.value.copy(remainingSeconds = remaining)
                if (remaining <= 3) vibrateShort()
                delay(1000)
            }
            _uiState.value = _uiState.value.copy(remainingSeconds = 0)
            vibrateLong()
            onIntervalComplete()
        }
    }

    private fun startRestPeriod(restSeconds: Int) {
        _uiState.value = _uiState.value.copy(
            phase = ExecutionPhase.REST,
            remainingSeconds = restSeconds
        )
        vibratePattern()

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (remaining in restSeconds downTo 1) {
                _uiState.value = _uiState.value.copy(remainingSeconds = remaining)
                if (remaining <= 3) vibrateShort()
                delay(1000)
            }
            _uiState.value = _uiState.value.copy(remainingSeconds = 0)
            vibrateLong()
            startInterval(_uiState.value.currentIntervalIndex + 1)
        }
    }

    private fun onIntervalComplete() {
        val currentIndex = _uiState.value.currentIntervalIndex
        val currentInterval = flattenedIntervals.getOrNull(currentIndex)
        val restSeconds = currentInterval?.restSeconds

        if (restSeconds != null && restSeconds > 0) {
            startRestPeriod(restSeconds)
        } else {
            startInterval(currentIndex + 1)
        }
    }

    private fun startElapsedTimer() {
        elapsedTimerJob?.cancel()
        elapsedTimerJob = viewModelScope.launch {
            while (true) {
                val elapsed = ((System.currentTimeMillis() - workoutStartTime) / 1000).toInt()
                _uiState.value = _uiState.value.copy(elapsedSeconds = elapsed)
                delay(1000)
            }
        }
    }

    private fun startHeartRateMonitoring() {
        heartRateJob?.cancel()
        heartRateJob = repository.startHeartRateMonitoring()
            .onEach { hr ->
                _uiState.value = _uiState.value.copy(currentHeartRate = hr)
                // Periodically send HR to phone
                repository.sendHeartRateToPhone(hr)
            }
            .launchIn(viewModelScope)
    }

    private fun completeWorkout() {
        timerJob?.cancel()
        elapsedTimerJob?.cancel()
        heartRateJob?.cancel()

        _uiState.value = _uiState.value.copy(phase = ExecutionPhase.COMPLETED)
        vibrateLong()

        viewModelScope.launch {
            val workout = _uiState.value.workout ?: return@launch
            val hrSummary = repository.getHeartRateSummary()
            val endTime = System.currentTimeMillis()
            val durationSeconds = ((endTime - workoutStartTime) / 1000).toInt()

            val completion = WearWorkoutCompletion(
                workoutId = workout.id,
                workoutName = workout.name,
                startedAt = workoutStartTime,
                endedAt = endTime,
                durationSeconds = durationSeconds,
                avgHeartRate = hrSummary?.average,
                maxHeartRate = hrSummary?.max,
                minHeartRate = hrSummary?.min
            )
            repository.submitCompletion(completion)
        }
    }

    // =========================================================================
    // User actions
    // =========================================================================

    fun onRepsCompleted() {
        onIntervalComplete()
    }

    fun onSkipRest() {
        timerJob?.cancel()
        startInterval(_uiState.value.currentIntervalIndex + 1)
    }

    fun onPause() {
        timerJob?.cancel()
        elapsedTimerJob?.cancel()
        _uiState.value = _uiState.value.copy(phase = ExecutionPhase.PAUSED)
    }

    fun onResume() {
        startElapsedTimer()
        val state = _uiState.value
        if (state.isTimedInterval && state.remainingSeconds > 0) {
            _uiState.value = state.copy(phase = ExecutionPhase.ACTIVE)
            startIntervalTimer(state.remainingSeconds)
        } else if (state.phase == ExecutionPhase.PAUSED) {
            // Was resting or doing reps
            _uiState.value = state.copy(phase = ExecutionPhase.ACTIVE)
        }
    }

    fun onEnd() {
        completeWorkout()
    }

    // =========================================================================
    // Haptics
    // =========================================================================

    private fun vibrateShort() {
        vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateLong() {
        vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibratePattern() {
        vibrator?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), -1)
        )
    }

    // =========================================================================
    // Interval flattening (simplified for watch)
    // =========================================================================

    private fun flattenIntervals(intervals: List<WearInterval>): List<WearInterval> {
        val result = mutableListOf<WearInterval>()
        for (interval in intervals) {
            if (interval.kind == "repeat" && interval.reps != null) {
                // Repeat blocks would be pre-flattened by phone, but handle just in case
                result.add(interval)
            } else {
                result.add(interval)
            }
        }
        return result
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        heartRateJob?.cancel()
        elapsedTimerJob?.cancel()
    }
}
