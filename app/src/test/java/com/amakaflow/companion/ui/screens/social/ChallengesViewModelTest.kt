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

class ChallengesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockApi: AmakaflowApi

    @Before
    fun setup() {
        mockApi = mockk(relaxed = true)
    }

    private fun makeChallenge(
        id: String = "1",
        title: String = "Volume Week",
        type: ChallengeType = ChallengeType.VOLUME,
        isJoined: Boolean = false
    ) = Challenge(
        id = id,
        title = title,
        type = type,
        status = ChallengeStatus.ACTIVE,
        target = 1000.0,
        targetUnit = "kg",
        startDate = "2026-04-01",
        endDate = "2026-04-07",
        creatorId = "creator-1",
        creatorName = "TestUser",
        participantCount = 5,
        isTeamMode = false,
        isJoined = isJoined
    )

    @Test
    fun `loads challenges successfully`() = runTest {
        val challenges = listOf(
            makeChallenge(id = "1", title = "Volume Week", type = ChallengeType.VOLUME),
            makeChallenge(id = "2", title = "Streak 7", type = ChallengeType.CONSISTENCY),
            makeChallenge(id = "3", title = "PR Hunt", type = ChallengeType.PR)
        )

        coEvery { mockApi.getChallenges() } returns Response.success(
            ChallengesResponse(challenges = challenges)
        )

        val viewModel = ChallengesViewModel(mockApi)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.challenges).hasSize(3)
            assertThat(state.filteredChallenges).hasSize(3)
            assertThat(state.error).isNull()
        }
    }

    @Test
    fun `shows error when challenges fail to load`() = runTest {
        coEvery { mockApi.getChallenges() } returns Response.error(
            500, "Server Error".toResponseBody()
        )

        val viewModel = ChallengesViewModel(mockApi)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.error).contains("Failed to load challenges")
            assertThat(state.challenges).isEmpty()
        }
    }

    @Test
    fun `filters by challenge type`() = runTest {
        val challenges = listOf(
            makeChallenge(id = "1", type = ChallengeType.VOLUME),
            makeChallenge(id = "2", type = ChallengeType.CONSISTENCY),
            makeChallenge(id = "3", type = ChallengeType.PR)
        )

        coEvery { mockApi.getChallenges() } returns Response.success(
            ChallengesResponse(challenges = challenges)
        )

        val viewModel = ChallengesViewModel(mockApi)

        viewModel.uiState.test {
            // Wait for initial load
            val loaded = expectMostRecentItem()
            assertThat(loaded.filteredChallenges).hasSize(3)

            // Filter by volume
            viewModel.setTypeFilter(ChallengeType.VOLUME)
            val filtered = expectMostRecentItem()
            assertThat(filtered.filteredChallenges).hasSize(1)
            assertThat(filtered.filteredChallenges[0].type).isEqualTo(ChallengeType.VOLUME)
            assertThat(filtered.selectedTypeFilter).isEqualTo(ChallengeType.VOLUME)

            // Clear filter
            viewModel.setTypeFilter(null)
            val all = expectMostRecentItem()
            assertThat(all.filteredChallenges).hasSize(3)
            assertThat(all.selectedTypeFilter).isNull()
        }
    }

    @Test
    fun `loads challenge detail`() = runTest {
        val challenge = makeChallenge(id = "1", title = "Test Challenge")
        val leaderboard = listOf(
            LeaderboardEntry(id = "le-1", rank = 1, userId = "u1", userName = "Alice", progress = 800.0, progressPercentage = 80.0),
            LeaderboardEntry(id = "le-2", rank = 2, userId = "u2", userName = "Bob", progress = 500.0, progressPercentage = 50.0)
        )
        val progress = ChallengeProgress(
            challengeId = "1",
            currentValue = 700.0,
            targetValue = 1000.0,
            percentage = 70.0,
            isCompleted = false
        )

        coEvery { mockApi.getChallenges() } returns Response.success(ChallengesResponse())
        coEvery { mockApi.getChallengeDetail("1") } returns Response.success(
            ChallengeDetailResponse(challenge = challenge, leaderboard = leaderboard, myProgress = progress)
        )

        val viewModel = ChallengesViewModel(mockApi)
        viewModel.loadChallengeDetail("1")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.selectedChallenge).isNotNull()
            assertThat(state.selectedChallenge!!.leaderboard).hasSize(2)
            assertThat(state.selectedChallenge!!.myProgress?.percentage).isEqualTo(70.0)
            assertThat(state.isLoadingDetail).isFalse()
        }
    }

    @Test
    fun `completion triggers celebration`() = runTest {
        val challenge = makeChallenge(id = "1", title = "Done!")
        val badge = ChallengeBadge(id = "b1", name = "PR Hunter", iconName = "trophy", description = "Completed a PR challenge")
        val progress = ChallengeProgress(
            challengeId = "1",
            currentValue = 100.0,
            targetValue = 100.0,
            percentage = 100.0,
            isCompleted = true,
            completedAt = "2026-04-07T00:00:00Z",
            badge = badge
        )

        coEvery { mockApi.getChallenges() } returns Response.success(ChallengesResponse())
        coEvery { mockApi.getChallengeDetail("1") } returns Response.success(
            ChallengeDetailResponse(challenge = challenge, leaderboard = emptyList(), myProgress = progress)
        )

        val viewModel = ChallengesViewModel(mockApi)
        viewModel.loadChallengeDetail("1")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.showCelebration).isTrue()
            assertThat(state.completedBadge?.name).isEqualTo("PR Hunter")
        }
    }

    @Test
    fun `dismiss celebration clears state`() = runTest {
        coEvery { mockApi.getChallenges() } returns Response.success(ChallengesResponse())

        val viewModel = ChallengesViewModel(mockApi)

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.dismissCelebration()
            val state = expectMostRecentItem()
            assertThat(state.showCelebration).isFalse()
            assertThat(state.completedBadge).isNull()
        }
    }

    @Test
    fun `join challenge calls API`() = runTest {
        val challenge = makeChallenge(id = "1")

        coEvery { mockApi.getChallenges() } returns Response.success(ChallengesResponse(challenges = listOf(challenge)))
        coEvery { mockApi.joinChallenge("1") } returns Response.success(Unit)
        coEvery { mockApi.getChallengeDetail("1") } returns Response.success(
            ChallengeDetailResponse(challenge = challenge.copy(isJoined = true), leaderboard = emptyList())
        )

        val viewModel = ChallengesViewModel(mockApi)
        viewModel.joinChallenge("1")

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isJoining).isFalse()
        }
        coVerify { mockApi.joinChallenge("1") }
    }

    @Test
    fun `create challenge calls API and reloads`() = runTest {
        coEvery { mockApi.getChallenges() } returns Response.success(ChallengesResponse())
        coEvery { mockApi.createChallenge(any()) } returns Response.success(Unit)

        val viewModel = ChallengesViewModel(mockApi)
        var callbackCalled = false

        viewModel.createChallenge(
            CreateChallengeRequest(
                title = "New Challenge",
                type = ChallengeType.VOLUME,
                target = 5000.0,
                targetUnit = "kg",
                startDate = "2026-04-01",
                endDate = "2026-04-07"
            ),
            onSuccess = { callbackCalled = true }
        )

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state.isCreating).isFalse()
            assertThat(state.createError).isNull()
        }
        coVerify { mockApi.createChallenge(any()) }
        assertThat(callbackCalled).isTrue()
    }
}
