package com.amakaflow.companion.ui.screens.rpefeedback

import app.cash.turbine.test
import com.amakaflow.companion.data.model.RPEFeedbackResponse
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.PlannerRepository
import com.amakaflow.companion.test.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RPEFeedbackViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockPlannerRepository: PlannerRepository

    @Before
    fun setup() {
        mockPlannerRepository = mockk(relaxed = true)
    }

    @Test
    fun `initial state is empty`() = runTest {
        val viewModel = RPEFeedbackViewModel(mockPlannerRepository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.selectedOption).isNull()
            assertThat(state.selectedMuscles).isEmpty()
            assertThat(state.isSubmitting).isFalse()
            assertThat(state.isSubmitted).isFalse()
            assertThat(state.deloadRecommended).isFalse()
            assertThat(state.error).isNull()
        }
    }

    @Test
    fun `selectOption updates state`() = runTest {
        val viewModel = RPEFeedbackViewModel(mockPlannerRepository)

        viewModel.selectOption(RPEOption.HARD)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.selectedOption).isEqualTo(RPEOption.HARD)
        }
    }

    @Test
    fun `selectOption replaces previous selection`() = runTest {
        val viewModel = RPEFeedbackViewModel(mockPlannerRepository)

        viewModel.selectOption(RPEOption.HARD)
        viewModel.selectOption(RPEOption.EASY)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.selectedOption).isEqualTo(RPEOption.EASY)
        }
    }

    @Test
    fun `toggleMuscle adds muscle`() = runTest {
        val viewModel = RPEFeedbackViewModel(mockPlannerRepository)

        viewModel.toggleMuscle(MuscleGroup.CHEST)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.selectedMuscles).contains(MuscleGroup.CHEST)
        }
    }

    @Test
    fun `toggleMuscle removes when already selected`() = runTest {
        val viewModel = RPEFeedbackViewModel(mockPlannerRepository)

        viewModel.toggleMuscle(MuscleGroup.CHEST)
        viewModel.toggleMuscle(MuscleGroup.CHEST)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.selectedMuscles).doesNotContain(MuscleGroup.CHEST)
        }
    }

    @Test
    fun `multiple muscles can be selected`() = runTest {
        val viewModel = RPEFeedbackViewModel(mockPlannerRepository)

        viewModel.toggleMuscle(MuscleGroup.CHEST)
        viewModel.toggleMuscle(MuscleGroup.LEGS)
        viewModel.toggleMuscle(MuscleGroup.CORE)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.selectedMuscles).hasSize(3)
            assertThat(state.selectedMuscles).containsExactly(
                MuscleGroup.CHEST, MuscleGroup.LEGS, MuscleGroup.CORE
            )
        }
    }

    @Test
    fun `submit sends correct RPE value`() = runTest {
        coEvery {
            mockPlannerRepository.submitRPEFeedback(any(), any(), any(), any())
        } returns Result.Success(
            RPEFeedbackResponse(success = true, message = "OK")
        )

        val viewModel = RPEFeedbackViewModel(mockPlannerRepository)
        viewModel.selectOption(RPEOption.HARD)

        var completed = false
        viewModel.submit(workoutId = "workout-123") { completed = true }

        // Wait for coroutine
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            mockPlannerRepository.submitRPEFeedback(
                workoutId = "workout-123",
                rpe = 8,  // HARD = 8
                muscleSoreness = null,
                notes = null
            )
        }
    }

    @Test
    fun `submit includes muscle soreness when selected`() = runTest {
        coEvery {
            mockPlannerRepository.submitRPEFeedback(any(), any(), any(), any())
        } returns Result.Success(
            RPEFeedbackResponse(success = true, message = "OK")
        )

        val viewModel = RPEFeedbackViewModel(mockPlannerRepository)
        viewModel.selectOption(RPEOption.MODERATE)
        viewModel.toggleMuscle(MuscleGroup.CHEST)
        viewModel.toggleMuscle(MuscleGroup.LEGS)

        viewModel.submit(workoutId = "workout-456") {}

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            mockPlannerRepository.submitRPEFeedback(
                workoutId = "workout-456",
                rpe = 6,  // MODERATE = 6
                muscleSoreness = match { it != null && it.containsAll(listOf("chest", "legs")) },
                notes = null
            )
        }
    }

    @Test
    fun `submit sets isSubmitted on success`() = runTest {
        coEvery {
            mockPlannerRepository.submitRPEFeedback(any(), any(), any(), any())
        } returns Result.Success(
            RPEFeedbackResponse(success = true, message = "OK")
        )

        val viewModel = RPEFeedbackViewModel(mockPlannerRepository)
        viewModel.selectOption(RPEOption.EASY)

        viewModel.submit(workoutId = "w-1") {}

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isSubmitted).isTrue()
            assertThat(state.isSubmitting).isFalse()
        }
    }

    @Test
    fun `submit sets deloadRecommended when API returns true`() = runTest {
        coEvery {
            mockPlannerRepository.submitRPEFeedback(any(), any(), any(), any())
        } returns Result.Success(
            RPEFeedbackResponse(success = true, message = "OK", deloadRecommended = true)
        )

        val viewModel = RPEFeedbackViewModel(mockPlannerRepository)
        viewModel.selectOption(RPEOption.CRUSHED)

        viewModel.submit(workoutId = "w-1") {}

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.deloadRecommended).isTrue()
        }
    }

    @Test
    fun `submit handles error gracefully`() = runTest {
        coEvery {
            mockPlannerRepository.submitRPEFeedback(any(), any(), any(), any())
        } returns Result.Error("Connection failed")

        val viewModel = RPEFeedbackViewModel(mockPlannerRepository)
        viewModel.selectOption(RPEOption.HARD)

        viewModel.submit(workoutId = "w-1") {}

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.error).isEqualTo("Could not save feedback")
            assertThat(state.isSubmitting).isFalse()
        }
    }

    @Test
    fun `submit without selection does nothing`() = runTest {
        val viewModel = RPEFeedbackViewModel(mockPlannerRepository)

        var completed = false
        viewModel.submit(workoutId = "w-1") { completed = true }

        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        // Should not call API
        coVerify(exactly = 0) {
            mockPlannerRepository.submitRPEFeedback(any(), any(), any(), any())
        }
        assertThat(completed).isFalse()
    }

    // MARK: - RPEOption tests

    @Test
    fun `RPEOption has correct values`() {
        assertThat(RPEOption.EASY.rpeValue).isEqualTo(4)
        assertThat(RPEOption.MODERATE.rpeValue).isEqualTo(6)
        assertThat(RPEOption.HARD.rpeValue).isEqualTo(8)
        assertThat(RPEOption.CRUSHED.rpeValue).isEqualTo(10)
    }

    @Test
    fun `RPEOption values are ordered by difficulty`() {
        val values = RPEOption.entries.map { it.rpeValue }
        for (i in 1 until values.size) {
            assertThat(values[i]).isGreaterThan(values[i - 1])
        }
    }

    @Test
    fun `all 4 RPEOptions exist`() {
        assertThat(RPEOption.entries).hasSize(4)
    }

    // MARK: - MuscleGroup tests

    @Test
    fun `all 6 MuscleGroups exist`() {
        assertThat(MuscleGroup.entries).hasSize(6)
    }

    @Test
    fun `MuscleGroup apiValues are lowercase`() {
        for (muscle in MuscleGroup.entries) {
            assertThat(muscle.apiValue).isEqualTo(muscle.apiValue.lowercase())
        }
    }
}
