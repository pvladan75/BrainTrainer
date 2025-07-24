package com.program.braintrainer.gamification

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.program.braintrainer.chess.model.data.SettingsManager
import com.program.braintrainer.score.ScoreManager

/**
 * Fabrika za kreiranje instance AchievementsViewModel-a.
 */
class AchievementsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AchievementsViewModel::class.java)) {
            val applicationContext = context.applicationContext

            // Kreiramo sve potrebne zavisnosti
            val settingsManager = SettingsManager(applicationContext)
            val achievementManager = AchievementManager(applicationContext, settingsManager)
            val scoreManager = ScoreManager(applicationContext)

            // ISPRAVKA: Prosleđujemo i 'context' u konstruktor ViewModel-a
            return AchievementsViewModel(
                achievementManager,
                scoreManager,
                applicationContext
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}