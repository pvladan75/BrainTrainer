package com.program.braintrainer.gamification

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.program.braintrainer.score.ScoreManager

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            // Sada prosleđujemo oba menadžera
            val scoreManager = ScoreManager(context.applicationContext)
            val achievementManager = AchievementManager(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(scoreManager, achievementManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
