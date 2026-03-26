package com.amakaflow.companion.ui.screens.suggest

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.Workout
import com.amakaflow.companion.data.model.WorkoutInterval
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestWorkoutScreen(
    onNavigateBack: () -> Unit,
    onAcceptWorkout: (Workout) -> Unit = {},
    viewModel: SuggestWorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suggested Workout") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmakaColors.background,
                    titleContentColor = AmakaColors.textPrimary
                )
            )
        },
        containerColor = AmakaColors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.state) {
                SuggestWorkoutState.IDLE -> {
                    // Auto-trigger suggestion on first load
                    LaunchedEffect(Unit) {
                        viewModel.requestSuggestion()
                    }
                }
                SuggestWorkoutState.NEEDS_ONBOARDING -> {
                    CoachingProfileOnboardingContent(viewModel = viewModel)
                }
                SuggestWorkoutState.LOADING -> {
                    LoadingContent()
                }
                SuggestWorkoutState.SUCCESS -> {
                    uiState.suggestedWorkout?.let { workout ->
                        WorkoutPreviewContent(
                            workout = workout,
                            onAccept = {
                                onAcceptWorkout(workout)
                                onNavigateBack()
                            },
                            onSuggestAnother = { viewModel.suggestWorkout() },
                            onDismiss = {
                                viewModel.reset()
                                onNavigateBack()
                            }
                        )
                    }
                }
                SuggestWorkoutState.ERROR -> {
                    ErrorContent(
                        message = uiState.errorMessage ?: "Something went wrong",
                        onRetry = { viewModel.suggestWorkout() }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("suggest_workout_loading"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = AmakaColors.accentOrange,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
        Text(
            text = "Generating your workout...",
            style = MaterialTheme.typography.bodyLarge,
            color = AmakaColors.textSecondary
        )
        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
        Text(
            text = "Our AI coach is crafting a workout based on your profile",
            style = MaterialTheme.typography.bodyMedium,
            color = AmakaColors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AmakaSpacing.xl.dp)
        )
    }
}

@Composable
private fun WorkoutPreviewContent(
    workout: Workout,
    onAccept: () -> Unit,
    onSuggestAnother: () -> Unit,
    onDismiss: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AmakaSpacing.md.dp)
            .testTag("suggest_workout_preview"),
        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
    ) {
        // Header card
        item {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = AmakaColors.accentOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(AmakaSpacing.xs.dp))
                        Text(
                            text = "AI Coach Suggestion",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmakaColors.accentOrange
                        )
                    }
                    Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AmakaColors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, null, tint = AmakaColors.textTertiary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(workout.formattedDuration, style = MaterialTheme.typography.bodySmall, color = AmakaColors.textTertiary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FitnessCenter, null, tint = AmakaColors.textTertiary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${workout.intervalCount} steps", style = MaterialTheme.typography.bodySmall, color = AmakaColors.textTertiary)
                        }
                    }
                    workout.description?.let { desc ->
                        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Workout steps
        item {
            Text(
                text = "Workout Steps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
        }

        itemsIndexed(workout.intervals) { index, interval ->
            IntervalRowItem(index = index + 1, interval = interval)
        }

        // Action buttons
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
            ) {
                // Accept button
                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("accept_workout_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AmakaColors.accentGreen),
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, null)
                    Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                    Text("Accept & Save", fontWeight = FontWeight.SemiBold)
                }

                // Suggest another
                OutlinedButton(
                    onClick = onSuggestAnother,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("suggest_another_button"),
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmakaColors.accentBlue)
                ) {
                    Icon(Icons.Filled.Refresh, null)
                    Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                    Text("Suggest Another")
                }

                // Dismiss
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dismiss", color = AmakaColors.textSecondary)
                }

                Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
            }
        }
    }
}

@Composable
private fun IntervalRowItem(index: Int, interval: WorkoutInterval) {
    val (name, detail, color) = getIntervalInfo(interval)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AmakaColors.textPrimary
            )
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textTertiary
                )
            }
        }
    }
}

private data class IntervalInfo(val name: String, val detail: String?, val color: Color)

private fun getIntervalInfo(interval: WorkoutInterval): IntervalInfo {
    return when (interval) {
        is WorkoutInterval.Warmup -> IntervalInfo("Warm Up", "${interval.seconds / 60} min", Color(0xFFFF9800))
        is WorkoutInterval.Cooldown -> IntervalInfo("Cool Down", "${interval.seconds / 60} min", AmakaColors.accentBlue)
        is WorkoutInterval.Time -> IntervalInfo(interval.target ?: "Timed Interval", "${interval.seconds / 60} min", AmakaColors.accentBlue)
        is WorkoutInterval.Reps -> {
            val parts = mutableListOf<String>()
            interval.sets?.let { parts.add("$it sets x") }
            parts.add("${interval.reps} reps")
            interval.load?.let { parts.add("@ $it") }
            interval.restSec?.let { parts.add("(${it}s rest)") }
            IntervalInfo(interval.name, parts.joinToString(" "), AmakaColors.accentGreen)
        }
        is WorkoutInterval.Distance -> IntervalInfo("${interval.meters}m", interval.target, AmakaColors.accentBlue)
        is WorkoutInterval.Repeat -> IntervalInfo("Repeat x${interval.reps}", "${interval.intervals.size} exercises", AmakaColors.accentBlue)
        is WorkoutInterval.Rest -> IntervalInfo("Rest", interval.seconds?.let { "${it}s" } ?: "Until ready", Color.Gray)
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AmakaSpacing.lg.dp)
            .testTag("suggest_workout_error"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = AmakaColors.accentOrange,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AmakaColors.textPrimary
        )
        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AmakaColors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AmakaColors.accentOrange),
            shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
        ) {
            Icon(Icons.Filled.Refresh, null)
            Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
            Text("Try Again")
        }
    }
}
