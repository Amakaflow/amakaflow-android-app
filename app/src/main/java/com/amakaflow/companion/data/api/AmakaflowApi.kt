package com.amakaflow.companion.data.api

import com.amakaflow.companion.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Amakaflow Mapper API interface for Retrofit
 */
interface AmakaflowApi {

    // MARK: - Pairing

    @POST("mobile/pairing/pair")
    suspend fun pair(@Body request: PairingRequest): Response<PairingResponse>

    @POST("mobile/pairing/refresh")
    suspend fun refreshToken(@Body request: TokenRefreshRequest): Response<TokenRefreshResponse>

    // MARK: - Workouts

    /**
     * Fetch workouts from connected calendars
     */
    @GET("workouts/incoming")
    suspend fun getIncomingWorkouts(): Response<IncomingWorkoutsResponse>

    /**
     * Fetch scheduled workouts
     */
    @GET("workouts/scheduled")
    suspend fun getScheduledWorkouts(): Response<List<ScheduledWorkout>>

    /**
     * Fetch workouts pushed to Android Companion App (using new sync queue endpoint)
     * AMA-307: Uses /sync/pending for proper sync state tracking
     */
    @GET("sync/pending")
    suspend fun getPushedWorkouts(
        @Query("device_type") deviceType: String = "android",
        @Query("limit") limit: Int = 50
    ): Response<PushedWorkoutsResponse>

    /**
     * Confirm successful workout sync (AMA-307)
     */
    @POST("sync/confirm")
    suspend fun confirmSync(@Body request: ConfirmSyncRequest): Response<SyncStatusResponse>

    /**
     * Report failed workout sync (AMA-307)
     */
    @POST("sync/failed")
    suspend fun reportSyncFailed(@Body request: ReportSyncFailedRequest): Response<SyncStatusResponse>

    /**
     * Get a specific workout by ID
     */
    @GET("workouts/{id}")
    suspend fun getWorkout(@Path("id") id: String): Response<WorkoutResponse>

    // MARK: - Workout Completions (History)

    /**
     * List workout history with pagination
     */
    @GET("workouts/completions")
    suspend fun getCompletions(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<CompletionsResponse>

    /**
     * Get detailed workout completion by ID
     */
    @GET("workouts/completions/{id}")
    suspend fun getCompletionDetail(@Path("id") id: String): Response<CompletionDetailResponse>

    /**
     * Submit a completed workout.
     * AMA-323: API returns {"success": true, "id": "...", "summary": {...}}, NOT a WorkoutCompletion.
     */
    @POST("workouts/complete")
    suspend fun completeWorkout(@Body submission: WorkoutCompletionSubmission): Response<WorkoutCompletionSaveResponse>
}

/**
 * Ingestor API interface for workout voice parsing and completion queue
 */
interface IngestorApi {

    /**
     * Queue a workout completion for later processing (offline support)
     */
    @POST("android-companion/pending")
    suspend fun queueCompletion(@Body submission: WorkoutCompletionSubmission): Response<Unit>

    /**
     * Submit a workout completion directly
     */
    @POST("submit")
    suspend fun submitCompletion(@Body submission: WorkoutCompletionSubmission): Response<WorkoutCompletion>

    /**
     * Parse voice input into structured workout
     */
    @POST("workouts/parse-voice")
    suspend fun parseVoiceWorkout(@Body request: VoiceWorkoutRequest): Response<VoiceWorkoutResponse>
}
