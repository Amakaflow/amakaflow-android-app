package com.amakaflow.companion.data.repository

import com.amakaflow.companion.data.model.Workout
import com.amakaflow.companion.data.model.WorkoutInterval
import com.amakaflow.companion.data.model.WorkoutSource
import com.amakaflow.companion.data.model.WorkoutSport
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Fixture-based workout repository for E2E testing.
 * Returns hardcoded workout data matching iOS test fixtures,
 * avoiding real API calls during Maestro test automation.
 */
class FixtureWorkoutRepository(
    private val fixtureNames: List<String>
) : WorkoutRepository {

    private val allFixtures = mapOf(
        "amrap_10min" to Workout(
            id = "fixture-amrap-10min",
            name = "AMRAP 10 Min",
            sport = WorkoutSport.CARDIO,
            duration = 600,
            source = WorkoutSource.AI,
            intervals = listOf(
                WorkoutInterval.Time(seconds = 600, target = "As many rounds as possible")
            )
        ),
        "emom_strength" to Workout(
            id = "fixture-emom-strength",
            name = "EMOM Strength",
            sport = WorkoutSport.STRENGTH,
            duration = 1200,
            source = WorkoutSource.COACH,
            intervals = listOf(
                WorkoutInterval.Repeat(
                    reps = 10,
                    intervals = listOf(
                        WorkoutInterval.Reps(sets = 1, reps = 10, name = "Kettlebell Swings"),
                        WorkoutInterval.Rest(seconds = null)
                    )
                )
            )
        ),
        "strength_block_w1" to Workout(
            id = "fixture-strength-w1",
            name = "Strength Block W1",
            sport = WorkoutSport.STRENGTH,
            duration = 2700,
            source = WorkoutSource.COACH,
            intervals = listOf(
                WorkoutInterval.Warmup(seconds = 300, target = "Light cardio"),
                WorkoutInterval.Reps(sets = 4, reps = 8, name = "Barbell Squat", load = "moderate"),
                WorkoutInterval.Rest(seconds = 90),
                WorkoutInterval.Reps(sets = 4, reps = 8, name = "Bench Press", load = "moderate"),
                WorkoutInterval.Rest(seconds = 90),
                WorkoutInterval.Reps(sets = 3, reps = 12, name = "Dumbbell Row"),
                WorkoutInterval.Cooldown(seconds = 300, target = "Stretch")
            )
        )
    )

    private val workouts: List<Workout> = if (fixtureNames.isEmpty()) {
        allFixtures.values.toList()
    } else {
        fixtureNames.mapNotNull { allFixtures[it] }
    }

    private val workoutMap: Map<String, Workout> = workouts.associateBy { it.id }

    private val _localWorkouts = MutableStateFlow(workouts)

    override fun getIncomingWorkouts(): Flow<Result<List<Workout>>> =
        flowOf(Result.Success(workouts))

    override fun getPushedWorkouts(): Flow<Result<List<Workout>>> =
        flowOf(Result.Success(workouts))

    override fun getLocalPushedWorkouts(): Flow<List<Workout>> =
        _localWorkouts

    override suspend fun getLocalPushedWorkoutsSync(): List<Workout> =
        workouts

    override suspend fun getLocalWorkout(workoutId: String): Workout? =
        workoutMap[workoutId]

    override fun getWorkout(id: String): Flow<Result<Workout>> {
        val workout = workoutMap[id]
        return if (workout != null) {
            flowOf(Result.Success(workout))
        } else {
            flowOf(Result.Error("Fixture workout not found: $id"))
        }
    }

    override fun getCachedWorkout(id: String): Workout? =
        workoutMap[id]

    override suspend fun confirmSync(workoutId: String): Result<Unit> =
        Result.Success(Unit)

    override suspend fun reportSyncFailed(workoutId: String, error: String): Result<Unit> =
        Result.Success(Unit)

    override suspend fun markWorkoutCompleted(workoutId: String) {
        // No-op for fixture data
    }
}
