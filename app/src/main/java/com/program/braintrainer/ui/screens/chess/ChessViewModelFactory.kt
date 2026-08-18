package com.program.braintrainer.ui.screens.chess

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.program.braintrainer.brainTrainerApp
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.chess.model.data.ProblemLoader
import com.program.braintrainer.score.ScoreManager

class ChessViewModelFactory(
    context: Context,
    private val module: Module,
    private val difficulty: Difficulty
) : ViewModelProvider.Factory {

    private val app = context.brainTrainerApp

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(ChessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChessViewModel(
                module = module,
                difficulty = difficulty,
                problemLoader = ProblemLoader(app),
                scoreManager = ScoreManager(app),
                settingsManager = app.settingsManager,
                achievementManager = app.achievementManager,
                // Nosi snapshot sesije preko ubijanja procesa.
                savedStateHandle = extras.createSavedStateHandle()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
