package com.amakaflow.companion.ui.screens.nutrition

import app.cash.turbine.test
import com.amakaflow.companion.data.nutrition.DailyNutritionSummary
import com.amakaflow.companion.data.nutrition.NutritionDisplayMode
import com.amakaflow.companion.data.nutrition.NutritionHealthConnectService
import com.amakaflow.companion.data.nutrition.NutritionLabel
import com.amakaflow.companion.data.nutrition.NutritionSettings
import com.amakaflow.companion.data.nutrition.NutritionSettingsRepository
import com.amakaflow.companion.test.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NutritionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockHealthConnectService: NutritionHealthConnectService
    private lateinit var mockSettingsRepository: NutritionSettingsRepository

    @Before
    fun setup() {
        mockHealthConnectService = mockk(relaxed = true)
        mockSettingsRepository = mockk(relaxed = true)

        // Default: nutrition disabled, no onboarding
        every { mockSettingsRepository.getSettings() } returns NutritionSettings()
        every { mockHealthConnectService.isAvailable() } returns false
        coEvery { mockHealthConnectService.hasPermissions() } returns false
    }

    private fun createViewModel(): NutritionViewModel {
        return NutritionViewModel(mockHealthConnectService, mockSettingsRepository)
    }

    @Test
    fun `initial state is loading then resolves`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.isEnabled).isFalse()
            assertThat(state.hasCompletedOnboarding).isFalse()
        }
    }

    @Test
    fun `loads settings from repository on init`() = runTest {
        every { mockSettingsRepository.getSettings() } returns NutritionSettings(
            isEnabled = true,
            hasCompletedOnboarding = true,
            displayMode = NutritionDisplayMode.FULL_MACROS,
            proteinTargetGrams = 150,
            waterTargetMl = 3000
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isEnabled).isTrue()
            assertThat(state.hasCompletedOnboarding).isTrue()
            assertThat(state.displayMode).isEqualTo(NutritionDisplayMode.FULL_MACROS)
            assertThat(state.proteinTargetGrams).isEqualTo(150)
            assertThat(state.waterTargetMl).isEqualTo(3000)
        }
    }

    @Test
    fun `detects Health Connect availability`() = runTest {
        every { mockHealthConnectService.isAvailable() } returns true
        coEvery { mockHealthConnectService.hasPermissions() } returns true

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isHealthConnectAvailable).isTrue()
            assertThat(state.hasPermissions).isTrue()
        }
    }

    @Test
    fun `setEnabled updates state and persists`() = runTest {
        val viewModel = createViewModel()

        viewModel.setEnabled(true)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isEnabled).isTrue()
        }
        verify { mockSettingsRepository.setEnabled(true) }
    }

    @Test
    fun `setDisplayMode updates state and persists`() = runTest {
        val viewModel = createViewModel()

        viewModel.setDisplayMode(NutritionDisplayMode.CALORIES_AND_MACROS)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.displayMode).isEqualTo(NutritionDisplayMode.CALORIES_AND_MACROS)
        }
        verify { mockSettingsRepository.setDisplayMode(NutritionDisplayMode.CALORIES_AND_MACROS) }
    }

    @Test
    fun `completeOnboarding enables feature and persists`() = runTest {
        val viewModel = createViewModel()

        viewModel.completeOnboarding()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.hasCompletedOnboarding).isTrue()
            assertThat(state.isEnabled).isTrue()
        }
        verify { mockSettingsRepository.setOnboardingComplete(true) }
        verify { mockSettingsRepository.setEnabled(true) }
    }

    @Test
    fun `addProtein calls health connect service`() = runTest {
        every { mockHealthConnectService.isAvailable() } returns true
        coEvery { mockHealthConnectService.hasPermissions() } returns true
        coEvery { mockHealthConnectService.addProtein(30.0) } returns true
        coEvery { mockHealthConnectService.getDailyNutrition() } returns DailyNutritionSummary(
            proteinGrams = 30.0
        )

        val viewModel = createViewModel()
        viewModel.addProtein(30)

        coVerify { mockHealthConnectService.addProtein(30.0) }
    }

    @Test
    fun `addWater calls health connect service`() = runTest {
        every { mockHealthConnectService.isAvailable() } returns true
        coEvery { mockHealthConnectService.hasPermissions() } returns true
        coEvery { mockHealthConnectService.addWater(250.0) } returns true
        coEvery { mockHealthConnectService.getDailyNutrition() } returns DailyNutritionSummary(
            waterMl = 250.0
        )

        val viewModel = createViewModel()
        viewModel.addWater(250)

        coVerify { mockHealthConnectService.addWater(250.0) }
    }

    @Test
    fun `addProtein sets error on failure`() = runTest {
        coEvery { mockHealthConnectService.addProtein(any()) } returns false

        val viewModel = createViewModel()
        viewModel.addProtein(30)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.error).isEqualTo("Failed to log protein")
        }
    }

    @Test
    fun `addWater sets error on failure`() = runTest {
        coEvery { mockHealthConnectService.addWater(any()) } returns false

        val viewModel = createViewModel()
        viewModel.addWater()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.error).isEqualTo("Failed to log water")
        }
    }

    @Test
    fun `deleteAllData resets state`() = runTest {
        every { mockSettingsRepository.getSettings() } returns NutritionSettings(
            isEnabled = true,
            hasCompletedOnboarding = true
        )
        every { mockHealthConnectService.isAvailable() } returns true
        coEvery { mockHealthConnectService.hasPermissions() } returns true

        val viewModel = createViewModel()
        viewModel.deleteAllData()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isEnabled).isFalse()
            assertThat(state.hasCompletedOnboarding).isFalse()
        }
        coVerify { mockHealthConnectService.deleteAllNutritionData() }
        verify { mockSettingsRepository.deleteAllData() }
    }

    @Test
    fun `clearError removes error message`() = runTest {
        coEvery { mockHealthConnectService.addProtein(any()) } returns false

        val viewModel = createViewModel()
        viewModel.addProtein(30)
        viewModel.clearError()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.error).isNull()
        }
    }

    @Test
    fun `setProteinTarget updates and persists`() = runTest {
        val viewModel = createViewModel()

        viewModel.setProteinTarget(150)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.proteinTargetGrams).isEqualTo(150)
        }
        verify { mockSettingsRepository.setProteinTarget(150) }
    }

    @Test
    fun `setWaterTarget updates and persists`() = runTest {
        val viewModel = createViewModel()

        viewModel.setWaterTarget(3000)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.waterTargetMl).isEqualTo(3000)
        }
        verify { mockSettingsRepository.setWaterTarget(3000) }
    }

    @Test
    fun `computeNutritionLabel returns NO_DATA for empty summary`() {
        val label = NutritionViewModel.computeNutritionLabel(DailyNutritionSummary())
        assertThat(label).isEqualTo(NutritionLabel.NO_DATA)
    }

    @Test
    fun `computeNutritionLabel returns WELL_FUELED for adequate nutrition`() {
        val label = NutritionViewModel.computeNutritionLabel(
            DailyNutritionSummary(calories = 2000.0, proteinGrams = 120.0)
        )
        assertThat(label).isEqualTo(NutritionLabel.WELL_FUELED)
    }

    @Test
    fun `computeNutritionLabel returns LOW_PROTEIN when protein is low`() {
        val label = NutritionViewModel.computeNutritionLabel(
            DailyNutritionSummary(calories = 2000.0, proteinGrams = 40.0)
        )
        assertThat(label).isEqualTo(NutritionLabel.LOW_PROTEIN)
    }

    @Test
    fun `computeNutritionLabel returns UNDER_EATING for low everything`() {
        val label = NutritionViewModel.computeNutritionLabel(
            DailyNutritionSummary(calories = 500.0, proteinGrams = 20.0)
        )
        assertThat(label).isEqualTo(NutritionLabel.UNDER_EATING)
    }

    @Test
    fun `onPermissionsGranted updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.onPermissionsGranted()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.hasPermissions).isTrue()
        }
    }

    @Test
    fun `refreshNutrition loads data from Health Connect`() = runTest {
        every { mockHealthConnectService.isAvailable() } returns true
        coEvery { mockHealthConnectService.hasPermissions() } returns true
        val summary = DailyNutritionSummary(
            calories = 1800.0,
            proteinGrams = 100.0,
            carbsGrams = 200.0,
            fatGrams = 60.0,
            waterMl = 1500.0
        )
        coEvery { mockHealthConnectService.getDailyNutrition() } returns summary

        every { mockSettingsRepository.getSettings() } returns NutritionSettings(
            isEnabled = true,
            hasCompletedOnboarding = true
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.nutrition.calories).isEqualTo(1800.0)
            assertThat(state.nutrition.proteinGrams).isEqualTo(100.0)
            assertThat(state.nutrition.carbsGrams).isEqualTo(200.0)
            assertThat(state.nutrition.fatGrams).isEqualTo(60.0)
            assertThat(state.nutrition.waterMl).isEqualTo(1500.0)
        }
    }
}
