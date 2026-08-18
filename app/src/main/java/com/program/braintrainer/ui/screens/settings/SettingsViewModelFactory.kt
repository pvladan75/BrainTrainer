package com.program.braintrainer.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.program.braintrainer.brainTrainerApp
import com.program.braintrainer.score.ScoreManager

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val app = context.brainTrainerApp
            val scoreManager = ScoreManager(app)
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                settingsManager = app.settingsManager,
                scoreManager = scoreManager,
                billingClientManager = app.billingClientManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
