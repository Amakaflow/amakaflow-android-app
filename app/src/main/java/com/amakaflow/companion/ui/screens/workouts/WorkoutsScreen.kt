package com.amakaflow.companion.ui.screens.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.Workout
import com.amakaflow.companion.data.model.WorkoutSport
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    onNavigateToWorkoutDetail: (String) -> Unit,
    viewModel: WorkoutsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = AmakaColors.background,
        bottomBar = {
            // Add Sample Workout button at bottom
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AmakaColors.background
            ) {
                Button(
                    onClick = { /* TODO: Add sample workout */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AmakaSpacing.md.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmakaColors.accentBlue
                    ),
                    shape = RoundedCornerShape(AmakaCornerRadius.lg.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                    Text(
                        text = "Add Sample Workout",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AmakaColors.background)
                .testTag("workouts_screen"),
            contentPadding = PaddingValues(horizontal = AmakaSpacing.md.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
                Text(
                    text = "Workouts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AmakaColors.textPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.search(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search workouts...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = AmakaColors.textSecondary
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AmakaColors.surface,
                        unfocusedContainerColor = AmakaColors.surface,
                        focusedBorderColor = AmakaColors.accentBlue,
                        unfocusedBorderColor = AmakaColors.borderLight,
                        focusedTextColor = AmakaColors.textPrimary,
                        unfocusedTextColor = AmakaColors.textPrimary,
                        cursorColor = AmakaColors.accentBlue
                    ),
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
            }

            // Upcoming Workouts section
            item {
                Text(
                    text = "Upcoming Workouts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            }

            // Upcoming workouts content
            if (uiState.workouts.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.CalendarMonth,
                        title = "No Upcoming Workouts",
                        subtitle = "Workouts you save will appear here"
                    )
                    Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
                }
            } else {
                itemsIndexed(uiState.workouts) { index, workout ->
                    WorkoutListItem(
                        workout = workout,
                        index = index,
                        onClick = { onNavigateToWorkoutDetail(workout.id) }
                    )
                    Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                }
                item {
                    Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
                }
            }

            // Incoming Workouts section
            item {
                Text(
                    text = "Incoming Workouts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            }

            item {
                EmptyStateCard(
                    icon = Icons.Filled.Download,
                    title = "No Incoming Workouts",
                    subtitle = "Workouts synced from the web will appear here"
                )
                Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.xl.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AmakaColors.textTertiary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AmakaColors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WorkoutListItem(
    workout: Workout,
    index: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AmakaCornerRadius.md.dp))
            .clickable(onClick = onClick)
            .testTag("workout_item_$index"),
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
                modifier = Modifier.size(48.dp),
                color = workout.sport.color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        tint = workout.sport.color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = AmakaColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = workout.sport.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = workout.sport.color
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textTertiary
                    )
                    Text(
                        text = workout.formattedDuration,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textTertiary
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textTertiary
                    )
                    Text(
                        text = "${workout.intervalCount} exercises",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textTertiary
                    )
                }
            }
        }
    }
}

// Extension to get sport color
private val WorkoutSport.color: androidx.compose.ui.graphics.Color
    get() = when (this) {
        WorkoutSport.RUNNING -> AmakaColors.sportRunning
        WorkoutSport.CYCLING -> AmakaColors.sportCycling
        WorkoutSport.STRENGTH -> AmakaColors.sportStrength
        WorkoutSport.MOBILITY -> AmakaColors.sportMobility
        WorkoutSport.SWIMMING -> AmakaColors.sportSwimming
        WorkoutSport.CARDIO -> AmakaColors.sportCardio
        WorkoutSport.OTHER -> AmakaColors.textSecondary
    }

private val WorkoutSport.displayName: String
    get() = when (this) {
        WorkoutSport.RUNNING -> "Running"
        WorkoutSport.CYCLING -> "Cycling"
        WorkoutSport.STRENGTH -> "Strength"
        WorkoutSport.MOBILITY -> "Mobility"
        WorkoutSport.SWIMMING -> "Swimming"
        WorkoutSport.CARDIO -> "Cardio"
        WorkoutSport.OTHER -> "Other"
    }
