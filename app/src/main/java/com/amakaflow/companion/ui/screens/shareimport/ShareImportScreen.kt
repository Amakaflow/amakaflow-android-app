package com.amakaflow.companion.ui.screens.shareimport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing
import com.amakaflow.companion.util.PlatformDetector

/**
 * AMA-1258: Mini preview UI shown when a URL is shared to AmakaFlow from another app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareImportScreen(
    viewModel: ShareImportViewModel,
    onDismiss: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Workout") },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("share_import_close_button"),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
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
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(horizontal = AmakaSpacing.lg.dp, vertical = AmakaSpacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp),
        ) {
            // Error state
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("share_import_error"),
                )
            }

            // URL preview cards
            uiState.urls.forEach { item ->
                ShareUrlCard(item = item)
            }

            // Import result
            uiState.importResult?.let { result ->
                ImportResultCard(result = result, onDismiss = onDismiss)
            }

            // Action buttons (only when there are URLs and no result yet)
            if (uiState.urls.isNotEmpty() && uiState.importResult == null) {
                Spacer(Modifier.height(AmakaSpacing.sm.dp))

                if (uiState.urls.size == 1) {
                    // Single URL — import now
                    Button(
                        onClick = { viewModel.importSingleUrl() },
                        enabled = !uiState.isImporting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_import_button"),
                    ) {
                        if (uiState.isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = AmakaColors.textPrimary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Importing...")
                        } else {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Import Workout")
                        }
                    }
                } else {
                    // Multiple URLs — background import
                    Button(
                        onClick = { viewModel.importInBackground() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_import_batch_button"),
                    ) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Import ${uiState.urls.size} Workouts")
                    }
                }

                // Always offer background option for single URL too
                if (uiState.urls.size == 1 && !uiState.isImporting) {
                    OutlinedButton(
                        onClick = { viewModel.importInBackground() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_import_background_button"),
                    ) {
                        Text("Import in Background")
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareUrlCard(item: ShareUrlItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("share_url_card"),
        colors = CardDefaults.cardColors(containerColor = AmakaColors.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Platform icon
            Icon(
                imageVector = Icons.Filled.Link,
                contentDescription = item.platform.displayName,
                tint = platformColor(item.platform),
                modifier = Modifier.size(32.dp),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.platform.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary,
                )
                Text(
                    text = item.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ImportResultCard(result: ImportResult, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("share_import_result"),
        colors = CardDefaults.cardColors(containerColor = AmakaColors.surface),
    ) {
        Column(
            modifier = Modifier.padding(AmakaSpacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = AmakaColors.accentGreen,
                    modifier = Modifier.size(20.dp),
                )
                when (result) {
                    is ImportResult.Success -> {
                        Text(
                            text = "Workout Imported",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmakaColors.accentGreen,
                        )
                    }
                    is ImportResult.BackgroundEnqueued -> {
                        Text(
                            text = "${result.count} workout(s) queued",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmakaColors.accentGreen,
                        )
                    }
                }
            }

            when (result) {
                is ImportResult.Success -> {
                    Text(
                        text = result.workoutName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AmakaColors.textPrimary,
                    )
                }
                is ImportResult.BackgroundEnqueued -> {
                    Text(
                        text = "Imports will continue in the background. You'll get a notification when they're done.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(AmakaSpacing.sm.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_import_done_button"),
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun platformColor(platform: PlatformDetector.Platform) = when (platform) {
    PlatformDetector.Platform.YOUTUBE -> AmakaColors.accentRed
    PlatformDetector.Platform.INSTAGRAM -> AmakaColors.accentOrange
    PlatformDetector.Platform.TIKTOK -> AmakaColors.accentBlue
    PlatformDetector.Platform.PINTEREST -> AmakaColors.accentRed
    PlatformDetector.Platform.UNKNOWN -> AmakaColors.textSecondary
}
