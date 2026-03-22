package com.amakaflow.companion.ui.screens.preferences

import app.cash.turbine.test
import com.amakaflow.companion.data.model.TrainingPreferences
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.PlannerRepository
import com.amakaflow.companion.test.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TrainingPreferencesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockPlannerRepository: PlannerRepository

    @Before
    fun setup() {
        mockPlannerRepository = mockk(relaxed = true)
    }

    @Test
    fun `loads preferences on init`() = runTest {
        val prefs = TrainingPreferences(
            daysPerWeek = 5,
            preferredSports = listOf("running", "swimming"),
            maxSessionMinutes = 45,
            fitnessLevel = "advanced",
            goal = "Marathon prep"
        )

        every { mockPlannerRepository.getTrainingPreferences() } returns flowOf(
            Result.Success(prefs)
        )

        val viewModel = TrainingPreferencesViewModel(mockPlannerRepository)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.preferences.daysPerWeek).isEqualTo(5)
            assertThat(state.preferences.preferredSports).containsExactly("running", "swimming")
            assertThat(state.preferences.fitnessLevel).isEqualTo("advanced")
        }
    }

    @Test
    fun `updateDaysPerWeek updates local state`() = runTest {
        every { mockPlannerRepository.getTrainingPreferences() } returns flowOf(
            Result.Success(TrainingPreferences())
        )

        val viewModel = TrainingPreferencesViewModel(mockPlannerRepository)
        viewModel.updateDaysPerWeek(6)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.preferences.daysPerWeek).isEqualTo(6)
        }
    }

    @Test
    fun `toggleSport adds and removes sports`() = runTest {
        every { mockPlannerRepository.getTrainingPreferences() } returns flowOf(
            Result.Success(TrainingPreferences(preferredSports = listOf("running")))
        )

        val viewModel = TrainingPreferencesViewModel(mockPlannerRepository)

        // Add swimming
        viewModel.toggleSport("swimming")
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.preferences.preferredSports).containsExactly("running", "swimming")
        }

        // Remove running
        viewModel.toggleSport("running")
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.preferences.preferredSports).containsExactly("swimming")
        }
    }

    @Test
    fun `savePreferences calls repository and updates state`() = runTest {
        val initialPrefs = TrainingPreferences(daysPerWeek = 4)
        val savedPrefs = TrainingPreferences(daysPerWeek = 5)

        every { mockPlannerRepository.getTrainingPreferences() } returns flowOf(
            Result.Success(initialPrefs)
        )
        coEvery { mockPlannerRepository.updateTrainingPreferences(any()) } returns Result.Success(savedPrefs)

        val viewModel = TrainingPreferencesViewModel(mockPlannerRepository)
        viewModel.updateDaysPerWeek(5)
        viewModel.savePreferences()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isSaving).isFalse()
            assertThat(state.saveSuccess).isTrue()
            assertThat(state.preferences.daysPerWeek).isEqualTo(5)
        }
    }

    @Test
    fun `savePreferences shows error on failure`() = runTest {
        every { mockPlannerRepository.getTrainingPreferences() } returns flowOf(
            Result.Success(TrainingPreferences())
        )
        coEvery { mockPlannerRepository.updateTrainingPreferences(any()) } returns Result.Error("Save failed")

        val viewModel = TrainingPreferencesViewModel(mockPlannerRepository)
        viewModel.savePreferences()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isSaving).isFalse()
            assertThat(state.error).isEqualTo("Save failed")
        }
    }
}
