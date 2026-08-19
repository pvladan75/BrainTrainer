package com.program.braintrainer.ui.screens.mistakes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.program.braintrainer.brainTrainerApp
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.chess.model.data.SettingsManager
import com.program.braintrainer.stats.AttemptRepository
import com.program.braintrainer.stats.OpenMistake
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Zagonetke jednog modula i težine koje čekaju revanš.
 *
 * Grupiše se po modulu i težini zato što sesija ne ume da meša pravila — svaki
 * modul ima svoja, a bodovanje zavisi od težine.
 */
data class MistakeGroup(
    val module: Module,
    val difficulty: Difficulty,
    val puzzleIds: List<String>,
    val lastAttemptAt: Long
)

data class MistakesUiState(
    val isLoading: Boolean = true,
    val isPremium: Boolean = false,
    val groups: List<MistakeGroup> = emptyList()
) {
    val totalOpen: Int get() = groups.sumOf { it.puzzleIds.size }
}

/**
 * Dnevnik grešaka: spisak zagonetki čiji poslednji pokušaj nije bio uspešan.
 */
class MistakesViewModel(
    private val attemptRepository: AttemptRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MistakesUiState())
    val uiState: StateFlow<MistakesUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /**
     * Poziva se i pri povratku sa partije: revanš menja spisak, pa bi zatečeni
     * prikaz nudio zagonetke koje su upravo rešene.
     */
    fun refresh() {
        viewModelScope.launch {
            val isPremium = settingsManager.settingsFlow.first().isPremiumUser
            val groups = groupMistakes(attemptRepository.openMistakes(MAX_MISTAKES))

            _uiState.update { it.copy(isLoading = false, isPremium = isPremium, groups = groups) }
        }
    }

    private companion object {
        /** Dovoljno da se vidi stanje, a da spisak ostane pregled a ne arhiva. */
        const val MAX_MISTAKES = 200
    }
}

/**
 * Otvorene greške u grupe po modulu i težini, najskorija grupa prva. Unutar
 * grupe se zadržava redosled koji je stigao iz baze — od najskorije greške.
 */
internal fun groupMistakes(mistakes: List<OpenMistake>): List<MistakeGroup> =
    mistakes
        .groupBy { it.module to it.difficulty }
        .map { (key, inGroup) ->
            MistakeGroup(
                module = key.first,
                difficulty = key.second,
                puzzleIds = inGroup.map { it.puzzleId },
                lastAttemptAt = inGroup.maxOf { it.lastAttemptAt }
            )
        }
        .sortedByDescending { it.lastAttemptAt }

class MistakesViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val app = context.brainTrainerApp

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MistakesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MistakesViewModel(app.attemptRepository, app.settingsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
