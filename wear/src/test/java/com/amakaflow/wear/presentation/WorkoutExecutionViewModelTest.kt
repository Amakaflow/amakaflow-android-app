package com.amakaflow.wear.presentation

import com.amakaflow.shared.model.WearInterval
import com.amakaflow.shared.model.WearWorkout
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Tests for WorkoutExecutionUiState model and related logic.
 *
 * The ViewModel itself uses System.currentTimeMillis() and infinite timer loops,
 * which don't work well with StandardTestDispatcher. Those paths are tested
 * via integration/instrumented tests on device.
 *
 * These tests cover the critical UI state model logic.
 */
class WorkoutExecutionViewModelTest {

    // =========================================================================
    // WorkoutExecutionUiState Tests
    // =========================================================================

    @Test
    fun `formattedElapsed shows minutes and seconds`() {
        val state = WorkoutExecutionUiState(elapsedSeconds = 125)
        assertThat(state.formattedElapsed).isEqualTo("2:05")
    }

    @Test
    fun `formattedElapsed shows zero correctly`() {
        val state = WorkoutExecutionUiState(elapsedSeconds = 0)
        assertThat(state.formattedElapsed).isEqualTo("0:00")
    }

    @Test
    fun `formattedElapsed handles large values`() {
        val state = WorkoutExecutionUiState(elapsedSeconds = 3661)
        assertThat(state.formattedElapsed).isEqualTo("61:01")
    }

    @Test
    fun `formattedRemaining shows correct format`() {
        val state = WorkoutExecutionUiState(remainingSeconds = 45)
        assertThat(state.formattedRemaining).isEqualTo("0:45")
    }

    @Test
    fun `formattedRemaining shows zero correctly`() {
        val state = WorkoutExecutionUiState(remainingSeconds = 0)
        assertThat(state.formattedRemaining).isEqualTo("0:00")
    }

    @Test
    fun `isTimedInterval is true when interval has positive seconds`() {
        val state = WorkoutExecutionUiState(
            currentInterval = WearInterval(kind = "time", seconds = 30)
        )
        assertThat(state.isTimedInterval).isTrue()
    }

    @Test
    fun `isTimedInterval is false when seconds is null`() {
        val state = WorkoutExecutionUiState(
            currentInterval = WearInterval(kind = "reps", reps = 10)
        )
        assertThat(state.isTimedInterval).isFalse()
    }

    @Test
    fun `isTimedInterval is false when seconds is zero`() {
        val state = WorkoutExecutionUiState(
            currentInterval = WearInterval(kind = "time", seconds = 0)
        )
        assertThat(state.isTimedInterval).isFalse()
    }

    @Test
    fun `isTimedInterval is false when interval is null`() {
        val state = WorkoutExecutionUiState(currentInterval = null)
        assertThat(state.isTimedInterval).isFalse()
    }

    @Test
    fun `progressFraction is zero for empty workout`() {
        val state = WorkoutExecutionUiState(totalIntervals = 0, currentIntervalIndex = 0)
        assertThat(state.progressFraction).isEqualTo(0f)
    }

    @Test
    fun `progressFraction calculates correctly at start`() {
        val state = WorkoutExecutionUiState(totalIntervals = 4, currentIntervalIndex = 0)
        assertThat(state.progressFraction).isEqualTo(0f)
    }

    @Test
    fun `progressFraction calculates correctly at midpoint`() {
        val state = WorkoutExecutionUiState(totalIntervals = 4, currentIntervalIndex = 2)
        assertThat(state.progressFraction).isEqualTo(0.5f)
    }

    @Test
    fun `progressFraction calculates correctly near end`() {
        val state = WorkoutExecutionUiState(totalIntervals = 4, currentIntervalIndex = 3)
        assertThat(state.progressFraction).isEqualTo(0.75f)
    }

    @Test
    fun `default phase is LOADING`() {
        val state = WorkoutExecutionUiState()
        assertThat(state.phase).isEqualTo(ExecutionPhase.LOADING)
    }

    @Test
    fun `default values are correct`() {
        val state = WorkoutExecutionUiState()
        assertThat(state.workout).isNull()
        assertThat(state.currentInterval).isNull()
        assertThat(state.currentIntervalIndex).isEqualTo(0)
        assertThat(state.totalIntervals).isEqualTo(0)
        assertThat(state.remainingSeconds).isEqualTo(0)
        assertThat(state.elapsedSeconds).isEqualTo(0)
        assertThat(state.currentHeartRate).isNull()
        assertThat(state.error).isNull()
    }

    // =========================================================================
    // ExecutionPhase Tests
    // =========================================================================

    @Test
    fun `all execution phases exist`() {
        val phases = ExecutionPhase.values()
        assertThat(phases).hasLength(7)
        assertThat(phases.toList()).containsExactly(
            ExecutionPhase.LOADING,
            ExecutionPhase.COUNTDOWN,
            ExecutionPhase.ACTIVE,
            ExecutionPhase.REST,
            ExecutionPhase.PAUSED,
            ExecutionPhase.COMPLETED,
            ExecutionPhase.ERROR
        )
    }

    // =========================================================================
    // WearInterval data tests (used by ViewModel)
    // =========================================================================

    @Test
    fun `warmup interval has correct kind`() {
        val interval = WearInterval(kind = "warmup", name = "Warm Up", seconds = 120)
        assertThat(interval.kind).isEqualTo("warmup")
        assertThat(interval.seconds).isEqualTo(120)
    }

    @Test
    fun `reps interval has name reps and rest`() {
        val interval = WearInterval(kind = "reps", name = "Push Ups", reps = 15, restSeconds = 30, load = "bodyweight")
        assertThat(interval.reps).isEqualTo(15)
        assertThat(interval.restSeconds).isEqualTo(30)
        assertThat(interval.load).isEqualTo("bodyweight")
    }

    @Test
    fun `workout with intervals creates flat list`() {
        val workout = WearWorkout(
            id = "w1",
            name = "Test",
            sport = "strength",
            duration = 600,
            intervals = listOf(
                WearInterval(kind = "warmup", seconds = 60),
                WearInterval(kind = "reps", name = "Squats", reps = 12),
                WearInterval(kind = "reps", name = "Lunges", reps = 10),
                WearInterval(kind = "cooldown", seconds = 60)
            )
        )
        assertThat(workout.intervals).hasSize(4)
    }
}
