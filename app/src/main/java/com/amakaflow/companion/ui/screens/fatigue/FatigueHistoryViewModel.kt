package com.amakaflow.companion.ui.screens.fatigue

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.model.DayState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

private const val TAG = "FatigueHistoryVM"

@HiltViewModel
class FatigueHistoryViewModel @Inject constructor(
    private val api: AmakaflowApi
) : ViewModel() {

    enum class DateRange(val label: String, val days: Int) {
        ONE_WEEK("1W", 7),
        TWO_WEEKS("2W", 14),
        ONE_MONTH("1M", 30)
    }

    var dayStates by mutableStateOf<List<DayState>>(emptyList())
        private set

    var selectedRange by mutableStateOf(DateRange.TWO_WEEKS)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    // Computed stats
    val averageScore: Double
        get() {
            val scores = dayStates.mapNotNull { it.readinessScore }
            return if (scores.isEmpty()) 0.0 else scores.average()
        }

    val greenDays: Int
        get() = dayStates.count { (it.readinessScore ?: 0.0) >= 70.0 }

    val yellowDays: Int
        get() = dayStates.count { score ->
            val r = score.readinessScore ?: 0.0
            r in 40.0..69.9
        }

    val redDays: Int
        get() = dayStates.count { (it.readinessScore ?: 100.0) < 40.0 }

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val tz = TimeZone.currentSystemDefault()
                val today = Clock.System.now().toLocalDateTime(tz).date
                val fromDate = today.minus(selectedRange.days, DateTimeUnit.DAY)

                // Use getDayState iteratively or getWeekState in batches
                // Use week-state for the range to minimize API calls
                val results = mutableListOf<DayState>()
                var cursor = fromDate
                while (cursor <= today) {
                    val weekResponse = api.getWeekState(cursor.toString())
                    if (weekResponse.isSuccessful) {
                        weekResponse.body()?.days?.let { days ->
                            results.addAll(days.filter { day ->
                                day.date >= fromDate.toString() && day.date <= today.toString()
                            })
                        }
                    }
                    cursor = cursor.plus(7, DateTimeUnit.DAY)
                }

                // Deduplicate and sort
                val deduplicated = results
                    .distinctBy { it.date }
                    .sortedByDescending { it.date }

                dayStates = deduplicated
            } catch (e: Exception) {
                Log.e(TAG, "loadHistory error", e)
                error = e.message ?: "Failed to load history"
            } finally {
                isLoading = false
            }
        }
    }

    fun changeRange(range: DateRange) {
        selectedRange = range
        loadHistory()
    }
}
