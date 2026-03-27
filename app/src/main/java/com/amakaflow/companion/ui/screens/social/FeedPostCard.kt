package com.amakaflow.companion.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amakaflow.companion.data.model.SocialFeedPost
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius
import com.amakaflow.companion.ui.theme.AmakaSpacing

private val goldColor = Color(0xFFD4A017)

@Composable
fun FeedPostCard(
    post: SocialFeedPost,
    onReact: (String) -> Unit,
    onComment: () -> Unit,
    onTapUser: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("feed_post_${post.id}"),
        color = AmakaColors.surface,
        shape = RoundedCornerShape(AmakaCornerRadius.lg.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AmakaColors.borderLight)
    ) {
        Column(
            modifier = Modifier.padding(AmakaSpacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AmakaColors.surfaceElevated)
                        .clickable { onTapUser(post.userId) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        post.userName.take(1).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AmakaColors.textSecondary
                    )
                }

                Spacer(Modifier.width(AmakaSpacing.sm.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        post.userName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AmakaColors.textPrimary,
                        modifier = Modifier.clickable { onTapUser(post.userId) }
                    )
                    Text(
                        post.postedAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = AmakaColors.textTertiary
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = AmakaColors.textTertiary
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Block User") }, onClick = { showMenu = false })
                        DropdownMenuItem(text = { Text("Report Post") }, onClick = { showMenu = false })
                    }
                }
            }

            // Workout name
            Text(
                post.workoutName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = AmakaColors.textPrimary
            )

            // Exercises (condensed, max 4)
            if (post.exercises.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    post.exercises.take(4).forEach { exercise ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                exercise.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = AmakaColors.textSecondary
                            )
                            if (exercise.sets != null && exercise.reps != null) {
                                Text(
                                    "${exercise.sets}x${exercise.reps}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmakaColors.textTertiary
                                )
                            }
                            exercise.weight?.let { w ->
                                if (w > 0) {
                                    Text(
                                        "@ ${w.toInt()}kg",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AmakaColors.textTertiary
                                    )
                                }
                            }
                        }
                    }
                    if (post.exercises.size > 4) {
                        Text(
                            "+${post.exercises.size - 4} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmakaColors.textTertiary
                        )
                    }
                }
            }

            // Stats row
            Row(horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.md.dp)) {
                post.totalVolume?.let { vol ->
                    if (vol > 0) {
                        Text(
                            if (vol >= 1000) "%.1fk kg".format(vol / 1000) else "${vol.toInt()} kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmakaColors.textSecondary
                        )
                    }
                }
                val minutes = post.durationSeconds / 60
                Text(
                    if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmakaColors.textSecondary
                )
            }

            // PR badges
            if (post.personalRecords.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AmakaSpacing.sm.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    post.personalRecords.forEach { pr ->
                        Surface(
                            color = goldColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(AmakaCornerRadius.sm.dp)
                        ) {
                            Text(
                                "\uD83C\uDFC6 ${pr.exerciseName} ${pr.metric}: ${pr.value}",
                                style = MaterialTheme.typography.labelSmall,
                                color = goldColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Reaction bar + comment count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ReactionBar(
                    reactions = post.reactions,
                    userReactions = post.userReactions,
                    onReact = onReact
                )

                Spacer(Modifier.weight(1f))

                TextButton(onClick = onComment) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comments",
                        modifier = Modifier.size(16.dp),
                        tint = AmakaColors.textSecondary
                    )
                    if (post.commentCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${post.commentCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmakaColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}
