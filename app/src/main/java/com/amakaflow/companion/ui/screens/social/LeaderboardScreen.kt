package com.amakaflow.companion.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.DimensionLeaderboardEntry
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit = {},
    crewId: String? = null,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(crewId) {
        if (crewId != null) {
            viewModel.crewId = crewId
            viewModel.changeScope(LeaderboardScope.CREW)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("leaderboard_screen")
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.md.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AmakaColors.textPrimary
                )
            }
            Text(
                "Leaderboards",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        // Scope tabs (only when no crew preset)
        if (crewId == null) {
            ScopeTabs(
                selected = state.selectedScope,
                onSelect = { viewModel.changeScope(it) }
            )
        }

        // Dimension tabs
        DimensionTabs(
            selected = state.selectedDimension,
            onSelect = { viewModel.changeDimension(it) }
        )

        // Period selector
        PeriodSelector(
            selected = state.selectedPeriod,
            onSelect = { viewModel.changePeriod(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = AmakaColors.borderLight)

        // Content
        if (state.isLoading && state.entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AmakaColors.accentBlue)
            }
        } else if (state.error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    state.error ?: "",
                    color = AmakaColors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(AmakaSpacing.lg.dp)
                )
            }
        } else if (state.entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No data yet", fontWeight = FontWeight.Bold, color = AmakaColors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Complete workouts to see rankings", color = AmakaColors.textTertiary, fontSize = 14.sp)
                }
            }
        } else {
            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.loadLeaderboard() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.entries, key = { it.userId }) { entry ->
                        LeaderboardEntryRow(
                            entry = entry,
                            formattedValue = viewModel.formattedValue(entry)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeTabs(selected: LeaderboardScope, onSelect: (LeaderboardScope) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.sm.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LeaderboardScope.entries.forEach { scope ->
            val isSelected = scope == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) AmakaColors.accentBlue else AmakaColors.surface)
                    .clickable { onSelect(scope) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    scope.displayName,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) AmakaColors.background else AmakaColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun DimensionTabs(selected: LeaderboardDimension, onSelect: (LeaderboardDimension) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.sm.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LeaderboardDimension.entries.forEach { dim ->
            val isSelected = dim == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) AmakaColors.accentBlue.copy(alpha = 0.12f) else AmakaColors.surface.copy(alpha = 0f))
                    .then(
                        if (isSelected) Modifier.border(1.dp, AmakaColors.accentBlue.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        else Modifier
                    )
                    .clickable { onSelect(dim) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    dim.displayName,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) AmakaColors.accentBlue else AmakaColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun PeriodSelector(selected: LeaderboardPeriod, onSelect: (LeaderboardPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AmakaSpacing.md.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AmakaColors.background)
            .padding(4.dp)
    ) {
        LeaderboardPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) AmakaColors.surface else AmakaColors.background.copy(alpha = 0f))
                    .clickable { onSelect(period) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    period.displayName,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) AmakaColors.textPrimary else AmakaColors.textTertiary
                )
            }
        }
    }
}

@Composable
fun LeaderboardEntryRow(entry: DimensionLeaderboardEntry, formattedValue: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (entry.isMe) AmakaColors.accentBlue.copy(alpha = 0.08f) else AmakaColors.background.copy(alpha = 0f)
            )
            .padding(horizontal = AmakaSpacing.md.dp, vertical = AmakaSpacing.sm.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
    ) {
        // Rank badge
        when (entry.rank) {
            1 -> RankMedal(emoji = "\uD83E\uDD47") // Gold
            2 -> RankMedal(emoji = "\uD83E\uDD48") // Silver
            3 -> RankMedal(emoji = "\uD83E\uDD49") // Bronze
            else -> Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${entry.rank}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmakaColors.textSecondary
                )
            }
        }

        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (entry.isMe) AmakaColors.accentBlue.copy(alpha = 0.2f) else AmakaColors.textTertiary.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                entry.displayName.take(1).uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (entry.isMe) AmakaColors.accentBlue else AmakaColors.textTertiary
            )
        }

        // Name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (entry.isMe) "You" else entry.displayName,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary
            )
            if (entry.isMe) {
                Text(
                    "That's you!",
                    fontSize = 11.sp,
                    color = AmakaColors.accentBlue
                )
            }
        }

        // Value
        Text(
            formattedValue,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (entry.isMe) AmakaColors.accentBlue else AmakaColors.textPrimary
        )
    }
}

@Composable
private fun RankMedal(emoji: String) {
    Box(
        modifier = Modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 22.sp)
    }
}
