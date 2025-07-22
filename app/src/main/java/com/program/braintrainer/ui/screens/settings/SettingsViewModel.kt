package com.program.braintrainer.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.program.braintrainer.chess.model.data.AppSettings
import com.program.braintrainer.chess.model.data.SettingsManager
import com.program.braintrainer.score.ScoreManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel za SettingsScreen.
 * Upravlja stanjem UI-a i komunicira sa SettingsManager-om i ScoreManager-om.
 *
 * @param settingsManager Instanca za upravljanje podešavanjima.
 * @param scoreManager Instanca za upravljanje rezultatima (za resetovanje).
 */
class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val scoreManager: ScoreManager
) : ViewModel() {

    // Učitavamo podešavanja i pretvaramo ih u StateFlow da bi UI mogao da reaguje na promene.
    val settingsState: StateFlow<AppSettings> = settingsManager.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        // Ispravljena inicijalna vrednost da uključuje i premium status
        initialValue = AppSettings(
            isSoundEnabled = true,
            appTheme = SettingsManager.AppTheme.SYSTEM,
            isPremiumUser = false
        )
    )

    /**
     * Poziva se kada korisnik promeni stanje prekidača za zvuk.
     */
    fun onSoundToggle(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setSoundEnabled(isEnabled)
        }
    }

    /**
     * Poziva se kada korisnik izabere novu temu.
     */
    fun onThemeChange(theme: SettingsManager.AppTheme) {
        viewModelScope.launch {
            settingsManager.setAppTheme(theme)
        }
    }

    /**
     * Poziva se kada korisnik potvrdi resetovanje napretka.
     * Briše sve sačuvane rezultate.
     */
    fun onResetProgressConfirmed() {
        viewModelScope.launch {
            scoreManager.resetAllScores()
            // Ovde možete dodati i brisanje achievementa ako/kada se implementiraju
        }
    }

    /**
     * Poziva se kada korisnik klikne na "Kupi".
     */
    fun onPurchasePremium() {
        // TODO: Ovde se pokreće Google Play Billing proces za kupovinu.
        // Nakon uspešne kupovine, poziva se sledeća linija koda.
        viewModelScope.launch {
            settingsManager.setPremiumUser(true)
        }
    }
}