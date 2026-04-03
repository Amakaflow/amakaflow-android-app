package com.amakaflow.companion.ui.screens.bulkimport

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.api.IngestorApi
import com.amakaflow.companion.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BulkImportVM"

enum class BulkImportStep(val index: Int, val title: String) {
    SOURCE_SELECTION(0, "Sources"),
    DETECTION(1, "Detection"),
    EXERCISE_MATCHING(2, "Matching"),
    PREVIEW(3, "Preview"),
    IMPORT(4, "Import")
}

data class BulkImportState(
    val currentStep: BulkImportStep = BulkImportStep.SOURCE_SELECTION,
    // Step 1 - Source selection
    val urls: List<String> = listOf(""),
    // Step 2 - Detection results
    val detectedItems: List<DetectedItem> = emptyList(),
    val selectedItemIds: Set<String> = emptySet(),
    // Step 3 - Exercise matching
    val exerciseMatches: List<ExerciseMatch> = emptyList(),
    val resolvedMatches: Map<String, String> = emptyMap(), // matchId -> chosen name
    // Step 4 - Preview
    val previewWorkouts: List<PreviewWorkout> = emptyList(),
    val selectedWorkoutIds: Set<String> = emptySet(),
    // Step 5 - Import
    val importJobId: String? = null,
    val importStatus: BulkImportStatus? = null,
    // Loading / error
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BulkImportViewModel @Inject constructor(
    private val ingestorApi: IngestorApi
) : ViewModel() {

    var state by mutableStateOf(BulkImportState())
        private set

    // -------------------------------------------------------------------------
    // URL management (Step 1)
    // -------------------------------------------------------------------------

    fun addUrl() {
        state = state.copy(urls = state.urls + "")
    }

    fun updateUrl(index: Int, value: String) {
        val updated = state.urls.toMutableList()
        updated[index] = value
        state = state.copy(urls = updated)
    }

    fun removeUrl(index: Int) {
        if (state.urls.size <= 1) return
        val updated = state.urls.toMutableList()
        updated.removeAt(index)
        state = state.copy(urls = updated)
    }

    val validUrls: List<String>
        get() = state.urls.filter { it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://")) }

    // -------------------------------------------------------------------------
    // Detection (Step 1 -> 2)
    // -------------------------------------------------------------------------

    fun detectSources() {
        val urls = validUrls
        if (urls.isEmpty()) return

        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val request = BulkDetectRequest(urls = urls)
                val response = ingestorApi.detectImport(request)
                if (response.isSuccessful && response.body() != null) {
                    val items = response.body()!!.items
                    state = state.copy(
                        isLoading = false,
                        detectedItems = items,
                        selectedItemIds = items.filter { it.status != "error" }.map { it.id }.toSet(),
                        currentStep = BulkImportStep.DETECTION
                    )
                } else {
                    state = state.copy(
                        isLoading = false,
                        error = "Detection failed: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "detectSources error", e)
                state = state.copy(isLoading = false, error = e.message ?: "Detection error")
            }
        }
    }

    fun toggleItemSelection(itemId: String) {
        val selected = state.selectedItemIds.toMutableSet()
        if (selected.contains(itemId)) selected.remove(itemId) else selected.add(itemId)
        state = state.copy(selectedItemIds = selected)
    }

    // -------------------------------------------------------------------------
    // Exercise Matching (Step 2 -> 3)
    // -------------------------------------------------------------------------

    fun proceedToMatching() {
        val selected = state.selectedItemIds.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val request = BulkMatchRequest(items = selected)
                val response = ingestorApi.matchExercises(request)
                if (response.isSuccessful && response.body() != null) {
                    val matches = response.body()!!.matches
                    state = state.copy(
                        isLoading = false,
                        exerciseMatches = matches,
                        resolvedMatches = matches.associate { it.id to (it.matchedName ?: it.sourceName) },
                        currentStep = BulkImportStep.EXERCISE_MATCHING
                    )
                } else {
                    state = state.copy(
                        isLoading = false,
                        error = "Matching failed: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "proceedToMatching error", e)
                state = state.copy(isLoading = false, error = e.message ?: "Matching error")
            }
        }
    }

    fun updateMatch(matchId: String, chosenName: String) {
        val resolved = state.resolvedMatches.toMutableMap()
        resolved[matchId] = chosenName
        state = state.copy(resolvedMatches = resolved)
    }

    // -------------------------------------------------------------------------
    // Preview (Step 3 -> 4)
    // -------------------------------------------------------------------------

    fun proceedToPreview() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val request = BulkPreviewRequest(
                    items = state.selectedItemIds.toList(),
                    matches = state.resolvedMatches
                )
                val response = ingestorApi.previewImport(request)
                if (response.isSuccessful && response.body() != null) {
                    val workouts = response.body()!!.workouts
                    state = state.copy(
                        isLoading = false,
                        previewWorkouts = workouts,
                        selectedWorkoutIds = workouts.filter { it.valid }.map { it.id }.toSet(),
                        currentStep = BulkImportStep.PREVIEW
                    )
                } else {
                    state = state.copy(
                        isLoading = false,
                        error = "Preview failed: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "proceedToPreview error", e)
                state = state.copy(isLoading = false, error = e.message ?: "Preview error")
            }
        }
    }

    fun toggleWorkoutSelection(workoutId: String) {
        val selected = state.selectedWorkoutIds.toMutableSet()
        if (selected.contains(workoutId)) selected.remove(workoutId) else selected.add(workoutId)
        state = state.copy(selectedWorkoutIds = selected)
    }

    // -------------------------------------------------------------------------
    // Execute Import (Step 4 -> 5)
    // -------------------------------------------------------------------------

    fun executeImport() {
        viewModelScope.launch {
            state = state.copy(
                isLoading = true,
                error = null,
                currentStep = BulkImportStep.IMPORT
            )
            try {
                val request = BulkExecuteRequest(
                    items = state.selectedWorkoutIds.toList(),
                    matches = state.resolvedMatches
                )
                val response = ingestorApi.executeImport(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    state = state.copy(isLoading = false, importJobId = body.jobId)
                    pollImportStatus(body.jobId)
                } else {
                    state = state.copy(
                        isLoading = false,
                        error = "Import failed: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "executeImport error", e)
                state = state.copy(isLoading = false, error = e.message ?: "Import error")
            }
        }
    }

    private suspend fun pollImportStatus(jobId: String) {
        while (true) {
            delay(2000)
            try {
                val response = ingestorApi.fetchImportStatus(jobId)
                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()!!
                    state = state.copy(importStatus = status)
                    if (status.status == "completed" || status.status == "failed") {
                        return
                    }
                } else {
                    state = state.copy(error = "Status check failed: ${response.code()}")
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "pollImportStatus error", e)
                state = state.copy(error = e.message ?: "Status polling error")
                return
            }
        }
    }

    // -------------------------------------------------------------------------
    // Navigation helpers
    // -------------------------------------------------------------------------

    fun goBack() {
        val prevIndex = state.currentStep.index - 1
        val prevStep = BulkImportStep.values().getOrNull(prevIndex) ?: return
        state = state.copy(currentStep = prevStep, error = null)
    }

    fun clearError() {
        state = state.copy(error = null)
    }
}
