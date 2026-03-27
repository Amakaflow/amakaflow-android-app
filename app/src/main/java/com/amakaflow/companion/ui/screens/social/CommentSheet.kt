package com.amakaflow.companion.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amakaflow.companion.data.model.SocialComment
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentSheet(
    comments: List<SocialComment>,
    isLoading: Boolean,
    isPosting: Boolean,
    onPost: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AmakaColors.background,
        modifier = modifier.testTag("comment_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp, max = 500.dp)
        ) {
            Text(
                "Comments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.sm.dp)
            )

            HorizontalDivider(color = AmakaColors.borderLight)

            when {
                isLoading -> {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AmakaColors.accentBlue)
                    }
                }
                comments.isEmpty() -> {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No comments yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AmakaColors.textSecondary
                            )
                            Text(
                                "Be the first to comment!",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmakaColors.textTertiary
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(AmakaSpacing.md.dp),
                        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
                    ) {
                        items(comments) { comment ->
                            CommentRow(comment)
                        }
                    }
                }
            }

            HorizontalDivider(color = AmakaColors.borderLight)

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AmakaColors.surface)
                    .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.sm.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Add a comment...", color = AmakaColors.textTertiary) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = AmakaColors.surfaceElevated,
                        unfocusedContainerColor = AmakaColors.surfaceElevated,
                        focusedTextColor = AmakaColors.textPrimary,
                        unfocusedTextColor = AmakaColors.textPrimary
                    ),
                    shape = RoundedCornerShape(AmakaCornerRadius.md.dp),
                    singleLine = true
                )

                Spacer(Modifier.width(AmakaSpacing.sm.dp))

                IconButton(
                    onClick = {
                        onPost(commentText)
                        commentText = ""
                    },
                    enabled = commentText.isNotBlank() && !isPosting,
                    modifier = Modifier.testTag("send_comment_button")
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AmakaColors.accentBlue
                        )
                    } else {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (commentText.isNotBlank()) AmakaColors.accentBlue else AmakaColors.textTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: SocialComment) {
    Row(horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AmakaColors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                comment.userName.take(1).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textSecondary
            )
        }

        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    comment.userName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
                Text(
                    comment.createdAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = AmakaColors.textTertiary
                )
            }
            Text(
                comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = AmakaColors.textPrimary
            )
        }
    }
}
