package com.amakaflow.companion.ui.screens.suggest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

@Composable
fun CoachingProfileOnboardingContent(
    viewModel: SuggestWorkoutViewModel
) {
    var experience by remember { mutableStateOf(ExperienceLevel.INTERMEDIATE) }
    var goal by remember { mutableStateOf(TrainingGoal.GENERAL_FITNESS) }
    var daysPerWeek by remember { mutableIntStateOf(3) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AmakaSpacing.md.dp)
            .testTag("coaching_onboarding"),
        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.lg.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = AmakaColors.accentOrange,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
                Text(
                    text = "Quick Setup",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AmakaColors.textPrimary
                )
                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                Text(
                    text = "Tell us a little about yourself so we can suggest the right workout for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmakaColors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Experience Level
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AmakaColors.surface,
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
            ) {
                Column(modifier = Modifier.padding(AmakaSpacing.lg.dp)) {
                    Text(
                        text = "Experience Level",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AmakaColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ExperienceLevel.entries.forEachIndexed { index, level ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index, ExperienceLevel.entries.size),
                                onClick = { experience = level },
                                selected = experience == level,
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = AmakaColors.accentOrange.copy(alpha = 0.2f),
                                    activeContentColor = AmakaColors.accentOrange
                                )
                            ) {
                                Text(level.displayName)
                            }
                        }
                    }
                }
            }
        }

        // Training Goal
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AmakaColors.surface,
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
            ) {
                Column(modifier = Modifier.padding(AmakaSpacing.lg.dp)) {
                    Text(
                        text = "Primary Goal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AmakaColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                    TrainingGoal.entries.forEachIndexed { index, goalOption ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { goal = goalOption }
                                .padding(vertical = AmakaSpacing.sm.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = goalOption.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmakaColors.textPrimary
                            )
                            Icon(
                                imageVector = if (goal == goalOption) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (goal == goalOption) AmakaColors.accentOrange else AmakaColors.textTertiary
                            )
                        }
                        if (index < TrainingGoal.entries.size - 1) {
                            HorizontalDivider(color = AmakaColors.borderLight)
                        }
                    }
                }
            }
        }

        // Days Per Week
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AmakaColors.surface,
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
            ) {
                Column(modifier = Modifier.padding(AmakaSpacing.lg.dp)) {
                    Text(
                        text = "Days Per Week",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AmakaColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
                    ) {
                        (1..7).forEach { day ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable { daysPerWeek = day },
                                color = if (daysPerWeek == day) AmakaColors.accentOrange else AmakaColors.background,
                                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (daysPerWeek == day) AmakaColors.background else AmakaColors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Submit button
        item {
            Button(
                onClick = {
                    viewModel.completeOnboarding(experience, goal, daysPerWeek)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generate_workout_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AmakaColors.accentOrange),
                shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, null)
                Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                Text("Generate My Workout", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))
        }
    }
}
