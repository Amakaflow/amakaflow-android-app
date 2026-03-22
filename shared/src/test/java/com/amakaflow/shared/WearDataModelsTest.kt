package com.amakaflow.shared

import com.amakaflow.shared.connectivity.WearDataPaths
import com.amakaflow.shared.model.ReadinessFactor
import com.amakaflow.shared.model.WearDaySchedule
import com.amakaflow.shared.model.WearHealthSnapshot
import com.amakaflow.shared.model.WearInterval
import com.amakaflow.shared.model.WearReadinessData
import com.amakaflow.shared.model.WearScheduleEntry
import com.amakaflow.shared.model.WearWorkout
import com.amakaflow.shared.model.WearWorkoutCompletion
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class WearDataModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    // =========================================================================
    // WearWorkout Tests
    // =========================================================================

    @Test
    fun `WearWorkout serialization round-trip`() {
        val workout = WearWorkout(
            id = "w-123",
            name = "HIIT Blast",
            sport = "cardio",
            duration = 1200,
            intervals = listOf(
                WearInterval(kind = "warmup", name = "Warm Up", seconds = 120),
                WearInterval(kind = "reps", name = "Burpees", reps = 15, restSeconds = 30),
                WearInterval(kind = "time", name = "Mountain Climbers", seconds = 45),
                WearInterval(kind = "cooldown", name = "Cool Down", seconds = 120)
            ),
            description = "Fast-paced HIIT",
            scheduledDate = "2026-03-22",
            scheduledTime = "7:00 AM"
        )

        val encoded = json.encodeToString(workout)
        val decoded = json.decodeFromString<WearWorkout>(encoded)

        assertThat(decoded.id).isEqualTo("w-123")
        assertThat(decoded.name).isEqualTo("HIIT Blast")
        assertThat(decoded.intervals).hasSize(4)
        assertThat(decoded.intervals[1].reps).isEqualTo(15)
        assertThat(decoded.intervals[1].restSeconds).isEqualTo(30)
        assertThat(decoded.scheduledTime).isEqualTo("7:00 AM")
    }

    @Test
    fun `WearWorkout formattedDuration for minutes`() {
        val workout = WearWorkout(id = "1", name = "test", sport = "running", duration = 1800)
        assertThat(workout.formattedDuration).isEqualTo("30m")
    }

    @Test
    fun `WearWorkout formattedDuration for hours and minutes`() {
        val workout = WearWorkout(id = "1", name = "test", sport = "running", duration = 5400)
        assertThat(workout.formattedDuration).isEqualTo("1h 30m")
    }

    @Test
    fun `WearWorkout formattedDuration for seconds`() {
        val workout = WearWorkout(id = "1", name = "test", sport = "running", duration = 45)
        assertThat(workout.formattedDuration).isEqualTo("45s")
    }

    // =========================================================================
    // WearWorkoutCompletion Tests
    // =========================================================================

    @Test
    fun `WearWorkoutCompletion serialization round-trip`() {
        val completion = WearWorkoutCompletion(
            workoutId = "w-123",
            workoutName = "HIIT Blast",
            startedAt = 1711100000000L,
            endedAt = 1711101200000L,
            durationSeconds = 1200,
            avgHeartRate = 145,
            maxHeartRate = 175,
            minHeartRate = 90,
            activeCalories = 350,
            steps = 2000
        )

        val encoded = json.encodeToString(completion)
        val decoded = json.decodeFromString<WearWorkoutCompletion>(encoded)

        assertThat(decoded.workoutId).isEqualTo("w-123")
        assertThat(decoded.durationSeconds).isEqualTo(1200)
        assertThat(decoded.avgHeartRate).isEqualTo(145)
        assertThat(decoded.maxHeartRate).isEqualTo(175)
        assertThat(decoded.activeCalories).isEqualTo(350)
    }

    @Test
    fun `WearWorkoutCompletion with null health metrics`() {
        val completion = WearWorkoutCompletion(
            workoutId = "w-1",
            workoutName = "Quick Run",
            startedAt = 1000L,
            endedAt = 2000L,
            durationSeconds = 1
        )

        val encoded = json.encodeToString(completion)
        val decoded = json.decodeFromString<WearWorkoutCompletion>(encoded)

        assertThat(decoded.avgHeartRate).isNull()
        assertThat(decoded.activeCalories).isNull()
        assertThat(decoded.steps).isNull()
    }

    // =========================================================================
    // WearReadinessData Tests
    // =========================================================================

    @Test
    fun `WearReadinessData serialization`() {
        val readiness = WearReadinessData(
            score = 85,
            label = "Ready",
            factors = listOf(
                ReadinessFactor("Sleep", "8h", "good"),
                ReadinessFactor("HRV", "65ms", "good"),
                ReadinessFactor("Strain", "Medium", "moderate")
            )
        )

        val encoded = json.encodeToString(readiness)
        val decoded = json.decodeFromString<WearReadinessData>(encoded)

        assertThat(decoded.score).isEqualTo(85)
        assertThat(decoded.label).isEqualTo("Ready")
        assertThat(decoded.factors).hasSize(3)
        assertThat(decoded.factors[0].name).isEqualTo("Sleep")
    }

    // =========================================================================
    // WearDaySchedule Tests
    // =========================================================================

    @Test
    fun `WearDaySchedule serialization`() {
        val schedule = WearDaySchedule(
            date = "2026-03-22",
            workouts = listOf(
                WearScheduleEntry(
                    workoutId = "w1",
                    workoutName = "AM Run",
                    sport = "running",
                    scheduledTime = "6:30 AM",
                    duration = 2400,
                    isCompleted = true
                ),
                WearScheduleEntry(
                    workoutId = "w2",
                    workoutName = "PM Strength",
                    sport = "strength",
                    duration = 3600,
                    isCompleted = false
                )
            )
        )

        val encoded = json.encodeToString(schedule)
        val decoded = json.decodeFromString<WearDaySchedule>(encoded)

        assertThat(decoded.date).isEqualTo("2026-03-22")
        assertThat(decoded.workouts).hasSize(2)
        assertThat(decoded.workouts[0].isCompleted).isTrue()
        assertThat(decoded.workouts[1].scheduledTime).isNull()
    }

    // =========================================================================
    // WearHealthSnapshot Tests
    // =========================================================================

    @Test
    fun `WearHealthSnapshot serialization`() {
        val snapshot = WearHealthSnapshot(
            heartRate = 72,
            steps = 8500,
            caloriesBurned = 350,
            timestamp = 1000L
        )

        val encoded = json.encodeToString(snapshot)
        val decoded = json.decodeFromString<WearHealthSnapshot>(encoded)

        assertThat(decoded.heartRate).isEqualTo(72)
        assertThat(decoded.steps).isEqualTo(8500)
        assertThat(decoded.timestamp).isEqualTo(1000L)
    }

    // =========================================================================
    // WearDataPaths Tests
    // =========================================================================

    @Test
    fun `data paths are consistent`() {
        assertThat(WearDataPaths.WORKOUTS_PATH).startsWith("/amakaflow/")
        assertThat(WearDataPaths.SCHEDULE_PATH).startsWith("/amakaflow/")
        assertThat(WearDataPaths.READINESS_PATH).startsWith("/amakaflow/")
        assertThat(WearDataPaths.MSG_WORKOUT_START).startsWith("/amakaflow/msg/")
        assertThat(WearDataPaths.MSG_WORKOUT_COMPLETE).startsWith("/amakaflow/msg/")
        assertThat(WearDataPaths.MSG_HEART_RATE).startsWith("/amakaflow/msg/")
    }

    @Test
    fun `capability names are set`() {
        assertThat(WearDataPaths.PHONE_CAPABILITY).isEqualTo("amakaflow_phone")
        assertThat(WearDataPaths.WATCH_CAPABILITY).isEqualTo("amakaflow_watch")
    }
}
