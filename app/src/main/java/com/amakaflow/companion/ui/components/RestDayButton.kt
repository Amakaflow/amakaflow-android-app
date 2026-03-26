package com.amakaflow.companion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

/**
 * "Log Rest Day" button with green checkmark and positive messaging (AMA-1286).
 *
 * Tap awards 10 XP and shows celebration message.
 * Button becomes disabled after logging with a confirmed state.
 */
@Composable
fun RestDayButton(
    onRestDayLogged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val restGreen = Color(0xFF00B894)
    var isLogged by remember { mutableStateOf(false) }

    val iconBgColor by animateColorAsState(
        targetValue = if (isLogged) restGreen else restGreen.copy(alpha = 0.15f),
        animationSpec = spring(),
        label = "iconBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isLogged) restGreen.copy(alpha = 0.5f) else AmakaColors.borderLight,
        animationSpec = spring(),
        label = "border"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AmakaCornerRadius.md.dp))
            .clickable(enabled = !isLogged) {
                isLogged = true
                onRestDayLogged()
            }
            .testTag("rest_day_button"),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(borderColor)
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon circle
            Surface(
                modifier = Modifier.size(40.dp),
                color = iconBgColor,
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLogged) Icons.Filled.CheckCircle else Icons.Filled.NightsStay,
                        contentDescription = if (isLogged) "Rest day logged" else "Log rest day",
                        tint = if (isLogged) Color.White else restGreen,
                        modifier = Modifier.size(if (isLogged) 24.dp else 20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLogged) "Rest Day Logged" else "Log Rest Day",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = AmakaColors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isLogged)
                        "Muscles grow during rest. You\u2019re doing this right."
                    else
                        "Take an intentional rest day (+10 XP)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLogged) restGreen else AmakaColors.textSecondary,
                    maxLines = 2
                )
            }

            if (!isLogged) {
                Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                Surface(
                    color = restGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
                ) {
                    Text(
                        text = "+10 XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = restGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun RestDayButtonPreview() {
    RestDayButton(onRestDayLogged = {})
}
