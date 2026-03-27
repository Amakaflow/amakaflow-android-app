package com.amakaflow.companion.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onNavigateToCrews: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
    onNavigateToChallenges: () -> Unit = {},
    onNavigateToLeaderboard: () -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showCommentSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("feed_screen")
    ) {
        // Top bar
        Text(
            "Social",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary,
            modifier = Modifier.padding(
                horizontal = AmakaSpacing.md.dp,
                vertical = AmakaSpacing.md.dp
            )
        )

        HorizontalDivider(color = AmakaColors.borderLight)

        // Crews entry (AMA-1277)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToCrews)
                .padding(horizontal = AmakaSpacing.md.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Group,
                contentDescription = null,
                tint = AmakaColors.accentBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Crews",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
                Text(
                    "Private training groups",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = AmakaColors.textTertiary
            )
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        // Challenges entry (AMA-1276)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToChallenges)
                .padding(horizontal = AmakaSpacing.md.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Challenges",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
                Text(
                    "Browse and join challenges",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = AmakaColors.textTertiary
            )
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        // Leaderboards entry (AMA-1278)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToLeaderboard)
                .padding(horizontal = AmakaSpacing.md.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = AmakaColors.accentBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Leaderboards",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
                Text(
                    "See how you rank among friends",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = AmakaColors.textTertiary
            )
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        when {
            state.isLoading && state.posts.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AmakaColors.accentBlue)
                }
            }
            state.posts.isEmpty() && state.error == null -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No posts yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = AmakaColors.textPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Follow other athletes to see their workouts here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(AmakaSpacing.md.dp),
                        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("feed_list")
                    ) {
                        items(state.posts, key = { it.id }) { post ->
                            FeedPostCard(
                                post = post,
                                onReact = { emoji -> viewModel.toggleReaction(post.id, emoji) },
                                onComment = {
                                    viewModel.loadComments(post.id)
                                    showCommentSheet = true
                                },
                                onTapUser = { userId -> onNavigateToUserProfile(userId) }
                            )
                        }

                        if (state.hasMore) {
                            item {
                                if (state.isLoadingMore) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(AmakaSpacing.md.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = AmakaColors.accentBlue
                                        )
                                    }
                                } else {
                                    LaunchedEffect(Unit) {
                                        viewModel.loadMore()
                                    }
                                }
                            }
                        }

                        // Bottom padding for nav bar
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }

        state.error?.let { error ->
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = AmakaColors.accentRed,
                modifier = Modifier.padding(AmakaSpacing.md.dp)
            )
        }
    }

    // Comment sheet
    if (showCommentSheet) {
        CommentSheet(
            comments = state.comments,
            isLoading = state.isLoadingComments,
            isPosting = state.isPostingComment,
            onPost = { text -> viewModel.postComment(text) },
            onDismiss = {
                showCommentSheet = false
                viewModel.dismissComments()
            }
        )
    }
}
