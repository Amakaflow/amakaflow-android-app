package com.amakaflow.companion.ui.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WaterDrop
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
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

/**
 * AMA-1292: Nutrition onboarding / opt-in prompt.
 * Shown once to explain the feature and get consent before enabling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionOnboardingScreen(
    onEnable: () -> Unit,
    onDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AmakaColors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmakaColors.background
                )
            )
        },
        containerColor = AmakaColors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AmakaSpacing.lg.dp)
                .testTag("nutrition_onboarding_screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                imageVector = Icons.Filled.Restaurant,
                contentDescription = null,
                tint = AmakaColors.accentGreen,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))

            // Title
            Text(
                text = "Track Your Nutrition",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

            // Subtitle
            Text(
                text = "Log protein and water to fuel your training",
                style = MaterialTheme.typography.bodyLarge,
                color = AmakaColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))

            // Feature bullets
            FeatureBullet(
                icon = Icons.Filled.Restaurant,
                title = "Quick Protein Logging",
                description = "Track protein with one-tap buttons"
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            FeatureBullet(
                icon = Icons.Filled.WaterDrop,
                title = "Water Tracking",
                description = "Stay hydrated with simple cup tracking"
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            FeatureBullet(
                icon = Icons.Filled.Security,
                title = "Privacy First",
                description = "Choose what to show — qualitative labels or full data"
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))

            // Enable button
            Button(
                onClick = onEnable,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("enable_nutrition_button"),
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmakaColors.accentGreen,
                    contentColor = AmakaColors.background
                )
            ) {
                Text(
                    text = "Enable Nutrition Tracking",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

            // Skip button
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("skip_nutrition_button")
            ) {
                Text(
                    text = "Maybe Later",
                    color = AmakaColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun FeatureBullet(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = AmakaColors.surface,
            shape = RoundedCornerShape(AmakaCornerRadius.sm.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AmakaColors.accentGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = AmakaColors.textSecondary
            )
        }
    }
}
