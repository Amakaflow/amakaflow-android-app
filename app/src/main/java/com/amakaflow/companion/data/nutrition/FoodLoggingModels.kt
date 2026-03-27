package com.amakaflow.companion.data.nutrition

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * AMA-1294: AI Food Logging data models.
 * Used by photo analysis, barcode scanning, and NLP text entry features.
 */

// --- Photo Analysis ---

@Serializable
data class PhotoAnalysisRequest(
    @SerialName("image_base64") val imageBase64: String
)

@Serializable
data class PhotoAnalysisResponse(
    @SerialName("calories_range") val caloriesRange: List<Int>,
    @SerialName("protein_g") val proteinG: Double,
    @SerialName("carbs_g") val carbsG: Double,
    @SerialName("fat_g") val fatG: Double,
    val confidence: String,
    val description: String
)

// --- Barcode Lookup ---

@Serializable
data class BarcodeProductResponse(
    @SerialName("product_name") val productName: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    @SerialName("serving_size") val servingSize: String
)

// --- NLP Text Entry ---

@Serializable
data class ParseTextRequest(
    val text: String
)

@Serializable
data class ParsedFoodItem(
    val name: String,
    val qty: Int,
    val calories: Int,
    val protein: Double
)

@Serializable
data class ParsedNutritionTotal(
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

@Serializable
data class ParseTextResponse(
    val items: List<ParsedFoodItem>,
    val total: ParsedNutritionTotal
)
