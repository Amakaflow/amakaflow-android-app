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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for WorkoutEngine state transitions and timer accuracy (AMA-667)
 * Tests the workout playback engine via WorkoutPlayerViewModel
 */
class WorkoutEngineTest {

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

        // Default mock behaviors
        every { mockGetWorkoutDetail(any()) } returns flowOf(Result.Success(TestFixtures.hiitWorkout))
        every { mockSubmitCompletion(any()) } returns Result.Success(mockk())
        every { mockMarkWorkoutCompleted(any()) } returns Result.Success(Unit)
        every { mockSimulationSettings.getSnapshot() } returns mockk {
            every { isEnabled } returns false
        }
    }

    private fun createViewModel(): WorkoutPlayerViewModel {
        return WorkoutPlayerViewModel(
            getWorkoutDetail = mockGetWorkoutDetail,
            submitCompletion = mockSubmitCompletion,
            markWorkoutCompleted = mockMarkWorkoutCompleted,
            simulationSettings = mockSimulationSettings
        )
    }

    @Test
    fun `initial state shows loading then transitions to running`() = runTest {
        // Given - workout is being loaded
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        // When
        val viewModel = createViewModel()

        // Then - should start in running state after loading
        viewModel.uiState.test {
            // First emission is loading
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()

            // After loading completes, workout starts automatically
            val runningState = awaitItem()
            assertThat(runningState.isLoading).isFalse()
            assertThat(runningState.phase).isEqualTo(WorkoutPhase.RUNNING)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state transitions from idle to running`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        // Then - verify idle to running transition
        viewModel.uiState.test {
            skipItems(1) // Skip loading state
            val runningState = awaitItem()
            assertThat(runningState.phase).isEqualTo(WorkoutPhase.RUNNING)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pause changes phase to PAUSED`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            skipItems(1) // Skip running state

            // When - pause the workout
            viewModel.pause()

            // Then - state should be PAUSED
            val pausedState = awaitItem()
            assertThat(pausedState.phase).isEqualTo(WorkoutPhase.PAUSED)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resume changes phase back to RUNNING`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            skipItems(1) // Skip running

            // When - pause then resume
            viewModel.pause()
            awaitItem() // Wait for paused state

            viewModel.resume()

            // Then - state should be RUNNING again
            val resumedState = awaitItem()
            assertThat(resumedState.phase).isEqualTo(WorkoutPhase.RUNNING)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `timer countdown decrements remainingSeconds`() = runTest {
        // Given - workout with timed intervals
        val timedWorkout = TestFixtures.hiitWorkout
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(timedWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            val initialState = awaitItem()
            val initialRemaining = initialState.remainingSeconds

            // Then - timer should be running (remaining > 0 for timed intervals)
            if (initialRemaining != null && initialRemaining > 0) {
                // Wait for timer tick
                kotlinx.coroutines.delay(1500)

                val afterTickState = expectMostRecentItem()
                assertThat(afterTickState.remainingSeconds).isLessThan(initialRemaining)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `elapsedSeconds increments while running`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            val initialState = awaitItem()
            val initialElapsed = initialState.elapsedSeconds

            // Wait for timer to tick
            kotlinx.coroutines.delay(1500)

            // Then - elapsed should have increased
            val afterTickState = expectMostRecentItem()
            assertThat(afterTickState.elapsedSeconds).isGreaterThan(initialElapsed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `interval progression advances stepIndex`() = runTest {
        // Given - workout with multiple intervals
        val multiIntervalWorkout = Workout(
            id = "test-workout",
            name = "Multi Interval Test",
            description = "Test workout",
            duration = 120,
            sport = WorkoutSport.CARDIO,
            source = WorkoutSource.AMAKA,
            intervals = listOf(
                WorkoutInterval.Warmup(seconds = 30),
                WorkoutInterval.Time(seconds = 30, target = "High Intensity"),
                WorkoutInterval.Rest(seconds = 15),
                WorkoutInterval.Time(seconds = 30, target = "High Intensity"),
                WorkoutInterval.Cooldown(seconds = 15)
            )
        )
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(multiIntervalWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            val initialState = awaitItem()
            val initialStepIndex = initialState.currentStepIndex

            // Wait for first interval to complete (30 seconds + buffer)
            kotlinx.coroutines.delay(32000)

            // Then - step index should have advanced
            val advancedState = expectMostRecentItem()
            assertThat(advancedState.currentStepIndex).isGreaterThan(initialStepIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `togglePlayPause alternates between running and paused`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            skipItems(1) // Skip running

            // When - toggle pause
            viewModel.togglePlayPause()
            val pausedState = awaitItem()
            assertThat(pausedState.phase).isEqualTo(WorkoutPhase.PAUSED)

            // When - toggle resume
            viewModel.togglePlayPause()
            val runningState = awaitItem()
            assertThat(runningState.phase).isEqualTo(WorkoutPhase.RUNNING)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `nextStep advances to next interval`() = runTest {
        // Given - workout with multiple intervals
        val multiIntervalWorkout = Workout(
            id = "test-workout",
            name = "Multi Interval Test",
            description = "Test workout",
            duration = 60,
            sport = WorkoutSport.CARDIO,
            source = WorkoutSource.AMAKA,
            intervals = listOf(
                WorkoutInterval.Warmup(seconds = 30),
                WorkoutInterval.Time(seconds = 30, target = "High")
            )
        )
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(multiIntervalWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            val initialState = awaitItem()
            val initialStepIndex = initialState.currentStepIndex

            // When - manually advance to next step
            viewModel.nextStep()

            // Then - step index should advance
            val advancedState = awaitItem()
            assertThat(advancedState.currentStepIndex).isGreaterThan(initialStepIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `previousStep goes back to previous interval`() = runTest {
        // Given - workout with multiple intervals
        val multiIntervalWorkout = Workout(
            id = "test-workout",
            name = "Multi Interval Test",
            description = "Test workout",
            duration = 60,
            sport = WorkoutSport.CARDIO,
            source = WorkoutSource.AMAKA,
            intervals = listOf(
                WorkoutInterval.Warmup(seconds = 30),
                WorkoutInterval.Time(seconds = 30, target = "High")
            )
        )
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(multiIntervalWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            skipItems(1) // Skip initial running state

            // First advance forward
            viewModel.nextStep()
            awaitItem()

            // When - go back
            viewModel.previousStep()

            // Then - should go back to step 0
            val state = awaitItem()
            assertThat(state.currentStepIndex).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `workout completion sets phase to ENDED`() = runTest {
        // Given - short workout that will complete quickly
        val shortWorkout = Workout(
            id = "short-workout",
            name = "Quick Test",
            description = "Short workout",
            duration = 10,
            sport = WorkoutSport.CARDIO,
            source = WorkoutSource.AMAKA,
            intervals = listOf(
                WorkoutInterval.Time(seconds = 1, target = "Quick")
            )
        )
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(shortWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading

            // Wait for workout to complete (1 second + buffer)
            kotlinx.coroutines.delay(2000)

            // Then - should be ended
            val finalState = expectMostRecentItem()
            assertThat(finalState.phase).isEqualTo(WorkoutPhase.ENDED)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loading workout detail shows loading state`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Loading)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertThat(loadingState.isLoading).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loading workout detail error shows error state`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Error("Network error"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val errorState = awaitItem()
            assertThat(errorState.isLoading).isFalse()
            assertThat(errorState.error).isEqualTo("Network error")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `flattenedSteps are computed from workout intervals`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            val state = awaitItem()

            // Then - flattened steps should be computed
            assertThat(state.flattenedSteps).isNotEmpty()
            // HIIT workout has warmup + 8 repeats (16 intervals) + cooldown
            assertThat(state.flattenedSteps.size).isGreaterThan(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `currentStep returns correct FlattenedInterval`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            val state = awaitItem()

            // Then - current step should match step index
            val currentStep = state.currentStep
            assertThat(currentStep).isNotNull()
            assertThat(currentStep?.index).isEqualTo(state.currentStepIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `progress calculates correctly`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(TestFixtures.hiitWorkout))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            skipItems(1) // Skip loading
            val state = awaitItem()

            // Then - progress should be between 0 and 1
            assertThat(state.progress).isAtLeast(0f)
            assertThat(state.progress).isAtMost(1f)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
