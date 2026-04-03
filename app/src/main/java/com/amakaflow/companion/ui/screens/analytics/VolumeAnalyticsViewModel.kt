package com.amakaflow.companion.ui.screens.analytics

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.model.VolumeAnalyticsResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "VolumeAnalyticsVM"
private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

enum class AnalyticsPeriod(val label: String, val days: Int, val granularity: String) {
    WEEK("1W", 7, "daily"),
    MONTH("1M", 30, "weekly"),
    QUARTER("3M", 90, "monthly")
}

// ---------------------------------------------------------------------------
// UI State
// ---------------------------------------------------------------------------

data class VolumeAnalyticsUiState(
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.MONTH,
    val currentData: VolumeAnalyticsResponse? = null,
    val previousData: VolumeAnalyticsResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

@HiltViewModel
class VolumeAnalyticsViewModel @Inject constructor(
    private val api: AmakaflowApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(VolumeAnalyticsUiState())
    val uiState: StateFlow<VolumeAnalyticsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadVolume()
    }

    // ---------------------------------------------------------------------------
    // Computed properties
    // ---------------------------------------------------------------------------

    /** push/pull ratio (0..1 where 0.5 is balanced). Returns null when no data. */
    fun pushPullRatio(state: VolumeAnalyticsUiState = _uiState.value): Double? {
        val breakdown = state.currentData?.summary?.muscleGroupBreakdown ?: return null
        val push = breakdown.entries
            .filter { it.key.lowercase() in PUSH_MUSCLE_GROUPS }
            .sumOf { it.value }
        val pull = breakdown.entries
            .filter { it.key.lowercase() in PULL_MUSCLE_GROUPS }
            .sumOf { it.value }
        val total = push + pull
        return if (total > 0) push / total else null
    }

    /** upper/lower ratio (0..1 where 0.5 is balanced). Returns null when no data. */
    fun upperLowerRatio(state: VolumeAnalyticsUiState = _uiState.value): Double? {
        val breakdown = state.currentData?.summary?.muscleGroupBreakdown ?: return null
        val upper = breakdown.entries
            .filter { it.key.lowercase() in UPPER_MUSCLE_GROUPS }
            .sumOf { it.value }
        val lower = breakdown.entries
            .filter { it.key.lowercase() in LOWER_MUSCLE_GROUPS }
            .sumOf { it.value }
        val total = upper + lower
        return if (total > 0) upper / total else null
    }

    /** Volume change percentage vs. previous period. */
    fun volumeChangePct(state: VolumeAnalyticsUiState = _uiState.value): Double? {
        val current = state.currentData?.summary?.totalVolume ?: return null
        val previous = state.previousData?.summary?.totalVolume ?: return null
        return if (previous > 0) ((current - previous) / previous) * 100.0 else null
    }

    /** Muscle groups sorted by descending volume. */
    fun sortedMuscleGroups(state: VolumeAnalyticsUiState = _uiState.value): List<Pair<String, Double>> {
        val breakdown = state.currentData?.summary?.muscleGroupBreakdown ?: return emptyList()
        return breakdown.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }

    // ---------------------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------------------

    fun changePeriod(period: AnalyticsPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadVolume() }
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadVolume() }
    }

    // ---------------------------------------------------------------------------
    // Internal
    // ---------------------------------------------------------------------------

    private fun loadVolume() {
        val period = _uiState.value.selectedPeriod
        val today = LocalDate.now()
        val currentStart = today.minusDays(period.days.toLong())
        val previousStart = currentStart.minusDays(period.days.toLong())

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                // Fetch current and previous period in parallel via separate launches
                var currentData: VolumeAnalyticsResponse? = null
                var previousData: VolumeAnalyticsResponse? = null

                val currentJob = launch {
                    val resp = api.fetchVolumeAnalytics(
                        startDate = currentStart.format(DATE_FMT),
                        endDate = today.format(DATE_FMT),
                        granularity = period.granularity
                    )
                    if (resp.isSuccessful) {
                        currentData = resp.body()
                    } else {
                        Log.w(TAG, "Current period request failed: ${resp.code()}")
                    }
                }

                val previousJob = launch {
                    val resp = api.fetchVolumeAnalytics(
                        startDate = previousStart.format(DATE_FMT),
                        endDate = currentStart.minusDays(1).format(DATE_FMT),
                        granularity = period.granularity
                    )
                    if (resp.isSuccessful) {
                        previousData = resp.body()
                    } else {
                        Log.w(TAG, "Previous period request failed: ${resp.code()}")
                    }
                }

                currentJob.join()
                previousJob.join()

                _uiState.update {
                    it.copy(
                        currentData = currentData,
                        previousData = previousData,
                        isLoading = false,
                        errorMessage = if (currentData == null) "Failed to load volume data" else null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Volume analytics error", e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unexpected error")
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Muscle group classification
    // ---------------------------------------------------------------------------

    companion object {
        private val PUSH_MUSCLE_GROUPS = setOf(
            "chest", "shoulders", "triceps", "pecs", "deltoids"
        )
        private val PULL_MUSCLE_GROUPS = setOf(
            "back", "biceps", "lats", "rhomboids", "traps", "rear delts"
        )
        private val UPPER_MUSCLE_GROUPS = PUSH_MUSCLE_GROUPS + PULL_MUSCLE_GROUPS + setOf(
            "core", "abs", "obliques"
        )
        private val LOWER_MUSCLE_GROUPS = setOf(
            "quads", "hamstrings", "glutes", "calves", "hip flexors", "adductors", "legs"
        )
    }
}
