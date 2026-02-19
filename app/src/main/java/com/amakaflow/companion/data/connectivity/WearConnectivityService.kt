package com.amakaflow.companion.data.connectivity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents the connection state between the phone and Wear OS device.
 */
sealed class WearConnectionState {
    data object Disconnected : WearConnectionState()
    data object Connecting : WearConnectionState()
    data object Connected : WearConnectionState()
    data class Error(val message: String) : WearConnectionState()
}

/**
 * Message sent from phone to watch or vice versa.
 */
data class WearMessage(
    val id: String,
    val type: MessageType,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Types of messages exchanged between phone and watch.
 */
enum class MessageType {
    WORKOUT_START,
    WORKOUT_PAUSE,
    WORKOUT_RESUME,
    WORKOUT_COMPLETE,
    WORKOUT_CANCEL,
    HEART_RATE_UPDATE,
    WORKOUT_STATE_SYNC,
    EXERCISE_COMPLETE,
    SET_COMPLETE,
    TIMER_SYNC
}

/**
 * Service interface for communication with Wear OS devices.
 * Handles message sending/receiving, connection state changes, and reconnection behavior.
 */
interface WearConnectivityService {
    /**
     * Current connection state as a StateFlow.
     */
    val connectionState: StateFlow<WearConnectionState>

    /**
     * Incoming messages from the watch.
     */
    val incomingMessages: Flow<WearMessage>

    /**
     * Connect to the Wear OS device.
     * @return true if connection was initiated successfully
     */
    suspend fun connect(): Boolean

    /**
     * Disconnect from the Wear OS device.
     */
    suspend fun disconnect()

    /**
     * Send a message to the connected watch.
     * @param message The message to send
     * @return true if message was sent successfully
     */
    suspend fun sendMessage(message: WearMessage): Boolean

    /**
     * Send a workout start message to the watch.
     */
    suspend fun sendWorkoutStart(workoutId: String, workoutName: String): Boolean

    /**
     * Send a workout pause message to the watch.
     */
    suspend fun sendWorkoutPause(): Boolean

    /**
     * Send a workout resume message to the watch.
     */
    suspend fun sendWorkoutResume(): Boolean

    /**
     * Send a workout complete message to the watch.
     */
    suspend fun sendWorkoutComplete(): Boolean

    /**
     * Send a heart rate update to the watch.
     */
    suspend fun sendHeartRateUpdate(heartRate: Int): Boolean

    /**
     * Check if currently connected to a watch.
     */
    fun isConnected(): Boolean

    /**
     * Attempt to reconnect to the watch.
     */
    suspend fun reconnect(): Boolean
}
