package com.amakaflow.companion.ui.screens.nutrition

import app.cash.turbine.test
import com.amakaflow.companion.data.nutrition.FuelingLevel
import com.amakaflow.companion.data.nutrition.FuelingRepository
import com.amakaflow.companion.data.nutrition.FuelingStatusResponse
import com.amakaflow.companion.data.nutrition.ProteinNudgeService
import com.amakaflow.companion.test.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FuelingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockFuelingRepository: FuelingRepository
    private lateinit var mockProteinNudgeService: ProteinNudgeService

    @Before
    fun setup() {
        mockFuelingRepository = mockk(relaxed = true)
        mockProteinNudgeService = mockk(relaxed = true)
    }

    private fun createViewModel(): FuelingViewModel {
        return FuelingViewModel(mockFuelingRepository, mockProteinNudgeService)
    }

    @Test
    fun `initial state is loading then resolves with fueling status`() = runTest {
        val status = FuelingStatusResponse(
            status = FuelingLevel.GREEN,
            proteinPct = 0.75,
            caloriesPct = 0.80,
            hydrationPct = 0.60,
            message = "Well fueled for training"
        )
        coEvery { mockFuelingRepository.getFuelingStatus() } returns status

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.fuelingStatus).isNotNull()
            assertThat(state.fuelingStatus!!.status).isEqualTo(FuelingLevel.GREEN)
            assertThat(state.fuelingStatus!!.proteinPct).isEqualTo(0.75)
            assertThat(state.error).isNull()
        }
    }

    @Test
    fun `sets error when API returns null`() = runTest {
        coEvery { mockFuelingRepository.getFuelingStatus() } returns null

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.fuelingStatus).isNull()
            assertThat(state.error).isEqualTo("Unable to load fueling status")
        }
    }

    @Test
    fun `sets error when API throws exception`() = runTest {
        coEvery { mockFuelingRepository.getFuelingStatus() } throws RuntimeException("Network error")

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.error).isEqualTo("Failed to load fueling status")
        }
    }

    @Test
    fun `loadFuelingStatus refreshes data`() = runTest {
        val greenStatus = FuelingStatusResponse(
            status = FuelingLevel.GREEN,
            proteinPct = 0.75,
            caloriesPct = 0.80,
            hydrationPct = 0.60,
            message = "Well fueled"
        )
        val yellowStatus = FuelingStatusResponse(
            status = FuelingLevel.YELLOW,
            proteinPct = 0.50,
            caloriesPct = 0.60,
            hydrationPct = 0.40,
            message = "Borderline fueling"
        )
        coEvery { mockFuelingRepository.getFuelingStatus() } returns greenStatus

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = expectMostRecentItem()
            assertThat(initial.fuelingStatus!!.status).isEqualTo(FuelingLevel.GREEN)

            coEvery { mockFuelingRepository.getFuelingStatus() } returns yellowStatus
            viewModel.loadFuelingStatus()

            val refreshed = expectMostRecentItem()
            assertThat(refreshed.fuelingStatus!!.status).isEqualTo(FuelingLevel.YELLOW)
        }
    }

    @Test
    fun `onWorkoutCompleted schedules protein nudge`() = runTest {
        coEvery { mockFuelingRepository.getFuelingStatus() } returns null

        val viewModel = createViewModel()
        viewModel.onWorkoutCompleted()

        verify { mockProteinNudgeService.schedulePostWorkoutCheck(ProteinNudgeService.DELAY_MINUTES_DEFAULT) }
    }

    @Test
    fun `clearError removes error message`() = runTest {
        coEvery { mockFuelingRepository.getFuelingStatus() } returns null

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val errorState = expectMostRecentItem()
            assertThat(errorState.error).isNotNull()

            viewModel.clearError()

            val clearedState = expectMostRecentItem()
            assertThat(clearedState.error).isNull()
        }
    }

    @Test
    fun `green fueling status is parsed correctly`() = runTest {
        val status = FuelingStatusResponse(
            status = FuelingLevel.GREEN,
            proteinPct = 0.90,
            caloriesPct = 0.85,
            hydrationPct = 0.70,
            message = "Great fueling today"
        )
        coEvery { mockFuelingRepository.getFuelingStatus() } returns status

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.fuelingStatus!!.caloriesPct).isEqualTo(0.85)
            assertThat(state.fuelingStatus!!.hydrationPct).isEqualTo(0.70)
            assertThat(state.fuelingStatus!!.message).isEqualTo("Great fueling today")
        }
    }

    @Test
    fun `red fueling status is parsed correctly`() = runTest {
        val status = FuelingStatusResponse(
            status = FuelingLevel.RED,
            proteinPct = 0.20,
            caloriesPct = 0.30,
            hydrationPct = 0.25,
            message = "Very low nutrition — consider lighter session"
        )
        coEvery { mockFuelingRepository.getFuelingStatus() } returns status

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.fuelingStatus!!.status).isEqualTo(FuelingLevel.RED)
            assertThat(state.fuelingStatus!!.proteinPct).isEqualTo(0.20)
        }
    }

    @Test
    fun `yellow fueling status is parsed correctly`() = runTest {
        val status = FuelingStatusResponse(
            status = FuelingLevel.YELLOW,
            proteinPct = 0.55,
            caloriesPct = 0.50,
            hydrationPct = 0.45,
            message = "Borderline — eat something before training"
        )
        coEvery { mockFuelingRepository.getFuelingStatus() } returns status

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.fuelingStatus!!.status).isEqualTo(FuelingLevel.YELLOW)
        }
    }
}
