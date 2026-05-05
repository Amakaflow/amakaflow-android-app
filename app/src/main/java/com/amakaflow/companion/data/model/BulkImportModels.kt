package com.amakaflow.companion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Bulk Import Models (AMA-1408 native parity)
// =============================================================================

/**
 * AMA-1770: matches mapper-api's `BulkDetectRequest` contract
 * (`profile_id`, `source_type`, `sources`). `profile_id` is optional —
 * the server falls back to the authenticated user from the JWT when
 * omitted, so we don't need to wire user_id retrieval into this VM.
 *
 * `source_type` defaults to "urls" since the Android UI only collects
 * URLs today; file/image flows would override.
 */
@Serializable
data class BulkDetectRequest(
    val sources: List<String>,
    @SerialName("source_type")
    val sourceType: String = "urls",
    @SerialName("profile_id")
    val profileId: String? = null,
)

@Serializable
data class BulkDetectResponse(
    val success: Boolean,
    val items: List<DetectedItem> = emptyList(),
    @SerialName("job_id")
    val jobId: String? = null
)

@Serializable
data class DetectedItem(
    val id: String,
    val url: String,
    val platform: String,
    @SerialName("workout_name")
    val workoutName: String? = null,
    @SerialName("workout_date")
    val workoutDate: String? = null,
    val confidence: Double = 0.0,
    val status: String = "pending",
    val error: String? = null
)

@Serializable
data class BulkMatchRequest(
    val items: List<String>,
    @SerialName("profile_id")
    val profileId: String? = null
)

@Serializable
data class BulkMatchResponse(
    val success: Boolean,
    val matches: List<ExerciseMatch> = emptyList()
)

@Serializable
data class ExerciseMatch(
    val id: String,
    @SerialName("source_name")
    val sourceName: String,
    @SerialName("matched_name")
    val matchedName: String? = null,
    @SerialName("match_score")
    val matchScore: Double = 0.0,
    val status: String = "matched",
    val suggestions: List<String> = emptyList()
)

@Serializable
data class BulkPreviewRequest(
    val items: List<String>,
    val matches: Map<String, String> = emptyMap(),
    @SerialName("profile_id")
    val profileId: String? = null
)

@Serializable
data class BulkPreviewResponse(
    val success: Boolean,
    val workouts: List<PreviewWorkout> = emptyList(),
    @SerialName("total_duration_seconds")
    val totalDurationSeconds: Int = 0,
    @SerialName("exercise_count")
    val exerciseCount: Int = 0
)

@Serializable
data class PreviewWorkout(
    val id: String,
    val name: String,
    val date: String? = null,
    @SerialName("duration_seconds")
    val durationSeconds: Int = 0,
    @SerialName("exercise_count")
    val exerciseCount: Int = 0,
    val platform: String? = null,
    val valid: Boolean = true,
    @SerialName("validation_errors")
    val validationErrors: List<String> = emptyList()
)

@Serializable
data class BulkExecuteRequest(
    val items: List<String>,
    val matches: Map<String, String> = emptyMap(),
    @SerialName("profile_id")
    val profileId: String? = null
)

@Serializable
data class BulkExecuteResponse(
    val success: Boolean,
    @SerialName("job_id")
    val jobId: String,
    val status: String
)

@Serializable
data class BulkImportStatus(
    @SerialName("job_id")
    val jobId: String,
    val status: String,
    val progress: Int = 0,
    val total: Int = 0,
    val completed: Int = 0,
    val failed: Int = 0,
    val results: List<ImportResult> = emptyList(),
    val stats: ImportStats? = null,
    val error: String? = null
)

@Serializable
data class ImportResult(
    val id: String,
    val status: String,
    @SerialName("workout_id")
    val workoutId: String? = null,
    val error: String? = null
)

@Serializable
data class ImportStats(
    val imported: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    @SerialName("total_workouts")
    val totalWorkouts: Int = 0
)
