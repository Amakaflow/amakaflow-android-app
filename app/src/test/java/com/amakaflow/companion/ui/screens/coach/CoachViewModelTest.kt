package com.amakaflow.companion.ui.screens.coach

import app.cash.turbine.test
import com.amakaflow.companion.data.model.CoachMessageResponse
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.PlannerRepository
import com.amakaflow.companion.test.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CoachViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockPlannerRepository: PlannerRepository

    @Before
    fun setup() {
        mockPlannerRepository = mockk(relaxed = true)
    }

    private fun createViewModel(): CoachViewModel {
        return CoachViewModel(mockPlannerRepository)
    }

    @Test
    fun `initial state has welcome message`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.messages).hasSize(1)
            assertThat(state.messages[0].isUser).isFalse()
            assertThat(state.messages[0].content).contains("coach")
            assertThat(state.messages[0].suggestions).isNotEmpty()
        }
    }

    @Test
    fun `sendMessage adds user message and coach response`() = runTest {
        val coachResponse = CoachMessageResponse(
            success = true,
            reply = "You should do a light recovery run today.",
            conversationId = "conv-123",
            suggestions = listOf("What about tomorrow?")
        )

        every { mockPlannerRepository.sendCoachMessage(any(), any()) } returns flowOf(
            Result.Success(coachResponse)
        )

        val viewModel = createViewModel()
        viewModel.sendMessage("What should I do today?")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            // Welcome message + user message + coach response = 3
            assertThat(state.messages).hasSize(3)
            assertThat(state.messages[1].isUser).isTrue()
            assertThat(state.messages[1].content).isEqualTo("What should I do today?")
            assertThat(state.messages[2].isUser).isFalse()
            assertThat(state.messages[2].content).contains("recovery run")
            assertThat(state.conversationId).isEqualTo("conv-123")
            assertThat(state.isLoading).isFalse()
        }

        verify { mockPlannerRepository.sendCoachMessage("What should I do today?", null) }
    }

    @Test
    fun `sendMessage shows error on failure`() = runTest {
        every { mockPlannerRepository.sendCoachMessage(any(), any()) } returns flowOf(
            Result.Error("Network error")
        )

        val viewModel = createViewModel()
        viewModel.sendMessage("Hello")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.error).isEqualTo("Network error")
            assertThat(state.isLoading).isFalse()
            // User message is still added
            assertThat(state.messages).hasSize(2) // welcome + user
        }
    }

    @Test
    fun `blank messages are ignored`() = runTest {
        val viewModel = createViewModel()
        viewModel.sendMessage("")
        viewModel.sendMessage("   ")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.messages).hasSize(1) // Only welcome message
        }
    }

    @Test
    fun `conversation ID is preserved across messages`() = runTest {
        every { mockPlannerRepository.sendCoachMessage(eq("first"), any()) } returns flowOf(
            Result.Success(
                CoachMessageResponse(
                    success = true,
                    reply = "Response 1",
                    conversationId = "conv-abc"
                )
            )
        )

        every { mockPlannerRepository.sendCoachMessage(eq("second"), eq("conv-abc")) } returns flowOf(
            Result.Success(
                CoachMessageResponse(
                    success = true,
                    reply = "Response 2",
                    conversationId = "conv-abc"
                )
            )
        )

        val viewModel = createViewModel()
        viewModel.sendMessage("first")

        // Wait for first response
        viewModel.uiState.test {
            val state1 = expectMostRecentItem()
            assertThat(state1.conversationId).isEqualTo("conv-abc")
        }

        viewModel.sendMessage("second")

        viewModel.uiState.test {
            val state2 = expectMostRecentItem()
            assertThat(state2.messages).hasSize(5) // welcome + first + resp1 + second + resp2
        }

        verify { mockPlannerRepository.sendCoachMessage("second", "conv-abc") }
    }
}
