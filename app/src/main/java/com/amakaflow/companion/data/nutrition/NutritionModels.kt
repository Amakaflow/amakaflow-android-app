package com.amakaflow.companion.data.nutrition

/**
 * AMA-1290/1291/1292: Nutrition data models for Health Connect integration.
 */

/**
 * Display mode for nutrition data — privacy-first approach.
 */
enum class NutritionDisplayMode(val label: String) {
    QUALITATIVE("Qualitative only"),
    PROTEIN_ONLY("Protein only"),
    FULL_MACROS("Full macros"),
    CALORIES_AND_MACROS("Calories + macros")
}

/**
 * Daily nutrition summary aggregated from Health Connect.
 */
data class DailyNutritionSummary(
    val calories: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val waterMl: Double = 0.0,
    val source: String = "Health Connect"
)

/**
 * Qualitative nutrition label based on macros.
 */
enum class NutritionLabel(val text: String) {
    WELL_FUELED("Well fueled"),
    LOW_PROTEIN("Low protein"),
    UNDER_EATING("Under-eating"),
    ON_TRACK("On track"),
    NO_DATA("No data yet")
}

/**
 * Nutrition settings stored in SharedPreferences.
 */
data class NutritionSettings(
    val isEnabled: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val displayMode: NutritionDisplayMode = NutritionDisplayMode.QUALITATIVE,
    val proteinTargetGrams: Int = 120,
    val waterTargetMl: Int = 2500
)
