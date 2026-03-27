package com.amakaflow.companion.ui.screens.social

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ChallengesViewModel"

data class ChallengesUiState(
    val challenges: List<Challenge> = emptyList(),
    val filteredChallenges: List<Challenge> = emptyList(),
    val selectedTypeFilter: ChallengeType? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    // Detail state
    val selectedChallenge: ChallengeDetailResponse? = null,
    val isLoadingDetail: Boolean = false,
    val isJoining: Boolean = false,
    // Creation state
    val isCreating: Boolean = false,
    val createError: String? = null,
    // Celebration state
    val showCelebration: Boolean = false,
    val completedBadge: ChallengeBadge? = null
)

@HiltViewModel
class ChallengesViewModel @Inject constructor(
    private val api: AmakaflowApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChallengesUiState())
    val uiState: StateFlow<ChallengesUiState> = _uiState.asStateFlow()

    init {
        loadChallenges()
    }

    fun loadChallenges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.getChallenges()
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _uiState.update {
                        it.copy(
                            challenges = body.challenges,
                            filteredChallenges = applyFilter(body.challenges, it.selectedTypeFilter),
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to load challenges (${response.code()})")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadChallenges error", e)
                _uiState.update {
                    it.copy(isLoading = false, error = "Could not load challenges: ${e.message}")
                }
            }
        }
    }

    fun setTypeFilter(type: ChallengeType?) {
        _uiState.update {
            it.copy(
                selectedTypeFilter = type,
                filteredChallenges = applyFilter(it.challenges, type)
            )
        }
    }

    private fun applyFilter(challenges: List<Challenge>, type: ChallengeType?): List<Challenge> {
        return if (type != null) {
            challenges.filter { it.type == type }
        } else {
            challenges
        }
    }

    fun loadChallengeDetail(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true) }
            try {
                val response = api.getChallengeDetail(id)
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _uiState.update {
                        it.copy(
                            selectedChallenge = body,
                            isLoadingDetail = false,
                            showCelebration = body.myProgress?.isCompleted == true && body.myProgress.badge != null,
                            completedBadge = body.myProgress?.badge
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoadingDetail = false, error = "Failed to load challenge")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadChallengeDetail error", e)
                _uiState.update {
                    it.copy(isLoadingDetail = false, error = "Could not load challenge: ${e.message}")
                }
            }
        }
    }

    fun joinChallenge(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true) }
            try {
                val response = api.joinChallenge(id)
                if (response.isSuccessful) {
                    loadChallengeDetail(id)
                    loadChallenges()
                } else {
                    _uiState.update {
                        it.copy(isJoining = false, error = "Failed to join challenge")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "joinChallenge error", e)
                _uiState.update {
                    it.copy(isJoining = false, error = "Could not join challenge: ${e.message}")
                }
            }
            _uiState.update { it.copy(isJoining = false) }
        }
    }

    fun createChallenge(request: CreateChallengeRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, createError = null) }
            try {
                val response = api.createChallenge(request)
                if (response.isSuccessful) {
                    loadChallenges()
                    _uiState.update { it.copy(isCreating = false) }
                    onSuccess()
                } else {
                    _uiState.update {
                        it.copy(isCreating = false, createError = "Failed to create challenge (${response.code()})")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "createChallenge error", e)
                _uiState.update {
                    it.copy(isCreating = false, createError = "Could not create challenge: ${e.message}")
                }
            }
        }
    }

    fun dismissCelebration() {
        _uiState.update { it.copy(showCelebration = false, completedBadge = null) }
    }
}
