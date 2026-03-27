package com.amakaflow.companion.ui.screens.social

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.amakaflow.companion.data.model.SocialFeedReaction
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaCornerRadius

private val emojiOptions = listOf(
    "heart" to "\u2764\uFE0F",
    "fire" to "\uD83D\uDD25",
    "muscle" to "\uD83D\uDCAA"
)

@Composable
fun ReactionBar(
    reactions: List<SocialFeedReaction>,
    userReactions: List<String>,
    onReact: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        emojiOptions.forEach { (key, display) ->
            val count = reactions.find { it.emoji == key }?.count ?: 0
            val isActive = userReactions.contains(key)

            Surface(
                onClick = { onReact(key) },
                modifier = Modifier
                    .testTag("reaction_$key")
                    .then(
                        if (isActive) Modifier.border(
                            1.dp,
                            AmakaColors.accentBlue.copy(alpha = 0.4f),
                            RoundedCornerShape(AmakaCornerRadius.sm.dp)
                        ) else Modifier
                    ),
                shape = RoundedCornerShape(AmakaCornerRadius.sm.dp),
                color = if (isActive) AmakaColors.accentBlue.copy(alpha = 0.15f) else AmakaColors.surfaceElevated
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(display, style = MaterialTheme.typography.bodyMedium)
                    if (count > 0) {
                        Text(
                            "$count",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) AmakaColors.accentBlue else AmakaColors.textTertiary
                        )
                    }
                }
            }
        }
    }
}
