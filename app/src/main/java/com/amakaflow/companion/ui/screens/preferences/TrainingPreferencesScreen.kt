package com.amakaflow.companion.ui.screens.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

private val availableSports = listOf("running", "cycling", "strength", "swimming", "cardio", "mobility")
private val fitnessLevels = listOf("beginner", "intermediate", "advanced")

@Composable
fun TrainingPreferencesScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: TrainingPreferencesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("training_preferences_screen")
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
                text = "Training Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AmakaColors.accentBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AmakaSpacing.md.dp),
                verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
            ) {
                // Days per week
                item {
                    PreferenceSection(title = "Days per week") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            (1..7).forEach { day ->
                                FilterChip(
                                    selected = state.preferences.daysPerWeek == day,
                                    onClick = { viewModel.updateDaysPerWeek(day) },
                                    label = { Text("$day") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmakaColors.accentBlue,
                                        selectedLabelColor = AmakaColors.textPrimary,
                                        containerColor = AmakaColors.surface,
                                        labelColor = AmakaColors.textSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                // Max session duration
                item {
                    PreferenceSection(title = "Max session (minutes)") {
                        Slider(
                            value = state.preferences.maxSessionMinutes.toFloat(),
                            onValueChange = { viewModel.updateMaxSessionMinutes(it.toInt()) },
                            valueRange = 15f..120f,
                            steps = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("max_session_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = AmakaColors.accentBlue,
                                activeTrackColor = AmakaColors.accentBlue
                            )
                        )
                        Text(
                            text = "${state.preferences.maxSessionMinutes} min",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.textSecondary
                        )
                    }
                }

                // Fitness level
                item {
                    PreferenceSection(title = "Fitness level") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
                        ) {
                            fitnessLevels.forEach { level ->
                                FilterChip(
                                    selected = state.preferences.fitnessLevel == level,
                                    onClick = { viewModel.updateFitnessLevel(level) },
                                    label = {
                                        Text(level.replaceFirstChar { it.uppercase() })
                                    },
                                    modifier = Modifier.testTag("fitness_level_$level"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmakaColors.accentBlue,
                                        selectedLabelColor = AmakaColors.textPrimary,
                                        containerColor = AmakaColors.surface,
                                        labelColor = AmakaColors.textSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                // Preferred sports
                item {
                    PreferenceSection(title = "Preferred sports") {
                        Column(verticalArrangement = Arrangement.spacedBy(AmakaSpacing.xs.dp)) {
                            availableSports.chunked(3).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
                                ) {
                                    row.forEach { sport ->
                                        FilterChip(
                                            selected = state.preferences.preferredSports.contains(sport),
                                            onClick = { viewModel.toggleSport(sport) },
                                            label = {
                                                Text(sport.replaceFirstChar { it.uppercase() })
                                            },
                                            modifier = Modifier.testTag("sport_$sport"),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AmakaColors.accentBlue,
                                                selectedLabelColor = AmakaColors.textPrimary,
                                                containerColor = AmakaColors.surface,
                                                labelColor = AmakaColors.textSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Goal
                item {
                    PreferenceSection(title = "Training goal") {
                        OutlinedTextField(
                            value = state.preferences.goal ?: "",
                            onValueChange = { viewModel.updateGoal(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("goal_input"),
                            placeholder = {
                                Text("e.g., Run a 5K, Build muscle", color = AmakaColors.textTertiary)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmakaColors.accentBlue,
                                unfocusedBorderColor = AmakaColors.borderMedium,
                                focusedTextColor = AmakaColors.textPrimary,
                                unfocusedTextColor = AmakaColors.textPrimary,
                                cursorColor = AmakaColors.accentBlue
                            ),
                            shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
                        )
                    }
                }

                // Save button
                item {
                    Button(
                        onClick = { viewModel.savePreferences() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_preferences_button"),
                        enabled = !state.isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmakaColors.accentBlue,
                            contentColor = AmakaColors.textPrimary
                        ),
                        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AmakaColors.textPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save Preferences", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (state.saveSuccess) {
                        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                        Text(
                            text = "Preferences saved!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.accentGreen
                        )
                    }

                    state.error?.let { error ->
                        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.accentRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
            content()
        }
    }
}
