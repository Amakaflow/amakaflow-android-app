package com.amakaflow.companion.domain.usecase.workout

import com.amakaflow.companion.data.model.*
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * AMA-293: Tests for ExecutionLogBuilder.
 */
class ExecutionLogBuilderTest {

    private fun makeRepsStep(name: String, reps: Int = 10, sets: Int = 3, index: Int = 0): FlattenedInterval {
        return FlattenedInterval(
            index = index,
            interval = WorkoutInterval.Reps(
                sets = sets,
                reps = reps,
                name = name
            ),
            roundInfo = null,
            hasRestAfter = true,
            restAfterSeconds = 60
        )
    }

    private fun makeTimedStep(name: String, seconds: Int, index: Int = 0): FlattenedInterval {
        return FlattenedInterval(
            index = index,
            interval = WorkoutInterval.Time(
                seconds = seconds,
                target = name
            ),
            roundInfo = null,
            hasRestAfter = false
        )
    }

    private fun makeWarmupStep(seconds: Int, index: Int = 0): FlattenedInterval {
        return FlattenedInterval(
            index = index,
            interval = WorkoutInterval.Warmup(seconds = seconds, target = "Warm Up"),
            roundInfo = null,
            hasRestAfter = false
        )
    }

    private fun makeRestStep(seconds: Int?, index: Int = 0): FlattenedInterval {
        return FlattenedInterval(
            index = index,
            interval = WorkoutInterval.Rest(seconds = seconds),
            roundInfo = null,
            hasRestAfter = false
        )
    }

    @Test
    fun `build returns ExecutionLog with correct version`() {
        val steps = listOf(makeRepsStep("Bench Press"))
        val builder = ExecutionLogBuilder(steps)
        val log = builder.build()

        assertThat(log.version).isEqualTo(2)
    }

    @Test
    fun `all intervals start as NOT_REACHED`() {
        val steps = listOf(
            makeWarmupStep(300, index = 0),
            makeRepsStep("Bench Press", index = 1),
            makeRepsStep("Squat", index = 2)
        )
        val builder = ExecutionLogBuilder(steps)
        val log = builder.build()

        assertThat(log.intervals).hasSize(3)
        assertThat(log.intervals.all { it.status == IntervalStatus.NOT_REACHED }).isTrue()
    }

    @Test
    fun `completeInterval marks interval as COMPLETED`() {
        val steps = listOf(makeWarmupStep(300, index = 0))
        val builder = ExecutionLogBuilder(steps)

        builder.startInterval(0)
        builder.completeInterval(0, actualDurationSeconds = 295)

        val log = builder.build()
        assertThat(log.intervals[0].status).isEqualTo(IntervalStatus.COMPLETED)
        assertThat(log.intervals[0].actualDurationSeconds).isEqualTo(295)
    }

    @Test
    fun `skipInterval marks interval as SKIPPED with reason`() {
        val steps = listOf(makeRepsStep("Bench Press", index = 0))
        val builder = ExecutionLogBuilder(steps)

        builder.skipInterval(0, "equipment_unavailable")

        val log = builder.build()
        assertThat(log.intervals[0].status).isEqualTo(IntervalStatus.SKIPPED)
        assertThat(log.intervals[0].skipReason).isEqualTo("equipment_unavailable")
    }

    @Test
    fun `recordSet adds set data to interval`() {
        val steps = listOf(makeRepsStep("Bench Press", reps = 10, sets = 3, index = 0))
        val builder = ExecutionLogBuilder(steps)

        builder.startInterval(0)
        builder.recordSet(
            intervalIndex = 0,
            setNumber = 1,
            repsPlanned = 10,
            repsCompleted = 10,
            weight = WeightEntry(
                displayLabel = "135 lbs",
                components = listOf(WeightComponent(source = "user", value = 135.0, unit = "lbs"))
            )
        )
        builder.recordSet(
            intervalIndex = 0,
            setNumber = 2,
            repsPlanned = 10,
            repsCompleted = 8
        )
        builder.completeInterval(0)

        val log = builder.build()
        val interval = log.intervals[0]

        assertThat(interval.sets).hasSize(2)
        assertThat(interval.sets!![0].setNumber).isEqualTo(1)
        assertThat(interval.sets!![0].repsCompleted).isEqualTo(10)
        assertThat(interval.sets!![0].weight?.displayLabel).isEqualTo("135 lbs")
        assertThat(interval.sets!![1].setNumber).isEqualTo(2)
        assertThat(interval.sets!![1].repsCompleted).isEqualTo(8)
    }

    @Test
    fun `build produces correct summary statistics`() {
        val steps = listOf(
            makeWarmupStep(300, index = 0),
            makeRepsStep("Bench Press", index = 1),
            makeRepsStep("Squat", index = 2),
            makeRepsStep("Deadlift", index = 3)
        )
        val builder = ExecutionLogBuilder(steps)
        builder.setWorkoutStartTime(1000000L)

        // Complete warmup and bench press
        builder.startInterval(0)
        builder.completeInterval(0, 300)
        builder.startInterval(1)
        builder.completeInterval(1, 120)

        // Skip squat
        builder.skipInterval(2, "injury")

        // Deadlift not reached

        builder.setWorkoutEndTime(1000000L + (420 * 1000)) // 420 seconds later

        val log = builder.build()
        val summary = log.summary!!

        assertThat(summary.totalIntervals).isEqualTo(4)
        assertThat(summary.completed).isEqualTo(2)
        assertThat(summary.skipped).isEqualTo(1)
        assertThat(summary.notReached).isEqualTo(1)
        assertThat(summary.completionPercentage).isEqualTo(50.0)
        assertThat(summary.totalDurationSeconds).isEqualTo(420)
        // Active duration = warmup (300) + bench press (120) = 420 (both are non-rest completed intervals)
        assertThat(summary.activeDurationSeconds).isEqualTo(420)
    }

    @Test
    fun `planned fields are correctly populated from intervals`() {
        val steps = listOf(
            makeWarmupStep(300, index = 0),
            makeRepsStep("Bench Press", reps = 10, sets = 3, index = 1),
            makeTimedStep("Plank", 60, index = 2)
        )
        val builder = ExecutionLogBuilder(steps)
        val log = builder.build()

        // Warmup
        assertThat(log.intervals[0].plannedName).isEqualTo("Warm Up")
        assertThat(log.intervals[0].plannedKind).isEqualTo("warmup")
        assertThat(log.intervals[0].plannedDurationSeconds).isEqualTo(300)

        // Reps exercise
        assertThat(log.intervals[1].plannedName).isEqualTo("Bench Press")
        assertThat(log.intervals[1].plannedKind).isEqualTo("reps")
        assertThat(log.intervals[1].plannedSets).isEqualTo(3)
        assertThat(log.intervals[1].plannedReps).isEqualTo(10)

        // Timed exercise
        assertThat(log.intervals[2].plannedName).isEqualTo("Plank")
        assertThat(log.intervals[2].plannedKind).isEqualTo("time")
        assertThat(log.intervals[2].plannedDurationSeconds).isEqualTo(60)
    }

    @Test
    fun `rest intervals are tracked correctly`() {
        val steps = listOf(makeRestStep(60, index = 0))
        val builder = ExecutionLogBuilder(steps)

        builder.startInterval(0)
        builder.completeInterval(0, 60)

        val log = builder.build()
        assertThat(log.intervals[0].plannedKind).isEqualTo("rest")
        assertThat(log.intervals[0].plannedDurationSeconds).isEqualTo(60)
        assertThat(log.intervals[0].status).isEqualTo(IntervalStatus.COMPLETED)
    }

    @Test
    fun `empty workout produces valid log`() {
        val builder = ExecutionLogBuilder(emptyList())
        val log = builder.build()

        assertThat(log.version).isEqualTo(2)
        assertThat(log.intervals).isEmpty()
        assertThat(log.summary).isNotNull()
        assertThat(log.summary!!.totalIntervals).isEqualTo(0)
    }

    @Test
    fun `summary counts sets correctly`() {
        val steps = listOf(makeRepsStep("Bench Press", index = 0))
        val builder = ExecutionLogBuilder(steps)

        builder.startInterval(0)
        builder.recordSet(0, SetLog(setNumber = 1, status = IntervalStatus.COMPLETED, repsPlanned = 10, repsCompleted = 10))
        builder.recordSet(0, SetLog(setNumber = 2, status = IntervalStatus.COMPLETED, repsPlanned = 10, repsCompleted = 10))
        builder.recordSet(0, SetLog(setNumber = 3, status = IntervalStatus.SKIPPED, repsPlanned = 10, skipReason = "fatigue"))
        builder.completeInterval(0)

        val log = builder.build()
        val summary = log.summary!!

        assertThat(summary.totalSets).isEqualTo(3)
        assertThat(summary.setsCompleted).isEqualTo(2)
        assertThat(summary.setsSkipped).isEqualTo(1)
    }

    @Test
    fun `intervals are sorted by index in output`() {
        val steps = listOf(
            makeRepsStep("A", index = 0),
            makeRepsStep("B", index = 1),
            makeRepsStep("C", index = 2)
        )
        val builder = ExecutionLogBuilder(steps)

        // Complete in reverse order
        builder.completeInterval(2)
        builder.completeInterval(0)
        builder.skipInterval(1)

        val log = builder.build()
        assertThat(log.intervals[0].intervalIndex).isEqualTo(0)
        assertThat(log.intervals[1].intervalIndex).isEqualTo(1)
        assertThat(log.intervals[2].intervalIndex).isEqualTo(2)
    }
}
