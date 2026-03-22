package com.amakaflow.wear.presentation

import androidx.lifecycle.ViewModel
import com.amakaflow.shared.model.WearDaySchedule
import com.amakaflow.wear.data.repository.WearWorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TodayScheduleViewModel @Inject constructor(
    repository: WearWorkoutRepository
) : ViewModel() {

    val schedule: StateFlow<WearDaySchedule?> = repository.schedule
}
