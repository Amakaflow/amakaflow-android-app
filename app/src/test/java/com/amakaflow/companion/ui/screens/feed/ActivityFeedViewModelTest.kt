package com.amakaflow.companion.ui.screens.feed

import app.cash.turbine.test
import com.amakaflow.companion.data.model.ActivityFeedItem
import com.amakaflow.companion.data.model.ActivityFeedResponse
import com.amakaflow.companion.data.model.ActivityType
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.PlannerRepository
import com.amakaflow.companion.test.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ActivityFeedViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockPlannerRepository: PlannerRepository

    @Before
    fun setup() {
        mockPlannerRepository = mockk(relaxed = true)
    }

    @Test
    fun `loads activity feed successfully`() = runTest {
        val items = listOf(
            ActivityFeedItem(
                id = "1",
                type = ActivityType.WORKOUT_COMPLETED,
                title = "Completed HIIT Blast",
                subtitle = "30 min - 320 cal",
                timestamp = "2 hours ago"
            ),
            ActivityFeedItem(
                id = "2",
                type = ActivityType.STREAK_MILESTONE,
                title = "7-day streak!",
                timestamp = "Yesterday"
            )
        )

        every { mockPlannerRepository.getActivityFeed(any(), any()) } returns flowOf(
            Result.Success(
                ActivityFeedResponse(
                    success = true,
                    items = items,
                    hasMore = true,
                    nextCursor = "cursor-abc"
                )
            )
        )

        val viewModel = ActivityFeedViewModel(mockPlannerRepository)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.items).hasSize(2)
            assertThat(state.items[0].title).isEqualTo("Completed HIIT Blast")
            assertThat(state.hasMore).isTrue()
            assertThat(state.nextCursor).isEqualTo("cursor-abc")
        }
    }

    @Test
    fun `shows error when feed fails to load`() = runTest {
        every { mockPlannerRepository.getActivityFeed(any(), any()) } returns flowOf(
            Result.Error("Failed to load")
        )

        val viewModel = ActivityFeedViewModel(mockPlannerRepository)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.error).isEqualTo("Failed to load")
            assertThat(state.items).isEmpty()
        }
    }

    @Test
    fun `loadMore appends items`() = runTest {
        val firstPage = ActivityFeedResponse(
            success = true,
            items = listOf(
                ActivityFeedItem(id = "1", type = ActivityType.WORKOUT_COMPLETED, title = "Item 1", timestamp = "now")
            ),
            hasMore = true,
            nextCursor = "cursor-1"
        )

        val secondPage = ActivityFeedResponse(
            success = true,
            items = listOf(
                ActivityFeedItem(id = "2", type = ActivityType.PR_ACHIEVED, title = "Item 2", timestamp = "now")
            ),
            hasMore = false,
            nextCursor = null
        )

        every { mockPlannerRepository.getActivityFeed(any(), isNull()) } returns flowOf(
            Result.Success(firstPage)
        )
        every { mockPlannerRepository.getActivityFeed(any(), eq("cursor-1")) } returns flowOf(
            Result.Success(secondPage)
        )

        val viewModel = ActivityFeedViewModel(mockPlannerRepository)

        // Wait for first page
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.items).hasSize(1)
        }

        viewModel.loadMore()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.items).hasSize(2)
            assertThat(state.hasMore).isFalse()
        }
    }
}
