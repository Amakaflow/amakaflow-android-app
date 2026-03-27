package com.amakaflow.companion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Challenge Models (AMA-1276)
// =============================================================================

@Serializable
enum class ChallengeType {
    @SerialName("volume") VOLUME,
    @SerialName("consistency") CONSISTENCY,
    @SerialName("pr") PR;

    val displayName: String
        get() = when (this) {
            VOLUME -> "Volume"
            CONSISTENCY -> "Consistency"
            PR -> "PR"
        }
}

@Serializable
enum class ChallengeStatus {
    @SerialName("active") ACTIVE,
    @SerialName("upcoming") UPCOMING,
    @SerialName("completed") COMPLETED,
    @SerialName("cancelled") CANCELLED
}

@Serializable
data class Challenge(
    val id: String,
    val title: String,
    val type: ChallengeType,
    val status: ChallengeStatus,
    val description: String? = null,
    val target: Double,
    @SerialName("target_unit")
    val targetUnit: String,
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String,
    @SerialName("creator_id")
    val creatorId: String,
    @SerialName("creator_name")
    val creatorName: String,
    @SerialName("participant_count")
    val participantCount: Int = 0,
    @SerialName("is_team_mode")
    val isTeamMode: Boolean = false,
    @SerialName("is_joined")
    val isJoined: Boolean = false,
    @SerialName("my_progress")
    val myProgress: Double? = null,
    @SerialName("my_progress_percentage")
    val myProgressPercentage: Double? = null
)

@Serializable
data class LeaderboardEntry(
    val id: String,
    val rank: Int,
    @SerialName("user_id")
    val userId: String,
    @SerialName("user_name")
    val userName: String,
    @SerialName("user_avatar_url")
    val userAvatarUrl: String? = null,
    val progress: Double,
    @SerialName("progress_percentage")
    val progressPercentage: Double
)

@Serializable
data class ChallengeProgress(
    @SerialName("challenge_id")
    val challengeId: String,
    @SerialName("current_value")
    val currentValue: Double,
    @SerialName("target_value")
    val targetValue: Double,
    val percentage: Double,
    @SerialName("is_completed")
    val isCompleted: Boolean = false,
    @SerialName("completed_at")
    val completedAt: String? = null,
    val badge: ChallengeBadge? = null
)

@Serializable
data class ChallengeBadge(
    val id: String,
    val name: String,
    @SerialName("icon_name")
    val iconName: String,
    val description: String
)

@Serializable
data class CreateChallengeRequest(
    val title: String,
    val type: ChallengeType,
    val description: String? = null,
    val target: Double,
    @SerialName("target_unit")
    val targetUnit: String,
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String,
    @SerialName("is_team_mode")
    val isTeamMode: Boolean = false
)

// API Responses

@Serializable
data class ChallengesResponse(
    val challenges: List<Challenge> = emptyList()
)

@Serializable
data class ChallengeDetailResponse(
    val challenge: Challenge,
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    @SerialName("my_progress")
    val myProgress: ChallengeProgress? = null
)
