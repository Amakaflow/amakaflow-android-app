package com.amakaflow.companion.ui.screens.prhistory

import app.cash.turbine.test
import com.amakaflow.companion.domain.usecase.pr.PRDetectionService
import com.amakaflow.companion.domain.usecase.pr.PRType
import com.amakaflow.companion.domain.usecase.pr.PersonalRecord
import com.amakaflow.companion.test.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PRHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockPRService: PRDetectionService

    @Before
    fun setup() {
        mockPRService = mockk(relaxed = true)
    }

    @Test
    fun `initial state loads PRs from service`() = runTest {
        val prData = listOf(
            "Bench Press" to listOf(
                PersonalRecord(
                    id = "1",
                    exerciseName = "Bench Press",
                    type = PRType.HEAVIEST_WEIGHT,
                    value = 80.0,
                    reps = 5,
                    weight = null,
                    dateIso = "2026-01-15T10:00:00Z",
                    workoutName = "Upper Body"
                )
            )
        )
        every { mockPRService.prsByExercise() } returns prData

        val viewModel = PRHistoryViewModel(mockPRService)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.groupedPRs).hasSize(1)
            assertThat(state.groupedPRs[0].first).isEqualTo("Bench Press")
            assertThat(state.groupedPRs[0].second).hasSize(1)
            assertThat(state.groupedPRs[0].second[0].value).isEqualTo(80.0)
        }
    }

    @Test
    fun `empty state when no PRs`() = runTest {
        every { mockPRService.prsByExercise() } returns emptyList()

        val viewModel = PRHistoryViewModel(mockPRService)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.groupedPRs).isEmpty()
        }
    }

    @Test
    fun `loads multiple exercise groups`() = runTest {
        val prData = listOf(
            "Bench Press" to listOf(
                PersonalRecord("1", "Bench Press", PRType.HEAVIEST_WEIGHT, 80.0, dateIso = "2026-01-15T10:00:00Z"),
                PersonalRecord("2", "Bench Press", PRType.MOST_VOLUME, 1200.0, dateIso = "2026-01-15T10:00:00Z")
            ),
            "Squat" to listOf(
                PersonalRecord("3", "Squat", PRType.HEAVIEST_WEIGHT, 120.0, dateIso = "2026-01-14T10:00:00Z")
            )
        )
        every { mockPRService.prsByExercise() } returns prData

        val viewModel = PRHistoryViewModel(mockPRService)

        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.groupedPRs).hasSize(2)
            assertThat(state.groupedPRs[0].second).hasSize(2)
            assertThat(state.groupedPRs[1].second).hasSize(1)
        }
    }
}
