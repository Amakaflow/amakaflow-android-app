package com.amakaflow.companion

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.amakaflow.companion.ui.screens.shareimport.ShareImportScreen
import com.amakaflow.companion.ui.screens.shareimport.ShareImportViewModel
import com.amakaflow.companion.ui.theme.AmakaFlowTheme
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "ShareImportActivity"

/**
 * AMA-1258: Activity that receives shared URLs from other apps via Android share sheet.
 * Uses a dialog-like theme for a compact import preview overlay.
 */
@AndroidEntryPoint
class ShareImportActivity : ComponentActivity() {

    private val viewModel: ShareImportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            AmakaFlowTheme {
                ShareImportScreen(
                    viewModel = viewModel,
                    onDismiss = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    Log.d(TAG, "Received ACTION_SEND: $sharedText")
                    viewModel.handleSharedText(sharedText)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                if (intent.type == "text/plain") {
                    val texts = intent.getStringArrayListExtra(Intent.EXTRA_TEXT)
                    Log.d(TAG, "Received ACTION_SEND_MULTIPLE: ${texts?.size} items")
                    if (texts != null) {
                        viewModel.handleMultipleSharedTexts(texts)
                    }
                }
            }
        }
    }
}
