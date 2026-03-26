package com.amakaflow.companion.util

import android.content.Intent
import android.net.Uri
import java.net.URLDecoder

/**
 * AMA-1259: Parses deep link URIs into import URLs.
 *
 * Supported deep link formats:
 * - https://amakaflow.com/import?url=ENCODED_URL
 * - https://app.amakaflow.com/import?url=ENCODED_URL
 * - amakaflow://import?url=ENCODED_URL (custom scheme fallback)
 */
object DeepLinkParser {

    private const val PATH_IMPORT = "/import"
    private const val PARAM_URL = "url"

    private val VALID_HOSTS = setOf("amakaflow.com", "app.amakaflow.com", "www.amakaflow.com")
    private const val CUSTOM_SCHEME = "amakaflow"
    private const val CUSTOM_SCHEME_IMPORT_HOST = "import"

    /**
     * Result of parsing a deep link.
     */
    sealed class DeepLinkResult {
        /** Successfully extracted an import URL from the deep link. */
        data class ImportUrl(val url: String) : DeepLinkResult()

        /** The URI was not a recognized deep link format. */
        data object NotADeepLink : DeepLinkResult()

        /** The URI matched the import path but was missing the url parameter. */
        data object MissingUrlParam : DeepLinkResult()
    }

    /**
     * Parse a deep link URI string and extract the import URL if present.
     * This method works without Android framework dependencies (testable in JVM unit tests).
     *
     * @param uriString The URI string to parse
     * @return DeepLinkResult indicating success or failure reason
     */
    fun parseString(uriString: String?): DeepLinkResult {
        if (uriString.isNullOrBlank()) return DeepLinkResult.NotADeepLink

        return try {
            val javaUri = java.net.URI(uriString)
            val scheme = javaUri.scheme?.lowercase()
            val host = javaUri.host?.lowercase()
            val path = javaUri.rawPath
            val query = javaUri.rawQuery

            val isHttpsLink = scheme == "https" && host in VALID_HOSTS
            val isCustomScheme = scheme == CUSTOM_SCHEME

            if (!isHttpsLink && !isCustomScheme) {
                return DeepLinkResult.NotADeepLink
            }

            // Check if this is an import path
            val isImportPath = when {
                isHttpsLink -> path == PATH_IMPORT
                isCustomScheme -> host == CUSTOM_SCHEME_IMPORT_HOST
                else -> false
            }

            if (!isImportPath) {
                return DeepLinkResult.NotADeepLink
            }

            // Extract the url query parameter
            val importUrl = extractQueryParam(query, PARAM_URL)
            if (importUrl.isNullOrBlank()) {
                return DeepLinkResult.MissingUrlParam
            }

            DeepLinkResult.ImportUrl(url = importUrl)
        } catch (_: Exception) {
            DeepLinkResult.NotADeepLink
        }
    }

    /**
     * Parse a deep link from an Android Uri object.
     */
    fun parse(uri: Uri?): DeepLinkResult {
        return parseString(uri?.toString())
    }

    /**
     * Parse a deep link from an Android Intent.
     * Handles ACTION_VIEW (deep link) intents.
     */
    fun parseIntent(intent: Intent?): DeepLinkResult {
        if (intent == null) return DeepLinkResult.NotADeepLink
        if (intent.action != Intent.ACTION_VIEW) return DeepLinkResult.NotADeepLink
        return parse(intent.data)
    }

    /**
     * Check if a URI string is a recognized AmakaFlow deep link (any path).
     */
    fun isAmakaFlowDeepLink(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return try {
            val javaUri = java.net.URI(uriString)
            val scheme = javaUri.scheme?.lowercase()
            val host = javaUri.host?.lowercase()
            val isHttpsLink = scheme == "https" && host in VALID_HOSTS
            val isCustomScheme = scheme == CUSTOM_SCHEME
            isHttpsLink || isCustomScheme
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Check if an Android Uri is a recognized AmakaFlow deep link.
     */
    fun isAmakaFlowDeepLink(uri: Uri?): Boolean {
        return isAmakaFlowDeepLink(uri?.toString())
    }

    /**
     * Extract a query parameter value from a raw query string.
     * Handles URL decoding.
     */
    private fun extractQueryParam(query: String?, paramName: String): String? {
        if (query.isNullOrBlank()) return null
        return query.split("&")
            .map { it.split("=", limit = 2) }
            .firstOrNull { it[0] == paramName }
            ?.getOrNull(1)
            ?.let { URLDecoder.decode(it, "UTF-8") }
    }
}
