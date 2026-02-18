package com.amakaflow.companion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Device types that workouts can be synced to
 */
@Serializable
enum class SyncDeviceType {
    @SerialName("apple_watch") APPLE_WATCH,
    @SerialName("garmin") GARMIN,
    @SerialName("wear_os") WEAR_OS;

    companion object {
        fun fromString(value: String): SyncDeviceType {
            return when (value.lowercase()) {
                "apple_watch" -> APPLE_WATCH
                "garmin" -> GARMIN
                "wear_os", "android_wear" -> WEAR_OS
                else -> WEAR_OS
            }
        }
    }
}

/**
 * Sync status states for a workout
 */
@Serializable
enum class SyncState {
    @SerialName("not_assigned") NOT_ASSIGNED,
    @SerialName("pending") PENDING,
    @SerialName("syncing") SYNCING,
    @SerialName("synced") SYNCED,
    @SerialName("failed") FAILED,
    @SerialName("outdated") OUTDATED;

    companion object {
        fun fromString(value: String): SyncState {
            return when (value.lowercase()) {
                "not_assigned" -> NOT_ASSIGNED
                "pending" -> PENDING
                "syncing" -> SYNCING
                "synced" -> SYNCED
                "failed" -> FAILED
                "outdated" -> OUTDATED
                else -> NOT_ASSIGNED
            }
        }
    }
}

/**
 * Sync status for a workout on a specific device
 */
@Serializable
data class WorkoutSyncStatus(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_type") val deviceType: SyncDeviceType,
    val status: SyncState,
    @SerialName("last_sync_at") val lastSyncAt: String? = null,
    @SerialName("error_message") val errorMessage: String? = null,
    @SerialName("queued_at") val queuedAt: String? = null
)

/**
 * Workout sport types
 */
@Serializable(with = WorkoutSportSerializer::class)
enum class WorkoutSport {
    RUNNING,
    CYCLING,
    STRENGTH,
    MOBILITY,
    SWIMMING,
    CARDIO,
    OTHER;

    companion object {
        fun fromString(value: String): WorkoutSport {
            return when (value.lowercase()) {
                "running", "run" -> RUNNING
                "cycling", "bike", "biking" -> CYCLING
                "strength", "strengthtraining", "strength_training", "weights" -> STRENGTH
                "mobility", "yoga", "stretching", "flexibility" -> MOBILITY
                "swimming", "swim" -> SWIMMING
                "cardio", "hiit" -> CARDIO
                else -> OTHER
            }
        }

        fun toApiString(sport: WorkoutSport): String {
            return when (sport) {
                RUNNING -> "running"
                CYCLING -> "cycling"
                STRENGTH -> "strength"
                MOBILITY -> "mobility"
                SWIMMING -> "swimming"
                CARDIO -> "cardio"
                OTHER -> "other"
            }
        }
    }
}

/**
 * Custom serializer for WorkoutSport that handles multiple API values
 */
object WorkoutSportSerializer : kotlinx.serialization.KSerializer<WorkoutSport> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
        "WorkoutSport",
        kotlinx.serialization.descriptors.PrimitiveKind.STRING
    )

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: WorkoutSport) {
        encoder.encodeString(WorkoutSport.toApiString(value))
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): WorkoutSport {
        return WorkoutSport.fromString(decoder.decodeString())
    }
}

/**
 * Workout source types
 */
@Serializable(with = WorkoutSourceSerializer::class)
enum class WorkoutSource {
    INSTAGRAM,
    YOUTUBE,
    IMAGE,
    AI,
    COACH,
    AMAKA,
    OTHER;

    companion object {
        fun fromString(value: String): WorkoutSource {
            return when (value.lowercase()) {
                "instagram" -> INSTAGRAM
                "youtube" -> YOUTUBE
                "image" -> IMAGE
                "ai" -> AI
                "coach" -> COACH
                "amaka", "amakaflow" -> AMAKA
                else -> OTHER
            }
        }

        fun toApiString(source: WorkoutSource): String {
            return when (source) {
                INSTAGRAM -> "instagram"
                YOUTUBE -> "youtube"
                IMAGE -> "image"
                AI -> "ai"
                COACH -> "coach"
                AMAKA -> "amaka"
                OTHER -> "other"
            }
        }
    }
}

/**
 * Custom serializer for WorkoutSource that handles multiple API values
 */
object WorkoutSourceSerializer : kotlinx.serialization.KSerializer<WorkoutSource> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor(
        "WorkoutSource",
        kotlinx.serialization.descriptors.PrimitiveKind.STRING
    )

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: WorkoutSource) {
        encoder.encodeString(WorkoutSource.toApiString(value))
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): WorkoutSource {
        return WorkoutSource.fromString(decoder.decodeString())
    }
}

/**
 * Main workout model
 */
@Serializable
data class Workout(
    val id: String,
    val name: String,
    val sport: WorkoutSport,
    val duration: Int, // seconds
    val intervals: List<WorkoutInterval> = emptyList(),
    val description: String? = null,
    val source: WorkoutSource,
    val sourceUrl: String? = null
) {
    val formattedDuration: String
        get() = WorkoutHelpers.formatDuration(duration)

    val intervalCount: Int
        get() = WorkoutHelpers.countIntervals(intervals)
}

/**
 * Scheduled workout with recurrence support
 */
@Serializable
data class ScheduledWorkout(
    val workout: Workout,
    val scheduledDate: String? = null, // ISO date string
    val scheduledTime: String? = null,
    val isRecurring: Boolean = false,
    val recurrenceDays: List<Int>? = null, // 0 = Sunday, 6 = Saturday
    val recurrenceWeeks: Int? = null,
    val syncedToGoogle: Boolean = false
) {
    val id: String get() = workout.id
}

/**
 * Helper functions for workout formatting
 */
object WorkoutHelpers {
    fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${secs}s"
        }
    }

    fun countIntervals(intervals: List<WorkoutInterval>): Int {
        var count = 0
        for (interval in intervals) {
            when (interval) {
                is WorkoutInterval.Repeat -> {
                    count += interval.reps * countIntervals(interval.intervals)
                }
                else -> count += 1
            }
        }
        return count
    }

    fun formatDistance(meters: Int): String {
        return if (meters >= 1000) {
            val km = meters / 1000.0
            String.format("%.1f km", km)
        } else {
            "${meters}m"
        }
    }

    fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return when {
            hours > 0 -> if (secs > 0) "${hours}h ${minutes}m ${secs}s" else "${hours}h ${minutes}m"
            minutes > 0 -> if (secs > 0) "${minutes}m ${secs}s" else "${minutes} min"
            else -> "${secs}s"
        }
    }
}
