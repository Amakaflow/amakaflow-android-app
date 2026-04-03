package com.amakaflow.companion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Volume Analytics API models — matches iOS VolumeAnalytics structs.
 */

@Serializable
data class VolumeAnalyticsResponse(
    val data: List<VolumeDataPoint> = emptyList(),
    val summary: VolumeSummary,
    val period: VolumePeriod,
    val granularity: String
)

@Serializable
data class VolumeDataPoint(
    val period: String,
    @SerialName("muscle_group")
    val muscleGroup: String,
    @SerialName("total_volume")
    val totalVolume: Double,
    @SerialName("total_sets")
    val totalSets: Int,
    @SerialName("total_reps")
    val totalReps: Int
)

@Serializable
data class VolumeSummary(
    @SerialName("total_volume")
    val totalVolume: Double,
    @SerialName("total_sets")
    val totalSets: Int,
    @SerialName("total_reps")
    val totalReps: Int,
    @SerialName("muscle_group_breakdown")
    val muscleGroupBreakdown: Map<String, Double> = emptyMap()
)

@Serializable
data class VolumePeriod(
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String
)
