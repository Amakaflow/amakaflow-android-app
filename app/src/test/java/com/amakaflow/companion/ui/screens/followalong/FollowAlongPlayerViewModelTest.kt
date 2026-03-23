package com.amakaflow.companion.ui.screens.followalong

import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import com.amakaflow.companion.data.model.Workout
import com.amakaflow.companion.data.model.WorkoutInterval
import com.amakaflow.companion.data.model.WorkoutSource
import com.amakaflow.companion.data.model.WorkoutSport
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.usecase.workout.GetWorkoutDetailUseCase
import com.amakaflow.companion.test.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FollowAlongPlayerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockGetWorkoutDetail: GetWorkoutDetailUseCase

    private val testWorkout = Workout(
        id = "test-follow-along",
        name = "HIIT Follow-Along",
        sport = WorkoutSport.STRENGTH,
        duration = 600,
        intervals = listOf(
            WorkoutInterval.Warmup(seconds = 60, target = "Easy"),
            WorkoutInterval.Reps(
                reps = 10,
                name = "Push-ups",
                restSec = 15,
                followAlongUrl = "https://example.com/video.mp4"
            ),
            WorkoutInterval.Cooldown(seconds = 60, target = null)
        ),
        source = WorkoutSource.COACH
    )

    private val emptyWorkout = Workout(
        id = "empty",
        name = "Empty",
        sport = WorkoutSport.OTHER,
        duration = 0,
        intervals = emptyList(),
        source = WorkoutSource.COACH
    )

    @Before
    fun setup() {
        mockGetWorkoutDetail = mockk()
        every { mockGetWorkoutDetail(any()) } returns flowOf(Result.Success(testWorkout))
    }

    private fun createViewModel(workout: Workout = testWorkout): FollowAlongPlayerViewModel {
        every { mockGetWorkoutDetail(any()) } returns flowOf(Result.Success(workout))
        return FollowAlongPlayerViewModel(
            getWorkoutDetail = mockGetWorkoutDetail,
            savedStateHandle = SavedStateHandle(mapOf("workoutId" to workout.id))
        )
    }

    // MARK: - Loading

    @Test
    fun `loads workout and extracts steps`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.phase).isEqualTo(FollowAlongPhase.READY)
            // warmup + reps + rest(15s) + cooldown = 4 steps
            assertThat(state.steps).hasSize(4)
            assertThat(state.currentStepIndex).isEqualTo(0)
            assertThat(state.elapsedSeconds).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads workout with repeat expands rounds`() = runTest {
        val workout = testWorkout.copy(
            intervals = listOf(
                WorkoutInterval.Repeat(
                    reps = 2,
                    intervals = listOf(
                        WorkoutInterval.Reps(reps = 5, name = "Squats")
                    )
                )
            )
        )
        val vm = createViewModel(workout)

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.steps).hasSize(2)
            assertThat(state.steps[0].name).contains("Round 1")
            assertThat(state.steps[1].name).contains("Round 2")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty workout ends with error`() = runTest {
        val vm = createViewModel(emptyWorkout)

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.phase).isEqualTo(FollowAlongPhase.ENDED)
            assertThat(state.errorMessage).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `api error sets error message`() = runTest {
        every { mockGetWorkoutDetail(any()) } returns flowOf(Result.Error("Network error"))
        val vm = FollowAlongPlayerViewModel(
            getWorkoutDetail = mockGetWorkoutDetail,
            savedStateHandle = SavedStateHandle(mapOf("workoutId" to "test"))
        )

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.phase).isEqualTo(FollowAlongPhase.ENDED)
            assertThat(state.errorMessage).isEqualTo("Network error")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // MARK: - Playback Controls

    @Test
    fun `play from ready`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            expectMostRecentItem() // READY state
            vm.play()
            val state = expectMostRecentItem()
            assertThat(state.phase).isEqualTo(FollowAlongPhase.PLAYING)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pause from playing`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            expectMostRecentItem() // READY
            vm.play()
            expectMostRecentItem() // PLAYING
            vm.pause()
            val state = expectMostRecentItem()
            assertThat(state.phase).isEqualTo(FollowAlongPhase.PAUSED)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggle play pause cycles states`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            expectMostRecentItem() // READY
            vm.togglePlayPause()
            assertThat(expectMostRecentItem().phase).isEqualTo(FollowAlongPhase.PLAYING)
            vm.togglePlayPause()
            assertThat(expectMostRecentItem().phase).isEqualTo(FollowAlongPhase.PAUSED)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `play does nothing when ended`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            expectMostRecentItem()
            vm.endWorkout()
            assertThat(expectMostRecentItem().phase).isEqualTo(FollowAlongPhase.ENDED)
            vm.play()
            // Phase should still be ENDED - no new emission
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // MARK: - Step Navigation

    @Test
    fun `skip to next step`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            val initialState = expectMostRecentItem()
            assertThat(initialState.currentStepIndex).isEqualTo(0)
            vm.skipToNextStep()
            val state = expectMostRecentItem()
            assertThat(state.currentStepIndex).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `skip to previous step`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            expectMostRecentItem()
            vm.skipToNextStep()
            expectMostRecentItem()
            vm.skipToPreviousStep()
            val state = expectMostRecentItem()
            assertThat(state.currentStepIndex).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `skip to previous does nothing at start`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            val initialState = expectMostRecentItem()
            assertThat(initialState.currentStepIndex).isEqualTo(0)
            vm.skipToPreviousStep()
            expectNoEvents() // No state change
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `skip to next ends workout on last step`() = runTest {
        val workout = testWorkout.copy(
            intervals = listOf(WorkoutInterval.Warmup(seconds = 30, target = null))
        )
        val vm = createViewModel(workout)

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.steps).hasSize(1)
            vm.skipToNextStep()
            val endedState = expectMostRecentItem()
            assertThat(endedState.phase).isEqualTo(FollowAlongPhase.ENDED)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `skip to specific step`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            expectMostRecentItem()
            vm.skipToStep(2)
            val state = expectMostRecentItem()
            assertThat(state.currentStepIndex).isEqualTo(2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `skip to invalid step does nothing`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            val initialState = expectMostRecentItem()
            assertThat(initialState.currentStepIndex).isEqualTo(0)
            vm.skipToStep(999)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // MARK: - Current Step / Progress

    @Test
    fun `current step returns correct step`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.currentStep).isNotNull()
            assertThat(state.currentStep!!.name).isEqualTo("Easy")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `progress calculation`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            val initialState = expectMostRecentItem()
            assertThat(initialState.progress).isWithin(0.01f).of(0f)
            vm.skipToNextStep()
            val state = expectMostRecentItem()
            assertThat(state.progress).isWithin(0.01f).of(1f / state.steps.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // MARK: - Step Properties

    @Test
    fun `timed step has remaining seconds`() = runTest {
        val workout = testWorkout.copy(
            intervals = listOf(WorkoutInterval.Time(seconds = 120, target = "Plank"))
        )
        val vm = createViewModel(workout)

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.stepRemainingSeconds).isEqualTo(120)
            assertThat(state.currentStep!!.isTimeBased).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reps step has no remaining seconds`() = runTest {
        val workout = testWorkout.copy(
            intervals = listOf(WorkoutInterval.Reps(reps = 10, name = "Squats"))
        )
        val vm = createViewModel(workout)

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.stepRemainingSeconds).isEqualTo(0)
            assertThat(state.currentStep!!.isTimeBased).isFalse()
            assertThat(state.currentStep!!.reps).isEqualTo(10)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // MARK: - End Workout

    @Test
    fun `end workout sets phase to ended`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            expectMostRecentItem()
            vm.play()
            expectMostRecentItem()
            vm.endWorkout()
            val state = expectMostRecentItem()
            assertThat(state.phase).isEqualTo(FollowAlongPhase.ENDED)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // MARK: - UI State Computed Properties

    @Test
    fun `formatted elapsed shows correct format`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.formattedElapsed).isEqualTo("0:00")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isLastStep returns true on single step workout`() = runTest {
        val workout = testWorkout.copy(
            intervals = listOf(WorkoutInterval.Warmup(seconds = 30, target = null))
        )
        val vm = createViewModel(workout)

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLastStep).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `canGoBack is false at start`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.canGoBack).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `canGoForward is true when not at last step`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.canGoForward).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `video url extracted from follow along url`() = runTest {
        val vm = createViewModel()

        vm.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.videoUrl).isNotNull()
            assertThat(state.videoUrl).isEqualTo("https://example.com/video.mp4")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
