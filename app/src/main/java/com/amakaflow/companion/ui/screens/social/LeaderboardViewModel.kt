package com.amakaflow.companion.ui.screens.social

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.model.DimensionLeaderboardEntry
import com.amakaflow.companion.data.model.DimensionLeaderboardResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "LeaderboardViewModel"

enum class LeaderboardDimension(val apiValue: String, val displayName: String, val unit: String) {
    CONSISTENCY("consistency", "Consistency", "weeks"),
    VOLUME("volume", "Volume", "kg"),
    PRS("prs", "PRs", "PRs"),
    WORKOUTS("workouts", "Workouts", "workouts")
}

enum class LeaderboardPeriod(val apiValue: String, val displayName: String) {
    WEEK("week", "This Week"),
    MONTH("month", "This Month"),
    ALL_TIME("all_time", "All Time")
}

enum class LeaderboardScope(val displayName: String) {
    FRIENDS("Friends"),
    CREW("Crew")
}

data class LeaderboardUiState(
    val entries: List<DimensionLeaderboardEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedDimension: LeaderboardDimension = LeaderboardDimension.VOLUME,
    val selectedPeriod: LeaderboardPeriod = LeaderboardPeriod.MONTH,
    val selectedScope: LeaderboardScope = LeaderboardScope.FRIENDS
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val api: AmakaflowApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    var crewId: String? = null

    init {
        loadLeaderboard()
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val state = _uiState.value
                val response = when (state.selectedScope) {
                    LeaderboardScope.FRIENDS -> api.getFriendsLeaderboard(
                        dimension = state.selectedDimension.apiValue,
                        period = state.selectedPeriod.apiValue
                    )
                    LeaderboardScope.CREW -> {
                        val cId = crewId
                        if (cId == null) {
                            _uiState.update { it.copy(error = "No crew selected", isLoading = false) }
                            return@launch
                        }
                        api.getCrewLeaderboard(
                            crewId = cId,
                            dimension = state.selectedDimension.apiValue,
                            period = state.selectedPeriod.apiValue
                        )
                    }
                }
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _uiState.update {
                        it.copy(entries = body.entries, isLoading = false)
                    }
                } else {
                    _uiState.update {
                        it.copy(error = "Failed to load leaderboard: ${response.code()}", isLoading = false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadLeaderboard failed", e)
                _uiState.update {
                    it.copy(error = "Could not load leaderboard: ${e.message}", isLoading = false)
                }
            }
        }
    }

    fun changeDimension(dimension: LeaderboardDimension) {
        _uiState.update { it.copy(selectedDimension = dimension) }
        loadLeaderboard()
    }

    fun changePeriod(period: LeaderboardPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadLeaderboard()
    }

    fun changeScope(scope: LeaderboardScope) {
        _uiState.update { it.copy(selectedScope = scope) }
        loadLeaderboard()
    }

    fun formattedValue(entry: DimensionLeaderboardEntry): String {
        val state = _uiState.value
        return when (state.selectedDimension) {
            LeaderboardDimension.VOLUME -> {
                if (entry.value >= 1000) {
                    String.format("%.1fk kg", entry.value / 1000)
                } else {
                    String.format("%.0f kg", entry.value)
                }
            }
            LeaderboardDimension.CONSISTENCY -> {
                val weeks = entry.value.toInt()
                "$weeks ${if (weeks == 1) "week" else "weeks"}"
            }
            LeaderboardDimension.PRS -> "${entry.value.toInt()} PRs"
            LeaderboardDimension.WORKOUTS -> "${entry.value.toInt()}"
        }
    }
}
