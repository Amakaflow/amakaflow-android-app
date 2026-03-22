package com.amakaflow.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Workout data synced from phone to watch via DataLayer.
 * Compact representation suitable for watch display and execution.
 */
@Serializable
data class WearWorkout(
    val id: String,
    val name: String,
    val sport: String,
    val duration: Int, // total seconds
    val intervals: List<WearInterval> = emptyList(),
    val description: String? = null,
    @SerialName("scheduled_date")
    val scheduledDate: String? = null,
    @SerialName("scheduled_time")
    val scheduledTime: String? = null
) {
    val formattedDuration: String
        get() {
            val hours = duration / 3600
            val minutes = (duration % 3600) / 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "${duration}s"
            }
        }
}

/**
 * Simplified interval for watch-side execution.
 */
@Serializable
data class WearInterval(
    val kind: String, // warmup, cooldown, time, reps, rest, distance
    val name: String? = null,
    val seconds: Int? = null,
    val reps: Int? = null,
    val sets: Int? = null,
    val load: String? = null,
    @SerialName("rest_seconds")
    val restSeconds: Int? = null,
    @SerialName("round_info")
    val roundInfo: String? = null
)

/**
 * Workout completion sent from watch back to phone.
 */
@Serializable
data class WearWorkoutCompletion(
    @SerialName("workout_id")
    val workoutId: String,
    @SerialName("workout_name")
    val workoutName: String,
    @SerialName("started_at")
    val startedAt: Long, // epoch millis
    @SerialName("ended_at")
    val endedAt: Long,
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
    val steps: Int? = null
)

/**
 * Today's readiness data synced from phone to watch.
 */
@Serializable
data class WearReadinessData(
    val score: Int, // 0-100
    val label: String, // "Ready", "Moderate", "Rest"
    val factors: List<ReadinessFactor> = emptyList()
)

/**
 * Factor contributing to readiness score.
 */
@Serializable
data class ReadinessFactor(
    val name: String,
    val value: String,
    val status: String // "good", "moderate", "poor"
)

/**
 * Day schedule synced to watch.
 */
@Serializable
data class WearDaySchedule(
    val date: String, // ISO date
    val workouts: List<WearScheduleEntry> = emptyList()
)

/**
 * Single schedule entry.
 */
@Serializable
data class WearScheduleEntry(
    @SerialName("workout_id")
    val workoutId: String,
    @SerialName("workout_name")
    val workoutName: String,
    val sport: String,
    @SerialName("scheduled_time")
    val scheduledTime: String? = null,
    val duration: Int,
    @SerialName("is_completed")
    val isCompleted: Boolean = false
)

/**
 * Health snapshot from watch.
 */
@Serializable
data class WearHealthSnapshot(
    @SerialName("heart_rate")
    val heartRate: Int? = null,
    val steps: Int? = null,
    @SerialName("calories_burned")
    val caloriesBurned: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)
