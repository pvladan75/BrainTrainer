package com.program.braintrainer.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.program.braintrainer.data.SettingsManager
import com.program.braintrainer.score.ScoreManager

/**
 * Fabrika (Factory) za kreiranje instance SettingsViewModel-a.
 * Ovo je neophodno jer ViewModel ima zavisnosti (settingsManager, scoreManager)
 * koje mu se moraju proslediti prilikom kreiranja.
 */
class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            // Kreiramo instance menadžera i prosleđujemo ih u ViewModel
            val settingsManager = SettingsManager(context.applicationContext)
            val scoreManager = ScoreManager(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsManager, scoreManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
