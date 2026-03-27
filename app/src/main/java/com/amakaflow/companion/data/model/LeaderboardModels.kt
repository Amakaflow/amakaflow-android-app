package com.amakaflow.companion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Multi-dimension Leaderboard Models (AMA-1278)
// =============================================================================

@Serializable
data class DimensionLeaderboardEntry(
    val rank: Int,
    @SerialName("user_id")
    val userId: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val value: Double,
    @SerialName("is_me")
    val isMe: Boolean = false
)

@Serializable
data class DimensionLeaderboardResponse(
    val dimension: String,
    val period: String,
    val entries: List<DimensionLeaderboardEntry> = emptyList()
)
