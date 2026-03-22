package com.amakaflow.companion.data.connectivity

import android.content.Context
import android.util.Log
import com.amakaflow.shared.connectivity.WearDataPaths
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of WearConnectivityService using Google Play Services
 * DataClient and MessageClient for Wear OS communication.
 *
 * DataClient is used for persistent synced data (workouts, schedule, readiness).
 * MessageClient is used for real-time fire-and-forget messages (controls, heart rate).
 */
@Singleton
class WearConnectivityServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WearConnectivityService, MessageClient.OnMessageReceivedListener {

    companion object {
        private const val TAG = "WearConnectivity"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val dataClient: DataClient by lazy { Wearable.getDataClient(context) }
    private val messageClient: MessageClient by lazy { Wearable.getMessageClient(context) }
    private val nodeClient: NodeClient by lazy { Wearable.getNodeClient(context) }
    private val capabilityClient: CapabilityClient by lazy { Wearable.getCapabilityClient(context) }

    private val _connectionState = MutableStateFlow<WearConnectionState>(WearConnectionState.Disconnected)
    override val connectionState: StateFlow<WearConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<WearMessage>(replay = 1)
    override val incomingMessages: Flow<WearMessage> = _incomingMessages.asSharedFlow()

    private var connectedNodeId: String? = null
    private var isListenerRegistered = false
    private var retryCount = 0
    private val maxRetries = 3

    override suspend fun connect(): Boolean {
        if (connectedNodeId != null) return true

        _connectionState.value = WearConnectionState.Connecting
        retryCount = 0

        return try {
            // Find connected watch node via capability
            val capabilityInfo = capabilityClient
                .getCapability(WearDataPaths.WATCH_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()

            val watchNode = capabilityInfo.nodes
                .firstOrNull { it.isNearby } ?: capabilityInfo.nodes.firstOrNull()

            if (watchNode != null) {
                connectedNodeId = watchNode.id
                registerMessageListener()
                _connectionState.value = WearConnectionState.Connected
                Log.d(TAG, "Connected to watch node: ${watchNode.displayName} (${watchNode.id})")
                true
            } else {
                // Fallback: try to find any connected node
                val nodes = nodeClient.connectedNodes.await()
                val node = nodes.firstOrNull()
                if (node != null) {
                    connectedNodeId = node.id
                    registerMessageListener()
                    _connectionState.value = WearConnectionState.Connected
                    Log.d(TAG, "Connected to node (fallback): ${node.displayName} (${node.id})")
                    true
                } else {
                    _connectionState.value = WearConnectionState.Error("No watch found")
                    Log.w(TAG, "No watch nodes found")
                    false
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            _connectionState.value = WearConnectionState.Error(e.message ?: "Connection failed")
            false
        }
    }

    override suspend fun disconnect() {
        unregisterMessageListener()
        connectedNodeId = null
        _connectionState.value = WearConnectionState.Disconnected
        Log.d(TAG, "Disconnected from watch")
    }

    override suspend fun sendMessage(message: WearMessage): Boolean {
        val nodeId = connectedNodeId ?: return false

        return try {
            val path = mapMessageTypeToPath(message.type)
            val payload = message.payload.toByteArray(Charsets.UTF_8)
            messageClient.sendMessage(nodeId, path, payload).await()
            Log.d(TAG, "Sent message: ${message.type} to $nodeId")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message: ${message.type}", e)
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

    override fun isConnected(): Boolean = connectedNodeId != null

    override suspend fun reconnect(): Boolean {
        if (isConnected()) return true

        for (attempt in 1..maxRetries) {
            retryCount = attempt
            _connectionState.value = WearConnectionState.Connecting

            try {
                if (connect()) {
                    return true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Reconnection attempt $attempt failed", e)
                _connectionState.value = WearConnectionState.Error(e.message ?: "Reconnection failed")
            }
        }

        _connectionState.value = WearConnectionState.Error("Max reconnection attempts reached")
        return false
    }

    // =========================================================================
    // DataLayer sync methods (persistent data)
    // =========================================================================

    /**
     * Sync workout list to watch via DataClient.
     * Data persists even if watch is temporarily disconnected.
     */
    suspend fun syncWorkouts(workoutsJson: String) {
        try {
            val request = PutDataMapRequest.create(WearDataPaths.WORKOUTS_PATH).apply {
                dataMap.putString(WearDataPaths.KEY_PAYLOAD, workoutsJson)
                dataMap.putLong(WearDataPaths.KEY_TIMESTAMP, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Log.d(TAG, "Synced workouts to watch")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync workouts", e)
        }
    }

    /**
     * Sync day schedule to watch via DataClient.
     */
    suspend fun syncSchedule(scheduleJson: String) {
        try {
            val request = PutDataMapRequest.create(WearDataPaths.SCHEDULE_PATH).apply {
                dataMap.putString(WearDataPaths.KEY_PAYLOAD, scheduleJson)
                dataMap.putLong(WearDataPaths.KEY_TIMESTAMP, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Log.d(TAG, "Synced schedule to watch")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync schedule", e)
        }
    }

    /**
     * Sync readiness data to watch via DataClient.
     */
    suspend fun syncReadiness(readinessJson: String) {
        try {
            val request = PutDataMapRequest.create(WearDataPaths.READINESS_PATH).apply {
                dataMap.putString(WearDataPaths.KEY_PAYLOAD, readinessJson)
                dataMap.putLong(WearDataPaths.KEY_TIMESTAMP, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Log.d(TAG, "Synced readiness to watch")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync readiness", e)
        }
    }

    // =========================================================================
    // MessageClient listener
    // =========================================================================

    override fun onMessageReceived(event: MessageEvent) {
        val path = event.path
        val payload = String(event.data, Charsets.UTF_8)
        val type = mapPathToMessageType(path)

        if (type != null) {
            val message = WearMessage(
                id = UUID.randomUUID().toString(),
                type = type,
                payload = payload
            )
            scope.launch {
                _incomingMessages.emit(message)
            }
            Log.d(TAG, "Received message: $type from ${event.sourceNodeId}")
        } else {
            Log.w(TAG, "Unknown message path: $path")
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private fun registerMessageListener() {
        if (!isListenerRegistered) {
            messageClient.addListener(this)
            isListenerRegistered = true
        }
    }

    private fun unregisterMessageListener() {
        if (isListenerRegistered) {
            messageClient.removeListener(this)
            isListenerRegistered = false
        }
    }

    private fun mapMessageTypeToPath(type: MessageType): String {
        return when (type) {
            MessageType.WORKOUT_START -> WearDataPaths.MSG_WORKOUT_START
            MessageType.WORKOUT_PAUSE -> WearDataPaths.MSG_WORKOUT_PAUSE
            MessageType.WORKOUT_RESUME -> WearDataPaths.MSG_WORKOUT_RESUME
            MessageType.WORKOUT_COMPLETE -> WearDataPaths.MSG_WORKOUT_COMPLETE
            MessageType.WORKOUT_CANCEL -> WearDataPaths.MSG_WORKOUT_CANCEL
            MessageType.HEART_RATE_UPDATE -> WearDataPaths.MSG_HEART_RATE
            MessageType.WORKOUT_STATE_SYNC -> WearDataPaths.MSG_STATE_SYNC
            MessageType.EXERCISE_COMPLETE -> WearDataPaths.MSG_WORKOUT_COMPLETE
            MessageType.SET_COMPLETE -> WearDataPaths.MSG_STATE_SYNC
            MessageType.TIMER_SYNC -> WearDataPaths.MSG_STATE_SYNC
        }
    }

    private fun mapPathToMessageType(path: String): MessageType? {
        return when (path) {
            WearDataPaths.MSG_WORKOUT_START -> MessageType.WORKOUT_START
            WearDataPaths.MSG_WORKOUT_PAUSE -> MessageType.WORKOUT_PAUSE
            WearDataPaths.MSG_WORKOUT_RESUME -> MessageType.WORKOUT_RESUME
            WearDataPaths.MSG_WORKOUT_COMPLETE -> MessageType.WORKOUT_COMPLETE
            WearDataPaths.MSG_WORKOUT_CANCEL -> MessageType.WORKOUT_CANCEL
            WearDataPaths.MSG_HEART_RATE -> MessageType.HEART_RATE_UPDATE
            WearDataPaths.MSG_STATE_SYNC -> MessageType.WORKOUT_STATE_SYNC
            WearDataPaths.MSG_REQUEST_SYNC -> MessageType.WORKOUT_STATE_SYNC
            WearDataPaths.MSG_HEALTH_SNAPSHOT -> MessageType.HEART_RATE_UPDATE
            else -> null
        }
    }
}
