package com.amakaflow.companion.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

/**
 * Apple Watch-style circular progress ring for weekly workout target (AMA-1286).
 *
 * Shows X/Y workouts completed this week with animated fill and
 * Zeigarnik-effect motivational text.
 */
@Composable
fun WeeklyProgressRing(
    workoutsCompleted: Int,
    weeklyTarget: Int,
    ringPercentage: Float,
    motivationalText: String,
    modifier: Modifier = Modifier,
) {
    val ringColor = Color(0xFF6C5CE7) // AmakaFlow purple
    val trackColor = Color.Gray.copy(alpha = 0.3f)
    val strokeWidth = 14.dp

    // Animate the progress
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(ringPercentage) {
        animatedProgress.animateTo(
            targetValue = ringPercentage,
            animationSpec = tween(durationMillis = 800)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_progress_ring"),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.lg.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                // Ring canvas
                Canvas(modifier = Modifier.size(120.dp)) {
                    val stroke = strokeWidth.toPx()
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset(stroke / 2, stroke / 2)
                    val arcSize = Size(diameter, diameter)

                    // Background track
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    // Progress arc
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress.value,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                // Center text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$workoutsCompleted/$weeklyTarget",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmakaColors.textPrimary
                    )
                    Text(
                        text = "workouts",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmakaColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

            // Motivational Zeigarnik text
            Text(
                text = motivationalText,
                style = MaterialTheme.typography.bodyMedium,
                color = AmakaColors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
private fun WeeklyProgressRingPreview() {
    WeeklyProgressRing(
        workoutsCompleted = 2,
        weeklyTarget = 3,
        ringPercentage = 0.67f,
        motivationalText = "2 of 3 \u2014 one more to go!"
    )
}
