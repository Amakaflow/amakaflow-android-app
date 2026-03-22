package com.amakaflow.companion.ui.screens.shoes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.ShoeDetail
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

@Composable
fun ShoeComparisonScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ShoeComparisonViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("shoe_comparison_screen")
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
                text = "Shoe Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AmakaSpacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
        ) {
            // Search input
            item {
                OutlinedTextField(
                    value = state.shoeInput,
                    onValueChange = { viewModel.updateShoeInput(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shoe_input"),
                    placeholder = {
                        Text(
                            "Enter shoe names (comma-separated)",
                            color = AmakaColors.textTertiary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = AmakaColors.textSecondary
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmakaColors.accentBlue,
                        unfocusedBorderColor = AmakaColors.borderMedium,
                        focusedTextColor = AmakaColors.textPrimary,
                        unfocusedTextColor = AmakaColors.textPrimary,
                        cursorColor = AmakaColors.accentBlue
                    ),
                    shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
                )
            }

            // Compare button
            item {
                Button(
                    onClick = { viewModel.compare() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("compare_button"),
                    enabled = !state.isLoading && state.shoeInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmakaColors.accentBlue,
                        contentColor = AmakaColors.textPrimary
                    ),
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AmakaColors.textPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Compare", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Results
            state.comparison?.let { comparison ->
                // Recommendation
                comparison.recommendation?.let { rec ->
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = AmakaColors.accentBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                        ) {
                            Text(
                                text = rec,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmakaColors.textPrimary,
                                modifier = Modifier.padding(AmakaSpacing.md.dp)
                            )
                        }
                    }
                }

                // Shoe cards
                items(comparison.shoes) { shoe ->
                    ShoeCard(shoe = shoe)
                }
            }

            // Error
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

@Composable
private fun ShoeCard(shoe: ShoeDetail) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shoe_card_${shoe.name}"),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = shoe.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AmakaColors.textPrimary
                    )
                    shoe.brand?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = AmakaColors.textSecondary
                        )
                    }
                }
                shoe.rating?.let { rating ->
                    Text(
                        text = String.format("%.1f/5", rating),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmakaColors.accentYellow
                    )
                }
            }

            shoe.priceRange?.let {
                Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary
                )
            }

            if (shoe.pros.isNotEmpty()) {
                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                Text(
                    text = "Pros",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmakaColors.accentGreen,
                    fontWeight = FontWeight.SemiBold
                )
                shoe.pros.forEach { pro ->
                    Text(
                        text = "+ $pro",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textPrimary
                    )
                }
            }

            if (shoe.cons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                Text(
                    text = "Cons",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmakaColors.accentRed,
                    fontWeight = FontWeight.SemiBold
                )
                shoe.cons.forEach { con ->
                    Text(
                        text = "- $con",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textPrimary
                    )
                }
            }

            shoe.bestFor?.let {
                Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                Text(
                    text = "Best for: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.accentBlue,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
