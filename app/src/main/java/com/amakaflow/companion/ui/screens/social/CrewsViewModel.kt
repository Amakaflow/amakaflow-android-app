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

private const val TAG = "CrewsViewModel"

data class CrewsUiState(
    val crews: List<Crew> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    // Detail state
    val selectedCrewDetail: CrewDetail? = null,
    val isLoadingDetail: Boolean = false,
    val crewFeedPosts: List<CrewFeedPost> = emptyList(),
    val isLoadingFeed: Boolean = false,
    // Creation state
    val isCreating: Boolean = false,
    val createError: String? = null,
    // Join state
    val isJoining: Boolean = false,
    val joinError: String? = null,
    val joinSuccess: Boolean = false
)

@HiltViewModel
class CrewsViewModel @Inject constructor(
    private val api: AmakaflowApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrewsUiState())
    val uiState: StateFlow<CrewsUiState> = _uiState.asStateFlow()

    init {
        loadCrews()
    }

    fun loadCrews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = api.getMyCrews()
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _uiState.update {
                        it.copy(crews = body.crews, isLoading = false)
                    }
                } else {
                    _uiState.update {
                        it.copy(error = "Failed to load crews: ${response.code()}", isLoading = false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadCrews failed", e)
                _uiState.update {
                    it.copy(error = "Could not load crews: ${e.message}", isLoading = false)
                }
            }
        }
    }

    fun loadCrewDetail(crewId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true) }
            try {
                val response = api.getCrewDetail(crewId)
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(selectedCrewDetail = response.body(), isLoadingDetail = false)
                    }
                } else {
                    _uiState.update {
                        it.copy(error = "Failed to load crew: ${response.code()}", isLoadingDetail = false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadCrewDetail failed", e)
                _uiState.update {
                    it.copy(error = "Could not load crew: ${e.message}", isLoadingDetail = false)
                }
            }
        }
    }

    fun loadCrewFeed(crewId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFeed = true) }
            try {
                val response = api.getCrewFeed(crewId)
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(crewFeedPosts = response.body()?.posts ?: emptyList(), isLoadingFeed = false)
                    }
                } else {
                    _uiState.update { it.copy(isLoadingFeed = false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadCrewFeed failed", e)
                _uiState.update { it.copy(isLoadingFeed = false) }
            }
        }
    }

    fun createCrew(name: String, description: String?, maxMembers: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, createError = null) }
            try {
                val request = CreateCrewApiRequest(name, description, maxMembers)
                val response = api.createCrew(request)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isCreating = false) }
                    loadCrews()
                } else {
                    _uiState.update {
                        it.copy(createError = "Failed to create crew: ${response.code()}", isCreating = false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "createCrew failed", e)
                _uiState.update {
                    it.copy(createError = "Could not create crew: ${e.message}", isCreating = false)
                }
            }
        }
    }

    fun joinCrew(crewId: String, inviteCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, joinError = null, joinSuccess = false) }
            try {
                val request = JoinCrewApiRequest(inviteCode)
                val response = api.joinCrew(crewId, request)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isJoining = false, joinSuccess = true) }
                    loadCrews()
                } else {
                    _uiState.update {
                        it.copy(joinError = "Failed to join crew: ${response.code()}", isJoining = false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "joinCrew failed", e)
                _uiState.update {
                    it.copy(joinError = "Could not join crew: ${e.message}", isJoining = false)
                }
            }
        }
    }

    fun leaveCrew(crewId: String) {
        viewModelScope.launch {
            try {
                val response = api.leaveCrew(crewId)
                if (response.isSuccessful) {
                    loadCrews()
                }
            } catch (e: Exception) {
                Log.e(TAG, "leaveCrew failed", e)
                _uiState.update { it.copy(error = "Could not leave crew: ${e.message}") }
            }
        }
    }
}
