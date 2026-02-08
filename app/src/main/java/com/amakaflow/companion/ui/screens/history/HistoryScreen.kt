package com.amakaflow.companion.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.CompletionSource
import com.amakaflow.companion.data.model.WeeklySummary
import com.amakaflow.companion.data.model.WorkoutCompletion
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToCompletionDetail: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Calculate this week's stats
    val weeklyStats = remember(uiState.completions) {
        WeeklySummary.fromCompletions(uiState.completions)
    }

    // Detect when scrolled to bottom for pagination
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 3 && uiState.canLoadMore
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMore()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("history_screen")
    ) {
        // Header with back button and filter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.sm.dp, vertical = AmakaSpacing.sm.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AmakaColors.textPrimary
                )
            }

            Surface(
                color = AmakaColors.surface,
                shape = RoundedCornerShape(AmakaCornerRadius.lg.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clickable { /* TODO: Show filter options */ }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = null,
                        tint = AmakaColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Filter",
                        style = MaterialTheme.typography.labelMedium,
                        color = AmakaColors.textPrimary
                    )
                }
            }
        }

        // Title
        Text(
            text = "Activity History",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary,
            modifier = Modifier.padding(horizontal = AmakaSpacing.md.dp)
        )

        Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))

        when {
            uiState.isLoading && !uiState.isRefreshing -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AmakaColors.accentBlue)
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (uiState.groupedCompletions.isEmpty() && !uiState.isLoading) {
                        // Show empty state for both empty data and errors (graceful degradation)
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = AmakaSpacing.md.dp)
                        ) {
                            // This Week stats card (even when empty)
                            item {
                                ThisWeekStatsCard(
                                    workoutCount = 0,
                                    totalTime = "0m",
                                    calories = 0
                                )
                                Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))
                            }

                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No workouts yet",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = AmakaColors.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
                                        Text(
                                            text = "Complete a workout to see it here",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AmakaColors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = AmakaSpacing.md.dp),
                            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
                        ) {
                            // This Week stats card
                            item {
                                ThisWeekStatsCard(
                                    workoutCount = weeklyStats.workoutCount,
                                    totalTime = weeklyStats.formattedDuration,
                                    calories = weeklyStats.totalCalories
                                )
                                Spacer(modifier = Modifier.height(AmakaSpacing.md.dp))
                            }

                            var completionIndex = 0
                            uiState.groupedCompletions.forEach { (category, completions) ->
                                item {
                                    Text(
                                        text = category.title.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AmakaColors.textTertiary,
                                        modifier = Modifier.padding(
                                            top = AmakaSpacing.md.dp,
                                            bottom = AmakaSpacing.sm.dp
                                        )
                                    )
                                }
                                val startIndex = completionIndex
                                itemsIndexed(completions) { index, completion ->
                                    CompletionListItem(
                                        completion = completion,
                                        index = startIndex + index,
                                        onClick = { onNavigateToCompletionDetail(completion.id) }
                                    )
                                }
                                completionIndex += completions.size
                            }

                            // Loading more indicator
                            if (uiState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(AmakaSpacing.md.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = AmakaColors.accentBlue,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(AmakaSpacing.xl.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThisWeekStatsCard(
    workoutCount: Int,
    totalTime: String,
    calories: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Column(
            modifier = Modifier.padding(AmakaSpacing.md.dp)
        ) {
            Text(
                text = "THIS WEEK",
                style = MaterialTheme.typography.labelMedium,
                color = AmakaColors.textTertiary
            )
            Spacer(modifier = Modifier.height(AmakaSpacing.sm.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(value = workoutCount.toString(), label = "workouts")
                StatColumn(value = totalTime, label = "total time")
                StatColumn(value = calories.toString(), label = "kcal")
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AmakaColors.textTertiary
        )
    }
}

@Composable
private fun CompletionListItem(
    completion: WorkoutCompletion,
    index: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AmakaCornerRadius.md.dp))
            .clickable(onClick = onClick)
            .testTag("completion_item_$index"),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.md.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AmakaSpacing.md.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Green checkmark
            Surface(
                modifier = Modifier.size(32.dp),
                color = AmakaColors.accentGreen,
                shape = CircleShape
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = AmakaColors.background,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(AmakaSpacing.md.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Workout name and time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = completion.workoutName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AmakaColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(AmakaSpacing.sm.dp))
                    Text(
                        text = completion.formattedStartTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textTertiary
                    )
                }

                Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))

                // Metrics row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Duration
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = AmakaColors.textTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = completion.formattedDuration,
                            style = MaterialTheme.typography.bodySmall,
                            color = AmakaColors.textSecondary
                        )
                    }

                    // Heart Rate
                    completion.avgHeartRate?.let { hr ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = AmakaColors.accentRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = hr.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = AmakaColors.textSecondary
                            )
                        }
                    }

                    // Calories
                    completion.activeCalories?.let { cal ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = AmakaColors.accentOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = cal.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = AmakaColors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AmakaSpacing.xs.dp))

                // Source device
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (completion.source) {
                            CompletionSource.PHONE -> Icons.Filled.Smartphone
                            else -> Icons.Filled.Watch
                        },
                        contentDescription = null,
                        tint = AmakaColors.textTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = completion.source.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = AmakaColors.textTertiary
                    )
                }
            }
        }
    }
}
