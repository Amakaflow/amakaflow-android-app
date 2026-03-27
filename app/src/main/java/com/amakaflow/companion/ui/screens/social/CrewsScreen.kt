package com.amakaflow.companion.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.Crew
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrewsScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToJoin: () -> Unit = {},
    viewModel: CrewsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("crews_screen")
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
                "Crews",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNavigateToCreate) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Create Crew",
                    tint = AmakaColors.accentBlue
                )
            }
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        if (state.isLoading && state.crews.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AmakaColors.accentBlue)
            }
        } else if (state.crews.isEmpty() && state.error == null) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AmakaSpacing.xl.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.Group,
                    contentDescription = null,
                    tint = AmakaColors.accentBlue,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(AmakaSpacing.md.dp))
                Text(
                    "No crews yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AmakaColors.textPrimary
                )
                Spacer(Modifier.height(AmakaSpacing.sm.dp))
                Text(
                    "Create a training crew or join one with an invite code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmakaColors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(AmakaSpacing.lg.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)) {
                    Button(onClick = onNavigateToCreate) {
                        Text("Create")
                    }
                    OutlinedButton(onClick = onNavigateToJoin) {
                        Text("Join")
                    }
                }
            }
        } else {
            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.loadCrews() }
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = AmakaSpacing.sm.dp),
                    verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
                ) {
                    state.error?.let { error ->
                        item {
                            Text(
                                error,
                                color = AmakaColors.accentRed,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = AmakaSpacing.md.dp)
                            )
                        }
                    }

                    items(state.crews) { crew ->
                        CrewCard(
                            crew = crew,
                            onClick = { onNavigateToDetail(crew.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CrewCard(
    crew: Crew,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AmakaSpacing.md.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, AmakaColors.accentBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .background(AmakaColors.surface)
            .clickable(onClick = onClick)
            .padding(AmakaSpacing.md.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    crew.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AmakaColors.textPrimary
                )
                crew.description?.let { desc ->
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = AmakaColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${crew.memberCount}/${crew.maxMembers}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AmakaColors.textPrimary
                )
                Text(
                    "members",
                    fontSize = 10.sp,
                    color = AmakaColors.textTertiary
                )
            }
        }
    }
}
