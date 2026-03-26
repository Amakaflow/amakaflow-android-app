package com.amakaflow.companion.ui.screens.prhistory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.domain.usecase.pr.PersonalRecord
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

private val Gold = Color(0xFFFFD700)
private val AmakaFlowPurple = Color(0xFF6C5CE7)

/**
 * PR history list screen showing all personal records grouped by exercise.
 * AMA-1282
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PRHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: PRHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("pr_history_screen")
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
                text = "Personal Records",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
        }

        if (uiState.groupedPRs.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = AmakaColors.textTertiary
                    )
                    Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
                    Text(
                        text = "No Personal Records Yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = AmakaColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                    Text(
                        text = "Complete workouts with weight tracking\nto start setting PRs.",
                        fontSize = 15.sp,
                        color = AmakaColors.textSecondary,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = AmakaSpacing.xl.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(AmakaSpacing.md.dp),
                verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
            ) {
                items(uiState.groupedPRs) { (exerciseName, records) ->
                    ExerciseSection(exerciseName = exerciseName, records = records)
                }
            }
        }
    }
}

@Composable
private fun ExerciseSection(exerciseName: String, records: List<PersonalRecord>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AmakaColors.surface, RoundedCornerShape(AmakaCornerRadius.md.dp))
            .border(1.dp, AmakaColors.borderLight, RoundedCornerShape(AmakaCornerRadius.md.dp))
            .padding(AmakaSpacing.md.dp)
    ) {
        // Exercise header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AmakaFlowPurple
            )
            Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

        records.forEach { record ->
            PRRow(record = record)
            if (record != records.last()) {
                Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))
            }
        }
    }
}

@Composable
private fun PRRow(record: PersonalRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AmakaSpacing.xs.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = record.typeLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AmakaColors.textSecondary
            )
            Text(
                text = record.formattedValue,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Gold
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = record.dateIso.take(10), // ISO date portion
                fontSize = 13.sp,
                color = AmakaColors.textTertiary
            )
            record.workoutName?.let { name ->
                Text(
                    text = name,
                    fontSize = 12.sp,
                    color = AmakaColors.textTertiary,
                    maxLines = 1
                )
            }
        }
    }
}
