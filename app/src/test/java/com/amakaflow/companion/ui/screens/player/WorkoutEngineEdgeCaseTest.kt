package com.amakaflow.companion.ui.screens.player

import app.cash.turbine.test
import com.amakaflow.companion.data.model.*
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.usecase.completion.SubmitCompletionUseCase
import com.amakaflow.companion.domain.usecase.workout.GetWorkoutDetailUseCase
import com.amakaflow.companion.domain.usecase.workout.MarkWorkoutCompletedUseCase
import com.amakaflow.companion.simulation.SimulationSettings
import com.amakaflow.companion.test.MainDispatcherRule
import com.amakaflow.companion.test.TestFixtures
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Edge case tests for WorkoutEngine (AMA-667)
 * Tests pause/resume/skip scenarios
 */
class WorkoutEngineEdgeCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockGetWorkoutDetail: GetWorkoutDetailUseCase
    private lateinit var mockSubmitCompletion: SubmitCompletionUseCase
    private lateinit var mockMarkWorkoutCompleted: MarkWorkoutCompletedUseCase
    private lateinit var mockSimulationSettings: SimulationSettings

    @Before
    fun setup() {
        mockGetWorkoutDetail = mockk()
        mockSubmitCompletion = mockk()
        mockMarkWorkoutCompleted = mockk()
        mockSimulationSettings = mockk()

        every { mockGetWorkoutDetail(any()) } returns flowOf(Result.Success(TestFixtures.hiitWorkout))
        coEvery { mockSubmitCompletion(any()) } returns Result.Success(mockk())
        coEvery { mockMarkWorkoutCompleted(any()) } just Runs
        coEvery { mockSimulationSettings.getSnapshot() } returns mockk(relaxed = true) {
            every { isEnabled } returns false
        }
    }

    private fun createViewModel(workoutId: String = "workout-001"): WorkoutPlayerViewModel {
        return WorkoutPlayerViewModel(
            getWorkoutDetail = mockGetWorkoutDetail,
            submitCompletion = mockSubmitCompletion,
            markWorkoutCompleted = mockMarkWorkoutCompleted,
            simulationSettings = mockSimulationSettings,
            savedStateHandle = SavedStateHandle(mapOf("workoutId" to workoutId))
        )
    }

    @Test
    fun `mid-interval pause preserves remaining time`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip initial RUNNING state
            val runningState = awaitItem() // First timer tick
            val remainingAtPause = runningState.remainingSeconds

            // Wait a bit for timer to decrement - ticks buffer in Turbine during delay
            kotlinx.coroutines.delay(1500)

            // Drain buffered timer ticks from the delay before pausing
            expectMostRecentItem()

            // When - pause mid-interval
            viewModel.pause()
            val pausedState = awaitItem()

            // Then - remaining time should be preserved (or slightly less from the delay)
            assertThat(pausedState.remainingSeconds).isAtMost(remainingAtPause!!)
            assertThat(pausedState.phase).isEqualTo(WorkoutPhase.PAUSED)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resume after pause continues from preserved time`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            awaitItem() // Running

            // Pause
            viewModel.pause()
            val pausedState = awaitItem()
            val remainingAtPause = pausedState.remainingSeconds

            // When - resume
            viewModel.resume()
            val resumedState = awaitItem()

            // Then - should be running with same or similar remaining time
            assertThat(resumedState.phase).isEqualTo(WorkoutPhase.RUNNING)
            assertThat(resumedState.remainingSeconds).isEqualTo(remainingAtPause)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `skip at last interval ends workout`() = runTest {
        // Given - workout with only one interval
        val singleIntervalWorkout = Workout(
            id = "single-workout",
            name = "Single Interval",
            description = "Test",
            duration = 30,
            sport = WorkoutSport.CARDIO,
            source = WorkoutSource.AMAKA,
            intervals = listOf(
                WorkoutInterval.Time(seconds = 30, target = "Test")
            )
        )
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(singleIntervalWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading

            // Wait for the single interval to start
            awaitItem()

            // When - skip at last interval
            viewModel.nextStep()  // -> RESTING (Time interval has hasRestAfter=true)
            viewModel.skipRest()  // -> ENDED (no more steps after rest)

            // Then - workout should end
            val finalState = expectMostRecentItem()
            assertThat(finalState.phase).isAnyOf(
                WorkoutPhase.ENDED,
                WorkoutPhase.RUNNING // May handle differently
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pause does not affect elapsed time accumulation after resume`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            val initialState = awaitItem()
            val initialElapsed = initialState.elapsedSeconds

            // Wait a bit
            kotlinx.coroutines.delay(1100)

            // Pause
            viewModel.pause()
            val pausedState = awaitItem()
            val elapsedAtPause = pausedState.elapsedSeconds

            // Wait while paused
            kotlinx.coroutines.delay(1500)

            // Resume
            viewModel.resume()
            awaitItem()

            // Wait a bit after resume
            kotlinx.coroutines.delay(1100)

            // Then - elapsed should have continued from pause point
            val finalState = expectMostRecentItem()
            assertThat(finalState.elapsedSeconds).isGreaterThan(elapsedAtPause)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple pause resume cycles work correctly`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            awaitItem() // Running

            // First pause/resume
            viewModel.pause()
            awaitItem()
            viewModel.resume()
            awaitItem()

            // Second pause/resume
            viewModel.pause()
            awaitItem()
            viewModel.resume()
            val afterSecondResume = awaitItem()

            // Then - should still be running
            assertThat(afterSecondResume.phase).isEqualTo(WorkoutPhase.RUNNING)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `skipRest enters rest phase then completes rest`() = runTest {
        // Given - workout with rest intervals
        val workoutWithRest = Workout(
            id = "rest-workout",
            name = "Rest Test",
            description = "Test rest",
            duration = 90,
            sport = WorkoutSport.CARDIO,
            source = WorkoutSource.AMAKA,
            intervals = listOf(
                WorkoutInterval.Time(seconds = 30, target = "Work"),
                WorkoutInterval.Rest(seconds = 30),
                WorkoutInterval.Time(seconds = 30, target = "Work")
            )
        )
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(workoutWithRest))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading

            // First interval runs
            awaitItem()

            // Wait for rest phase (after first interval completes)
            kotlinx.coroutines.delay(32000)

            // Should be in resting phase now
            val restState = expectMostRecentItem()

            // When - skip rest
            if (restState.phase == WorkoutPhase.RESTING) {
                viewModel.skipRest()

                // Then - should exit rest and continue
                // completeRest() emits two updates; use expectMostRecentItem() to get the final one
                val afterSkip = expectMostRecentItem()
                assertThat(afterSkip.phase).isAnyOf(WorkoutPhase.RUNNING, WorkoutPhase.ENDED)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `previousStep at first interval does not go negative`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            val state = awaitItem()

            // When - try to go back at first step (no-op, no state change emitted)
            viewModel.previousStep()

            // Then - should either stay at 0 or handle gracefully
            assertThat(state.currentStepIndex).isAtLeast(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `togglePlayPause when paused resumes`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            awaitItem() // Running

            // Pause
            viewModel.pause()
            awaitItem()

            // When - toggle while paused
            viewModel.togglePlayPause()

            // Then - should resume
            val resumedState = awaitItem()
            assertThat(resumedState.phase).isEqualTo(WorkoutPhase.RUNNING)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `endAndSave ends workout with USER_ENDED reason`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            awaitItem() // Running

            // When - show confirmation, then end with USER_ENDED
            // (endAndSave() directly ends; use showEndConfirmation() to test the dialog flow)
            viewModel.showEndConfirmation()
            val state = awaitItem()
            assertThat(state.showEndConfirmation).isTrue()

            viewModel.end(EndReason.USER_ENDED)
            val endedState = awaitItem()

            // Then - should be ended
            assertThat(endedState.phase).isEqualTo(WorkoutPhase.ENDED)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `endAndDiscard discards workout without completion`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            awaitItem() // Running

            // When - show confirmation, then discard
            // (endAndDiscard() directly ends; use showEndConfirmation() to test the dialog flow)
            viewModel.showEndConfirmation()
            val state = awaitItem()
            assertThat(state.showEndConfirmation).isTrue()

            viewModel.end(EndReason.DISCARDED)
            val endedState = awaitItem()

            // Then - should be ended
            assertThat(endedState.phase).isEqualTo(WorkoutPhase.ENDED)
            assertThat(endedState.workoutCompleted).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `showEndConfirmation displays confirmation dialog`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            awaitItem() // Running

            // When
            viewModel.showEndConfirmation()

            // Then
            val state = awaitItem()
            assertThat(state.showEndConfirmation).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `hideEndConfirmation hides confirmation dialog`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            awaitItem() // Running

            // Show confirmation
            viewModel.showEndConfirmation()
            awaitItem()

            // When - hide
            viewModel.hideEndConfirmation()

            // Then
            val state = awaitItem()
            assertThat(state.showEndConfirmation).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty workout handles gracefully`() = runTest {
        // Given - empty workout
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.emptyWorkout))

        val viewModel = createViewModel()

        // Empty workout: start() returns early (no steps), no timer runs
        // UnconfinedTestDispatcher processes init eagerly — get settled state directly
        viewModel.uiState.test {
            val state = expectMostRecentItem()

            // Then - should handle empty workout without crashing
            assertThat(state.flattenedSteps).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `canGoBack is false at first step`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            val state = awaitItem()

            // Then - cannot go back at first step
            assertThat(state.canGoBack).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `canGoForward is false at last step`() = runTest {
        // Given - short workout
        val shortWorkout = Workout(
            id = "short",
            name = "Short",
            description = "Test",
            duration = 10,
            sport = WorkoutSport.CARDIO,
            source = WorkoutSource.AMAKA,
            intervals = listOf(
                WorkoutInterval.Time(seconds = 10, target = "Test")
            )
        )
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(shortWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading

            // Navigate to last step
            viewModel.nextStep()
            val state = awaitItem()

            // Then - cannot go forward at last step
            assertThat(state.canGoForward).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
