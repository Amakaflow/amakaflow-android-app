package com.amakaflow.companion.domain.usecase.workout

import com.amakaflow.companion.data.model.*

/**
 * AMA-293: Builds an ExecutionLog during workout playback.
 *
 * Tracks what was actually performed vs. what was planned:
 * - Completed intervals and sets
 * - Skipped intervals (with reason)
 * - Actual durations vs. planned durations
 * - Weight/reps data per set
 * - Summary statistics
 *
 * Usage:
 *   val builder = ExecutionLogBuilder(flattenedSteps)
 *   builder.startInterval(0)          // Starting first interval
 *   builder.completeInterval(0, 295)  // Completed in 295s
 *   builder.skipInterval(1, "user_skipped")  // Skipped second interval
 *   builder.recordSet(2, SetLog(...))        // Record a set
 *   val log = builder.build()                // Get final ExecutionLog
 */
class ExecutionLogBuilder(
    private val flattenedSteps: List<FlattenedInterval>
) {
    private val intervalLogs = mutableMapOf<Int, IntervalLogState>()
    private var workoutStartTimeMs: Long = 0L
    private var workoutEndTimeMs: Long = 0L

    /** Internal mutable state for building an IntervalLog */
    private data class IntervalLogState(
        val intervalIndex: Int,
        val plannedName: String?,
        val plannedKind: String?,
        val plannedDurationSeconds: Int?,
        val plannedSets: Int?,
        val plannedReps: Int?,
        var status: IntervalStatus = IntervalStatus.NOT_REACHED,
        var actualDurationSeconds: Int? = null,
        var skipReason: String? = null,
        val sets: MutableList<SetLog> = mutableListOf(),
        var startedAtMs: Long? = null
    )

    init {
        // Pre-populate all intervals as NOT_REACHED
        for ((index, step) in flattenedSteps.withIndex()) {
            val kind = when (step.interval) {
                is WorkoutInterval.Warmup -> "warmup"
                is WorkoutInterval.Cooldown -> "cooldown"
                is WorkoutInterval.Time -> "time"
                is WorkoutInterval.Reps -> "reps"
                is WorkoutInterval.Distance -> "distance"
                is WorkoutInterval.Rest -> "rest"
                is WorkoutInterval.Repeat -> "repeat"
            }
            val plannedSets = when (val i = step.interval) {
                is WorkoutInterval.Reps -> i.sets
                else -> null
            }
            val plannedReps = when (val i = step.interval) {
                is WorkoutInterval.Reps -> i.reps
                else -> null
            }

            intervalLogs[index] = IntervalLogState(
                intervalIndex = index,
                plannedName = step.stepName,
                plannedKind = kind,
                plannedDurationSeconds = step.durationSeconds,
                plannedSets = plannedSets,
                plannedReps = plannedReps
            )
        }
    }

    /**
     * Record that the workout has started.
     */
    fun setWorkoutStartTime(timestampMs: Long) {
        workoutStartTimeMs = timestampMs
    }

    /**
     * Record that an interval has started.
     */
    fun startInterval(index: Int) {
        intervalLogs[index]?.let { state ->
            state.startedAtMs = System.currentTimeMillis()
            if (state.status == IntervalStatus.NOT_REACHED) {
                // Mark as in-progress (will be finalized by complete/skip)
                // Keep NOT_REACHED for now; it will be updated on complete/skip
            }
        }
    }

    /**
     * Mark an interval as completed with its actual duration.
     */
    fun completeInterval(index: Int, actualDurationSeconds: Int? = null) {
        intervalLogs[index]?.let { state ->
            state.status = IntervalStatus.COMPLETED
            state.actualDurationSeconds = actualDurationSeconds
                ?: state.startedAtMs?.let {
                    ((System.currentTimeMillis() - it) / 1000).toInt()
                }
        }
    }

    /**
     * Mark an interval as skipped.
     */
    fun skipInterval(index: Int, reason: String? = "user_skipped") {
        intervalLogs[index]?.let { state ->
            state.status = IntervalStatus.SKIPPED
            state.skipReason = reason
        }
    }

    /**
     * Record a set within an interval (for reps-based exercises).
     */
    fun recordSet(intervalIndex: Int, setLog: SetLog) {
        intervalLogs[intervalIndex]?.sets?.add(setLog)
    }

    /**
     * Record a set from weight tracking data.
     */
    fun recordSet(
        intervalIndex: Int,
        setNumber: Int,
        repsPlanned: Int?,
        repsCompleted: Int?,
        weight: WeightEntry? = null,
        rpe: Int? = null,
        modified: Boolean? = null
    ) {
        val log = SetLog(
            setNumber = setNumber,
            status = IntervalStatus.COMPLETED,
            repsPlanned = repsPlanned,
            repsCompleted = repsCompleted,
            weight = weight,
            rpe = rpe,
            modified = modified
        )
        recordSet(intervalIndex, log)
    }

    /**
     * Record that the workout ended.
     */
    fun setWorkoutEndTime(timestampMs: Long) {
        workoutEndTimeMs = timestampMs
    }

    /**
     * Build the final ExecutionLog.
     */
    fun build(): ExecutionLog {
        val intervals = intervalLogs.values
            .sortedBy { it.intervalIndex }
            .map { state ->
                IntervalLog(
                    intervalIndex = state.intervalIndex,
                    plannedName = state.plannedName,
                    plannedKind = state.plannedKind,
                    status = state.status,
                    plannedDurationSeconds = state.plannedDurationSeconds,
                    actualDurationSeconds = state.actualDurationSeconds,
                    plannedSets = state.plannedSets,
                    plannedReps = state.plannedReps,
                    sets = state.sets.ifEmpty { null },
                    skipReason = state.skipReason
                )
            }

        val summary = buildSummary(intervals)

        return ExecutionLog(
            version = 2,
            intervals = intervals,
            summary = summary
        )
    }

    private fun buildSummary(intervals: List<IntervalLog>): ExecutionSummary {
        val totalIntervals = intervals.size
        val completed = intervals.count { it.status == IntervalStatus.COMPLETED }
        val skipped = intervals.count { it.status == IntervalStatus.SKIPPED }
        val notReached = intervals.count { it.status == IntervalStatus.NOT_REACHED }
        val completionPercentage = if (totalIntervals > 0) {
            (completed.toDouble() / totalIntervals) * 100.0
        } else {
            0.0
        }

        val allSets = intervals.flatMap { it.sets ?: emptyList() }
        val totalSets = allSets.size
        val setsCompleted = allSets.count { it.status == IntervalStatus.COMPLETED }
        val setsSkipped = allSets.count { it.status == IntervalStatus.SKIPPED }

        val totalDurationSeconds = if (workoutEndTimeMs > 0 && workoutStartTimeMs > 0) {
            ((workoutEndTimeMs - workoutStartTimeMs) / 1000).toInt()
        } else {
            0
        }

        val activeDurationSeconds = intervals
            .filter { it.status == IntervalStatus.COMPLETED && it.plannedKind != "rest" }
            .sumOf { it.actualDurationSeconds ?: it.plannedDurationSeconds ?: 0 }

        return ExecutionSummary(
            totalIntervals = totalIntervals,
            completed = completed,
            skipped = skipped,
            notReached = notReached,
            completionPercentage = completionPercentage,
            totalSets = totalSets,
            setsCompleted = setsCompleted,
            setsSkipped = setsSkipped,
            totalDurationSeconds = totalDurationSeconds,
            activeDurationSeconds = activeDurationSeconds
        )
    }
}
