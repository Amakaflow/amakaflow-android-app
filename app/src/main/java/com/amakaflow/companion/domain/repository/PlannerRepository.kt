package com.amakaflow.companion.domain.repository

import com.amakaflow.companion.data.model.*
import com.amakaflow.companion.domain.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for planner, coach, calendar, activity feed, preferences,
 * shoe comparison, and fatigue advisor operations.
 * AMA-1148: New backend API integration.
 */
interface PlannerRepository {

    // Calendar / DayState
    fun getDayState(date: String): Flow<Result<DayState>>
    fun getWeekState(startDate: String): Flow<Result<List<DayState>>>

    // Planner
    fun generateWeek(startDate: String, goalDescription: String? = null): Flow<Result<WeekPlan>>

    // Coach Chat
    fun sendCoachMessage(
        message: String,
        conversationId: String? = null
    ): Flow<Result<CoachMessageResponse>>

    // Activity Feed
    fun getActivityFeed(limit: Int = 20, cursor: String? = null): Flow<Result<ActivityFeedResponse>>

    // Training Preferences
    fun getTrainingPreferences(): Flow<Result<TrainingPreferences>>
    suspend fun updateTrainingPreferences(preferences: TrainingPreferences): Result<TrainingPreferences>

    // Conflicts
    fun getConflicts(date: String): Flow<Result<List<ScheduleConflict>>>

    // Shoe Comparison
    fun compareShoes(shoes: List<String>, sport: String? = null): Flow<Result<ShoeComparison>>

    // Fatigue Advisor

    // Suggest Workout (AMA-1265)
    fun suggestWorkout(
        durationMinutes: Int? = null,
        focusMuscleGroups: List<String>? = null,
        notes: String? = null
    ): Flow<Result<SuggestWorkoutResponse>>
    fun getFatigueAdvice(): Flow<Result<FatigueAdvisorResponse>>
}
