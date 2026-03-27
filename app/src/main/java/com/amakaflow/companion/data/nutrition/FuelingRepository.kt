package com.amakaflow.companion.data.nutrition

import android.util.Log
import com.amakaflow.companion.data.api.AmakaflowApi
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FuelingRepository"

/**
 * AMA-1293: Repository for fetching fueling status and protein nudge data from the backend.
 */
@Singleton
class FuelingRepository @Inject constructor(
    private val api: AmakaflowApi
) {
    /**
     * Fetch today's fueling status. Returns null on error (caller decides how to handle).
     */
    suspend fun getFuelingStatus(): FuelingStatusResponse? {
        return try {
            val response = api.getFuelingStatus()
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.w(TAG, "Fueling status API returned ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching fueling status", e)
            null
        }
    }

    /**
     * Check whether a post-workout protein nudge should fire.
     * Returns null on error.
     */
    suspend fun checkProteinNudge(): ProteinNudgeResponse? {
        return try {
            val response = api.checkProteinNudge()
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.w(TAG, "Protein nudge check API returned ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking protein nudge", e)
            null
        }
    }
}
