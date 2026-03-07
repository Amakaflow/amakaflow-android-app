package com.amakaflow.companion.ui.screens.aiimport

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: AIImportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var message by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Import") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("ai_import_back_button"),
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmakaColors.surface,
                    titleContentColor = AmakaColors.textPrimary,
                ),
            )
        },
        containerColor = AmakaColors.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AmakaSpacing.lg.dp, vertical = AmakaSpacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp),
        ) {
            OutlinedTextField(
                value = sourceUrl,
                onValueChange = { sourceUrl = it },
                label = { Text("Workout URL (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_import_url_field"),
                singleLine = true,
                enabled = !uiState.isStreaming,
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("What do you want to import?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_import_message_field"),
                minLines = 3,
                maxLines = 6,
                enabled = !uiState.isStreaming,
            )

            Button(
                onClick = {
                    viewModel.startImport(
                        message = message,
                        sourceUrl = sourceUrl.ifBlank { null },
                    )
                },
                enabled = message.isNotBlank() && !uiState.isStreaming,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_import_button"),
            ) {
                if (uiState.isStreaming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = AmakaColors.textPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Importing…")
                } else {
                    Text("Start AI Import")
                }
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (uiState.isDone && uiState.events.isEmpty()) {
                Text(
                    text = "Import complete.",
                    color = AmakaColors.accentGreen,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (uiState.events.isNotEmpty()) {
                Text(
                    text = "Events",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmakaColors.textSecondary,
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(uiState.events) { event ->
                        Text(
                            text = event,
                            style = MaterialTheme.typography.bodySmall,
                            color = AmakaColors.textSecondary,
                        )
                    }
                }
            }

            if (uiState.isDone) {
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("ai_import_done_button"),
                ) {
                    Text("Done")
                }
            }
        }
    }
}
