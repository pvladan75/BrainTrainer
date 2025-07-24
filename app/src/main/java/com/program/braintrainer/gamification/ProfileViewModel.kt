package com.program.braintrainer.gamification

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.program.braintrainer.R
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.chess.model.data.SettingsManager
import com.program.braintrainer.score.ScoreManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val currentRank: Rank,
    val nextRank: Rank?,
    val totalXp: Int,
    val requirementsForNextRank: List<AchievementProgress> = emptyList(),
    val canAdvance: Boolean = false
)

class ProfileViewModel(
    private val scoreManager: ScoreManager,
    private val achievementManager: AchievementManager,
    private val context: Context,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private var previousRank: Rank? = null

    val uiState: StateFlow<ProfileUiState?> =
        achievementManager.unlockedAchievementsFlow.map { unlockedIds ->
            val totalXp = scoreManager.getTotalXp()
            val currentRank = RankManager.determineRank(totalXp, unlockedIds)

            if (previousRank != null && currentRank.id != previousRank!!.id) {
                viewModelScope.launch {
                    if (settingsManager.settingsFlow.first().isSoundEnabled) {
                        MediaPlayer.create(context, R.raw.rank_up).start()
                    }
                }
            }
            previousRank = currentRank

            val nextRank = RankManager.getNextRank(currentRank)

            val requirements = nextRank?.let {
                val allAchievements = getAchievementsList(context)
                currentRank.requiredAchievements.map { achievementId ->
                    val achievement = allAchievements.find { it.id == achievementId }!!
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

    private fun getProgressForAchievement(id: AchievementId): Pair<Int, Int> {
        return when (id) {
            AchievementId.FIRST_PUZZLE_SOLVED -> Pair(scoreManager.getTotalPuzzlesSolved(), 1)
            AchievementId.SOLVE_10_M1 -> Pair(scoreManager.getSolvedInModule(Module.Module1), 10)
            AchievementId.STREAK_3_PERFECT -> Pair(scoreManager.getPerfectStreak(), 3)
            AchievementId.SOLVE_5_M1_MEDIUM -> Pair(scoreManager.getSolvedCount(Module.Module1, Difficulty.MEDIUM), 5)
            AchievementId.SOLVE_10_M2_PERFECT -> Pair(scoreManager.getPerfectSolvedCount(Module.Module2, Difficulty.EASY), 10)
            AchievementId.SOLVE_5_HARD -> Pair(
                scoreManager.getSolvedCount(Module.Module1, Difficulty.HARD) +
                        scoreManager.getSolvedCount(Module.Module2, Difficulty.HARD) +
                        scoreManager.getSolvedCount(Module.Module3, Difficulty.HARD), 5
            )
            AchievementId.SOLVE_5_M2_MEDIUM -> Pair(scoreManager.getSolvedCount(Module.Module2, Difficulty.MEDIUM), 5)
            AchievementId.SOLVE_10_M3 -> Pair(scoreManager.getSolvedInModule(Module.Module3), 10)
            AchievementId.SOLVE_5_M2_HARD -> Pair(scoreManager.getSolvedCount(Module.Module2, Difficulty.HARD), 5)
            AchievementId.SOLVE_5_M3_MEDIUM -> Pair(scoreManager.getSolvedCount(Module.Module3, Difficulty.MEDIUM), 5)
            AchievementId.SOLVE_20_M3_HARD_PERFECT -> Pair(scoreManager.getPerfectSolvedCount(Module.Module3, Difficulty.HARD), 20)
            AchievementId.SOLVE_20_M2_HARD_PERFECT -> Pair(scoreManager.getPerfectSolvedCount(Module.Module2, Difficulty.HARD), 20)
        }
    }
}