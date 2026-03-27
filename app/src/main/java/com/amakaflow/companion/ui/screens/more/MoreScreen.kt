package com.amakaflow.companion.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing

@Composable
fun MoreScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAIImport: () -> Unit,
    onNavigateToCoachChat: () -> Unit = {},
    onNavigateToActivityFeed: () -> Unit = {},
    onNavigateToTrainingPreferences: () -> Unit = {},
    onNavigateToShoeComparison: () -> Unit = {},
    onNavigateToFatigueAdvisor: () -> Unit = {},
    onNavigateToFoodLogging: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("more_screen")
    ) {
        // Header
        Text(
            text = "More",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AmakaColors.textPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AmakaSpacing.md.dp),
            textAlign = TextAlign.Center
        )

        HorizontalDivider(color = AmakaColors.borderLight)

        // Menu items
        MoreMenuItem(
            icon = Icons.Filled.History,
            title = "History",
            onClick = onNavigateToHistory
        )

        MoreMenuDivider()

        MoreMenuItem(
            icon = Icons.Filled.Psychology,
            title = "AI Coach",
            onClick = onNavigateToCoachChat
        )

        MoreMenuDivider()

        MoreMenuItem(
            icon = Icons.Filled.DynamicFeed,
            title = "Activity Feed",
            onClick = onNavigateToActivityFeed
        )

        MoreMenuDivider()

        MoreMenuItem(
            icon = Icons.Filled.BatteryAlert,
            title = "Fatigue Advisor",
            onClick = onNavigateToFatigueAdvisor
        )

        MoreMenuDivider()

        MoreMenuItem(
            icon = Icons.AutoMirrored.Filled.CompareArrows,
            title = "Shoe Comparison",
            onClick = onNavigateToShoeComparison
        )

        MoreMenuDivider()

        MoreMenuItem(
            icon = Icons.Filled.AutoAwesome,
            title = "AI Import",
            onClick = onNavigateToAIImport
        )

        MoreMenuDivider()

        MoreMenuItem(
            icon = Icons.Filled.Tune,
            title = "Training Preferences",
            onClick = onNavigateToTrainingPreferences
        )

        MoreMenuDivider()

        MoreMenuItem(
            icon = Icons.Filled.Restaurant,
            title = "Log Food",
            onClick = onNavigateToFoodLogging
        )

        MoreMenuDivider()

        MoreMenuItem(
            icon = Icons.Filled.Settings,
            title = "Settings",
            onClick = onNavigateToSettings
        )

        HorizontalDivider(color = AmakaColors.borderLight)
    }
}

@Composable
private fun MoreMenuDivider() {
    HorizontalDivider(
        color = AmakaColors.borderLight,
        modifier = Modifier.padding(start = 56.dp)
    )
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.md.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AmakaColors.accentBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = AmakaColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = AmakaColors.textTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
