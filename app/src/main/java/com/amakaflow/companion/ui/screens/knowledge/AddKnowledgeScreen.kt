package com.amakaflow.companion.ui.screens.knowledge

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddKnowledgeScreen(
    onNavigateBack: () -> Unit,
    viewModel: KnowledgeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var urlText by remember { mutableStateOf("") }
    var manualText by remember { mutableStateOf("") }

    LaunchedEffect(viewModel) {
        viewModel.navigationEvent.collectLatest { onNavigateBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add to Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmakaColors.surface,
                    titleContentColor = AmakaColors.textPrimary,
                    navigationIconContentColor = AmakaColors.textPrimary,
                ),
            )
        },
        containerColor = AmakaColors.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AmakaSpacing.lg.dp, vertical = AmakaSpacing.md.dp)
                .testTag("add_knowledge_screen"),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp),
        ) {
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("URL (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("knowledge_url_field"),
                singleLine = true,
                enabled = !uiState.isSaving,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AmakaColors.textPrimary,
                    unfocusedTextColor = AmakaColors.textPrimary,
                    focusedLabelColor = AmakaColors.accentBlue,
                    unfocusedLabelColor = AmakaColors.textSecondary,
                    focusedBorderColor = AmakaColors.accentBlue,
                    unfocusedBorderColor = AmakaColors.borderMedium,
                ),
            )

            OutlinedTextField(
                value = manualText,
                onValueChange = { manualText = it },
                label = { Text("Or paste text") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("knowledge_text_field"),
                minLines = 4,
                maxLines = 8,
                enabled = !uiState.isSaving,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AmakaColors.textPrimary,
                    unfocusedTextColor = AmakaColors.textPrimary,
                    focusedLabelColor = AmakaColors.accentBlue,
                    unfocusedLabelColor = AmakaColors.textSecondary,
                    focusedBorderColor = AmakaColors.accentBlue,
                    unfocusedBorderColor = AmakaColors.borderMedium,
                ),
            )

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    viewModel.ingest(
                        url = urlText.takeIf { it.isNotBlank() },
                        text = manualText.takeIf { it.isNotBlank() },
                    )
                },
                enabled = (urlText.isNotBlank() || manualText.isNotBlank()) && !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("knowledge_save_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmakaColors.accentBlue,
                    contentColor = AmakaColors.textPrimary,
                ),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = AmakaColors.textPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Saving…")
                } else {
                    Text("Save to Library")
                }
            }
        }
    }
}
