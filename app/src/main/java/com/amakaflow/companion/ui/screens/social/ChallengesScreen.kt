package com.amakaflow.companion.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.Challenge
import com.amakaflow.companion.data.model.ChallengeType
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing

fun challengeTypeColor(type: ChallengeType): Color = when (type) {
    ChallengeType.VOLUME -> AmakaColors.accentBlue
    ChallengeType.CONSISTENCY -> AmakaColors.accentGreen
    ChallengeType.PR -> Color(0xFFFFD700) // Gold
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    viewModel: ChallengesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("challenges_screen")
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
                "Challenges",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNavigateToCreate) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Create Challenge",
                    tint = AmakaColors.accentBlue
                )
            }
        }

        // Filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = AmakaSpacing.md.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = AmakaSpacing.sm.dp)
        ) {
            item {
                FilterChipItem(
                    label = "All",
                    isSelected = state.selectedTypeFilter == null,
                    color = AmakaColors.accentBlue,
                    onClick = { viewModel.setTypeFilter(null) }
                )
            }
            items(ChallengeType.entries) { type ->
                FilterChipItem(
                    label = type.displayName,
                    isSelected = state.selectedTypeFilter == type,
                    color = challengeTypeColor(type),
                    onClick = { viewModel.setTypeFilter(type) }
                )
            }
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        when {
            state.isLoading && state.challenges.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AmakaColors.accentBlue)
                }
            }
            state.challenges.isEmpty() && state.error == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No challenges yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = AmakaColors.textPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Create a challenge or wait for one to be posted.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToCreate,
                            colors = ButtonDefaults.buttonColors(containerColor = AmakaColors.accentBlue)
                        ) {
                            Text("Create Challenge")
                        }
                    }
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.loadChallenges() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = AmakaSpacing.md.dp,
                            vertical = AmakaSpacing.sm.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
                    ) {
                        items(state.filteredChallenges, key = { it.id }) { challenge ->
                            ChallengeCard(
                                challenge = challenge,
                                onClick = { onNavigateToDetail(challenge.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeCard(challenge: Challenge, onClick: () -> Unit) {
    val typeColor = challengeTypeColor(challenge.type)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, typeColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .background(AmakaColors.surface)
            .clickable(onClick = onClick)
            .padding(AmakaSpacing.md.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    challenge.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
                Text(
                    challenge.type.displayName + if (challenge.isTeamMode) " \u00B7 Team" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${challenge.participantCount}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
                Text(
                    "participants",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmakaColors.textTertiary
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (challenge.isJoined && challenge.myProgressPercentage != null) {
            LinearProgressIndicator(
                progress = { (challenge.myProgressPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = typeColor,
                trackColor = AmakaColors.surfaceElevated
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${challenge.myProgressPercentage.toInt()}% complete",
                    fontSize = 12.sp,
                    color = AmakaColors.textSecondary
                )
                Text(
                    challenge.endDate,
                    fontSize = 12.sp,
                    color = AmakaColors.textTertiary
                )
            }
        } else if (!challenge.isJoined) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${challenge.startDate} - ${challenge.endDate}",
                    fontSize = 12.sp,
                    color = AmakaColors.textSecondary
                )
                Text(
                    "Tap to join",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = typeColor
                )
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) color else AmakaColors.surfaceElevated,
        onClick = onClick
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else AmakaColors.textSecondary
        )
    }
}
