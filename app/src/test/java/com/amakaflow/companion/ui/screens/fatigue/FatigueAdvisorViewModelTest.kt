package com.amakaflow.companion.ui.screens.fatigue

import app.cash.turbine.test
import com.amakaflow.companion.data.model.FatigueAdvisorResponse
import com.amakaflow.companion.data.model.FatigueFactor
import com.amakaflow.companion.data.model.FatigueLevel
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.PlannerRepository
import com.amakaflow.companion.test.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FatigueAdvisorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockPlannerRepository: PlannerRepository

    @Before
    fun setup() {
        mockPlannerRepository = mockk(relaxed = true)
    }

    @Test
    fun `loads fatigue advice on init`() = runTest {
        val response = FatigueAdvisorResponse(
            success = true,
            fatigueScore = 65.0,
            level = FatigueLevel.MODERATE,
            advice = "Consider a lighter session today.",
            recommendations = listOf("Take a rest day", "Do mobility work"),
            contributingFactors = listOf(
                FatigueFactor(factor = "Training load", impact = 70.0, description = "High recent volume")
            )
        )

        every { mockPlannerRepository.getFatigueAdvice() } returns flowOf(Result.Success(response))

        val viewModel = FatigueAdvisorViewModel(mockPlannerRepository)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.fatigueScore).isEqualTo(65.0)
            assertThat(state.level).isEqualTo(FatigueLevel.MODERATE)
            assertThat(state.advice).contains("lighter session")
            assertThat(state.recommendations).hasSize(2)
            assertThat(state.contributingFactors).hasSize(1)
            assertThat(state.error).isNull()
        }
    }

    @Test
    fun `shows error when loading fails`() = runTest {
        every { mockPlannerRepository.getFatigueAdvice() } returns flowOf(
            Result.Error("Connection failed")
        )

        val viewModel = FatigueAdvisorViewModel(mockPlannerRepository)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.error).isEqualTo("Connection failed")
        }
    }

    @Test
    fun `refresh reloads fatigue data`() = runTest {
        every { mockPlannerRepository.getFatigueAdvice() } returns flowOf(
            Result.Success(
                FatigueAdvisorResponse(
                    success = true,
                    fatigueScore = 30.0,
                    level = FatigueLevel.LOW,
                    advice = "You're well recovered!"
                )
            )
        )

        val viewModel = FatigueAdvisorViewModel(mockPlannerRepository)
        viewModel.refresh()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.fatigueScore).isEqualTo(30.0)
            assertThat(state.level).isEqualTo(FatigueLevel.LOW)
        }
    }
}
