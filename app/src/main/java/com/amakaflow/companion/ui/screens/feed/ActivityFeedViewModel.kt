package com.amakaflow.companion.ui.screens.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.model.ActivityFeedItem
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.PlannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ActivityFeedViewModel"

data class ActivityFeedUiState(
    val items: List<ActivityFeedItem> = emptyList(),
    val isLoading: Boolean = true,
    val hasMore: Boolean = false,
    val nextCursor: String? = null,
    val error: String? = null
)

@HiltViewModel
class ActivityFeedViewModel @Inject constructor(
    private val plannerRepository: PlannerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityFeedUiState())
    val uiState: StateFlow<ActivityFeedUiState> = _uiState.asStateFlow()

    init {
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            plannerRepository.getActivityFeed().collect { result ->
                when (result) {
                    is Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Result.Success -> {
                        val response = result.data
                        _uiState.update {
                            it.copy(
                                items = response.items,
                                isLoading = false,
                                hasMore = response.hasMore,
                                nextCursor = response.nextCursor,
                                error = null
                            )
                        }
                    }
                    is Result.Error -> {
                        Log.e(TAG, "loadFeed error: ${result.message}")
                        _uiState.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                    }
                }
            }
        }
    }

    fun loadMore() {
        val cursor = _uiState.value.nextCursor ?: return
        viewModelScope.launch {
            plannerRepository.getActivityFeed(cursor = cursor).collect { result ->
                if (result is Result.Success) {
                    val response = result.data
                    _uiState.update {
                        it.copy(
                            items = it.items + response.items,
                            hasMore = response.hasMore,
                            nextCursor = response.nextCursor
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(nextCursor = null) }
        loadFeed()
    }
}
