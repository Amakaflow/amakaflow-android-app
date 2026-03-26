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

    // MARK: - Calendar / DayState (AMA-1148)

    /**
     * Get the state of a specific day (workouts, fatigue, conflicts).
     */
    @GET("calendar/day-state")
    suspend fun getDayState(@Query("date") date: String): Response<DayStateResponse>

    /**
     * Get the state of a week starting from a given date.
     */
    @GET("calendar/week-state")
    suspend fun getWeekState(@Query("start") startDate: String): Response<WeekStateResponse>

    // MARK: - Planner (AMA-1148)

    /**
     * Generate a weekly training plan.
     */
    @POST("planner/generate-week")
    suspend fun generateWeek(@Body request: GenerateWeekRequest): Response<GenerateWeekResponse>

    // MARK: - Coach Chat (AMA-1148)

    /**
     * Send a message to the AI coach.
     */
    @POST("coach/message")
    suspend fun sendCoachMessage(@Body request: CoachMessageRequest): Response<CoachMessageResponse>

    // MARK: - Activity Feed (AMA-1148)

    /**
     * Get the activity feed.
     */
    @GET("activity/feed")
    suspend fun getActivityFeed(
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): Response<ActivityFeedResponse>

    // MARK: - Training Preferences (AMA-1148)

    /**
     * Get training preferences.
     */
    @GET("preferences/training")
    suspend fun getTrainingPreferences(): Response<TrainingPreferencesResponse>

    /**
     * Update training preferences.
     */
    @PUT("preferences/training")
    suspend fun updateTrainingPreferences(@Body preferences: TrainingPreferences): Response<TrainingPreferencesResponse>

    // MARK: - Conflict Detection (AMA-1148)

    /**
     * Check for scheduling conflicts on a given date.
     */
    @GET("calendar/conflicts")
    suspend fun getConflicts(@Query("date") date: String): Response<ConflictsResponse>

    // MARK: - Shoe Comparison (AMA-1148)

    /**
     * Compare shoes based on criteria.
     */
    @POST("coach/shoe-comparison")
    suspend fun compareShoes(@Body request: ShoeComparisonRequest): Response<ShoeComparisonResponse>

    // MARK: - Fatigue Advisor (AMA-1148)

    /**
     * Get fatigue advice based on recent activity.
     */
    @GET("coach/fatigue-advisor")

    /**
     * Suggest a workout based on coaching profile (AMA-1265).
     */
    @POST("coach/suggest-workout")
    suspend fun suggestWorkout(@Body request: SuggestWorkoutRequest): Response<SuggestWorkoutResponse>
    suspend fun getFatigueAdvice(): Response<FatigueAdvisorResponse>

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

    /**
     * AMA-1258: Generic URL import — backend auto-detects platform from URL
     */
    @POST("ingest/url")
    suspend fun importUrl(@Body request: UrlImportRequest): Response<WorkoutResponse>
}
