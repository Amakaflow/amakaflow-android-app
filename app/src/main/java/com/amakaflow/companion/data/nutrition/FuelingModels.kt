package com.amakaflow.companion.data.nutrition

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * AMA-1293: Fueling status response from GET /nutrition/fueling-status.
 * Indicates how well-fueled the user is for their workout.
 */
@Serializable
data class FuelingStatusResponse(
    val status: FuelingLevel,
    @SerialName("protein_pct") val proteinPct: Double,
    @SerialName("calories_pct") val caloriesPct: Double,
    @SerialName("hydration_pct") val hydrationPct: Double,
    val message: String
)

/**
 * Traffic-light fueling level: green = well fueled, yellow = borderline, red = under-fueled.
 */
@Serializable
enum class FuelingLevel {
    @SerialName("green") GREEN,
    @SerialName("yellow") YELLOW,
    @SerialName("red") RED
}

/**
 * AMA-1293: Protein nudge check response from POST /nutrition/protein-nudge/check.
 * Determines whether the post-workout protein reminder should fire.
 */
@Serializable
data class ProteinNudgeResponse(
    @SerialName("should_nudge") val shouldNudge: Boolean,
    @SerialName("protein_current") val proteinCurrent: Int,
    @SerialName("protein_target") val proteinTarget: Int,
    val message: String
)
