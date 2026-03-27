package com.amakaflow.companion.ui.screens.social

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amakaflow.companion.data.model.ChallengeBadge
import com.amakaflow.companion.ui.theme.AmakaColors
import kotlin.random.Random

private val goldColor = Color(0xFFFFD700)
private val orangeColor = Color(0xFFFFA500)

@Composable
fun ChallengeCompletionOverlay(
    badge: ChallengeBadge,
    onDismiss: () -> Unit
) {
    val scaleAnim = remember { Animatable(0.3f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f)
        )
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        contentAlpha.animateTo(1f, animationSpec = tween(500))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .testTag("challenge_completion_overlay"),
        contentAlignment = Alignment.Center
    ) {
        // Confetti particles
        ConfettiLayer()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Trophy badge
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scaleAnim.value)
                    .background(
                        brush = Brush.linearGradient(listOf(goldColor, orangeColor)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = "Trophy",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                "Challenge Complete!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.alpha(contentAlpha.value)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(contentAlpha.value)
            ) {
                Text(
                    badge.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = goldColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    badge.description,
                    fontSize = 15.sp,
                    color = AmakaColors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = goldColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .alpha(contentAlpha.value)
                    .padding(top = 8.dp)
            ) {
                Text(
                    "Awesome!",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ConfettiLayer() {
    val colors = listOf(
        goldColor,
        AmakaColors.accentBlue,
        AmakaColors.accentGreen,
        AmakaColors.accentOrange,
        Color(0xFFFF69B4)
    )

    val particles = remember {
        (0 until 30).map {
            ConfettiData(
                color = colors[it % colors.size],
                startX = Random.nextFloat() * 300f - 150f,
                startY = Random.nextFloat() * -400f - 100f,
                endY = Random.nextFloat() * 400f + 200f,
                size = Random.nextFloat() * 6f + 4f,
                duration = (Random.nextFloat() * 1500f + 1500f).toInt(),
                delay = (it * 50)
            )
        }
    }

    particles.forEach { data ->
        val yAnim = remember { Animatable(data.startY) }
        val alphaAnim = remember { Animatable(1f) }

        LaunchedEffect(data) {
            kotlinx.coroutines.delay(data.delay.toLong())
            kotlinx.coroutines.launch {
                yAnim.animateTo(data.endY, animationSpec = tween(data.duration, easing = EaseIn))
            }
            kotlinx.coroutines.launch {
                alphaAnim.animateTo(0f, animationSpec = tween(data.duration, easing = EaseIn))
            }
        }

        Box(
            modifier = Modifier
                .offset(x = data.startX.dp, y = yAnim.value.dp)
                .size(data.size.dp)
                .alpha(alphaAnim.value)
                .background(data.color, CircleShape)
        )
    }
}

private data class ConfettiData(
    val color: Color,
    val startX: Float,
    val startY: Float,
    val endY: Float,
    val size: Float,
    val duration: Int,
    val delay: Int
)
