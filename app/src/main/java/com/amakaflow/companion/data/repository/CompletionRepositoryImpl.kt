package com.amakaflow.companion.data.repository

import android.content.Context
import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.local.PendingCompletionDao
import com.amakaflow.companion.data.local.PendingCompletionEntity
import com.amakaflow.companion.data.model.WorkoutCompletionDetail
import com.amakaflow.companion.data.model.WorkoutCompletionSaveResponse
import com.amakaflow.companion.data.model.WorkoutCompletionSubmission
import com.amakaflow.companion.data.sync.CompletionSyncWorker
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.CompletionRepository
import com.amakaflow.companion.domain.repository.CompletionsResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompletionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: AmakaflowApi,
    private val pendingCompletionDao: PendingCompletionDao,
    private val json: Json
) : CompletionRepository {

    override fun getPendingCount(): Flow<Int> = pendingCompletionDao.getCount()

    override fun getPendingCompletions(): Flow<List<PendingCompletionEntity>> = pendingCompletionDao.getAll()

    override fun getCompletions(limit: Int, offset: Int): Flow<Result<CompletionsResult>> = flow {
        emit(Result.Loading)
        try {
            val response = api.getCompletions(limit, offset)
            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!
                emit(Result.Success(CompletionsResult(
                    completions = body.completions,
                    total = body.total
                )))
            } else {
                emit(Result.Error("Failed to load completions", response.code()))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Unknown error"))
        }
    }

    override fun getCompletionDetail(id: String): Flow<Result<WorkoutCompletionDetail>> = flow {
        emit(Result.Loading)
        try {
            val response = api.getCompletionDetail(id)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Result.Success(response.body()!!.completion))
            } else {
                emit(Result.Error("Failed to load completion detail", response.code()))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Unknown error"))
        }
    }

    /**
     * Submit a workout completion.
     * If online, attempts immediate submission. If offline or failed, queues for later sync.
     */
    // AMA-323: Return type corrected from Result<WorkoutCompletion> to Result<WorkoutCompletionSaveResponse>.
    // POST /workouts/complete returns {"success": true, "id": "...", "summary": {...}} — not a full WorkoutCompletion.
    // The mismatch caused deserialization to always throw, queuing every completion as "pending" even on success.
    override suspend fun submitCompletion(submission: WorkoutCompletionSubmission): Result<WorkoutCompletionSaveResponse> {
        return try {
            // Try immediate submission
            val response = api.completeWorkout(submission)

            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                // Queue for later if server error
                queueCompletion(submission)
                Result.Error("Submission queued for later sync", response.code())
            }
        } catch (e: Exception) {
            // Queue for later on network error
            queueCompletion(submission)
            Result.Error("No network - submission queued for later sync")
        }
    }

    /**
     * Queue a completion for later sync
     */
    override suspend fun queueCompletion(submission: WorkoutCompletionSubmission) {
        // Calculate duration from startedAt and endedAt
        val durationSeconds = submission.endedAt?.let { ended ->
            ((ended.toEpochMilliseconds() - submission.startedAt.toEpochMilliseconds()) / 1000).toInt()
        } ?: 0

        val entity = PendingCompletionEntity(
            workoutId = submission.workoutId,
            workoutName = submission.workoutName,
            startedAt = submission.startedAt.toString(),
            endedAt = submission.endedAt?.toString(),
            durationSeconds = durationSeconds,
            source = submission.source.name.lowercase(),
            avgHeartRate = submission.healthMetrics.avgHeartRate,
            maxHeartRate = submission.healthMetrics.maxHeartRate,
            minHeartRate = submission.healthMetrics.minHeartRate,
            activeCalories = submission.healthMetrics.activeCalories,
            totalCalories = submission.healthMetrics.totalCalories,
            deviceInfoJson = submission.deviceInfo?.let { json.encodeToString(it) },
            workoutStructureJson = submission.workoutStructure?.let { json.encodeToString(it) },
            isSimulated = submission.isSimulated
        )

        pendingCompletionDao.insert(entity)

        // Trigger sync worker
        CompletionSyncWorker.enqueue(context)
    }

    /**
     * Manually trigger sync of pending completions
     */
    override fun triggerSync() {
        CompletionSyncWorker.enqueue(context)
    }

    /**
     * Schedule periodic sync
     */
    fun schedulePeriodicSync() {
        CompletionSyncWorker.schedulePeriodicSync(context)
    }

    /**
     * Get count of pending completions synchronously
     */
    override suspend fun getPendingCountSync(): Int = pendingCompletionDao.getCountSync()
}
