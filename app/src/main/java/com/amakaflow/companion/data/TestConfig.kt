package com.amakaflow.companion.data

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.content.edit
import com.amakaflow.companion.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration for E2E test mode.
 * When enabled, uses test headers instead of JWT authentication.
 *
 * Supports Maestro launch arguments (intent extras):
 * - UITEST_MODE: "true" to enable test mode
 * - UITEST_AUTH_SECRET: auth secret for test header bypass
 * - UITEST_USER_ID: test user ID
 * - UITEST_USE_FIXTURES: "true" to use fixture data instead of API calls
 * - UITEST_FIXTURES: comma-separated fixture names (e.g., "amrap_10min,emom_strength")
 * - UITEST_SKIP_ONBOARDING: "true" to skip pairing/onboarding screen
 */
@Singleton
class TestConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("test_config", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TEST_MODE_ENABLED = "test_mode_enabled"
        private const val KEY_TEST_AUTH_SECRET = "test_auth_secret"
        private const val KEY_TEST_USER_ID = "test_user_id"
        private const val KEY_TEST_USER_EMAIL = "test_user_email"
        private const val KEY_USE_FIXTURES = "use_fixtures"
        private const val KEY_FIXTURES = "fixtures"
        private const val KEY_SKIP_ONBOARDING = "skip_onboarding"
        private const val KEY_APP_ENVIRONMENT = "app_environment"

        // Default test credentials for e2e testing
        const val DEFAULT_TEST_USER_EMAIL = "soopergeri+e2e-android@gmail.com"

        // Maestro intent extra keys
        const val EXTRA_UITEST_MODE = "UITEST_MODE"
        const val EXTRA_UITEST_AUTH_SECRET = "UITEST_AUTH_SECRET"
        const val EXTRA_UITEST_USER_ID = "UITEST_USER_ID"
        const val EXTRA_UITEST_USE_FIXTURES = "UITEST_USE_FIXTURES"
        const val EXTRA_UITEST_FIXTURES = "UITEST_FIXTURES"
        const val EXTRA_UITEST_SKIP_ONBOARDING = "UITEST_SKIP_ONBOARDING"
    }

    var isTestModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_TEST_MODE_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_TEST_MODE_ENABLED, value) }

    var testAuthSecret: String?
        get() = prefs.getString(KEY_TEST_AUTH_SECRET, null)
        set(value) = prefs.edit { putString(KEY_TEST_AUTH_SECRET, value) }

    var testUserId: String?
        get() = prefs.getString(KEY_TEST_USER_ID, null)
        set(value) = prefs.edit { putString(KEY_TEST_USER_ID, value) }

    var testUserEmail: String?
        get() = prefs.getString(KEY_TEST_USER_EMAIL, DEFAULT_TEST_USER_EMAIL)
        set(value) = prefs.edit { putString(KEY_TEST_USER_EMAIL, value) }

    var useFixtures: Boolean
        get() = prefs.getBoolean(KEY_USE_FIXTURES, false)
        set(value) = prefs.edit { putBoolean(KEY_USE_FIXTURES, value) }

    var fixtures: String?
        get() = prefs.getString(KEY_FIXTURES, null)
        set(value) = prefs.edit { putString(KEY_FIXTURES, value) }

    var skipOnboarding: Boolean
        get() = prefs.getBoolean(KEY_SKIP_ONBOARDING, false)
        set(value) = prefs.edit { putBoolean(KEY_SKIP_ONBOARDING, value) }

    /** List of fixture names parsed from comma-separated string */
    val fixtureNames: List<String>
        get() = fixtures?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    fun enableTestMode(authSecret: String, userId: String, userEmail: String = DEFAULT_TEST_USER_EMAIL) {
        testAuthSecret = authSecret
        testUserId = userId
        testUserEmail = userEmail
        isTestModeEnabled = true
    }

    /**
     * Configure test mode from Maestro intent extras.
     * Called from MainActivity.onCreate when UITEST_MODE is detected.
     */
    fun configureFromIntentExtras(extras: Bundle) {
        val isTestMode = extras.getString(EXTRA_UITEST_MODE)?.toBoolean() == true
        if (!isTestMode) return

        val authSecret = extras.getString(EXTRA_UITEST_AUTH_SECRET) ?: ""
        val userId = extras.getString(EXTRA_UITEST_USER_ID) ?: "test-user-123"
        val shouldUseFixtures = extras.getString(EXTRA_UITEST_USE_FIXTURES)?.toBoolean() == true
        val fixtureList = extras.getString(EXTRA_UITEST_FIXTURES)
        val shouldSkipOnboarding = extras.getString(EXTRA_UITEST_SKIP_ONBOARDING)?.toBoolean() == true

        enableTestMode(authSecret, userId)
        useFixtures = shouldUseFixtures
        fixtures = fixtureList
        skipOnboarding = shouldSkipOnboarding

        Log.d("TestConfig", "Test mode configured from intent: " +
            "authSecret=${authSecret.take(4)}..., userId=$userId, " +
            "useFixtures=$shouldUseFixtures, fixtures=$fixtureList, " +
            "skipOnboarding=$shouldSkipOnboarding")
    }

    fun disableTestMode() {
        isTestModeEnabled = false
        testAuthSecret = null
        testUserId = null
        useFixtures = false
        fixtures = null
        skipOnboarding = false
    }

    var appEnvironment: AppEnvironment
        get() {
            val defaultEnv = BuildConfig.DEFAULT_ENVIRONMENT
            val name = prefs.getString(KEY_APP_ENVIRONMENT, defaultEnv)
            val env = try {
                AppEnvironment.valueOf(name ?: defaultEnv)
            } catch (e: IllegalArgumentException) {
                AppEnvironment.valueOf(defaultEnv)
            }
            Log.d("TestConfig", "Getting appEnvironment: stored=$name, resolved=$env, default=$defaultEnv")
            return env
        }
        set(value) {
            Log.d("TestConfig", "Setting appEnvironment: $value")
            prefs.edit { putString(KEY_APP_ENVIRONMENT, value.name) }
            AppEnvironment.current = value
        }

    init {
        // Initialize AppEnvironment.current from persisted value on startup
        val storedEnv = appEnvironment
        Log.d("TestConfig", "Init: Loading stored environment=$storedEnv, setting AppEnvironment.current")
        AppEnvironment.current = storedEnv
    }
}
