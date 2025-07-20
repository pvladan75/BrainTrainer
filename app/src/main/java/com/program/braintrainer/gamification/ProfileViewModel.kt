package com.program.braintrainer.gamification

import androidx.lifecycle.ViewModel
import com.program.braintrainer.score.ScoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Data klasa koja sadrži sve informacije potrebne za prikaz na ProfileScreen-u.
 */
data class ProfileUiState(
    val currentRank: Rank,
    val nextRank: Rank?,
    val totalXp: Int,
    val xpForNextRank: Int,
    val xpProgress: Float // Vrednost od 0.0f do 1.0f za progress bar
)

/**
 * ViewModel za ProfileScreen.
 * Upravlja stanjem i logikom vezanom za prikazivanje rankova i XP poena.
 */
class ProfileViewModel(private val scoreManager: ScoreManager) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState?>(null)
    val uiState: StateFlow<ProfileUiState?> = _uiState.asStateFlow()

    init {
        loadProfileData()
    }

    /**
     * Učitava podatke o XP poenima i preračunava ih u stanje za UI.
     */
    private fun loadProfileData() {
        val totalXp = scoreManager.getTotalXp()
        val currentRank = RankManager.getRankForXp(totalXp)
        val nextRank = RankManager.getNextRank(currentRank)

        val xpIntoCurrentRank = totalXp - currentRank.requiredXp
        val xpNeededForNext = if (nextRank != null) {
            nextRank.requiredXp - currentRank.requiredXp
        } else {
            0 // Ako je na najvišem rangu, progress je 0
        }

        val progress = if (xpNeededForNext > 0) {
            (xpIntoCurrentRank.toFloat() / xpNeededForNext.toFloat()).coerceIn(0f, 1f)
        } else {
            1f // Ako je max rang, progress bar je pun
        }

        _uiState.update {
            ProfileUiState(
                currentRank = currentRank,
                nextRank = nextRank,
                totalXp = totalXp,
                xpForNextRank = nextRank?.requiredXp ?: totalXp,
                xpProgress = progress
            )
        }
    }
}
