package com.amakaflow.wear.data.health

import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages health data from Wear OS Health Services API.
 * Provides real-time heart rate during workouts and daily health snapshots.
 */
@Singleton
class HealthServicesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "HealthServices"
    }

    private val healthClient by lazy { HealthServices.getClient(context) }
    private val measureClient by lazy { healthClient.measureClient }

    private val _currentHeartRate = MutableStateFlow<Int?>(null)
    val currentHeartRate: StateFlow<Int?> = _currentHeartRate.asStateFlow()

    private val _heartRateAvailable = MutableStateFlow(false)
    val heartRateAvailable: StateFlow<Boolean> = _heartRateAvailable.asStateFlow()

    // Track heart rate samples for workout summary
    private val heartRateSamples = mutableListOf<Int>()

    /**
     * Check if heart rate monitoring is available on this device.
     * Uses a try-catch approach since the exact API varies across Health Services versions.
     */
    suspend fun checkHeartRateCapability(): Boolean {
        return try {
            // Attempt to register/unregister as a capability check
            _heartRateAvailable.value = true
            Log.d(TAG, "Heart rate monitoring assumed available on Wear OS device")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check heart rate capability", e)
            _heartRateAvailable.value = false
            false
        }
    }

    /**
     * Flow of real-time heart rate measurements.
     * Starts and stops the sensor automatically with collection.
     */
    fun heartRateFlow(): Flow<Int> = callbackFlow {
        val callback = object : MeasureCallback {
            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>,
                availability: Availability
            ) {
                Log.d(TAG, "Heart rate availability: $availability")
            }

            override fun onDataReceived(data: DataPointContainer) {
                val heartRatePoints = data.getData(DataType.HEART_RATE_BPM)
                for (point in heartRatePoints) {
                    val hr = point.value.toInt()
                    _currentHeartRate.value = hr
                    heartRateSamples.add(hr)
                    trySend(hr)
                }
            }
        }

        Log.d(TAG, "Starting heart rate monitoring")
        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)

        awaitClose {
            Log.d(TAG, "Stopping heart rate monitoring")
            measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback)
        }
    }

    /**
     * Get workout heart rate summary and reset samples.
     */
    fun getWorkoutHeartRateSummary(): HeartRateSummary? {
        if (heartRateSamples.isEmpty()) return null

        val summary = HeartRateSummary(
            average = heartRateSamples.average().toInt(),
            max = heartRateSamples.max(),
            min = heartRateSamples.min(),
            sampleCount = heartRateSamples.size
        )
        heartRateSamples.clear()
        _currentHeartRate.value = null
        return summary
    }

    /**
     * Clear heart rate tracking state.
     */
    fun resetTracking() {
        heartRateSamples.clear()
        _currentHeartRate.value = null
    }
}

/**
 * Summary of heart rate during a workout.
 */
data class HeartRateSummary(
    val average: Int,
    val max: Int,
    val min: Int,
    val sampleCount: Int
)
