package com.amakaflow.companion

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.amakaflow.companion.data.AppEnvironment
import com.amakaflow.companion.data.TestConfig
import com.amakaflow.companion.data.sync.CompletionSyncWorker
import com.amakaflow.companion.debug.DebugLog
import com.amakaflow.companion.debug.GlobalExceptionHandler
import dagger.hilt.android.HiltAndroidApp
import io.sentry.Sentry
import io.sentry.SentryOptions
import javax.inject.Inject

@HiltAndroidApp
class AmakaFlowApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // Inject TestConfig early to initialize AppEnvironment.current from persisted value
    @Inject
    lateinit var testConfig: TestConfig

    override fun onCreate() {
        super.onCreate()

        // Initialize Sentry for error tracking
        initializeSentry()

        // Install global exception handler for crash logging
        GlobalExceptionHandler.install()

        // Log app startup
        DebugLog.info("App started - v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", "App")
        DebugLog.info("Build type: ${BuildConfig.BUILD_TYPE}", "App")

        // Force TestConfig initialization to set AppEnvironment.current
        // The init block in TestConfig handles this
        testConfig.hashCode()

        DebugLog.info("Environment: ${AppEnvironment.current.displayName}", "App")

        // Schedule periodic completion sync
        CompletionSyncWorker.schedulePeriodicSync(this)
        DebugLog.debug("Completion sync worker scheduled", "App")
    }

    private fun initializeSentry() {
        val dsn = BuildConfig.SENTRY_DSN
        
        Sentry.init { options ->
            options.dsn = dsn
            options.environment = BuildConfig.DEFAULT_ENVIRONMENT
            
            // Performance monitoring
            options.tracesSampleRate = 1.0 // 100% for now, adjust for production
            
            // Session tracking
            options.anrEnabled = true
            options.anrTimeoutIntervalMs = 5000
            
            // Data filtering - filter out sensitive data
            options.isAttachServerName = false
            options.isAttachThreads = true
            options.isAttachStacktrace = true
            
            // Release tracking
            options.release = "com.amakaflow.companion@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
            
            // Set common tags
            options.tags["app.version"] = BuildConfig.VERSION_NAME
            options.tags["app.version.code"] = BuildConfig.VERSION_CODE.toString()
            options.tags["build.type"] = BuildConfig.BUILD_TYPE
            
            // Enable debug in debug builds
            options.debug = BuildConfig.DEBUG
        }
        
        DebugLog.info("Sentry initialized with DSN: ${dsn.take(20)}...", "Sentry")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
