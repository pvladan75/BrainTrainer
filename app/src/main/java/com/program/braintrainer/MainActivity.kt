package com.program.braintrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge // <-- DODAT IMPORT
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.MobileAds
import com.program.braintrainer.chess.model.data.SettingsManager
import com.program.braintrainer.ui.AppNavigation
import com.program.braintrainer.ui.theme.BrainTrainerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Omogućava prikaz od ivice do ivice za celu aplikaciju
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}

        setContent {
            val settingsManager = SettingsManager(LocalContext.current)
            val settings by settingsManager.settingsFlow.collectAsState(
                initial = null
            )

            settings?.let { appSettings ->
                BrainTrainerTheme(
                    appSettings = appSettings
                ) {
                    AppNavigation()
                }
            }
        }
    }
}