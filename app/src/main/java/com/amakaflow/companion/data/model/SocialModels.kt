package com.amakaflow.companion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Social / Community Feed Models (AMA-1273)
// =============================================================================

@Serializable
data class SocialFeedResponse(
    val posts: List<SocialFeedPost> = emptyList(),
    @SerialName("next_cursor")
    val nextCursor: String? = null,
    @SerialName("has_more")
    val hasMore: Boolean = false
)

@Serializable
data class SocialFeedPost(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("user_name")
    val userName: String,
    @SerialName("user_avatar_url")
    val userAvatarUrl: String? = null,
    @SerialName("posted_at")
    val postedAt: String,
    @SerialName("workout_name")
    val workoutName: String,
    val exercises: List<SocialFeedExercise> = emptyList(),
    @SerialName("total_volume")
    val totalVolume: Double? = null,
    @SerialName("duration_seconds")
    val durationSeconds: Int = 0,
    @SerialName("personal_records")
    val personalRecords: List<SocialFeedPR> = emptyList(),
    @SerialName("photo_url")
    val photoUrl: String? = null,
    val reactions: List<SocialFeedReaction> = emptyList(),
    @SerialName("comment_count")
    val commentCount: Int = 0,
    @SerialName("user_reactions")
    val userReactions: List<String> = emptyList()
)

@Serializable
data class SocialFeedExercise(
    val name: String,
    val sets: Int? = null,
    val reps: Int? = null,
    val weight: Double? = null
)

@Serializable
data class SocialFeedPR(
    @SerialName("exercise_name")
    val exerciseName: String,
    val metric: String,
    val value: String
)

@Serializable
data class SocialFeedReaction(
    val emoji: String,
    val count: Int
)

@Serializable
data class SocialCommentsResponse(
    val comments: List<SocialComment> = emptyList()
)

@Serializable
data class SocialComment(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("user_name")
    val userName: String,
    @SerialName("user_avatar_url")
    val userAvatarUrl: String? = null,
    val text: String,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class SocialSettings(
    val discoverable: Boolean = true,
    @SerialName("share_workouts")
    val shareWorkouts: Boolean = true,
    @SerialName("hide_weights")
    val hideWeights: Boolean = false
)

@Serializable
data class UserPublicProfile(
    @SerialName("user_id")
    val userId: String,
    @SerialName("user_name")
    val userName: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("workout_count")
    val workoutCount: Int = 0,
    @SerialName("total_volume")
    val totalVolume: Double = 0.0,
    @SerialName("streak_days")
    val streakDays: Int = 0,
    @SerialName("is_following")
    val isFollowing: Boolean = false,
    @SerialName("recent_workouts")
    val recentWorkouts: List<SocialFeedPost> = emptyList()
)

@Serializable
data class ReactRequest(val emoji: String)

@Serializable
data class CommentRequest(val text: String)
