package com.amakaflow.companion.data.nutrition

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class FoodLoggingModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `PhotoAnalysisResponse deserializes correctly`() {
        val jsonStr = """
            {
                "calories_range": [400, 500],
                "protein_g": 35.0,
                "carbs_g": 60.0,
                "fat_g": 15.0,
                "confidence": "medium",
                "description": "Grilled chicken with rice and vegetables"
            }
        """.trimIndent()

        val result = json.decodeFromString<PhotoAnalysisResponse>(jsonStr)
        assertThat(result.caloriesRange).containsExactly(400, 500)
        assertThat(result.proteinG).isEqualTo(35.0)
        assertThat(result.carbsG).isEqualTo(60.0)
        assertThat(result.fatG).isEqualTo(15.0)
        assertThat(result.confidence).isEqualTo("medium")
        assertThat(result.description).isEqualTo("Grilled chicken with rice and vegetables")
    }

    @Test
    fun `BarcodeProductResponse deserializes correctly`() {
        val jsonStr = """
            {
                "product_name": "Greek Yogurt",
                "calories": 150,
                "protein": 15.0,
                "carbs": 12.0,
                "fat": 5.0,
                "serving_size": "170g"
            }
        """.trimIndent()

        val result = json.decodeFromString<BarcodeProductResponse>(jsonStr)
        assertThat(result.productName).isEqualTo("Greek Yogurt")
        assertThat(result.calories).isEqualTo(150)
        assertThat(result.protein).isEqualTo(15.0)
        assertThat(result.servingSize).isEqualTo("170g")
    }

    @Test
    fun `ParseTextResponse deserializes correctly`() {
        val jsonStr = """
            {
                "items": [
                    {"name": "eggs", "qty": 2, "calories": 140, "protein": 12.0},
                    {"name": "toast", "qty": 1, "calories": 80, "protein": 3.0}
                ],
                "total": {
                    "calories": 220,
                    "protein": 15.0,
                    "carbs": 20.0,
                    "fat": 10.0
                }
            }
        """.trimIndent()

        val result = json.decodeFromString<ParseTextResponse>(jsonStr)
        assertThat(result.items).hasSize(2)
        assertThat(result.items[0].name).isEqualTo("eggs")
        assertThat(result.items[0].qty).isEqualTo(2)
        assertThat(result.items[0].calories).isEqualTo(140)
        assertThat(result.items[1].name).isEqualTo("toast")
        assertThat(result.total.calories).isEqualTo(220)
        assertThat(result.total.protein).isEqualTo(15.0)
        assertThat(result.total.carbs).isEqualTo(20.0)
        assertThat(result.total.fat).isEqualTo(10.0)
    }

    @Test
    fun `ParseTextRequest serializes correctly`() {
        val request = ParseTextRequest(text = "I ate 2 eggs and toast")
        val serialized = json.encodeToString(ParseTextRequest.serializer(), request)
        assertThat(serialized).contains("\"text\"")
        assertThat(serialized).contains("I ate 2 eggs and toast")
    }

    @Test
    fun `PhotoAnalysisRequest serializes correctly`() {
        val request = PhotoAnalysisRequest(imageBase64 = "abc123==")
        val serialized = json.encodeToString(PhotoAnalysisRequest.serializer(), request)
        assertThat(serialized).contains("\"image_base64\"")
        assertThat(serialized).contains("abc123==")
    }
}
