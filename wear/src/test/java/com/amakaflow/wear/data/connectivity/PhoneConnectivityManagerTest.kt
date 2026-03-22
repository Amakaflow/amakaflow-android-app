package com.amakaflow.wear.data.connectivity

import com.amakaflow.shared.model.WearDaySchedule
import com.amakaflow.shared.model.WearReadinessData
import com.amakaflow.shared.model.WearScheduleEntry
import com.amakaflow.shared.model.WearWorkout
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

/**
 * Tests for PhoneConnectivityManager data parsing.
 * Tests the data reception callbacks that process JSON from the phone.
 *
 * Note: sendToPhone/checkPhoneConnection require Google Play Services
 * and are tested via integration tests on device.
 */
class PhoneConnectivityManagerTest {

    private val json = Json { ignoreUnknownKeys = true }

    // We test the JSON parsing logic directly since PhoneConnectivityManager
    // requires Android context for MessageClient. The parsing is the critical
    // business logic we want to verify.

    @Test
    fun `parse workout list from JSON`() {
        val workouts = listOf(
            WearWorkout(
                id = "w1",
                name = "Morning HIIT",
                sport = "cardio",
                duration = 1800,
                description = "Intense session"
            ),
            WearWorkout(
                id = "w2",
                name = "Leg Day",
                sport = "strength",
                duration = 2700
            )
        )
        val payload = json.encodeToString(workouts)
        val parsed = json.decodeFromString<List<WearWorkout>>(payload)

        assertThat(parsed).hasSize(2)
        assertThat(parsed[0].id).isEqualTo("w1")
        assertThat(parsed[0].name).isEqualTo("Morning HIIT")
        assertThat(parsed[0].sport).isEqualTo("cardio")
        assertThat(parsed[0].duration).isEqualTo(1800)
        assertThat(parsed[0].formattedDuration).isEqualTo("30m")
        assertThat(parsed[1].formattedDuration).isEqualTo("45m")
    }

    @Test
    fun `parse schedule from JSON`() {
        val schedule = WearDaySchedule(
            date = "2026-03-22",
            workouts = listOf(
                WearScheduleEntry(
                    workoutId = "w1",
                    workoutName = "Morning Run",
                    sport = "running",
                    scheduledTime = "7:00 AM",
                    duration = 2400,
                    isCompleted = false
                ),
                WearScheduleEntry(
                    workoutId = "w2",
                    workoutName = "Evening Yoga",
                    sport = "mobility",
                    scheduledTime = "6:00 PM",
                    duration = 1800,
                    isCompleted = true
                )
            )
        )
        val payload = json.encodeToString(schedule)
        val parsed = json.decodeFromString<WearDaySchedule>(payload)

        assertThat(parsed.date).isEqualTo("2026-03-22")
        assertThat(parsed.workouts).hasSize(2)
        assertThat(parsed.workouts[0].isCompleted).isFalse()
        assertThat(parsed.workouts[1].isCompleted).isTrue()
    }

    @Test
    fun `parse readiness from JSON`() {
        val readiness = WearReadinessData(
            score = 78,
            label = "Ready",
            factors = listOf(
                com.amakaflow.shared.model.ReadinessFactor("Sleep", "7.5h", "good"),
                com.amakaflow.shared.model.ReadinessFactor("Recovery", "85%", "good"),
                com.amakaflow.shared.model.ReadinessFactor("Stress", "High", "poor")
            )
        )
        val payload = json.encodeToString(readiness)
        val parsed = json.decodeFromString<WearReadinessData>(payload)

        assertThat(parsed.score).isEqualTo(78)
        assertThat(parsed.label).isEqualTo("Ready")
        assertThat(parsed.factors).hasSize(3)
        assertThat(parsed.factors[2].status).isEqualTo("poor")
    }

    @Test
    fun `WearWorkout formattedDuration handles hours`() {
        val workout = WearWorkout(id = "1", name = "Long Run", sport = "running", duration = 5400)
        assertThat(workout.formattedDuration).isEqualTo("1h 30m")
    }

    @Test
    fun `WearWorkout formattedDuration handles seconds only`() {
        val workout = WearWorkout(id = "1", name = "Sprint", sport = "running", duration = 30)
        assertThat(workout.formattedDuration).isEqualTo("30s")
    }

    @Test
    fun `WearWorkout formattedDuration handles zero`() {
        val workout = WearWorkout(id = "1", name = "Quick", sport = "other", duration = 0)
        assertThat(workout.formattedDuration).isEqualTo("0s")
    }

    @Test
    fun `parse empty workout list from JSON`() {
        val payload = "[]"
        val parsed = json.decodeFromString<List<WearWorkout>>(payload)
        assertThat(parsed).isEmpty()
    }

    @Test
    fun `parse schedule with no workouts`() {
        val schedule = WearDaySchedule(date = "2026-03-22", workouts = emptyList())
        val payload = json.encodeToString(schedule)
        val parsed = json.decodeFromString<WearDaySchedule>(payload)

        assertThat(parsed.workouts).isEmpty()
    }

    @Test
    fun `parse readiness with no factors`() {
        val readiness = WearReadinessData(score = 50, label = "Moderate")
        val payload = json.encodeToString(readiness)
        val parsed = json.decodeFromString<WearReadinessData>(payload)

        assertThat(parsed.factors).isEmpty()
    }
}
