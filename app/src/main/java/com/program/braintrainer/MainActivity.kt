package com.program.braintrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.program.braintrainer.ui.AppNavigation
import com.program.braintrainer.ui.theme.BrainTrainerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val settingsManager = remember { applicationContext.brainTrainerApp.settingsManager }
            val settings by settingsManager.settingsFlow.collectAsState(initial = null)

            settings?.let { appSettings ->
                BrainTrainerTheme(appSettings = appSettings) {
                    AppNavigation()
                }
            }
        }
    }
}
