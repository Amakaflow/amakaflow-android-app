package com.amakaflow.companion.ui.screens.fatigue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.FatigueLevel
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

@Composable
fun FatigueAdvisorScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: FatigueAdvisorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("fatigue_advisor_screen")
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.sm.dp, vertical = AmakaSpacing.sm.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AmakaColors.textPrimary
                )
            }
            Text(
                text = "Fatigue Advisor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AmakaColors.accentBlue)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AmakaSpacing.md.dp),
                    verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
                ) {
                    // Fatigue score gauge
                    item {
                        FatigueScoreCard(
                            score = state.fatigueScore,
                            level = state.level
                        )
                    }

                    // Advice
                    state.advice?.let { advice ->
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fatigue_advice"),
                                color = AmakaColors.surface,
                                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(AmakaSpacing.md.dp)
                                ) {
                                    Text(
                                        text = "Advice",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AmakaColors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                                    Text(
                                        text = advice,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AmakaColors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Recommendations
                    if (state.recommendations.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recommendations",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AmakaColors.textPrimary
                            )
                        }
                        items(state.recommendations) { rec ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = AmakaColors.surface,
                                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(AmakaSpacing.md.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "\u2022",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AmakaColors.accentBlue,
                                        modifier = Modifier.padding(end = AmakaSpacing.sm.dp)
                                    )
                                    Text(
                                        text = rec,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AmakaColors.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Contributing factors
                    if (state.contributingFactors.isNotEmpty()) {
                        item {
                            Text(
                                text = "Contributing Factors",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AmakaColors.textPrimary
                            )
                        }
                        items(state.contributingFactors) { factor ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = AmakaColors.surface,
                                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(AmakaSpacing.md.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = factor.factor,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = AmakaColors.textPrimary
                                        )
                                        factor.description?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AmakaColors.textSecondary
                                            )
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { (factor.impact / 100.0).toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .width(60.dp)
                                            .height(6.dp),
                                        color = when {
                                            factor.impact > 70 -> AmakaColors.accentRed
                                            factor.impact > 40 -> AmakaColors.accentOrange
                                            else -> AmakaColors.accentGreen
                                        },
                                        trackColor = AmakaColors.borderLight
                                    )
                                }
                            }
                        }
                    }

                    state.error?.let { error ->
                        item {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmakaColors.accentRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FatigueScoreCard(score: Double, level: FatigueLevel) {
    val levelColor = when (level) {
        FatigueLevel.LOW -> AmakaColors.accentGreen
        FatigueLevel.MODERATE -> AmakaColors.accentYellow
        FatigueLevel.HIGH -> AmakaColors.accentOrange
        FatigueLevel.VERY_HIGH -> AmakaColors.accentRed
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fatigue_score_card"),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.lg.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Fatigue Score",
                style = MaterialTheme.typography.titleSmall,
                color = AmakaColors.textSecondary
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
            Text(
                text = String.format("%.0f", score),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = levelColor
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))
            Surface(
                color = levelColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
            ) {
                Text(
                    text = level.name.replace("_", " "),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = levelColor,
                    modifier = Modifier.padding(
                        horizontal = AmakaSpacing.md.dp,
                        vertical = AmakaSpacing.xs.dp
                    )
                )
            }
            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            LinearProgressIndicator(
                progress = { (score / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = levelColor,
                trackColor = AmakaColors.borderLight
            )
        }
    }
}
