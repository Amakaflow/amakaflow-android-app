package com.amakaflow.companion.ui.screens.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

/**
 * AMA-1291: Water logging card with cup visual.
 * +250ml per tap, target 2.5L. Writes to Health Connect.
 */
@Composable
fun WaterTrackerCard(
    currentMl: Double,
    targetMl: Int,
    onAddWater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cupsConsumed = (currentMl / 250.0).toInt()
    val totalCups = (targetMl / 250.0).toInt()
    val progress = (currentMl / targetMl.toDouble()).coerceIn(0.0, 1.0).toFloat()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("water_tracker_card"),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(
            modifier = Modifier.padding(AmakaSpacing.md.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.WaterDrop,
                        contentDescription = null,
                        tint = AmakaColors.accentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AmakaSpacing.xs.dp))
                    Text(
                        text = "Water",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AmakaColors.textPrimary
                    )
                }
                Text(
                    text = "${formatMl(currentMl)} / ${formatMl(targetMl.toDouble())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmakaColors.textSecondary,
                    modifier = Modifier.testTag("water_progress_text")
                )
            }

            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .testTag("water_progress_bar"),
                color = AmakaColors.accentBlue,
                trackColor = AmakaColors.surfaceElevated,
                drawStopIndicator = {}
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

            // Cup visual — row of water drop icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("water_cups_row"),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (i in 0 until totalCups.coerceAtMost(10)) {
                    Icon(
                        imageVector = Icons.Filled.WaterDrop,
                        contentDescription = null,
                        tint = if (i < cupsConsumed) AmakaColors.accentBlue
                               else AmakaColors.surfaceElevated,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

            // Add water button
            Button(
                onClick = onAddWater,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("water_add_button"),
                shape = RoundedCornerShape(AmakaCornerRadius.sm.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmakaColors.accentBlue.copy(alpha = 0.15f),
                    contentColor = AmakaColors.accentBlue
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.WaterDrop,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(AmakaSpacing.xs.dp))
                Text(
                    text = "+ 250ml",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatMl(ml: Double): String {
    return if (ml >= 1000) {
        "${String.format("%.1f", ml / 1000.0)}L"
    } else {
        "${ml.toInt()}ml"
    }
}
