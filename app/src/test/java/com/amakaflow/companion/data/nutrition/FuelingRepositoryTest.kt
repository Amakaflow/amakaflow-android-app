package com.amakaflow.companion.data.nutrition

import com.amakaflow.companion.data.api.AmakaflowApi
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class FuelingRepositoryTest {

    private lateinit var mockApi: AmakaflowApi
    private lateinit var repository: FuelingRepository

    @Before
    fun setup() {
        mockApi = mockk(relaxed = true)
        repository = FuelingRepository(mockApi)
    }

    @Test
    fun `getFuelingStatus returns data on success`() = runTest {
        val expected = FuelingStatusResponse(
            status = FuelingLevel.GREEN,
            proteinPct = 0.75,
            caloriesPct = 0.80,
            hydrationPct = 0.60,
            message = "Well fueled"
        )
        coEvery { mockApi.getFuelingStatus() } returns Response.success(expected)

        val result = repository.getFuelingStatus()

        assertThat(result).isNotNull()
        assertThat(result!!.status).isEqualTo(FuelingLevel.GREEN)
        assertThat(result.proteinPct).isEqualTo(0.75)
    }

    @Test
    fun `getFuelingStatus returns null on HTTP error`() = runTest {
        coEvery { mockApi.getFuelingStatus() } returns Response.error(
            500,
            "Server error".toResponseBody()
        )

        val result = repository.getFuelingStatus()

        assertThat(result).isNull()
    }

    @Test
    fun `getFuelingStatus returns null on exception`() = runTest {
        coEvery { mockApi.getFuelingStatus() } throws RuntimeException("Network error")

        val result = repository.getFuelingStatus()

        assertThat(result).isNull()
    }

    @Test
    fun `checkProteinNudge returns data on success`() = runTest {
        val expected = ProteinNudgeResponse(
            shouldNudge = true,
            proteinCurrent = 45,
            proteinTarget = 120,
            message = "Have you had your post-workout protein?"
        )
        coEvery { mockApi.checkProteinNudge() } returns Response.success(expected)

        val result = repository.checkProteinNudge()

        assertThat(result).isNotNull()
        assertThat(result!!.shouldNudge).isTrue()
        assertThat(result.proteinCurrent).isEqualTo(45)
        assertThat(result.proteinTarget).isEqualTo(120)
    }

    @Test
    fun `checkProteinNudge returns null on HTTP error`() = runTest {
        coEvery { mockApi.checkProteinNudge() } returns Response.error(
            404,
            "Not found".toResponseBody()
        )

        val result = repository.checkProteinNudge()

        assertThat(result).isNull()
    }

    @Test
    fun `checkProteinNudge returns null on exception`() = runTest {
        coEvery { mockApi.checkProteinNudge() } throws RuntimeException("Timeout")

        val result = repository.checkProteinNudge()

        assertThat(result).isNull()
    }

    @Test
    fun `getFuelingStatus with yellow status`() = runTest {
        val expected = FuelingStatusResponse(
            status = FuelingLevel.YELLOW,
            proteinPct = 0.50,
            caloriesPct = 0.55,
            hydrationPct = 0.40,
            message = "Borderline"
        )
        coEvery { mockApi.getFuelingStatus() } returns Response.success(expected)

        val result = repository.getFuelingStatus()

        assertThat(result).isNotNull()
        assertThat(result!!.status).isEqualTo(FuelingLevel.YELLOW)
    }

    @Test
    fun `getFuelingStatus with red status`() = runTest {
        val expected = FuelingStatusResponse(
            status = FuelingLevel.RED,
            proteinPct = 0.20,
            caloriesPct = 0.25,
            hydrationPct = 0.15,
            message = "Under-fueled"
        )
        coEvery { mockApi.getFuelingStatus() } returns Response.success(expected)

        val result = repository.getFuelingStatus()

        assertThat(result).isNotNull()
        assertThat(result!!.status).isEqualTo(FuelingLevel.RED)
    }

    @Test
    fun `checkProteinNudge when should not nudge`() = runTest {
        val expected = ProteinNudgeResponse(
            shouldNudge = false,
            proteinCurrent = 100,
            proteinTarget = 120,
            message = "Protein intake is on track"
        )
        coEvery { mockApi.checkProteinNudge() } returns Response.success(expected)

        val result = repository.checkProteinNudge()

        assertThat(result).isNotNull()
        assertThat(result!!.shouldNudge).isFalse()
    }
}
