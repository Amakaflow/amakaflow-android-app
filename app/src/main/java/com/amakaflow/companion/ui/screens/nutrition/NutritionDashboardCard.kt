package com.amakaflow.companion.ui.screens.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amakaflow.companion.data.nutrition.DailyNutritionSummary
import com.amakaflow.companion.data.nutrition.NutritionDisplayMode
import com.amakaflow.companion.data.nutrition.NutritionLabel
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

/**
 * AMA-1290: Dashboard card showing "Today's Nutrition" on the home screen.
 * Respects privacy settings — qualitative labels by default, numeric when opted in.
 */
@Composable
fun NutritionDashboardCard(
    nutrition: DailyNutritionSummary,
    nutritionLabel: NutritionLabel,
    displayMode: NutritionDisplayMode,
    source: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("nutrition_dashboard_card"),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(
            modifier = Modifier.padding(AmakaSpacing.md.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Restaurant,
                    contentDescription = null,
                    tint = AmakaColors.accentGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                Text(
                    text = "Today's Nutrition",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

            // Content based on display mode
            when (displayMode) {
                NutritionDisplayMode.QUALITATIVE -> {
                    QualitativeDisplay(nutritionLabel)
                }
                NutritionDisplayMode.PROTEIN_ONLY -> {
                    ProteinOnlyDisplay(nutrition)
                }
                NutritionDisplayMode.FULL_MACROS -> {
                    FullMacrosDisplay(nutrition)
                }
                NutritionDisplayMode.CALORIES_AND_MACROS -> {
                    CaloriesAndMacrosDisplay(nutrition)
                }
            }

            // Source attribution
            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
            Text(
                text = "Source: $source",
                style = MaterialTheme.typography.labelSmall,
                color = AmakaColors.textTertiary,
                modifier = Modifier.testTag("nutrition_source")
            )
        }
    }
}

@Composable
private fun QualitativeDisplay(label: NutritionLabel) {
    val color = when (label) {
        NutritionLabel.WELL_FUELED -> AmakaColors.accentGreen
        NutritionLabel.ON_TRACK -> AmakaColors.accentBlue
        NutritionLabel.LOW_PROTEIN -> AmakaColors.accentOrange
        NutritionLabel.UNDER_EATING -> AmakaColors.accentRed
        NutritionLabel.NO_DATA -> AmakaColors.textTertiary
    }

    Text(
        text = label.text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.testTag("nutrition_label")
    )
}

@Composable
private fun ProteinOnlyDisplay(nutrition: DailyNutritionSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        NutrientItem(
            value = "${nutrition.proteinGrams.toInt()}g",
            label = "Protein"
        )
    }
}

@Composable
private fun FullMacrosDisplay(nutrition: DailyNutritionSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        NutrientItem(value = "${nutrition.proteinGrams.toInt()}g", label = "Protein")
        NutrientItem(value = "${nutrition.carbsGrams.toInt()}g", label = "Carbs")
        NutrientItem(value = "${nutrition.fatGrams.toInt()}g", label = "Fat")
    }
}

@Composable
private fun CaloriesAndMacrosDisplay(nutrition: DailyNutritionSummary) {
    Column {
        Text(
            text = "${nutrition.calories.toInt()} kcal",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.accentBlue,
            modifier = Modifier.testTag("nutrition_calories")
        )
        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NutrientItem(value = "${nutrition.proteinGrams.toInt()}g", label = "Protein")
            NutrientItem(value = "${nutrition.carbsGrams.toInt()}g", label = "Carbs")
            NutrientItem(value = "${nutrition.fatGrams.toInt()}g", label = "Fat")
        }
    }
}

@Composable
private fun NutrientItem(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AmakaColors.textTertiary
        )
    }
}
