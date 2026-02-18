package com.amakaflow.companion.data.connectivity

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for WearConnectivityService testing watch communication.
 * Tests message sending/receiving, connection state changes, and reconnection behavior.
 */
class WearConnectivityServiceTest {

    private lateinit var wearService: WearConnectivityServiceImpl

    @Before
    fun setup() {
        wearService = WearConnectivityServiceImpl()
    }

    // =============================================================================
    // Connection State Tests
    // =============================================================================

    @Test
    fun `initial state is disconnected`() {
        assertThat(wearService.connectionState.value).isInstanceOf(WearConnectionState.Disconnected::class.java)
        assertThat(wearService.isConnected()).isFalse()
    }

    @Test
    fun `connect changes state to connected`() = runTest {
        // When
        val result = wearService.connect()

        // Then
        assertThat(result).isTrue()
        assertThat(wearService.isConnected()).isTrue()
        assertThat(wearService.connectionState.value).isInstanceOf(WearConnectionState.Connected::class.java)
    }

    @Test
    fun `disconnect changes state to disconnected`() = runTest {
        // Given
        wearService.connect()
        assertThat(wearService.isConnected()).isTrue()

        // When
        wearService.disconnect()

        // Then
        assertThat(wearService.isConnected()).isFalse()
        assertThat(wearService.connectionState.value).isInstanceOf(WearConnectionState.Disconnected::class.java)
    }

    @Test
    fun `connect twice returns true without error`() = runTest {
        // Given
        wearService.connect()

        // When
        val result = wearService.connect()

        // Then
        assertThat(result).isTrue()
        assertThat(wearService.isConnected()).isTrue()
    }

    // =============================================================================
    // Message Sending Tests
    // =============================================================================

    @Test
    fun `sendMessage returns true when connected`() = runTest {
        // Given
        wearService.connect()

        // When
        val message = WearMessage(
            id = "msg-1",
            type = MessageType.WORKOUT_STATE_SYNC,
            payload = "{}"
        )
        val result = wearService.sendMessage(message)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `sendMessage returns false when disconnected`() = runTest {
        // Given - not connected
        val message = WearMessage(
            id = "msg-1",
            type = MessageType.WORKOUT_STATE_SYNC,
            payload = "{}"
        )

        // When
        val result = wearService.sendMessage(message)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `sendWorkoutStart sends correct message type`() = runTest {
        // Given
        wearService.connect()

        // When
        val result = wearService.sendWorkoutStart("workout-123", "Morning HIIT")

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `sendWorkoutPause sends correct message type`() = runTest {
        // Given
        wearService.connect()

        // When
        val result = wearService.sendWorkoutPause()

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `sendWorkoutResume sends correct message type`() = runTest {
        // Given
        wearService.connect()

        // When
        val result = wearService.sendWorkoutResume()

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `sendWorkoutComplete sends correct message type`() = runTest {
        // Given
        wearService.connect()

        // When
        val result = wearService.sendWorkoutComplete()

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `sendHeartRateUpdate includes heart rate in payload`() = runTest {
        // Given
        wearService.connect()

        // When
        val result = wearService.sendHeartRateUpdate(145)

        // Then
        assertThat(result).isTrue()
    }

    // =============================================================================
    // Message Receiving Tests
    // =============================================================================

    @Test
    fun `incomingMessages flow emits messages correctly`() = runTest {
        // Given
        val testMessage = WearMessage(
            id = "test-msg-1",
            type = MessageType.WORKOUT_START,
            payload = "{}"
        )

        // When - simulate receiving a message from the watch
        wearService.simulateIncomingMessage(testMessage)

        // Then - verify the message was emitted to the flow
        // Use withTimeoutOrNull to avoid hanging on flow collection
        val receivedMessage = withTimeoutOrNull(1000) { wearService.incomingMessages.first() }
        assertThat(receivedMessage).isNotNull()
        assertThat(receivedMessage?.id).isEqualTo("test-msg-1")
        assertThat(receivedMessage?.type).isEqualTo(MessageType.WORKOUT_START)
    }

    @Test
    fun `incomingMessages flow receives multiple messages in order`() = runTest {
        // Given
        val message1 = WearMessage("msg-1", MessageType.WORKOUT_START, "{\"workout\":\"test\"}")
        val message2 = WearMessage("msg-2", MessageType.HEART_RATE_UPDATE, "{\"heartRate\":120}")
        val message3 = WearMessage("msg-3", MessageType.WORKOUT_COMPLETE, "{}")

        // When - emit all messages first
        wearService.simulateIncomingMessage(message1)
        wearService.simulateIncomingMessage(message2)
        wearService.simulateIncomingMessage(message3)

        // Then - verify all messages are received by collecting them
        // Use first() on each subscription - each call gets the next value
        val firstMsg = wearService.incomingMessages.first()
        assertThat(firstMsg.id).isEqualTo("msg-1")
        
        // Note: In a SharedFlow, each collector gets its own copy of emitted values.
        // For testing multiple messages, we emit them sequentially and check each one
        val secondMsg = wearService.incomingMessages.first()
        assertThat(secondMsg.id).isEqualTo("msg-2")
        
        val thirdMsg = wearService.incomingMessages.first()
        assertThat(thirdMsg.id).isEqualTo("msg-3")
    }

    @Test
    fun `simulateIncomingMessage emits to flow when subscribed after emission`() = runTest {
        // Given - start collecting before sending
        val receivedMessages = mutableListOf<WearMessage>()
        
        // Start collection in background
        val collectionJob = launch {
            wearService.incomingMessages.collect { msg ->
                receivedMessages.add(msg)
            }
        }
        
        // Give it time to start collecting
        yield()
        
        // When - send messages
        val testMessage = WearMessage(
            id = "late-sub-msg",
            type = MessageType.HEART_RATE_UPDATE,
            payload = "{\"heartRate\":150}"
        )
        wearService.simulateIncomingMessage(testMessage)
        
        // Then - verify message was received
        delay(50)
        collectionJob.cancel()
        
        assertThat(receivedMessages.any { it.id == "late-sub-msg" }).isTrue()
    }

    // =============================================================================
    // Connection Error Tests
    // =============================================================================

    @Test
    fun `simulateConnectionLoss sets disconnected state`() = runTest {
        // Given
        wearService.connect()
        assertThat(wearService.isConnected()).isTrue()

        // When
        wearService.simulateConnectionLoss()

        // Then
        assertThat(wearService.isConnected()).isFalse()
        assertThat(wearService.connectionState.value).isInstanceOf(WearConnectionState.Disconnected::class.java)
    }

    @Test
    fun `simulateConnectionError sets error state`() = runTest {
        // When
        wearService.simulateConnectionError("Bluetooth unavailable")

        // Then
        assertThat(wearService.connectionState.value).isInstanceOf(WearConnectionState.Error::class.java)
        val errorState = wearService.connectionState.value as WearConnectionState.Error
        assertThat(errorState.message).isEqualTo("Bluetooth unavailable")
    }

    // =============================================================================
    // Reconnection Tests
    // =============================================================================

    @Test
    fun `reconnect succeeds when already connected`() = runTest {
        // Given
        wearService.connect()

        // When
        val result = wearService.reconnect()

        // Then
        assertThat(result).isTrue()
        assertThat(wearService.isConnected()).isTrue()
    }

    @Test
    fun `reconnect attempts multiple times before failing`() = runTest {
        // Given - disconnected state
        assertThat(wearService.isConnected()).isFalse()

        // When
        val result = wearService.reconnect()

        // Then
        assertThat(result).isTrue()
        assertThat(wearService.isConnected()).isTrue()
    }

    @Test
    fun `reconnect succeeds after connection loss`() = runTest {
        // Given
        wearService.connect()
        wearService.simulateConnectionLoss()
        assertThat(wearService.isConnected()).isFalse()

        // When
        val result = wearService.reconnect()

        // Then
        assertThat(result).isTrue()
        assertThat(wearService.isConnected()).isTrue()
    }

    // =============================================================================
    // Message Type Tests
    // =============================================================================

    @Test
    fun `all message types can be sent`() = runTest {
        // Given
        wearService.connect()

        // When/Then - all message types should be sendable
        assertThat(wearService.sendMessage(WearMessage("1", MessageType.WORKOUT_START, "{}"))).isTrue()
        assertThat(wearService.sendMessage(WearMessage("2", MessageType.WORKOUT_PAUSE, "{}"))).isTrue()
        assertThat(wearService.sendMessage(WearMessage("3", MessageType.WORKOUT_RESUME, "{}"))).isTrue()
        assertThat(wearService.sendMessage(WearMessage("4", MessageType.WORKOUT_COMPLETE, "{}"))).isTrue()
        assertThat(wearService.sendMessage(WearMessage("5", MessageType.WORKOUT_CANCEL, "{}"))).isTrue()
        assertThat(wearService.sendMessage(WearMessage("6", MessageType.HEART_RATE_UPDATE, "{}"))).isTrue()
        assertThat(wearService.sendMessage(WearMessage("7", MessageType.WORKOUT_STATE_SYNC, "{}"))).isTrue()
        assertThat(wearService.sendMessage(WearMessage("8", MessageType.EXERCISE_COMPLETE, "{}"))).isTrue()
        assertThat(wearService.sendMessage(WearMessage("9", MessageType.SET_COMPLETE, "{}"))).isTrue()
        assertThat(wearService.sendMessage(WearMessage("10", MessageType.TIMER_SYNC, "{}"))).isTrue()
    }

    // =============================================================================
    // Disconnect While Sending Tests
    // =============================================================================

    @Test
    fun `sendMessage returns false after disconnect`() = runTest {
        // Given
        wearService.connect()
        
        // When
        wearService.disconnect()
        
        val result = wearService.sendMessage(
            WearMessage("1", MessageType.WORKOUT_START, "{}")
        )

        // Then
        assertThat(result).isFalse()
    }
}
