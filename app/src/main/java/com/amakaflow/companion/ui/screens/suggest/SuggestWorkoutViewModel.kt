package com.amakaflow.companion.ui.screens.suggest

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.model.SuggestWorkoutRequest
import com.amakaflow.companion.data.model.SuggestWorkoutResponse
import com.amakaflow.companion.data.model.Workout
import com.amakaflow.companion.data.model.WorkoutInterval
import com.amakaflow.companion.data.model.WorkoutSource
import com.amakaflow.companion.data.model.WorkoutSport
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.PlannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

private const val TAG = "SuggestWorkoutVM"
private const val PROFILE_KEY = "coaching_profile"
private const val PREFS_NAME = "amakaflow_coaching"

// MARK: - Coaching Profile

@Serializable
data class CoachingProfile(
    val experience: ExperienceLevel,
    val goal: TrainingGoal,
    val daysPerWeek: Int
)

@Serializable
enum class ExperienceLevel {
    BEGINNER, INTERMEDIATE, ADVANCED;

    val displayName: String
        get() = when (this) {
            BEGINNER -> "Beginner"
            INTERMEDIATE -> "Intermediate"
            ADVANCED -> "Advanced"
        }
}

@Serializable
enum class TrainingGoal {
    LOSE_WEIGHT, BUILD_MUSCLE, IMPROVE_ENDURANCE, GENERAL_FITNESS, ATHLETIC;

    val displayName: String
        get() = when (this) {
            LOSE_WEIGHT -> "Lose Weight"
            BUILD_MUSCLE -> "Build Muscle"
            IMPROVE_ENDURANCE -> "Improve Endurance"
            GENERAL_FITNESS -> "General Fitness"
            ATHLETIC -> "Athletic Performance"
        }
}

// MARK: - UI State

enum class SuggestWorkoutState {
    IDLE,
    NEEDS_ONBOARDING,
    LOADING,
    SUCCESS,
    ERROR
}

data class SuggestWorkoutUiState(
    val state: SuggestWorkoutState = SuggestWorkoutState.IDLE,
    val suggestedWorkout: Workout? = null,
    val errorMessage: String? = null,
    val hasProfile: Boolean = false
)

// MARK: - ViewModel

@HiltViewModel
class SuggestWorkoutViewModel @Inject constructor(
    private val plannerRepository: PlannerRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuggestWorkoutUiState())
    val uiState: StateFlow<SuggestWorkoutUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        _uiState.update { it.copy(hasProfile = hasCoachingProfile()) }
    }

    // MARK: - Profile Management

    fun hasCoachingProfile(): Boolean {
        return prefs.getString(PROFILE_KEY, null) != null
    }

    fun loadProfile(): CoachingProfile? {
        val data = prefs.getString(PROFILE_KEY, null) ?: return null
        return try {
            json.decodeFromString<CoachingProfile>(data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode coaching profile", e)
            null
        }
    }

    fun saveProfile(profile: CoachingProfile) {
        val data = json.encodeToString(profile)
        prefs.edit().putString(PROFILE_KEY, data).apply()
        _uiState.update { it.copy(hasProfile = true) }
    }

    // MARK: - Suggest Workout

    fun requestSuggestion() {
        if (!hasCoachingProfile()) {
            _uiState.update { it.copy(state = SuggestWorkoutState.NEEDS_ONBOARDING) }
            return
        }
        suggestWorkout()
    }

    fun completeOnboarding(experience: ExperienceLevel, goal: TrainingGoal, daysPerWeek: Int) {
        val profile = CoachingProfile(experience = experience, goal = goal, daysPerWeek = daysPerWeek)
        saveProfile(profile)
        suggestWorkout()
    }

    fun suggestWorkout() {
        _uiState.update { it.copy(state = SuggestWorkoutState.LOADING, errorMessage = null) }

        viewModelScope.launch {
            plannerRepository.suggestWorkout().collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.update { it.copy(state = SuggestWorkoutState.LOADING) }
                    }
                    is Result.Success -> {
                        val response = result.data
                        val workout = buildWorkout(response)
                        _uiState.update {
                            it.copy(
                                state = SuggestWorkoutState.SUCCESS,
                                suggestedWorkout = workout,
                                errorMessage = null
                            )
                        }
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Suggest workout failed: ${result.message}")
                        _uiState.update {
                            it.copy(
                                state = SuggestWorkoutState.ERROR,
                                errorMessage = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun reset() {
        _uiState.update {
            SuggestWorkoutUiState(hasProfile = hasCoachingProfile())
        }
    }

    // MARK: - Build Workout from Response

    private fun buildWorkout(response: SuggestWorkoutResponse): Workout {
        val intervals = mutableListOf<WorkoutInterval>()

        // Add warm-up if present
        response.warmUp?.let {
            intervals.add(WorkoutInterval.Warmup(seconds = it.seconds, target = it.target))
        }

        // Add main blocks
        intervals.addAll(response.blocks)

        // Add cooldown if present
        response.cooldown?.let {
            intervals.add(WorkoutInterval.Cooldown(seconds = it.seconds, target = it.target))
        }

        val estimatedDuration = response.durationSeconds ?: intervals.sumOf { interval ->
            when (interval) {
                is WorkoutInterval.Warmup -> interval.seconds
                is WorkoutInterval.Cooldown -> interval.seconds
                is WorkoutInterval.Time -> interval.seconds
                is WorkoutInterval.Reps -> interval.restSec ?: 60
                is WorkoutInterval.Rest -> interval.seconds ?: 60
                else -> 60
            }
        }

        return Workout(
            id = UUID.randomUUID().toString(),
            name = response.name ?: "AI Suggested Workout",
            sport = response.sport ?: WorkoutSport.STRENGTH,
            duration = estimatedDuration,
            intervals = intervals,
            description = response.description,
            source = WorkoutSource.COACH
        )
    }
}
