package com.amakaflow.companion.domain

import com.amakaflow.companion.data.model.Workout
import com.amakaflow.companion.data.model.WorkoutInterval
import com.amakaflow.companion.data.model.WorkoutSource
import com.amakaflow.companion.data.model.WorkoutSport
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for Workout data model
 * Part of AMA-666: Android Phase 1 - Core unit tests
 */
class WorkoutModelTest {

    @Test
    fun workout_creation_with_required_fields() {
        val workout = Workout(
            id = "workout-1",
            name = "Morning Run",
            sport = WorkoutSport.RUNNING,
            duration = 1800,
            source = WorkoutSource.AI
        )

        assertThat(workout.id).isEqualTo("workout-1")
        assertThat(workout.name).isEqualTo("Morning Run")
        assertThat(workout.sport).isEqualTo(WorkoutSport.RUNNING)
        assertThat(workout.duration).isEqualTo(1800)
        assertThat(workout.source).isEqualTo(WorkoutSource.AI)
    }

    @Test
    fun workout_creation_with_intervals() {
        val intervals = listOf(
            WorkoutInterval.Warmup(
                seconds = 300,
                target = "Warmup"
            ),
            WorkoutInterval.Time(
                seconds = 1200,
                target = "Running"
            ),
            WorkoutInterval.Cooldown(
                seconds = 300,
                target = "Cooldown"
            )
        )

        val workout = Workout(
            id = "workout-2",
            name = "Intervals Run",
            sport = WorkoutSport.RUNNING,
            duration = 1800,
            source = WorkoutSource.COACH,
            intervals = intervals
        )

        assertThat(workout.intervals).hasSize(3)
    }

    @Test
    fun workout_with_repeat_interval() {
        val intervals = listOf(
            WorkoutInterval.Repeat(
                reps = 3,
                intervals = listOf(
                    WorkoutInterval.Time(
                        seconds = 60,
                        target = "Burpee"
                    )
                )
            )
        )

        val workout = Workout(
            id = "workout-3",
            name = "HIIT Workout",
            sport = WorkoutSport.CARDIO,
            duration = 600,
            source = WorkoutSource.AI,
            intervals = intervals
        )

        assertThat(workout.intervals).hasSize(1)
        val repeat = workout.intervals[0] as? WorkoutInterval.Repeat
        assertThat(repeat).isNotNull()
        assertThat(repeat?.reps).isEqualTo(3)
    }

    @Test
    fun workout_computed_properties() {
        val workout = Workout(
            id = "workout-4",
            name = "Test Workout",
            sport = WorkoutSport.STRENGTH,
            duration = 3600,
            source = WorkoutSource.AMAKA,
            description = "A test workout"
        )

        // Test formattedDuration (computed property calls WorkoutHelpers.formatDuration)
        assertThat(workout.formattedDuration).isEqualTo("1h 0m")

        // Test intervalCount with empty intervals
        assertThat(workout.intervalCount).isEqualTo(0)
    }

    @Test
    fun workout_interval_count_with_nested_intervals() {
        val intervals = listOf(
            WorkoutInterval.Repeat(
                reps = 2,
                intervals = listOf(
                    WorkoutInterval.Time(
                        seconds = 30,
                        target = "Sprint"
                    ),
                    WorkoutInterval.Rest(
                        seconds = 30
                    )
                )
            ),
            WorkoutInterval.Cooldown(
                seconds = 300,
                target = "Cooldown"
            )
        )

        val workout = Workout(
            id = "workout-5",
            name = "Sprint Intervals",
            sport = WorkoutSport.RUNNING,
            duration = 420,
            source = WorkoutSource.COACH,
            intervals = intervals
        )

        // Repeat(2) * (Sprint + Rest) = 2 * 2 = 4 + Cooldown(1) = 5
        assertThat(workout.intervalCount).isEqualTo(5)
    }

    @Test
    fun workout_empty_intervals() {
        val workout = Workout(
            id = "workout-6",
            name = "Empty Intervals",
            sport = WorkoutSport.STRENGTH,
            duration = 2700,
            source = WorkoutSource.IMAGE,
            intervals = emptyList()
        )

        assertThat(workout.intervals).isEmpty()
        assertThat(workout.intervalCount).isEqualTo(0)
    }

    @Test
    fun workout_single_interval() {
        val intervals = listOf(
            WorkoutInterval.Time(
                seconds = 60,
                target = "Plank"
            )
        )

        val workout = Workout(
            id = "workout-7",
            name = "Single Exercise",
            sport = WorkoutSport.STRENGTH,
            duration = 60,
            source = WorkoutSource.AI,
            intervals = intervals
        )

        assertThat(workout.intervalCount).isEqualTo(1)
    }

    @Test
    fun workout_equality() {
        val workout1 = Workout(
            id = "workout-8",
            name = "Test",
            sport = WorkoutSport.RUNNING,
            duration = 1800,
            source = WorkoutSource.AI
        )

        val workout2 = Workout(
            id = "workout-8",
            name = "Test",
            sport = WorkoutSport.RUNNING,
            duration = 1800,
            source = WorkoutSource.AI
        )

        assertThat(workout1).isEqualTo(workout2)
    }

    @Test
    fun workout_copy_behavior() {
        val original = Workout(
            id = "workout-9",
            name = "Original",
            sport = WorkoutSport.STRENGTH,
            duration = 3600,
            source = WorkoutSource.COACH
        )

        val copied = original.copy(
            name = "Copied",
            duration = 2700
        )

        assertThat(copied.id).isEqualTo("workout-9") // id preserved
        assertThat(copied.name).isEqualTo("Copied")
        assertThat(copied.duration).isEqualTo(2700)
        assertThat(copied.sport).isEqualTo(WorkoutSport.STRENGTH) // sport preserved
    }

    @Test
    fun workout_with_source_url() {
        val workout = Workout(
            id = "workout-10",
            name = "YouTube Workout",
            sport = WorkoutSport.MOBILITY,
            duration = 2700,
            source = WorkoutSource.YOUTUBE,
            sourceUrl = "https://youtube.com/watch?v=abc123"
        )

        assertThat(workout.sourceUrl).isEqualTo("https://youtube.com/watch?v=abc123")
    }

    @Test
    fun workout_all_sport_types() {
        val sports = listOf(
            WorkoutSport.RUNNING,
            WorkoutSport.CYCLING,
            WorkoutSport.STRENGTH,
            WorkoutSport.MOBILITY,
            WorkoutSport.SWIMMING,
            WorkoutSport.CARDIO,
            WorkoutSport.OTHER
        )

        sports.forEach { sport ->
            val workout = Workout(
                id = "workout-${sport.name}",
                name = "${sport.name} Workout",
                sport = sport,
                duration = 1800,
                source = WorkoutSource.AI
            )
            assertThat(workout.sport).isEqualTo(sport)
        }
    }
}
