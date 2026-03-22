package com.amakaflow.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import com.amakaflow.wear.presentation.ExecutionPhase
import com.amakaflow.wear.presentation.WorkoutExecutionViewModel
import com.amakaflow.wear.ui.theme.HeartRateRed

@Composable
fun WorkoutExecutionScreen(
    workoutId: String,
    onFinished: () -> Unit,
    viewModel: WorkoutExecutionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (state.phase) {
            ExecutionPhase.LOADING -> LoadingView()
            ExecutionPhase.COUNTDOWN -> CountdownView(state.countdownValue)
            ExecutionPhase.ACTIVE -> ActiveIntervalView(
                state = state,
                onPause = viewModel::onPause,
                onRepsComplete = viewModel::onRepsCompleted,
                onEnd = viewModel::onEnd
            )
            ExecutionPhase.REST -> RestView(
                state = state,
                onSkipRest = viewModel::onSkipRest,
                onPause = viewModel::onPause
            )
            ExecutionPhase.PAUSED -> PausedView(
                state = state,
                onResume = viewModel::onResume,
                onEnd = viewModel::onEnd
            )
            ExecutionPhase.COMPLETED -> CompletedView(
                state = state,
                onDone = onFinished
            )
            ExecutionPhase.ERROR -> ErrorView(
                message = state.error ?: "Unknown error",
                onDismiss = onFinished
            )
        }
    }
}

@Composable
private fun LoadingView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Loading...",
            style = MaterialTheme.typography.body2
        )
    }
}

@Composable
private fun CountdownView(value: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Get Ready",
            style = MaterialTheme.typography.title3,
            color = MaterialTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value.toString(),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )
    }
}

@Composable
private fun ActiveIntervalView(
    state: com.amakaflow.wear.presentation.WorkoutExecutionUiState,
    onPause: () -> Unit,
    onRepsComplete: () -> Unit,
    onEnd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Progress and round info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicator
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = state.progressFraction,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 3.dp,
                    indicatorColor = MaterialTheme.colors.primary
                )
            }
            state.roundInfo?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.caption3,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            }
        }

        // Interval name and timer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = state.currentInterval?.name ?: state.currentInterval?.kind?.replaceFirstChar { it.uppercase() } ?: "",
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (state.isTimedInterval) {
                // Timer display
                Text(
                    text = state.formattedRemaining,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.remainingSeconds <= 3) MaterialTheme.colors.error else MaterialTheme.colors.onSurface
                )
            } else if (state.targetReps > 0) {
                // Reps display
                Text(
                    text = "${state.targetReps} reps",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                state.currentInterval?.load?.let { load ->
                    Text(
                        text = load,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }
            }

            // Heart rate
            state.currentHeartRate?.let { hr ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Heart Rate",
                        modifier = Modifier.size(14.dp),
                        tint = HeartRateRed
                    )
                    Text(
                        text = "$hr",
                        style = MaterialTheme.typography.caption1,
                        color = HeartRateRed
                    )
                }
            }
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Pause button
            CompactButton(
                onClick = onPause,
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    modifier = Modifier.size(20.dp)
                )
            }

            if (!state.isTimedInterval && state.targetReps > 0) {
                // Done button for reps
                Button(
                    onClick = onRepsComplete,
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // End button
            CompactButton(
                onClick = onEnd,
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "End",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RestView(
    state: com.amakaflow.wear.presentation.WorkoutExecutionUiState,
    onSkipRest: () -> Unit,
    onPause: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Rest",
            style = MaterialTheme.typography.title3,
            color = MaterialTheme.colors.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = state.formattedRemaining,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Heart rate during rest
        state.currentHeartRate?.let { hr ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = HeartRateRed
                )
                Text(text = "$hr", style = MaterialTheme.typography.caption1, color = HeartRateRed)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Skip rest
        CompactButton(
            onClick = onSkipRest,
            colors = ButtonDefaults.secondaryButtonColors()
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Skip Rest",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PausedView(
    state: com.amakaflow.wear.presentation.WorkoutExecutionUiState,
    onResume: () -> Unit,
    onEnd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Paused",
            style = MaterialTheme.typography.title3,
            color = MaterialTheme.colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = state.formattedElapsed,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onResume,
                colors = ButtonDefaults.primaryButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume",
                    modifier = Modifier.size(24.dp)
                )
            }
            Button(
                onClick = onEnd,
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "End",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CompletedView(
    state: com.amakaflow.wear.presentation.WorkoutExecutionUiState,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colors.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Workout Complete!",
            style = MaterialTheme.typography.title3,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = state.formattedElapsed,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurfaceVariant
        )
        state.currentHeartRate?.let { hr ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = HeartRateRed
                )
                Text(text = "Avg $hr bpm", style = MaterialTheme.typography.caption2, color = HeartRateRed)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onDone,
            colors = ButtonDefaults.primaryButtonColors()
        ) {
            Text("Done")
        }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.title3,
            color = MaterialTheme.colors.error
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onDismiss) {
            Text("Back")
        }
    }
}
