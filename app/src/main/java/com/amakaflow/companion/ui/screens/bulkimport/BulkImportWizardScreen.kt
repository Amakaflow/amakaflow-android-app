package com.amakaflow.companion.ui.screens.bulkimport

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.DetectedItem
import com.amakaflow.companion.data.model.ExerciseMatch
import com.amakaflow.companion.data.model.PreviewWorkout
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

@Composable
fun BulkImportWizardScreen(
    onNavigateBack: () -> Unit,
    onImportComplete: () -> Unit = {},
    viewModel: BulkImportViewModel = hiltViewModel()
) {
    val state = viewModel.state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("bulk_import_screen")
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.sm.dp, vertical = AmakaSpacing.sm.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (state.currentStep == BulkImportStep.SOURCE_SELECTION) onNavigateBack()
                else viewModel.goBack()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AmakaColors.textPrimary
                )
            }
            Text(
                text = "Bulk Import",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        // Progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.sm.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BulkImportStep.values().forEach { step ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (step.index <= state.currentStep.index) AmakaColors.accentBlue
                            else AmakaColors.borderMedium,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }

        // Step label
        Text(
            text = "Step ${state.currentStep.index + 1}: ${state.currentStep.title}",
            style = MaterialTheme.typography.labelMedium,
            color = AmakaColors.textSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.xs.dp),
            textAlign = TextAlign.Center
        )

        HorizontalDivider(color = AmakaColors.borderLight)

        // Error banner
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

        // Step content
        Box(modifier = Modifier.weight(1f)) {
            when (state.currentStep) {
                BulkImportStep.SOURCE_SELECTION -> SourceSelectionStep(state, viewModel)
                BulkImportStep.DETECTION -> DetectionStep(state, viewModel)
                BulkImportStep.EXERCISE_MATCHING -> ExerciseMatchingStep(state, viewModel)
                BulkImportStep.PREVIEW -> PreviewStep(state, viewModel)
                BulkImportStep.IMPORT -> ImportProgressStep(state, onImportComplete)
            }
        }
    }
}

// =====================================================================
// Step 1: Source Selection
// =====================================================================

@Composable
private fun SourceSelectionStep(state: BulkImportState, viewModel: BulkImportViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(AmakaSpacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
        ) {
            item {
                Text(
                    text = "Add workout URLs to import",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary,
                    modifier = Modifier.padding(bottom = AmakaSpacing.sm.dp)
                )
                Text(
                    text = "Supported: Strava, Garmin Connect, TrainingPeaks, and more",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary,
                    modifier = Modifier.padding(bottom = AmakaSpacing.md.dp)
                )
            }

            itemsIndexed(state.urls) { index, url ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
                ) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { viewModel.updateUrl(index, it) },
                        placeholder = {
                            Text(
                                "https://...",
                                color = AmakaColors.textTertiary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmakaColors.accentBlue,
                            unfocusedBorderColor = AmakaColors.borderLight,
                            focusedTextColor = AmakaColors.textPrimary,
                            unfocusedTextColor = AmakaColors.textPrimary
                        )
                    )
                    if (state.urls.size > 1) {
                        IconButton(onClick = { viewModel.removeUrl(index) }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove",
                                tint = AmakaColors.textSecondary
                            )
                        }
                    }
                }
            }

            item {
                TextButton(
                    onClick = { viewModel.addUrl() },
                    colors = ButtonDefaults.textButtonColors(contentColor = AmakaColors.accentBlue)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add URL")
                }
            }
        }

        // Action button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AmakaColors.accentBlue
                )
            } else {
                Button(
                    onClick = { viewModel.detectSources() },
                    enabled = viewModel.validUrls.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmakaColors.accentBlue,
                        disabledContainerColor = AmakaColors.borderMedium
                    )
                ) {
                    Text("Detect Workouts")
                }
            }
        }
    }
}

// =====================================================================
// Step 2: Detection Results
// =====================================================================

@Composable
private fun DetectionStep(state: BulkImportState, viewModel: BulkImportViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(AmakaSpacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
        ) {
            item {
                Text(
                    text = "Detected ${state.detectedItems.size} workout(s)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary,
                    modifier = Modifier.padding(bottom = AmakaSpacing.sm.dp)
                )
            }

            items(state.detectedItems) { item ->
                DetectedItemCard(
                    item = item,
                    selected = item.id in state.selectedItemIds,
                    onToggle = { viewModel.toggleItemSelection(item.id) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AmakaColors.accentBlue
                )
            } else {
                Button(
                    onClick = { viewModel.proceedToMatching() },
                    enabled = state.selectedItemIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmakaColors.accentBlue,
                        disabledContainerColor = AmakaColors.borderMedium
                    )
                ) {
                    Text("Match Exercises (${state.selectedItemIds.size} selected)")
                }
            }
        }
    }
}

@Composable
private fun DetectedItemCard(item: DetectedItem, selected: Boolean, onToggle: () -> Unit) {
    val isError = item.status == "error"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isError) Modifier.clickable(onClick = onToggle) else Modifier)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = when {
                    isError -> AmakaColors.accentRed
                    selected -> AmakaColors.accentBlue
                    else -> AmakaColors.borderLight
                },
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
            ),
        color = when {
            isError -> AmakaColors.accentRed.copy(alpha = 0.05f)
            selected -> AmakaColors.accentBlue.copy(alpha = 0.1f)
            else -> AmakaColors.surface
        },
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Row(
            modifier = Modifier.padding(AmakaSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.workoutName ?: item.platform,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = AmakaColors.textPrimary
                )
                Text(
                    text = item.url.take(50) + if (item.url.length > 50) "…" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary
                )
                if (item.error != null) {
                    Text(
                        text = item.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.accentRed
                    )
                }
            }
            Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
            // Confidence badge
            if (!isError) {
                ConfidenceBadge(confidence = item.confidence)
            }
            // Checkbox
            if (!isError) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AmakaColors.accentBlue,
                        uncheckedColor = AmakaColors.borderMedium
                    )
                )
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(confidence: Double) {
    val color = when {
        confidence >= 0.8 -> AmakaColors.accentGreen
        confidence >= 0.5 -> AmakaColors.accentYellow
        else -> AmakaColors.accentOrange
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
    ) {
        Text(
            text = "${(confidence * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = AmakaSpacing.sm.dp, vertical = 4.dp)
        )
    }
}

// =====================================================================
// Step 3: Exercise Matching
// =====================================================================

@Composable
private fun ExerciseMatchingStep(state: BulkImportState, viewModel: BulkImportViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(AmakaSpacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
        ) {
            item {
                Text(
                    text = "Review exercise matches",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary,
                    modifier = Modifier.padding(bottom = AmakaSpacing.sm.dp)
                )
            }

            if (state.exerciseMatches.isEmpty()) {
                item {
                    Text(
                        text = "All exercises matched automatically",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmakaColors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AmakaSpacing.lg.dp)
                    )
                }
            } else {
                items(state.exerciseMatches) { match ->
                    ExerciseMatchCard(
                        match = match,
                        resolvedName = state.resolvedMatches[match.id] ?: match.matchedName ?: match.sourceName,
                        onUpdateMatch = { chosen -> viewModel.updateMatch(match.id, chosen) }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AmakaColors.accentBlue
                )
            } else {
                Button(
                    onClick = { viewModel.proceedToPreview() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AmakaColors.accentBlue)
                ) {
                    Text("Preview Import")
                }
            }
        }
    }
}

@Composable
private fun ExerciseMatchCard(
    match: ExerciseMatch,
    resolvedName: String,
    onUpdateMatch: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val matchColor = when {
        match.matchScore >= 0.8 -> AmakaColors.accentGreen
        match.matchScore >= 0.5 -> AmakaColors.accentYellow
        else -> AmakaColors.accentOrange
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(modifier = Modifier.padding(AmakaSpacing.md.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = match.sourceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textSecondary
                    )
                    Text(
                        text = resolvedName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = AmakaColors.textPrimary
                    )
                }
                Surface(
                    color = matchColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
                ) {
                    Text(
                        text = "${(match.matchScore * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = matchColor,
                        modifier = Modifier.padding(horizontal = AmakaSpacing.sm.dp, vertical = 4.dp)
                    )
                }
            }

            if (match.suggestions.isNotEmpty()) {
                TextButton(
                    onClick = { expanded = !expanded },
                    colors = ButtonDefaults.textButtonColors(contentColor = AmakaColors.accentBlue)
                ) {
                    Text(
                        text = if (expanded) "Hide suggestions" else "Change match",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (expanded) {
                    match.suggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUpdateMatch(suggestion); expanded = false }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (suggestion == resolvedName) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = AmakaColors.accentBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (suggestion == resolvedName) AmakaColors.accentBlue
                                        else AmakaColors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// Step 4: Preview
// =====================================================================

@Composable
private fun PreviewStep(state: BulkImportState, viewModel: BulkImportViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(AmakaSpacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
        ) {
            item {
                Text(
                    text = "Select workouts to import",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary,
                    modifier = Modifier.padding(bottom = AmakaSpacing.sm.dp)
                )
            }

            items(state.previewWorkouts) { workout ->
                PreviewWorkoutCard(
                    workout = workout,
                    selected = workout.id in state.selectedWorkoutIds,
                    onToggle = { viewModel.toggleWorkoutSelection(workout.id) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp)
        ) {
            Button(
                onClick = { viewModel.executeImport() },
                enabled = state.selectedWorkoutIds.isNotEmpty() && !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmakaColors.accentBlue,
                    disabledContainerColor = AmakaColors.borderMedium
                )
            ) {
                Text("Import ${state.selectedWorkoutIds.size} Workout(s)")
            }
        }
    }
}

@Composable
private fun PreviewWorkoutCard(workout: PreviewWorkout, selected: Boolean, onToggle: () -> Unit) {
    val canSelect = workout.valid
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (canSelect) Modifier.clickable(onClick = onToggle) else Modifier)
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
                    text = workout.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = AmakaColors.textPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
                ) {
                    workout.date?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = AmakaColors.textSecondary
                        )
                    }
                    if (workout.durationSeconds > 0) {
                        Text(
                            text = formatDuration(workout.durationSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = AmakaColors.textSecondary
                        )
                    }
                    if (workout.exerciseCount > 0) {
                        Text(
                            text = "${workout.exerciseCount} exercises",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmakaColors.textSecondary
                        )
                    }
                }
                if (!workout.valid && workout.validationErrors.isNotEmpty()) {
                    Text(
                        text = workout.validationErrors.first(),
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.accentOrange
                    )
                }
            }
            if (canSelect) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AmakaColors.accentBlue,
                        uncheckedColor = AmakaColors.borderMedium
                    )
                )
            } else {
                Surface(
                    color = AmakaColors.accentOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
                ) {
                    Text(
                        text = "Invalid",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmakaColors.accentOrange,
                        modifier = Modifier.padding(horizontal = AmakaSpacing.sm.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// =====================================================================
// Step 5: Import Progress
// =====================================================================

@Composable
private fun ImportProgressStep(state: BulkImportState, onImportComplete: () -> Unit = {}) {
    val importStatus = state.importStatus

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AmakaSpacing.md.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
    ) {
        Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))

        when {
            state.error != null && (importStatus == null || (importStatus.status != "completed" && importStatus.status != "failed")) -> {
                // Polling failed or timed out — show error instead of a stale spinner
                Text(
                    text = "Import Error",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AmakaColors.accentRed
                )
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmakaColors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
            importStatus == null || (importStatus.status != "completed" && importStatus.status != "failed") -> {
                CircularProgressIndicator(color = AmakaColors.accentBlue)
                Text(
                    text = "Importing workouts...",
                    style = MaterialTheme.typography.titleSmall,
                    color = AmakaColors.textPrimary
                )
                importStatus?.let { status ->
                    if (status.total > 0) {
                        LinearProgressIndicator(
                            progress = { status.completed.toFloat() / status.total },
                            modifier = Modifier.fillMaxWidth(),
                            color = AmakaColors.accentBlue,
                            trackColor = AmakaColors.borderLight
                        )
                        Text(
                            text = "${status.completed} / ${status.total}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmakaColors.textSecondary
                        )
                    }
                }
            }
            importStatus.status == "completed" -> {
                val stats = importStatus.stats
                Text(
                    text = "Import Complete!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AmakaColors.accentGreen
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AmakaColors.surface,
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                ) {
                    Column(modifier = Modifier.padding(AmakaSpacing.md.dp)) {
                        if (stats != null) {
                            ImportStatRow("Imported", "${stats.imported}", AmakaColors.accentGreen)
                            ImportStatRow("Skipped", "${stats.skipped}", AmakaColors.accentYellow)
                            ImportStatRow("Failed", "${stats.failed}", AmakaColors.accentRed)
                        } else {
                            Text(
                                text = "Successfully imported ${importStatus.completed} workout(s)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmakaColors.textPrimary
                            )
                        }
                    }
                }
                TextButton(onClick = onImportComplete) {
                    Text("Done", color = AmakaColors.accentBlue)
                }
            }
            importStatus.status == "failed" -> {
                Text(
                    text = "Import Failed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AmakaColors.accentRed
                )
                Text(
                    text = importStatus.error ?: "Unknown error occurred",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmakaColors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ImportStatRow(label: String, value: String, color: Color) {
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
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
