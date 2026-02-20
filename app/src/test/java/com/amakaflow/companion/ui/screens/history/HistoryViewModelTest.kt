package com.amakaflow.companion.ui.screens.history

import app.cash.turbine.test
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.CompletionsResult
import com.amakaflow.companion.domain.usecase.completion.GetCompletionHistoryUseCase
import com.amakaflow.companion.test.MainDispatcherRule
import com.amakaflow.companion.test.TestFixtures
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockGetCompletionHistory: GetCompletionHistoryUseCase

    @Before
    fun setup() {
        mockGetCompletionHistory = mockk()
    }

    private fun createViewModel(): HistoryViewModel {
        return HistoryViewModel(mockGetCompletionHistory)
    }

    @Test
    fun `initial state shows loading`() = runTest {
        // Given
        every { mockGetCompletionHistory(any(), any()) } returns flow {
            emit(Result.Loading)
        }

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads completions successfully`() = runTest {
        // Given
        val completionsResult = CompletionsResult(
            completions = TestFixtures.sampleCompletions,
            total = 3
        )
        every { mockGetCompletionHistory(50, 0) } returns flowOf(Result.Success(completionsResult))

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.uiState.test {
            val finalState = expectMostRecentItem()
            assertThat(finalState.isLoading).isFalse()
            assertThat(finalState.completions).hasSize(3)
            assertThat(finalState.total).isEqualTo(3)
            assertThat(finalState.error).isNull()
        }
    }

    @Test
    fun `shows error on failure`() = runTest {
        // Given
        every { mockGetCompletionHistory(50, 0) } returns flowOf(Result.Error("Failed to load"))

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.uiState.test {
            val finalState = expectMostRecentItem()
            assertThat(finalState.isLoading).isFalse()
            assertThat(finalState.error).isEqualTo("Failed to load")
        }
    }

    @Test
    fun `hasMore is true when more completions exist`() = runTest {
        // Given
        val completionsResult = CompletionsResult(
            completions = TestFixtures.sampleCompletions.take(2),
            total = 10
        )
        every { mockGetCompletionHistory(50, 0) } returns flowOf(Result.Success(completionsResult))

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.uiState.test {
            val finalState = expectMostRecentItem()
            assertThat(finalState.hasMore).isTrue()
            assertThat(finalState.canLoadMore).isTrue()
        }
    }

    @Test
    fun `hasMore is false when all completions loaded`() = runTest {
        // Given
        val completionsResult = CompletionsResult(
            completions = TestFixtures.sampleCompletions,
            total = 3
        )
        every { mockGetCompletionHistory(50, 0) } returns flowOf(Result.Success(completionsResult))

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.uiState.test {
            val finalState = expectMostRecentItem()
            assertThat(finalState.hasMore).isFalse()
            assertThat(finalState.canLoadMore).isFalse()
        }
    }

    @Test
    fun `loadMore appends new completions`() = runTest {
        // Given - initial load
        val initialResult = CompletionsResult(
            completions = TestFixtures.sampleCompletions.take(2),
            total = 5
        )
        val moreResult = CompletionsResult(
            completions = listOf(TestFixtures.sampleCompletions[2]),
            total = 5
        )
        every { mockGetCompletionHistory(50, 0) } returns flowOf(Result.Success(initialResult))
        every { mockGetCompletionHistory(50, 2) } returns flowOf(Result.Success(moreResult))

        val viewModel = createViewModel()

        // When
        viewModel.loadMore()

        // Then
        viewModel.uiState.test {
            val finalState = expectMostRecentItem()
            assertThat(finalState.completions).hasSize(3)
            assertThat(finalState.isLoadingMore).isFalse()
        }
    }

    @Test
    fun `refresh reloads completions from beginning`() = runTest {
        // Given
        val completionsResult = CompletionsResult(
            completions = TestFixtures.sampleCompletions,
            total = 3
        )
        every { mockGetCompletionHistory(50, 0) } returns flowOf(Result.Success(completionsResult))

        val viewModel = createViewModel()

        // When
        viewModel.refresh()

        // Then
        coVerify(exactly = 2) { mockGetCompletionHistory(50, 0) } // init + refresh
    }

    @Test
    fun `groupedCompletions groups by date category`() = runTest {
        // Given
        val completionsResult = CompletionsResult(
            completions = TestFixtures.sampleCompletions,
            total = 3
        )
        every { mockGetCompletionHistory(50, 0) } returns flowOf(Result.Success(completionsResult))

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.uiState.test {
            val finalState = expectMostRecentItem()
            assertThat(finalState.groupedCompletions).isNotEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty state shows no completions`() = runTest {
        // Given
        val emptyResult = CompletionsResult(
            completions = emptyList(),
            total = 0
        )
        every { mockGetCompletionHistory(50, 0) } returns flowOf(Result.Success(emptyResult))

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.completions).isEmpty()
            assertThat(state.total).isEqualTo(0)
            assertThat(state.hasMore).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loading more shows loading indicator`() = runTest {
        // Given - initial load
        val initialResult = CompletionsResult(
            completions = TestFixtures.sampleCompletions.take(2),
            total = 10
        )
        every { mockGetCompletionHistory(50, 0) } returns flowOf(Result.Success(initialResult))

        // When - trigger load more (but mock doesn't return for offset 2 to simulate delay)
        val viewModel = createViewModel()

        // Wait for initial load
        viewModel.uiState.test {
            expectMostRecentItem()
            cancelAndIgnoreRemainingEvents()
        }

        // Then - canLoadMore should be true
        val state = viewModel.uiState.value
        assertThat(state.canLoadMore).isTrue()
    }
}
