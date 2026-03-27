package com.amakaflow.companion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Training Crews Models (AMA-1277)
// =============================================================================

@Serializable
data class Crew(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("created_by")
    val createdBy: String,
    @SerialName("max_members")
    val maxMembers: Int = 8,
    @SerialName("invite_code")
    val inviteCode: String,
    @SerialName("member_count")
    val memberCount: Int = 0,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class CrewMember(
    @SerialName("user_id")
    val userId: String,
    val role: String,
    @SerialName("joined_at")
    val joinedAt: String
) {
    val isAdmin: Boolean get() = role == "admin"
}

@Serializable
data class CrewDetail(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("created_by")
    val createdBy: String,
    @SerialName("max_members")
    val maxMembers: Int = 8,
    @SerialName("invite_code")
    val inviteCode: String,
    val members: List<CrewMember> = emptyList(),
    @SerialName("member_count")
    val memberCount: Int = 0,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class CrewFeedPost(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    val content: Map<String, String> = emptyMap(),
    @SerialName("photo_url")
    val photoUrl: String? = null,
    @SerialName("pr_badges")
    val prBadges: List<String> = emptyList(),
    @SerialName("created_at")
    val createdAt: String
) {
    val workoutName: String get() = content["workout_name"] ?: "Workout"
}

// API Requests

@Serializable
data class CreateCrewApiRequest(
    val name: String,
    val description: String? = null,
    @SerialName("max_members")
    val maxMembers: Int = 8
)

@Serializable
data class JoinCrewApiRequest(
    @SerialName("invite_code")
    val inviteCode: String
)

// API Responses

@Serializable
data class CrewListResponse(
    val crews: List<Crew> = emptyList(),
    val count: Int = 0
)

@Serializable
data class CrewFeedResponse(
    val posts: List<CrewFeedPost> = emptyList(),
    @SerialName("next_cursor")
    val nextCursor: String? = null
)

@Serializable
data class JoinCrewApiResponse(
    val status: String,
    @SerialName("crew_id")
    val crewId: String,
    @SerialName("user_id")
    val userId: String
)

@Serializable
data class LeaveCrewApiResponse(
    val status: String,
    @SerialName("crew_id")
    val crewId: String,
    @SerialName("user_id")
    val userId: String
)
