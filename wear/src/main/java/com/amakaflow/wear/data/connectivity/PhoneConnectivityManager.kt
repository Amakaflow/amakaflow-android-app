package com.amakaflow.wear.data.connectivity

import android.content.Context
import android.util.Log
import com.amakaflow.shared.connectivity.WearDataPaths
import com.amakaflow.shared.model.WearDaySchedule
import com.amakaflow.shared.model.WearReadinessData
import com.amakaflow.shared.model.WearWorkout
import com.amakaflow.shared.model.WearWorkoutCompletion
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the watch-side connection to the phone app.
 * Receives synced data and sends messages back to the phone.
 */
@Singleton
class PhoneConnectivityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "PhoneConnectivity"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val messageClient: MessageClient by lazy { Wearable.getMessageClient(context) }
    private val capabilityClient: CapabilityClient by lazy { Wearable.getCapabilityClient(context) }

    private val _workouts = MutableStateFlow<List<WearWorkout>>(emptyList())
    val workouts: StateFlow<List<WearWorkout>> = _workouts.asStateFlow()

    private val _schedule = MutableStateFlow<WearDaySchedule?>(null)
    val schedule: StateFlow<WearDaySchedule?> = _schedule.asStateFlow()

    private val _readiness = MutableStateFlow<WearReadinessData?>(null)
    val readiness: StateFlow<WearReadinessData?> = _readiness.asStateFlow()

    private val _isPhoneConnected = MutableStateFlow(false)
    val isPhoneConnected: StateFlow<Boolean> = _isPhoneConnected.asStateFlow()

    // Workout execution events from phone
    private val _workoutStartEvent = MutableStateFlow<String?>(null)
    val workoutStartEvent: StateFlow<String?> = _workoutStartEvent.asStateFlow()

    private val _workoutPauseEvent = MutableStateFlow(false)
    val workoutPauseEvent: StateFlow<Boolean> = _workoutPauseEvent.asStateFlow()

    private val _workoutResumeEvent = MutableStateFlow(false)
    val workoutResumeEvent: StateFlow<Boolean> = _workoutResumeEvent.asStateFlow()

    private val _workoutCancelEvent = MutableStateFlow(false)
    val workoutCancelEvent: StateFlow<Boolean> = _workoutCancelEvent.asStateFlow()

    // =========================================================================
    // Data reception (called from WearDataListenerService)
    // =========================================================================

    fun onWorkoutsReceived(payload: String) {
        try {
            val workouts = json.decodeFromString<List<WearWorkout>>(payload)
            _workouts.value = workouts
            Log.d(TAG, "Received ${workouts.size} workouts from phone")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse workouts", e)
        }
    }

    fun onScheduleReceived(payload: String) {
        try {
            val schedule = json.decodeFromString<WearDaySchedule>(payload)
            _schedule.value = schedule
            Log.d(TAG, "Received schedule for ${schedule.date}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse schedule", e)
        }
    }

    fun onReadinessReceived(payload: String) {
        try {
            val readiness = json.decodeFromString<WearReadinessData>(payload)
            _readiness.value = readiness
            Log.d(TAG, "Received readiness score: ${readiness.score}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse readiness", e)
        }
    }

    fun onWorkoutStartReceived(payload: String) {
        _workoutStartEvent.value = payload
    }

    fun onWorkoutPauseReceived() {
        _workoutPauseEvent.value = true
    }

    fun onWorkoutResumeReceived() {
        _workoutResumeEvent.value = true
    }

    fun onWorkoutCancelReceived() {
        _workoutCancelEvent.value = true
    }

    fun onStateSyncReceived(payload: String) {
        Log.d(TAG, "State sync received: $payload")
    }

    // =========================================================================
    // Send messages to phone
    // =========================================================================

    suspend fun sendWorkoutCompletion(completion: WearWorkoutCompletion): Boolean {
        return sendToPhone(
            WearDataPaths.MSG_WORKOUT_COMPLETE,
            json.encodeToString(completion)
        )
    }

    suspend fun sendHeartRate(heartRate: Int): Boolean {
        return sendToPhone(
            WearDataPaths.MSG_HEART_RATE,
            """{"heartRate":$heartRate}"""
        )
    }

    suspend fun requestSync(): Boolean {
        return sendToPhone(WearDataPaths.MSG_REQUEST_SYNC, "{}")
    }

    suspend fun checkPhoneConnection(): Boolean {
        return try {
            val capInfo = capabilityClient
                .getCapability(WearDataPaths.PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()
            val connected = capInfo.nodes.isNotEmpty()
            _isPhoneConnected.value = connected
            connected
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check phone connection", e)
            _isPhoneConnected.value = false
            false
        }
    }

    private suspend fun sendToPhone(path: String, payload: String): Boolean {
        return try {
            val capInfo = capabilityClient
                .getCapability(WearDataPaths.PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()

            val phoneNode = capInfo.nodes.firstOrNull { it.isNearby }
                ?: capInfo.nodes.firstOrNull()

            if (phoneNode != null) {
                messageClient.sendMessage(
                    phoneNode.id,
                    path,
                    payload.toByteArray(Charsets.UTF_8)
                ).await()
                Log.d(TAG, "Sent message to phone: $path")
                true
            } else {
                Log.w(TAG, "No phone node found for: $path")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message to phone: $path", e)
            false
        }
    }

    // =========================================================================
    // Event consumption helpers
    // =========================================================================

    fun consumeWorkoutStartEvent() {
        _workoutStartEvent.value = null
    }

    fun consumeWorkoutPauseEvent() {
        _workoutPauseEvent.value = false
    }

    fun consumeWorkoutResumeEvent() {
        _workoutResumeEvent.value = false
    }

    fun consumeWorkoutCancelEvent() {
        _workoutCancelEvent.value = false
    }
}
