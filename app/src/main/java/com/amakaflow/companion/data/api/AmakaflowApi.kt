package com.amakaflow.companion.data.api

import com.amakaflow.companion.data.model.*
import com.amakaflow.companion.data.nutrition.BarcodeProductResponse
import com.amakaflow.companion.data.nutrition.FuelingStatusResponse
import com.amakaflow.companion.data.nutrition.ParseTextRequest
import com.amakaflow.companion.data.nutrition.ParseTextResponse
import com.amakaflow.companion.data.nutrition.PhotoAnalysisRequest
import com.amakaflow.companion.data.nutrition.PhotoAnalysisResponse
import com.amakaflow.companion.data.nutrition.ProteinNudgeResponse
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

    // MARK: - RPE Feedback (AMA-1266)

    /**
     * Submit post-workout RPE feedback.
     */
    @POST("coach/rpe-feedback")
    suspend fun submitRPEFeedback(@Body request: RPEFeedbackRequest): Response<RPEFeedbackResponse>

    // MARK: - Social Feed (AMA-1273)

    @GET("social/feed")
    suspend fun getSocialFeed(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<SocialFeedResponse>

    @POST("social/posts/{postId}/react")
    suspend fun addSocialReaction(
        @Path("postId") postId: String,
        @Body request: ReactRequest
    ): Response<Unit>

    @DELETE("social/posts/{postId}/react/{emoji}")
    suspend fun removeSocialReaction(
        @Path("postId") postId: String,
        @Path("emoji") emoji: String
    ): Response<Unit>

    @GET("social/posts/{postId}/comments")
    suspend fun getSocialComments(
        @Path("postId") postId: String
    ): Response<SocialCommentsResponse>

    @POST("social/posts/{postId}/comment")
    suspend fun postSocialComment(
        @Path("postId") postId: String,
        @Body request: CommentRequest
    ): Response<Unit>

    @GET("social/settings")
    suspend fun getSocialSettings(): Response<SocialSettings>

    @PUT("social/settings")
    suspend fun updateSocialSettings(
        @Body settings: SocialSettings
    ): Response<Unit>

    @GET("social/users/{userId}/profile")
    suspend fun getUserPublicProfile(
        @Path("userId") userId: String
    ): Response<UserPublicProfile>

    @POST("social/users/{userId}/follow")
    suspend fun followUser(@Path("userId") userId: String): Response<Unit>

    @POST("social/users/{userId}/unfollow")
    suspend fun unfollowUser(@Path("userId") userId: String): Response<Unit>

    // MARK: - Training Crews (AMA-1277)

    @GET("social/crews")
    suspend fun getMyCrews(): Response<CrewListResponse>

    @GET("social/crews/{id}")
    suspend fun getCrewDetail(
        @Path("id") id: String
    ): Response<CrewDetail>

    @GET("social/crews/{id}/feed")
    suspend fun getCrewFeed(
        @Path("id") id: String
    ): Response<CrewFeedResponse>

    @POST("social/crews")
    suspend fun createCrew(
        @Body request: CreateCrewApiRequest
    ): Response<Unit>

    @POST("social/crews/{id}/join")
    suspend fun joinCrew(
        @Path("id") id: String,
        @Body request: JoinCrewApiRequest
    ): Response<Unit>

    @DELETE("social/crews/{id}/leave")
    suspend fun leaveCrew(
        @Path("id") id: String
    ): Response<Unit>

    // MARK: - Challenges (AMA-1276)

    @GET("social/challenges")
    suspend fun getChallenges(): Response<ChallengesResponse>

    @GET("social/challenges/{id}")
    suspend fun getChallengeDetail(
        @Path("id") id: String
    ): Response<ChallengeDetailResponse>

    @POST("social/challenges")
    suspend fun createChallenge(
        @Body request: CreateChallengeRequest
    ): Response<Unit>

    @POST("social/challenges/{id}/join")
    suspend fun joinChallenge(
        @Path("id") id: String
    ): Response<Unit>

    // MARK: - Leaderboards (AMA-1278)

    @GET("social/leaderboards/friends")
    suspend fun getFriendsLeaderboard(
        @Query("dimension") dimension: String = "volume",
        @Query("period") period: String = "month"
    ): Response<DimensionLeaderboardResponse>

    @GET("social/leaderboards/crew/{crewId}")
    suspend fun getCrewLeaderboard(
        @Path("crewId") crewId: String,
        @Query("dimension") dimension: String = "volume",
        @Query("period") period: String = "month"
    ): Response<DimensionLeaderboardResponse>


    // MARK: - Volume Analytics

    /**
     * Fetch volume analytics for a date range.
     */
    @GET("progression/volume")
    suspend fun fetchVolumeAnalytics(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("granularity") granularity: String
    ): Response<VolumeAnalyticsResponse>

    // MARK: - Nutrition / Fueling (AMA-1293)

    /**
     * Get today's fueling status based on nutrition data.
     */
    @GET("nutrition/fueling-status")
    suspend fun getFuelingStatus(): Response<FuelingStatusResponse>

    /**
     * Check whether a post-workout protein nudge should fire.
     */
    @POST("nutrition/protein-nudge/check")
    suspend fun checkProteinNudge(): Response<ProteinNudgeResponse>


    // MARK: - AI Food Logging (AMA-1294)

    /**
     * Analyze a meal photo using Claude Vision to estimate macros.
     */
    @POST("nutrition/analyze-photo")
    suspend fun analyzePhoto(@Body request: PhotoAnalysisRequest): Response<PhotoAnalysisResponse>

    /**
     * Look up a product by barcode via Open Food Facts.
     */
    @GET("nutrition/barcode/{code}")
    suspend fun lookupBarcode(@Path("code") code: String): Response<BarcodeProductResponse>

    /**
     * Parse free-text food description using Claude NLP.
     */
    @POST("nutrition/parse-text")
    suspend fun parseNutritionText(@Body request: ParseTextRequest): Response<ParseTextResponse>
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
