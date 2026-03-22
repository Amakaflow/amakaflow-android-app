package com.amakaflow.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.amakaflow.wear.ui.WearNavHost
import com.amakaflow.wear.ui.theme.AmakaFlowWearTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmakaFlowWearTheme {
                WearNavHost()
            }
        }
    }
}
