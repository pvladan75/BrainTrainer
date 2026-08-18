package com.program.braintrainer.gamification

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.program.braintrainer.brainTrainerApp
import com.program.braintrainer.score.ScoreManager

/**
 * Fabrika za kreiranje instance AchievementsViewModel-a.
 */
class AchievementsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AchievementsViewModel::class.java)) {
            val app = context.brainTrainerApp

            return AchievementsViewModel(
                app.achievementManager,
                ScoreManager(app),
                app
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}