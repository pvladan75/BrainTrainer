package com.program.braintrainer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Definišemo DataStore instancu na nivou fajla
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Menadžer za upravljanje podešavanjima aplikacije koristeći Jetpack DataStore.
 * @param context Kontekst aplikacije.
 */
class SettingsManager(private val context: Context) {

    // Definišemo ključeve za svako podešavanje koje čuvamo
    companion object {
        val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
        val APP_THEME_KEY = stringPreferencesKey("app_theme")
    }

    /**
     * Enum za definisanje mogućih tema aplikacije.
     */
    enum class AppTheme(val value: String) {
        LIGHT("light"),
        DARK("dark"),
        SYSTEM("system")
    }

    /**
     * Pruža Flow koji emituje trenutna podešavanja svaki put kada se promene.
     */
    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val soundEnabled = preferences[SOUND_ENABLED_KEY] ?: true // Zvuk je uključen po defaultu
        val theme = AppTheme.valueOf(
            preferences[APP_THEME_KEY] ?: AppTheme.SYSTEM.name
        )
        AppSettings(soundEnabled, theme)
    }

    /**
     * Asinhrono postavlja vrednost za zvučne efekte.
     * @param isEnabled Da li su zvuci uključeni.
     */
    suspend fun setSoundEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[SOUND_ENABLED_KEY] = isEnabled
        }
    }

    /**
     * Asinhrono postavlja izabranu temu aplikacije.
     * @param theme Izabrana tema.
     */
    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { settings ->
            settings[APP_THEME_KEY] = theme.name
        }
    }
}

/**
 * Data klasa koja predstavlja sva podešavanja aplikacije.
 */
data class AppSettings(
    val isSoundEnabled: Boolean,
    val appTheme: SettingsManager.AppTheme
)
