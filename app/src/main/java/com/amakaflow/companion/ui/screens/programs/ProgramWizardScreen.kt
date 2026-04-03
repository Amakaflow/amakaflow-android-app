@file:OptIn(ExperimentalLayoutApi::class)

package com.amakaflow.companion.ui.screens.programs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")
private val DAY_NUMBERS = listOf(1, 2, 3, 4, 5, 6, 7)

@Composable
fun ProgramWizardScreen(
    onNavigateBack: () -> Unit,
    onProgramCreated: (String) -> Unit = {},
    viewModel: ProgramWizardViewModel = hiltViewModel()
) {
    val state = viewModel.state

    // When complete, notify caller
    LaunchedEffect(state.isComplete, state.generatedProgramId) {
        if (state.isComplete && state.generatedProgramId != null) {
            onProgramCreated(state.generatedProgramId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("program_wizard_screen")
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.sm.dp, vertical = AmakaSpacing.sm.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (state.currentStep == WizardStep.GOAL) onNavigateBack()
                else viewModel.goBack()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AmakaColors.textPrimary
                )
            }
            Text(
                text = "Create Program",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        // Step progress indicator
        WizardProgressBar(
            currentStep = state.currentStep.index,
            totalSteps = WizardStep.values().size
        )

        // Step label
        Text(
            text = "Step ${state.currentStep.index + 1} of ${WizardStep.values().size}: ${state.currentStep.title}",
            style = MaterialTheme.typography.labelMedium,
            color = AmakaColors.textSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.sm.dp),
            textAlign = TextAlign.Center
        )

        HorizontalDivider(color = AmakaColors.borderLight)

        // Step content
        Box(modifier = Modifier.weight(1f)) {
            when (state.currentStep) {
                WizardStep.GOAL -> GoalStep(state, viewModel)
                WizardStep.EXPERIENCE -> ExperienceStep(state, viewModel)
                WizardStep.SCHEDULE -> ScheduleStep(state, viewModel)
                WizardStep.EQUIPMENT -> EquipmentStep(state, viewModel)
                WizardStep.PREFERENCES -> PreferencesStep(state, viewModel)
                WizardStep.REVIEW -> ReviewStep(state, viewModel)
            }
        }

        // Error snackbar
        state.error?.let { error ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AmakaSpacing.md.dp),
                color = AmakaColors.accentRed.copy(alpha = 0.15f),
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.accentRed,
                    modifier = Modifier.padding(AmakaSpacing.md.dp)
                )
            }
        }

        // Bottom navigation buttons
        if (state.currentStep != WizardStep.REVIEW) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AmakaSpacing.md.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { viewModel.goNext() },
                    enabled = viewModel.canGoNext,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmakaColors.accentBlue,
                        disabledContainerColor = AmakaColors.borderMedium
                    )
                ) {
                    Text("Next")
                }
            }
        }
    }
}

@Composable
private fun WizardProgressBar(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.sm.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (index <= currentStep) AmakaColors.accentBlue
                        else AmakaColors.borderMedium
                    )
            )
        }
    }
}

// =====================================================================
// Step 1: Goal
// =====================================================================

@Composable
private fun GoalStep(state: ProgramWizardState, viewModel: ProgramWizardViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AmakaSpacing.md.dp),
        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
    ) {
        item {
            Text(
                text = "What's your primary goal?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.padding(bottom = AmakaSpacing.sm.dp)
            )
        }

        items(ProgramGoal.values()) { goal ->
            SelectionChip(
                label = goal.label,
                selected = state.goal == goal,
                onClick = { viewModel.setGoal(goal) }
            )
        }

        item { Spacer(modifier = Modifier.height(AmakaSpacing.md.dp)) }

        item {
            Text(
                text = "Duration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)) {
                items(listOf(4, 6, 8, 12, 16)) { weeks ->
                    SelectionChipSmall(
                        label = "${weeks}W",
                        selected = state.durationWeeks == weeks,
                        onClick = { viewModel.setDurationWeeks(weeks) }
                    )
                }
            }
        }
    }
}

// =====================================================================
// Step 2: Experience
// =====================================================================

@Composable
private fun ExperienceStep(state: ProgramWizardState, viewModel: ProgramWizardViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AmakaSpacing.md.dp),
        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
    ) {
        item {
            Text(
                text = "Your experience level?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.padding(bottom = AmakaSpacing.sm.dp)
            )
        }

        item {
            ExperienceCard(
                level = ExperienceLevel.BEGINNER,
                description = "0-1 year of consistent training. Focus on learning proper form and building base fitness.",
                selected = state.experienceLevel == ExperienceLevel.BEGINNER,
                onClick = { viewModel.setExperienceLevel(ExperienceLevel.BEGINNER) }
            )
        }

        item {
            ExperienceCard(
                level = ExperienceLevel.INTERMEDIATE,
                description = "1-3 years of training. Comfortable with major lifts and ready for more complex programming.",
                selected = state.experienceLevel == ExperienceLevel.INTERMEDIATE,
                onClick = { viewModel.setExperienceLevel(ExperienceLevel.INTERMEDIATE) }
            )
        }

        item {
            ExperienceCard(
                level = ExperienceLevel.ADVANCED,
                description = "3+ years of consistent training. Looking for advanced periodization and specialized programming.",
                selected = state.experienceLevel == ExperienceLevel.ADVANCED,
                onClick = { viewModel.setExperienceLevel(ExperienceLevel.ADVANCED) }
            )
        }
    }
}

@Composable
private fun ExperienceCard(
    level: ExperienceLevel,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) AmakaColors.accentBlue else AmakaColors.borderLight,
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
            ),
        color = if (selected) AmakaColors.accentBlue.copy(alpha = 0.1f) else AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Row(
            modifier = Modifier.padding(AmakaSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = level.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) AmakaColors.accentBlue else AmakaColors.textPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = AmakaColors.accentBlue,
                    modifier = Modifier
                        .padding(start = AmakaSpacing.sm.dp)
                        .size(20.dp)
                )
            }
        }
    }
}

// =====================================================================
// Step 3: Schedule
// =====================================================================

@Composable
private fun ScheduleStep(state: ProgramWizardState, viewModel: ProgramWizardViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AmakaSpacing.md.dp),
        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
    ) {
        item {
            Text(
                text = "Training schedule",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary
            )
        }

        item {
            Text(
                text = "Sessions per week",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)) {
                items((2..6).toList()) { sessions ->
                    SelectionChipSmall(
                        label = "$sessions",
                        selected = state.sessionsPerWeek == sessions,
                        onClick = { viewModel.setSessionsPerWeek(sessions) }
                    )
                }
            }
        }

        item {
            Text(
                text = "Preferred days",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)) {
                DAY_LABELS.zip(DAY_NUMBERS).forEach { (label, dayNum) ->
                    val selected = dayNum in state.preferredDays
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                if (selected) AmakaColors.accentBlue
                                else AmakaColors.surface
                            )
                            .border(
                                1.dp,
                                if (selected) AmakaColors.accentBlue else AmakaColors.borderLight,
                                RoundedCornerShape(22.dp)
                            )
                            .clickable { viewModel.togglePreferredDay(dayNum) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) Color.White else AmakaColors.textSecondary
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Time per session",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)) {
                items(listOf(30, 45, 60, 75, 90)) { minutes ->
                    SelectionChipSmall(
                        label = "${minutes}m",
                        selected = state.timePerSession == minutes,
                        onClick = { viewModel.setTimePerSession(minutes) }
                    )
                }
            }
        }
    }
}

// =====================================================================
// Step 4: Equipment
// =====================================================================

@Composable
private fun EquipmentStep(state: ProgramWizardState, viewModel: ProgramWizardViewModel) {
    val customEquipmentOptions = listOf(
        "barbell", "dumbbell", "cable_machine", "pull_up_bar",
        "bench", "kettlebell", "resistance_band", "bodyweight"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AmakaSpacing.md.dp),
        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
    ) {
        item {
            Text(
                text = "Available equipment",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.padding(bottom = AmakaSpacing.sm.dp)
            )
        }

        items(EquipmentPreset.values().filter { it != EquipmentPreset.CUSTOM }) { preset ->
            SelectionChip(
                label = preset.label,
                selected = state.equipmentPreset == preset,
                onClick = { viewModel.setEquipmentPreset(preset) }
            )
        }

        item {
            SelectionChip(
                label = "Custom",
                selected = state.equipmentPreset == EquipmentPreset.CUSTOM,
                onClick = { viewModel.setEquipmentPreset(EquipmentPreset.CUSTOM) }
            )
        }

        if (state.equipmentPreset == EquipmentPreset.CUSTOM) {
            item {
                Text(
                    text = "Select equipment:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmakaColors.textSecondary,
                    modifier = Modifier.padding(top = AmakaSpacing.sm.dp)
                )
            }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp),
                    verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
                ) {
                    customEquipmentOptions.forEach { item ->
                        SelectionChipSmall(
                            label = item.replace("_", " ").replaceFirstChar { it.uppercase() },
                            selected = item in state.customEquipment,
                            onClick = { viewModel.toggleCustomEquipment(item) }
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================
// Step 5: Preferences
// =====================================================================

@Composable
private fun PreferencesStep(state: ProgramWizardState, viewModel: ProgramWizardViewModel) {
    val focusAreaOptions = listOf("chest", "back", "shoulders", "arms", "legs", "core", "glutes")
    val avoidOptions = listOf("deadlift", "squat", "bench press", "overhead press", "pull-up", "running")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AmakaSpacing.md.dp),
        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
    ) {
        item {
            Text(
                text = "Preferences & Limitations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary
            )
        }

        item {
            Text(
                text = "Injuries or limitations (optional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
        }

        item {
            OutlinedTextField(
                value = state.injuries,
                onValueChange = { viewModel.setInjuries(it) },
                placeholder = {
                    Text(
                        "e.g. lower back pain, knee issues",
                        color = AmakaColors.textTertiary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmakaColors.accentBlue,
                    unfocusedBorderColor = AmakaColors.borderLight,
                    focusedTextColor = AmakaColors.textPrimary,
                    unfocusedTextColor = AmakaColors.textPrimary
                )
            )
        }

        item {
            Text(
                text = "Focus areas (optional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp),
                verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
            ) {
                focusAreaOptions.forEach { area ->
                    SelectionChipSmall(
                        label = area.replaceFirstChar { it.uppercase() },
                        selected = area in state.focusAreas,
                        onClick = { viewModel.toggleFocusArea(area) }
                    )
                }
            }
        }

        item {
            Text(
                text = "Exercises to avoid (optional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp),
                verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
            ) {
                avoidOptions.forEach { exercise ->
                    SelectionChipSmall(
                        label = exercise.replaceFirstChar { it.uppercase() },
                        selected = exercise in state.avoidExercises,
                        onClick = { viewModel.toggleAvoidExercise(exercise) }
                    )
                }
            }
        }
    }
}

// =====================================================================
// Step 6: Review / Generate
// =====================================================================

@Composable
private fun ReviewStep(state: ProgramWizardState, viewModel: ProgramWizardViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AmakaSpacing.md.dp),
        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
    ) {
        item {
            Text(
                text = "Review your program",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary
            )
        }

        item {
            ReviewSummaryCard {
                ReviewRow("Goal", state.goal?.label ?: "—")
                ReviewRow("Duration", "${state.durationWeeks} weeks")
                ReviewRow("Experience", state.experienceLevel?.label ?: "—")
                ReviewRow("Sessions/week", "${state.sessionsPerWeek}")
                ReviewRow("Time/session", "${state.timePerSession} min")
                val days = state.preferredDays.joinToString(", ") { dayNum ->
                    DAY_LABELS.getOrNull(dayNum - 1) ?: dayNum.toString()
                }
                ReviewRow("Preferred days", days.ifEmpty { "Any" })
                val equipment = if (state.equipmentPreset == EquipmentPreset.CUSTOM) {
                    state.customEquipment.joinToString(", ").ifEmpty { "None selected" }
                } else {
                    state.equipmentPreset?.label ?: "—"
                }
                ReviewRow("Equipment", equipment)
                if (state.injuries.isNotBlank()) ReviewRow("Limitations", state.injuries)
                if (state.focusAreas.isNotEmpty()) ReviewRow("Focus areas", state.focusAreas.joinToString(", "))
                if (state.avoidExercises.isNotEmpty()) ReviewRow("Avoid exercises", state.avoidExercises.joinToString(", "))
            }
        }

        if (state.isGenerating) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
                ) {
                    CircularProgressIndicator(color = AmakaColors.accentBlue)
                    Text(
                        text = "Generating your program...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmakaColors.textSecondary
                    )
                    if (state.generationProgress > 0) {
                        LinearProgressIndicator(
                            progress = { state.generationProgress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = AmakaColors.accentBlue,
                            trackColor = AmakaColors.borderLight
                        )
                        Text(
                            text = "${state.generationProgress}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmakaColors.textSecondary
                        )
                    }
                }
            }
        } else {
            item {
                Button(
                    onClick = { viewModel.generateProgram() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("generate_program_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = AmakaColors.accentBlue),
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                ) {
                    Text(
                        text = "Generate Program",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewSummaryCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(modifier = Modifier.padding(AmakaSpacing.md.dp), content = content)
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AmakaColors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = AmakaColors.textPrimary
        )
    }
}

// =====================================================================
// Shared chip components
// =====================================================================

@Composable
private fun SelectionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) AmakaColors.accentBlue else AmakaColors.borderLight,
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
            ),
        color = if (selected) AmakaColors.accentBlue.copy(alpha = 0.1f) else AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AmakaSpacing.md.dp,
                vertical = AmakaSpacing.md.dp
            ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) AmakaColors.accentBlue else AmakaColors.textPrimary
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = AmakaColors.accentBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectionChipSmall(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) AmakaColors.accentBlue else AmakaColors.borderLight,
                shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
            ),
        color = if (selected) AmakaColors.accentBlue else AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else AmakaColors.textSecondary,
            modifier = Modifier.padding(
                horizontal = AmakaSpacing.md.dp,
                vertical = AmakaSpacing.sm.dp
            )
        )
    }
}
