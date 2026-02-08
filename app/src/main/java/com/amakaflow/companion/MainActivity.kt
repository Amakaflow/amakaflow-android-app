package com.amakaflow.companion

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.amakaflow.companion.data.TestConfig
import com.amakaflow.companion.debug.DebugLog
import com.amakaflow.companion.ui.navigation.MainScreen
import com.amakaflow.companion.ui.theme.AmakaFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var testConfig: TestConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for Maestro UITEST_MODE intent extras (E2E test automation)
        handleTestModeExtras()

        enableEdgeToEdge()
        setContent {
            AmakaFlowTheme {
                MainScreen(testConfig = testConfig)
            }
        }
    }

    /**
     * Read Maestro launch arguments from intent extras to configure test mode.
     * Maestro passes launchApp arguments as intent extras (strings).
     */
    private fun handleTestModeExtras() {
        val extras = intent.extras ?: return
        val isTestMode = extras.getString(TestConfig.EXTRA_UITEST_MODE)

        if (isTestMode?.toBoolean() == true) {
            Log.d(TAG, "UITEST_MODE detected in intent extras, configuring test mode")
            DebugLog.info("E2E test mode activated via Maestro launch arguments", TAG)
            testConfig.configureFromIntentExtras(extras)
        }
    }
}
