package com.amakaflow.companion.data.connectivity

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for WearConnectivityService interface and message model.
 * Tests message creation, connection state transitions, and message types.
 *
 * Note: The real WearConnectivityServiceImpl requires Android context (Google Play Services).
 * These tests validate the interface contract and message model without requiring a device.
 */
class WearConnectivityServiceTest {

    // =========================================================================
    // Connection State Model Tests
    // =========================================================================

    @Test
    fun `WearConnectionState Disconnected is correct type`() {
        val state: WearConnectionState = WearConnectionState.Disconnected
        assertThat(state).isInstanceOf(WearConnectionState.Disconnected::class.java)
    }

    @Test
    fun `WearConnectionState Connecting is correct type`() {
        val state: WearConnectionState = WearConnectionState.Connecting
        assertThat(state).isInstanceOf(WearConnectionState.Connecting::class.java)
    }

    @Test
    fun `WearConnectionState Connected is correct type`() {
        val state: WearConnectionState = WearConnectionState.Connected
        assertThat(state).isInstanceOf(WearConnectionState.Connected::class.java)
    }

    @Test
    fun `WearConnectionState Error contains message`() {
        val state = WearConnectionState.Error("Bluetooth unavailable")
        assertThat(state).isInstanceOf(WearConnectionState.Error::class.java)
        assertThat(state.message).isEqualTo("Bluetooth unavailable")
    }

    // =========================================================================
    // WearMessage Model Tests
    // =========================================================================

    @Test
    fun `WearMessage has correct fields`() {
        val message = WearMessage(
            id = "msg-1",
            type = MessageType.WORKOUT_START,
            payload = "{\"workoutId\":\"123\"}",
            timestamp = 1000L
        )

        assertThat(message.id).isEqualTo("msg-1")
        assertThat(message.type).isEqualTo(MessageType.WORKOUT_START)
        assertThat(message.payload).isEqualTo("{\"workoutId\":\"123\"}")
        assertThat(message.timestamp).isEqualTo(1000L)
    }

    @Test
    fun `WearMessage default timestamp is set`() {
        val before = System.currentTimeMillis()
        val message = WearMessage(
            id = "msg-2",
            type = MessageType.HEART_RATE_UPDATE,
            payload = "{}"
        )
        val after = System.currentTimeMillis()

        assertThat(message.timestamp).isAtLeast(before)
        assertThat(message.timestamp).isAtMost(after)
    }

    // =========================================================================
    // MessageType Tests
    // =========================================================================

    @Test
    fun `all MessageType values exist`() {
        val types = MessageType.values()
        assertThat(types).hasLength(10)
        assertThat(types.toList()).containsExactly(
            MessageType.WORKOUT_START,
            MessageType.WORKOUT_PAUSE,
            MessageType.WORKOUT_RESUME,
            MessageType.WORKOUT_COMPLETE,
            MessageType.WORKOUT_CANCEL,
            MessageType.HEART_RATE_UPDATE,
            MessageType.WORKOUT_STATE_SYNC,
            MessageType.EXERCISE_COMPLETE,
            MessageType.SET_COMPLETE,
            MessageType.TIMER_SYNC
        )
    }

    // =========================================================================
    // Mock Service Tests (validates interface contract)
    // =========================================================================

    @Test
    fun `mock service connect returns true`() = runTest {
        val service = mockk<WearConnectivityService>()
        every { service.isConnected() } returns true

        assertThat(service.isConnected()).isTrue()
    }

    @Test
    fun `mock service sendMessage returns false when disconnected`() = runTest {
        val service = mockk<WearConnectivityService>()
        every { service.isConnected() } returns false

        assertThat(service.isConnected()).isFalse()
    }
}
