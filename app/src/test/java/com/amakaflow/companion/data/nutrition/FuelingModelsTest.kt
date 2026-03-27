package com.amakaflow.companion.data.nutrition

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class FuelingModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `FuelingStatusResponse deserializes green status`() {
        val jsonStr = """
            {
                "status": "green",
                "protein_pct": 0.75,
                "calories_pct": 0.80,
                "hydration_pct": 0.60,
                "message": "Well fueled for training"
            }
        """.trimIndent()

        val result = json.decodeFromString<FuelingStatusResponse>(jsonStr)

        assertThat(result.status).isEqualTo(FuelingLevel.GREEN)
        assertThat(result.proteinPct).isEqualTo(0.75)
        assertThat(result.caloriesPct).isEqualTo(0.80)
        assertThat(result.hydrationPct).isEqualTo(0.60)
        assertThat(result.message).isEqualTo("Well fueled for training")
    }

    @Test
    fun `FuelingStatusResponse deserializes yellow status`() {
        val jsonStr = """
            {
                "status": "yellow",
                "protein_pct": 0.50,
                "calories_pct": 0.55,
                "hydration_pct": 0.40,
                "message": "Borderline fueling"
            }
        """.trimIndent()

        val result = json.decodeFromString<FuelingStatusResponse>(jsonStr)

        assertThat(result.status).isEqualTo(FuelingLevel.YELLOW)
    }

    @Test
    fun `FuelingStatusResponse deserializes red status`() {
        val jsonStr = """
            {
                "status": "red",
                "protein_pct": 0.20,
                "calories_pct": 0.25,
                "hydration_pct": 0.15,
                "message": "Nutrition very low"
            }
        """.trimIndent()

        val result = json.decodeFromString<FuelingStatusResponse>(jsonStr)

        assertThat(result.status).isEqualTo(FuelingLevel.RED)
        assertThat(result.proteinPct).isEqualTo(0.20)
    }

    @Test
    fun `ProteinNudgeResponse deserializes when should nudge`() {
        val jsonStr = """
            {
                "should_nudge": true,
                "protein_current": 45,
                "protein_target": 120,
                "message": "Have you had your post-workout protein?"
            }
        """.trimIndent()

        val result = json.decodeFromString<ProteinNudgeResponse>(jsonStr)

        assertThat(result.shouldNudge).isTrue()
        assertThat(result.proteinCurrent).isEqualTo(45)
        assertThat(result.proteinTarget).isEqualTo(120)
        assertThat(result.message).isEqualTo("Have you had your post-workout protein?")
    }

    @Test
    fun `ProteinNudgeResponse deserializes when should not nudge`() {
        val jsonStr = """
            {
                "should_nudge": false,
                "protein_current": 100,
                "protein_target": 120,
                "message": "Protein intake is on track"
            }
        """.trimIndent()

        val result = json.decodeFromString<ProteinNudgeResponse>(jsonStr)

        assertThat(result.shouldNudge).isFalse()
        assertThat(result.proteinCurrent).isEqualTo(100)
    }

    @Test
    fun `FuelingStatusResponse ignores unknown keys`() {
        val jsonStr = """
            {
                "status": "green",
                "protein_pct": 0.75,
                "calories_pct": 0.80,
                "hydration_pct": 0.60,
                "message": "Well fueled",
                "extra_field": "should be ignored"
            }
        """.trimIndent()

        val result = json.decodeFromString<FuelingStatusResponse>(jsonStr)

        assertThat(result.status).isEqualTo(FuelingLevel.GREEN)
    }

    @Test
    fun `FuelingStatusResponse serializes correctly`() {
        val status = FuelingStatusResponse(
            status = FuelingLevel.GREEN,
            proteinPct = 0.75,
            caloriesPct = 0.80,
            hydrationPct = 0.60,
            message = "Well fueled"
        )

        val jsonStr = json.encodeToString(FuelingStatusResponse.serializer(), status)

        assertThat(jsonStr).contains("\"status\":\"green\"")
        assertThat(jsonStr).contains("\"protein_pct\":0.75")
        assertThat(jsonStr).contains("\"calories_pct\":0.8")
    }
}
