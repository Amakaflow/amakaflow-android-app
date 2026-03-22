package com.amakaflow.wear.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.shared.model.WearWorkout
import com.amakaflow.wear.data.repository.WearWorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutListViewModel @Inject constructor(
    private val repository: WearWorkoutRepository
) : ViewModel() {

    val workouts: StateFlow<List<WearWorkout>> = repository.workouts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isPhoneConnected: StateFlow<Boolean> = repository.isPhoneConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            repository.checkPhoneConnection()
        }
    }

    fun requestSync() {
        viewModelScope.launch {
            repository.requestSync()
        }
    }
}
