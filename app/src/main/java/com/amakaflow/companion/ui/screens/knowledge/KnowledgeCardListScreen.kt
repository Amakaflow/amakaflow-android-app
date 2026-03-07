package com.amakaflow.companion.ui.screens.knowledge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.KnowledgeCard
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeCardListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAdd: () -> Unit,
    viewModel: KnowledgeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        delay(300L)
        when {
            searchQuery.length >= 3 -> viewModel.search(searchQuery)
            searchQuery.isEmpty() -> viewModel.loadCards()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Knowledge Library") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToAdd,
                        modifier = Modifier.testTag("knowledge_add_button"),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmakaColors.surface,
                    titleContentColor = AmakaColors.textPrimary,
                    navigationIconContentColor = AmakaColors.textPrimary,
                    actionIconContentColor = AmakaColors.accentBlue,
                ),
            )
        },
        containerColor = AmakaColors.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("knowledge_library_screen"),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.sm.dp)
                    .testTag("knowledge_search_field"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AmakaColors.textPrimary,
                    unfocusedTextColor = AmakaColors.textPrimary,
                    focusedLabelColor = AmakaColors.accentBlue,
                    unfocusedLabelColor = AmakaColors.textSecondary,
                    focusedBorderColor = AmakaColors.accentBlue,
                    unfocusedBorderColor = AmakaColors.borderMedium,
                ),
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = AmakaColors.accentBlue)
                    }
                }

                uiState.cards.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("knowledge_empty_state"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No knowledge cards yet.\nTap + to add your first one.",
                            color = AmakaColors.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = AmakaSpacing.sm.dp),
                    ) {
                        items(uiState.cards, key = { it.id }) { card ->
                            KnowledgeCardRow(
                                card = card,
                                onClick = { onNavigateToDetail(card.id) },
                            )
                            HorizontalDivider(
                                color = AmakaColors.borderLight,
                                modifier = Modifier.padding(start = AmakaSpacing.md.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KnowledgeCardRow(
    card: KnowledgeCard,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.sm.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = card.title ?: card.sourceUrl ?: "Untitled",
            style = MaterialTheme.typography.bodyLarge,
            color = AmakaColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (!card.microSummary.isNullOrBlank()) {
            Text(
                text = card.microSummary,
                style = MaterialTheme.typography.bodySmall,
                color = AmakaColors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (card.tags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(card.tags.take(3)) { tag ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = AmakaColors.accentBlue.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = AmakaColors.accentBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
