package com.amakaflow.companion.ui.screens.knowledge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeCardDetailScreen(
    cardId: String,
    onNavigateBack: () -> Unit,
    viewModel: KnowledgeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(cardId) {
        viewModel.loadCard(cardId)
    }

    val card = uiState.selectedCard

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card?.title ?: "Card Detail") },
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
        Box(modifier = Modifier.fillMaxSize().testTag("knowledge_card_detail_screen")) {
        if (card == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = AmakaColors.accentBlue,
                        modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                    )
                } else {
                    Text(
                        text = "Card not found.",
                        color = AmakaColors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.Center)
                            .padding(AmakaSpacing.md.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(AmakaSpacing.md.dp),
                verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp),
            ) {
                if (!card.summary.isNullOrBlank()) {
                    item {
                        Text(
                            text = "Summary",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmakaColors.textSecondary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = card.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.textPrimary,
                        )
                    }
                }

                if (card.keyTakeaways.isNotEmpty()) {
                    item {
                        Text(
                            text = "Key Takeaways",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmakaColors.textSecondary,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    items(card.keyTakeaways) { takeaway ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "\u2022",
                                color = AmakaColors.accentBlue,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = takeaway,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AmakaColors.textPrimary,
                            )
                        }
                    }
                }

                if (!card.sourceUrl.isNullOrBlank()) {
                    item {
                        Text(
                            text = "Source",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmakaColors.textSecondary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = card.sourceUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = AmakaColors.accentBlue,
                        )
                    }
                }
            }
        }
        } // end testTag Box
    }
}
