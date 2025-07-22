package com.program.braintrainer.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.score.ScoreManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val currentRank: Rank,
    val nextRank: Rank?,
    val totalXp: Int,
    // Lista dostignuća potrebnih za sledeći rang, sa napretkom
    val requirementsForNextRank: List<AchievementProgress> = emptyList(),
    val canAdvance: Boolean = false
)

class ProfileViewModel(
    private val scoreManager: ScoreManager,
    private val achievementManager: AchievementManager
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState?> =
        // Kombinujemo podatke iz dva flow-a: XP (implicitno kroz ScoreManager) i otključana dostignuća
        achievementManager.unlockedAchievementsFlow.map { unlockedIds ->
            val totalXp = scoreManager.getTotalXp()
            // KORISTIMO NOVU, ISPRAVNU FUNKCIJU ZA ODREĐIVANJE RANGA
            val currentRank = RankManager.determineRank(totalXp, unlockedIds)
            val nextRank = RankManager.getNextRank(currentRank)

            val requirements = nextRank?.let {
                // Zahtevi za sledeći rang su dostignuća definisana u TRENUTNOM rangu
                currentRank.requiredAchievements.map { achievementId ->
                    val achievement = AchievementsList.allAchievements.find { it.id == achievementId }!!
                    val (current, target) = getProgressForAchievement(achievementId)
                    AchievementProgress(
                        achievement = achievement,
                        isUnlocked = unlockedIds.contains(achievementId),
                        currentProgress = current,
                        target = target
                    )
                }
            } ?: emptyList()

            val canAdvance = nextRank != null &&
                    totalXp >= nextRank.requiredXp &&
                    requirements.all { it.isUnlocked }

            ProfileUiState(
                currentRank = currentRank,
                nextRank = nextRank,
                totalXp = totalXp,
                requirementsForNextRank = requirements,
                canAdvance = canAdvance
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Pomoćna funkcija koja vraća trenutni i ciljni napredak za dato dostignuće.
     * U naprednijoj arhitekturi, ova logika bi bila izdvojena u zajednički servis
     * da se ne bi ponavljala u AchievementsViewModel-u.
     */
    private fun getProgressForAchievement(id: AchievementId): Pair<Int, Int> {
        return when (id) {
            // Početnik
            AchievementId.FIRST_PUZZLE_SOLVED -> Pair(scoreManager.getTotalPuzzlesSolved(), 1)
            AchievementId.SOLVE_10_M1 -> Pair(scoreManager.getSolvedInModule(Module.Module1), 10)
            AchievementId.STREAK_3_PERFECT -> Pair(scoreManager.getPerfectStreak(), 3)


            // Učenik
            AchievementId.SOLVE_5_M1_MEDIUM -> Pair(scoreManager.getSolvedCount(Module.Module1, Difficulty.MEDIUM), 5)

            AchievementId.SOLVE_10_M2_PERFECT -> Pair(scoreManager.getPerfectSolvedCount(Module.Module2, Difficulty.EASY), 10)

            // Amater
            AchievementId.SOLVE_5_HARD -> Pair(
                scoreManager.getSolvedCount(Module.Module1, Difficulty.HARD) +
                        scoreManager.getSolvedCount(Module.Module2, Difficulty.HARD) +
                        scoreManager.getSolvedCount(Module.Module3, Difficulty.HARD), 5
            )
            AchievementId.SOLVE_5_M2_MEDIUM -> Pair(scoreManager.getSolvedCount(Module.Module2, Difficulty.MEDIUM), 5)

            AchievementId.SOLVE_10_M3 -> Pair(scoreManager.getSolvedInModule(Module.Module3), 10)

            // Iskusni Igrač
            AchievementId.SOLVE_5_M2_HARD -> Pair(scoreManager.getSolvedCount(Module.Module2, Difficulty.HARD), 5)
            AchievementId.SOLVE_5_M3_MEDIUM -> Pair(scoreManager.getSolvedCount(Module.Module3, Difficulty.MEDIUM), 5)


            // Majstor
            AchievementId.SOLVE_20_M3_HARD_PERFECT -> Pair(scoreManager.getPerfectSolvedCount(Module.Module3, Difficulty.HARD), 20)
            AchievementId.SOLVE_20_M2_HARD_PERFECT -> Pair(scoreManager.getPerfectSolvedCount(Module.Module2, Difficulty.HARD), 20)

        }
    }
}
