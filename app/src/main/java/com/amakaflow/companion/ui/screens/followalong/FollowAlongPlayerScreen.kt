package com.amakaflow.companion.ui.screens.followalong

import android.net.Uri
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

/**
 * AMA-1182: Follow-along video playback screen.
 * AndroidView(VideoView) video playback with step overlay,
 * play/pause, skip controls, auto-advance, and step list.
 */
@Composable
fun FollowAlongPlayerScreen(
    workoutId: String,
    onDismiss: () -> Unit,
    viewModel: FollowAlongPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEndConfirmation by remember { mutableStateOf(false) }
    var showStepList by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("follow_along_player_screen")
    ) {
        when {
            uiState.phase == FollowAlongPhase.LOADING -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AmakaColors.accentBlue)
                }
            }
            uiState.phase == FollowAlongPhase.ENDED -> {
                EndedView(
                    formattedElapsed = uiState.formattedElapsed,
                    errorMessage = uiState.errorMessage,
                    onDone = onDismiss
                )
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    HeaderBar(
                        workoutName = uiState.workout?.name ?: "",
                        formattedElapsed = uiState.formattedElapsed,
                        onClose = {
                            if (uiState.phase == FollowAlongPhase.PLAYING) {
                                viewModel.pause()
                            }
                            showEndConfirmation = true
                        },
                        onShowStepList = { showStepList = true }
                    )

                    // Video player area
                    VideoPlayerArea(
                        videoUrl = uiState.videoUrl,
                        currentStepName = uiState.currentStep?.name ?: "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.4f)
                    )

                    // Step overlay
                    CurrentStepOverlay(
                        step = uiState.currentStep,
                        stepIndex = uiState.currentStepIndex,
                        totalSteps = uiState.steps.size,
                        remainingSeconds = uiState.stepRemainingSeconds,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.sm.dp)
                    )

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { uiState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(horizontal = AmakaSpacing.md.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = AmakaColors.accentGreen,
                        trackColor = AmakaColors.surface
                    )

                    Spacer(modifier = Modifier.weight(0.1f))

                    // Step chips (horizontal scroll)
                    StepChips(
                        steps = uiState.steps,
                        currentIndex = uiState.currentStepIndex,
                        onStepClick = { viewModel.skipToStep(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AmakaSpacing.sm.dp)
                    )

                    Spacer(modifier = Modifier.weight(0.1f))

                    // Player controls
                    PlayerControls(
                        isPlaying = uiState.phase == FollowAlongPhase.PLAYING,
                        canGoBack = uiState.canGoBack,
                        canGoForward = uiState.canGoForward,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onPrevious = { viewModel.skipToPreviousStep() },
                        onNext = { viewModel.skipToNextStep() }
                    )
                }
            }
        }

        // End confirmation dialog
        if (showEndConfirmation) {
            AlertDialog(
                onDismissRequest = { showEndConfirmation = false },
                title = { Text("End Follow-Along?", fontWeight = FontWeight.Bold) },
                text = { Text("Your progress will not be saved.") },
                confirmButton = {
                    TextButton(onClick = {
                        showEndConfirmation = false
                        viewModel.endWorkout()
                    }) {
                        Text("End & Close", color = AmakaColors.accentRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showEndConfirmation = false
                        viewModel.play()
                    }) {
                        Text("Cancel")
                    }
                },
                containerColor = AmakaColors.surface
            )
        }

        // Step list bottom sheet
        if (showStepList) {
            StepListSheet(
                steps = uiState.steps,
                currentStepIndex = uiState.currentStepIndex,
                onStepSelected = {
                    viewModel.skipToStep(it)
                    showStepList = false
                },
                onDismiss = { showStepList = false }
            )
        }
    }
}

// MARK: - Header

@Composable
private fun HeaderBar(
    workoutName: String,
    formattedElapsed: String,
    onClose: () -> Unit,
    onShowStepList: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AmakaSpacing.md.dp),
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
                color = AmakaColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formattedElapsed,
                style = MaterialTheme.typography.bodySmall,
                color = AmakaColors.textSecondary
            )
        }

        IconButton(onClick = onShowStepList) {
            Icon(
                imageVector = Icons.Filled.List,
                contentDescription = "Step list",
                tint = AmakaColors.textPrimary
            )
        }
    }
}

// MARK: - Video Player

@Composable
private fun VideoPlayerArea(
    videoUrl: String?,
    currentStepName: String,
    modifier: Modifier = Modifier
) {
    if (videoUrl != null) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setVideoURI(Uri.parse(videoUrl))
                    val controller = MediaController(context)
                    controller.setAnchorView(this)
                    setMediaController(controller)
                }
            },
            modifier = modifier
                .padding(horizontal = AmakaSpacing.md.dp)
                .clip(RoundedCornerShape(AmakaCornerRadius.md.dp))
        )
    } else {
        // Placeholder when no video
        Box(
            modifier = modifier
                .padding(horizontal = AmakaSpacing.md.dp)
                .clip(RoundedCornerShape(AmakaCornerRadius.md.dp))
                .background(AmakaColors.surface),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = AmakaColors.textTertiary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                Text(
                    text = currentStepName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AmakaColors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = AmakaSpacing.lg.dp)
                )
            }
        }
    }
}

// MARK: - Step Overlay

@Composable
private fun CurrentStepOverlay(
    step: FollowAlongStep?,
    stepIndex: Int,
    totalSteps: Int,
    remainingSeconds: Int,
    modifier: Modifier = Modifier
) {
    if (step == null) return

    Surface(
        modifier = modifier,
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Step ${stepIndex + 1} of $totalSteps",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmakaColors.textTertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AmakaColors.textPrimary
                )
            }

            if (step.isTimeBased) {
                val m = remainingSeconds / 60
                val s = remainingSeconds % 60
                Text(
                    text = String.format("%d:%02d", m, s),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    ),
                    color = AmakaColors.accentGreen
                )
            } else if (step.reps != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${step.reps}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        ),
                        color = AmakaColors.accentBlue
                    )
                    Text(
                        text = "reps",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmakaColors.textSecondary
                    )
                }
            }
        }
    }
}

// MARK: - Step Chips

@Composable
private fun StepChips(
    steps: List<FollowAlongStep>,
    currentIndex: Int,
    onStepClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = AmakaSpacing.md.dp),
        horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
    ) {
        steps.forEachIndexed { index, step ->
            val isCurrent = index == currentIndex
            val isPast = index < currentIndex

            val bgColor = when {
                isCurrent -> AmakaColors.accentBlue
                isPast -> AmakaColors.accentGreen.copy(alpha = 0.15f)
                else -> AmakaColors.surface
            }
            val textColor = when {
                isCurrent -> Color.White
                isPast -> AmakaColors.accentGreen
                else -> AmakaColors.textSecondary
            }

            Surface(
                modifier = Modifier
                    .clickable { onStepClick(index) }
                    .widthIn(min = 80.dp),
                color = bgColor,
                shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = AmakaSpacing.sm.dp,
                        vertical = AmakaSpacing.xs.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = step.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = step.formattedDuration,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// MARK: - Player Controls

@Composable
private fun PlayerControls(
    isPlaying: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
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
            // Previous
            IconButton(
                onClick = onPrevious,
                enabled = canGoBack
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous step",
                    tint = if (canGoBack) AmakaColors.textPrimary else AmakaColors.textTertiary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Play/Pause
            Surface(
                onClick = onPlayPause,
                color = AmakaColors.accentGreen,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
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

            // Next
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next step",
                    tint = AmakaColors.textPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

// MARK: - Ended View

@Composable
private fun EndedView(
    formattedElapsed: String,
    errorMessage: String?,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AmakaSpacing.lg.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = AmakaColors.accentGreen,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
        Text(
            text = if (errorMessage != null) "Error" else "Follow-Along Complete!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary
        )
        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = AmakaColors.textSecondary,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "Duration: $formattedElapsed",
                style = MaterialTheme.typography.bodyMedium,
                color = AmakaColors.textSecondary
            )
        }
        Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AmakaColors.accentBlue),
            shape = RoundedCornerShape(AmakaCornerRadius.lg.dp)
        ) {
            Text("Done", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// MARK: - Step List Sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepListSheet(
    steps: List<FollowAlongStep>,
    currentStepIndex: Int,
    onStepSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AmakaColors.background
    ) {
        Text(
            text = "All Steps",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary,
            modifier = Modifier.padding(horizontal = AmakaSpacing.lg.dp, vertical = AmakaSpacing.sm.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            itemsIndexed(steps) { index, step ->
                val isCurrent = index == currentStepIndex
                val isPast = index < currentStepIndex

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStepSelected(index) },
                    color = if (isCurrent) AmakaColors.accentBlue.copy(alpha = 0.1f) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AmakaSpacing.lg.dp, vertical = AmakaSpacing.sm.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Step number
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCurrent -> AmakaColors.accentBlue
                                        isPast -> AmakaColors.accentGreen.copy(alpha = 0.2f)
                                        else -> AmakaColors.surfaceElevated
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isCurrent -> Color.White
                                    isPast -> AmakaColors.accentGreen
                                    else -> AmakaColors.textSecondary
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = step.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = AmakaColors.textPrimary
                            )
                            Text(
                                text = step.formattedDuration,
                                style = MaterialTheme.typography.bodySmall,
                                color = AmakaColors.textSecondary
                            )
                        }

                        when {
                            isCurrent -> Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Current",
                                tint = AmakaColors.accentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            isPast -> Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Complete",
                                tint = AmakaColors.accentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
    }
}
