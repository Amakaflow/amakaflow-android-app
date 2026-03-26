package com.amakaflow.companion.ui.screens.rpefeedback

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

@Composable
fun RPEFeedbackScreen(
    uiState: RPEFeedbackUiState,
    onSelectOption: (RPEOption) -> Unit,
    onToggleMuscle: (MuscleGroup) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("rpe_feedback_screen")
    ) {
        if (uiState.isSubmitted) {
            // Success state
            SubmittedContent(deloadRecommended = uiState.deloadRecommended)
        } else {
            // Feedback form
            FeedbackFormContent(
                uiState = uiState,
                onSelectOption = onSelectOption,
                onToggleMuscle = onToggleMuscle,
                onSubmit = onSubmit,
                onSkip = onSkip
            )
        }
    }
}

@Composable
private fun FeedbackFormContent(
    uiState: RPEFeedbackUiState,
    onSelectOption: (RPEOption) -> Unit,
    onToggleMuscle: (MuscleGroup) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AmakaSpacing.lg.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.3f))

        // Title
        Text(
            text = "How was that?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

        Text(
            text = "Rate your effort",
            style = MaterialTheme.typography.bodyLarge,
            color = AmakaColors.textSecondary
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))

        // RPE emoji buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
        ) {
            RPEOption.entries.forEach { option ->
                RPEOptionButton(
                    option = option,
                    isSelected = uiState.selectedOption == option,
                    onClick = { onSelectOption(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Muscle soreness (shown after selection)
        AnimatedVisibility(
            visible = uiState.selectedOption != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(top = AmakaSpacing.lg.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Any soreness?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textSecondary
                )

                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.xs.dp),
                    verticalArrangement = Arrangement.spacedBy(AmakaSpacing.xs.dp),
                    modifier = Modifier.height(80.dp)
                ) {
                    items(MuscleGroup.entries) { muscle ->
                        MuscleChip(
                            muscle = muscle,
                            isSelected = uiState.selectedMuscles.contains(muscle),
                            onClick = { onToggleMuscle(muscle) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.selectedOption != null) {
                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmakaColors.accentBlue
                    ),
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("rpe_submit_button")
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Submit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier.testTag("rpe_skip_button")
            ) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AmakaColors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
    }
}

@Composable
private fun RPEOptionButton(
    option: RPEOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) AmakaColors.accentBlue.copy(alpha = 0.2f) else AmakaColors.surface
    val borderColor = if (isSelected) AmakaColors.accentBlue else AmakaColors.borderLight
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .border(borderWidth, borderColor, RoundedCornerShape(AmakaCornerRadius.md.dp))
            .background(bgColor, RoundedCornerShape(AmakaCornerRadius.md.dp))
            .clickable(onClick = onClick)
            .padding(vertical = AmakaSpacing.md.dp)
            .testTag("rpe_option_${option.label.lowercase()}")
    ) {
        Text(
            text = option.emoji,
            fontSize = 36.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))

        Text(
            text = option.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) AmakaColors.textPrimary else AmakaColors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MuscleChip(
    muscle: MuscleGroup,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) AmakaColors.accentBlue else AmakaColors.surface
    val textColor = if (isSelected) Color.White else AmakaColors.textSecondary
    val borderColor = if (isSelected) AmakaColors.accentBlue else AmakaColors.borderLight

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .border(1.dp, borderColor, RoundedCornerShape(AmakaCornerRadius.sm.dp))
            .background(bgColor, RoundedCornerShape(AmakaCornerRadius.sm.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = AmakaSpacing.sm.dp, vertical = AmakaSpacing.xs.dp)
            .testTag("muscle_${muscle.apiValue}")
    ) {
        Text(
            text = muscle.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SubmittedContent(deloadRecommended: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Success",
            tint = AmakaColors.accentGreen,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

        Text(
            text = "Thanks!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary
        )

        if (deloadRecommended) {
            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
            Text(
                text = "Consider a lighter session next time",
                style = MaterialTheme.typography.bodyLarge,
                color = AmakaColors.accentOrange
            )
        }
    }
}
