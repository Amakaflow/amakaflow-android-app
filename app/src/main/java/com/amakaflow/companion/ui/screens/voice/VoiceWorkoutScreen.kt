package com.amakaflow.companion.ui.screens.voice

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay

enum class VoiceWorkoutState {
    PERMISSIONS_REQUIRED,
    READY_TO_RECORD,
    RECORDING
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceWorkoutScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    var currentState by remember { mutableStateOf(VoiceWorkoutState.PERMISSIONS_REQUIRED) }
    var recordingTime by remember { mutableIntStateOf(0) }

    // Update state based on permission
    LaunchedEffect(micPermissionState.status.isGranted) {
        currentState = if (micPermissionState.status.isGranted) {
            VoiceWorkoutState.READY_TO_RECORD
        } else {
            VoiceWorkoutState.PERMISSIONS_REQUIRED
        }
    }

    // Recording timer
    LaunchedEffect(currentState) {
        if (currentState == VoiceWorkoutState.RECORDING) {
            recordingTime = 0
            while (currentState == VoiceWorkoutState.RECORDING) {
                delay(1000)
                recordingTime++
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("voice_workout_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.md.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                if (currentState == VoiceWorkoutState.RECORDING) {
                    currentState = VoiceWorkoutState.READY_TO_RECORD
                } else {
                    onDismiss()
                }
            }) {
                Text(
                    text = if (currentState == VoiceWorkoutState.RECORDING) "Back" else if (currentState == VoiceWorkoutState.PERMISSIONS_REQUIRED) "Back" else "Close",
                    color = AmakaColors.accentBlue,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Text(
                text = when (currentState) {
                    VoiceWorkoutState.PERMISSIONS_REQUIRED -> "Permissions Required"
                    VoiceWorkoutState.READY_TO_RECORD -> "Create Workout"
                    VoiceWorkoutState.RECORDING -> "Recording"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )

            // Spacer to balance the header
            Spacer(modifier = Modifier.width(60.dp))
        }

        when (currentState) {
            VoiceWorkoutState.PERMISSIONS_REQUIRED -> {
                PermissionsRequiredContent(
                    onEnablePermissions = {
                        micPermissionState.launchPermissionRequest()
                    },
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    showRationale = micPermissionState.status.shouldShowRationale
                )
            }
            VoiceWorkoutState.READY_TO_RECORD -> {
                ReadyToRecordContent(
                    onStartRecording = {
                        currentState = VoiceWorkoutState.RECORDING
                    }
                )
            }
            VoiceWorkoutState.RECORDING -> {
                RecordingContent(
                    recordingTime = recordingTime,
                    onStopRecording = {
                        currentState = VoiceWorkoutState.READY_TO_RECORD
                        // TODO: Process the recording
                    },
                    onCancel = {
                        currentState = VoiceWorkoutState.READY_TO_RECORD
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionsRequiredContent(
    onEnablePermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    showRationale: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AmakaSpacing.xl.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Microphone icon with slash
        Surface(
            modifier = Modifier.size(80.dp),
            color = Color.Transparent,
            shape = CircleShape
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.MicOff,
                    contentDescription = null,
                    tint = AmakaColors.accentOrange,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))

        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

        Text(
            text = "AmakaFlow needs access to your microphone and speech recognition to create workouts from your voice.",
            style = MaterialTheme.typography.bodyMedium,
            color = AmakaColors.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Enable Permissions button
        Button(
            onClick = onEnablePermissions,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AmakaColors.accentBlue,
                contentColor = AmakaColors.textPrimary
            ),
            shape = RoundedCornerShape(AmakaCornerRadius.lg.dp)
        ) {
            Text(
                text = "Enable Permissions",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

        // Open Settings button
        TextButton(onClick = onOpenSettings) {
            Text(
                text = "Open Settings",
                color = AmakaColors.textSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ReadyToRecordContent(
    onStartRecording: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AmakaSpacing.xl.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Large blue microphone button
        Surface(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .clickable(onClick = onStartRecording)
                .testTag("record_button"),
            color = AmakaColors.accentBlue,
            shape = CircleShape
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Start Recording",
                    tint = AmakaColors.textPrimary,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))

        Text(
            text = "Tap to Start Recording",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

        Text(
            text = "Describe the workout you just completed",
            style = MaterialTheme.typography.bodyMedium,
            color = AmakaColors.textSecondary
        )

        Text(
            text = "Include exercises, sets, reps, and how long it took",
            style = MaterialTheme.typography.bodySmall,
            color = AmakaColors.textTertiary
        )

        Spacer(modifier = Modifier.weight(1f))

        // Example prompts
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AmakaColors.surface,
            shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
        ) {
            Column(
                modifier = Modifier.padding(AmakaSpacing.md.dp)
            ) {
                Text(
                    text = "Try saying something like:",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary
                )

                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

                ExamplePrompt(
                    text = "\"I just did a 45 minute strength workout with 4 sets of 10 squats, 3 sets of 12 lunges, and 3 sets of 15 pushups\""
                )

                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

                ExamplePrompt(
                    text = "\"Just finished a 30 minute run, felt pretty good, did about 5K\""
                )

                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

                ExamplePrompt(
                    text = "\"I did upper body today for about an hour: bench press 4x8, overhead press 3x10, and dumbbell rows 3x12 each side\""
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun ExamplePrompt(text: String) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "❝❝",
            style = MaterialTheme.typography.bodySmall,
            color = AmakaColors.accentOrange
        )
        Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = AmakaColors.textSecondary
        )
    }
}

@Composable
private fun RecordingContent(
    recordingTime: Int,
    onStopRecording: () -> Unit,
    onCancel: () -> Unit
) {
    // Pulsing animation for recording indicator
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse2"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AmakaSpacing.xl.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Animated recording indicator
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            // Outer pulse ring
            Surface(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulse2),
                color = AmakaColors.accentRed.copy(alpha = 0.15f),
                shape = CircleShape
            ) {}

            // Inner pulse ring
            Surface(
                modifier = Modifier
                    .size(130.dp)
                    .scale(pulse1),
                color = AmakaColors.accentRed.copy(alpha = 0.3f),
                shape = CircleShape
            ) {}

            // Center microphone
            Surface(
                modifier = Modifier.size(100.dp),
                color = AmakaColors.accentRed,
                shape = CircleShape
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Recording",
                        tint = AmakaColors.textPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))

        // Timer display
        Text(
            text = formatTime(recordingTime),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Light,
            color = AmakaColors.textPrimary,
            fontSize = 56.sp
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

        Text(
            text = "Describe your workout...",
            style = MaterialTheme.typography.bodyMedium,
            color = AmakaColors.textSecondary
        )

        Spacer(modifier = Modifier.weight(1f))

        // Stop Recording button
        Button(
            onClick = onStopRecording,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AmakaColors.accentRed,
                contentColor = AmakaColors.textPrimary
            ),
            shape = RoundedCornerShape(AmakaCornerRadius.lg.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
            Text(
                text = "Stop Recording",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

        // Cancel button
        TextButton(onClick = onCancel) {
            Text(
                text = "Cancel",
                color = AmakaColors.textSecondary,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}
