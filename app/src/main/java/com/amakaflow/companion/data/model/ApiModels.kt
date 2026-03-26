package com.amakaflow.companion.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pairing request to exchange code for JWT
 */
@Serializable
data class PairingRequest(
    val token: String? = null,
    @SerialName("short_code")
    val shortCode: String? = null,
    @SerialName("device_info")
    val deviceInfo: DeviceInfo
)

/**
 * Device information for pairing
 */
@Serializable
data class DeviceInfo(
    val device: String,
    val os: String,
    @SerialName("app_version")
    val appVersion: String,
    @SerialName("device_id")
    val deviceId: String
)

/**
 * Pairing response with JWT
 */
@Serializable
data class PairingResponse(
    val jwt: String,
    val profile: UserProfile? = null,
    @SerialName("expires_at")
    val expiresAt: String
)

/**
 * User profile information
 */
@Serializable
data class UserProfile(
    val id: String,
    val email: String? = null,
    val name: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)

/**
 * Token refresh request
 */
@Serializable
data class TokenRefreshRequest(
    @SerialName("device_id")
    val deviceId: String
)

/**
 * Token refresh response
 */
@Serializable
data class TokenRefreshResponse(
    val jwt: String,
    @SerialName("expires_at")
    val expiresAt: Instant,
    @SerialName("refreshed_at")
    val refreshedAt: Instant
)

/**
 * API error response
 */
@Serializable
data class ApiErrorResponse(
    val detail: String? = null,
    val error: String? = null,
    val message: String? = null
)

/**
 * Incoming workouts response
 */
@Serializable
data class IncomingWorkoutsResponse(
    val success: Boolean,
    val workouts: List<Workout> = emptyList(),
    val count: Int? = null,
    val message: String? = null
)

/**
 * Health metrics for workout completion
 */
@Serializable
data class HealthMetrics(
    @SerialName("avg_heart_rate")
    val avgHeartRate: Int? = null,
    @SerialName("max_heart_rate")
    val maxHeartRate: Int? = null,
    @SerialName("min_heart_rate")
    val minHeartRate: Int? = null,
    @SerialName("active_calories")
    val activeCalories: Int? = null,
    @SerialName("total_calories")
    val totalCalories: Int? = null,
    @SerialName("distance_meters")
    val distanceMeters: Int? = null,
    val steps: Int? = null
)

/**
 * AMA-287: Set entry for weight tracking during reps exercises
 */
@Serializable
data class SetEntry(
    @SerialName("set_number")
    val setNumber: Int,
    val weight: Double? = null,
    val unit: String? = null,
    val completed: Boolean = true
)

/**
 * AMA-287: Exercise set log for weight tracking during submission
 */
@Serializable
data class ExerciseSetLog(
    @SerialName("exercise_name")
    val exerciseName: String,
    @SerialName("exercise_index")
    val exerciseIndex: Int,
    val sets: List<SetEntry>
)

/**
 * Workout completion submission - matches iOS WorkoutCompletionRequest structure
 */
@Serializable
data class WorkoutCompletionSubmission(
    @SerialName("workout_id")
    val workoutId: String? = null,
    @SerialName("workout_name")
    val workoutName: String,
    @SerialName("started_at")
    val startedAt: Instant,
    @SerialName("ended_at")
    val endedAt: Instant? = null,
    val source: CompletionSource,
    @SerialName("health_metrics")
    val healthMetrics: HealthMetrics,
    @SerialName("device_info")
    val deviceInfo: CompletionDeviceInfo? = null,
    @SerialName("workout_structure")
    val workoutStructure: List<WorkoutIntervalSubmission>? = null,
    @SerialName("is_simulated")
    val isSimulated: Boolean = false,
    @SerialName("set_logs")
    val setLogs: List<ExerciseSetLog>? = null,
    // AMA-293: Execution log — what was actually done vs planned
    @SerialName("execution_log")
    val executionLog: ExecutionLog? = null
)

/**
 * Simplified interval for workout submission
 */
@Serializable
data class WorkoutIntervalSubmission(
    val type: String,
    val seconds: Int? = null,
    val target: String? = null,
    val reps: Int? = null,
    val sets: Int? = null,
    val name: String? = null,
    val intervals: List<WorkoutIntervalSubmission>? = null  // For nested intervals in repeat blocks
)

/**
 * Heart rate data point for completion detail
 */
@Serializable
data class HeartRateDataPoint(
    val timestamp: Instant,
    val bpm: Int
)

/**
 * Device info for completion detail
 */
@Serializable
data class CompletionDeviceInfo(
    val model: String? = null,
    val platform: String? = null,
    @SerialName("os_version")
    val osVersion: String? = null
) {
    val displayName: String
        get() {
            model?.let {
                return when {
                    it.startsWith("Pixel") -> it
                    it.startsWith("SM-") -> "Samsung Galaxy"
                    else -> it
                }
            }
            platform?.let {
                return when (it.lowercase()) {
                    "android" -> "Android"
                    "wear_os" -> "Wear OS"
                    else -> it.replaceFirstChar { c -> c.uppercase() }
                }
            }
            return "Unknown Device"
        }
}

// =============================================================================
// Execution Log Models (AMA-292)
// =============================================================================

/**
 * Status of an interval or set execution
 */
@Serializable
enum class IntervalStatus {
    @SerialName("completed") COMPLETED,
    @SerialName("skipped") SKIPPED,
    @SerialName("not_reached") NOT_REACHED
}

/**
 * Weight component for detailed weight tracking
 */
@Serializable
data class WeightComponent(
    val source: String,
    val value: Double? = null,
    val unit: String? = null
)

/**
 * Weight entry with components and display label
 */
@Serializable
data class WeightEntry(
    val components: List<WeightComponent> = emptyList(),
    @SerialName("display_label")
    val displayLabel: String
)

/**
 * Execution data for a single set within an interval
 */
@Serializable
data class SetLog(
    @SerialName("set_number")
    val setNumber: Int,
    val status: IntervalStatus = IntervalStatus.COMPLETED,
    @SerialName("duration_seconds")
    val durationSeconds: Int? = null,
    @SerialName("reps_planned")
    val repsPlanned: Int? = null,
    @SerialName("reps_completed")
    val repsCompleted: Int? = null,
    val weight: WeightEntry? = null,
    val rpe: Int? = null,
    val modified: Boolean? = null,
    @SerialName("skip_reason")
    val skipReason: String? = null
)

/**
 * Execution data for a single interval
 */
@Serializable
data class IntervalLog(
    @SerialName("interval_index")
    val intervalIndex: Int,
    @SerialName("planned_name")
    val plannedName: String? = null,
    @SerialName("planned_kind")
    val plannedKind: String? = null,
    val status: IntervalStatus = IntervalStatus.COMPLETED,
    @SerialName("planned_duration_seconds")
    val plannedDurationSeconds: Int? = null,
    @SerialName("actual_duration_seconds")
    val actualDurationSeconds: Int? = null,
    @SerialName("planned_sets")
    val plannedSets: Int? = null,
    @SerialName("planned_reps")
    val plannedReps: Int? = null,
    val sets: List<SetLog>? = null,
    @SerialName("skip_reason")
    val skipReason: String? = null
)

/**
 * Summary statistics for execution log
 */
@Serializable
data class ExecutionSummary(
    @SerialName("total_intervals")
    val totalIntervals: Int = 0,
    val completed: Int = 0,
    val skipped: Int = 0,
    @SerialName("not_reached")
    val notReached: Int = 0,
    @SerialName("completion_percentage")
    val completionPercentage: Double = 0.0,
    @SerialName("total_sets")
    val totalSets: Int = 0,
    @SerialName("sets_completed")
    val setsCompleted: Int = 0,
    @SerialName("sets_skipped")
    val setsSkipped: Int = 0,
    @SerialName("total_duration_seconds")
    val totalDurationSeconds: Int = 0,
    @SerialName("active_duration_seconds")
    val activeDurationSeconds: Int = 0
)

/**
 * Full execution log structure (v2 format)
 */
@Serializable
data class ExecutionLog(
    val version: Int = 2,
    val intervals: List<IntervalLog> = emptyList(),
    val summary: ExecutionSummary? = null
)

/**
 * Extended workout completion detail
 */
@Serializable
data class WorkoutCompletionDetail(
    val id: String,
    @SerialName("workout_name")
    val workoutName: String,
    @SerialName("started_at")
    val startedAt: Instant,
    @SerialName("ended_at")
    val endedAt: Instant? = null,
    @SerialName("duration_seconds")
    val durationSeconds: Int,
    @SerialName("avg_heart_rate")
    val avgHeartRate: Int? = null,
    @SerialName("max_heart_rate")
    val maxHeartRate: Int? = null,
    @SerialName("min_heart_rate")
    val minHeartRate: Int? = null,
    @SerialName("active_calories")
    val activeCalories: Int? = null,
    @SerialName("total_calories")
    val totalCalories: Int? = null,
    val steps: Int? = null,
    @SerialName("distance_meters")
    val distanceMeters: Int? = null,
    val source: CompletionSource,
    @SerialName("device_info")
    val deviceInfo: CompletionDeviceInfo? = null,
    @SerialName("heart_rate_samples")
    val heartRateSamples: List<HeartRateDataPoint>? = null,
    @SerialName("synced_to_strava")
    val syncedToStrava: Boolean? = null,
    @SerialName("strava_activity_id")
    val stravaActivityId: String? = null,
    @SerialName("workout_id")
    val workoutId: String? = null,
    @SerialName("workout_structure")
    val workoutStructure: List<WorkoutIntervalSubmission>? = null,
    @SerialName("execution_log")
    val executionLog: ExecutionLog? = null  // AMA-292: Actual execution data
) {
    // AMA-292: Helper to check if we have execution log data
    val hasExecutionLog: Boolean
        get() = executionLog != null && executionLog.intervals.isNotEmpty()

    // AMA-292: Get exercises from execution log (intervals with sets)
    val exercises: List<IntervalLog>
        get() = executionLog?.intervals?.filter { it.plannedKind == "reps" && !it.sets.isNullOrEmpty() } ?: emptyList()
    val resolvedEndedAt: Instant
        get() = endedAt ?: (startedAt + kotlin.time.Duration.parse("${durationSeconds}s"))

    val isSyncedToStrava: Boolean
        get() = syncedToStrava ?: false

    val hasHeartRateData: Boolean
        get() = avgHeartRate != null || !heartRateSamples.isNullOrEmpty()

    val hasHeartRateSamples: Boolean
        get() = !heartRateSamples.isNullOrEmpty()

    val hasSummaryMetrics: Boolean
        get() = activeCalories != null || steps != null || distanceMeters != null

    val formattedDuration: String
        get() {
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            val seconds = durationSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%d:%02d", minutes, seconds)
            }
        }
}

/**
 * Voice workout parsing request
 */
@Serializable
data class VoiceWorkoutRequest(
    val transcript: String,
    val sport: String? = null
)

/**
 * Voice workout parsing response
 */
@Serializable
data class VoiceWorkoutResponse(
    val workout: Workout? = null,
    val error: String? = null,
    @SerialName("raw_response")
    val rawResponse: String? = null
)

/**
 * Completions list response wrapper
 */
@Serializable
data class CompletionsResponse(
    val success: Boolean,
    val completions: List<WorkoutCompletion>,
    val total: Int
)

/**
 * Single completion detail response wrapper
 */
@Serializable
data class CompletionDetailResponse(
    val success: Boolean,
    val completion: WorkoutCompletionDetail
)

/**
 * Pushed workouts response wrapper
 */
@Serializable
data class PushedWorkoutsResponse(
    val success: Boolean,
    val workouts: List<Workout> = emptyList(),
    val message: String? = null
)

/**
 * AMA-323: Response from POST /workouts/complete.
 * The API returns {"success": true, "id": "...", "summary": {...}} — NOT a full WorkoutCompletion.
 */
@Serializable
data class WorkoutCompletionSaveResponse(
    val success: Boolean,
    val id: String? = null
    // summary is intentionally omitted — ignoreUnknownKeys = true handles the field safely
)

/**
 * Single workout response wrapper
 */
@Serializable
data class WorkoutResponse(
    val success: Boolean,
    val workout: Workout? = null,
    val message: String? = null
)

// AMA-1258: Share Intent URL Import

/**
 * Generic URL import request — backend auto-detects platform from URL
 */
@Serializable
data class UrlImportRequest(
    val url: String
)

// AMA-307: Sync Queue Models

/**
 * Request to confirm a successful workout sync
 */
@Serializable
data class ConfirmSyncRequest(
    @SerialName("workout_id")
    val workoutId: String,
    @SerialName("device_type")
    val deviceType: String = "android",
    @SerialName("device_id")
    val deviceId: String? = null
)

/**
 * Request to report a failed workout sync
 */
@Serializable
data class ReportSyncFailedRequest(
    @SerialName("workout_id")
    val workoutId: String,
    @SerialName("device_type")
    val deviceType: String = "android",
    val error: String,
    @SerialName("device_id")
    val deviceId: String? = null
)

/**
 * Response for sync confirmation/failure endpoints
 */
@Serializable
data class SyncStatusResponse(
    val success: Boolean,
    val message: String? = null
)
