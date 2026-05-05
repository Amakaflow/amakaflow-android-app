package com.amakaflow.companion.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from POST /api/telegram/link-token on mapper-api.
 * Mints a single-use, 15-min-TTL token + the deep links the user can tap to land in Telegram.
 *
 * AMA-1758 Phase C — Connect Telegram UI on Android.
 */
@Serializable
data class TelegramLinkTokenResponse(
    val token: String,
    @SerialName("deep_link") val deepLink: String,
    @SerialName("native_link") val nativeLink: String,
    @SerialName("expires_in_seconds") val expiresInSeconds: Long,
)

/**
 * Response from GET /api/telegram/link-status?token=...
 *
 * `linked = false` is the pre-consumption state. Once the bot processes
 * `/start <token>`, the response carries the user's Telegram chat id.
 */
@Serializable
data class TelegramLinkStatusResponse(
    val linked: Boolean,
    @SerialName("telegram_id") val telegramId: Long? = null,
    @SerialName("used_at") val usedAt: String? = null,
)
