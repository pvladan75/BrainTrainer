package com.program.braintrainer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.program.braintrainer.data.AppSettings
import com.program.braintrainer.data.SettingsManager

// Definicija paleta boja koje koriste vrednosti iz Color.kt
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun BrainTrainerTheme(
    content: @Composable () -> Unit
) {
    // 1. Kreiramo instancu SettingsManager-a
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    // 2. Skupljamo stanje teme iz DataStore-a
    val currentTheme = settingsManager.settingsFlow.collectAsState(
        initial = AppSettings(isSoundEnabled = true, appTheme = SettingsManager.AppTheme.SYSTEM)
    ).value.appTheme

    // 3. Određujemo da li treba koristiti tamnu temu na osnovu podešavanja
    val useDarkTheme = when (currentTheme) {
        SettingsManager.AppTheme.LIGHT -> false
        SettingsManager.AppTheme.DARK -> true
        SettingsManager.AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
