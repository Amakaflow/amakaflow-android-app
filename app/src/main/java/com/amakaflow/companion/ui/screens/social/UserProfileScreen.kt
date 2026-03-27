package com.amakaflow.companion.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amakaflow.companion.data.api.AmakaflowApi
import com.amakaflow.companion.data.model.UserPublicProfile
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit = {},
    api: AmakaflowApi? = null // injected via hiltViewModel in real usage
) {
    var profile by remember { mutableStateOf<UserPublicProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        api?.let {
            try {
                val response = it.getUserPublicProfile(userId)
                if (response.isSuccessful) {
                    profile = response.body()
                } else {
                    error = "Failed to load profile"
                }
            } catch (e: Exception) {
                error = e.message
            }
            isLoading = false
        } ?: run { isLoading = false; error = "API not available" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("user_profile_screen")
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
                "Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AmakaColors.accentBlue)
                }
            }
            profile != null -> {
                val p = profile!!
                LazyColumn(
                    contentPadding = PaddingValues(AmakaSpacing.md.dp),
                    verticalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Avatar + name
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(AmakaColors.surfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    p.userName.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AmakaColors.textSecondary
                                )
                            }

                            Spacer(Modifier.height(AmakaSpacing.sm.dp))

                            Text(
                                p.userName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AmakaColors.textPrimary
                            )

                            Spacer(Modifier.height(AmakaSpacing.sm.dp))

                            Button(
                                onClick = { /* TODO: follow/unfollow */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (p.isFollowing) AmakaColors.surfaceElevated else AmakaColors.accentBlue
                                ),
                                shape = RoundedCornerShape(AmakaCornerRadius.md.dp),
                                modifier = Modifier
                                    .width(120.dp)
                                    .testTag("follow_button")
                            ) {
                                Text(
                                    if (p.isFollowing) "Following" else "Follow",
                                    color = if (p.isFollowing) AmakaColors.textPrimary else AmakaColors.textPrimary
                                )
                            }
                        }
                    }

                    // Stats row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem("${p.workoutCount}", "Workouts")
                            StatItem(
                                if (p.totalVolume >= 1000) "%.0fk".format(p.totalVolume / 1000) else "${p.totalVolume.toInt()}",
                                "Volume"
                            )
                            StatItem("${p.streakDays}d", "Streak")
                        }
                    }

                    item {
                        HorizontalDivider(color = AmakaColors.borderLight)
                    }

                    // Recent workouts
                    if (p.recentWorkouts.isNotEmpty()) {
                        item {
                            Text(
                                "Recent Workouts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AmakaColors.textPrimary
                            )
                        }

                        items(p.recentWorkouts) { post ->
                            FeedPostCard(
                                post = post,
                                onReact = {},
                                onComment = {},
                                onTapUser = {}
                            )
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error ?: "Could not load profile", color = AmakaColors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = AmakaColors.textTertiary
        )
    }
}
