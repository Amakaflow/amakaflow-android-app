package com.amakaflow.companion.ui.screens.social

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.data.model.CrewFeedPost
import com.amakaflow.companion.data.model.CrewMember
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrewDetailScreen(
    crewId: String,
    onNavigateBack: () -> Unit = {},
    viewModel: CrewsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(crewId) {
        viewModel.loadCrewDetail(crewId)
        viewModel.loadCrewFeed(crewId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("crew_detail_screen")
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
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AmakaColors.textPrimary
                )
            }
            Text(
                state.selectedCrewDetail?.name ?: "Crew",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = AmakaColors.textPrimary
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Copy Invite Code") },
                        onClick = {
                            showMenu = false
                            state.selectedCrewDetail?.inviteCode?.let { code ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Invite Code", code))
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Leave Crew", color = AmakaColors.accentRed) },
                        onClick = {
                            showMenu = false
                            showLeaveDialog = true
                        }
                    )
                }
            }
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        if (state.isLoadingDetail && state.selectedCrewDetail == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AmakaColors.accentBlue)
            }
        } else {
            state.selectedCrewDetail?.let { detail ->
                // Header info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AmakaSpacing.md.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    detail.description?.let { desc ->
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AmakaColors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(AmakaSpacing.sm.dp))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${detail.memberCount}/${detail.maxMembers} members",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmakaColors.textSecondary
                        )
                        Text(
                            detail.inviteCode,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AmakaColors.accentBlue
                        )
                    }
                }

                // Tab row
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = AmakaColors.background,
                    contentColor = AmakaColors.accentBlue
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text("Feed") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text("Members") }
                    )
                }

                HorizontalPager(state = pagerState) { page ->
                    when (page) {
                        0 -> FeedTab(state.crewFeedPosts, state.isLoadingFeed)
                        1 -> MembersTab(detail.members)
                    }
                }
            }
        }
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Crew") },
            text = { Text("Are you sure you want to leave this crew?") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    viewModel.leaveCrew(crewId)
                    onNavigateBack()
                }) {
                    Text("Leave", color = AmakaColors.accentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FeedTab(posts: List<CrewFeedPost>, isLoading: Boolean) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AmakaColors.accentBlue)
        }
    } else if (posts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No activity yet",
                style = MaterialTheme.typography.bodyMedium,
                color = AmakaColors.textSecondary
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(AmakaSpacing.sm.dp),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
        ) {
            items(posts) { post ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmakaColors.surface)
                        .padding(AmakaSpacing.md.dp)
                ) {
                    Text(
                        post.workoutName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AmakaColors.textPrimary
                    )
                    if (post.prBadges.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${post.prBadges.size} PR${if (post.prBadges.size == 1) "" else "s"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFFD700)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        post.createdAt,
                        fontSize = 11.sp,
                        color = AmakaColors.textTertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun MembersTab(members: List<CrewMember>) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = AmakaSpacing.sm.dp)
    ) {
        items(members) { member ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AmakaSpacing.md.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (member.isAdmin) "Admin" else "Member",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (member.isAdmin) Color(0xFFFFD700) else AmakaColors.textTertiary,
                    modifier = Modifier.width(52.dp)
                )
                Spacer(Modifier.width(AmakaSpacing.sm.dp))
                Text(
                    member.userId,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmakaColors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = AmakaSpacing.md.dp),
                color = AmakaColors.borderLight
            )
        }
    }
}
