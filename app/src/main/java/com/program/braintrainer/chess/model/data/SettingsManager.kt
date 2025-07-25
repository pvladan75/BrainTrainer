package com.program.braintrainer.chess.model.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.program.braintrainer.BuildConfig // <-- DODAT IMPORT ZA PRISTUP BUILD VARIJABLAMA
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val isSoundEnabled: Boolean,
    val appTheme: SettingsManager.AppTheme,
    val isPremiumUser: Boolean
)

class SettingsManager(private val context: Context) {

    companion object {
        val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
        val APP_THEME_KEY = stringPreferencesKey("app_theme")
        val PREMIUM_USER_KEY = booleanPreferencesKey("is_premium_user")
    }

    enum class AppTheme(val value: String) {
        LIGHT("light"),
        DARK("dark"),
        SYSTEM("system")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val soundEnabled = preferences[SOUND_ENABLED_KEY] ?: true
        val theme = AppTheme.valueOf(
            preferences[APP_THEME_KEY] ?: AppTheme.SYSTEM.name
        )

        // --- IZMENJENA LOGIKA ZA PREMIUM STATUS ---
        // 1. Čitamo sačuvanu vrednost iz memorije telefona
        val isPremiumFromStorage = preferences[PREMIUM_USER_KEY] ?: false

        // 2. Finalni status je 'true' ILI ako je sačuvano kao premium ILI ako je ovo testna verzija
        val finalIsPremium = isPremiumFromStorage || BuildConfig.IS_TEST_BUILD

        AppSettings(soundEnabled, theme, finalIsPremium)
    }

    suspend fun setSoundEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[SOUND_ENABLED_KEY] = isEnabled
        }
    }

    suspend fun setPremiumUser(isPremium: Boolean) {
        context.dataStore.edit { settings ->
            settings[PREMIUM_USER_KEY] = isPremium
        }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { settings ->
            settings[APP_THEME_KEY] = theme.name
        }
    }
}