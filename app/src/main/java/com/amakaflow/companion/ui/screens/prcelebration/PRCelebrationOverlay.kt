package com.amakaflow.companion.ui.screens.prcelebration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amakaflow.companion.domain.usecase.pr.PRDetectionResult
import com.amakaflow.companion.domain.usecase.pr.PRType
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing
import kotlinx.coroutines.delay
import kotlin.random.Random

private val Gold = Color(0xFFFFD700)
private val GoldDark = Color(0xFFFFA500)
private val AmakaFlowPurple = Color(0xFF6C5CE7)

/**
 * Full-screen confetti overlay shown when a personal record is detected.
 * Auto-dismisses after 3 seconds or tap to dismiss.
 * AMA-1282
 */
@Composable
fun PRCelebrationOverlay(
    newPRs: List<PRDetectionResult.NewPR>,
    onDismiss: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showContent = true
        delay(3000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
            .testTag("pr_celebration_overlay")
    ) {
        // Confetti particles
        ConfettiLayer()

        // Content
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(animationSpec = tween(600)) + scaleIn(
                initialScale = 0.3f,
                animationSpec = spring(dampingRatio = 0.7f)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AmakaSpacing.lg.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Trophy icon
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Trophy",
                    modifier = Modifier.size(72.dp),
                    tint = Gold
                )

                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

                Text(
                    text = "New Personal Record!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))

                // PR details
                newPRs.forEach { pr ->
                    PRDetailCard(pr = pr)
                    Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                }

                Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))

                Text(
                    text = "Tap anywhere to dismiss",
                    fontSize = 13.sp,
                    color = AmakaColors.textTertiary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun PRDetailCard(pr: PRDetectionResult.NewPR) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                AmakaColors.surface.copy(alpha = 0.8f),
                RoundedCornerShape(AmakaCornerRadius.md.dp)
            )
            .padding(AmakaSpacing.md.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = pr.exerciseName,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (pr.oldValue != null) {
                Text(
                    text = formatPRValue(pr.oldValue, pr.type, pr.weight),
                    fontSize = 15.sp,
                    color = AmakaColors.textSecondary,
                    textDecoration = TextDecoration.LineThrough
                )
                Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                Text(
                    text = "\u2192",
                    fontSize = 12.sp,
                    color = Gold
                )
                Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
            }

            Text(
                text = formatPRValue(pr.newValue, pr.type, pr.weight),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Gold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = pr.type.displayLabel,
            fontSize = 13.sp,
            color = AmakaColors.textTertiary
        )
    }
}

private val PRType.displayLabel: String
    get() = when (this) {
        PRType.HEAVIEST_WEIGHT -> "Max Weight"
        PRType.MOST_REPS -> "Max Reps"
        PRType.MOST_VOLUME -> "Max Volume"
    }

private fun formatPRValue(value: Double, type: PRType, weight: Double?): String {
    return when (type) {
        PRType.HEAVIEST_WEIGHT -> String.format("%.1f kg", value)
        PRType.MOST_REPS -> if (weight != null) {
            "${value.toInt()} reps @ ${String.format("%.1f", weight)} kg"
        } else "${value.toInt()} reps"
        PRType.MOST_VOLUME -> String.format("%.0f kg vol", value)
    }
}

// MARK: - Confetti

@Composable
private fun ConfettiLayer() {
    val confettiColors = listOf(
        Gold,
        AmakaFlowPurple,
        Color(0xFFFF6B6B),
        Color(0xFF4EDF9B),
        Color(0xFF3A8BFF),
        Color(0xFFFFA500)
    )

    val particles = remember {
        (0 until 40).map { i ->
            ConfettiParticle(
                color = confettiColors[i % confettiColors.size],
                startXFraction = Random.nextFloat(),
                duration = (2000 + Random.nextInt(2000)),
                delay = Random.nextInt(800),
                size = (6 + Random.nextInt(8)).toFloat(),
                isCircle = Random.nextBoolean()
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            ConfettiPiece(particle = particle)
        }
    }
}

private data class ConfettiParticle(
    val color: Color,
    val startXFraction: Float,
    val duration: Int,
    val delay: Int,
    val size: Float,
    val isCircle: Boolean
)

@Composable
private fun ConfettiPiece(particle: ConfettiParticle) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")

    val yOffset by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = particle.duration,
                delayMillis = particle.delay,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "y"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = particle.duration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )

    val alpha = if (yOffset > 1000f) ((1200f - yOffset) / 200f).coerceIn(0f, 1f) else 1f

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val xPos = (maxWidth.value * particle.startXFraction).dp

        Box(
            modifier = Modifier
                .offset(x = xPos, y = yOffset.dp)
                .rotate(rotation)
                .alpha(alpha)
                .size(particle.size.dp)
                .clip(if (particle.isCircle) CircleShape else RoundedCornerShape(2.dp))
                .background(particle.color)
        )
    }
}
