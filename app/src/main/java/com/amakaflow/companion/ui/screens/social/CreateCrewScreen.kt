package com.amakaflow.companion.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCrewScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: CrewsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var maxMembers by remember { mutableIntStateOf(8) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("create_crew_screen")
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
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AmakaColors.textPrimary
                )
            }
            Text(
                "Create Crew",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    viewModel.createCrew(name, description.ifBlank { null }, maxMembers)
                    onNavigateBack()
                },
                enabled = name.isNotBlank() && !state.isCreating
            ) {
                Text("Create", color = AmakaColors.accentBlue)
            }
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Crew name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Max members: $maxMembers",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmakaColors.textPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)) {
                    OutlinedButton(
                        onClick = { if (maxMembers > 3) maxMembers-- },
                        enabled = maxMembers > 3
                    ) {
                        Text("-")
                    }
                    OutlinedButton(
                        onClick = { if (maxMembers < 8) maxMembers++ },
                        enabled = maxMembers < 8
                    ) {
                        Text("+")
                    }
                }
            }

            state.createError?.let { error ->
                Text(
                    error,
                    color = AmakaColors.accentRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
