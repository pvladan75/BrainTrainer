package com.program.braintrainer.gamification

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.program.braintrainer.score.ScoreManager

/**
 * Fabrika za kreiranje instance AchievementsViewModel-a.
 */
class AchievementsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AchievementsViewModel::class.java)) {
            val achievementManager = AchievementManager(context.applicationContext)
            val scoreManager = ScoreManager(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return AchievementsViewModel(achievementManager, scoreManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}