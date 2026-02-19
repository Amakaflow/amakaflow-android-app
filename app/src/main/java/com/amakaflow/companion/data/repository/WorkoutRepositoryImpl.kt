package com.amakaflow.companion.data.repository

import android.util.Log
import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.local.PushedWorkoutDao
import com.amakaflow.companion.data.local.WorkoutEntityMapper
import com.amakaflow.companion.data.model.ConfirmSyncRequest
import com.amakaflow.companion.data.model.ReportSyncFailedRequest
import com.amakaflow.companion.data.model.Workout
import com.amakaflow.companion.data.model.WorkoutCompletionDetail
import com.amakaflow.companion.data.model.WorkoutCompletionSaveResponse
import com.amakaflow.companion.data.model.WorkoutCompletionSubmission
import com.amakaflow.companion.debug.DebugLog
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.CompletionsResult
import com.amakaflow.companion.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "API"

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val api: AmakaflowApi,
    private val pushedWorkoutDao: PushedWorkoutDao
) : WorkoutRepository {
    // In-memory cache of workouts for quick lookup
    private val workoutCache = mutableMapOf<String, Workout>()

    companion object {
        private const val REPO_TAG = "WorkoutRepository"
    }

    /**
     * Cache workouts for later lookup by ID
     */
    private fun cacheWorkouts(workouts: List<Workout>) {
        workouts.forEach { workout ->
            workoutCache[workout.id] = workout
        }
    }

    /**
     * Get a cached workout by ID, or null if not found
     */
    override fun getCachedWorkout(id: String): Workout? = workoutCache[id]

    override fun getIncomingWorkouts(): Flow<Result<List<Workout>>> = flow {
        emit(Result.Loading)
        DebugLog.info("Fetching incoming workouts...", TAG)
        try {
            val response = api.getIncomingWorkouts()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    // Cache workouts for later lookup by ID
                    cacheWorkouts(body.workouts)
                    DebugLog.success("Fetched ${body.workouts.size} incoming workout(s)", TAG)
                    emit(Result.Success(body.workouts))
                } else {
                    DebugLog.error("Failed to load incoming workouts: ${body.message}", TAG)
                    emit(Result.Error(body.message ?: "Failed to load incoming workouts", response.code()))
                }
            } else {
                DebugLog.error("Failed to load incoming workouts: ${response.code()}", TAG)
                emit(Result.Error("Failed to load incoming workouts", response.code()))
            }
        } catch (e: Exception) {
            DebugLog.error("Exception fetching incoming workouts: ${e.message}", TAG)
            emit(Result.Error(e.message ?: "Unknown error"))
        }
    }

    override fun getPushedWorkouts(): Flow<Result<List<Workout>>> = flow {
        emit(Result.Loading)
        DebugLog.info("Fetching pushed workouts...", TAG)
        try {
            val response = api.getPushedWorkouts()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    // Cache workouts for later lookup by ID
                    cacheWorkouts(body.workouts)

                    // AMA-320: Store workouts in Room for local persistence
                    if (body.workouts.isNotEmpty()) {
                        Log.d(REPO_TAG, "Storing ${body.workouts.size} workouts in local database")
                        val entities = WorkoutEntityMapper.toEntities(body.workouts)
                        pushedWorkoutDao.upsertAll(entities)
                    }

                    DebugLog.success("Fetched ${body.workouts.size} pushed workout(s)", TAG)
                    emit(Result.Success(body.workouts))
                } else {
                    DebugLog.error("Failed to load pushed workouts: ${body.message}", TAG)
                    emit(Result.Error(body.message ?: "Failed to load pushed workouts", response.code()))
                }
            } else {
                DebugLog.error("Failed to load pushed workouts: ${response.code()}", TAG)
                emit(Result.Error("Failed to load pushed workouts", response.code()))
            }
        } catch (e: Exception) {
            DebugLog.error("Exception fetching pushed workouts: ${e.message}", TAG)
            emit(Result.Error(e.message ?: "Unknown error"))
        }
    }

    /**
     * Get locally stored pushed workouts (AMA-320)
     */
    override fun getLocalPushedWorkouts(): Flow<List<Workout>> {
        Log.d(REPO_TAG, "Getting local pushed workouts from Room")
        return pushedWorkoutDao.getActiveWorkouts().map { entities ->
            Log.d(REPO_TAG, "Found ${entities.size} local workouts")
            WorkoutEntityMapper.toWorkouts(entities)
        }
    }

    /**
     * Get locally stored pushed workouts synchronously (AMA-320)
     */
    override suspend fun getLocalPushedWorkoutsSync(): List<Workout> {
        val entities = pushedWorkoutDao.getActiveWorkoutsSync()
        Log.d(REPO_TAG, "Found ${entities.size} local workouts (sync)")
        return WorkoutEntityMapper.toWorkouts(entities)
    }

    /**
     * Get a specific workout from local storage (AMA-320)
     */
    override suspend fun getLocalWorkout(workoutId: String): Workout? {
        val entity = pushedWorkoutDao.getById(workoutId)
        return entity?.let { WorkoutEntityMapper.toWorkout(it) }
    }

    /**
     * Mark a workout as completed in local storage (AMA-320)
     */
    override suspend fun markWorkoutCompleted(workoutId: String) {
        Log.d(REPO_TAG, "Marking workout $workoutId as completed")
        pushedWorkoutDao.markCompleted(workoutId)
    }

    override fun getWorkout(id: String): Flow<Result<Workout>> = flow {
        // First check the in-memory cache for the workout
        val cachedWorkout = workoutCache[id]
        if (cachedWorkout != null) {
            emit(Result.Success(cachedWorkout))
            return@flow
        }

        // AMA-320: Check local Room storage
        val localWorkout = pushedWorkoutDao.getById(id)
        if (localWorkout != null) {
            val workout = WorkoutEntityMapper.toWorkout(localWorkout)
            workoutCache[id] = workout // Also add to memory cache
            emit(Result.Success(workout))
            return@flow
        }

        // Workout not in cache or local storage, try fetching from API
        emit(Result.Loading)
        try {
            val response = api.getWorkout(id)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success && body.workout != null) {
                    // Cache the fetched workout
                    workoutCache[id] = body.workout
                    emit(Result.Success(body.workout))
                } else {
                    emit(Result.Error(body.message ?: "Workout not found", response.code()))
                }
            } else {
                emit(Result.Error("Failed to load workout", response.code()))
            }
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "Unknown error"))
        }
    }

    // --- Completion methods (will be moved to CompletionRepository in future) ---

    fun getCompletions(limit: Int = 50, offset: Int = 0): Flow<Result<CompletionsResult>> = flow {
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

    fun getCompletionDetail(id: String): Flow<Result<WorkoutCompletionDetail>> = flow {
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
     * Submit a completed workout to the API
     */
    suspend fun completeWorkout(submission: WorkoutCompletionSubmission): Result<WorkoutCompletionSaveResponse> {
        DebugLog.info("Submitting workout completion for: ${submission.workoutId}", TAG)
        return try {
            val response = api.completeWorkout(submission)
            if (response.isSuccessful && response.body() != null) {
                DebugLog.success("Workout completion submitted successfully", TAG)
                Result.Success(response.body()!!)
            } else {
                DebugLog.error("Failed to submit completion: ${response.code()}", TAG)
                Result.Error("Failed to submit completion", response.code())
            }
        } catch (e: Exception) {
            DebugLog.error("Exception submitting completion: ${e.message}", TAG)
            Result.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Confirm successful workout sync to backend (AMA-307)
     */
    override suspend fun confirmSync(workoutId: String): Result<Unit> {
        DebugLog.info("Confirming sync for workout: $workoutId", TAG)
        return try {
            val request = ConfirmSyncRequest(workoutId = workoutId)
            val response = api.confirmSync(request)
            if (response.isSuccessful) {
                // AMA-320: Mark workout as synced in local storage
                pushedWorkoutDao.markSynced(workoutId)
                Log.d(REPO_TAG, "Marked workout $workoutId as synced in local storage")

                DebugLog.success("Sync confirmed for workout: $workoutId", TAG)
                Result.Success(Unit)
            } else {
                DebugLog.error("Failed to confirm sync: ${response.code()}", TAG)
                Result.Error("Failed to confirm sync", response.code())
            }
        } catch (e: Exception) {
            DebugLog.error("Exception confirming sync: ${e.message}", TAG)
            Result.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Report failed workout sync to backend (AMA-307)
     */
    override suspend fun reportSyncFailed(workoutId: String, error: String): Result<Unit> {
        DebugLog.info("Reporting sync failure for workout: $workoutId - $error", TAG)
        return try {
            val request = ReportSyncFailedRequest(workoutId = workoutId, error = error)
            val response = api.reportSyncFailed(request)
            if (response.isSuccessful) {
                DebugLog.success("Sync failure reported for workout: $workoutId", TAG)
                Result.Success(Unit)
            } else {
                DebugLog.error("Failed to report sync failure: ${response.code()}", TAG)
                Result.Error("Failed to report sync failure", response.code())
            }
        } catch (e: Exception) {
            DebugLog.error("Exception reporting sync failure: ${e.message}", TAG)
            Result.Error(e.message ?: "Unknown error")
        }
    }
}
