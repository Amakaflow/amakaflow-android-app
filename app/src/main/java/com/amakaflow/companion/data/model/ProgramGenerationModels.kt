package com.amakaflow.companion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Program Generation Models (AMA-1408 native parity)
// =============================================================================

@Serializable
data class ProgramGenerationRequest(
    val goal: String,
    @SerialName("experience_level")
    val experienceLevel: String,
    @SerialName("duration_weeks")
    val durationWeeks: Int,
    @SerialName("sessions_per_week")
    val sessionsPerWeek: Int,
    @SerialName("preferred_days")
    val preferredDays: List<Int>,
    @SerialName("time_per_session")
    val timePerSession: Int,
    val equipment: List<String>,
    val injuries: String?,
    @SerialName("focus_areas")
    val focusAreas: List<String>?,
    @SerialName("avoid_exercises")
    val avoidExercises: List<String>?
)

@Serializable
data class ProgramGenerationResponse(
    @SerialName("job_id")
    val jobId: String,
    val status: String,
    @SerialName("program_id")
    val programId: String? = null,
    val error: String? = null
)

@Serializable
data class ProgramGenerationStatus(
    @SerialName("job_id")
    val jobId: String,
    val status: String,
    val progress: Int,
    @SerialName("program_id")
    val programId: String? = null,
    val error: String? = null
)
