package com.amakaflow.companion.ui.screens.fatigue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.DayState
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

@Composable
fun FatigueHistoryScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: FatigueHistoryViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("fatigue_history_screen")
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
                text = "Readiness History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        // Range picker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FatigueHistoryViewModel.DateRange.values().forEach { range ->
                val selected = viewModel.selectedRange == range
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 4.dp),
                    color = if (selected) AmakaColors.accentBlue else AmakaColors.surface,
                    shape = RoundedCornerShape(AmakaCornerRadius.sm.dp),
                    onClick = { viewModel.changeRange(range) }
                ) {
                    Text(
                        text = range.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color.White else AmakaColors.textSecondary,
                        modifier = Modifier.padding(
                            horizontal = AmakaSpacing.md.dp,
                            vertical = AmakaSpacing.sm.dp
                        )
                    )
                }
            }
        }

        when {
            viewModel.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AmakaColors.accentBlue)
                }
            }
            viewModel.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = viewModel.error ?: "Unknown error",
                            color = AmakaColors.accentRed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(AmakaSpacing.md.dp)
                        )
                        Button(
                            onClick = { viewModel.loadHistory() },
                            colors = ButtonDefaults.buttonColors(containerColor = AmakaColors.accentBlue)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                // Stats summary row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AmakaSpacing.md.dp)
                        .padding(bottom = AmakaSpacing.md.dp),
                    horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
                ) {
                    StatCard(
                        label = "Avg",
                        value = if (viewModel.dayStates.isEmpty()) "—"
                                else String.format("%.0f", viewModel.averageScore),
                        color = AmakaColors.accentBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Green",
                        value = viewModel.greenDays.toString(),
                        color = AmakaColors.accentGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Yellow",
                        value = viewModel.yellowDays.toString(),
                        color = AmakaColors.accentYellow,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Red",
                        value = viewModel.redDays.toString(),
                        color = AmakaColors.accentRed,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (viewModel.dayStates.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No readiness data for this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = AmakaSpacing.md.dp,
                            vertical = AmakaSpacing.sm.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
                    ) {
                        items(viewModel.dayStates) { dayState ->
                            DayReadinessRow(dayState = dayState)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(
            modifier = Modifier.padding(AmakaSpacing.sm.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = AmakaColors.textSecondary
            )
        }
    }
}

@Composable
private fun DayReadinessRow(dayState: DayState) {
    val readiness = dayState.readinessScore
    val badgeColor = when {
        readiness == null -> AmakaColors.textTertiary
        readiness >= 70.0 -> AmakaColors.accentGreen
        readiness >= 40.0 -> AmakaColors.accentYellow
        else -> AmakaColors.accentRed
    }
    val readinessLabel = when {
        readiness == null -> "No data"
        readiness >= 70.0 -> "High"
        readiness >= 40.0 -> "Moderate"
        else -> "Low"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("day_readiness_row"),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Row(
            modifier = Modifier.padding(AmakaSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDate(dayState.date),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = AmakaColors.textPrimary
                )
                Text(
                    text = dayState.status.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
            ) {
                // Readiness badge
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
                ) {
                    Text(
                        text = readinessLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor,
                        modifier = Modifier.padding(
                            horizontal = AmakaSpacing.sm.dp,
                            vertical = 4.dp
                        )
                    )
                }

                // Score
                Text(
                    text = readiness?.let { String.format("%.0f", it) } ?: "—",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val month = when (parts[1].toInt()) {
                1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
                5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
                9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Dec"
            }
            "$month ${parts[2]}, ${parts[0]}"
        } else dateStr
    } catch (e: Exception) {
        dateStr
    }
}
