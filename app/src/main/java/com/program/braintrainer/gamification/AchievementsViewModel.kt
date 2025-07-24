package com.program.braintrainer.gamification

import android.content.Context
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
    private val scoreManager: ScoreManager,
    // IZMENA: Dodajemo Context u konstruktor
    private val context: Context
) : ViewModel() {

    val uiState: StateFlow<AchievementsUiState> =
        achievementManager.unlockedAchievementsFlow.map { unlockedIds ->
            // IZMENA: Pozivamo novu funkciju da dobijemo prevedenu listu
            val progressList = getAchievementsList(context).map { achievement ->
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