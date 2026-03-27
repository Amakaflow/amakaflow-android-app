package com.amakaflow.companion.ui.screens.nutrition

import app.cash.turbine.test
import com.amakaflow.companion.data.nutrition.BarcodeProductResponse
import com.amakaflow.companion.data.nutrition.FoodLoggingRepository
import com.amakaflow.companion.data.nutrition.NutritionHealthConnectService
import com.amakaflow.companion.data.nutrition.ParseTextResponse
import com.amakaflow.companion.data.nutrition.ParsedFoodItem
import com.amakaflow.companion.data.nutrition.ParsedNutritionTotal
import com.amakaflow.companion.data.nutrition.PhotoAnalysisResponse
import com.amakaflow.companion.test.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FoodLoggingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockRepository: FoodLoggingRepository
    private lateinit var mockHealthConnect: NutritionHealthConnectService

    @Before
    fun setup() {
        mockRepository = mockk(relaxed = true)
        mockHealthConnect = mockk(relaxed = true)
    }

    private fun createViewModel(): FoodLoggingViewModel {
        return FoodLoggingViewModel(mockRepository, mockHealthConnect)
    }

    // --- Initial state ---

    @Test
    fun `initial state has TEXT mode and no results`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.mode).isEqualTo(FoodLoggingMode.TEXT)
            assertThat(state.isAnalyzing).isFalse()
            assertThat(state.photoResult).isNull()
            assertThat(state.barcodeResult).isNull()
            assertThat(state.textResult).isNull()
            assertThat(state.addedToDaily).isFalse()
            assertThat(state.error).isNull()
        }
    }

    // --- Mode switching ---

    @Test
    fun `setMode switches to PHOTO and clears results`() = runTest {
        val viewModel = createViewModel()
        viewModel.setMode(FoodLoggingMode.PHOTO)
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.mode).isEqualTo(FoodLoggingMode.PHOTO)
            assertThat(state.photoResult).isNull()
            assertThat(state.barcodeResult).isNull()
            assertThat(state.textResult).isNull()
        }
    }

    @Test
    fun `setMode switches to BARCODE and clears results`() = runTest {
        val viewModel = createViewModel()
        viewModel.setMode(FoodLoggingMode.BARCODE)
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.mode).isEqualTo(FoodLoggingMode.BARCODE)
        }
    }

    @Test
    fun `setMode clears previous error and addedToDaily`() = runTest {
        val viewModel = createViewModel()
        // Trigger an error first
        viewModel.setTextInput("")
        viewModel.parseText()

        viewModel.setMode(FoodLoggingMode.PHOTO)
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.error).isNull()
            assertThat(state.addedToDaily).isFalse()
        }
    }

    // --- Photo Analysis ---

    @Test
    fun `analyzePhoto sets result on success`() = runTest {
        val response = PhotoAnalysisResponse(
            caloriesRange = listOf(400, 500),
            proteinG = 35.0,
            carbsG = 60.0,
            fatG = 15.0,
            confidence = "medium",
            description = "Grilled chicken with rice"
        )
        coEvery { mockRepository.analyzePhoto(any()) } returns response

        val viewModel = createViewModel()
        viewModel.analyzePhoto("base64data")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isAnalyzing).isFalse()
            assertThat(state.photoResult).isEqualTo(response)
            assertThat(state.error).isNull()
        }
    }

    @Test
    fun `analyzePhoto sets error when repository returns null`() = runTest {
        coEvery { mockRepository.analyzePhoto(any()) } returns null

        val viewModel = createViewModel()
        viewModel.analyzePhoto("base64data")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isAnalyzing).isFalse()
            assertThat(state.photoResult).isNull()
            assertThat(state.error).isEqualTo("Could not analyze photo")
        }
    }

    @Test
    fun `analyzePhoto sets error on exception`() = runTest {
        coEvery { mockRepository.analyzePhoto(any()) } throws RuntimeException("Network error")

        val viewModel = createViewModel()
        viewModel.analyzePhoto("base64data")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isAnalyzing).isFalse()
            assertThat(state.error).isEqualTo("Photo analysis failed")
        }
    }

    // --- Barcode Lookup ---

    @Test
    fun `lookupBarcode sets result on success`() = runTest {
        val response = BarcodeProductResponse(
            productName = "Protein Bar",
            calories = 200,
            protein = 20.0,
            carbs = 25.0,
            fat = 8.0,
            servingSize = "60g"
        )
        coEvery { mockRepository.lookupBarcode("1234567890") } returns response

        val viewModel = createViewModel()
        viewModel.lookupBarcode("1234567890")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isAnalyzing).isFalse()
            assertThat(state.barcodeResult).isEqualTo(response)
            assertThat(state.error).isNull()
        }
    }

    @Test
    fun `lookupBarcode sets error when product not found`() = runTest {
        coEvery { mockRepository.lookupBarcode(any()) } returns null

        val viewModel = createViewModel()
        viewModel.lookupBarcode("0000000000")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isAnalyzing).isFalse()
            assertThat(state.barcodeResult).isNull()
            assertThat(state.error).isEqualTo("Product not found")
        }
    }

    @Test
    fun `lookupBarcode sets error on exception`() = runTest {
        coEvery { mockRepository.lookupBarcode(any()) } throws RuntimeException("Network")

        val viewModel = createViewModel()
        viewModel.lookupBarcode("1234567890")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.error).isEqualTo("Barcode lookup failed")
        }
    }

    // --- NLP Text Entry ---

    @Test
    fun `parseText sets result on success`() = runTest {
        val response = ParseTextResponse(
            items = listOf(
                ParsedFoodItem(name = "eggs", qty = 2, calories = 140, protein = 12.0),
                ParsedFoodItem(name = "toast", qty = 1, calories = 80, protein = 3.0)
            ),
            total = ParsedNutritionTotal(calories = 220, protein = 15.0, carbs = 20.0, fat = 10.0)
        )
        coEvery { mockRepository.parseText("I ate 2 eggs and toast") } returns response

        val viewModel = createViewModel()
        viewModel.setTextInput("I ate 2 eggs and toast")
        viewModel.parseText()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isAnalyzing).isFalse()
            assertThat(state.textResult).isEqualTo(response)
            assertThat(state.textResult!!.items).hasSize(2)
            assertThat(state.error).isNull()
        }
    }

    @Test
    fun `parseText sets error when text is blank`() = runTest {
        val viewModel = createViewModel()
        viewModel.setTextInput("   ")
        viewModel.parseText()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.error).isEqualTo("Enter what you ate")
        }
    }

    @Test
    fun `parseText sets error when repository returns null`() = runTest {
        coEvery { mockRepository.parseText(any()) } returns null

        val viewModel = createViewModel()
        viewModel.setTextInput("something weird")
        viewModel.parseText()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.error).isEqualTo("Could not parse food description")
        }
    }

    @Test
    fun `parseText sets error on exception`() = runTest {
        coEvery { mockRepository.parseText(any()) } throws RuntimeException("Network")

        val viewModel = createViewModel()
        viewModel.setTextInput("eggs")
        viewModel.parseText()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.error).isEqualTo("Text parsing failed")
        }
    }

    // --- Add to Daily ---

    @Test
    fun `addToDaily from photo result writes to Health Connect`() = runTest {
        val response = PhotoAnalysisResponse(
            caloriesRange = listOf(400, 500),
            proteinG = 35.0,
            carbsG = 60.0,
            fatG = 15.0,
            confidence = "medium",
            description = "Chicken and rice"
        )
        coEvery { mockRepository.analyzePhoto(any()) } returns response
        coEvery {
            mockHealthConnect.addNutritionRecord(any(), any(), any(), any())
        } returns true

        val viewModel = createViewModel()
        viewModel.analyzePhoto("base64")

        // Wait for photo analysis
        viewModel.uiState.test {
            awaitItem() // wait until photoResult is set
        }

        viewModel.addToDaily()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.addedToDaily).isTrue()
            assertThat(state.error).isNull()
        }

        coVerify {
            mockHealthConnect.addNutritionRecord(
                calories = 450.0, // average of 400, 500
                proteinGrams = 35.0,
                carbsGrams = 60.0,
                fatGrams = 15.0
            )
        }
    }

    @Test
    fun `addToDaily from barcode result writes to Health Connect`() = runTest {
        val response = BarcodeProductResponse(
            productName = "Bar",
            calories = 200,
            protein = 20.0,
            carbs = 25.0,
            fat = 8.0,
            servingSize = "60g"
        )
        coEvery { mockRepository.lookupBarcode(any()) } returns response
        coEvery {
            mockHealthConnect.addNutritionRecord(any(), any(), any(), any())
        } returns true

        val viewModel = createViewModel()
        viewModel.lookupBarcode("123")

        viewModel.uiState.test { awaitItem() }

        viewModel.addToDaily()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.addedToDaily).isTrue()
        }

        coVerify {
            mockHealthConnect.addNutritionRecord(200.0, 20.0, 25.0, 8.0)
        }
    }

    @Test
    fun `addToDaily from text result writes to Health Connect`() = runTest {
        val response = ParseTextResponse(
            items = listOf(ParsedFoodItem("eggs", 2, 140, 12.0)),
            total = ParsedNutritionTotal(calories = 140, protein = 12.0, carbs = 1.0, fat = 10.0)
        )
        coEvery { mockRepository.parseText(any()) } returns response
        coEvery {
            mockHealthConnect.addNutritionRecord(any(), any(), any(), any())
        } returns true

        val viewModel = createViewModel()
        viewModel.setTextInput("2 eggs")
        viewModel.parseText()

        viewModel.uiState.test { awaitItem() }

        viewModel.addToDaily()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.addedToDaily).isTrue()
        }
    }

    @Test
    fun `addToDaily sets error when no result available`() = runTest {
        val viewModel = createViewModel()
        viewModel.addToDaily()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.error).isEqualTo("No nutrition data to add")
        }
    }

    @Test
    fun `addToDaily sets error when Health Connect fails`() = runTest {
        val response = BarcodeProductResponse("Bar", 200, 20.0, 25.0, 8.0, "60g")
        coEvery { mockRepository.lookupBarcode(any()) } returns response
        coEvery {
            mockHealthConnect.addNutritionRecord(any(), any(), any(), any())
        } returns false

        val viewModel = createViewModel()
        viewModel.lookupBarcode("123")
        viewModel.uiState.test { awaitItem() }
        viewModel.addToDaily()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.addedToDaily).isFalse()
            assertThat(state.error).isEqualTo("Failed to save to Health Connect")
        }
    }

    // --- Utility ---

    @Test
    fun `clearError removes error message`() = runTest {
        val viewModel = createViewModel()
        viewModel.setTextInput("")
        viewModel.parseText()

        viewModel.uiState.test {
            val errorState = expectMostRecentItem()
            assertThat(errorState.error).isNotNull()

            viewModel.clearError()

            val clearedState = expectMostRecentItem()
            assertThat(clearedState.error).isNull()
        }
    }

    @Test
    fun `reset clears all state but preserves mode`() = runTest {
        val viewModel = createViewModel()
        viewModel.setMode(FoodLoggingMode.PHOTO)

        val response = PhotoAnalysisResponse(
            listOf(400, 500), 35.0, 60.0, 15.0, "medium", "Chicken"
        )
        coEvery { mockRepository.analyzePhoto(any()) } returns response
        viewModel.analyzePhoto("base64")
        viewModel.uiState.test { awaitItem() }

        viewModel.reset()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.mode).isEqualTo(FoodLoggingMode.PHOTO)
            assertThat(state.photoResult).isNull()
            assertThat(state.addedToDaily).isFalse()
        }
    }

    @Test
    fun `setTextInput updates text in state`() = runTest {
        val viewModel = createViewModel()
        viewModel.setTextInput("chicken and rice")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.textInput).isEqualTo("chicken and rice")
        }
    }
}
