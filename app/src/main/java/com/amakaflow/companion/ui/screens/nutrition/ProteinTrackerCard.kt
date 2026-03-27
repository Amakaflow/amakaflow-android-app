package com.amakaflow.companion.ui.screens.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * AMA-1291: Quick protein logging card with progress bar and quick-add buttons.
 * Target based on bodyweight (default 120g). Writes to Health Connect.
 */
@Composable
fun ProteinTrackerCard(
    currentGrams: Double,
    targetGrams: Int,
    onAddProtein: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (currentGrams / targetGrams.toDouble()).coerceIn(0.0, 1.0).toFloat()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("protein_tracker_card"),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(
            modifier = Modifier.padding(AmakaSpacing.md.dp)
        ) {
            // Header with progress text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Protein",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
                Text(
                    text = "${currentGrams.toInt()}g / ${targetGrams}g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmakaColors.textSecondary,
                    modifier = Modifier.testTag("protein_progress_text")
                )
            }

            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .testTag("protein_progress_bar"),
                color = AmakaColors.accentPurple,
                trackColor = AmakaColors.surfaceElevated,
                drawStopIndicator = {}
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

            // Quick-add buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
            ) {
                QuickAddButton(
                    label = "+20g",
                    onClick = { onAddProtein(20) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("protein_add_20")
                )
                QuickAddButton(
                    label = "+30g",
                    onClick = { onAddProtein(30) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("protein_add_30")
                )
                QuickAddButton(
                    label = "+40g",
                    onClick = { onAddProtein(40) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("protein_add_40")
                )
            }
        }
    }
}

@Composable
private fun QuickAddButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(AmakaCornerRadius.sm.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AmakaColors.accentPurple
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
