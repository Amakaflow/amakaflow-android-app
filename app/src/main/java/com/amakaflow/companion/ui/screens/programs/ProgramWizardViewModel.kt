package com.amakaflow.companion.ui.screens.programs

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.model.ProgramGenerationRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ProgramWizardVM"

enum class WizardStep(val index: Int, val title: String) {
    GOAL(0, "Goal"),
    EXPERIENCE(1, "Experience"),
    SCHEDULE(2, "Schedule"),
    EQUIPMENT(3, "Equipment"),
    PREFERENCES(4, "Preferences"),
    REVIEW(5, "Review")
}

enum class ProgramGoal(val label: String, val apiValue: String) {
    BUILD_MUSCLE("Build Muscle", "build_muscle"),
    LOSE_WEIGHT("Lose Weight", "lose_weight"),
    IMPROVE_ENDURANCE("Improve Endurance", "improve_endurance"),
    INCREASE_STRENGTH("Increase Strength", "increase_strength"),
    GENERAL_FITNESS("General Fitness", "general_fitness"),
    SPORT_PERFORMANCE("Sport Performance", "sport_performance")
}

enum class ExperienceLevel(val label: String, val apiValue: String) {
    BEGINNER("Beginner", "beginner"),
    INTERMEDIATE("Intermediate", "intermediate"),
    ADVANCED("Advanced", "advanced")
}

enum class EquipmentPreset(val label: String, val equipment: List<String>) {
    FULL_GYM("Full Gym", listOf("barbell", "dumbbell", "cable_machine", "pull_up_bar", "bench")),
    HOME_DUMBBELLS("Home + Dumbbells", listOf("dumbbell", "pull_up_bar", "resistance_band")),
    BODYWEIGHT_ONLY("Bodyweight Only", listOf("bodyweight")),
    RESISTANCE_BANDS("Resistance Bands", listOf("resistance_band", "bodyweight")),
    CUSTOM("Custom", emptyList())
}

data class ProgramWizardState(
    val currentStep: WizardStep = WizardStep.GOAL,
    // Step 1 - Goal
    val goal: ProgramGoal? = null,
    val durationWeeks: Int = 8,
    // Step 2 - Experience
    val experienceLevel: ExperienceLevel? = null,
    // Step 3 - Schedule
    val sessionsPerWeek: Int = 4,
    val preferredDays: List<Int> = emptyList(), // 1=Mon, 7=Sun
    val timePerSession: Int = 60, // minutes
    // Step 4 - Equipment
    val equipmentPreset: EquipmentPreset? = null,
    val customEquipment: List<String> = emptyList(),
    // Step 5 - Preferences
    val injuries: String = "",
    val focusAreas: List<String> = emptyList(),
    val avoidExercises: List<String> = emptyList(),
    // Generation state
    val isGenerating: Boolean = false,
    val generationProgress: Int = 0,
    val generatedProgramId: String? = null,
    val error: String? = null,
    val isComplete: Boolean = false
)

@HiltViewModel
class ProgramWizardViewModel @Inject constructor(
    private val api: AmakaflowApi
) : ViewModel() {

    var state by mutableStateOf(ProgramWizardState())
        private set

    val canGoNext: Boolean
        get() = when (state.currentStep) {
            WizardStep.GOAL -> state.goal != null
            WizardStep.EXPERIENCE -> state.experienceLevel != null
            WizardStep.SCHEDULE -> state.preferredDays.isNotEmpty()
            WizardStep.EQUIPMENT -> state.equipmentPreset != null
            WizardStep.PREFERENCES -> true
            WizardStep.REVIEW -> false
        }

    fun setGoal(goal: ProgramGoal) {
        state = state.copy(goal = goal)
    }

    fun setDurationWeeks(weeks: Int) {
        state = state.copy(durationWeeks = weeks)
    }

    fun setExperienceLevel(level: ExperienceLevel) {
        state = state.copy(experienceLevel = level)
    }

    fun setSessionsPerWeek(sessions: Int) {
        state = state.copy(sessionsPerWeek = sessions)
    }

    fun togglePreferredDay(day: Int) {
        val days = state.preferredDays.toMutableList()
        if (days.contains(day)) days.remove(day) else days.add(day)
        state = state.copy(preferredDays = days.sorted())
    }

    fun setTimePerSession(minutes: Int) {
        state = state.copy(timePerSession = minutes)
    }

    fun setEquipmentPreset(preset: EquipmentPreset) {
        state = state.copy(equipmentPreset = preset, customEquipment = emptyList())
    }

    fun toggleCustomEquipment(item: String) {
        val equipment = state.customEquipment.toMutableList()
        if (equipment.contains(item)) equipment.remove(item) else equipment.add(item)
        state = state.copy(customEquipment = equipment)
    }

    fun setInjuries(text: String) {
        state = state.copy(injuries = text)
    }

    fun toggleFocusArea(area: String) {
        val areas = state.focusAreas.toMutableList()
        if (areas.contains(area)) areas.remove(area) else areas.add(area)
        state = state.copy(focusAreas = areas)
    }

    fun toggleAvoidExercise(exercise: String) {
        val list = state.avoidExercises.toMutableList()
        if (list.contains(exercise)) list.remove(exercise) else list.add(exercise)
        state = state.copy(avoidExercises = list)
    }

    fun goNext() {
        val nextIndex = state.currentStep.index + 1
        val nextStep = WizardStep.values().getOrNull(nextIndex) ?: return
        state = state.copy(currentStep = nextStep, error = null)
    }

    fun goBack() {
        val prevIndex = state.currentStep.index - 1
        val prevStep = WizardStep.values().getOrNull(prevIndex) ?: return
        state = state.copy(currentStep = prevStep, error = null)
    }

    fun generateProgram() {
        val goal = state.goal ?: return
        val experience = state.experienceLevel ?: return

        val equipment = if (state.equipmentPreset == EquipmentPreset.CUSTOM) {
            state.customEquipment
        } else {
            state.equipmentPreset?.equipment ?: emptyList()
        }

        val request = ProgramGenerationRequest(
            goal = goal.apiValue,
            experienceLevel = experience.apiValue,
            durationWeeks = state.durationWeeks,
            sessionsPerWeek = state.sessionsPerWeek,
            preferredDays = state.preferredDays,
            timePerSession = state.timePerSession,
            equipment = equipment,
            injuries = state.injuries.takeIf { it.isNotBlank() },
            focusAreas = state.focusAreas.takeIf { it.isNotEmpty() },
            avoidExercises = state.avoidExercises.takeIf { it.isNotEmpty() }
        )

        viewModelScope.launch {
            state = state.copy(isGenerating = true, error = null, generationProgress = 0)
            try {
                val response = api.generateProgram(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.programId != null) {
                        state = state.copy(
                            isGenerating = false,
                            generatedProgramId = body.programId,
                            isComplete = true
                        )
                    } else {
                        pollGenerationStatus(body.jobId)
                    }
                } else {
                    state = state.copy(
                        isGenerating = false,
                        error = "Failed to start generation: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "generateProgram error", e)
                state = state.copy(isGenerating = false, error = e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun pollGenerationStatus(jobId: String) {
        val maxAttempts = 60 // 2 min max at 2s intervals
        var attempts = 0
        while (attempts < maxAttempts) {
            delay(2000)
            attempts++
            try {
                val response = api.fetchGenerationStatus(jobId)
                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()!!
                    state = state.copy(generationProgress = status.progress)
                    when (status.status) {
                        "completed" -> {
                            val programId = status.programId
                            if (programId != null) {
                                state = state.copy(
                                    isGenerating = false,
                                    generatedProgramId = programId,
                                    isComplete = true
                                )
                            } else {
                                state = state.copy(
                                    isGenerating = false,
                                    error = "Generation completed but no program was returned"
                                )
                            }
                            return
                        }
                        "failed" -> {
                            state = state.copy(
                                isGenerating = false,
                                error = status.error ?: "Generation failed"
                            )
                            return
                        }
                        else -> {
                            // Still in progress, keep polling
                        }
                    }
                } else {
                    state = state.copy(
                        isGenerating = false,
                        error = "Status check failed: ${response.code()}"
                    )
                    return
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "pollGenerationStatus error", e)
                state = state.copy(isGenerating = false, error = e.message ?: "Polling error")
                return
            }
        }
        // Timed out waiting for terminal status
        state = state.copy(isGenerating = false, error = "Generation timed out — please try again")
    }

    fun clearError() {
        state = state.copy(error = null)
    }
}
