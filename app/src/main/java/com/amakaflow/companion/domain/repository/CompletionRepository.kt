package com.amakaflow.companion.domain.repository

import com.amakaflow.companion.data.local.PendingCompletionEntity
import com.amakaflow.companion.data.model.WorkoutCompletion
import com.amakaflow.companion.data.model.WorkoutCompletionDetail
import com.amakaflow.companion.data.model.WorkoutCompletionSaveResponse
import com.amakaflow.companion.data.model.WorkoutCompletionSubmission
import com.amakaflow.companion.domain.Result
import kotlinx.coroutines.flow.Flow

/**
 * Result wrapper for paginated completions.
 */
data class CompletionsResult(
    val completions: List<WorkoutCompletion>,
    val total: Int
)

/**
 * Repository interface for workout completion operations.
 * Part of the domain layer - defines the contract for completion data access.
 */
interface CompletionRepository {
    /**
     * Get paginated list of workout completions (history).
     */
    fun getCompletions(limit: Int = 50, offset: Int = 0): Flow<Result<CompletionsResult>>

    /**
     * Get detailed information about a specific completion.
     */
    fun getCompletionDetail(id: String): Flow<Result<WorkoutCompletionDetail>>

    /**
     * Submit a workout completion.
     * Attempts immediate submission; queues for later if offline or failed.
     * AMA-323: Returns WorkoutCompletionSaveResponse (the actual API shape), not WorkoutCompletion.
     */
    suspend fun submitCompletion(submission: WorkoutCompletionSubmission): Result<WorkoutCompletionSaveResponse>

    /**
     * Queue a completion for later submission.
     * Used when offline or when immediate submission fails.
     */
    suspend fun queueCompletion(submission: WorkoutCompletionSubmission)

    /**
     * Get the number of pending completions in the queue.
     */
    fun getPendingCount(): Flow<Int>

    /**
     * Get all pending completions.
     */
    fun getPendingCompletions(): Flow<List<PendingCompletionEntity>>

    /**
     * Manually trigger sync of pending completions.
     */
    fun triggerSync()

    /**
     * Get count of pending completions synchronously.
     */
    suspend fun getPendingCountSync(): Int
}
