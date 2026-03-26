package com.amakaflow.companion.data.api

import com.amakaflow.companion.data.model.*
import okhttp3.ResponseBody
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
    suspend fun getFatigueAdvice(): Response<FatigueAdvisorResponse>

    // MARK: - Workout Editor (AMA-1232)

    /**
     * Create or save a workout.
     */
    @POST("workouts/save")
    suspend fun saveWorkout(@Body request: WorkoutSaveRequest): Response<WorkoutResponse>

    // MARK: - Workout Tags (AMA-1242)

    /**
     * Update tags on a workout.
     */
    @PATCH("workouts/{id}/tags")
    suspend fun updateWorkoutTags(
        @Path("id") id: String,
        @Body request: UpdateTagsRequest
    ): Response<WorkoutResponse>

    // MARK: - Knowledge Library (AMA-1241)

    /**
     * List knowledge cards with pagination.
     */
    @GET("knowledge/cards")
    suspend fun getKnowledgeCards(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<KnowledgeCardListResponse>

    /**
     * Search knowledge cards by query.
     */
    @GET("knowledge/search")
    suspend fun searchKnowledgeCards(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): Response<KnowledgeCardListResponse>

    /**
     * Ingest a new knowledge card (URL or text).
     */
    @POST("knowledge/ingest")
    suspend fun ingestKnowledge(@Body request: KnowledgeIngestRequest): Response<KnowledgeCard>
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
     * Import a workout from an Instagram reel URL (AMA-1237)
     */
    @POST("ingest/instagram_reel")
    suspend fun importInstagramReel(@Body request: InstagramReelRequest): Response<WorkoutResponse>

    /**
     * Import a workout from a YouTube URL (AMA-1239)
     */
    @POST("ingest/youtube")
    suspend fun importYouTube(@Body request: UrlImportRequest): Response<WorkoutResponse>

    /**
     * Import a workout from a TikTok URL (AMA-1239)
     */
    @POST("ingest/tiktok")
    suspend fun importTikTok(@Body request: UrlImportRequest): Response<WorkoutResponse>

    /**
     * Import a workout from a Pinterest URL (AMA-1239)
     */
    @POST("ingest/pinterest")
    suspend fun importPinterest(@Body request: UrlImportRequest): Response<WorkoutResponse>

    /**
     * AMA-1258: Generic URL import — backend auto-detects platform from URL
     */
    @POST("ingest/url")
    suspend fun importUrl(@Body request: UrlImportRequest): Response<WorkoutResponse>

    // MARK: - Workout Export (AMA-1233)

    /**
     * Export a workout as a FIT file (Garmin-compatible)
     */
    @POST("export/fit")
    suspend fun exportFIT(@Body request: ExportRequest): Response<ResponseBody>

    /**
     * Export a workout as a CSV file (Strong-compatible)
     */
    @POST("export/csv")
    suspend fun exportCSV(@Body request: ExportRequest): Response<ResponseBody>
}
