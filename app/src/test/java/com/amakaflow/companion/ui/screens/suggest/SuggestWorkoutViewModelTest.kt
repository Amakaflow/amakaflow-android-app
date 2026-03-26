package com.amakaflow.companion.ui.screens.suggest

import android.content.Context
import android.content.SharedPreferences
import app.cash.turbine.test
import com.amakaflow.companion.data.model.SuggestWorkoutResponse
import com.amakaflow.companion.data.model.WarmUpCooldown
import com.amakaflow.companion.data.model.WorkoutInterval
import com.amakaflow.companion.data.model.WorkoutSport
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.PlannerRepository
import com.amakaflow.companion.test.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SuggestWorkoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockPlannerRepository: PlannerRepository
    private lateinit var mockContext: Context
    private lateinit var mockSharedPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        mockPlannerRepository = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)
        mockSharedPrefs = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPrefs
        every { mockSharedPrefs.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
    }

    private fun createViewModel(hasProfile: Boolean = false): SuggestWorkoutViewModel {
        every { mockSharedPrefs.getString("coaching_profile", null) } returns if (hasProfile) {
            """{"experience":"INTERMEDIATE","goal":"GENERAL_FITNESS","daysPerWeek":3}"""
        } else {
            null
        }
        return SuggestWorkoutViewModel(mockPlannerRepository, mockContext)
    }

    @Test
    fun `initial state is IDLE`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.state).isEqualTo(SuggestWorkoutState.IDLE)
            assertThat(state.suggestedWorkout).isNull()
        }
    }

    @Test
    fun `requestSuggestion shows onboarding when no profile`() = runTest {
        val viewModel = createViewModel(hasProfile = false)
        viewModel.requestSuggestion()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.state).isEqualTo(SuggestWorkoutState.NEEDS_ONBOARDING)
        }
    }

    @Test
    fun `requestSuggestion starts loading when profile exists`() = runTest {
        every { mockPlannerRepository.suggestWorkout(any(), any(), any()) } returns flowOf(
            Result.Loading
        )

        val viewModel = createViewModel(hasProfile = true)
        viewModel.requestSuggestion()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.state).isEqualTo(SuggestWorkoutState.LOADING)
        }
    }

    @Test
    fun `successful suggestion updates state with workout`() = runTest {
        val response = SuggestWorkoutResponse(
            blocks = listOf(
                WorkoutInterval.Reps(sets = 3, reps = 10, name = "Squat", load = null, restSec = 60, followAlongUrl = null)
            ),
            warmUp = WarmUpCooldown(seconds = 300, target = "Light jog"),
            cooldown = WarmUpCooldown(seconds = 300, target = "Stretching"),
            name = "Full Body Strength",
            sport = WorkoutSport.STRENGTH,
            durationSeconds = 1800,
            description = "A balanced full body workout"
        )

        every { mockPlannerRepository.suggestWorkout(any(), any(), any()) } returns flowOf(
            Result.Success(response)
        )

        val viewModel = createViewModel(hasProfile = true)
        viewModel.suggestWorkout()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.state).isEqualTo(SuggestWorkoutState.SUCCESS)
            assertThat(state.suggestedWorkout).isNotNull()
            assertThat(state.suggestedWorkout!!.name).isEqualTo("Full Body Strength")
            assertThat(state.suggestedWorkout!!.sport).isEqualTo(WorkoutSport.STRENGTH)
            assertThat(state.suggestedWorkout!!.duration).isEqualTo(1800)
            // Warm-up + 1 block + cooldown = 3 intervals
            assertThat(state.suggestedWorkout!!.intervals).hasSize(3)
        }
    }

    @Test
    fun `error state shows error message`() = runTest {
        every { mockPlannerRepository.suggestWorkout(any(), any(), any()) } returns flowOf(
            Result.Error("Network error")
        )

        val viewModel = createViewModel(hasProfile = true)
        viewModel.suggestWorkout()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.state).isEqualTo(SuggestWorkoutState.ERROR)
            assertThat(state.errorMessage).isEqualTo("Network error")
        }
    }

    @Test
    fun `completeOnboarding saves profile and starts suggestion`() = runTest {
        every { mockPlannerRepository.suggestWorkout(any(), any(), any()) } returns flowOf(
            Result.Loading
        )

        val viewModel = createViewModel(hasProfile = false)
        viewModel.completeOnboarding(
            ExperienceLevel.INTERMEDIATE,
            TrainingGoal.BUILD_MUSCLE,
            4
        )

        verify { mockEditor.putString("coaching_profile", any()) }
        verify { mockEditor.apply() }

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.hasProfile).isTrue()
            // Should be loading since suggestWorkout was called
            assertThat(state.state).isEqualTo(SuggestWorkoutState.LOADING)
        }
    }

    @Test
    fun `reset clears state`() = runTest {
        val viewModel = createViewModel(hasProfile = true)
        viewModel.reset()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.state).isEqualTo(SuggestWorkoutState.IDLE)
            assertThat(state.suggestedWorkout).isNull()
            assertThat(state.errorMessage).isNull()
        }
    }

    @Test
    fun `experience level display names are correct`() {
        assertThat(ExperienceLevel.BEGINNER.displayName).isEqualTo("Beginner")
        assertThat(ExperienceLevel.INTERMEDIATE.displayName).isEqualTo("Intermediate")
        assertThat(ExperienceLevel.ADVANCED.displayName).isEqualTo("Advanced")
    }

    @Test
    fun `training goal display names are correct`() {
        assertThat(TrainingGoal.LOSE_WEIGHT.displayName).isEqualTo("Lose Weight")
        assertThat(TrainingGoal.BUILD_MUSCLE.displayName).isEqualTo("Build Muscle")
        assertThat(TrainingGoal.IMPROVE_ENDURANCE.displayName).isEqualTo("Improve Endurance")
        assertThat(TrainingGoal.GENERAL_FITNESS.displayName).isEqualTo("General Fitness")
        assertThat(TrainingGoal.ATHLETIC.displayName).isEqualTo("Athletic Performance")
    }
}
