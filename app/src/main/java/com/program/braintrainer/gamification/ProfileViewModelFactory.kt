package com.program.braintrainer.gamification

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.program.braintrainer.brainTrainerApp
import com.program.braintrainer.score.ScoreManager

class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            val app = context.brainTrainerApp
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(
                ScoreManager(app),
                app.achievementManager,
                app,
                app.settingsManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}