package com.amakaflow.companion.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.FlattenedInterval
import com.amakaflow.companion.data.model.StepType
import com.amakaflow.companion.data.model.WorkoutPhase
import com.amakaflow.companion.ui.components.SimulationBanner
import com.amakaflow.companion.ui.components.WeightInputView
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing
import com.amakaflow.companion.ui.screens.rpefeedback.RPEFeedbackScreen
import com.amakaflow.companion.ui.screens.rpefeedback.RPEFeedbackViewModel
import com.amakaflow.companion.ui.screens.rpefeedback.RPEFeedbackUiState

@Composable
fun WorkoutPlayerScreen(
    workoutId: String,
    onDismiss: () -> Unit,
    onRunAgain: (String) -> Unit = {},
    viewModel: WorkoutPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AmakaColors.accentBlue)
                }
            }
            // Show completion screen when workout ends, then RPE feedback (AMA-1266)
            uiState.phase == WorkoutPhase.ENDED && uiState.workoutCompleted -> {
                var showRPEFeedback by remember { mutableStateOf(false) }

                if (showRPEFeedback) {
                    val rpeFeedbackViewModel: RPEFeedbackViewModel = hiltViewModel()
                    val rpeFeedbackState by rpeFeedbackViewModel.uiState.collectAsState()

                    RPEFeedbackScreen(
                        uiState = rpeFeedbackState,
                        onSelectOption = rpeFeedbackViewModel::selectOption,
                        onToggleMuscle = rpeFeedbackViewModel::toggleMuscle,
                        onSubmit = { rpeFeedbackViewModel.submit(workoutId = workoutId, onComplete = onDismiss) },
                        onSkip = onDismiss
                    )
                } else {
                    WorkoutCompletionScreen(
                        workoutName = uiState.workout?.name ?: "Workout",
                        durationSeconds = uiState.elapsedSeconds,
                        onDone = { showRPEFeedback = true },
                        onRunAgain = {
                            uiState.workout?.id?.let { id ->
                                onRunAgain(id)
                            } ?: onDismiss()
                        }
                    )
                }
            }
            // Dismiss immediately if discarded
            uiState.phase == WorkoutPhase.ENDED && !uiState.workoutCompleted -> {
                LaunchedEffect(Unit) {
                    onDismiss()
                }
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AmakaSpacing.lg.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error loading workout",
                        style = MaterialTheme.typography.titleMedium,
                        color = AmakaColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                    Text(
                        text = uiState.error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmakaColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
                    Button(onClick = onDismiss) {
                        Text("Go Back")
                    }
                }
            }
            else -> {
                // Main player content
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    PlayerHeader(
                        workoutName = uiState.workout?.name ?: "",
                        elapsedTime = uiState.formattedElapsedTime,
                        progress = uiState.progress,
                        onClose = { viewModel.showEndConfirmation() }
                    )

                    // Simulation mode banner
                    if (uiState.isSimulationMode) {
                        SimulationBanner(
                            speed = uiState.simulationSpeed,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.xs.dp)
                        )
                    }

                    // Content based on phase
                    // AMA-291: Use key() to force complete recomposition when step changes
                    // This ensures UI updates properly during rapid simulation mode transitions
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (uiState.isResting) {
                            key(uiState.currentStepIndex, uiState.isManualRest) {
                                RestPeriodView(
                                    isManualRest = uiState.isManualRest,
                                    remainingTime = uiState.formattedRestTime,
                                    onSkip = { viewModel.skipRest() }
                                )
                            }
                        } else {
                            key(uiState.currentStepIndex, uiState.setNumber) {
                                CurrentStepView(
                                    step = uiState.currentStep,
                                    remainingTime = uiState.formattedRemainingTime,
                                    stepIndex = uiState.currentStepIndex + 1,
                                    totalSteps = uiState.flattenedSteps.size,
                                    nextStep = uiState.nextStep,
                                    // AMA-287: Weight input props
                                    setNumber = uiState.setNumber,
                                    totalSetsForExercise = uiState.totalSetsForExercise,
                                    suggestedWeight = uiState.suggestedWeight,
                                    weightUnit = uiState.weightUnit,
                                    onLogSet = { weight, unit -> viewModel.logSetWeight(weight, unit) },
                                    onSkipWeight = { viewModel.skipSetWeight() }
                                )
                            }
                        }
                    }

                    // Player controls
                    PlayerControls(
                        isPlaying = uiState.isPlaying,
                        isPaused = uiState.isPaused,
                        isResting = uiState.isResting,
                        canGoBack = uiState.canGoBack,
                        canGoForward = uiState.canGoForward,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onPrevious = { viewModel.previousStep() },
                        onNext = { viewModel.nextStep() },
                        onEnd = { viewModel.showEndConfirmation() }
                    )
                }
            }
        }

        // End confirmation dialog
        if (uiState.showEndConfirmation) {
            EndWorkoutDialog(
                onDismiss = { viewModel.hideEndConfirmation() },
                onEndAndSave = { viewModel.endAndSave() },
                onEndAndDiscard = { viewModel.endAndDiscard() }
            )
        }
    }
}

@Composable
private fun PlayerHeader(
    workoutName: String,
    elapsedTime: String,
    progress: Float,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AmakaSpacing.md.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "End workout",
                    tint = AmakaColors.textPrimary
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = workoutName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
                Text(
                    text = elapsedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary
                )
            }

            // Placeholder for symmetry
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = AmakaColors.accentGreen,
            trackColor = AmakaColors.surface
        )
    }
}

@Composable
private fun CurrentStepView(
    step: FlattenedInterval?,
    remainingTime: String,
    stepIndex: Int,
    totalSteps: Int,
    nextStep: FlattenedInterval?,
    // AMA-287: Weight input parameters
    setNumber: Int = 1,
    totalSetsForExercise: Int = 1,
    suggestedWeight: Double? = null,
    weightUnit: String = "lbs",
    onLogSet: (Double?, String) -> Unit = { _, _ -> },
    onSkipWeight: () -> Unit = {}
) {
    if (step == null) return

    // AMA-322: Different layouts for different step types
    // REPS steps need special handling to ensure action buttons are always visible
    if (step.stepType == StepType.REPS) {
        // For REPS: Use Column layout that ensures buttons at bottom are visible
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AmakaSpacing.lg.dp)
                .padding(top = AmakaSpacing.md.dp, bottom = AmakaSpacing.sm.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step counter
            Text(
                text = "Step $stepIndex of $totalSteps",
                style = MaterialTheme.typography.labelMedium,
                color = AmakaColors.textTertiary
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

            // Round info badge
            step.roundInfo?.let { roundInfo ->
                Surface(
                    color = AmakaColors.accentBlue.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
                ) {
                    Text(
                        text = roundInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = AmakaColors.accentBlue,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
            }

            // Reps display (compact)
            Text(
                text = "${step.targetReps ?: 0}",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = AmakaColors.accentBlue
            )
            Text(
                text = "reps",
                style = MaterialTheme.typography.titleMedium,
                color = AmakaColors.textSecondary
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

            // Weight input view - takes remaining space
            WeightInputView(
                exerciseName = step.stepName,
                setNumber = setNumber,
                totalSets = totalSetsForExercise,
                suggestedWeight = suggestedWeight,
                weightUnit = weightUnit,
                onLogSet = onLogSet,
                onSkipWeight = onSkipWeight,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        // For TIMED/DISTANCE: Use centered Box layout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(AmakaSpacing.lg.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step counter
                Text(
                    text = "Step $stepIndex of $totalSteps",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmakaColors.textTertiary
                )

                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

                // Round info badge
                step.roundInfo?.let { roundInfo ->
                    Surface(
                        color = AmakaColors.accentBlue.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
                    ) {
                        Text(
                            text = roundInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = AmakaColors.accentBlue,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
                }

                when (step.stepType) {
                    StepType.TIMED -> {
                        Text(
                            text = step.stepName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = AmakaColors.textPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

                        Text(
                            text = remainingTime,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 72.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = AmakaColors.accentGreen
                        )
                    }
                    StepType.DISTANCE -> {
                        Text(
                            text = step.stepName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = AmakaColors.textPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

                        Text(
                            text = step.stepName,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = AmakaColors.accentOrange
                        )
                    }
                    else -> { /* REPS handled above */ }
                }

                // Coming up next
                nextStep?.let { next ->
                    Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))
                    ComingUpView(nextStep = next)
                }
            }
        }
    }
}

@Composable
private fun ComingUpView(nextStep: FlattenedInterval) {
    val timeText = nextStep.durationSeconds?.let { seconds ->
        val minutes = seconds / 60
        val secs = seconds % 60
        String.format("%d:%02d", minutes, secs)
    } ?: nextStep.targetReps?.let { "$it reps" } ?: ""

    Surface(
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AmakaSpacing.md.dp)
        ) {
            Text(
                text = "COMING UP",
                style = MaterialTheme.typography.labelSmall,
                color = AmakaColors.textTertiary
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nextStep.stepName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmakaColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun RestPeriodView(
    isManualRest: Boolean,
    remainingTime: String,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.surface)
            .padding(AmakaSpacing.lg.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Pause,
            contentDescription = null,
            tint = AmakaColors.accentBlue,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

        Text(
            text = "Rest",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))

        if (isManualRest) {
            Text(
                text = "Tap when ready",
                style = MaterialTheme.typography.titleLarge,
                color = AmakaColors.textSecondary
            )
        } else {
            Text(
                text = remainingTime,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = AmakaColors.accentBlue
            )
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))

        Button(
            onClick = onSkip,
            colors = ButtonDefaults.buttonColors(
                containerColor = AmakaColors.accentGreen
            ),
            shape = RoundedCornerShape(AmakaCornerRadius.lg.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (isManualRest) "Continue" else "Skip Rest",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    isPaused: Boolean,
    isResting: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onEnd: () -> Unit
) {
    Surface(
        color = AmakaColors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.lg.dp, vertical = AmakaSpacing.md.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            IconButton(
                onClick = onPrevious,
                enabled = canGoBack && !isResting
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = if (canGoBack && !isResting) AmakaColors.textPrimary else AmakaColors.textTertiary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Play/Pause button
            Surface(
                onClick = onPlayPause,
                color = AmakaColors.accentGreen,
                shape = CircleShape,
                modifier = Modifier.size(64.dp),
                enabled = !isResting
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Next button
            IconButton(
                onClick = onNext,
                enabled = !isResting
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = if (!isResting) AmakaColors.textPrimary else AmakaColors.textTertiary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun EndWorkoutDialog(
    onDismiss: () -> Unit,
    onEndAndSave: () -> Unit,
    onEndAndDiscard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "End Workout?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("What would you like to do with your progress?")
        },
        confirmButton = {
            TextButton(onClick = onEndAndSave) {
                Text(
                    text = "Save Progress",
                    color = AmakaColors.accentGreen
                )
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onEndAndDiscard) {
                    Text(
                        text = "Discard",
                        color = AmakaColors.accentRed
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Continue")
                }
            }
        },
        containerColor = AmakaColors.surface
    )
}
