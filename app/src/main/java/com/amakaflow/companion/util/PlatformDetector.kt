package com.amakaflow.companion.util

/**
 * AMA-1258: Detects workout video platform from a URL and extracts clean URLs from shared text.
 *
 * Instagram, TikTok, and other platforms often include caption text alongside the URL when sharing.
 * This utility extracts the URL using regex and identifies the source platform.
 */
object PlatformDetector {

    enum class Platform(val displayName: String, val apiEndpoint: String) {
        YOUTUBE("YouTube", "ingest/youtube"),
        INSTAGRAM("Instagram", "ingest/instagram_reel"),
        TIKTOK("TikTok", "ingest/tiktok"),
        PINTEREST("Pinterest", "ingest/pinterest"),
        UNKNOWN("Link", "ingest/url"),
    }

    private val URL_REGEX = Regex(
        """https?://[^\s<>"{}|\\^`\[\]]+""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Extract the first URL from shared text. Instagram and TikTok often prepend
     * caption text before the actual URL.
     */
    fun extractUrl(sharedText: String): String? {
        return URL_REGEX.find(sharedText.trim())?.value
    }

    /**
     * Extract all URLs from shared text (for ACTION_SEND_MULTIPLE support).
     */
    fun extractAllUrls(sharedText: String): List<String> {
        return URL_REGEX.findAll(sharedText.trim()).map { it.value }.toList()
    }

    /**
     * Detect the platform from a URL.
     */
    fun detectPlatform(url: String): Platform {
        val lower = url.lowercase()
        return when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> Platform.YOUTUBE
            lower.contains("instagram.com") -> Platform.INSTAGRAM
            lower.contains("tiktok.com") || lower.contains("vm.tiktok.com") -> Platform.TIKTOK
            lower.contains("pinterest.com") || lower.contains("pin.it") -> Platform.PINTEREST
            else -> Platform.UNKNOWN
        }
    }

    /**
     * Extract URL and detect platform in one call.
     * Returns null if no URL found in the shared text.
     */
    fun parseSharedText(sharedText: String): ParseResult? {
        val url = extractUrl(sharedText) ?: return null
        return ParseResult(url = url, platform = detectPlatform(url))
    }

    data class ParseResult(
        val url: String,
        val platform: Platform,
    )
}
