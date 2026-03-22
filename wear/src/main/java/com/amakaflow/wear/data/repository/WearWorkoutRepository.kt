package com.amakaflow.wear.data.repository

import com.amakaflow.shared.model.WearDaySchedule
import com.amakaflow.shared.model.WearReadinessData
import com.amakaflow.shared.model.WearWorkout
import com.amakaflow.shared.model.WearWorkoutCompletion
import com.amakaflow.wear.data.connectivity.PhoneConnectivityManager
import com.amakaflow.wear.data.health.HealthServicesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for workout data on the watch.
 * Aggregates data from PhoneConnectivityManager and HealthServicesManager.
 */
@Singleton
class WearWorkoutRepository @Inject constructor(
    private val phoneConnectivityManager: PhoneConnectivityManager,
    private val healthServicesManager: HealthServicesManager
) {
    /** Available workouts synced from phone. */
    val workouts: StateFlow<List<WearWorkout>> = phoneConnectivityManager.workouts

    /** Today's schedule synced from phone. */
    val schedule: StateFlow<WearDaySchedule?> = phoneConnectivityManager.schedule

    /** Readiness data synced from phone. */
    val readiness: StateFlow<WearReadinessData?> = phoneConnectivityManager.readiness

    /** Whether the phone is connected. */
    val isPhoneConnected: StateFlow<Boolean> = phoneConnectivityManager.isPhoneConnected

    /** Current heart rate from watch sensors. */
    val currentHeartRate: StateFlow<Int?> = healthServicesManager.currentHeartRate

    /**
     * Get a specific workout by ID.
     */
    fun getWorkout(workoutId: String): Flow<WearWorkout?> {
        return workouts.map { list -> list.find { it.id == workoutId } }
    }

    /**
     * Start heart rate monitoring.
     */
    fun startHeartRateMonitoring(): Flow<Int> {
        return healthServicesManager.heartRateFlow()
    }

    /**
     * Send workout completion to phone.
     */
    suspend fun submitCompletion(completion: WearWorkoutCompletion): Boolean {
        return phoneConnectivityManager.sendWorkoutCompletion(completion)
    }

    /**
     * Send heart rate to phone during workout.
     */
    suspend fun sendHeartRateToPhone(heartRate: Int): Boolean {
        return phoneConnectivityManager.sendHeartRate(heartRate)
    }

    /**
     * Request a data sync from phone.
     */
    suspend fun requestSync(): Boolean {
        return phoneConnectivityManager.requestSync()
    }

    /**
     * Check if phone is reachable.
     */
    suspend fun checkPhoneConnection(): Boolean {
        return phoneConnectivityManager.checkPhoneConnection()
    }

    /**
     * Get heart rate summary for completed workout.
     */
    fun getHeartRateSummary() = healthServicesManager.getWorkoutHeartRateSummary()

    /**
     * Reset health tracking state.
     */
    fun resetHealthTracking() = healthServicesManager.resetTracking()
}
