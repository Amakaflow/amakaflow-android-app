package com.amakaflow.companion.data.nutrition

import com.amakaflow.companion.data.api.AmakaflowApi
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class FoodLoggingRepositoryTest {

    private lateinit var mockApi: AmakaflowApi
    private lateinit var repository: FoodLoggingRepository

    @Before
    fun setup() {
        mockApi = mockk(relaxed = true)
        repository = FoodLoggingRepository(mockApi)
    }

    // --- Photo Analysis ---

    @Test
    fun `analyzePhoto returns response on success`() = runTest {
        val response = PhotoAnalysisResponse(
            caloriesRange = listOf(400, 500),
            proteinG = 35.0,
            carbsG = 60.0,
            fatG = 15.0,
            confidence = "medium",
            description = "Grilled chicken with rice"
        )
        coEvery { mockApi.analyzePhoto(any()) } returns Response.success(response)

        val result = repository.analyzePhoto("base64data")
        assertThat(result).isEqualTo(response)
        assertThat(result!!.confidence).isEqualTo("medium")
        assertThat(result.caloriesRange).containsExactly(400, 500)
    }

    @Test
    fun `analyzePhoto returns null on HTTP error`() = runTest {
        coEvery { mockApi.analyzePhoto(any()) } returns Response.error(
            500, okhttp3.ResponseBody.create(null, "error")
        )

        val result = repository.analyzePhoto("base64data")
        assertThat(result).isNull()
    }

    @Test
    fun `analyzePhoto returns null on network exception`() = runTest {
        coEvery { mockApi.analyzePhoto(any()) } throws RuntimeException("Network error")

        val result = repository.analyzePhoto("base64data")
        assertThat(result).isNull()
    }

    // --- Barcode Lookup ---

    @Test
    fun `lookupBarcode returns response on success`() = runTest {
        val response = BarcodeProductResponse(
            productName = "Protein Bar",
            calories = 200,
            protein = 20.0,
            carbs = 25.0,
            fat = 8.0,
            servingSize = "60g"
        )
        coEvery { mockApi.lookupBarcode("1234567890") } returns Response.success(response)

        val result = repository.lookupBarcode("1234567890")
        assertThat(result).isEqualTo(response)
        assertThat(result!!.productName).isEqualTo("Protein Bar")
    }

    @Test
    fun `lookupBarcode returns null on 404`() = runTest {
        coEvery { mockApi.lookupBarcode(any()) } returns Response.error(
            404, okhttp3.ResponseBody.create(null, "not found")
        )

        val result = repository.lookupBarcode("0000000000")
        assertThat(result).isNull()
    }

    @Test
    fun `lookupBarcode returns null on exception`() = runTest {
        coEvery { mockApi.lookupBarcode(any()) } throws RuntimeException("Timeout")

        val result = repository.lookupBarcode("1234567890")
        assertThat(result).isNull()
    }

    // --- Text Parsing ---

    @Test
    fun `parseText returns response on success`() = runTest {
        val response = ParseTextResponse(
            items = listOf(
                ParsedFoodItem(name = "eggs", qty = 2, calories = 140, protein = 12.0),
                ParsedFoodItem(name = "toast", qty = 1, calories = 80, protein = 3.0)
            ),
            total = ParsedNutritionTotal(calories = 220, protein = 15.0, carbs = 20.0, fat = 10.0)
        )
        coEvery { mockApi.parseNutritionText(any()) } returns Response.success(response)

        val result = repository.parseText("I ate 2 eggs and toast")
        assertThat(result).isEqualTo(response)
        assertThat(result!!.items).hasSize(2)
        assertThat(result.total.calories).isEqualTo(220)
    }

    @Test
    fun `parseText returns null on HTTP error`() = runTest {
        coEvery { mockApi.parseNutritionText(any()) } returns Response.error(
            500, okhttp3.ResponseBody.create(null, "error")
        )

        val result = repository.parseText("something")
        assertThat(result).isNull()
    }

    @Test
    fun `parseText returns null on exception`() = runTest {
        coEvery { mockApi.parseNutritionText(any()) } throws RuntimeException("Network")

        val result = repository.parseText("eggs")
        assertThat(result).isNull()
    }
}
