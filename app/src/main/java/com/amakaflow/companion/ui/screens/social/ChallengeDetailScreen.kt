package com.amakaflow.companion.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
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
import com.amakaflow.companion.data.model.*
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing

@Composable
fun ChallengeDetailScreen(
    challengeId: String,
    onNavigateBack: () -> Unit = {},
    viewModel: ChallengesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(challengeId) {
        viewModel.loadChallengeDetail(challengeId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("challenge_detail_screen")
    ) {
        Column(Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AmakaSpacing.sm.dp, vertical = AmakaSpacing.sm.dp),
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
                    "Challenge",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AmakaColors.textPrimary
                )
            }

            when {
                state.isLoadingDetail && state.selectedChallenge == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AmakaColors.accentBlue)
                    }
                }
                state.selectedChallenge != null -> {
                    val detail = state.selectedChallenge!!
                    LazyColumn(
                        contentPadding = PaddingValues(AmakaSpacing.md.dp),
                        verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)
                    ) {
                        // Header
                        item { ChallengeHeader(detail.challenge) }

                        // Progress
                        detail.myProgress?.let { progress ->
                            item { ProgressSection(progress, detail.challenge) }
                        }

                        // Join button
                        if (!detail.challenge.isJoined) {
                            item {
                                Button(
                                    onClick = { viewModel.joinChallenge(challengeId) },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    enabled = !state.isJoining,
                                    colors = ButtonDefaults.buttonColors(containerColor = AmakaColors.accentBlue),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (state.isJoining) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Join Challenge", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // Leaderboard header
                        if (detail.leaderboard.isNotEmpty()) {
                            item {
                                Text(
                                    "Leaderboard",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AmakaColors.textPrimary
                                )
                            }
                            items(detail.leaderboard, key = { it.id }) { entry ->
                                LeaderboardRow(entry, detail.challenge.type)
                            }
                        }
                    }
                }
                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Challenge not found", color = AmakaColors.textSecondary)
                    }
                }
            }
        }

        // Celebration overlay
        if (state.showCelebration && state.completedBadge != null) {
            ChallengeCompletionOverlay(
                badge = state.completedBadge!!,
                onDismiss = { viewModel.dismissCelebration() }
            )
        }
    }
}

@Composable
private fun ChallengeHeader(challenge: Challenge) {
    val typeColor = challengeTypeColor(challenge.type)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AmakaColors.surface)
            .padding(AmakaSpacing.md.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                challenge.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = typeColor
            ) {
                Text(
                    challenge.type.displayName,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        challenge.description?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = AmakaColors.textSecondary)
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InfoItem(label = "Participants", value = "${challenge.participantCount}")
            InfoItem(label = "Target", value = "${challenge.target.toInt()} ${challenge.targetUnit}")
            InfoItem(label = "End", value = challenge.endDate)
        }

        if (challenge.isTeamMode) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Groups,
                    contentDescription = null,
                    tint = AmakaColors.accentBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Team Challenge", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AmakaColors.accentBlue)
            }
        }
    }
}

@Composable
private fun ProgressSection(progress: ChallengeProgress, challenge: Challenge) {
    val typeColor = challengeTypeColor(challenge.type)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AmakaColors.surface)
            .padding(AmakaSpacing.md.dp)
    ) {
        Text(
            "My Progress",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = AmakaColors.textPrimary
        )
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AmakaColors.surfaceElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((progress.percentage / 100.0).toFloat().coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(8.dp))
                    .background(typeColor)
            )
            Text(
                "${progress.percentage.toInt()}%",
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${progress.currentValue.toInt()} / ${progress.targetValue.toInt()} ${challenge.targetUnit}",
                fontSize = 13.sp,
                color = AmakaColors.textSecondary
            )
            if (progress.isCompleted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = AmakaColors.accentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Completed!", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AmakaColors.accentGreen)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry, challengeType: ChallengeType) {
    val typeColor = challengeTypeColor(challengeType)
    val rankColor = when (entry.rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> AmakaColors.textSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "#${entry.rank}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = rankColor,
            modifier = Modifier.width(36.dp)
        )
        Spacer(Modifier.width(8.dp))

        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AmakaColors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(
                entry.userName.take(1).uppercase(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textSecondary
            )
        }
        Spacer(Modifier.width(8.dp))

        Text(
            entry.userName,
            style = MaterialTheme.typography.bodyMedium,
            color = AmakaColors.textPrimary,
            modifier = Modifier.weight(1f)
        )

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${entry.progressPercentage.toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
            LinearProgressIndicator(
                progress = { (entry.progressPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.width(60.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = typeColor,
                trackColor = AmakaColors.surfaceElevated
            )
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AmakaColors.textPrimary)
        Text(label, fontSize = 10.sp, color = AmakaColors.textTertiary)
    }
}
