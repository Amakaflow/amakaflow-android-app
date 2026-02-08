package com.amakaflow.companion.ui.screens.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.Workout
import com.amakaflow.companion.data.model.WorkoutInterval
import com.amakaflow.companion.data.model.WorkoutSport
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

/**
 * Represents a flattened step in the workout breakdown
 */
private sealed class BreakdownItem {
    data class IntervalStep(
        val index: Int,
        val interval: WorkoutInterval,
        val duration: String
    ) : BreakdownItem()

    data class NestedExercise(
        val name: String,
        val duration: String
    ) : BreakdownItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutId: String,
    onNavigateBack: () -> Unit,
    onStartWorkout: (String) -> Unit,
    viewModel: WorkoutDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(workoutId) {
        viewModel.loadWorkout(workoutId)
    }

    Scaffold(
        modifier = Modifier.testTag("workout_detail_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.workout?.name?.take(30)?.let {
                            if (uiState.workout?.name?.length ?: 0 > 30) "$it..." else it
                        } ?: "",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmakaColors.background,
                    titleContentColor = AmakaColors.textPrimary,
                    navigationIconContentColor = AmakaColors.textPrimary
                )
            )
        },
        bottomBar = {
            uiState.workout?.let { workout ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AmakaColors.background
                ) {
                    Button(
                        onClick = { onStartWorkout(workout.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AmakaSpacing.md.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmakaColors.accentBlue
                        ),
                        shape = RoundedCornerShape(AmakaCornerRadius.lg.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                        Text(
                            text = "Start Follow-Along",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        },
        containerColor = AmakaColors.background
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AmakaColors.accentBlue)
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("error_view"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = AmakaColors.textSecondary
                    )
                }
            }
            uiState.workout != null -> {
                WorkoutDetailContent(
                    workout = uiState.workout!!,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun WorkoutDetailContent(
    workout: Workout,
    modifier: Modifier = Modifier
) {
    val breakdownItems = remember(workout) { buildBreakdownItems(workout.intervals) }
    val totalSteps = remember(workout) { countTotalSteps(workout.intervals) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AmakaSpacing.md.dp)
            .testTag("exercise_list"),
        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
    ) {
        // Header Card
        item {
            WorkoutHeaderCard(workout = workout)
        }

        // Stats Card
        item {
            StatsCard(
                duration = workout.formattedDuration,
                steps = totalSteps
            )
        }

        // Section Title
        item {
            Text(
                text = "Step-by-Step Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.padding(top = AmakaSpacing.sm.dp)
            )
        }

        // Breakdown Items
        itemsIndexed(breakdownItems) { _, item ->
            when (item) {
                is BreakdownItem.IntervalStep -> {
                    IntervalStepRow(item)
                }
                is BreakdownItem.NestedExercise -> {
                    NestedExerciseRow(item)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))
        }
    }
}

@Composable
private fun WorkoutHeaderCard(workout: Workout) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.lg.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.lg.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Workout Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(AmakaCornerRadius.md.dp))
                    .background(AmakaColors.accentBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = AmakaColors.accentBlue,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AmakaColors.textPrimary,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = workout.sport.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmakaColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun StatsCard(duration: String, steps: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.lg.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp)
        ) {
            // Duration
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = AmakaColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textSecondary
                    )
                }
                Text(
                    text = duration,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AmakaColors.textPrimary
                )
            }

            // Steps
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.FormatListNumbered,
                        contentDescription = null,
                        tint = AmakaColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Steps",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textSecondary
                    )
                }
                Text(
                    text = steps.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AmakaColors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun IntervalStepRow(item: BreakdownItem.IntervalStep) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step number in circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AmakaColors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.index.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = AmakaColors.textSecondary
                )
            }

            Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))

            // Icon for interval type
            Icon(
                imageVector = item.interval.icon,
                contentDescription = null,
                tint = item.interval.iconColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))

            // Interval name
            Text(
                text = item.interval.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = AmakaColors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            // Duration
            Text(
                text = item.duration,
                style = MaterialTheme.typography.bodyMedium,
                color = AmakaColors.textSecondary
            )
        }
    }
}

@Composable
private fun NestedExerciseRow(item: BreakdownItem.NestedExercise) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = AmakaSpacing.md.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bullet point
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = AmakaColors.textSecondary
        )

        Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))

        // Exercise name
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            color = AmakaColors.textSecondary,
            modifier = Modifier.weight(1f)
        )

        // Duration
        Text(
            text = item.duration,
            style = MaterialTheme.typography.bodySmall,
            color = AmakaColors.textTertiary
        )
    }
}

/**
 * Build the breakdown items list, expanding Repeat intervals to show nested exercises
 */
private fun buildBreakdownItems(intervals: List<WorkoutInterval>): List<BreakdownItem> {
    val items = mutableListOf<BreakdownItem>()
    var stepIndex = 1

    for (interval in intervals) {
        items.add(
            BreakdownItem.IntervalStep(
                index = stepIndex,
                interval = interval,
                duration = interval.totalDuration
            )
        )
        stepIndex++

        // For Repeat intervals, add nested exercises
        if (interval is WorkoutInterval.Repeat) {
            for (nestedInterval in interval.intervals) {
                items.add(
                    BreakdownItem.NestedExercise(
                        name = nestedInterval.displayName,
                        duration = nestedInterval.totalDuration
                    )
                )
            }
        }
    }

    return items
}

/**
 * Count total steps including expanded repeats
 * Rest intervals inside repeats are not counted as separate steps
 */
private fun countTotalSteps(intervals: List<WorkoutInterval>): Int {
    var count = 0
    for (interval in intervals) {
        when (interval) {
            is WorkoutInterval.Repeat -> {
                // Count non-rest intervals inside repeat
                val nonRestCount = interval.intervals.count { it !is WorkoutInterval.Rest }
                count += 1 + (nonRestCount * interval.reps)
            }
            is WorkoutInterval.Rest -> {
                // Rest intervals at top level count as steps
                count++
            }
            else -> count++
        }
    }
    return count
}

// Extension properties for WorkoutInterval
private val WorkoutInterval.displayName: String
    get() = when (this) {
        is WorkoutInterval.Warmup -> "Warm Up"
        is WorkoutInterval.Cooldown -> "Cool Down"
        is WorkoutInterval.Time -> target ?: "Work"
        is WorkoutInterval.Reps -> name
        is WorkoutInterval.Distance -> "Distance"
        is WorkoutInterval.Repeat -> "Repeat ${reps}x"
        is WorkoutInterval.Rest -> if (seconds != null) "Rest" else "Rest"
    }

private val WorkoutInterval.totalDuration: String
    get() = when (this) {
        is WorkoutInterval.Warmup -> formatTime(seconds)
        is WorkoutInterval.Cooldown -> formatTime(seconds)
        is WorkoutInterval.Time -> formatTime(seconds)
        is WorkoutInterval.Reps -> {
            // Estimate time for reps-based exercises
            val totalSeconds = (restSec ?: 0) * (sets ?: 1)
            if (totalSeconds > 0) formatTime(totalSeconds) else "0m"
        }
        is WorkoutInterval.Distance -> formatDistance(meters)
        is WorkoutInterval.Repeat -> {
            // Calculate total time for all nested intervals * reps
            val nestedDuration = intervals.sumOf { nested ->
                when (nested) {
                    is WorkoutInterval.Time -> nested.seconds
                    is WorkoutInterval.Warmup -> nested.seconds
                    is WorkoutInterval.Cooldown -> nested.seconds
                    is WorkoutInterval.Reps -> nested.restSec ?: 0
                    is WorkoutInterval.Rest -> nested.seconds ?: 0
                    else -> 0
                }
            }
            formatTime(nestedDuration * reps)
        }
        is WorkoutInterval.Rest -> seconds?.let { formatTime(it) } ?: "Tap when ready"
    }

private val WorkoutInterval.icon: ImageVector
    get() = when (this) {
        is WorkoutInterval.Warmup -> Icons.Filled.Whatshot
        is WorkoutInterval.Cooldown -> Icons.Filled.AcUnit
        is WorkoutInterval.Time -> Icons.Filled.Timer
        is WorkoutInterval.Reps -> Icons.Filled.FitnessCenter
        is WorkoutInterval.Distance -> Icons.Filled.DirectionsRun
        is WorkoutInterval.Repeat -> Icons.Filled.Refresh
        is WorkoutInterval.Rest -> Icons.Filled.HourglassEmpty
    }

private val WorkoutInterval.iconColor: Color
    get() = when (this) {
        is WorkoutInterval.Warmup -> AmakaColors.accentOrange
        is WorkoutInterval.Cooldown -> AmakaColors.accentBlue
        is WorkoutInterval.Time -> AmakaColors.accentGreen
        is WorkoutInterval.Reps -> AmakaColors.accentPurple
        is WorkoutInterval.Distance -> AmakaColors.accentGreen
        is WorkoutInterval.Repeat -> AmakaColors.accentYellow
        is WorkoutInterval.Rest -> AmakaColors.textSecondary  // Gray for rest intervals
    }

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) {
        if (secs > 0) "${mins}m ${secs}s" else "${mins}m"
    } else {
        "${secs}s"
    }
}

private fun formatDistance(meters: Int): String {
    return if (meters >= 1000) {
        String.format("%.1f km", meters / 1000.0)
    } else {
        "${meters}m"
    }
}

// Extension for sport
private val WorkoutSport.displayName: String
    get() = when (this) {
        WorkoutSport.RUNNING -> "Running"
        WorkoutSport.CYCLING -> "Cycling"
        WorkoutSport.STRENGTH -> "Strength"
        WorkoutSport.MOBILITY -> "Mobility"
        WorkoutSport.SWIMMING -> "Swimming"
        WorkoutSport.CARDIO -> "Cardio"
        WorkoutSport.OTHER -> "Other"
    }
