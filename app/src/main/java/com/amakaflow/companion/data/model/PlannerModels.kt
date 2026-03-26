package com.amakaflow.companion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// DayState Models
// =============================================================================

/**
 * Represents the state of a single day on the calendar.
 * Returned by GET /calendar/day-state?date=YYYY-MM-DD
 */
@Serializable
data class DayState(
    val date: String,
    val status: DayStatus = DayStatus.REST,
    val workouts: List<DayWorkoutSummary> = emptyList(),
    @SerialName("fatigue_level")
    val fatigueLevel: Double? = null,
    @SerialName("readiness_score")
    val readinessScore: Double? = null,
    val conflicts: List<ScheduleConflict> = emptyList(),
    val notes: String? = null
)

@Serializable
enum class DayStatus {
    @SerialName("rest") REST,
    @SerialName("easy") EASY,
    @SerialName("moderate") MODERATE,
    @SerialName("hard") HARD,
    @SerialName("race") RACE
}

@Serializable
data class DayWorkoutSummary(
    val id: String,
    val name: String,
    val sport: WorkoutSport,
    val duration: Int,
    @SerialName("scheduled_time")
    val scheduledTime: String? = null,
    val completed: Boolean = false
)

/**
 * Response for GET /calendar/day-state
 */
@Serializable
data class DayStateResponse(
    val success: Boolean,
    @SerialName("day_state")
    val dayState: DayState
)

/**
 * Response for GET /calendar/week-state?start=YYYY-MM-DD
 */
@Serializable
data class WeekStateResponse(
    val success: Boolean,
    val days: List<DayState> = emptyList()
)

// =============================================================================
// Planner / "Generate My Week" Models
// =============================================================================

/**
 * Request body for POST /planner/generate-week
 */
@Serializable
data class GenerateWeekRequest(
    @SerialName("start_date")
    val startDate: String,
    @SerialName("goal_description")
    val goalDescription: String? = null,
    val preferences: PlannerPreferences? = null
)

@Serializable
data class PlannerPreferences(
    @SerialName("days_per_week")
    val daysPerWeek: Int? = null,
    @SerialName("max_duration_minutes")
    val maxDurationMinutes: Int? = null,
    @SerialName("preferred_sports")
    val preferredSports: List<String>? = null,
    @SerialName("avoid_days")
    val avoidDays: List<Int>? = null
)

/**
 * Response from POST /planner/generate-week
 */
@Serializable
data class GenerateWeekResponse(
    val success: Boolean,
    val plan: WeekPlan? = null,
    val message: String? = null
)

@Serializable
data class WeekPlan(
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String,
    val days: List<PlannedDay> = emptyList(),
    val summary: String? = null
)

@Serializable
data class PlannedDay(
    val date: String,
    val workouts: List<Workout> = emptyList(),
    val notes: String? = null,
    @SerialName("is_rest_day")
    val isRestDay: Boolean = false
)

// =============================================================================
// Coach Chat Models
// =============================================================================

/**
 * Request body for POST /coach/message
 */
@Serializable
data class CoachMessageRequest(
    val message: String,
    @SerialName("conversation_id")
    val conversationId: String? = null,
    val context: CoachContext? = null
)

@Serializable
data class CoachContext(
    @SerialName("current_date")
    val currentDate: String? = null,
    @SerialName("recent_workouts")
    val recentWorkouts: Int? = null
)

/**
 * Response from POST /coach/message
 */
@Serializable
data class CoachMessageResponse(
    val success: Boolean,
    val reply: String? = null,
    @SerialName("conversation_id")
    val conversationId: String? = null,
    val suggestions: List<String> = emptyList(),
    val actions: List<CoachAction> = emptyList()
)

@Serializable
data class CoachAction(
    val type: String,
    val label: String,
    val payload: String? = null
)

// =============================================================================
// Activity Feed Models
// =============================================================================

/**
 * Response from GET /activity/feed
 */
@Serializable
data class ActivityFeedResponse(
    val success: Boolean,
    val items: List<ActivityFeedItem> = emptyList(),
    @SerialName("has_more")
    val hasMore: Boolean = false,
    @SerialName("next_cursor")
    val nextCursor: String? = null
)

@Serializable
data class ActivityFeedItem(
    val id: String,
    val type: ActivityType,
    val title: String,
    val subtitle: String? = null,
    val timestamp: String,
    val icon: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
enum class ActivityType {
    @SerialName("workout_completed") WORKOUT_COMPLETED,
    @SerialName("plan_generated") PLAN_GENERATED,
    @SerialName("streak_milestone") STREAK_MILESTONE,
    @SerialName("coach_insight") COACH_INSIGHT,
    @SerialName("pr_achieved") PR_ACHIEVED,
    @SerialName("rest_day") REST_DAY
}

// =============================================================================
// Training Preferences Models
// =============================================================================

/**
 * Response/request for GET/PUT /preferences/training
 */
@Serializable
data class TrainingPreferences(
    @SerialName("days_per_week")
    val daysPerWeek: Int = 4,
    @SerialName("preferred_sports")
    val preferredSports: List<String> = listOf("running", "strength"),
    @SerialName("max_session_minutes")
    val maxSessionMinutes: Int = 60,
    @SerialName("fitness_level")
    val fitnessLevel: String = "intermediate",
    @SerialName("goal")
    val goal: String? = null,
    @SerialName("avoid_days")
    val avoidDays: List<Int> = emptyList(),
    @SerialName("preferred_time")
    val preferredTime: String? = null
)

@Serializable
data class TrainingPreferencesResponse(
    val success: Boolean,
    val preferences: TrainingPreferences
)

// =============================================================================
// Conflict Detection Models
// =============================================================================

@Serializable
data class ScheduleConflict(
    val id: String? = null,
    val type: ConflictType = ConflictType.TIME_OVERLAP,
    val message: String,
    @SerialName("workout_ids")
    val workoutIds: List<String> = emptyList(),
    val severity: ConflictSeverity = ConflictSeverity.WARNING
)

@Serializable
enum class ConflictType {
    @SerialName("time_overlap") TIME_OVERLAP,
    @SerialName("fatigue_risk") FATIGUE_RISK,
    @SerialName("insufficient_recovery") INSUFFICIENT_RECOVERY,
    @SerialName("overtraining") OVERTRAINING
}

@Serializable
enum class ConflictSeverity {
    @SerialName("info") INFO,
    @SerialName("warning") WARNING,
    @SerialName("critical") CRITICAL
}

@Serializable
data class ConflictsResponse(
    val success: Boolean,
    val conflicts: List<ScheduleConflict> = emptyList()
)

// =============================================================================
// Shoe Comparison Models
// =============================================================================

@Serializable
data class ShoeComparisonRequest(
    val shoes: List<String>,
    val sport: String? = null,
    val preference: String? = null
)

@Serializable
data class ShoeComparisonResponse(
    val success: Boolean,
    val comparison: ShoeComparison? = null
)

@Serializable
data class ShoeComparison(
    val shoes: List<ShoeDetail> = emptyList(),
    val recommendation: String? = null,
    @SerialName("best_for")
    val bestFor: Map<String, String> = emptyMap()
)

@Serializable
data class ShoeDetail(
    val name: String,
    val brand: String? = null,
    val rating: Double? = null,
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList(),
    @SerialName("best_for")
    val bestFor: String? = null,
    @SerialName("price_range")
    val priceRange: String? = null
)

// =============================================================================
// Fatigue Advisor Models
// =============================================================================

@Serializable
data class FatigueAdvisorResponse(
    val success: Boolean,
    @SerialName("fatigue_score")
    val fatigueScore: Double = 0.0,
    val level: FatigueLevel = FatigueLevel.LOW,
    val advice: String? = null,
    val recommendations: List<String> = emptyList(),
    @SerialName("contributing_factors")
    val contributingFactors: List<FatigueFactor> = emptyList()
)

@Serializable
enum class FatigueLevel {
    @SerialName("low") LOW,
    @SerialName("moderate") MODERATE,
    @SerialName("high") HIGH,
    @SerialName("very_high") VERY_HIGH
}

@Serializable
data class FatigueFactor(
    val factor: String,
    val impact: Double = 0.0,
    val description: String? = null
)

// =============================================================================
// RPE Feedback Models (AMA-1266)
// =============================================================================

@Serializable
data class RPEFeedbackRequest(
    @SerialName("workout_id")
    val workoutId: String,
    val rpe: Int,
    @SerialName("muscle_soreness")
    val muscleSoreness: List<String>? = null,
    val notes: String? = null
)

@Serializable
data class RPEFeedbackResponse(
    val success: Boolean,
    val message: String = "",
    @SerialName("deload_recommended")
    val deloadRecommended: Boolean? = null
)
