package com.amakaflow.companion.ui.screens.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

@Composable
fun CoachChatScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: CoachViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("coach_chat_screen")
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
                text = "AI Coach",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        // Messages list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("coach_messages_list"),
            state = listState,
            contentPadding = PaddingValues(AmakaSpacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
        ) {
            items(state.messages) { message ->
                ChatBubble(message = message, onSuggestionClick = { suggestion ->
                    viewModel.sendMessage(suggestion)
                })
            }

            if (state.isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AmakaSpacing.sm.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AmakaColors.accentBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                        Text(
                            text = "Thinking...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.textSecondary
                        )
                    }
                }
            }
        }

        // Error
        state.error?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = AmakaColors.accentRed,
                modifier = Modifier.padding(horizontal = AmakaSpacing.md.dp)
            )
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.sm.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("coach_input_field"),
                placeholder = {
                    Text(
                        text = "Ask your coach...",
                        color = AmakaColors.textTertiary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmakaColors.accentBlue,
                    unfocusedBorderColor = AmakaColors.borderMedium,
                    focusedTextColor = AmakaColors.textPrimary,
                    unfocusedTextColor = AmakaColors.textPrimary,
                    cursorColor = AmakaColors.accentBlue
                ),
                shape = RoundedCornerShape(AmakaCornerRadius.lg.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier.testTag("coach_send_button"),
                enabled = inputText.isNotBlank() && !state.isLoading
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank()) AmakaColors.accentBlue else AmakaColors.textTertiary
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 300.dp),
            color = if (message.isUser) AmakaColors.accentBlue else AmakaColors.surface,
            shape = RoundedCornerShape(
                topStart = AmakaCornerRadius.md.dp,
                topEnd = AmakaCornerRadius.md.dp,
                bottomStart = if (message.isUser) AmakaCornerRadius.md.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else AmakaCornerRadius.md.dp
            )
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = AmakaColors.textPrimary,
                modifier = Modifier.padding(AmakaSpacing.md.dp)
            )
        }

        // Suggestion chips
        if (message.suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
            ) {
                items(message.suggestions) { suggestion ->
                    SuggestionChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = {
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodySmall,
                                color = AmakaColors.accentBlue
                            )
                        },
                        modifier = Modifier.testTag("suggestion_chip"),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = AmakaColors.accentBlue.copy(alpha = 0.5f)
                        ),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = AmakaColors.accentBlue.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    }
}
