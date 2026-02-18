package com.amakaflow.companion.debug

import android.content.Context
import com.amakaflow.companion.data.AppEnvironment
import dagger.hilt.android.qualifiers.ApplicationContext
import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.protocol.User
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SentryHelper provides convenient methods for capturing errors, 
 * breadcrumbs, user context, and custom tags in the AmakaFlow app.
 * 
 * Usage examples:
 * 
 * 1. Manual error capture:
 *    try {
 *        // code that might throw
 *    } catch (e: Exception) {
 *        SentryHelper.captureException(e, "Additional context")
 *    }
 * 
 * 2. Breadcrumbs (automatic events leading up to an error):
 *    SentryHelper.addBreadcrumb("User tapped pair device", "ui")
 *    SentryHelper.addBreadcrumb("Device paired successfully", "network")
 * 
 * 3. User context (set after pairing):
 *    SentryHelper.setUser(userId, email, deviceId)
 * 
 * 4. Custom tags:
 *    SentryHelper.setTag("device_model", Build.MODEL)
 *    SentryHelper.setTag("paired_device", deviceName)
 * 
 * 5. Performance monitoring:
 *    SentryHelper.traceTransaction("api_call", "fetch_workouts") {
 *        // code to measure
 *    }
 */
@Suppress("UNUSED_PARAMETER")
@Singleton
class SentryHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val BREADCRUMB_CATEGORY_UI = "ui"
        private const val BREADCRUMB_CATEGORY_NETWORK = "network"
        private const val BREADCRUMB_CATEGORY_DATABASE = "database"
        private const val BREADCRUMB_CATEGORY_SYSTEM = "system"
        
        /**
         * Capture an exception with optional additional context.
         */
        fun captureException(throwable: Throwable, contextMessage: String? = null) {
            if (contextMessage != null) {
                // Capture both the message and the exception
                Sentry.captureException(throwable)
                Sentry.captureMessage(contextMessage, io.sentry.SentryLevel.ERROR)
            } else {
                Sentry.captureException(throwable)
            }
        }

        /**
         * Capture a message with optional level.
         */
        fun captureMessage(message: String, level: io.sentry.SentryLevel = io.sentry.SentryLevel.INFO) {
            Sentry.captureMessage(message, level)
        }

        /**
         * Add a breadcrumb for tracking user actions leading up to errors.
         * 
         * @param message Description of the action
         * @param category Category of the breadcrumb (ui, network, database, system)
         * @param data Optional additional data as key-value pairs
         */
        fun addBreadcrumb(
            message: String,
            category: String = BREADCRUMB_CATEGORY_UI,
            data: Map<String, Any>? = null
        ) {
            val breadcrumb = io.sentry.Breadcrumb(message)
            breadcrumb.category = category
            breadcrumb.type = "default"
            
            data?.let {
                it.forEach { (key, value) ->
                    breadcrumb.setData(key, value.toString())
                }
            }
            
            Sentry.addBreadcrumb(breadcrumb)
        }

        /**
         * Set user context after device pairing.
         * Call this when a user successfully pairs their device.
         * 
         * @param userId Unique identifier for the user
         * @param email User's email (optional)
         * @param deviceId Paired device identifier
         */
        fun setUser(userId: String, email: String? = null, deviceId: String? = null) {
            if (userId.isBlank()) return
            
            val user = User()
            user.id = userId
            email?.let { user.email = it }
            
            // Store device ID in the user data
            deviceId?.let {
                user.data = mapOf("device_id" to it)
            }
            
            Sentry.setUser(user)
            
            // Also set as a tag for easier filtering
            setTag("user.id", userId)
            deviceId?.let { setTag("paired_device_id", it) }
        }

        /**
         * Clear user context (e.g., when user unpairs).
         */
        fun clearUser() {
            Sentry.setUser(null)
            removeTag("user.id")
            removeTag("paired_device_id")
        }

        /**
         * Set a custom tag for filtering in Sentry dashboard.
         */
        fun setTag(key: String, value: String) {
            Sentry.setTag(key, value)
        }

        /**
         * Remove a custom tag.
         */
        fun removeTag(key: String) {
            Sentry.removeTag(key)
        }

        /**
         * Set multiple tags at once.
         */
        fun setTags(tags: Map<String, String>) {
            tags.forEach { (key, value) ->
                Sentry.setTag(key, value)
            }
        }

        /**
         * Set environment tag based on current app environment.
         */
        fun setEnvironmentTag(environment: AppEnvironment) {
            val envName = when (environment) {
                AppEnvironment.PRODUCTION -> "prod"
                AppEnvironment.STAGING -> "staging"
                AppEnvironment.DEVELOPMENT -> "dev"
            }
            Sentry.setTag("environment", envName)
        }

        /**
         * Wrap code in a transaction for performance monitoring.
         * 
         * @param operation The operation name (e.g., "api_call", "db_query")
         * @param description The specific action being measured
         * @param block The code to measure
         * @return The result of the block
         */
        inline fun <T> traceTransaction(
            operation: String,
            description: String,
            block: () -> T
        ): T {
            val transaction = Sentry.startTransaction(operation, description)
            return try {
                val result = block()
                transaction.status = SpanStatus.OK
                result
            } catch (e: Exception) {
                transaction.status = SpanStatus.INTERNAL_ERROR
                throw e
            } finally {
                transaction.finish()
            }
        }

        /**
         * Manually start a transaction for more complex use cases.
         */
        fun startTransaction(operation: String, description: String): io.sentry.ITransaction {
            return Sentry.startTransaction(operation, description)
        }

        /**
         * Add context data that will be attached to all events.
         */
        fun setContext(name: String, data: Map<String, Any>) {
            Sentry.configureScope { scope ->
                scope.setContexts(name, data)
            }
        }

        /**
         * Clear all breadcrumbs.
         */
        fun clearBreadcrumbs() {
            Sentry.clearBreadcrumbs()
        }

        /**
         * Flush pending events to Sentry.
         * Call this before app exit or when backgrounded for a while.
         */
        fun flush() {
            Sentry.flush(2000) // Wait up to 2 seconds
        }
    }
}
