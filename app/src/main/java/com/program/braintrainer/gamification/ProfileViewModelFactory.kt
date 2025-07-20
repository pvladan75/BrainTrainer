package com.program.braintrainer.gamification

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.program.braintrainer.score.ScoreManager

/**
 * Fabrika (Factory) za kreiranje instance ProfileViewModel-a.
 * Neophodna je jer ViewModel zavisi od ScoreManager-a.
 */
class ProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            val scoreManager = ScoreManager(context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(scoreManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
