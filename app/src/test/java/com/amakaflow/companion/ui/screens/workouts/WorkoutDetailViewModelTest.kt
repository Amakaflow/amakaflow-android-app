package com.amakaflow.companion.ui.screens.workouts

import app.cash.turbine.test
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.usecase.workout.GetWorkoutDetailUseCase
import com.amakaflow.companion.test.MainDispatcherRule
import com.amakaflow.companion.test.TestFixtures
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WorkoutDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockGetWorkoutDetail: GetWorkoutDetailUseCase

    @Before
    fun setup() {
        mockGetWorkoutDetail = mockk()
    }

    private fun createViewModel(): WorkoutDetailViewModel {
        return WorkoutDetailViewModel(mockGetWorkoutDetail)
    }

    @Test
    fun `initial state shows loading`() = runTest {
        // Given
        every { mockGetWorkoutDetail(any()) } returns flow { /* never emits */ }

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isLoading).isTrue()
            assertThat(initialState.workout).isNull()
            assertThat(initialState.error).isNull()
        }
    }

    @Test
    fun `loads workout successfully`() = runTest {
        // Given
        val workout = TestFixtures.hiitWorkout
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(workout))

        // When
        val viewModel = createViewModel()
        viewModel.loadWorkout("workout-001")

        // Then
        viewModel.uiState.test {
            val finalState = expectMostRecentItem()
            assertThat(finalState.isLoading).isFalse()
            assertThat(finalState.workout).isNotNull()
            assertThat(finalState.workout?.id).isEqualTo("workout-001")
            assertThat(finalState.workout?.name).isEqualTo("HIIT Blast")
            assertThat(finalState.error).isNull()
        }
    }

    @Test
    fun `shows error on failure`() = runTest {
        // Given
        every { mockGetWorkoutDetail("invalid-id") } returns flowOf(Result.Error("Workout not found"))

        // When
        val viewModel = createViewModel()
        viewModel.loadWorkout("invalid-id")

        // Then
        viewModel.uiState.test {
            val finalState = expectMostRecentItem()
            assertThat(finalState.isLoading).isFalse()
            assertThat(finalState.workout).isNull()
            assertThat(finalState.error).isEqualTo("Workout not found")
        }
    }

    @Test
    fun `refresh reloads workout details`() = runTest {
        // Given
        val workout = TestFixtures.hiitWorkout
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Success(workout))

        val viewModel = createViewModel()
        viewModel.loadWorkout("workout-001")

        // Wait for initial load
        viewModel.uiState.test {
            expectMostRecentItem()
            cancelAndIgnoreRemainingEvents()
        }

        // When - refresh
        viewModel.refresh()

        // Then - should reload
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.workout).isNotNull()
            assertThat(state.error).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh shows error on network failure during refresh`() = runTest {
        // Given
        every { mockGetWorkoutDetail("workout-001") } returns flowOf(Result.Error("Network error"))

        val viewModel = createViewModel()
        viewModel.loadWorkout("workout-001")

        // Wait for initial load
        viewModel.uiState.test {
            expectMostRecentItem()
            cancelAndIgnoreRemainingEvents()
        }

        // When - refresh fails
        viewModel.refresh()

        // Then - should show error
        viewModel.uiState.test {
            val finalState = expectMostRecentItem()
            assertThat(finalState.error).isEqualTo("Network error")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `not found error shows specific message`() = runTest {
        // Given
        every { mockGetWorkoutDetail("missing-id") } returns flowOf(Result.Error("Workout not found"))

        // When
        val viewModel = createViewModel()
        viewModel.loadWorkout("missing-id")

        // Then
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.error).contains("not found")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
