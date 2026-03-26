package com.amakaflow.companion.ui.screens.sharecard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amakaflow.companion.domain.usecase.pr.PRDetectionResult
import com.amakaflow.companion.domain.usecase.pr.PRType

private val Gold = Color(0xFFFFD700)
private val AmakaFlowPurple = Color(0xFF6C5CE7)
private val CardBackground = Color(0xFF0D0D0F)
private val CardSurface = Color(0xFF1A1A2E)

/**
 * Workout share card data model.
 */
data class WorkoutShareCardData(
    val workoutName: String,
    val durationSeconds: Int,
    val exerciseCount: Int,
    val totalVolumeKg: Double,
    val newPRs: List<PRDetectionResult.NewPR>,
    val currentStreak: Int
) {
    val formattedDuration: String
        get() {
            val hours = durationSeconds / 3600
            val mins = (durationSeconds % 3600) / 60
            return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        }

    val formattedVolume: String
        get() = if (totalVolumeKg >= 1000) {
            String.format("%.1fk kg", totalVolumeKg / 1000)
        } else {
            String.format("%.0f kg", totalVolumeKg)
        }
}

enum class ShareCardAspect {
    STORIES,  // 9:16
    SQUARE;   // 1:1

    val widthDp: Int get() = 1080
    val heightDp: Int get() = when (this) {
        STORIES -> 1920
        SQUARE -> 1080
    }
}

/**
 * Post-workout shareable card for social media.
 * AMA-1284
 */
@Composable
fun WorkoutShareCard(
    data: WorkoutShareCardData,
    aspect: ShareCardAspect,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(CardBackground, CardSurface, CardBackground)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Workout name
            Text(
                text = data.workoutName,
                fontSize = if (aspect == ShareCardAspect.STORIES) 32.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "WORKOUT COMPLETE",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AmakaFlowPurple,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(if (aspect == ShareCardAspect.STORIES) 40.dp else 24.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(value = data.formattedDuration, label = "Duration")
                VerticalDivider()
                StatItem(value = "${data.exerciseCount}", label = "Exercises")
                VerticalDivider()
                StatItem(value = data.formattedVolume, label = "Volume")
            }

            // PRs section
            if (data.newPRs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(if (aspect == ShareCardAspect.STORIES) 32.dp else 20.dp))
                PRSection(prs = data.newPRs)
            }

            // Streak
            if (data.currentStreak > 1) {
                Spacer(modifier = Modifier.height(20.dp))
                StreakBadge(streak = data.currentStreak)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = if (aspect == ShareCardAspect.STORIES) 36.dp else 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(AmakaFlowPurple, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AmakaFlow",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF9CA3AF)
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(Color.White.copy(alpha = 0.1f))
    )
}

@Composable
private fun PRSection(prs: List<PRDetectionResult.NewPR>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Gold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "NEW PERSONAL RECORDS",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Gold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        prs.take(3).forEach { pr ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Gold.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pr.exerciseName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = formatPRValue(pr),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun StreakBadge(streak: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color(0xFFF97316).copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color(0xFFF97316)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$streak day streak",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

private fun formatPRValue(pr: PRDetectionResult.NewPR): String {
    return when (pr.type) {
        PRType.HEAVIEST_WEIGHT -> String.format("%.1f kg", pr.newValue)
        PRType.MOST_REPS -> "${pr.newValue.toInt()} reps"
        PRType.MOST_VOLUME -> String.format("%.0f kg vol", pr.newValue)
    }
}
