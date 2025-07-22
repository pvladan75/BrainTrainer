package com.program.braintrainer.gamification

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.program.braintrainer.chess.model.data.SettingsManager
import com.program.braintrainer.score.ScoreManager

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            // --- ISPRAVKA: Kreiramo SVE menadžere i prosleđujemo ih ---
            val settingsManager = SettingsManager(context.applicationContext)
            val scoreManager = ScoreManager(context.applicationContext)
            val achievementManager = AchievementManager(context.applicationContext, settingsManager)
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(scoreManager, achievementManager, context.applicationContext, settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}