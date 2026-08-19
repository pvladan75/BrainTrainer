package com.program.braintrainer.ui.screens.training

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.program.braintrainer.brainTrainerApp
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.chess.model.data.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomTrainingUiState(
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    val module: Module = Module.Module1,
    val difficulty: Difficulty = Difficulty.EASY,
    val sessionSize: Int = DEFAULT_SIZE,
    val hideTimer: Boolean = false
) {
    companion object {
        const val DEFAULT_SIZE = 10

        /** Kratko za jedan predah, obično, i dugačko za pravu vežbu. */
        val SIZES = listOf(5, 10, 20)
    }
}

/**
 * Trening po meri: sastav sesije bira igrač umesto da uvek bude deset
 * nasumičnih zagonetki tekućeg modula.
 */
class CustomTrainingViewModel(
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomTrainingUiState())
    val uiState: StateFlow<CustomTrainingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val isPremium = settingsManager.settingsFlow.first().isPremiumUser
            _uiState.update { it.copy(isLoading = false, isPremium = isPremium) }
        }
    }

    fun onModuleSelected(module: Module) = _uiState.update { it.copy(module = module) }

    fun onDifficultySelected(difficulty: Difficulty) =
        _uiState.update { it.copy(difficulty = difficulty) }

    fun onSessionSizeSelected(size: Int) = _uiState.update { it.copy(sessionSize = size) }

    fun onHideTimerChanged(hide: Boolean) = _uiState.update { it.copy(hideTimer = hide) }
}

class CustomTrainingViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val app = context.brainTrainerApp

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomTrainingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CustomTrainingViewModel(app.settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
