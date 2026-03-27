package com.amakaflow.companion.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.amakaflow.companion.ui.theme.AmakaColors
import com.amakaflow.companion.ui.theme.AmakaSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinCrewScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: CrewsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var inviteCode by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AmakaColors.background)
            .testTag("join_crew_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
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
                "Join Crew",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AmakaColors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider(color = AmakaColors.borderLight)

        Spacer(Modifier.weight(1f))

        Icon(
            Icons.Filled.PersonAdd,
            contentDescription = null,
            tint = AmakaColors.accentBlue,
            modifier = Modifier.size(64.dp)
        )

        Spacer(Modifier.height(AmakaSpacing.md.dp))

        Text(
            "Join a Crew",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AmakaColors.textPrimary
        )

        Spacer(Modifier.height(AmakaSpacing.sm.dp))

        Text(
            "Enter the 8-character invite code\nshared by a crew member.",
            style = MaterialTheme.typography.bodyMedium,
            color = AmakaColors.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(AmakaSpacing.lg.dp))

        OutlinedTextField(
            value = inviteCode,
            onValueChange = {
                if (it.text.length <= 8) {
                    inviteCode = it.copy(text = it.text.uppercase())
                }
            },
            label = { Text("Invite Code") },
            singleLine = true,
            modifier = Modifier
                .padding(horizontal = AmakaSpacing.xl.dp)
                .fillMaxWidth()
        )

        Spacer(Modifier.height(AmakaSpacing.md.dp))

        state.joinError?.let { error ->
            Text(
                error,
                color = AmakaColors.accentRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = AmakaSpacing.xl.dp)
            )
            Spacer(Modifier.height(AmakaSpacing.sm.dp))
        }

        if (state.joinSuccess) {
            Text(
                "Joined successfully!",
                color = AmakaColors.accentGreen,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(AmakaSpacing.sm.dp))
        }

        Button(
            onClick = {
                viewModel.joinCrew(inviteCode.text, inviteCode.text)
            },
            enabled = inviteCode.text.length == 8 && !state.isJoining,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AmakaSpacing.xl.dp)
        ) {
            if (state.isJoining) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = AmakaColors.textPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Join Crew")
            }
        }

        Spacer(Modifier.weight(1f))
    }
}
