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

data class AchievementProgress(
    val achievement: Achievement,
    val isUnlocked: Boolean,
    val currentProgress: Int,
    val target: Int
)

data class AchievementsUiState(
    val achievements: List<AchievementProgress> = emptyList()
)

class AchievementsViewModel(
    achievementManager: AchievementManager,
    private val scoreManager: ScoreManager
) : ViewModel() {

    val uiState: StateFlow<AchievementsUiState> =
        achievementManager.unlockedAchievementsFlow.map { unlockedIds ->
            val progressList = AchievementsList.allAchievements.map { achievement ->
                val (current, target) = getProgressForAchievement(achievement.id)
                AchievementProgress(
                    achievement = achievement,
                    isUnlocked = unlockedIds.contains(achievement.id),
                    currentProgress = current,
                    target = target
                )
            }
            AchievementsUiState(progressList)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AchievementsUiState()
        )

    /**
     * Pomoćna funkcija koja vraća trenutni i ciljni napredak za dato dostignuće.
     */
    private fun getProgressForAchievement(id: AchievementId): Pair<Int, Int> {
        return when (id) {
            // Početnik
            AchievementId.FIRST_PUZZLE_SOLVED -> Pair(scoreManager.getTotalPuzzlesSolved(), 1)
            AchievementId.SOLVE_10_M1 -> Pair(scoreManager.getSolvedInModule(Module.Module1), 10)
            AchievementId.STREAK_3_PERFECT -> Pair(scoreManager.getPerfectStreak(), 3)
            AchievementId.FINISH_SESSION -> Pair(0, 1) // Zahteva složeniju logiku praćenja sesije
            AchievementId.SOLVE_UNDER_15_SECS -> Pair(0, 1) // Zahteva praćenje najboljeg vremena

            // Učenik
            AchievementId.SOLVE_5_M1_MEDIUM -> Pair(scoreManager.getSolvedCount(Module.Module1, Difficulty.MEDIUM), 5)
            AchievementId.PERFECT_SESSION -> Pair(0, 1) // Zahteva složeniju logiku
            AchievementId.SOLVE_1_M2_UNDER_30_SECS -> Pair(0, 1)
            AchievementId.SOLVE_10_M2_PERFECT -> Pair(scoreManager.getPerfectSolvedCount(Module.Module2, Difficulty.EASY), 10)

            // Amater
            AchievementId.SOLVE_5_HARD -> Pair(scoreManager.getSolvedCount(Module.Module1, Difficulty.HARD) + scoreManager.getSolvedCount(Module.Module2, Difficulty.HARD) + scoreManager.getSolvedCount(Module.Module3, Difficulty.HARD), 5)
            AchievementId.SOLVE_5_M2_MEDIUM -> Pair(scoreManager.getSolvedCount(Module.Module2, Difficulty.MEDIUM), 5)
            AchievementId.SOLVE_1_HARD_UNDER_60_SECS -> Pair(0, 1)
            AchievementId.SOLVE_1_MEDIUM_UNDER_45_SECS -> Pair(0, 1)
            AchievementId.SOLVE_10_M3 -> Pair(scoreManager.getSolvedInModule(Module.Module3), 10)

            // Iskusni Igrač
            AchievementId.SOLVE_5_M2_HARD -> Pair(scoreManager.getSolvedCount(Module.Module2, Difficulty.HARD), 5)
            AchievementId.SOLVE_5_M3_MEDIUM -> Pair(scoreManager.getSolvedCount(Module.Module3, Difficulty.MEDIUM), 5)
            AchievementId.SOLVE_1_M2_HARD_PERFECT_UNDER_60_SECS -> Pair(0, 1)
            AchievementId.SOLVE_1_M3_MEDIUM_PERFECT_UNDER_45_SECS -> Pair(0, 1)

            // Majstor
            AchievementId.SOLVE_20_M3_HARD_PERFECT -> Pair(scoreManager.getPerfectSolvedCount(Module.Module3, Difficulty.HARD), 20)
            AchievementId.SOLVE_20_M2_HARD_PERFECT -> Pair(scoreManager.getPerfectSolvedCount(Module.Module2, Difficulty.HARD), 20)
            AchievementId.SOLVE_1_M3_HARD_PERFECT_UNDER_120_SECS -> Pair(0, 1)
            AchievementId.SOLVE_1_M3_MEDIUM_PERFECT_UNDER_60_SECS -> Pair(0, 1)
        }
    }
}
