package com.amakaflow.companion.data.repository

import android.util.Log
import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.model.*
import com.amakaflow.companion.domain.Result
import com.amakaflow.companion.domain.repository.PlannerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PlannerRepository"

@Singleton
class PlannerRepositoryImpl @Inject constructor(
    private val api: AmakaflowApi
) : PlannerRepository {

    // -------------------------------------------------------------------------
    // Calendar / DayState
    // -------------------------------------------------------------------------

    override fun getDayState(date: String): Flow<Result<DayState>> = flow {
        emit(Result.Loading)
        try {
            val response = api.getDayState(date)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.Success(response.body()!!.dayState))
            } else {
                emit(Result.Error("Failed to load day state: ${response.code()}", response.code()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getDayState error", e)
            emit(Result.Error(e.message ?: "Unknown error", exception = e))
        }
    }

    override fun getWeekState(startDate: String): Flow<Result<List<DayState>>> = flow {
        emit(Result.Loading)
        try {
            val response = api.getWeekState(startDate)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.Success(response.body()!!.days))
            } else {
                emit(Result.Error("Failed to load week state: ${response.code()}", response.code()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getWeekState error", e)
            emit(Result.Error(e.message ?: "Unknown error", exception = e))
        }
    }

    // -------------------------------------------------------------------------
    // Planner
    // -------------------------------------------------------------------------

    override fun generateWeek(startDate: String, goalDescription: String?): Flow<Result<WeekPlan>> = flow {
        emit(Result.Loading)
        try {
            val request = GenerateWeekRequest(startDate = startDate, goalDescription = goalDescription)
            val response = api.generateWeek(request)
            if (response.isSuccessful && response.body()?.plan != null) {
                emit(Result.Success(response.body()!!.plan!!))
            } else {
                val msg = response.body()?.message ?: "Failed to generate week plan: ${response.code()}"
                emit(Result.Error(msg, response.code()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "generateWeek error", e)
            emit(Result.Error(e.message ?: "Unknown error", exception = e))
        }
    }

    // -------------------------------------------------------------------------
    // Coach Chat
    // -------------------------------------------------------------------------

    override fun sendCoachMessage(message: String, conversationId: String?): Flow<Result<CoachMessageResponse>> = flow {
        emit(Result.Loading)
        try {
            val request = CoachMessageRequest(message = message, conversationId = conversationId)
            val response = api.sendCoachMessage(request)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.Success(response.body()!!))
            } else {
                emit(Result.Error("Coach message failed: ${response.code()}", response.code()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendCoachMessage error", e)
            emit(Result.Error(e.message ?: "Unknown error", exception = e))
        }
    }

    // -------------------------------------------------------------------------
    // Activity Feed
    // -------------------------------------------------------------------------

    override fun getActivityFeed(limit: Int, cursor: String?): Flow<Result<ActivityFeedResponse>> = flow {
        emit(Result.Loading)
        try {
            val response = api.getActivityFeed(limit, cursor)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.Success(response.body()!!))
            } else {
                emit(Result.Error("Failed to load activity feed: ${response.code()}", response.code()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getActivityFeed error", e)
            emit(Result.Error(e.message ?: "Unknown error", exception = e))
        }
    }

    // -------------------------------------------------------------------------
    // Training Preferences
    // -------------------------------------------------------------------------

    override fun getTrainingPreferences(): Flow<Result<TrainingPreferences>> = flow {
        emit(Result.Loading)
        try {
            val response = api.getTrainingPreferences()
            if (response.isSuccessful && response.body() != null) {
                emit(Result.Success(response.body()!!.preferences))
            } else {
                emit(Result.Error("Failed to load preferences: ${response.code()}", response.code()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTrainingPreferences error", e)
            emit(Result.Error(e.message ?: "Unknown error", exception = e))
        }
    }

    override suspend fun updateTrainingPreferences(preferences: TrainingPreferences): Result<TrainingPreferences> {
        return try {
            val response = api.updateTrainingPreferences(preferences)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!.preferences)
            } else {
                Result.Error("Failed to update preferences: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateTrainingPreferences error", e)
            Result.Error(e.message ?: "Unknown error", exception = e)
        }
    }

    // -------------------------------------------------------------------------
    // Conflicts
    // -------------------------------------------------------------------------

    override fun getConflicts(date: String): Flow<Result<List<ScheduleConflict>>> = flow {
        emit(Result.Loading)
        try {
            val response = api.getConflicts(date)
            if (response.isSuccessful && response.body() != null) {
                emit(Result.Success(response.body()!!.conflicts))
            } else {
                emit(Result.Error("Failed to load conflicts: ${response.code()}", response.code()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getConflicts error", e)
            emit(Result.Error(e.message ?: "Unknown error", exception = e))
        }
    }

    // -------------------------------------------------------------------------
    // Shoe Comparison
    // -------------------------------------------------------------------------

    override fun compareShoes(shoes: List<String>, sport: String?): Flow<Result<ShoeComparison>> = flow {
        emit(Result.Loading)
        try {
            val request = ShoeComparisonRequest(shoes = shoes, sport = sport)
            val response = api.compareShoes(request)
            if (response.isSuccessful && response.body()?.comparison != null) {
                emit(Result.Success(response.body()!!.comparison!!))
            } else {
                emit(Result.Error("Shoe comparison failed: ${response.code()}", response.code()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "compareShoes error", e)
            emit(Result.Error(e.message ?: "Unknown error", exception = e))
        }
    }

    // -------------------------------------------------------------------------
    // Fatigue Advisor
    // -------------------------------------------------------------------------

    override fun getFatigueAdvice(): Flow<Result<FatigueAdvisorResponse>> = flow {
        emit(Result.Loading)
        try {
            val response = api.getFatigueAdvice()
            if (response.isSuccessful && response.body() != null) {
                emit(Result.Success(response.body()!!))
            } else {
                emit(Result.Error("Failed to load fatigue advice: ${response.code()}", response.code()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getFatigueAdvice error", e)
            emit(Result.Error(e.message ?: "Unknown error", exception = e))
        }
    }
}
