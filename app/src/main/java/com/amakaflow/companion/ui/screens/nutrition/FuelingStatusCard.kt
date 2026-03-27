package com.amakaflow.companion.ui.screens.nutrition

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amakaflow.companion.data.nutrition.FuelingLevel
import com.amakaflow.companion.data.nutrition.FuelingStatusResponse
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

/**
 * AMA-1293: Fueling status card shown before a workout starts.
 * Displays a green/yellow/red traffic-light indicator with a coaching message
 * based on today's nutrition data.
 */
@Composable
fun FuelingStatusCard(
    fuelingStatus: FuelingStatusResponse,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (fuelingStatus.status) {
            FuelingLevel.GREEN -> AmakaColors.accentGreen
            FuelingLevel.YELLOW -> AmakaColors.accentYellow
            FuelingLevel.RED -> AmakaColors.accentRed
        },
        label = "fueling_status_color"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("fueling_status_card"),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(
            modifier = Modifier.padding(AmakaSpacing.md.dp)
        ) {
            // Header row with icon and status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                Text(
                    text = "Fueling Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                // Traffic light dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                        .testTag("fueling_status_indicator")
                )
            }

            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

            // Coaching message
            Text(
                text = fuelingStatus.message,
                style = MaterialTheme.typography.bodyMedium,
                color = AmakaColors.textSecondary,
                modifier = Modifier.testTag("fueling_status_message")
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

            // Progress bars for each metric
            FuelingMetricBar(
                label = "Calories",
                progress = fuelingStatus.caloriesPct.toFloat().coerceIn(0f, 1f),
                color = statusColor,
                testTag = "fueling_calories_bar"
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
            FuelingMetricBar(
                label = "Protein",
                progress = fuelingStatus.proteinPct.toFloat().coerceIn(0f, 1f),
                color = statusColor,
                testTag = "fueling_protein_bar"
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
            FuelingMetricBar(
                label = "Hydration",
                progress = fuelingStatus.hydrationPct.toFloat().coerceIn(0f, 1f),
                color = statusColor,
                testTag = "fueling_hydration_bar"
            )
        }
    }
}

@Composable
private fun FuelingMetricBar(
    label: String,
    progress: Float,
    color: androidx.compose.ui.graphics.Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = AmakaColors.textTertiary
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = AmakaColors.textTertiary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .testTag(testTag),
            color = color,
            trackColor = AmakaColors.surfaceElevated,
            drawStopIndicator = {}
        )
    }
}
