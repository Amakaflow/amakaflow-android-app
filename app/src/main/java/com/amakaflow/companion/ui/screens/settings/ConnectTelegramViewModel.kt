package com.amakaflow.companion.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amakaflow.companion.data.api.AmakaflowApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State machine for the "Connect Telegram" Settings flow (AMA-1758 Phase C).
 *
 * Flow: Idle → Minting → AwaitingUserTap → Polling → Connected | Expired | Error.
 * Backend contract is on mapper-api: POST /api/telegram/link-token mints a 15-min one-time
 * token; GET /api/telegram/link-status?token=... polls until the bot consumes it.
 */
sealed interface ConnectTelegramUiState {
    /** Initial state and the state we return to after a successful connection or cancel. */
    data object Idle : ConnectTelegramUiState

    /** Hitting the mint endpoint. UI should show a spinner on the connect button. */
    data object Minting : ConnectTelegramUiState

    /**
     * Token minted; the screen has launched the Telegram intent and is waiting for the user
     * to tap Start. We poll /link-status until [linked=true] or we time out.
     */
    data class AwaitingPair(
        val token: String,
        val deepLink: String,
        val nativeLink: String,
        val secondsRemaining: Int,
    ) : ConnectTelegramUiState

    /** Pairing succeeded. */
    data class Connected(val telegramId: Long) : ConnectTelegramUiState

    /** The 15-min token expired before consumption — user can retry from Idle. */
    data object Expired : ConnectTelegramUiState

    /** Network or 5xx during mint or poll — user can retry from Idle. */
    data class Error(val message: String) : ConnectTelegramUiState
}

@HiltViewModel
class ConnectTelegramViewModel @Inject constructor(
    private val api: AmakaflowApi,
) : ViewModel() {

    private val _state = MutableStateFlow<ConnectTelegramUiState>(ConnectTelegramUiState.Idle)
    val state: StateFlow<ConnectTelegramUiState> = _state.asStateFlow()

    /** One-shot signal to the screen: "open this URL in Telegram". Cleared after consumption. */
    private val _launchTelegram = MutableStateFlow<String?>(null)
    val launchTelegram: StateFlow<String?> = _launchTelegram.asStateFlow()

    private var connectJob: Job? = null
    private var pollJob: Job? = null

    /** Begin the connect flow: mint a token, surface the native_link to the screen, start polling. */
    fun startConnect() {
        if (_state.value is ConnectTelegramUiState.Minting ||
            _state.value is ConnectTelegramUiState.AwaitingPair
        ) {
            return
        }
        _state.value = ConnectTelegramUiState.Minting
        connectJob?.cancel()
        connectJob = viewModelScope.launch {
            val resp = runCatching { api.mintTelegramLinkToken() }.getOrNull()
            // If the user cancelled while the mint request was in flight, bail before
            // touching state / launching the Telegram intent.
            if (_state.value !is ConnectTelegramUiState.Minting) return@launch
            val body = resp?.body()
            if (resp == null || !resp.isSuccessful || body == null) {
                _state.value = ConnectTelegramUiState.Error(
                    message = "Couldn't start Telegram link (${resp?.code() ?: "network error"}). Try again."
                )
                return@launch
            }
            // Use the server's TTL (clamped to a sane max so a misconfigured server can't
            // cause indefinite polling). This avoids the false-Expired bug when the user
            // came back to the app after a brief background.
            val pollWindowSeconds = body.expiresInSeconds
                .coerceAtLeast(1L)
                .coerceAtMost(MAX_POLL_WINDOW_SECONDS.toLong())
                .toInt()
            _state.value = ConnectTelegramUiState.AwaitingPair(
                token = body.token,
                deepLink = body.deepLink,
                nativeLink = body.nativeLink,
                secondsRemaining = pollWindowSeconds,
            )
            _launchTelegram.value = body.nativeLink
            startPolling(body.token, pollWindowSeconds)
        }
    }

    /** Marks the launch-telegram one-shot as consumed; called from the screen after Intent fires. */
    fun onTelegramLaunched() {
        _launchTelegram.value = null
    }

    /** User cancels mid-flow — stop both the mint request and polling, and return to Idle. */
    fun cancel() {
        connectJob?.cancel()
        connectJob = null
        pollJob?.cancel()
        pollJob = null
        _state.value = ConnectTelegramUiState.Idle
        _launchTelegram.value = null
    }

    /** Convenience to dismiss terminal states (Connected / Expired / Error) back to Idle. */
    fun dismissTerminal() {
        when (_state.value) {
            is ConnectTelegramUiState.Connected,
            ConnectTelegramUiState.Expired,
            is ConnectTelegramUiState.Error,
            -> _state.value = ConnectTelegramUiState.Idle
            else -> Unit
        }
    }

    private fun startPolling(token: String, pollWindowSeconds: Int) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var elapsed = 0
            while (elapsed < pollWindowSeconds) {
                delay(POLL_INTERVAL_MS)
                elapsed += (POLL_INTERVAL_MS / 1000).toInt()

                val resp = runCatching { api.getTelegramLinkStatus(token) }.getOrNull()
                val body = resp?.body()
                if (resp != null && resp.isSuccessful && body != null && body.linked) {
                    _state.value = ConnectTelegramUiState.Connected(body.telegramId ?: -1L)
                    return@launch
                }
                _state.update { current ->
                    if (current is ConnectTelegramUiState.AwaitingPair) {
                        current.copy(secondsRemaining = (pollWindowSeconds - elapsed).coerceAtLeast(0))
                    } else {
                        current
                    }
                }
            }
            // window elapsed without success
            _state.value = ConnectTelegramUiState.Expired
        }
    }

    override fun onCleared() {
        connectJob?.cancel()
        pollJob?.cancel()
        super.onCleared()
    }

    companion object {
        const val POLL_INTERVAL_MS: Long = 3_000L
        /** Hard upper bound on how long we'll keep polling, even if the server says the
         *  token lives longer. Keeps us from looping forever on a misconfigured server. */
        const val MAX_POLL_WINDOW_SECONDS: Int = 15 * 60
    }
}
