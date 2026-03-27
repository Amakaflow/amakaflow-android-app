package com.amakaflow.companion.ui.screens.social

import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.model.DimensionLeaderboardEntry
import com.amakaflow.companion.data.model.DimensionLeaderboardResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {

    private lateinit var api: AmakaflowApi
    private lateinit var viewModel: LeaderboardViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        api = mock(AmakaflowApi::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): LeaderboardViewModel {
        viewModel = LeaderboardViewModel(api)
        return viewModel
    }

    private val sampleEntries = listOf(
        DimensionLeaderboardEntry(rank = 1, userId = "user-1", displayName = "Alex", avatarUrl = null, value = 15000.0, isMe = false),
        DimensionLeaderboardEntry(rank = 2, userId = "user-2", displayName = "You", avatarUrl = null, value = 12500.0, isMe = true),
        DimensionLeaderboardEntry(rank = 3, userId = "user-3", displayName = "Sam", avatarUrl = null, value = 8000.0, isMe = false),
    )

    private val sampleResponse = DimensionLeaderboardResponse(
        dimension = "volume",
        period = "month",
        entries = sampleEntries
    )

    @Test
    fun `initial load fetches friends leaderboard`() = runTest {
        `when`(api.getFriendsLeaderboard(anyString(), anyString()))
            .thenReturn(Response.success(sampleResponse))

        createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.entries.size)
        assertNull(state.error)
    }

    @Test
    fun `changeDimension updates state and reloads`() = runTest {
        `when`(api.getFriendsLeaderboard(anyString(), anyString()))
            .thenReturn(Response.success(sampleResponse))

        createViewModel()
        advanceUntilIdle()

        viewModel.changeDimension(LeaderboardDimension.CONSISTENCY)
        advanceUntilIdle()

        assertEquals(LeaderboardDimension.CONSISTENCY, viewModel.uiState.value.selectedDimension)
    }

    @Test
    fun `changePeriod updates state and reloads`() = runTest {
        `when`(api.getFriendsLeaderboard(anyString(), anyString()))
            .thenReturn(Response.success(sampleResponse))

        createViewModel()
        advanceUntilIdle()

        viewModel.changePeriod(LeaderboardPeriod.WEEK)
        advanceUntilIdle()

        assertEquals(LeaderboardPeriod.WEEK, viewModel.uiState.value.selectedPeriod)
    }

    @Test
    fun `changeScope to crew without crewId sets error`() = runTest {
        `when`(api.getFriendsLeaderboard(anyString(), anyString()))
            .thenReturn(Response.success(sampleResponse))

        createViewModel()
        advanceUntilIdle()

        viewModel.changeScope(LeaderboardScope.CREW)
        advanceUntilIdle()

        assertEquals("No crew selected", viewModel.uiState.value.error)
    }

    @Test
    fun `crew leaderboard works with crewId`() = runTest {
        val crewResponse = sampleResponse.copy(dimension = "workouts")
        `when`(api.getFriendsLeaderboard(anyString(), anyString()))
            .thenReturn(Response.success(sampleResponse))
        `when`(api.getCrewLeaderboard(anyString(), anyString(), anyString()))
            .thenReturn(Response.success(crewResponse))

        createViewModel()
        advanceUntilIdle()

        viewModel.crewId = "crew-001"
        viewModel.changeScope(LeaderboardScope.CREW)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `API failure sets error`() = runTest {
        `when`(api.getFriendsLeaderboard(anyString(), anyString()))
            .thenReturn(Response.error(500, "Server Error".toResponseBody()))

        createViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error!!.contains("500"))
    }

    @Test
    fun `network exception sets error`() = runTest {
        `when`(api.getFriendsLeaderboard(anyString(), anyString()))
            .thenThrow(RuntimeException("Network timeout"))

        createViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error!!.contains("Network timeout"))
    }

    @Test
    fun `formattedValue formats volume correctly`() = runTest {
        `when`(api.getFriendsLeaderboard(anyString(), anyString()))
            .thenReturn(Response.success(sampleResponse))

        createViewModel()
        advanceUntilIdle()

        val bigEntry = DimensionLeaderboardEntry(rank = 1, userId = "u1", displayName = "A", value = 15000.0)
        assertEquals("15.0k kg", viewModel.formattedValue(bigEntry))

        val smallEntry = DimensionLeaderboardEntry(rank = 1, userId = "u1", displayName = "A", value = 500.0)
        assertEquals("500 kg", viewModel.formattedValue(smallEntry))
    }

    @Test
    fun `formattedValue formats consistency correctly`() = runTest {
        `when`(api.getFriendsLeaderboard(anyString(), anyString()))
            .thenReturn(Response.success(sampleResponse))

        createViewModel()
        advanceUntilIdle()

        viewModel.changeDimension(LeaderboardDimension.CONSISTENCY)
        advanceUntilIdle()

        val entry = DimensionLeaderboardEntry(rank = 1, userId = "u1", displayName = "A", value = 5.0)
        assertEquals("5 weeks", viewModel.formattedValue(entry))

        val single = DimensionLeaderboardEntry(rank = 1, userId = "u1", displayName = "A", value = 1.0)
        assertEquals("1 week", viewModel.formattedValue(single))
    }

    @Test
    fun `formattedValue formats prs correctly`() = runTest {
        `when`(api.getFriendsLeaderboard(anyString(), anyString()))
            .thenReturn(Response.success(sampleResponse))

        createViewModel()
        advanceUntilIdle()

        viewModel.changeDimension(LeaderboardDimension.PRS)
        advanceUntilIdle()

        val entry = DimensionLeaderboardEntry(rank = 1, userId = "u1", displayName = "A", value = 7.0)
        assertEquals("7 PRs", viewModel.formattedValue(entry))
    }

    @Test
    fun `is_me flag is preserved in entries`() = runTest {
        `when`(api.getFriendsLeaderboard(anyString(), anyString()))
            .thenReturn(Response.success(sampleResponse))

        createViewModel()
        advanceUntilIdle()

        val meEntries = viewModel.uiState.value.entries.filter { it.isMe }
        assertEquals(1, meEntries.size)
        assertEquals("You", meEntries[0].displayName)
    }
}
