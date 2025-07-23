package com.program.braintrainer.ui.screens.settings

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.program.braintrainer.chess.model.data.AppSettings
import com.program.braintrainer.chess.model.data.BillingClientManager
import com.program.braintrainer.chess.model.data.SettingsManager
import com.program.braintrainer.score.ScoreManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsManager: SettingsManager,
    private val scoreManager: ScoreManager,
    context: Context // Kontekst se prosleđuje spolja, npr. iz Hilt/Koin ili Factory-ja
) : ViewModel() {

    private val billingClientManager: BillingClientManager

    init {
        // ISPRAVKA: Prosleđen je 'viewModelScope' kao 'externalScope'
        billingClientManager = BillingClientManager(
            context = context,
            externalScope = viewModelScope,
            onPurchaseSuccess = {
                // Nakon uspešne kupovine, ažuriramo status korisnika
                viewModelScope.launch {
                    settingsManager.setPremiumUser(true)
                }
            }
        )
    }

    val settingsState: StateFlow<AppSettings> = settingsManager.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings(
            isSoundEnabled = true,
            appTheme = SettingsManager.AppTheme.SYSTEM,
            isPremiumUser = false
        )
    )

    /**
     * Proverava postojeće kupovine. Treba pozvati kada se UI vraća u prvi plan (onResume).
     * Ovo je ključno za obradu kupovina koje su se desile van aplikacije.
     */
    fun queryExistingPurchasesOnResume() {
        viewModelScope.launch {
            billingClientManager.queryExistingPurchases()
        }
    }

    fun onSoundToggle(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setSoundEnabled(isEnabled)
        }
    }

    fun onThemeChange(theme: SettingsManager.AppTheme) {
        viewModelScope.launch {
            settingsManager.setAppTheme(theme)
        }
    }

    fun onResetProgressConfirmed() {
        viewModelScope.launch {
            scoreManager.resetAllScores()
        }
    }

    fun onPurchasePremium(activity: Activity) {
        viewModelScope.launch {
            billingClientManager.launchPurchaseFlow(activity)
        }
    }

    // Dobra praksa je da se oslobode resursi kada ViewModel više nije potreban
    override fun onCleared() {
        super.onCleared()
        billingClientManager.destroy()
    }
}
