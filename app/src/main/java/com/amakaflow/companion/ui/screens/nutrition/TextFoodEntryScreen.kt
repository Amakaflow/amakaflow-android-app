package com.amakaflow.companion.ui.screens.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amakaflow.companion.data.nutrition.ParseTextResponse
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

/**
 * AMA-1294: Natural language food entry screen.
 * Text field "I ate..." -> sends to API -> shows parsed items -> confirm and add.
 */
@Composable
fun TextFoodEntryScreen(
    uiState: FoodLoggingUiState,
    onTextChanged: (String) -> Unit,
    onParse: () -> Unit,
    onAddToDaily: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AmakaSpacing.md.dp)
            .testTag("text_food_entry_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Describe what you ate",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AmakaColors.textPrimary
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

        // Text input
        OutlinedTextField(
            value = uiState.textInput,
            onValueChange = onTextChanged,
            placeholder = {
                Text(
                    text = "e.g. I ate 2 eggs and toast with avocado",
                    color = AmakaColors.textTertiary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("text_food_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AmakaColors.accentBlue,
                unfocusedBorderColor = AmakaColors.borderMedium,
                focusedTextColor = AmakaColors.textPrimary,
                unfocusedTextColor = AmakaColors.textPrimary,
                cursorColor = AmakaColors.accentBlue
            ),
            minLines = 2,
            maxLines = 4,
            enabled = !uiState.isAnalyzing
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

        // Parse button
        Button(
            onClick = onParse,
            enabled = uiState.textInput.isNotBlank() && !uiState.isAnalyzing,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("text_parse_btn"),
            colors = ButtonDefaults.buttonColors(
                containerColor = AmakaColors.accentBlue
            )
        ) {
            if (uiState.isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = AmakaColors.textPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(AmakaSpacing.sm.dp))
                Text("Analyzing...")
            } else {
                Icon(Icons.Filled.Send, contentDescription = null)
                Spacer(Modifier.width(AmakaSpacing.sm.dp))
                Text("Analyze")
            }
        }

        Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))

        // Result
        uiState.textResult?.let { result ->
            TextResultCard(result = result)

            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

            if (!uiState.addedToDaily) {
                Button(
                    onClick = onAddToDaily,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("text_add_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmakaColors.accentGreen
                    )
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(AmakaSpacing.sm.dp))
                    Text("Add to Daily Totals")
                }
            } else {
                Text(
                    text = "Added to daily totals!",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.accentGreen,
                    modifier = Modifier.testTag("text_added_label")
                )
            }
        }

        // Error
        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = AmakaColors.accentRed,
                modifier = Modifier.testTag("text_error")
            )
        }
    }
}

@Composable
private fun TextResultCard(result: ParseTextResponse) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("text_result_card"),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(modifier = Modifier.padding(AmakaSpacing.md.dp)) {
            Text(
                text = "Parsed Items",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

            // Individual items
            result.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AmakaSpacing.xs.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.qty}x ${item.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AmakaColors.textPrimary
                    )
                    Text(
                        text = "${item.calories} kcal | ${item.protein.toInt()}g protein",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textSecondary
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = AmakaSpacing.sm.dp),
                color = AmakaColors.borderLight
            )

            // Totals
            Text(
                text = "${result.total.calories} kcal total",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.accentBlue
            )

            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextMacroItem(value = "${result.total.protein.toInt()}g", label = "Protein")
                TextMacroItem(value = "${result.total.carbs.toInt()}g", label = "Carbs")
                TextMacroItem(value = "${result.total.fat.toInt()}g", label = "Fat")
            }
        }
    }
}

@Composable
private fun TextMacroItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AmakaColors.textTertiary
        )
    }
}
