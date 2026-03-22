package com.amakaflow.companion.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.ActivityFeedItem
import com.amakaflow.companion.data.model.ActivityType
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

@Composable
fun ActivityFeedScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ActivityFeedViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("activity_feed_screen")
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
                text = "Activity Feed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AmakaColors.accentBlue)
                }
            }
            state.items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No activity yet.\nComplete a workout to see your feed!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AmakaColors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(AmakaSpacing.md.dp),
                    verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
                ) {
                    items(state.items) { item ->
                        ActivityFeedItemCard(item = item)
                    }

                    if (state.hasMore) {
                        item {
                            TextButton(
                                onClick = { viewModel.loadMore() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("load_more_button")
                            ) {
                                Text("Load more", color = AmakaColors.accentBlue)
                            }
                        }
                    }
                }
            }
        }

        state.error?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = AmakaColors.accentRed,
                modifier = Modifier.padding(AmakaSpacing.md.dp)
            )
        }
    }
}

@Composable
private fun ActivityFeedItemCard(item: ActivityFeedItem) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("feed_item_${item.id}"),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(40.dp),
                color = activityTypeColor(item.type).copy(alpha = 0.15f),
                shape = CircleShape
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = activityTypeIcon(item.type),
                        contentDescription = null,
                        tint = activityTypeColor(item.type),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = AmakaColors.textPrimary
                )
                item.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = AmakaColors.textTertiary
                )
            }
        }
    }
}

private fun activityTypeIcon(type: ActivityType): ImageVector {
    return when (type) {
        ActivityType.WORKOUT_COMPLETED -> Icons.Filled.FitnessCenter
        ActivityType.PLAN_GENERATED -> Icons.Filled.AutoAwesome
        ActivityType.STREAK_MILESTONE -> Icons.Filled.EmojiEvents
        ActivityType.COACH_INSIGHT -> Icons.Filled.Psychology
        ActivityType.PR_ACHIEVED -> Icons.AutoMirrored.Filled.TrendingUp
        ActivityType.REST_DAY -> Icons.Filled.Hotel
    }
}

private fun activityTypeColor(type: ActivityType): androidx.compose.ui.graphics.Color {
    return when (type) {
        ActivityType.WORKOUT_COMPLETED -> AmakaColors.accentGreen
        ActivityType.PLAN_GENERATED -> AmakaColors.accentBlue
        ActivityType.STREAK_MILESTONE -> AmakaColors.accentYellow
        ActivityType.COACH_INSIGHT -> AmakaColors.accentPurple
        ActivityType.PR_ACHIEVED -> AmakaColors.accentOrange
        ActivityType.REST_DAY -> AmakaColors.textTertiary
    }
}
