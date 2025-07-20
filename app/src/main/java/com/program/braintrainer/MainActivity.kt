package com.program.braintrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.program.braintrainer.ui.AppNavigation // Importujemo našu navigaciju
import com.program.braintrainer.ui.theme.BrainTrainerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BrainTrainerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Jedina stvar koju MainActivity sada radi je pokretanje AppNavigation
                    AppNavigation()
                }
            }
        }
    }
}
