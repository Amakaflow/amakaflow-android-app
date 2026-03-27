package com.amakaflow.companion.ui.screens.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.nutrition.NutritionDisplayMode
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

/**
 * AMA-1292: Nutrition privacy settings screen.
 * - Show/hide nutrition features
 * - Display mode selection
 * - Delete all nutrition data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionSettingsScreen(
    onDismiss: () -> Unit,
    viewModel: NutritionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete All Nutrition Data") },
            text = {
                Text("This will remove all nutrition data and reset settings. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllData()
                        showDeleteConfirmation = false
                    },
                    modifier = Modifier.testTag("delete_confirm_button")
                ) {
                    Text("Delete", color = AmakaColors.accentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
            containerColor = AmakaColors.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nutrition Settings",
                        color = AmakaColors.textPrimary
                    )
                },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AmakaSpacing.md.dp)
                .testTag("nutrition_settings_screen"),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
        ) {
            // Enable/disable nutrition
            item {
                Surface(
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
                                text = "Show Nutrition Features",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AmakaColors.textPrimary
                            )
                            Text(
                                text = "Display nutrition cards on home screen",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmakaColors.textSecondary
                            )
                        }
                        Switch(
                            checked = uiState.isEnabled,
                            onCheckedChange = { viewModel.setEnabled(it) },
                            modifier = Modifier.testTag("nutrition_enable_toggle"),
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = AmakaColors.accentGreen
                            )
                        )
                    }
                }
            }

            // Display mode
            item {
                Surface(
                    color = AmakaColors.surface,
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AmakaSpacing.md.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = AmakaColors.accentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                            Text(
                                text = "Display Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AmakaColors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                        Text(
                            text = "Control how much detail is shown",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmakaColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

                        NutritionDisplayMode.entries.forEach { mode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AmakaSpacing.xs.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.displayMode == mode,
                                    onClick = { viewModel.setDisplayMode(mode) },
                                    modifier = Modifier.testTag("display_mode_${mode.name}"),
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = AmakaColors.accentBlue
                                    )
                                )
                                Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AmakaColors.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Delete all data
            item {
                Surface(
                    color = AmakaColors.surface,
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
                ) {
                    TextButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AmakaSpacing.sm.dp)
                            .testTag("delete_nutrition_data_button"),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = AmakaColors.accentRed
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                        Text(
                            text = "Delete All Nutrition Data",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Bottom spacer
            item {
                Spacer(modifier = Modifier.height(AmakaSpacing.lg.dp))
            }
        }
    }
}
