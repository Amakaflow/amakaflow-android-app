package com.amakaflow.companion.ui.screens.followalong

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.model.Workout
import com.amakaflow.companion.data.model.WorkoutInterval
import com.amakaflow.companion.debug.DebugLog
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.usecase.workout.GetWorkoutDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "FollowAlong"

/**
 * Represents a single step in a follow-along workout
 */
data class FollowAlongStep(
    val id: String,
    val name: String,
    val durationSeconds: Int?,
    val reps: Int?,
    val videoUrl: String?,
    val videoTimestamp: Double
) {
    val isTimeBased: Boolean get() = durationSeconds != null

    val formattedDuration: String
        get() {
            durationSeconds?.let { seconds ->
                val m = seconds / 60
                val s = seconds % 60
                return if (m > 0) String.format("%d:%02d", m, s) else "${s}s"
            }
            reps?.let { return "$it reps" }
            return ""
        }
}

/**
 * Phase of the follow-along player
 */
enum class FollowAlongPhase {
    LOADING, READY, PLAYING, PAUSED, ENDED
}

/**
 * UI state for the follow-along player
 */
data class FollowAlongUiState(
    val phase: FollowAlongPhase = FollowAlongPhase.LOADING,
    val steps: List<FollowAlongStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val elapsedSeconds: Int = 0,
    val stepRemainingSeconds: Int = 0,
    val errorMessage: String? = null,
    val videoUrl: String? = null,
    val workout: Workout? = null
) {
    val currentStep: FollowAlongStep?
        get() = steps.getOrNull(currentStepIndex)

    val nextStep: FollowAlongStep?
        get() = steps.getOrNull(currentStepIndex + 1)

    val progress: Float
        get() = if (steps.isEmpty()) 0f else currentStepIndex.toFloat() / steps.size

    val formattedElapsed: String
        get() {
            val h = elapsedSeconds / 3600
            val m = (elapsedSeconds % 3600) / 60
            val s = elapsedSeconds % 60
            return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
            else String.format("%d:%02d", m, s)
        }

    val isLastStep: Boolean
        get() = currentStepIndex >= steps.size - 1

    val canGoBack: Boolean
        get() = currentStepIndex > 0

    val canGoForward: Boolean
        get() = currentStepIndex < steps.size - 1
}

/**
 * AMA-1182: ViewModel for follow-along video playback with step tracking.
 * Manages media player state, current step progression, elapsed time,
 * and auto-advance when a timed step completes.
 */
@HiltViewModel
class FollowAlongPlayerViewModel @Inject constructor(
    private val getWorkoutDetail: GetWorkoutDetailUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val workoutId: String = savedStateHandle["workoutId"] ?: ""

    private val _uiState = MutableStateFlow(FollowAlongUiState())
    val uiState: StateFlow<FollowAlongUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadWorkout()
    }

    private fun loadWorkout() {
        DebugLog.info("Loading follow-along workout: $workoutId", TAG)
        viewModelScope.launch {
            getWorkoutDetail(workoutId).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.update { it.copy(phase = FollowAlongPhase.LOADING) }
                    }
                    is Result.Success -> {
                        val workout = result.data
                        val steps = extractSteps(workout)
                        if (steps.isEmpty()) {
                            _uiState.update {
                                it.copy(
                                    phase = FollowAlongPhase.ENDED,
                                    errorMessage = "No follow-along steps found in this workout.",
                                    workout = workout
                                )
                            }
                            return@collect
                        }

                        // Find first video URL for the player
                        val firstVideoUrl = steps.firstOrNull { it.videoUrl != null }?.videoUrl

                        DebugLog.success("Loaded follow-along: ${workout.name} (${steps.size} steps)", TAG)
                        _uiState.update {
                            it.copy(
                                phase = FollowAlongPhase.READY,
                                steps = steps,
                                currentStepIndex = 0,
                                elapsedSeconds = 0,
                                stepRemainingSeconds = steps.firstOrNull()?.durationSeconds ?: 0,
                                videoUrl = firstVideoUrl,
                                workout = workout,
                                errorMessage = null
                            )
                        }
                    }
                    is Result.Error -> {
                        DebugLog.error("Failed to load follow-along: ${result.message}", TAG)
                        _uiState.update {
                            it.copy(
                                phase = FollowAlongPhase.ENDED,
                                errorMessage = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    // MARK: - Playback Controls

    fun play() {
        val currentPhase = _uiState.value.phase
        if (currentPhase != FollowAlongPhase.READY && currentPhase != FollowAlongPhase.PAUSED) return
        _uiState.update { it.copy(phase = FollowAlongPhase.PLAYING) }
        startTimer()
    }

    fun pause() {
        if (_uiState.value.phase != FollowAlongPhase.PLAYING) return
        _uiState.update { it.copy(phase = FollowAlongPhase.PAUSED) }
        stopTimer()
    }

    fun togglePlayPause() {
        when (_uiState.value.phase) {
            FollowAlongPhase.PLAYING -> pause()
            FollowAlongPhase.READY, FollowAlongPhase.PAUSED -> play()
            else -> {}
        }
    }

    fun skipToNextStep() {
        val state = _uiState.value
        if (state.currentStepIndex >= state.steps.size - 1) {
            endWorkout()
            return
        }
        val newIndex = state.currentStepIndex + 1
        _uiState.update { it.copy(currentStepIndex = newIndex) }
        setupCurrentStep()
        if (_uiState.value.phase == FollowAlongPhase.PLAYING) {
            startTimer()
        }
    }

    fun skipToPreviousStep() {
        if (_uiState.value.currentStepIndex <= 0) return
        val newIndex = _uiState.value.currentStepIndex - 1
        _uiState.update { it.copy(currentStepIndex = newIndex) }
        setupCurrentStep()
        if (_uiState.value.phase == FollowAlongPhase.PLAYING) {
            startTimer()
        }
    }

    fun skipToStep(index: Int) {
        val state = _uiState.value
        if (index !in state.steps.indices) return
        _uiState.update { it.copy(currentStepIndex = index) }
        setupCurrentStep()
        if (_uiState.value.phase == FollowAlongPhase.PLAYING) {
            startTimer()
        }
    }

    fun endWorkout() {
        stopTimer()
        _uiState.update { it.copy(phase = FollowAlongPhase.ENDED) }
    }

    // MARK: - Timer

    private fun setupCurrentStep() {
        stopTimer()
        val step = _uiState.value.currentStep ?: return
        _uiState.update {
            it.copy(stepRemainingSeconds = step.durationSeconds ?: 0)
        }
    }

    private fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                tick()
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun tick() {
        _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }

        val step = _uiState.value.currentStep ?: return
        if (!step.isTimeBased) return

        val remaining = _uiState.value.stepRemainingSeconds
        if (remaining > 0) {
            _uiState.update { it.copy(stepRemainingSeconds = remaining - 1) }
        }

        if (_uiState.value.stepRemainingSeconds == 0) {
            // Auto-advance
            if (_uiState.value.isLastStep) {
                endWorkout()
            } else {
                skipToNextStep()
            }
        }
    }

    // MARK: - Step Extraction

    private fun extractSteps(workout: Workout): List<FollowAlongStep> {
        val result = mutableListOf<FollowAlongStep>()
        var timeOffset = 0.0
        var idCounter = 0

        fun process(intervals: List<WorkoutInterval>, roundPrefix: String? = null) {
            for (interval in intervals) {
                when (interval) {
                    is WorkoutInterval.Warmup -> {
                        val name = interval.target ?: "Warm Up"
                        result.add(FollowAlongStep(
                            id = "step-${idCounter++}",
                            name = name,
                            durationSeconds = interval.seconds,
                            reps = null,
                            videoUrl = null,
                            videoTimestamp = timeOffset
                        ))
                        timeOffset += interval.seconds.toDouble()
                    }
                    is WorkoutInterval.Cooldown -> {
                        val name = interval.target ?: "Cool Down"
                        result.add(FollowAlongStep(
                            id = "step-${idCounter++}",
                            name = name,
                            durationSeconds = interval.seconds,
                            reps = null,
                            videoUrl = null,
                            videoTimestamp = timeOffset
                        ))
                        timeOffset += interval.seconds.toDouble()
                    }
                    is WorkoutInterval.Time -> {
                        val name = interval.target ?: "Work"
                        result.add(FollowAlongStep(
                            id = "step-${idCounter++}",
                            name = name,
                            durationSeconds = interval.seconds,
                            reps = null,
                            videoUrl = null,
                            videoTimestamp = timeOffset
                        ))
                        timeOffset += interval.seconds.toDouble()
                    }
                    is WorkoutInterval.Reps -> {
                        val prefix = roundPrefix?.let { "$it - " } ?: ""
                        result.add(FollowAlongStep(
                            id = "step-${idCounter++}",
                            name = "$prefix${interval.name}",
                            durationSeconds = null,
                            reps = interval.reps,
                            videoUrl = interval.followAlongUrl,
                            videoTimestamp = timeOffset
                        ))
                        timeOffset += interval.reps * 3.0 // ~3s per rep estimate

                        // Add rest step if specified
                        if ((interval.restSec ?: 0) > 0) {
                            result.add(FollowAlongStep(
                                id = "step-${idCounter++}",
                                name = "Rest",
                                durationSeconds = interval.restSec,
                                reps = null,
                                videoUrl = null,
                                videoTimestamp = timeOffset
                            ))
                            timeOffset += (interval.restSec ?: 0).toDouble()
                        }
                    }
                    is WorkoutInterval.Distance -> {
                        val name = interval.target ?: "${interval.meters}m"
                        val estimatedSeconds = (interval.meters.toDouble() / 1000.0 * 360).toInt()
                        result.add(FollowAlongStep(
                            id = "step-${idCounter++}",
                            name = name,
                            durationSeconds = if (estimatedSeconds > 0) estimatedSeconds else null,
                            reps = null,
                            videoUrl = null,
                            videoTimestamp = timeOffset
                        ))
                        timeOffset += estimatedSeconds.toDouble()
                    }
                    is WorkoutInterval.Repeat -> {
                        for (round in 1..interval.reps) {
                            process(interval.intervals, "Round $round/${interval.reps}")
                        }
                    }
                    is WorkoutInterval.Rest -> {
                        val dur = interval.seconds ?: 30
                        result.add(FollowAlongStep(
                            id = "step-${idCounter++}",
                            name = "Rest",
                            durationSeconds = interval.seconds,
                            reps = null,
                            videoUrl = null,
                            videoTimestamp = timeOffset
                        ))
                        timeOffset += dur.toDouble()
                    }
                }
            }
        }

        process(workout.intervals)
        return result
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
