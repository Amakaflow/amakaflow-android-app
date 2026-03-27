package com.amakaflow.companion.ui.screens.social

import app.cash.turbine.test
import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.model.*
import com.amakaflow.companion.test.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class FeedViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockApi: AmakaflowApi

    @Before
    fun setup() {
        mockApi = mockk(relaxed = true)
    }

    private fun makePost(
        id: String = "1",
        userName: String = "Alice",
        workoutName: String = "Push Day",
        reactions: List<SocialFeedReaction> = emptyList(),
        userReactions: List<String> = emptyList()
    ) = SocialFeedPost(
        id = id,
        userId = "user-$id",
        userName = userName,
        postedAt = "2h ago",
        workoutName = workoutName,
        exercises = listOf(SocialFeedExercise(name = "Bench Press", sets = 3, reps = 10, weight = 80.0)),
        totalVolume = 2400.0,
        durationSeconds = 3600,
        reactions = reactions,
        commentCount = 0,
        userReactions = userReactions
    )

    @Test
    fun `loads feed successfully`() = runTest {
        val posts = listOf(
            makePost(id = "1", userName = "Alice"),
            makePost(id = "2", userName = "Bob", workoutName = "Leg Day")
        )

        coEvery { mockApi.getSocialFeed(any(), any()) } returns Response.success(
            SocialFeedResponse(posts = posts, nextCursor = "cursor-abc", hasMore = true)
        )

        val viewModel = FeedViewModel(mockApi)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.posts).hasSize(2)
            assertThat(state.posts[0].userName).isEqualTo("Alice")
            assertThat(state.hasMore).isTrue()
            assertThat(state.nextCursor).isEqualTo("cursor-abc")
        }
    }

    @Test
    fun `shows error when feed fails to load`() = runTest {
        coEvery { mockApi.getSocialFeed(any(), any()) } returns Response.error(
            500, "Server Error".toResponseBody()
        )

        val viewModel = FeedViewModel(mockApi)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.error).isNotNull()
            assertThat(state.posts).isEmpty()
        }
    }

    @Test
    fun `loadMore appends posts`() = runTest {
        val firstPage = SocialFeedResponse(
            posts = listOf(makePost(id = "1")),
            nextCursor = "cursor-1",
            hasMore = true
        )
        val secondPage = SocialFeedResponse(
            posts = listOf(makePost(id = "2", userName = "Bob")),
            nextCursor = null,
            hasMore = false
        )

        coEvery { mockApi.getSocialFeed(isNull(), any()) } returns Response.success(firstPage)
        coEvery { mockApi.getSocialFeed(eq("cursor-1"), any()) } returns Response.success(secondPage)

        val viewModel = FeedViewModel(mockApi)

        viewModel.uiState.test {
            val initialState = expectMostRecentItem()
            assertThat(initialState.posts).hasSize(1)
        }

        viewModel.loadMore()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.posts).hasSize(2)
            assertThat(state.hasMore).isFalse()
        }
    }

    @Test
    fun `toggleReaction adds reaction optimistically`() = runTest {
        val posts = listOf(makePost(id = "1"))
        coEvery { mockApi.getSocialFeed(any(), any()) } returns Response.success(
            SocialFeedResponse(posts = posts, hasMore = false)
        )
        coEvery { mockApi.addSocialReaction(any(), any()) } returns Response.success(Unit)

        val viewModel = FeedViewModel(mockApi)

        viewModel.uiState.test {
            expectMostRecentItem()
        }

        viewModel.toggleReaction("1", "heart")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.posts[0].userReactions).contains("heart")
            assertThat(state.posts[0].reactions.find { it.emoji == "heart" }?.count).isEqualTo(1)
        }

        coVerify { mockApi.addSocialReaction("1", ReactRequest("heart")) }
    }

    @Test
    fun `toggleReaction removes reaction optimistically`() = runTest {
        val posts = listOf(
            makePost(
                id = "1",
                reactions = listOf(SocialFeedReaction("heart", 1)),
                userReactions = listOf("heart")
            )
        )
        coEvery { mockApi.getSocialFeed(any(), any()) } returns Response.success(
            SocialFeedResponse(posts = posts, hasMore = false)
        )
        coEvery { mockApi.removeSocialReaction(any(), any()) } returns Response.success(Unit)

        val viewModel = FeedViewModel(mockApi)

        viewModel.uiState.test {
            expectMostRecentItem()
        }

        viewModel.toggleReaction("1", "heart")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.posts[0].userReactions).doesNotContain("heart")
        }

        coVerify { mockApi.removeSocialReaction("1", "heart") }
    }

    @Test
    fun `postComment calls API and increments count`() = runTest {
        val posts = listOf(makePost(id = "1"))
        coEvery { mockApi.getSocialFeed(any(), any()) } returns Response.success(
            SocialFeedResponse(posts = posts, hasMore = false)
        )
        coEvery { mockApi.postSocialComment(any(), any()) } returns Response.success(Unit)
        coEvery { mockApi.getSocialComments(any()) } returns Response.success(
            SocialCommentsResponse(comments = listOf(
                SocialComment("c1", "user-1", "Alice", null, "Nice!", "just now")
            ))
        )

        val viewModel = FeedViewModel(mockApi)

        viewModel.uiState.test {
            expectMostRecentItem()
        }

        viewModel.loadComments("1")
        viewModel.postComment("Great workout!")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.posts[0].commentCount).isEqualTo(1)
            assertThat(state.isPostingComment).isFalse()
        }

        coVerify { mockApi.postSocialComment("1", CommentRequest("Great workout!")) }
    }

    @Test
    fun `postComment with blank text does nothing`() = runTest {
        val posts = listOf(makePost(id = "1"))
        coEvery { mockApi.getSocialFeed(any(), any()) } returns Response.success(
            SocialFeedResponse(posts = posts, hasMore = false)
        )

        val viewModel = FeedViewModel(mockApi)

        viewModel.uiState.test {
            expectMostRecentItem()
        }

        viewModel.loadComments("1")
        viewModel.postComment("   ")

        coVerify(exactly = 0) { mockApi.postSocialComment(any(), any()) }
    }

    @Test
    fun `refresh replaces posts`() = runTest {
        val firstLoad = SocialFeedResponse(
            posts = listOf(makePost(id = "1")),
            hasMore = false
        )
        val refreshed = SocialFeedResponse(
            posts = listOf(makePost(id = "1"), makePost(id = "2")),
            hasMore = false
        )

        coEvery { mockApi.getSocialFeed(isNull(), any()) } returnsMany listOf(
            Response.success(firstLoad),
            Response.success(refreshed)
        )

        val viewModel = FeedViewModel(mockApi)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.posts).hasSize(1)
        }

        viewModel.refresh()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.posts).hasSize(2)
            assertThat(state.isRefreshing).isFalse()
        }
    }
}
