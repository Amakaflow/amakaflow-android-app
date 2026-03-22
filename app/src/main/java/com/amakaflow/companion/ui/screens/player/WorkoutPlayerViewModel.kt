package com.amakaflow.companion.ui.screens.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.model.*
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.usecase.completion.SubmitCompletionUseCase
import com.amakaflow.companion.domain.usecase.workout.ExecutionLogBuilder
import com.amakaflow.companion.domain.usecase.workout.GetWorkoutDetailUseCase
import com.amakaflow.companion.domain.usecase.workout.MarkWorkoutCompletedUseCase
import com.amakaflow.companion.debug.DebugLog
import com.amakaflow.companion.simulation.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

private const val TAG = "Workout"

data class WorkoutPlayerUiState(
    val isLoading: Boolean = true,
    val workout: Workout? = null,
    val phase: WorkoutPhase = WorkoutPhase.IDLE,
    val currentStepIndex: Int = 0,
    val remainingSeconds: Int = 0,
    val elapsedSeconds: Int = 0,
    val flattenedSteps: List<FlattenedInterval> = emptyList(),
    val restRemainingSeconds: Int = 0,
    val isManualRest: Boolean = false,
    val error: String? = null,
    val showEndConfirmation: Boolean = false,
    val workoutCompleted: Boolean = false,
    // AMA-287: Weight tracking for reps exercises
    val setNumber: Int = 1,
    val totalSetsForExercise: Int = 1,
    val suggestedWeight: Double? = null,
    val weightUnit: String = "lbs",
    // Simulation mode indicator
    val isSimulationMode: Boolean = false,
    val simulationSpeed: Double = 1.0
) {
    val currentStep: FlattenedInterval?
        get() = flattenedSteps.getOrNull(currentStepIndex)

    val nextStep: FlattenedInterval?
        get() = flattenedSteps.getOrNull(currentStepIndex + 1)

    val progress: Float
        get() {
            if (flattenedSteps.isEmpty()) return 0f
            return (currentStepIndex + 1).toFloat() / flattenedSteps.size
        }

    val formattedElapsedTime: String
        get() {
            val hours = elapsedSeconds / 3600
            val minutes = (elapsedSeconds % 3600) / 60
            val secs = elapsedSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, secs)
            } else {
                String.format("%d:%02d", minutes, secs)
            }
        }

    val formattedRemainingTime: String
        get() {
            val minutes = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return String.format("%d:%02d", minutes, secs)
        }

    val formattedRestTime: String
        get() {
            val minutes = restRemainingSeconds / 60
            val secs = restRemainingSeconds % 60
            return String.format("%d:%02d", minutes, secs)
        }

    val isPlaying: Boolean
        get() = phase == WorkoutPhase.RUNNING

    val isPaused: Boolean
        get() = phase == WorkoutPhase.PAUSED

    val isResting: Boolean
        get() = phase == WorkoutPhase.RESTING

    val canGoBack: Boolean
        get() = currentStepIndex > 0

    val canGoForward: Boolean
        get() = currentStepIndex < flattenedSteps.size - 1
}

@HiltViewModel
class WorkoutPlayerViewModel @Inject constructor(
    private val getWorkoutDetail: GetWorkoutDetailUseCase,
    private val submitCompletion: SubmitCompletionUseCase,
    private val markWorkoutCompleted: MarkWorkoutCompletedUseCase,
    private val simulationSettings: SimulationSettings,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val workoutId: String = savedStateHandle["workoutId"] ?: ""

    private val _uiState = MutableStateFlow(WorkoutPlayerUiState())
    val uiState: StateFlow<WorkoutPlayerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var workoutStartTime: Instant? = null

    // AMA-287: Weight tracking
    private val setLogs = mutableMapOf<String, MutableList<SetEntry>>() // exercise name -> set entries
    private var lastLoggedWeights = mutableMapOf<String, Double>() // exercise name -> last weight

    // AMA-293: Execution log builder — tracks what was actually done vs planned
    private var executionLogBuilder: ExecutionLogBuilder? = null

    // AMA-291: Simulation state tracking
    private var simulationSnapshot: SimulationSnapshot? = null
    private var simulatedHealthProvider: SimulatedHealthProvider? = null
    private var virtualElapsedSeconds: Int = 0 // Virtual time passed at normal speed

    init {
        loadWorkout()
    }

    private fun loadWorkout() {
        DebugLog.info("Loading workout: $workoutId", TAG)
        viewModelScope.launch {
            getWorkoutDetail(workoutId).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Result.Success -> {
                        val workout = result.data
                        val flattenedSteps = IntervalFlattener.flatten(workout.intervals)
                        DebugLog.success("Loaded workout: ${workout.name} (${flattenedSteps.size} steps)", TAG)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                workout = workout,
                                flattenedSteps = flattenedSteps,
                                error = null
                            )
                        }
                        // Auto-start the workout
                        start()
                    }
                    is Result.Error -> {
                        DebugLog.error("Failed to load workout: ${result.message}", TAG)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun start() {
        val currentState = _uiState.value
        if (currentState.flattenedSteps.isEmpty()) return

        DebugLog.info("Starting workout: ${currentState.workout?.name}", TAG)
        timerJob?.cancel()
        workoutStartTime = Clock.System.now()
        virtualElapsedSeconds = 0

        // AMA-293: Initialize execution log builder
        executionLogBuilder = ExecutionLogBuilder(currentState.flattenedSteps)
        executionLogBuilder?.setWorkoutStartTime(workoutStartTime!!.toEpochMilliseconds())
        DebugLog.info("AMA-293: Initialized ExecutionLogBuilder with ${currentState.flattenedSteps.size} steps", TAG)

        // AMA-291: Initialize simulation state and start workout
        viewModelScope.launch {
            val snapshot = simulationSettings.getSnapshot()
            simulationSnapshot = snapshot

            if (snapshot.isEnabled && snapshot.generateHealthData) {
                // Create health provider with accelerated clock for proper timestamps
                val clock = AcceleratedClock(snapshot.speed, workoutStartTime!!)
                simulatedHealthProvider = SimulatedHealthProvider(snapshot.hrProfile, clock)
                DebugLog.info("AMA-291: Initialized SimulatedHealthProvider (speed=${snapshot.speed}x)", TAG)
            }

            _uiState.update {
                it.copy(
                    phase = WorkoutPhase.RUNNING,
                    currentStepIndex = 0,
                    remainingSeconds = 0,
                    elapsedSeconds = 0,
                    isSimulationMode = snapshot.isEnabled,
                    simulationSpeed = snapshot.speed
                )
            }

            setupCurrentStep()
        }
    }

    fun pause() {
        if (_uiState.value.phase != WorkoutPhase.RUNNING) return
        DebugLog.debug("Workout paused", TAG)
        _uiState.update { it.copy(phase = WorkoutPhase.PAUSED) }
        timerJob?.cancel()
    }

    fun resume() {
        if (_uiState.value.phase != WorkoutPhase.PAUSED) return
        DebugLog.debug("Workout resumed", TAG)
        _uiState.update { it.copy(phase = WorkoutPhase.RUNNING) }
        startTimer()
    }

    fun togglePlayPause() {
        when (_uiState.value.phase) {
            WorkoutPhase.RUNNING -> pause()
            WorkoutPhase.PAUSED -> resume()
            else -> {}
        }
    }

    fun nextStep() {
        val currentState = _uiState.value
        val step = currentState.currentStep ?: return

        // AMA-293: Mark current interval as completed in execution log
        executionLogBuilder?.completeInterval(currentState.currentStepIndex)

        // Check if current step has rest after it (matches iOS behavior)
        if (step.hasRestAfter && currentState.phase != WorkoutPhase.RESTING) {
            // restAfterSeconds: null = manual rest, >0 = timed countdown
            enterRestPhase(step.restAfterSeconds)
            return
        }

        if (currentState.currentStepIndex >= currentState.flattenedSteps.size - 1) {
            end(EndReason.COMPLETED)
            return
        }

        _uiState.update { it.copy(currentStepIndex = it.currentStepIndex + 1) }
        setupCurrentStep()
    }

    fun previousStep() {
        if (_uiState.value.currentStepIndex <= 0) return
        _uiState.update { it.copy(currentStepIndex = it.currentStepIndex - 1) }
        setupCurrentStep()
    }

    fun skipRest() {
        if (_uiState.value.phase != WorkoutPhase.RESTING) return
        completeRest()
    }

    // AMA-287: Log weight for current set and advance to next step
    fun logSetWeight(weight: Double?, unit: String) {
        val currentState = _uiState.value
        val step = currentState.currentStep ?: return

        if (step.stepType != StepType.REPS) return

        val exerciseName = step.stepName
        val setNumber = currentState.setNumber

        // Record the weight
        val setEntry = SetEntry(
            setNumber = setNumber,
            weight = weight,
            unit = if (weight != null) unit else null,
            completed = true
        )

        // Add to set logs
        setLogs.getOrPut(exerciseName) { mutableListOf() }.add(setEntry)

        // Remember weight for next set of same exercise
        if (weight != null) {
            lastLoggedWeights[exerciseName] = weight
        }

        DebugLog.debug("Logged set: $exerciseName set $setNumber, weight=${weight ?: "skipped"} $unit", TAG)

        // AMA-293: Record set in execution log
        val weightEntry = if (weight != null) {
            WeightEntry(
                displayLabel = "$weight $unit",
                components = listOf(WeightComponent(source = "user", value = weight, unit = unit))
            )
        } else null
        executionLogBuilder?.recordSet(
            intervalIndex = currentState.currentStepIndex,
            setNumber = setNumber,
            repsPlanned = step.targetReps,
            repsCompleted = step.targetReps, // Assume completed as prescribed
            weight = weightEntry
        )

        // Advance to next step
        nextStep()
    }

    // AMA-287: Skip weight entry and advance
    fun skipSetWeight() {
        logSetWeight(null, _uiState.value.weightUnit)
    }

    fun showEndConfirmation() {
        _uiState.update { it.copy(showEndConfirmation = true) }
    }

    fun hideEndConfirmation() {
        _uiState.update { it.copy(showEndConfirmation = false) }
    }

    fun endAndSave() {
        hideEndConfirmation()
        end(EndReason.USER_ENDED)
    }

    fun endAndDiscard() {
        hideEndConfirmation()
        end(EndReason.DISCARDED)
    }

    fun end(reason: EndReason) {
        val currentState = _uiState.value
        DebugLog.info("Ending workout: reason=$reason, elapsed=${currentState.elapsedSeconds}s", TAG)

        // AMA-293: Finalize execution log
        executionLogBuilder?.setWorkoutEndTime(Clock.System.now().toEpochMilliseconds())
        // Mark current interval as completed if still on it
        if (reason == EndReason.COMPLETED) {
            executionLogBuilder?.completeInterval(currentState.currentStepIndex)
        }

        val workoutData = Triple(
            currentState.workout?.id,
            currentState.workout?.name,
            workoutStartTime
        )
        val duration = currentState.elapsedSeconds
        val intervals = currentState.workout?.intervals

        timerJob?.cancel()
        _uiState.update {
            it.copy(
                phase = WorkoutPhase.ENDED,
                workoutCompleted = reason == EndReason.COMPLETED || reason == EndReason.USER_ENDED
            )
        }

        // AMA-293: Build execution log
        val executionLog = executionLogBuilder?.build()
        DebugLog.info("AMA-293: ExecutionLog built - ${executionLog?.intervals?.size} intervals, " +
            "completed=${executionLog?.summary?.completed}, skipped=${executionLog?.summary?.skipped}", TAG)

        // Post completion to API if completed or user ended
        if (reason == EndReason.COMPLETED || reason == EndReason.USER_ENDED) {
            DebugLog.info("Posting workout completion...", TAG)
            postWorkoutCompletion(
                workoutId = workoutData.first,
                workoutName = workoutData.second,
                startedAt = workoutData.third,
                durationSeconds = duration,
                intervals = intervals,
                executionLog = executionLog
            )
        } else {
            DebugLog.debug("Workout discarded, not posting completion", TAG)
        }
    }

    // Timer Management

    private fun setupCurrentStep() {
        timerJob?.cancel()

        val step = _uiState.value.currentStep ?: return

        // AMA-293: Track interval start in execution log
        executionLogBuilder?.startInterval(_uiState.value.currentStepIndex)

        // AMA-291: Simulate health data for this step
        simulateHealthForStep(step)

        // AMA-287: Calculate set number and total sets for reps exercises
        val (setNumber, totalSets, suggestedWeight) = if (step.stepType == StepType.REPS) {
            calculateSetInfo(step)
        } else {
            Triple(1, 1, null)
        }

        // AMA-291: Log step info for debugging
        DebugLog.debug("Step: ${step.stepName}, type=${step.stepType}, targetReps=${step.targetReps}, set $setNumber/$totalSets", TAG)

        if (step.durationSeconds != null) {
            _uiState.update {
                it.copy(
                    remainingSeconds = step.durationSeconds!!,
                    setNumber = setNumber,
                    totalSetsForExercise = totalSets,
                    suggestedWeight = suggestedWeight
                )
            }
            if (_uiState.value.phase == WorkoutPhase.RUNNING) {
                startTimer()
            }
        } else {
            _uiState.update {
                it.copy(
                    remainingSeconds = 0,
                    setNumber = setNumber,
                    totalSetsForExercise = totalSets,
                    suggestedWeight = suggestedWeight
                )
            }
            // For reps-based exercises, still run elapsed timer
            if (_uiState.value.phase == WorkoutPhase.RUNNING) {
                startElapsedOnlyTimer()
            }
        }

        // AMA-308: Auto-select weight in simulation mode for REPS exercises
        if (step.stepType == StepType.REPS) {
            viewModelScope.launch {
                // AMA-291: Small delay to let Compose render the new step info first
                delay(100)

                val snapshot = simulationSettings.getSnapshot()
                if (snapshot.isEnabled && snapshot.simulateWeight) {
                    // Create weight provider with user's configured profile
                    val provider = SimulatedWeightProvider(snapshot.weightProfileEnum)
                    val simulatedWeight = provider.getSimulatedWeight(
                        step.stepName,
                        _uiState.value.weightUnit
                    )

                    // Apply realistic delay based on behavior profile, scaled by simulation speed
                    // AMA-291: Ensure minimum visible time of 2000ms so user can see reps/set info
                    val reactionTimeMs = (snapshot.behaviorProfile.reactionTime.random() * 1000).toLong()
                    val scaledDelayMs = (reactionTimeMs / snapshot.speed.toLong().coerceAtLeast(1)).coerceAtLeast(2000)
                    DebugLog.debug("AMA-291: UI rendered, waiting ${scaledDelayMs}ms before auto-weight for ${step.stepName} (set ${_uiState.value.setNumber}/${_uiState.value.totalSetsForExercise}, reps=${step.targetReps})", TAG)
                    delay(scaledDelayMs)

                    // Log the simulated weight
                    DebugLog.debug("AMA-308: Auto-selecting weight ${simulatedWeight ?: "null"} ${_uiState.value.weightUnit} for ${step.stepName}", TAG)
                    logSetWeight(simulatedWeight, _uiState.value.weightUnit)
                }
            }
        }
    }

    // AMA-287: Calculate set info for current exercise
    private fun calculateSetInfo(step: FlattenedInterval): Triple<Int, Int, Double?> {
        val exerciseName = step.stepName
        val allSteps = _uiState.value.flattenedSteps
        val currentIndex = _uiState.value.currentStepIndex

        // Count total sets of this exercise in the workout
        val totalSets = allSteps.count {
            it.stepType == StepType.REPS && it.stepName == exerciseName
        }

        // Count which set we're on (1-based)
        var setNumber = 1
        for (i in 0 until currentIndex) {
            val s = allSteps[i]
            if (s.stepType == StepType.REPS && s.stepName == exerciseName) {
                setNumber++
            }
        }

        // Get suggested weight from last logged weight for this exercise
        val suggestedWeight = lastLoggedWeights[exerciseName]

        return Triple(setNumber, totalSets, suggestedWeight)
    }

    // AMA-291: Simulate health data for a workout step
    private fun simulateHealthForStep(step: FlattenedInterval) {
        val provider = simulatedHealthProvider ?: return
        val durationSeconds = step.durationSeconds?.toDouble() ?: 30.0 // Default 30s for reps exercises

        // Determine intensity based on interval type
        val isRest = step.interval is WorkoutInterval.Rest
        val intensity = when (step.interval) {
            is WorkoutInterval.Rest -> ExerciseIntensity.REST
            is WorkoutInterval.Warmup -> ExerciseIntensity.LOW
            is WorkoutInterval.Cooldown -> ExerciseIntensity.LOW
            is WorkoutInterval.Reps -> ExerciseIntensity.MODERATE
            else -> ExerciseIntensity.HIGH // Time, Distance work intervals
        }

        // Generate health data for this step
        if (isRest) {
            provider.simulateRest(durationSeconds)
        } else {
            provider.simulateWork(durationSeconds, intensity)
        }

        DebugLog.debug("AMA-291: Simulated ${step.stepType} for ${durationSeconds}s, HR=${provider.currentHR}", TAG)
    }

    // AMA-291: Calculate virtual elapsed time increment based on simulation speed
    private fun getVirtualTimeIncrement(): Int {
        val snapshot = simulationSnapshot ?: return 1
        return if (snapshot.isEnabled) snapshot.speed.toInt().coerceAtLeast(1) else 1
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                timerTick()
            }
        }
    }

    private fun startElapsedOnlyTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                // AMA-291: Increment by virtual time in simulation mode
                val increment = getVirtualTimeIncrement()
                virtualElapsedSeconds += increment
                _uiState.update { it.copy(elapsedSeconds = virtualElapsedSeconds) }
            }
        }
    }

    private fun timerTick() {
        val currentState = _uiState.value

        // AMA-291: Increment by virtual time in simulation mode
        val increment = getVirtualTimeIncrement()
        virtualElapsedSeconds += increment
        _uiState.update { it.copy(elapsedSeconds = virtualElapsedSeconds) }

        if (currentState.remainingSeconds <= 0) {
            // For reps-based, don't auto-advance
            if (currentState.currentStep?.stepType == StepType.REPS) return
            nextStep()
            return
        }

        // AMA-291: Decrement remaining time by simulation speed increment
        _uiState.update { it.copy(remainingSeconds = (it.remainingSeconds - increment).coerceAtLeast(0)) }

        if (_uiState.value.remainingSeconds == 0) {
            viewModelScope.launch {
                delay(500)
                nextStep()
            }
        }
    }

    // Rest Phase

    private fun enterRestPhase(restSeconds: Int?) {
        timerJob?.cancel()
        _uiState.update { it.copy(phase = WorkoutPhase.RESTING) }

        if (restSeconds != null && restSeconds > 0) {
            _uiState.update {
                it.copy(
                    isManualRest = false,
                    restRemainingSeconds = restSeconds
                )
            }
            startRestTimer()
        } else if (simulationSnapshot?.isEnabled == true) {
            // AMA-291: In simulation mode, convert manual rest to short timed rest (3s)
            // This allows simulation to auto-advance without user interaction
            DebugLog.debug("AMA-291: Simulation mode - converting manual rest to 3s timed rest", TAG)
            _uiState.update {
                it.copy(
                    isManualRest = false,
                    restRemainingSeconds = 3
                )
            }
            startRestTimer()
        } else {
            _uiState.update {
                it.copy(
                    isManualRest = true,
                    restRemainingSeconds = 0
                )
            }
        }
    }

    private fun startRestTimer() {
        // AMA-291: Simulate rest health data
        val restDuration = _uiState.value.restRemainingSeconds
        simulatedHealthProvider?.simulateRest(restDuration.toDouble())

        timerJob = viewModelScope.launch {
            while (_uiState.value.restRemainingSeconds > 0 && isActive) {
                delay(1000)
                // AMA-291: Increment by virtual time in simulation mode
                val increment = getVirtualTimeIncrement()
                virtualElapsedSeconds += increment
                _uiState.update {
                    it.copy(
                        // AMA-291: Decrement rest time by simulation speed increment
                        restRemainingSeconds = (it.restRemainingSeconds - increment).coerceAtLeast(0),
                        elapsedSeconds = virtualElapsedSeconds
                    )
                }
            }
            if (_uiState.value.restRemainingSeconds == 0) {
                delay(500)
                completeRest()
            }
        }
    }

    private fun completeRest() {
        if (_uiState.value.phase != WorkoutPhase.RESTING) return

        timerJob?.cancel()
        _uiState.update {
            it.copy(
                restRemainingSeconds = 0,
                isManualRest = false
            )
        }

        val currentState = _uiState.value
        if (currentState.currentStepIndex >= currentState.flattenedSteps.size - 1) {
            end(EndReason.COMPLETED)
            return
        }

        _uiState.update {
            it.copy(
                currentStepIndex = it.currentStepIndex + 1,
                phase = WorkoutPhase.RUNNING
            )
        }
        setupCurrentStep()
    }

    // API

    private fun postWorkoutCompletion(
        workoutId: String?,
        workoutName: String?,
        startedAt: Instant?,
        durationSeconds: Int,
        intervals: List<WorkoutInterval>?,
        executionLog: ExecutionLog? = null
    ) {
        if (workoutId == null || startedAt == null) return

        viewModelScope.launch {
            try {
                // AMA-287: Build set logs for submission
                val allSteps = _uiState.value.flattenedSteps
                val setLogsForSubmission = buildSetLogsForSubmission(allSteps)

                // AMA-291: Get simulated health data if available
                val healthData = simulatedHealthProvider?.getCollectedData()
                val isSimulated = simulationSnapshot?.isEnabled ?: false

                // AMA-291: Calculate proper end time based on virtual duration
                val virtualEndedAt = Instant.fromEpochMilliseconds(
                    startedAt.toEpochMilliseconds() + (durationSeconds * 1000L)
                )

                DebugLog.info("AMA-291: Posting completion - duration=${durationSeconds}s, " +
                    "simulated=$isSimulated, healthData=${healthData != null}", TAG)

                // AMA-323: Only send workout_structure if we don't have set_logs.
                // When set_logs is present, the backend uses exercise_index to match against
                // workout_structure indices. However, for workouts with Repeat blocks, the
                // indices don't match (flattened steps vs original nested structure).
                // By not sending workout_structure when we have set_logs, the backend uses
                // its fallback logic that matches by exercise name instead of index.
                val hasSetLogs = setLogsForSubmission.isNotEmpty()
                val workoutStructureForSubmission = if (hasSetLogs) {
                    null
                } else {
                    intervals?.map { it.toSubmissionInterval() }
                }

                val submission = WorkoutCompletionSubmission(
                    workoutId = workoutId,
                    workoutName = workoutName ?: "Workout",
                    startedAt = startedAt,
                    endedAt = virtualEndedAt,
                    source = CompletionSource.PHONE,
                    healthMetrics = HealthMetrics(
                        avgHeartRate = healthData?.avgHR,
                        maxHeartRate = healthData?.maxHR,
                        minHeartRate = healthData?.minHR,
                        activeCalories = healthData?.calories,
                        totalCalories = healthData?.calories,
                        distanceMeters = null,
                        steps = healthData?.steps
                    ),
                    workoutStructure = workoutStructureForSubmission,
                    isSimulated = isSimulated,
                    setLogs = setLogsForSubmission.ifEmpty { null },
                    executionLog = executionLog  // AMA-293
                )
                val result = submitCompletion(submission)
                when (result) {
                    is Result.Success -> {
                        DebugLog.success("Workout completion posted: ${result.data.id}", TAG)
                        // AMA-320: Mark workout as completed in local storage
                        markWorkoutCompleted(workoutId)
                        DebugLog.info("AMA-320: Marked workout as completed in local storage", TAG)
                    }
                    is Result.Error -> {
                        DebugLog.error("Failed to post completion: ${result.message}", TAG)
                    }
                    is Result.Loading -> {}
                }
            } catch (e: Exception) {
                DebugLog.error("Exception posting completion: ${e.message}", TAG)
                DebugLog.error(e, TAG)
            }
        }
    }

    // AMA-287: Build ExerciseSetLog list for completion submission
    private fun buildSetLogsForSubmission(allSteps: List<FlattenedInterval>): List<ExerciseSetLog> {
        // Group set entries by exercise name and calculate exercise index
        val exerciseIndices = mutableMapOf<String, Int>()
        var currentIndex = 0

        for (step in allSteps) {
            if (step.stepType == StepType.REPS) {
                val name = step.stepName
                if (!exerciseIndices.containsKey(name)) {
                    exerciseIndices[name] = currentIndex++
                }
            }
        }

        return setLogs.mapNotNull { (exerciseName, entries) ->
            val exerciseIndex = exerciseIndices[exerciseName] ?: return@mapNotNull null
            if (entries.isEmpty()) return@mapNotNull null

            ExerciseSetLog(
                exerciseName = exerciseName,
                exerciseIndex = exerciseIndex,
                sets = entries.toList()
            )
        }.sortedBy { it.exerciseIndex }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        // AMA-291: Clean up simulation state
        simulatedHealthProvider = null
        simulationSnapshot = null
    }
}
