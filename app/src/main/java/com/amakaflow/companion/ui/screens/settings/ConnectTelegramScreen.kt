package com.amakaflow.companion.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * Connect Telegram flow screen — AMA-1758 Phase C.
 *
 * Mints a one-time pairing token, deep-links the user into @amakaflow_userbot,
 * and polls /link-status until the bot consumes the token.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConnectTelegramScreen(
    onDismiss: () -> Unit,
    viewModel: ConnectTelegramViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val launchTelegram by viewModel.launchTelegram.collectAsState()
    val context = LocalContext.current

    // Fire the Telegram intent exactly once when the viewmodel signals it.
    LaunchedEffect(launchTelegram) {
        val target = launchTelegram ?: return@LaunchedEffect
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Fall back to the https deep link if the user doesn't have Telegram installed.
            val httpsLink = (state as? ConnectTelegramUiState.AwaitingPair)?.deepLink
            if (httpsLink != null) {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(httpsLink)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }
        } finally {
            viewModel.onTelegramLaunched()
        }
    }

    Scaffold(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag("connect-telegram-screen"),
        topBar = {
            TopAppBar(
                title = { Text("Connect Telegram") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val s = state) {
                is ConnectTelegramUiState.Idle -> IdleContent(onConnect = viewModel::startConnect)
                is ConnectTelegramUiState.Minting -> Centered { CircularProgressIndicator() }
                is ConnectTelegramUiState.AwaitingPair -> AwaitingPairContent(
                    secondsRemaining = s.secondsRemaining,
                    onCancel = viewModel::cancel,
                )
                is ConnectTelegramUiState.Connected -> ConnectedContent(
                    telegramId = s.telegramId,
                    onDone = {
                        viewModel.dismissTerminal()
                        onDismiss()
                    },
                )
                ConnectTelegramUiState.Expired -> TerminalContent(
                    icon = Icons.Filled.ErrorOutline,
                    title = "Link expired",
                    message = "The pairing link expired before Telegram confirmed it. Try again.",
                    primaryLabel = "Try again",
                    onPrimary = {
                        viewModel.dismissTerminal()
                        viewModel.startConnect()
                    },
                    secondaryLabel = "Close",
                    onSecondary = {
                        viewModel.dismissTerminal()
                        onDismiss()
                    },
                )
                is ConnectTelegramUiState.Error -> TerminalContent(
                    icon = Icons.Filled.ErrorOutline,
                    title = "Couldn't connect",
                    message = s.message,
                    primaryLabel = "Retry",
                    onPrimary = {
                        viewModel.dismissTerminal()
                        viewModel.startConnect()
                    },
                    secondaryLabel = "Close",
                    onSecondary = {
                        viewModel.dismissTerminal()
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun IdleContent(onConnect: () -> Unit) {
    Text(
        text = "Get your daily plan, log workouts, and chat with the coach over Telegram.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Text(
        text = "We'll open Telegram and link your AmakaFlow account to @amakaflow_userbot. Takes about 5 seconds.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(
        onClick = onConnect,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("connect-telegram-button"),
    ) {
        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Connect Telegram")
    }
}

@Composable
private fun AwaitingPairContent(secondsRemaining: Int, onCancel: () -> Unit) {
    CircularProgressIndicator()
    Text(
        text = "Waiting for Telegram…",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = "Tap Start in Telegram to finish linking. We'll detect it automatically.",
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = "Times out in ${secondsRemaining}s.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(
        onClick = onCancel,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("connect-telegram-cancel"),
    ) {
        Text("Cancel")
    }
}

@Composable
private fun ConnectedContent(telegramId: Long, onDone: () -> Unit) {
    Centered {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
    }
    Text(
        text = "Connected!",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = "Telegram chat #$telegramId is now linked. Your coach will message you here.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = onDone,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("connect-telegram-done"),
    ) {
        Text("Done")
    }
}

@Composable
private fun TerminalContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
) {
    Centered {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(72.dp),
        )
    }
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = onPrimary,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(primaryLabel) }
    OutlinedButton(
        onClick = onSecondary,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(secondaryLabel) }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}
