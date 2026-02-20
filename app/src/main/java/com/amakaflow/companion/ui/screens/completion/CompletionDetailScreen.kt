package com.amakaflow.companion.ui.screens.completion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.CompletionSource
import com.amakaflow.companion.data.model.HeartRateDataPoint
import com.amakaflow.companion.data.model.IntervalLog
import com.amakaflow.companion.data.model.IntervalStatus
import com.amakaflow.companion.data.model.SetLog
import com.amakaflow.companion.data.model.WorkoutCompletionDetail
import com.amakaflow.companion.data.model.WorkoutIntervalSubmission
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletionDetailScreen(
    completionId: String,
    onNavigateBack: () -> Unit,
    onRunAgain: ((String) -> Unit)? = null,
    onSaveToMyWorkouts: (() -> Unit)? = null,
    viewModel: CompletionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.sm.dp, vertical = AmakaSpacing.sm.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AmakaColors.textPrimary
                )
            }
            Text(
                text = "Workout Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AmakaColors.accentBlue)
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error loading details",
                            style = MaterialTheme.typography.titleMedium,
                            color = AmakaColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.textSecondary
                        )
                    }
                }
            }
            uiState.completion != null -> {
                CompletionDetailContent(
                    completion = uiState.completion!!,
                    onRunAgain = onRunAgain,
                    onSaveToMyWorkouts = onSaveToMyWorkouts
                )
            }
        }
    }
}

@Composable
private fun CompletionDetailContent(
    completion: WorkoutCompletionDetail,
    onRunAgain: ((String) -> Unit)? = null,
    onSaveToMyWorkouts: (() -> Unit)? = null
) {
    val localDateTime = completion.startedAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val dateString = "${months[localDateTime.monthNumber - 1]} ${localDateTime.dayOfMonth}, ${localDateTime.year}"

    // Format start time
    val startHour = localDateTime.hour
    val startMinute = localDateTime.minute
    val startAmPm = if (startHour >= 12) "PM" else "AM"
    val startHour12 = if (startHour == 0) 12 else if (startHour > 12) startHour - 12 else startHour
    val startTimeString = String.format("%d:%02d %s", startHour12, startMinute, startAmPm)

    // Format end time
    val endInstant = completion.resolvedEndedAt
    val endLocalDateTime = endInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    val endHour = endLocalDateTime.hour
    val endMinute = endLocalDateTime.minute
    val endAmPm = if (endHour >= 12) "PM" else "AM"
    val endHour12 = if (endHour == 0) 12 else if (endHour > 12) endHour - 12 else endHour
    val endTimeString = String.format("%d:%02d %s", endHour12, endMinute, endAmPm)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AmakaSpacing.md.dp),
        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
    ) {
        // Header section (like iOS)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AmakaColors.surface,
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AmakaSpacing.md.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Workout name
                    Text(
                        text = completion.workoutName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AmakaColors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))

                    // Date
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmakaColors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

                    // Prominent duration display
                    Text(
                        text = completion.formattedDuration,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmakaColors.textPrimary
                    )
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmakaColors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

                    // Time range (start → end)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = AmakaColors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = startTimeString,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.textSecondary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = AmakaColors.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = endTimeString,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.textSecondary
                        )
                    }
                }
            }
        }

        // Calories card - always visible
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AmakaColors.surface,
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AmakaSpacing.md.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = AmakaColors.accentOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                    Text(
                        text = "${completion.activeCalories ?: 0}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AmakaColors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(AmakaSpacing.xs.dp))
                    Text(
                        text = "calories",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmakaColors.textSecondary
                    )
                }
            }
        }

        // Activity metrics card (steps and distance only when > 0)
        val hasSteps = (completion.steps ?: 0) > 0
        val hasDistance = (completion.distanceMeters ?: 0) > 0
        if (hasSteps || hasDistance) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AmakaColors.surface,
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AmakaSpacing.md.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (hasSteps) {
                            completion.steps?.let { steps ->
                                StatItem(
                                    icon = Icons.Filled.DirectionsWalk,
                                    value = if (steps >= 1000) String.format("%.1fk", steps / 1000.0) else "$steps",
                                    label = "Steps",
                                    iconTint = AmakaColors.accentGreen
                                )
                            }
                        }
                        if (hasDistance) {
                            completion.distanceMeters?.let { meters ->
                                val distanceStr = if (meters >= 1000) {
                                    String.format("%.2f km", meters / 1000.0)
                                } else {
                                    "$meters m"
                                }
                                StatItem(
                                    icon = Icons.Filled.Straighten,
                                    value = distanceStr,
                                    label = "Distance",
                                    iconTint = AmakaColors.accentBlue
                                )
                            }
                        }
                    }
                }
            }
        }

        // Heart rate details (if available)
        if (completion.hasHeartRateData) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AmakaColors.surface,
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                ) {
                    Column(modifier = Modifier.padding(AmakaSpacing.md.dp)) {
                        Text(
                            text = "Heart Rate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AmakaColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            completion.minHeartRate?.let {
                                MiniStatItem(value = "$it", label = "Min")
                            }
                            completion.avgHeartRate?.let {
                                MiniStatItem(value = "$it", label = "Avg")
                            }
                            completion.maxHeartRate?.let {
                                MiniStatItem(value = "$it", label = "Max")
                            }
                        }
                    }
                }
            }
        }

        // Heart rate graph (only when heart_rate_samples available)
        if (completion.hasHeartRateSamples) {
            item {
                HeartRateGraphCard(
                    heartRateSamples = completion.heartRateSamples!!,
                    avgHeartRate = completion.avgHeartRate
                )
            }
        }

        // AMA-292: Exercises section (from execution_log) - shows actual execution data
        if (completion.hasExecutionLog) {
            item {
                ExercisesSection(completion = completion)
            }
        }

        // Workout Breakdown section (hierarchical like web app - AMA-264)
        // Only show if no execution log (fallback to planned structure)
        val workoutSteps = completion.workoutStructure
        if (!workoutSteps.isNullOrEmpty() && !completion.hasExecutionLog) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AmakaColors.surface,
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                ) {
                    Column(modifier = Modifier.padding(AmakaSpacing.md.dp)) {
                        Text(
                            text = "Workout Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AmakaColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

                        // Display hierarchical workout structure (like web app)
                        workoutSteps.forEachIndexed { index, interval ->
                            WorkoutIntervalRow(
                                stepNumber = index + 1,
                                interval = interval
                            )
                            if (index < workoutSteps.size - 1) {
                                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                            }
                        }
                    }
                }
            }
        }

        // Details section (Source device)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AmakaColors.surface,
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
            ) {
                Column(modifier = Modifier.padding(AmakaSpacing.md.dp)) {
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AmakaColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

                    // Source device row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (completion.source) {
                                CompletionSource.PHONE -> Icons.Filled.Smartphone
                                else -> Icons.Filled.Watch
                            },
                            contentDescription = null,
                            tint = AmakaColors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))
                        Text(
                            text = "Source",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.textSecondary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = completion.deviceInfo?.displayName ?: completion.source.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.textPrimary
                        )
                    }

                    // Strava sync status row
                    if (completion.isSyncedToStrava) {
                        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = AmakaColors.accentGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))
                            Text(
                                text = "Strava",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmakaColors.textSecondary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Synced",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmakaColors.accentGreen
                            )
                        }
                    }
                }
            }
        }

        // Save to My Workouts button (when applicable - has workoutId and callback provided)
        if (completion.workoutId != null && onSaveToMyWorkouts != null) {
            item {
                OutlinedButton(
                    onClick = { onSaveToMyWorkouts() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AmakaColors.accentBlue
                    ),
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                    Text(
                        text = "Save to My Workouts",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Run Again button (always present)
        item {
            Button(
                onClick = { completion.workoutId?.let { onRunAgain?.invoke(it) } },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmakaColors.accentGreen
                ),
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                Text(
                    text = "Run Again",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Strava sync button (always show - as placeholder for future functionality)
        item {
            Button(
                onClick = { /* TODO: Implement Strava sync */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmakaColors.accentOrange
                ),
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
            ) {
                Icon(
                    imageVector = if (completion.isSyncedToStrava) Icons.Filled.Link else Icons.Filled.Upload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                Text(
                    text = if (completion.isSyncedToStrava) "View on Strava" else "Sync to Strava",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))
        }
    }
}

/**
 * Display a workout interval row with hierarchical support.
 * Handles repeat blocks with nested content, showing them like the web app.
 */
@Composable
private fun WorkoutIntervalRow(stepNumber: Int, interval: WorkoutIntervalSubmission) {
    when (interval.type.lowercase()) {
        "repeat" -> {
            // Display repeat block with nested content (like web app)
            RepeatBlockRow(stepNumber = stepNumber, repeat = interval)
        }
        else -> {
            // Display single interval
            SingleIntervalRow(stepNumber = stepNumber, interval = interval)
        }
    }
}

/**
 * Display a repeat block with its nested intervals (like web app).
 * Shows: "Repeat [n] - [sets] sets" with nested exercise and rest steps indented.
 */
@Composable
private fun RepeatBlockRow(stepNumber: Int, repeat: WorkoutIntervalSubmission) {
    val sets = repeat.reps ?: 1
    val setsText = if (sets == 1) "1 set" else "$sets sets"

    Column(modifier = Modifier.fillMaxWidth()) {
        // Repeat header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step number badge
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(AmakaColors.accentPurple),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$stepNumber",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))

            // Repeat label with sets count
            Text(
                text = "Repeat $stepNumber",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AmakaColors.textPrimary
            )

            Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))

            Text(
                text = "- $setsText",
                style = MaterialTheme.typography.bodyMedium,
                color = AmakaColors.textSecondary
            )
        }

        // Nested intervals (indented)
        if (!repeat.intervals.isNullOrEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp, top = AmakaSpacing.xs.dp)
            ) {
                repeat.intervals.forEach { nested ->
                    NestedIntervalRow(interval = nested)
                    Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))
                }
            }
        }
    }
}

/**
 * Display a nested interval within a repeat block (indented, shows icon).
 */
@Composable
private fun NestedIntervalRow(interval: WorkoutIntervalSubmission) {
    val info = getIntervalDisplayInfo(interval)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Material icon for the interval type
        Icon(
            imageVector = info.icon,
            contentDescription = null,
            tint = info.iconColor,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))

        // Name
        Text(
            text = info.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = AmakaColors.textPrimary
        )

        if (info.detail.isNotEmpty()) {
            Spacer(modifier = Modifier.width(AmakaSpacing.xs.dp))
            Text(
                text = info.detail,
                style = MaterialTheme.typography.bodySmall,
                color = AmakaColors.textSecondary
            )
        }
    }
}

/**
 * Display a single interval row (not inside a repeat block).
 */
@Composable
private fun SingleIntervalRow(stepNumber: Int, interval: WorkoutIntervalSubmission) {
    val info = getIntervalDisplayInfo(interval)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Material icon for the interval type
        Icon(
            imageVector = info.icon,
            contentDescription = null,
            tint = info.iconColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))

        // Name and detail
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = info.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AmakaColors.textPrimary
            )
            if (info.detail.isNotEmpty()) {
                Text(
                    text = info.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary
                )
            }
        }
    }
}

/**
 * Data class for interval display info including icon
 */
private data class IntervalDisplayInfo(
    val name: String,
    val detail: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconColor: Color
)

/**
 * Get display info for an interval type.
 * Returns IntervalDisplayInfo with Material icon based on type
 */
private fun getIntervalDisplayInfo(interval: WorkoutIntervalSubmission): IntervalDisplayInfo {
    return when (interval.type.lowercase()) {
        "warmup" -> {
            val name = interval.target?.takeIf { it.isNotEmpty() } ?: "Warm Up"
            IntervalDisplayInfo(
                name = name,
                detail = interval.seconds?.let { formatTime(it) } ?: "",
                icon = Icons.Filled.Whatshot,
                iconColor = AmakaColors.accentOrange
            )
        }
        "cooldown" -> {
            val name = interval.target?.takeIf { it.isNotEmpty() } ?: "Cool Down"
            IntervalDisplayInfo(
                name = name,
                detail = interval.seconds?.let { formatTime(it) } ?: "",
                icon = Icons.Filled.AcUnit,
                iconColor = AmakaColors.accentBlue
            )
        }
        "time" -> {
            val name = interval.target?.takeIf { it.isNotEmpty() } ?: "Timed Interval"
            IntervalDisplayInfo(
                name = name,
                detail = interval.seconds?.let { formatTime(it) } ?: "",
                icon = Icons.Filled.Timer,
                iconColor = AmakaColors.accentGreen
            )
        }
        "reps" -> {
            val name = interval.name ?: "Exercise"
            var detail = "${interval.reps ?: 0} reps"
            if (interval.sets != null && interval.sets > 1) {
                detail = "${interval.sets} × ${interval.reps ?: 0} reps"
            }
            IntervalDisplayInfo(
                name = name,
                detail = detail,
                icon = Icons.Filled.FitnessCenter,
                iconColor = AmakaColors.accentPurple
            )
        }
        "distance" -> {
            val name = interval.target?.takeIf { it.isNotEmpty() } ?: "Distance"
            val meters = interval.seconds ?: 0
            val distStr = if (meters >= 1000) {
                String.format("%.1f km", meters / 1000.0)
            } else {
                "${meters}m"
            }
            IntervalDisplayInfo(
                name = name,
                detail = distStr,
                icon = Icons.Filled.DirectionsRun,
                iconColor = AmakaColors.accentGreen
            )
        }
        "rest" -> {
            val detail = interval.seconds?.let { formatTime(it) } ?: "Tap when ready"
            IntervalDisplayInfo(
                name = "Rest",
                detail = detail,
                icon = Icons.Filled.HourglassEmpty,
                iconColor = AmakaColors.textTertiary
            )
        }
        else -> {
            IntervalDisplayInfo(
                name = interval.type.replaceFirstChar { it.uppercase() },
                detail = "",
                icon = Icons.Filled.Circle,
                iconColor = AmakaColors.textSecondary
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    return if (seconds >= 60) {
        val minutes = seconds / 60
        val secs = seconds % 60
        if (secs > 0) {
            "${minutes}m ${secs}s"
        } else {
            "$minutes min"
        }
    } else {
        "${seconds}s"
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    iconTint: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AmakaColors.textTertiary
        )
    }
}

@Composable
private fun MiniStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AmakaColors.textPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AmakaColors.textTertiary
        )
    }
}

private val CompletionSource.displayName: String
    get() = when (this) {
        CompletionSource.APPLE_WATCH -> "Apple Watch"
        CompletionSource.GARMIN -> "Garmin"
        CompletionSource.MANUAL -> "Manual"
        CompletionSource.PHONE -> "Phone"
        CompletionSource.WEAR_OS -> "Wear OS"
    }

// =============================================================================
// AMA-292: Exercises Section (from execution_log)
// =============================================================================

/**
 * Display exercises from execution_log with sets, weights, and status indicators.
 * Groups consecutive intervals by exercise name (iOS sends one interval per set).
 */
@Composable
private fun ExercisesSection(completion: WorkoutCompletionDetail) {
    val executionLog = completion.executionLog ?: return
    val intervals = executionLog.intervals

    // Group consecutive intervals by planned_name (like web UI does)
    data class GroupedExercise(
        val name: String,
        val status: IntervalStatus,
        val sets: List<Pair<SetLog, Int?>>  // Pair of set and interval duration
    )

    val groupedExercises = mutableListOf<GroupedExercise>()
    var currentGroup: GroupedExercise? = null

    intervals.filter { it.plannedKind == "reps" }.forEach { interval ->
        val name = interval.plannedName ?: "Unknown Exercise"
        val sets = interval.sets ?: emptyList()

        if (currentGroup != null && currentGroup!!.name == name) {
            // Add sets to current group with renumbered set_number
            val existingSets = currentGroup!!.sets.toMutableList()
            sets.forEach { set ->
                val nextSetNumber = existingSets.size + 1
                existingSets.add(Pair(set.copy(setNumber = nextSetNumber), interval.actualDurationSeconds))
            }
            currentGroup = currentGroup!!.copy(sets = existingSets)
        } else {
            // Start a new group
            if (currentGroup != null) {
                groupedExercises.add(currentGroup!!)
            }
            currentGroup = GroupedExercise(
                name = name,
                status = interval.status,
                sets = sets.mapIndexed { idx, set ->
                    Pair(set.copy(setNumber = idx + 1), interval.actualDurationSeconds)
                }
            )
        }
    }
    // Don't forget the last group
    if (currentGroup != null) {
        groupedExercises.add(currentGroup!!)
    }

    if (groupedExercises.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(modifier = Modifier.padding(AmakaSpacing.md.dp)) {
            Text(
                text = "Exercises",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

            // Table header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AmakaSpacing.sm.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Set",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmakaColors.textTertiary,
                    modifier = Modifier.width(40.dp)
                )
                Text(
                    text = "Reps",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmakaColors.textTertiary,
                    modifier = Modifier.width(60.dp)
                )
                Text(
                    text = "Time",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmakaColors.textTertiary,
                    modifier = Modifier.width(60.dp)
                )
                Text(
                    text = "Weight",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmakaColors.textTertiary,
                    modifier = Modifier.width(70.dp)
                )
                Spacer(modifier = Modifier.width(24.dp))  // Status icon space
            }

            HorizontalDivider(color = AmakaColors.borderLight)

            // Exercise groups
            groupedExercises.forEachIndexed { exerciseIdx, exercise ->
                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

                // Exercise name row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Exercise number badge
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(AmakaColors.accentPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${exerciseIdx + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = AmakaColors.textPrimary
                    )
                }

                // Sets for this exercise
                exercise.sets.forEach { (set, intervalDuration) ->
                    Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))
                    SetRow(set = set, intervalDuration = intervalDuration)
                }

                if (exerciseIdx < groupedExercises.size - 1) {
                    Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                    HorizontalDivider(color = AmakaColors.borderLight.copy(alpha = 0.5f))
                }
            }
        }
    }
}

/**
 * Display a single set row with reps, time, weight, and status.
 */
@Composable
private fun SetRow(set: SetLog, intervalDuration: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp),  // Indent under exercise name
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Set number
        Text(
            text = "${set.setNumber}",
            style = MaterialTheme.typography.bodySmall,
            color = AmakaColors.textSecondary,
            modifier = Modifier.width(40.dp)
        )

        // Reps (planned/completed)
        val repsText = when {
            set.repsPlanned != null && set.repsCompleted != null ->
                "${set.repsCompleted}/${set.repsPlanned}"
            set.repsCompleted != null -> "${set.repsCompleted}"
            set.repsPlanned != null -> "${set.repsPlanned}"
            else -> "----"
        }
        Text(
            text = repsText,
            style = MaterialTheme.typography.bodySmall,
            color = AmakaColors.textPrimary,
            modifier = Modifier.width(60.dp)
        )

        // Time - use set duration or fall back to interval duration
        val durationSeconds = set.durationSeconds ?: intervalDuration
        val timeText = durationSeconds?.let { formatSetTime(it) } ?: "----"
        Text(
            text = timeText,
            style = MaterialTheme.typography.bodySmall,
            color = AmakaColors.textSecondary,
            modifier = Modifier.width(60.dp)
        )

        // Weight
        val weightText = set.weight?.displayLabel ?: "----"
        Text(
            text = weightText,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = AmakaColors.textPrimary,
            modifier = Modifier.width(70.dp)
        )

        // Status icon
        SetStatusIcon(status = set.status, modifier = Modifier.size(20.dp))
    }
}

/**
 * Status icon for a set (completed, skipped, not reached)
 */
@Composable
private fun SetStatusIcon(status: IntervalStatus, modifier: Modifier = Modifier) {
    when (status) {
        IntervalStatus.COMPLETED -> {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Completed",
                tint = AmakaColors.accentGreen,
                modifier = modifier
            )
        }
        IntervalStatus.SKIPPED -> {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Skipped",
                tint = AmakaColors.accentOrange,
                modifier = modifier
            )
        }
        IntervalStatus.NOT_REACHED -> {
            Icon(
                imageVector = Icons.Filled.RadioButtonUnchecked,
                contentDescription = "Not reached",
                tint = AmakaColors.textTertiary,
                modifier = modifier
            )
        }
    }
}

/**
 * Format seconds to a readable time string for sets
 */
private fun formatSetTime(seconds: Int): String {
    return when {
        seconds >= 60 -> {
            val min = seconds / 60
            val sec = seconds % 60
            if (sec > 0) "${min}:${String.format("%02d", sec)}" else "${min}m"
        }
        else -> "${seconds}s"
    }
}

// =============================================================================
// Heart Rate Graph (AMA-277)
// =============================================================================

/**
 * Heart rate graph card with orange gradient fill and average dashed line.
 * Matches iOS implementation.
 */
@Composable
private fun HeartRateGraphCard(
    heartRateSamples: List<HeartRateDataPoint>,
    avgHeartRate: Int?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(modifier = Modifier.padding(AmakaSpacing.md.dp)) {
            Text(
                text = "Heart Rate",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

            // Heart rate graph
            HeartRateGraph(
                heartRateSamples = heartRateSamples,
                avgHeartRate = avgHeartRate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            // Time labels
            if (heartRateSamples.isNotEmpty()) {
                val firstSample = heartRateSamples.first()
                val lastSample = heartRateSamples.last()
                val firstTime = firstSample.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
                val lastTime = lastSample.timestamp.toLocalDateTime(TimeZone.currentSystemDefault())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTimeOfDay(firstTime.hour, firstTime.minute),
                        style = MaterialTheme.typography.labelSmall,
                        color = AmakaColors.textTertiary
                    )
                    Text(
                        text = formatTimeOfDay(lastTime.hour, lastTime.minute),
                        style = MaterialTheme.typography.labelSmall,
                        color = AmakaColors.textTertiary
                    )
                }
            }
        }
    }
}

/**
 * Heart rate graph using Canvas with orange gradient fill and average dashed line.
 */
@Composable
private fun HeartRateGraph(
    heartRateSamples: List<HeartRateDataPoint>,
    avgHeartRate: Int?,
    modifier: Modifier = Modifier
) {
    if (heartRateSamples.isEmpty()) return

    val orangeColor = AmakaColors.accentOrange
    val gradientStartColor = AmakaColors.accentOrange.copy(alpha = 0.6f)
    val gradientEndColor = AmakaColors.accentOrange.copy(alpha = 0.1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 8.dp.toPx()

        // Find min/max heart rate for scaling
        val bpmValues = heartRateSamples.map { it.bpm }
        val minBpm = (bpmValues.minOrNull() ?: 60) - 10
        val maxBpm = (bpmValues.maxOrNull() ?: 180) + 10
        val bpmRange = (maxBpm - minBpm).toFloat()

        // Calculate points
        val points = heartRateSamples.mapIndexed { index, sample ->
            val x = padding + (index.toFloat() / (heartRateSamples.size - 1)) * (width - 2 * padding)
            val normalizedBpm = (sample.bpm - minBpm) / bpmRange
            val y = height - padding - (normalizedBpm * (height - 2 * padding))
            Offset(x, y)
        }

        // Draw gradient fill
        if (points.size >= 2) {
            val fillPath = Path().apply {
                moveTo(points.first().x, height - padding)
                points.forEach { point ->
                    lineTo(point.x, point.y)
                }
                lineTo(points.last().x, height - padding)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(gradientStartColor, gradientEndColor),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw the line on top
            val linePath = Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) {
                        moveTo(point.x, point.y)
                    } else {
                        lineTo(point.x, point.y)
                    }
                }
            }

            drawPath(
                path = linePath,
                color = orangeColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Draw average dashed line
        avgHeartRate?.let { avg ->
            val normalizedAvg = (avg - minBpm) / bpmRange
            val avgY = height - padding - (normalizedAvg * (height - 2 * padding))

            // Draw dashed line
            val dashWidth = 10.dp.toPx()
            val dashGap = 5.dp.toPx()
            var currentX = padding

            while (currentX < width - padding) {
                drawLine(
                    color = orangeColor.copy(alpha = 0.7f),
                    start = Offset(currentX, avgY),
                    end = Offset(minOf(currentX + dashWidth, width - padding), avgY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                currentX += dashWidth + dashGap
            }
        }
    }
}

/**
 * Format hour and minute to time string (e.g., "9:30 AM")
 */
private fun formatTimeOfDay(hour: Int, minute: Int): String {
    val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val amPm = if (hour >= 12) "PM" else "AM"
    return String.format("%d:%02d %s", hour12, minute, amPm)
}
