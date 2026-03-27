package com.amakaflow.companion.data.nutrition

import android.util.Log
import com.amakaflow.companion.data.api.AmakaflowApi
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FoodLoggingRepository"

/**
 * AMA-1294: Repository for AI food logging — photo analysis, barcode lookup, and NLP text parsing.
 */
@Singleton
class FoodLoggingRepository @Inject constructor(
    private val api: AmakaflowApi
) {
    /**
     * Analyze a meal photo using Claude Vision.
     * Returns estimated macros with confidence level, or null on error.
     */
    suspend fun analyzePhoto(imageBase64: String): PhotoAnalysisResponse? {
        return try {
            val response = api.analyzePhoto(PhotoAnalysisRequest(imageBase64))
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.w(TAG, "Photo analysis API returned ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing photo", e)
            null
        }
    }

    /**
     * Look up a product by barcode using Open Food Facts.
     * Returns product nutrition info, or null if not found / error.
     */
    suspend fun lookupBarcode(code: String): BarcodeProductResponse? {
        return try {
            val response = api.lookupBarcode(code)
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.w(TAG, "Barcode lookup API returned ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up barcode", e)
            null
        }
    }

    /**
     * Parse free-text food description using Claude NLP.
     * Returns parsed items with estimated macros, or null on error.
     */
    suspend fun parseText(text: String): ParseTextResponse? {
        return try {
            val response = api.parseNutritionText(ParseTextRequest(text))
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.w(TAG, "Text parsing API returned ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing food text", e)
            null
        }
    }
}
