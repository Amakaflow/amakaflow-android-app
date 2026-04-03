package com.amakaflow.companion.ui.screens.coach

import com.amakaflow.companion.test.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Rule
import org.junit.Test

class CoachViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun mockSseResponse(body: String, statusCode: Int = 200): Pair<OkHttpClient, Call> {
        val fakeCall = mockk<Call>()
        val fakeRequest = Request.Builder().url("http://localhost/chat/stream").build()
        val fakeResponse = Response.Builder()
            .request(fakeRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(statusCode)
            .message(if (statusCode == 200) "OK" else "Error")
            .body(body.toResponseBody("text/event-stream".toMediaType()))
            .build()
        every { fakeCall.execute() } returns fakeResponse
        every { fakeCall.cancel() } returns Unit

        val mockClient = mockk<OkHttpClient>()
        every { mockClient.newCall(any()) } returns fakeCall
        return Pair(mockClient, fakeCall)
    }

    private fun createViewModel(client: OkHttpClient): CoachViewModel {
        return CoachViewModel(client).also { vm ->
            // testBaseUrl must be non-null so the ViewModel doesn't call AppEnvironment
            vm.testBaseUrl = "http://localhost"
        }
    }

    /** Suspend until isLoading becomes false (max 5s). */
    private suspend fun CoachViewModel.awaitSettled(): CoachUiState =
        withTimeout(5_000) { uiState.first { !it.isLoading } }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun `initial state has welcome message`() {
        val viewModel = CoachViewModel(mockk(relaxed = true))
        val state = viewModel.uiState.value
        assertThat(state.messages).hasSize(1)
        assertThat(state.messages[0].isUser).isFalse()
        assertThat(state.messages[0].content).contains("coach")
        assertThat(state.messages[0].suggestions).isNotEmpty()
    }

    @Test
    fun `sendMessage adds user message and coach response via SSE`() = runBlocking(Dispatchers.Default) {
        val sse = buildString {
            append("event: message_start\n")
            append("data: {\"session_id\":\"sess-123\"}\n\n")
            append("event: content_delta\n")
            append("data: {\"text\":\"You should do a light recovery run today.\"}\n\n")
            append("event: message_end\n")
            append("data: {}\n\n")
        }
        val (client, _) = mockSseResponse(sse)
        val viewModel = createViewModel(client)

        viewModel.sendMessage("What should I do today?")
        val state = viewModel.awaitSettled()

        // Welcome message + user message + coach response = 3
        assertThat(state.messages).hasSize(3)
        assertThat(state.messages[1].isUser).isTrue()
        assertThat(state.messages[1].content).isEqualTo("What should I do today?")
        assertThat(state.messages[2].isUser).isFalse()
        assertThat(state.messages[2].content).contains("recovery run")
        assertThat(state.sessionId).isEqualTo("sess-123")
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `sendMessage shows error on HTTP failure`() = runBlocking(Dispatchers.Default) {
        val (client, _) = mockSseResponse("{\"detail\":\"Internal error\"}", statusCode = 500)
        val viewModel = createViewModel(client)

        viewModel.sendMessage("Hello")
        val state = viewModel.awaitSettled()

        assertThat(state.error).isNotNull()
        assertThat(state.isLoading).isFalse()
        // User message is removed on HTTP failure
        assertThat(state.messages).hasSize(1) // welcome only
    }

    @Test
    fun `blank messages are ignored`() {
        val viewModel = CoachViewModel(mockk(relaxed = true))
        viewModel.sendMessage("")
        viewModel.sendMessage("   ")

        val state = viewModel.uiState.value
        assertThat(state.messages).hasSize(1) // Only welcome message
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `session ID is preserved across messages`() = runBlocking(Dispatchers.Default) {
        val sse1 = buildString {
            append("event: message_start\n")
            append("data: {\"session_id\":\"sess-abc\"}\n\n")
            append("event: content_delta\n")
            append("data: {\"text\":\"Response 1\"}\n\n")
            append("event: message_end\n")
            append("data: {\"session_id\":\"sess-abc\"}\n\n")
        }
        val sse2 = buildString {
            append("event: message_start\n")
            append("data: {\"session_id\":\"sess-abc\"}\n\n")
            append("event: content_delta\n")
            append("data: {\"text\":\"Response 2\"}\n\n")
            append("event: message_end\n")
            append("data: {\"session_id\":\"sess-abc\"}\n\n")
        }

        val fakeCall1 = mockk<Call>()
        val fakeCall2 = mockk<Call>()
        val fakeRequest = Request.Builder().url("http://localhost/chat/stream").build()

        every { fakeCall1.execute() } returns Response.Builder()
            .request(fakeRequest).protocol(Protocol.HTTP_1_1).code(200).message("OK")
            .body(sse1.toResponseBody("text/event-stream".toMediaType())).build()
        every { fakeCall1.cancel() } returns Unit

        every { fakeCall2.execute() } returns Response.Builder()
            .request(fakeRequest).protocol(Protocol.HTTP_1_1).code(200).message("OK")
            .body(sse2.toResponseBody("text/event-stream".toMediaType())).build()
        every { fakeCall2.cancel() } returns Unit

        val mockClient = mockk<OkHttpClient>()
        every { mockClient.newCall(any()) } returnsMany listOf(fakeCall1, fakeCall2)

        val viewModel = createViewModel(mockClient)
        viewModel.sendMessage("first")
        val state1 = viewModel.awaitSettled()
        assertThat(state1.sessionId).isEqualTo("sess-abc")

        viewModel.sendMessage("second")
        val state2 = viewModel.awaitSettled()
        assertThat(state2.messages).hasSize(5) // welcome + first + resp1 + second + resp2
    }
}
