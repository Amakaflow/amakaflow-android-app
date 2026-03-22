package com.amakaflow.wear.presentation

import androidx.lifecycle.ViewModel
import com.amakaflow.shared.model.WearReadinessData
import com.amakaflow.wear.data.repository.WearWorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ReadinessViewModel @Inject constructor(
    repository: WearWorkoutRepository
) : ViewModel() {

    val readiness: StateFlow<WearReadinessData?> = repository.readiness
}
