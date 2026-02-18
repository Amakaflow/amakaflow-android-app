package com.amakaflow.companion.domain

import com.amakaflow.companion.data.model.WorkoutHelpers
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for Workout formatting helpers
 * Part of AMA-666: Android Phase 1 - Core unit tests
 */
class WorkoutHelpersTest {

    // ==================== Duration Formatting Tests ====================

    @Test
    fun formatDuration_seconds_only() {
        assertThat(WorkoutHelpers.formatDuration(30)).isEqualTo("30m")
    }

    @Test
    fun formatDuration_minutes_only() {
        assertThat(WorkoutHelpers.formatDuration(1800)).isEqualTo("30m")
    }

    @Test
    fun formatDuration_hours_and_minutes() {
        assertThat(WorkoutHelpers.formatDuration(3600)).isEqualTo("1h 0m")
        assertThat(WorkoutHelpers.formatDuration(5400)).isEqualTo("1h 30m")
        assertThat(WorkoutHelpers.formatDuration(7200)).isEqualTo("2h 0m")
        assertThat(WorkoutHelpers.formatDuration(9000)).isEqualTo("2h 30m")
    }

    @Test
    fun formatDuration_edge_cases() {
        // Zero
        assertThat(WorkoutHelpers.formatDuration(0)).isEqualTo("0m")
        
        // Very small
        assertThat(WorkoutHelpers.formatDuration(30)).isEqualTo("30m")
        
        // Very large
        assertThat(WorkoutHelpers.formatDuration(36000)).isEqualTo("10h 0m")
    }

    @Test
    fun formatDuration_no_hours() {
        assertThat(WorkoutHelpers.formatDuration(2700)).isEqualTo("45m")
    }

    // ==================== Distance Formatting Tests ====================

    @Test
    fun formatDistance_meters_only() {
        assertThat(WorkoutHelpers.formatDistance(100)).isEqualTo("100m")
        assertThat(WorkoutHelpers.formatDistance(400)).isEqualTo("400m")
        assertThat(WorkoutHelpers.formatDistance(900)).isEqualTo("900m")
    }

    @Test
    fun formatDistance_kilometers() {
        assertThat(WorkoutHelpers.formatDistance(1000)).isEqualTo("1.0 km")
        assertThat(WorkoutHelpers.formatDistance(1500)).isEqualTo("1.5 km")
        assertThat(WorkoutHelpers.formatDistance(2000)).isEqualTo("2.0 km")
        assertThat(WorkoutHelpers.formatDistance(5000)).isEqualTo("5.0 km")
        assertThat(WorkoutHelpers.formatDistance(10000)).isEqualTo("10.0 km")
    }

    @Test
    fun formatDistance_edge_cases() {
        // Zero
        assertThat(WorkoutHelpers.formatDistance(0)).isEqualTo("0m")
        
        // Just under 1km
        assertThat(WorkoutHelpers.formatDistance(999)).isEqualTo("999m")
        
        // Exactly 1km
        assertThat(WorkoutHelpers.formatDistance(1000)).isEqualTo("1.0 km")
        
        // Large distance
        assertThat(WorkoutHelpers.formatDistance(42195)).isEqualTo("42.2 km")
    }

    @Test
    fun formatDistance_decimal_rounding() {
        assertThat(WorkoutHelpers.formatDistance(1500)).isEqualTo("1.5 km")
        assertThat(WorkoutHelpers.formatDistance(1600)).isEqualTo("1.6 km")
        assertThat(WorkoutHelpers.formatDistance(1234)).isEqualTo("1.2 km")
    }

    // ==================== Time Formatting Tests ====================

    @Test
    fun formatTime_seconds_only() {
        assertThat(WorkoutHelpers.formatTime(30)).isEqualTo("30s")
        assertThat(WorkoutHelpers.formatTime(10)).isEqualTo("10s")
    }

    @Test
    fun formatTime_minutes_and_seconds() {
        assertThat(WorkoutHelpers.formatTime(60)).isEqualTo("1 min")
        assertThat(WorkoutHelpers.formatTime(90)).isEqualTo("1m 30s")
        assertThat(WorkoutHelpers.formatTime(120)).isEqualTo("2 min")
        assertThat(WorkoutHelpers.formatTime(150)).isEqualTo("2m 30s")
    }

    @Test
    fun formatTime_edge_cases() {
        // Zero
        assertThat(WorkoutHelpers.formatTime(0)).isEqualTo("0s")
        
        // Just under a minute
        assertThat(WorkoutHelpers.formatTime(59)).isEqualTo("59s")
        
        // Exactly one minute
        assertThat(WorkoutHelpers.formatTime(60)).isEqualTo("1 min")
        
        // Large value
        assertThat(WorkoutHelpers.formatTime(3600)).isEqualTo("60 min")
    }

    @Test
    fun formatTime_no_remainder_seconds() {
        assertThat(WorkoutHelpers.formatTime(180)).isEqualTo("3 min")
        assertThat(WorkoutHelpers.formatTime(300)).isEqualTo("5 min")
    }
}
