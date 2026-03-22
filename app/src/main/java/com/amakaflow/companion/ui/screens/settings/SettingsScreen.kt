package com.amakaflow.companion.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.BuildConfig
import com.amakaflow.companion.data.AppEnvironment
import com.amakaflow.companion.data.TestConfig
import com.amakaflow.companion.debug.DebugLog
import com.amakaflow.companion.simulation.SimulationSettings
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing
import kotlinx.coroutines.delay

enum class WorkoutDevice(val title: String, val subtitle: String) {
    WEAR_OS_PHONE("Wear OS + Android Phone", "Watch tracks • Phone guides"),
    PHONE_ONLY("Android Phone Only", "Full-screen follow-along")
}

enum class MusicBehavior {
    DUCK, PAUSE
}

@Composable
fun SettingsScreen(
    onNavigateToPairing: () -> Unit = {},
    onNavigateToWorkoutDebug: () -> Unit = {},
    onNavigateToDebugLog: () -> Unit = {},
    onNavigateToTranscriptionSettings: () -> Unit = {},
    testConfig: TestConfig? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
    simulationSettings: SimulationSettings? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEnvironmentDialog by remember { mutableStateOf(false) }
    var showUnpairDialog by remember { mutableStateOf(false) }

    // 7-tap gesture to access debug settings
    var tapCount by remember { mutableIntStateOf(0) }
    var showDebugSettings by remember { mutableStateOf(false) }

    // Reset tap count after 2 seconds of inactivity
    LaunchedEffect(tapCount) {
        if (tapCount > 0 && tapCount < 7) {
            delay(2000)
            tapCount = 0
        }
    }

    // Settings state
    var selectedDevice by remember { mutableStateOf(WorkoutDevice.PHONE_ONLY) }
    var voiceCuesEnabled by remember { mutableStateOf(true) }
    var musicBehavior by remember { mutableStateOf(MusicBehavior.DUCK) }
    var countdownBeepsEnabled by remember { mutableStateOf(true) }
    var hapticFeedbackEnabled by remember { mutableStateOf(true) }

    // Use provided simulationSettings if available, otherwise get from viewModel
    val effectiveSimulationSettings = simulationSettings ?: viewModel.simulationSettings

    // Show debug settings as fullscreen overlay
    if (showDebugSettings) {
        DebugSettingsScreen(
            settings = effectiveSimulationSettings,
            onBack = { showDebugSettings = false }
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .padding(horizontal = AmakaSpacing.md.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = AmakaColors.textPrimary,
                    modifier = Modifier.clickable {
                        tapCount++
                        DebugLog.info("Settings tap: $tapCount/7", "SettingsScreen")
                        if (tapCount >= 7) {
                            DebugLog.info("Opening debug settings!", "SettingsScreen")
                            showDebugSettings = true
                            tapCount = 0
                        }
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
                // Show tap progress hint after 3 taps (like iOS)
                if (tapCount >= 3 && tapCount < 7) {
                    Text(
                        text = "${7 - tapCount} more...",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmakaColors.textTertiary.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
        }

        // WORKOUT DEVICE section
        item {
            SectionHeader("WORKOUT DEVICE")
        }
        item {
            DeviceOptionItem(
                icon = Icons.Filled.Watch,
                iconBackground = AmakaColors.accentBlue.copy(alpha = 0.2f),
                iconTint = AmakaColors.accentBlue,
                title = WorkoutDevice.WEAR_OS_PHONE.title,
                subtitle = WorkoutDevice.WEAR_OS_PHONE.subtitle,
                isSelected = selectedDevice == WorkoutDevice.WEAR_OS_PHONE,
                onClick = { selectedDevice = WorkoutDevice.WEAR_OS_PHONE }
            )
        }
        item {
            DeviceOptionItem(
                icon = Icons.Filled.PhoneAndroid,
                iconBackground = AmakaColors.accentGreen.copy(alpha = 0.2f),
                iconTint = AmakaColors.accentGreen,
                title = WorkoutDevice.PHONE_ONLY.title,
                subtitle = WorkoutDevice.PHONE_ONLY.subtitle,
                isSelected = selectedDevice == WorkoutDevice.PHONE_ONLY,
                onClick = { selectedDevice = WorkoutDevice.PHONE_ONLY }
            )
        }

        // AUDIO CUES section
        item {
            Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
            SectionHeader("AUDIO CUES")
        }
        item {
            ToggleSettingItem(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                iconBackground = AmakaColors.accentBlue.copy(alpha = 0.2f),
                iconTint = AmakaColors.accentBlue,
                title = "Voice Cues",
                subtitle = "Announce exercise names and transitions",
                isEnabled = voiceCuesEnabled,
                onToggle = { voiceCuesEnabled = it }
            )
        }

        // Music behavior toggle (only show if voice cues enabled)
        if (voiceCuesEnabled) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AmakaColors.surface,
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(AmakaSpacing.md.dp)
                    ) {
                        Text(
                            text = "When music is playing:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
                        ) {
                            MusicBehaviorButton(
                                text = "Duck music",
                                isSelected = musicBehavior == MusicBehavior.DUCK,
                                onClick = { musicBehavior = MusicBehavior.DUCK },
                                modifier = Modifier.weight(1f)
                            )
                            MusicBehaviorButton(
                                text = "Pause music",
                                isSelected = musicBehavior == MusicBehavior.PAUSE,
                                onClick = { musicBehavior = MusicBehavior.PAUSE },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        item {
            ToggleSettingItem(
                icon = Icons.Filled.Timer,
                iconBackground = AmakaColors.accentBlue.copy(alpha = 0.2f),
                iconTint = AmakaColors.accentBlue,
                title = "Countdown Beeps",
                subtitle = "Audio beeps for last 5 seconds of timed intervals",
                isEnabled = countdownBeepsEnabled,
                onToggle = { countdownBeepsEnabled = it }
            )
        }
        item {
            ToggleSettingItem(
                icon = Icons.Filled.Vibration,
                iconBackground = AmakaColors.accentBlue.copy(alpha = 0.2f),
                iconTint = AmakaColors.accentBlue,
                title = "Haptic Feedback",
                subtitle = "Vibrate on exercise transitions (watch & phone)",
                isEnabled = hapticFeedbackEnabled,
                onToggle = { hapticFeedbackEnabled = it }
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AmakaSpacing.sm.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = AmakaColors.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                Text(
                    text = "Audio cues work with any music app. Your music controls remain accessible via headphones, Control Center, or lock screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textTertiary
                )
            }
        }

        // VOICE TRANSCRIPTION section
        item {
            Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
            SectionHeader("VOICE TRANSCRIPTION")
        }
        item {
            NavigationSettingItem(
                icon = Icons.Filled.GraphicEq,
                iconBackground = Color(0xFF9C27B0).copy(alpha = 0.2f),
                iconTint = Color(0xFF9C27B0),
                title = "Transcription Settings",
                subtitle = "Provider, accent, and personal dictionary",
                onClick = onNavigateToTranscriptionSettings
            )
        }

        // INTEGRATIONS section
        item {
            Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
            SectionHeader("INTEGRATIONS")
        }
        item {
            IntegrationItem(
                icon = Icons.Filled.Favorite,
                iconBackground = AmakaColors.accentRed,
                title = "Health Connect",
                subtitle = "Sync workouts and activity data",
                isConnected = false,
                onReauthorize = { /* Re-authorize Health Connect */ }
            )
        }

        // ACCOUNT section
        item {
            Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
            SectionHeader("ACCOUNT")
        }
        item {
            AccountInfoCard(
                userName = uiState.userEmail?.substringBefore("@")?.replace("+", " ")?.replaceFirstChar { it.uppercase() } ?: "User",
                userEmail = uiState.userEmail ?: "Not connected",
                isConnected = uiState.isPaired,
                environment = uiState.environment.displayName,
                appVersion = uiState.appVersion,
                onCopyToken = { /* Copy API token */ },
                onRefreshToken = { /* Refresh token */ },
                onCheckNow = { viewModel.checkPendingWorkouts() },
                onWorkoutDebug = onNavigateToWorkoutDebug,
                onEnvironmentClick = { showEnvironmentDialog = true },
                pendingWorkouts = uiState.pendingWorkouts
            )
        }
        item {
            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            DisconnectButton(
                onClick = { showUnpairDialog = true }
            )
        }

        // DEBUG section (only in debug builds or staging)
        if (BuildConfig.DEBUG || uiState.environment == AppEnvironment.STAGING) {
            item {
                Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
                SectionHeader("DEBUG")
            }
            item {
                val logCount by DebugLog.entries.collectAsState()
                NavigationSettingItem(
                    icon = Icons.Filled.BugReport,
                    iconBackground = AmakaColors.accentOrange.copy(alpha = 0.2f),
                    iconTint = AmakaColors.accentOrange,
                    title = "Debug Logs",
                    subtitle = "${logCount.size} entries",
                    onClick = onNavigateToDebugLog
                )
            }
        }

        // LEGAL section
        item {
            Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
            SectionHeader("LEGAL")
        }
        item {
            NavigationSettingItem(
                icon = Icons.Filled.Info,
                iconBackground = AmakaColors.accentBlue.copy(alpha = 0.2f),
                iconTint = AmakaColors.accentBlue,
                title = "About",
                subtitle = null,
                onClick = { /* Navigate to about */ }
            )
        }

        item {
            Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))
        }
    }

    // Environment selection dialog
    if (showEnvironmentDialog) {
        AlertDialog(
            onDismissRequest = { showEnvironmentDialog = false },
            title = { Text("Select Environment") },
            text = {
                Column {
                    AppEnvironment.entries.forEach { env ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(AmakaCornerRadius.sm.dp))
                                .clickable {
                                    viewModel.setEnvironment(env)
                                    showEnvironmentDialog = false
                                }
                                .padding(AmakaSpacing.md.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.environment == env,
                                onClick = {
                                    viewModel.setEnvironment(env)
                                    showEnvironmentDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                            Text(
                                text = env.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEnvironmentDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = AmakaColors.surface,
            titleContentColor = AmakaColors.textPrimary,
            textContentColor = AmakaColors.textPrimary
        )
    }

    // Unpair confirmation dialog
    if (showUnpairDialog) {
        AlertDialog(
            onDismissRequest = { showUnpairDialog = false },
            title = { Text("Disconnect Account?") },
            text = {
                Text("This will remove the connection to your account. You can pair again anytime.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unpair()
                        showUnpairDialog = false
                        onNavigateToPairing()
                    }
                ) {
                    Text(
                        text = "Disconnect",
                        color = AmakaColors.accentRed
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpairDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = AmakaColors.surface,
            titleContentColor = AmakaColors.textPrimary,
            textContentColor = AmakaColors.textSecondary
        )
    }

}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = AmakaColors.textTertiary,
        modifier = Modifier.padding(vertical = AmakaSpacing.sm.dp)
    )
}

@Composable
private fun DeviceOptionItem(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = AmakaColors.accentBlue,
                        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(AmakaCornerRadius.md.dp))
            .clickable(onClick = onClick),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = iconBackground,
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = AmakaColors.textPrimary
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = AmakaColors.accentBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ToggleSettingItem(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = isEnabled,
                    onValueChange = onToggle,
                    role = Role.Switch
                )
                .padding(AmakaSpacing.md.dp)
                .semantics { contentDescription = "$title. $subtitle" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = iconBackground,
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AmakaColors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AmakaColors.textPrimary,
                    checkedTrackColor = AmakaColors.accentBlue,
                    uncheckedThumbColor = AmakaColors.textTertiary,
                    uncheckedTrackColor = AmakaColors.surface
                )
            )
        }
    }
}

@Composable
private fun MusicBehaviorButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(AmakaCornerRadius.lg.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) AmakaColors.accentBlue else AmakaColors.background,
        shape = RoundedCornerShape(AmakaCornerRadius.lg.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) AmakaColors.textPrimary else AmakaColors.textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun NavigationSettingItem(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AmakaCornerRadius.md.dp))
            .clickable(onClick = onClick),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = iconBackground,
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AmakaColors.textPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textSecondary
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = AmakaColors.textTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun IntegrationItem(
    icon: ImageVector,
    iconBackground: Color,
    title: String,
    subtitle: String,
    isConnected: Boolean,
    onReauthorize: () -> Unit
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    color = iconBackground,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = AmakaColors.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = AmakaColors.textPrimary
                        )
                        if (isConnected) {
                            Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                            Text(
                                text = "✓ Connected",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmakaColors.accentGreen
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            OutlinedButton(
                onClick = onReauthorize,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AmakaColors.textPrimary
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(AmakaColors.borderLight)
                ),
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
            ) {
                Text(if (isConnected) "Re-authorize Health" else "Connect Health")
            }
        }
    }
}

@Composable
private fun AccountInfoCard(
    userName: String,
    userEmail: String,
    isConnected: Boolean,
    environment: String,
    appVersion: String,
    onCopyToken: () -> Unit,
    onRefreshToken: () -> Unit,
    onCheckNow: () -> Unit,
    onWorkoutDebug: () -> Unit,
    onEnvironmentClick: () -> Unit,
    pendingWorkouts: PendingWorkoutsState = PendingWorkoutsState()
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(
            modifier = Modifier.padding(AmakaSpacing.md.dp)
        ) {
            // User info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    color = AmakaColors.accentGreen,
                    shape = CircleShape
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = AmakaColors.background,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AmakaColors.textPrimary
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textSecondary
                    )
                }
                if (isConnected) {
                    Surface(
                        color = AmakaColors.accentGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(AmakaCornerRadius.lg.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(AmakaColors.accentGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Connected",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmakaColors.accentGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))

            // Info rows
            AccountInfoRow(
                icon = Icons.Filled.Cloud,
                label = "Environment: $environment",
                onClick = onEnvironmentClick
            )
            AccountInfoRow(
                icon = Icons.Filled.Info,
                label = "Version $appVersion"
            )
            AccountInfoRow(
                icon = Icons.Filled.Key,
                label = "API Token",
                action = {
                    SmallButton(text = "Copy", color = AmakaColors.accentBlue, onClick = onCopyToken)
                }
            )
            AccountInfoRow(
                icon = Icons.Filled.Refresh,
                label = "Refresh Token",
                action = {
                    SmallButton(text = "Refresh", color = AmakaColors.accentGreen, onClick = onRefreshToken)
                }
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

            // Pending workouts
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = AmakaColors.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                Text(
                    text = "Pending Workouts:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmakaColors.textSecondary
                )
            }

            // Pending workouts content
            Column(modifier = Modifier.padding(start = 24.dp, top = 4.dp)) {
                when {
                    pendingWorkouts.isLoading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp,
                                color = AmakaColors.accentBlue
                            )
                            Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                            Text(
                                text = "Checking...",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmakaColors.textTertiary
                            )
                        }
                    }
                    pendingWorkouts.error != null -> {
                        Text(
                            text = "Error: ${pendingWorkouts.error}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmakaColors.accentRed
                        )
                    }
                    pendingWorkouts.workouts.isEmpty() -> {
                        Text(
                            text = "No pending workouts",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmakaColors.textTertiary
                        )
                    }
                    else -> {
                        Text(
                            text = "Found ${pendingWorkouts.workouts.size} workout(s)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = AmakaColors.textPrimary
                        )
                        pendingWorkouts.workouts.firstOrNull()?.let { workout ->
                            Text(
                                text = "First: ${workout.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmakaColors.textSecondary
                            )
                        }
                        if (pendingWorkouts.isSynced) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = AmakaColors.accentGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Synced!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmakaColors.accentGreen
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
            ) {
                SmallButton(
                    text = "Check Now",
                    color = AmakaColors.accentBlue,
                    onClick = onCheckNow
                )
                SmallButton(
                    text = "Workout Debug",
                    color = AmakaColors.accentOrange,
                    onClick = onWorkoutDebug,
                    icon = Icons.Filled.BugReport
                )
            }
        }
    }
}

@Composable
private fun AccountInfoRow(
    icon: ImageVector,
    label: String,
    action: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = AmakaSpacing.xs.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AmakaColors.textTertiary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AmakaColors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}

@Composable
private fun SmallButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(AmakaCornerRadius.sm.dp))
            .clickable(onClick = onClick),
        color = color,
        shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AmakaColors.textPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = AmakaColors.textPrimary
            )
        }
    }
}

@Composable
private fun DisconnectButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AmakaCornerRadius.md.dp))
            .clickable(onClick = onClick),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = AmakaColors.accentRed.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExitToApp,
                        contentDescription = null,
                        tint = AmakaColors.accentRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))
            Text(
                text = "Disconnect Account",
                style = MaterialTheme.typography.titleMedium,
                color = AmakaColors.textPrimary
            )
        }
    }
}
