package com.amakaflow.companion.data.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Tests for ExecutionLog model serialization (AMA-667)
 */
class ExecutionLogTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `ExecutionLog serializes to JSON correctly`() {
        // Given
        val executionLog = ExecutionLog(
            version = 2,
            intervals = listOf(
                IntervalLog(
                    intervalIndex = 0,
                    plannedName = "Warm Up",
                    plannedKind = "warmup",
                    status = IntervalStatus.COMPLETED,
                    plannedDurationSeconds = 300,
                    actualDurationSeconds = 295
                ),
                IntervalLog(
                    intervalIndex = 1,
                    plannedName = "Bench Press",
                    plannedKind = "reps",
                    status = IntervalStatus.COMPLETED,
                    plannedDurationSeconds = null,
                    plannedSets = 4,
                    plannedReps = 10,
                    sets = listOf(
                        SetLog(
                            setNumber = 1,
                            status = IntervalStatus.COMPLETED,
                            repsPlanned = 10,
                            repsCompleted = 10
                        ),
                        SetLog(
                            setNumber = 2,
                            status = IntervalStatus.COMPLETED,
                            repsPlanned = 10,
                            repsCompleted = 10
                        )
                    )
                )
            ),
            summary = ExecutionSummary(
                totalDurationSeconds = 1800,
                activeDurationSeconds = 1500
            )
        )

        // When
        val serialized = json.encodeToString(executionLog)

        // Then
        assertThat(serialized).contains("\"version\":2")
        assertThat(serialized).contains("\"planned_name\":\"Warm Up\"")
        assertThat(serialized).contains("\"planned_kind\":\"reps\"")
    }

    @Test
    fun `ExecutionLog deserializes from JSON correctly`() {
        // Given
        val jsonString = """
            {
                "version": 2,
                "intervals": [
                    {
                        "interval_index": 0,
                        "planned_name": "Warm Up",
                        "planned_kind": "warmup",
                        "status": "completed",
                        "planned_duration_seconds": 300,
                        "actual_duration_seconds": 295
                    },
                    {
                        "interval_index": 1,
                        "planned_name": "Squats",
                        "planned_kind": "reps",
                        "status": "skipped",
                        "planned_sets": 3,
                        "planned_reps": 12,
                        "skip_reason": "user_skipped"
                    }
                ],
                "summary": {
                    "total_duration_seconds": 1200,
                    "active_duration_seconds": 900
                }
            }
        """.trimIndent()

        // When
        val executionLog = json.decodeFromString<ExecutionLog>(jsonString)

        // Then
        assertThat(executionLog.version).isEqualTo(2)
        assertThat(executionLog.intervals).hasSize(2)
        assertThat(executionLog.intervals[0].plannedName).isEqualTo("Warm Up")
        assertThat(executionLog.intervals[0].status).isEqualTo(IntervalStatus.COMPLETED)
        assertThat(executionLog.intervals[1].plannedName).isEqualTo("Squats")
        assertThat(executionLog.intervals[1].status).isEqualTo(IntervalStatus.SKIPPED)
        assertThat(executionLog.intervals[1].skipReason).isEqualTo("user_skipped")
        assertThat(executionLog.summary?.totalDurationSeconds).isEqualTo(1200)
    }

    @Test
    fun `IntervalLog with SetLog serializes correctly`() {
        // Given
        val intervalLog = IntervalLog(
            intervalIndex = 0,
            plannedName = "Deadlift",
            plannedKind = "reps",
            status = IntervalStatus.COMPLETED,
            plannedSets = 5,
            plannedReps = 5,
            sets = listOf(
                SetLog(
                    setNumber = 1,
                    status = IntervalStatus.COMPLETED,
                    repsPlanned = 5,
                    repsCompleted = 5,
                    weight = WeightEntry(
                        displayLabel = "135 lbs",
                        components = listOf(
                            WeightComponent(source = "barbell", value = 45.0, unit = "lbs"),
                            WeightComponent(source = "plate", value = 45.0, unit = "lbs")
                        )
                    ),
                    rpe = 8
                ),
                SetLog(
                    setNumber = 2,
                    status = IntervalStatus.COMPLETED,
                    repsPlanned = 5,
                    repsCompleted = 5,
                    weight = WeightEntry(
                        displayLabel = "155 lbs",
                        components = listOf(
                            WeightComponent(source = "barbell", value = 45.0, unit = "lbs"),
                            WeightComponent(source = "plate", value = 55.0, unit = "lbs")
                        )
                    ),
                    rpe = 9
                )
            )
        )

        // When
        val serialized = json.encodeToString(intervalLog)

        // Then
        assertThat(serialized).contains("\"set_number\":1")
        assertThat(serialized).contains("\"set_number\":2")
        assertThat(serialized).contains("\"reps_completed\":5")
        assertThat(serialized).contains("\"display_label\":\"135 lbs\"")
    }

    @Test
    fun `ExecutionLog roundtrip serialization preserves data`() {
        // Given
        val original = ExecutionLog(
            version = 2,
            intervals = listOf(
                IntervalLog(
                    intervalIndex = 0,
                    plannedName = "Push-ups",
                    plannedKind = "reps",
                    status = IntervalStatus.COMPLETED,
                    plannedSets = 3,
                    plannedReps = 15,
                    sets = listOf(
                        SetLog(
                            setNumber = 1,
                            status = IntervalStatus.COMPLETED,
                            repsPlanned = 15,
                            repsCompleted = 15
                        )
                    )
                )
            )
        )

        // When
        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<ExecutionLog>(serialized)

        // Then
        assertThat(deserialized.version).isEqualTo(original.version)
        assertThat(deserialized.intervals).hasSize(original.intervals.size)
        assertThat(deserialized.intervals[0].plannedName).isEqualTo(original.intervals[0].plannedName)
        assertThat(deserialized.intervals[0].sets?.get(0)?.setNumber).isEqualTo(1)
    }

    @Test
    fun `IntervalStatus enum values are correct`() {
        // Verify all expected enum values exist
        assertThat(IntervalStatus.entries.map { it.name }).containsExactly(
            "COMPLETED",
            "SKIPPED",
            "NOT_REACHED"
        )
    }

    @Test
    fun `ExecutionLog default values are applied correctly`() {
        // Given - minimal JSON without optional fields
        val jsonString = """
            {
                "intervals": []
            }
        """.trimIndent()

        // When
        val executionLog = json.decodeFromString<ExecutionLog>(jsonString)

        // Then - defaults should be applied
        assertThat(executionLog.version).isEqualTo(2) // default
        assertThat(executionLog.intervals).isEmpty()
        assertThat(executionLog.summary).isNull()
    }

    @Test
    fun `SetLog with modified status serializes correctly`() {
        // Given - a set that was modified (weight changed from prescribed)
        val setLog = SetLog(
            setNumber = 3,
            status = IntervalStatus.COMPLETED,
            repsPlanned = 10,
            repsCompleted = 10,
            weight = WeightEntry(
                displayLabel = "50 lbs",
                components = listOf(
                    WeightComponent(source = "dumbbell", value = 25.0, unit = "lbs"),
                    WeightComponent(source = "dumbbell", value = 25.0, unit = "lbs")
                )
            ),
            modified = true,
            rpe = 7
        )

        // When
        val serialized = json.encodeToString(setLog)

        // Then
        assertThat(serialized).contains("\"modified\":true")
        assertThat(serialized).contains("\"rpe\":7")
    }
}
