package com.amakaflow.companion.data.connectivity

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of WearConnectivityService for communicating with Wear OS devices.
 * Uses a simulated connection for testing purposes.
 */
@Singleton
class WearConnectivityServiceImpl @Inject constructor() : WearConnectivityService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val json = Json { ignoreUnknownKeys = true }

    private val _connectionState = MutableStateFlow<WearConnectionState>(WearConnectionState.Disconnected)
    override val connectionState: StateFlow<WearConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<WearMessage>(replay = 1)
    override val incomingMessages: Flow<WearMessage> = _incomingMessages.asSharedFlow()

    private var isConnected = false
    private var retryCount = 0
    private val maxRetries = 3

    override suspend fun connect(): Boolean {
        if (isConnected) return true

        _connectionState.value = WearConnectionState.Connecting
        retryCount = 0

        return try {
            // Simulate connection delay
            delay(100)
            isConnected = true
            _connectionState.value = WearConnectionState.Connected
            retryCount = 0
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _connectionState.value = WearConnectionState.Error(e.message ?: "Connection failed")
            false
        }
    }

    override suspend fun disconnect() {
        isConnected = false
        _connectionState.value = WearConnectionState.Disconnected
    }

    override suspend fun sendMessage(message: WearMessage): Boolean {
        if (!isConnected) {
            return false
        }

        return try {
            // Simulate message sending delay
            delay(50)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun sendWorkoutStart(workoutId: String, workoutName: String): Boolean {
        val payload = json.encodeToString(mapOf("workoutId" to workoutId, "workoutName" to workoutName))
        val message = WearMessage(
            id = UUID.randomUUID().toString(),
            type = MessageType.WORKOUT_START,
            payload = payload
        )
        return sendMessage(message)
    }

    override suspend fun sendWorkoutPause(): Boolean {
        val message = WearMessage(
            id = UUID.randomUUID().toString(),
            type = MessageType.WORKOUT_PAUSE,
            payload = "{}"
        )
        return sendMessage(message)
    }

    override suspend fun sendWorkoutResume(): Boolean {
        val message = WearMessage(
            id = UUID.randomUUID().toString(),
            type = MessageType.WORKOUT_RESUME,
            payload = "{}"
        )
        return sendMessage(message)
    }

    override suspend fun sendWorkoutComplete(): Boolean {
        val message = WearMessage(
            id = UUID.randomUUID().toString(),
            type = MessageType.WORKOUT_COMPLETE,
            payload = "{}"
        )
        return sendMessage(message)
    }

    override suspend fun sendHeartRateUpdate(heartRate: Int): Boolean {
        val payload = json.encodeToString(mapOf("heartRate" to heartRate))
        val message = WearMessage(
            id = UUID.randomUUID().toString(),
            type = MessageType.HEART_RATE_UPDATE,
            payload = payload
        )
        return sendMessage(message)
    }

    override fun isConnected(): Boolean = isConnected

    override suspend fun reconnect(): Boolean {
        if (isConnected) return true

        for (attempt in 1..maxRetries) {
            retryCount = attempt
            _connectionState.value = WearConnectionState.Connecting
            
            try {
                delay(200L * attempt) // Exponential backoff
                
                if (connect()) {
                    return true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _connectionState.value = WearConnectionState.Error(e.message ?: "Reconnection failed")
            }
        }

        _connectionState.value = WearConnectionState.Error("Max reconnection attempts reached")
        return false
    }

    /**
     * Simulate receiving a message from the watch (for testing).
     */
    suspend fun simulateIncomingMessage(message: WearMessage) {
        _incomingMessages.emit(message)
    }

    /**
     * Simulate connection loss (for testing).
     */
    fun simulateConnectionLoss() {
        isConnected = false
        _connectionState.value = WearConnectionState.Disconnected
    }

    /**
     * Simulate connection error (for testing).
     */
    fun simulateConnectionError(errorMessage: String) {
        isConnected = false
        _connectionState.value = WearConnectionState.Error(errorMessage)
    }
}
