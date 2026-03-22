package com.amakaflow.companion.ui.screens.shoes

import app.cash.turbine.test
import com.amakaflow.companion.data.model.ShoeComparison
import com.amakaflow.companion.data.model.ShoeDetail
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

class ShoeComparisonViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockPlannerRepository: PlannerRepository

    @Before
    fun setup() {
        mockPlannerRepository = mockk(relaxed = true)
    }

    @Test
    fun `compare requires at least 2 shoes`() = runTest {
        val viewModel = ShoeComparisonViewModel(mockPlannerRepository)

        viewModel.updateShoeInput("Nike Pegasus")
        viewModel.compare()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.error).contains("at least 2 shoes")
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `compare sends correct shoe names`() = runTest {
        val comparison = ShoeComparison(
            shoes = listOf(
                ShoeDetail(
                    name = "Nike Pegasus 41",
                    brand = "Nike",
                    rating = 4.5,
                    pros = listOf("Comfortable", "Durable"),
                    cons = listOf("Heavy"),
                    bestFor = "Daily training"
                ),
                ShoeDetail(
                    name = "ASICS Gel-Nimbus 26",
                    brand = "ASICS",
                    rating = 4.3,
                    pros = listOf("Great cushioning"),
                    cons = listOf("Pricey"),
                    bestFor = "Long runs"
                )
            ),
            recommendation = "Nike Pegasus for daily training, ASICS for long runs."
        )

        every { mockPlannerRepository.compareShoes(any(), any()) } returns flowOf(
            Result.Success(comparison)
        )

        val viewModel = ShoeComparisonViewModel(mockPlannerRepository)
        viewModel.updateShoeInput("Nike Pegasus 41, ASICS Gel-Nimbus 26")
        viewModel.compare()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.comparison).isNotNull()
            assertThat(state.comparison!!.shoes).hasSize(2)
            assertThat(state.comparison!!.recommendation).contains("Nike Pegasus")
            assertThat(state.error).isNull()
        }

        verify {
            mockPlannerRepository.compareShoes(
                match { it.size == 2 && it.contains("Nike Pegasus 41") },
                isNull()
            )
        }
    }

    @Test
    fun `compare shows error on failure`() = runTest {
        every { mockPlannerRepository.compareShoes(any(), any()) } returns flowOf(
            Result.Error("API error")
        )

        val viewModel = ShoeComparisonViewModel(mockPlannerRepository)
        viewModel.updateShoeInput("Shoe A, Shoe B")
        viewModel.compare()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.error).isEqualTo("API error")
        }
    }
}
