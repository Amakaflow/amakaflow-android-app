package com.amakaflow.companion.domain.usecase.pr

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

// MARK: - PR Models

@Serializable
data class PersonalRecord(
    val id: String,
    val exerciseName: String,
    val type: PRType,
    val value: Double,
    val reps: Int? = null,
    val weight: Double? = null,
    val dateIso: String,
    val workoutName: String? = null
) {
    val formattedValue: String
        get() = when (type) {
            PRType.HEAVIEST_WEIGHT -> String.format("%.1f kg", value)
            PRType.MOST_REPS -> if (weight != null) {
                "${value.toInt()} reps @ ${String.format("%.1f", weight)} kg"
            } else "${value.toInt()} reps"
            PRType.MOST_VOLUME -> String.format("%.0f kg vol", value)
        }

    val typeLabel: String
        get() = when (type) {
            PRType.HEAVIEST_WEIGHT -> "Max Weight"
            PRType.MOST_REPS -> "Max Reps"
            PRType.MOST_VOLUME -> "Max Volume"
        }
}

@Serializable
enum class PRType {
    HEAVIEST_WEIGHT,
    MOST_REPS,
    MOST_VOLUME
}

data class PRDetectionResult(
    val newPRs: List<NewPR>
) {
    val hasPRs: Boolean get() = newPRs.isNotEmpty()

    data class NewPR(
        val exerciseName: String,
        val type: PRType,
        val oldValue: Double?,
        val newValue: Double,
        val reps: Int?,
        val weight: Double?
    )
}

data class ExerciseSetData(
    val exerciseName: String,
    val setNumber: Int,
    val repsCompleted: Int,
    val weightKg: Double
)

// MARK: - PR Detection Service

@Singleton
class PRDetectionService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("amakaflow_personal_records", Context.MODE_PRIVATE)
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Detect PRs from workout set data and persist any new records.
     */
    fun detectPRs(sets: List<ExerciseSetData>, workoutName: String?): PRDetectionResult {
        val storedPRs = loadPRs()
        val newPRs = mutableListOf<PRDetectionResult.NewPR>()

        // Group sets by exercise
        val grouped = sets.groupBy { it.exerciseName }

        for ((exerciseName, exerciseSets) in grouped) {
            val exercisePRs = storedPRs.filter { it.exerciseName == exerciseName }

            // 1. Check heaviest weight
            val heaviest = exerciseSets.maxByOrNull { it.weightKg }
            if (heaviest != null && heaviest.weightKg > 0) {
                val currentMax = exercisePRs
                    .filter { it.type == PRType.HEAVIEST_WEIGHT }
                    .maxOfOrNull { it.value } ?: 0.0

                if (heaviest.weightKg > currentMax) {
                    newPRs.add(PRDetectionResult.NewPR(
                        exerciseName = exerciseName,
                        type = PRType.HEAVIEST_WEIGHT,
                        oldValue = if (currentMax > 0) currentMax else null,
                        newValue = heaviest.weightKg,
                        reps = heaviest.repsCompleted,
                        weight = null
                    ))
                }
            }

            // 2. Check most reps at heaviest weight used
            val heaviestWeight = exerciseSets.maxOfOrNull { it.weightKg } ?: 0.0
            if (heaviestWeight > 0) {
                val repsAtWeight = exerciseSets
                    .filter { it.weightKg == heaviestWeight }
                    .maxOfOrNull { it.repsCompleted } ?: 0

                val currentMaxReps = exercisePRs
                    .filter { it.type == PRType.MOST_REPS && it.weight == heaviestWeight }
                    .maxOfOrNull { it.value } ?: 0.0

                if (repsAtWeight.toDouble() > currentMaxReps && repsAtWeight > 0) {
                    newPRs.add(PRDetectionResult.NewPR(
                        exerciseName = exerciseName,
                        type = PRType.MOST_REPS,
                        oldValue = if (currentMaxReps > 0) currentMaxReps else null,
                        newValue = repsAtWeight.toDouble(),
                        reps = repsAtWeight,
                        weight = heaviestWeight
                    ))
                }
            }

            // 3. Check most volume
            val totalVolume = exerciseSets.sumOf { it.repsCompleted.toDouble() * it.weightKg }
            if (totalVolume > 0) {
                val currentMaxVolume = exercisePRs
                    .filter { it.type == PRType.MOST_VOLUME }
                    .maxOfOrNull { it.value } ?: 0.0

                if (totalVolume > currentMaxVolume) {
                    newPRs.add(PRDetectionResult.NewPR(
                        exerciseName = exerciseName,
                        type = PRType.MOST_VOLUME,
                        oldValue = if (currentMaxVolume > 0) currentMaxVolume else null,
                        newValue = totalVolume,
                        reps = null,
                        weight = null
                    ))
                }
            }
        }

        // Persist new PRs
        if (newPRs.isNotEmpty()) {
            savePRs(newPRs, storedPRs, workoutName)
        }

        return PRDetectionResult(newPRs = newPRs)
    }

    /**
     * Load all stored PRs.
     */
    fun loadPRs(): List<PersonalRecord> {
        val jsonStr = prefs.getString("prs", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<PersonalRecord>>(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get PRs grouped by exercise name.
     */
    fun prsByExercise(): List<Pair<String, List<PersonalRecord>>> {
        val prs = loadPRs()
        return prs.groupBy { it.exerciseName }
            .map { (name, records) -> name to records.sortedByDescending { it.dateIso } }
            .sortedBy { it.first }
    }

    private fun savePRs(
        newPRs: List<PRDetectionResult.NewPR>,
        existing: List<PersonalRecord>,
        workoutName: String?
    ) {
        val updated = existing.toMutableList()
        val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

        for (pr in newPRs) {
            updated.removeAll { record ->
                record.exerciseName == pr.exerciseName &&
                record.type == pr.type &&
                (pr.type != PRType.MOST_REPS || record.weight == pr.weight)
            }

            updated.add(PersonalRecord(
                id = java.util.UUID.randomUUID().toString(),
                exerciseName = pr.exerciseName,
                type = pr.type,
                value = pr.newValue,
                reps = pr.reps,
                weight = pr.weight,
                dateIso = now,
                workoutName = workoutName
            ))
        }

        prefs.edit().putString("prs", json.encodeToString(updated)).apply()
    }
}
