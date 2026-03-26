package com.amakaflow.companion.ui.screens.xp

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

/**
 * XP progress bar showing current level name + XP count.
 * AMA-1285
 */
@Composable
fun XPBarComponent(
    xpTotal: Int,
    currentLevel: Int,
    levelName: String,
    xpToNextLevel: Int,
    xpToday: Int = 0,
    dailyCap: Int = 300,
    modifier: Modifier = Modifier,
) {
    val thresholds = listOf(0, 500, 1500, 3500, 7000, 12000, 20000, 35000, 55000, 80000)
    val idx = (currentLevel - 1).coerceIn(0, thresholds.lastIndex)
    val currentThreshold = thresholds[idx]

    val levelProgress = if (currentLevel >= thresholds.size) {
        1f
    } else {
        val nextThreshold = thresholds[(idx + 1).coerceAtMost(thresholds.lastIndex)]
        val range = nextThreshold - currentThreshold
        if (range > 0) ((xpTotal - currentThreshold).toFloat() / range).coerceIn(0f, 1f) else 1f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = levelProgress,
        animationSpec = tween(durationMillis = 500),
        label = "xp_progress"
    )

    val levelColor = when (currentLevel) {
        in 1..2 -> AmakaColors.accentGreen
        in 3..4 -> AmakaColors.accentBlue
        in 5..6 -> Color(0xFFFFA500)
        in 7..8 -> Color(0xFF9333EA)
        in 9..10 -> Color(0xFFFFD700)
        else -> AmakaColors.accentBlue
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("xp_bar"),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(
            modifier = Modifier.padding(AmakaSpacing.md.dp)
        ) {
            // Level label + XP count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.xs.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = levelColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Lv.$currentLevel $levelName",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AmakaColors.textPrimary
                    )
                }

                Text(
                    text = "$xpTotal XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmakaColors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AmakaColors.background)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(levelColor, levelColor.copy(alpha = 0.7f))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))

            // XP to next level
            Text(
                text = if (xpToNextLevel > 0) "$xpToNextLevel XP to next level" else "Max level reached!",
                style = MaterialTheme.typography.labelSmall,
                color = if (xpToNextLevel > 0) AmakaColors.textTertiary else levelColor,
                fontSize = 12.sp
            )
        }
    }
}
