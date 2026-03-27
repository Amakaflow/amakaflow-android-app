package com.amakaflow.companion.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.Workout
import com.amakaflow.companion.data.model.WorkoutSport
import com.amakaflow.companion.ui.components.RestDayButton
import com.amakaflow.companion.ui.components.WeeklyProgressRing
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing
import com.amakaflow.companion.ui.screens.suggest.SuggestWorkoutButton
import com.amakaflow.companion.ui.screens.xp.XPBarComponent
import com.amakaflow.companion.ui.screens.nutrition.NutritionDashboardCard
import com.amakaflow.companion.ui.screens.nutrition.NutritionViewModel
import com.amakaflow.companion.ui.screens.nutrition.ProteinTrackerCard
import com.amakaflow.companion.ui.screens.nutrition.WaterTrackerCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSuggestWorkout: () -> Unit = {},
    onNavigateToWorkouts: () -> Unit,
    onNavigateToWorkoutDetail: (String) -> Unit,
    onNavigateToVoiceWorkout: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    nutritionViewModel: NutritionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val nutritionState by nutritionViewModel.uiState.collectAsState()
    val today = LocalDate.now()
    var showQuickStartSheet by remember { mutableStateOf(false) }

    // Simulation mode state (AMA-271)
    val isSimulationEnabled by viewModel.simulationSettings.isEnabled.collectAsState(initial = false)
    val simulationSpeed by viewModel.simulationSettings.speed.collectAsState(initial = 10.0)

    // Quick Start Bottom Sheet
    if (showQuickStartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickStartSheet = false },
            containerColor = AmakaColors.surface
        ) {
            QuickStartContent(
                workouts = uiState.todayWorkouts,
                onWorkoutSelect = { workout ->
                    showQuickStartSheet = false
                    onNavigateToWorkoutDetail(workout.id)
                },
                onCancel = { showQuickStartSheet = false }
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .padding(horizontal = AmakaSpacing.md.dp)
            .testTag("home_screen"),
        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.lg.dp)
    ) {
        // Date header (iOS style)
        item {
            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            DateHeader(date = today)
        }

        // Simulation mode indicator (AMA-271) - like iOS
        if (isSimulationEnabled) {
            item {
                SimulationModeBanner(
                    speed = simulationSpeed
                )
            }
        }

        // Weekly Progress Ring (AMA-1286)
        item {
            WeeklyProgressRing(
                workoutsCompleted = uiState.weeklyStats.workoutCount,
                weeklyTarget = maxOf(1, uiState.weeklyStats.workoutCount + 1), // TODO: fetch from /gamification/weekly-progress
                ringPercentage = uiState.weeklyStats.workoutCount.toFloat() / maxOf(1, uiState.weeklyStats.workoutCount + 1).toFloat(),
                motivationalText = weeklyMotivationalText(uiState.weeklyStats.workoutCount, maxOf(1, uiState.weeklyStats.workoutCount + 1))
            )
        }

        // Suggest Workout button (AMA-1265)
        item {
            SuggestWorkoutButton(onClick = onNavigateToSuggestWorkout)
        }

        // XP progress bar (AMA-1285)
        item {
            XPBarComponent(
                xpTotal = uiState.xpTotal,
                currentLevel = uiState.currentLevel,
                levelName = uiState.levelName,
                xpToNextLevel = uiState.xpToNextLevel,
                xpToday = uiState.xpToday,
                dailyCap = uiState.dailyCap
            )
        }

        // AMA-1290-1292: Nutrition cards (shown when enabled)
        if (nutritionState.isEnabled) {
            item {
                NutritionDashboardCard(
                    nutrition = nutritionState.nutrition,
                    nutritionLabel = nutritionState.nutritionLabel,
                    displayMode = nutritionState.displayMode,
                    source = nutritionState.nutrition.source
                )
            }

            item {
                ProteinTrackerCard(
                    currentGrams = nutritionState.nutrition.proteinGrams,
                    targetGrams = nutritionState.proteinTargetGrams,
                    onAddProtein = { grams -> nutritionViewModel.addProtein(grams) }
                )
            }

            item {
                WaterTrackerCard(
                    currentMl = nutritionState.nutrition.waterMl,
                    targetMl = nutritionState.waterTargetMl,
                    onAddWater = { nutritionViewModel.addWater() }
                )
            }
        }

        // Quick action buttons (iOS style)
        item {
            QuickActionButtons(
                onQuickStart = { showQuickStartSheet = true },
                onLogWorkout = onNavigateToVoiceWorkout
            )
        }

        // Rest Day Button (AMA-1286)
        item {
            RestDayButton(
                onRestDayLogged = {
                    // TODO: call POST /gamification/rest-day
                }
            )
        }

        // Today's Workouts section with count badge
        item {
            TodaysWorkoutsHeader(workoutCount = uiState.todayWorkouts.size)
        }

        // Workouts or empty state
        item {
            if (uiState.todayWorkouts.isEmpty()) {
                EmptyWorkoutsCard(
                    onLogWithVoice = onNavigateToVoiceWorkout
                )
            } else {
                TodayWorkoutsCard(
                    workouts = uiState.todayWorkouts,
                    onWorkoutClick = onNavigateToWorkoutDetail
                )
            }
        }

        // This Week stats card
        item {
            WeeklyStatsCard(
                workoutCount = uiState.weeklyStats.workoutCount,
                totalDuration = uiState.weeklyStats.formattedDuration,
                totalCalories = uiState.weeklyStats.formattedCalories
            )
        }

        item {
            Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))
        }
    }
}

private fun weeklyMotivationalText(completed: Int, target: Int): String {
    val remaining = maxOf(0, target - completed)
    return when {
        completed >= target -> "Target hit! $completed of $target \u2014 crushing it!"
        remaining == 1 -> "$completed of $target \u2014 one more to go!"
        completed == 0 -> "0 of $target \u2014 let\u2019s get started this week!"
        else -> "$completed of $target \u2014 $remaining more to go!"
    }
}

@Composable
private fun QuickStartContent(
    workouts: List<Workout>,
    onWorkoutSelect: (Workout) -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AmakaSpacing.md.dp)
            .padding(bottom = AmakaSpacing.xl.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Start",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
            TextButton(onClick = onCancel) {
                Text(
                    text = "Cancel",
                    color = AmakaColors.accentBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

        Text(
            text = "Select a workout to start",
            style = MaterialTheme.typography.bodyMedium,
            color = AmakaColors.textSecondary
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))

        if (workouts.isEmpty()) {
            Text(
                text = "No workouts available. Push a workout from the web app first.",
                style = MaterialTheme.typography.bodyMedium,
                color = AmakaColors.textTertiary,
                modifier = Modifier.padding(vertical = AmakaSpacing.lg.dp)
            )
        } else {
            workouts.forEach { workout ->
                QuickStartWorkoutItem(
                    workout = workout,
                    onClick = { onWorkoutSelect(workout) }
                )
                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
            }
        }
    }
}

@Composable
private fun QuickStartWorkoutItem(
    workout: Workout,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AmakaCornerRadius.md.dp))
            .clickable(onClick = onClick),
        color = AmakaColors.background,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sport icon with colored background
            Surface(
                modifier = Modifier.size(48.dp),
                color = workout.sport.color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        tint = workout.sport.color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = AmakaColors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = AmakaColors.textTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${workout.formattedDuration} \u2022 ${workout.sport.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textTertiary
                    )
                }
                Text(
                    text = "${workout.intervalCount} steps",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textTertiary
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = AmakaColors.textTertiary
            )
        }
    }
}

@Composable
private fun DateHeader(date: LocalDate) {
    Column {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())),
            style = MaterialTheme.typography.bodyLarge,
            color = AmakaColors.textSecondary
        )
        Text(
            text = date.format(DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary
        )
    }
}

@Composable
private fun QuickActionButtons(
    onQuickStart: () -> Unit,
    onLogWorkout: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
    ) {
        // Quick Start button (blue/teal gradient style)
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(80.dp)
                .clip(RoundedCornerShape(AmakaCornerRadius.md.dp))
                .clickable(onClick = onQuickStart),
            color = AmakaColors.accentBlue,
            shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AmakaSpacing.md.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = AmakaColors.textPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Quick Start",
                    style = MaterialTheme.typography.titleSmall,
                    color = AmakaColors.textPrimary
                )
            }
        }

        // Log Workout button (green solid)
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(80.dp)
                .clip(RoundedCornerShape(AmakaCornerRadius.md.dp))
                .clickable(onClick = onLogWorkout)
                .testTag("log_workout_button"),
            color = AmakaColors.accentGreen,
            shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AmakaSpacing.md.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = AmakaColors.background,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Log Workout",
                    style = MaterialTheme.typography.titleSmall,
                    color = AmakaColors.background
                )
            }
        }
    }
}

@Composable
private fun TodaysWorkoutsHeader(workoutCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Today's Workouts",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = AmakaColors.textPrimary
        )
        Surface(
            color = AmakaColors.surface,
            shape = RoundedCornerShape(AmakaCornerRadius.lg.dp)
        ) {
            Text(
                text = "$workoutCount workout${if (workoutCount != 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = AmakaColors.textSecondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun TodayWorkoutsCard(
    workouts: List<Workout>,
    onWorkoutClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(
            modifier = Modifier.padding(AmakaSpacing.sm.dp)
        ) {
            workouts.forEachIndexed { index, workout ->
                TodayWorkoutRow(
                    workout = workout,
                    onClick = { onWorkoutClick(workout.id) }
                )
                if (index < workouts.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = AmakaSpacing.sm.dp),
                        color = AmakaColors.borderLight
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayWorkoutRow(
    workout: Workout,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AmakaCornerRadius.sm.dp))
            .clickable(onClick = onClick)
            .padding(AmakaSpacing.sm.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time column
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.width(48.dp)
        ) {
            Text(
                text = "9:00",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AmakaColors.textPrimary
            )
            Text(
                text = workout.formattedDuration,
                style = MaterialTheme.typography.bodySmall,
                color = AmakaColors.textTertiary
            )
        }

        Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))

        // Blue dot indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(AmakaColors.accentBlue)
        )

        Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))

        // Workout name
        Text(
            text = workout.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = AmakaColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))

        // Sport badge
        Surface(
            color = AmakaColors.background,
            shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
        ) {
            Text(
                text = workout.sport.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = AmakaColors.textSecondary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun EmptyWorkoutsCard(
    onLogWithVoice: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.lg.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = AmakaColors.textTertiary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            Text(
                text = "No workouts scheduled",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))
            Text(
                text = "Add a workout from the web, or log one you've completed",
                style = MaterialTheme.typography.bodyMedium,
                color = AmakaColors.textSecondary
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            TextButton(onClick = onLogWithVoice) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = AmakaColors.accentRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(AmakaSpacing.xs.dp))
                Text(
                    text = "Log with Voice",
                    color = AmakaColors.accentRed
                )
            }
        }
    }
}

@Composable
private fun WeeklyStatsCard(
    workoutCount: Int,
    totalDuration: String,
    totalCalories: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(
            modifier = Modifier.padding(AmakaSpacing.md.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.TrendingUp,
                    contentDescription = null,
                    tint = AmakaColors.accentBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                Text(
                    text = "This Week",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(value = workoutCount.toString(), label = "Workouts")
                StatItem(value = totalDuration, label = "Time")
                StatItem(value = totalCalories, label = "Calories")
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = AmakaColors.accentBlue
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AmakaColors.textTertiary
        )
    }
}

// Extension to get sport color
private val WorkoutSport.color: Color
    get() = when (this) {
        WorkoutSport.RUNNING -> AmakaColors.sportRunning
        WorkoutSport.CYCLING -> AmakaColors.sportCycling
        WorkoutSport.STRENGTH -> AmakaColors.sportStrength
        WorkoutSport.MOBILITY -> AmakaColors.sportMobility
        WorkoutSport.SWIMMING -> AmakaColors.sportSwimming
        WorkoutSport.CARDIO -> AmakaColors.sportCardio
        WorkoutSport.OTHER -> AmakaColors.textSecondary
    }

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

/**
 * AMA-271: Simulation mode banner for home screen.
 * Yellow banner indicating simulation mode is active with speed multiplier.
 */
@Composable
private fun SimulationModeBanner(
    speed: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFD600), // Bright yellow (same as iOS)
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.sm.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
            Text(
                text = "Simulation Mode: ${speed.toInt()}x",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Settings \u2192 Title \u00D7 7",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black.copy(alpha = 0.6f)
            )
        }
    }
}
