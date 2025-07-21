package com.program.braintrainer.gamification

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Fabrika za kreiranje instance AchievementsViewModel-a.
 */
class AchievementsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AchievementsViewModel::class.java)) {
            val achievementManager = AchievementManager(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return AchievementsViewModel(achievementManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
