package com.amakaflow.companion.data.repository

import app.cash.turbine.test
import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.model.*
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.test.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class PlannerRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var mockApi: AmakaflowApi
    private lateinit var repository: PlannerRepositoryImpl

    @Before
    fun setup() {
        mockApi = mockk()
        repository = PlannerRepositoryImpl(mockApi)
    }

    // ---- DayState ----

    @Test
    fun `getDayState returns success`() = runTest {
        val dayState = DayState(
            date = "2026-03-21",
            status = DayStatus.MODERATE,
            workouts = listOf(
                DayWorkoutSummary(id = "w1", name = "Run", sport = WorkoutSport.RUNNING, duration = 1800)
            )
        )

        coEvery { mockApi.getDayState("2026-03-21") } returns Response.success(
            DayStateResponse(success = true, dayState = dayState)
        )

        repository.getDayState("2026-03-21").test {
            assertThat(awaitItem()).isInstanceOf(Result.Loading::class.java)
            val success = awaitItem() as Result.Success
            assertThat(success.data.status).isEqualTo(DayStatus.MODERATE)
            assertThat(success.data.workouts).hasSize(1)
            awaitComplete()
        }
    }

    @Test
    fun `getDayState returns error on API failure`() = runTest {
        coEvery { mockApi.getDayState(any()) } returns Response.error(
            500, "Server error".toResponseBody()
        )

        repository.getDayState("2026-03-21").test {
            assertThat(awaitItem()).isInstanceOf(Result.Loading::class.java)
            val error = awaitItem() as Result.Error
            assertThat(error.message).contains("500")
            awaitComplete()
        }
    }

    @Test
    fun `getDayState returns error on exception`() = runTest {
        coEvery { mockApi.getDayState(any()) } throws RuntimeException("Network down")

        repository.getDayState("2026-03-21").test {
            assertThat(awaitItem()).isInstanceOf(Result.Loading::class.java)
            val error = awaitItem() as Result.Error
            assertThat(error.message).isEqualTo("Network down")
            awaitComplete()
        }
    }

    // ---- Generate Week ----

    @Test
    fun `generateWeek returns success`() = runTest {
        val plan = WeekPlan(
            startDate = "2026-03-21",
            endDate = "2026-03-27",
            days = listOf(
                PlannedDay(date = "2026-03-21", isRestDay = false),
                PlannedDay(date = "2026-03-22", isRestDay = true)
            ),
            summary = "Balanced week"
        )

        coEvery { mockApi.generateWeek(any()) } returns Response.success(
            GenerateWeekResponse(success = true, plan = plan)
        )

        repository.generateWeek("2026-03-21", "Get fit").test {
            assertThat(awaitItem()).isInstanceOf(Result.Loading::class.java)
            val success = awaitItem() as Result.Success
            assertThat(success.data.summary).isEqualTo("Balanced week")
            assertThat(success.data.days).hasSize(2)
            awaitComplete()
        }
    }

    @Test
    fun `generateWeek returns error when plan is null`() = runTest {
        coEvery { mockApi.generateWeek(any()) } returns Response.success(
            GenerateWeekResponse(success = false, plan = null, message = "Insufficient data")
        )

        repository.generateWeek("2026-03-21").test {
            assertThat(awaitItem()).isInstanceOf(Result.Loading::class.java)
            val error = awaitItem() as Result.Error
            assertThat(error.message).isEqualTo("Insufficient data")
            awaitComplete()
        }
    }

    // ---- Coach Message ----

    @Test
    fun `sendCoachMessage returns success`() = runTest {
        val response = CoachMessageResponse(
            success = true,
            reply = "Do a light run",
            conversationId = "conv-1",
            suggestions = listOf("What about strength?")
        )

        coEvery { mockApi.sendCoachMessage(any()) } returns Response.success(response)

        repository.sendCoachMessage("What should I do?").test {
            assertThat(awaitItem()).isInstanceOf(Result.Loading::class.java)
            val success = awaitItem() as Result.Success
            assertThat(success.data.reply).isEqualTo("Do a light run")
            assertThat(success.data.conversationId).isEqualTo("conv-1")
            awaitComplete()
        }
    }

    // ---- Training Preferences ----

    @Test
    fun `getTrainingPreferences returns success`() = runTest {
        val prefs = TrainingPreferences(daysPerWeek = 5, fitnessLevel = "advanced")

        coEvery { mockApi.getTrainingPreferences() } returns Response.success(
            TrainingPreferencesResponse(success = true, preferences = prefs)
        )

        repository.getTrainingPreferences().test {
            assertThat(awaitItem()).isInstanceOf(Result.Loading::class.java)
            val success = awaitItem() as Result.Success
            assertThat(success.data.daysPerWeek).isEqualTo(5)
            assertThat(success.data.fitnessLevel).isEqualTo("advanced")
            awaitComplete()
        }
    }

    @Test
    fun `updateTrainingPreferences returns success`() = runTest {
        val prefs = TrainingPreferences(daysPerWeek = 6)

        coEvery { mockApi.updateTrainingPreferences(any()) } returns Response.success(
            TrainingPreferencesResponse(success = true, preferences = prefs)
        )

        val result = repository.updateTrainingPreferences(prefs)
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data.daysPerWeek).isEqualTo(6)
    }

    @Test
    fun `updateTrainingPreferences returns error on failure`() = runTest {
        coEvery { mockApi.updateTrainingPreferences(any()) } returns Response.error(
            400, "Bad request".toResponseBody()
        )

        val result = repository.updateTrainingPreferences(TrainingPreferences())
        assertThat(result).isInstanceOf(Result.Error::class.java)
    }

    // ---- Fatigue Advisor ----

    @Test
    fun `getFatigueAdvice returns success`() = runTest {
        val response = FatigueAdvisorResponse(
            success = true,
            fatigueScore = 45.0,
            level = FatigueLevel.MODERATE,
            advice = "Take it easy",
            recommendations = listOf("Rest", "Stretch")
        )

        coEvery { mockApi.getFatigueAdvice() } returns Response.success(response)

        repository.getFatigueAdvice().test {
            assertThat(awaitItem()).isInstanceOf(Result.Loading::class.java)
            val success = awaitItem() as Result.Success
            assertThat(success.data.fatigueScore).isEqualTo(45.0)
            assertThat(success.data.level).isEqualTo(FatigueLevel.MODERATE)
            assertThat(success.data.recommendations).hasSize(2)
            awaitComplete()
        }
    }
}
