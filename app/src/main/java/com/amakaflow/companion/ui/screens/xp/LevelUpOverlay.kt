package com.amakaflow.companion.ui.screens.xp

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
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing
import kotlinx.coroutines.delay
import kotlin.random.Random

private val Gold = Color(0xFFFFD700)
private val AmakaFlowPurple = Color(0xFF6C5CE7)

/**
 * Full-screen celebration overlay shown when a user levels up.
 * Auto-dismisses after 3 seconds or tap to dismiss.
 * AMA-1285
 */
@Composable
fun LevelUpOverlay(
    newLevel: Int,
    levelName: String,
    onDismiss: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }

    val levelGradient = when (newLevel) {
        in 1..2 -> listOf(Color(0xFF4EDF9B), Color(0xFF22C55E))
        in 3..4 -> listOf(Color(0xFF3A8BFF), Color(0xFF2563EB))
        in 5..6 -> listOf(Color(0xFFFFA500), Color(0xFFF97316))
        in 7..8 -> listOf(Color(0xFF9333EA), Color(0xFF7C3AED))
        in 9..10 -> listOf(Gold, Color(0xFFFFA500))
        else -> listOf(Color(0xFF3A8BFF), Color(0xFF2563EB))
    }

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
            .testTag("level_up_celebration_overlay")
    ) {
        // Confetti particles
        LevelUpConfettiLayer()

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
                // Star icon
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Level Up",
                    modifier = Modifier.size(72.dp),
                    tint = levelGradient.first()
                )

                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

                Text(
                    text = "Level Up!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))

                Text(
                    text = "Level $newLevel",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = levelGradient.first(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

                Text(
                    text = levelName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textSecondary,
                    textAlign = TextAlign.Center
                )

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

// MARK: - Confetti

@Composable
private fun LevelUpConfettiLayer() {
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
            LevelConfettiParticle(
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
            LevelConfettiPiece(particle = particle)
        }
    }
}

private data class LevelConfettiParticle(
    val color: Color,
    val startXFraction: Float,
    val duration: Int,
    val delay: Int,
    val size: Float,
    val isCircle: Boolean
)

@Composable
private fun LevelConfettiPiece(particle: LevelConfettiParticle) {
    val infiniteTransition = rememberInfiniteTransition(label = "level_confetti")

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
