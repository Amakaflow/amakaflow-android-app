package com.amakaflow.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val AmakaFlowPrimary = Color(0xFF6C63FF)
val AmakaFlowSecondary = Color(0xFF03DAC6)
val AmakaFlowError = Color(0xFFCF6679)
val AmakaFlowBackground = Color(0xFF121212)
val AmakaFlowSurface = Color(0xFF1E1E1E)

val ReadinessGood = Color(0xFF4CAF50)
val ReadinessModerate = Color(0xFFFFC107)
val ReadinessPoor = Color(0xFFF44336)

val HeartRateRed = Color(0xFFE53935)

internal val wearColors = Colors(
    primary = AmakaFlowPrimary,
    secondary = AmakaFlowSecondary,
    error = AmakaFlowError,
    background = AmakaFlowBackground,
    surface = AmakaFlowSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onError = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0B0)
)

@Composable
fun AmakaFlowWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = wearColors,
        content = content
    )
}
