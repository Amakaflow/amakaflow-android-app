package com.amakaflow.companion.domain.usecase.completion

import com.amakaflow.companion.data.model.CompletionSource
import com.amakaflow.companion.data.model.HealthMetrics
import com.amakaflow.companion.data.model.WorkoutCompletionSaveResponse
import com.amakaflow.companion.data.model.WorkoutCompletionSubmission
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.CompletionRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test

class SubmitCompletionUseCaseTest {

    private lateinit var useCase: SubmitCompletionUseCase
    private lateinit var mockRepository: CompletionRepository

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = SubmitCompletionUseCase(mockRepository)
    }

    private fun createSubmission(
        workoutId: String? = "workout-001",
        workoutName: String = "HIIT Blast",
        avgHeartRate: Int? = null,
        activeCalories: Int? = null
    ) = WorkoutCompletionSubmission(
        workoutId = workoutId,
        workoutName = workoutName,
        startedAt = Instant.parse("2024-01-15T10:00:00Z"),
        endedAt = Instant.parse("2024-01-15T10:30:00Z"),
        source = CompletionSource.PHONE,
        healthMetrics = HealthMetrics(
            avgHeartRate = avgHeartRate,
            activeCalories = activeCalories
        )
    )

    @Test
    fun `invoke returns Success when submission succeeds`() = runTest {
        // Given
        val submission = createSubmission()
        val saveResponse = WorkoutCompletionSaveResponse(success = true, id = "completion-001")
        coEvery { mockRepository.submitCompletion(submission) } returns Result.Success(saveResponse)

        // When
        val result = useCase(submission)

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data.id).isEqualTo("completion-001")
    }

    @Test
    fun `invoke returns Error when submission fails`() = runTest {
        // Given
        val submission = createSubmission()
        coEvery { mockRepository.submitCompletion(submission) } returns Result.Error("Network error")

        // When
        val result = useCase(submission)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).message).isEqualTo("Network error")
    }

    @Test
    fun `invoke passes submission to repository`() = runTest {
        // Given
        val submission = createSubmission(
            workoutId = "workout-002",
            workoutName = "Strength Training",
            avgHeartRate = 120,
            activeCalories = 250
        )
        coEvery { mockRepository.submitCompletion(submission) } returns Result.Success(
            WorkoutCompletionSaveResponse(success = true, id = "completion-002")
        )

        // When
        useCase(submission)

        // Then
        coVerify { mockRepository.submitCompletion(submission) }
    }

    @Test
    fun `invoke returns Error with code when API returns error`() = runTest {
        // Given
        val submission = createSubmission()
        coEvery { mockRepository.submitCompletion(submission) } returns Result.Error("Unauthorized", 401)

        // When
        val result = useCase(submission)

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        assertThat((result as Result.Error).code).isEqualTo(401)
    }
}
