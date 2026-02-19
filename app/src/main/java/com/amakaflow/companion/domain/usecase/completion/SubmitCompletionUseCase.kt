package com.amakaflow.companion.domain.usecase.completion

import com.amakaflow.companion.data.model.WorkoutCompletionSaveResponse
import com.amakaflow.companion.data.model.WorkoutCompletionSubmission
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.CompletionRepository
import javax.inject.Inject

/**
 * Use case for submitting a workout completion.
 */
class SubmitCompletionUseCase @Inject constructor(
    private val completionRepository: CompletionRepository
) {
    /**
     * Submit a workout completion.
     * Attempts immediate submission; queues for later sync if offline or failed.
     */
    suspend operator fun invoke(submission: WorkoutCompletionSubmission): Result<WorkoutCompletionSaveResponse> {
        return completionRepository.submitCompletion(submission)
    }
}
