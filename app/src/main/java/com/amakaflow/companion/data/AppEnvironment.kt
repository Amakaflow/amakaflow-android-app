package com.amakaflow.companion.data

import com.amakaflow.companion.BuildConfig

/**
 * App environment configuration
 */
enum class AppEnvironment {
    PRODUCTION,
    STAGING,
    DEVELOPMENT;

    val mapperApiUrl: String
        get() = when (this) {
            PRODUCTION -> BuildConfig.MAPPER_API_URL_PROD
            STAGING -> BuildConfig.MAPPER_API_URL_STAGING
            DEVELOPMENT -> BuildConfig.MAPPER_API_URL_DEV
        }

    val ingestorApiUrl: String
        get() = when (this) {
            PRODUCTION -> BuildConfig.INGESTOR_API_URL_PROD
            STAGING -> BuildConfig.INGESTOR_API_URL_STAGING
            DEVELOPMENT -> BuildConfig.INGESTOR_API_URL_DEV
        }

    val mcpApiUrl: String
        get() = when (this) {
            PRODUCTION -> BuildConfig.MCP_API_URL_PROD
            STAGING -> BuildConfig.MCP_API_URL_STAGING
            DEVELOPMENT -> BuildConfig.MCP_API_URL_DEV
        }

    val chatApiUrl: String
        get() = when (this) {
            PRODUCTION -> BuildConfig.CHAT_API_URL_PROD
            STAGING -> BuildConfig.CHAT_API_URL_STAGING
            DEVELOPMENT -> BuildConfig.CHAT_API_URL_DEV
        }

    val displayName: String
        get() = when (this) {
            PRODUCTION -> "Production"
            STAGING -> "Staging"
            DEVELOPMENT -> "Development (localhost)"
        }

    companion object {
        var current: AppEnvironment = valueOf(BuildConfig.DEFAULT_ENVIRONMENT)
    }
}
