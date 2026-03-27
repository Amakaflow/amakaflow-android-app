package com.amakaflow.companion.ui.screens.social

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.model.SocialSettings
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing
import kotlinx.coroutines.launch

private const val TAG = "SocialSettingsScreen"

@Composable
fun SocialSettingsScreen(
    onNavigateBack: () -> Unit = {},
    api: AmakaflowApi? = null
) {
    var settings by remember { mutableStateOf(SocialSettings()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        api?.let {
            try {
                val response = it.getSocialSettings()
                if (response.isSuccessful) {
                    response.body()?.let { s -> settings = s }
                }
            } catch (e: Exception) {
                Log.e(TAG, "load settings error", e)
            }
            isLoading = false
        } ?: run { isLoading = false }
    }

    fun save(newSettings: SocialSettings) {
        settings = newSettings
        scope.launch {
            try {
                api?.updateSocialSettings(newSettings)
            } catch (e: Exception) {
                Log.e(TAG, "save settings error", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("social_settings_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.sm.dp, vertical = AmakaSpacing.sm.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AmakaColors.textPrimary
                )
            }
            Text(
                "Social Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AmakaColors.accentBlue)
            }
        } else {
            Column(
                modifier = Modifier.padding(AmakaSpacing.md.dp),
                verticalArrangement = Arrangement.spacedBy(AmakaSpacing.lg.dp)
            ) {
                // Discoverable
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Discoverable",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = AmakaColors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.discoverable,
                            onCheckedChange = { save(settings.copy(discoverable = it)) },
                            colors = SwitchDefaults.colors(checkedTrackColor = AmakaColors.accentBlue),
                            modifier = Modifier.testTag("toggle_discoverable")
                        )
                    }
                    Text(
                        "Allow other users to find you by name or username.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textTertiary
                    )
                }

                // Share Workouts
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Share Workouts",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = AmakaColors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.shareWorkouts,
                            onCheckedChange = { save(settings.copy(shareWorkouts = it)) },
                            colors = SwitchDefaults.colors(checkedTrackColor = AmakaColors.accentBlue),
                            modifier = Modifier.testTag("toggle_share_workouts")
                        )
                    }
                    Text(
                        "Automatically share completed workouts with your followers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textTertiary
                    )
                }

                // Hide Weights
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Hide Weights",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = AmakaColors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.hideWeights,
                            onCheckedChange = { save(settings.copy(hideWeights = it)) },
                            colors = SwitchDefaults.colors(checkedTrackColor = AmakaColors.accentBlue),
                            modifier = Modifier.testTag("toggle_hide_weights")
                        )
                    }
                    Text(
                        "Hide specific weights from shared workouts. Only exercise names and sets/reps will be shown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textTertiary
                    )
                }
            }
        }
    }
}
