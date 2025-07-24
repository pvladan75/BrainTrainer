package com.program.braintrainer.gamification

import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module

enum class RankId {
    BEGINNER,
    STUDENT,
    AMATEUR,
    EXPERIENCED,
    MASTER,
    GRANDMASTER
}

data class UnlockedContent(
    val module: Module,
    val availableDifficulties: List<Difficulty>
)

data class Rank(
    val id: RankId,
    val requiredXp: Int,
    val requiredAchievements: List<AchievementId>,
    val unlockedContent: List<UnlockedContent>
)

object RankManager {

    val ranks = listOf(
        Rank(
            id = RankId.BEGINNER,
            requiredXp = 0,
            requiredAchievements = listOf(
                AchievementId.FIRST_PUZZLE_SOLVED,
                AchievementId.SOLVE_10_M1,
                AchievementId.STREAK_3_PERFECT
            ),
            unlockedContent = listOf(
                UnlockedContent(Module.Module1, listOf(Difficulty.EASY)),
                UnlockedContent(Module.Module2, emptyList()),
                UnlockedContent(Module.Module3, emptyList())
            )
        ),
        Rank(
            id = RankId.STUDENT,
            requiredXp = 1000,
            requiredAchievements = listOf(
                AchievementId.SOLVE_5_M1_MEDIUM,
                AchievementId.SOLVE_10_M2_PERFECT
            ),
            unlockedContent = listOf(
                UnlockedContent(Module.Module1, listOf(Difficulty.EASY, Difficulty.MEDIUM)),
                UnlockedContent(Module.Module2, listOf(Difficulty.EASY)),
                UnlockedContent(Module.Module3, emptyList())
            )
        ),
        Rank(
            id = RankId.AMATEUR,
            requiredXp = 3000,
            requiredAchievements = listOf(
                AchievementId.SOLVE_5_HARD,
                AchievementId.SOLVE_5_M2_MEDIUM,
                AchievementId.SOLVE_10_M3
            ),
            unlockedContent = listOf(
                UnlockedContent(Module.Module1, listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD)),
                UnlockedContent(Module.Module2, listOf(Difficulty.EASY, Difficulty.MEDIUM)),
                UnlockedContent(Module.Module3, listOf(Difficulty.EASY))
            )
        ),
        Rank(
            id = RankId.EXPERIENCED,
            requiredXp = 7000,
            requiredAchievements = listOf(
                AchievementId.SOLVE_5_M2_HARD,
                AchievementId.SOLVE_5_M3_MEDIUM
            ),
            unlockedContent = listOf(
                UnlockedContent(Module.Module1, Difficulty.values().toList()),
                UnlockedContent(Module.Module2, listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD)),
                UnlockedContent(Module.Module3, listOf(Difficulty.EASY, Difficulty.MEDIUM))
            )
        ),
        Rank(
            id = RankId.MASTER,
            requiredXp = 15000,
            requiredAchievements = listOf(
                AchievementId.SOLVE_20_M3_HARD_PERFECT,
                AchievementId.SOLVE_20_M2_HARD_PERFECT
            ),
            unlockedContent = listOf(
                UnlockedContent(Module.Module1, Difficulty.values().toList()),
                UnlockedContent(Module.Module2, Difficulty.values().toList()),
                UnlockedContent(Module.Module3, Difficulty.values().toList())
            )
        ),
        Rank(
            id = RankId.GRANDMASTER,
            requiredXp = 30000,
            requiredAchievements = emptyList(),
            unlockedContent = listOf(
                UnlockedContent(Module.Module1, Difficulty.values().toList()),
                UnlockedContent(Module.Module2, Difficulty.values().toList()),
                UnlockedContent(Module.Module3, Difficulty.values().toList())
            )
        )
    )

    fun getRankForXp(totalXp: Int): Rank {
        return ranks.lastOrNull { totalXp >= it.requiredXp } ?: ranks.first()
    }

    fun determineRank(totalXp: Int, unlockedAchievements: Set<AchievementId>): Rank {
        var currentRank = ranks.first()
        for (i in 0 until ranks.size - 1) {
            val rank = ranks[i]
            val nextRank = ranks[i + 1]
            val hasEnoughXp = totalXp >= nextRank.requiredXp
            val hasAllAchievements = unlockedAchievements.containsAll(rank.requiredAchievements)
            if (hasEnoughXp && hasAllAchievements) {
                currentRank = nextRank
            } else {
                break
            }
        }
        return currentRank
    }

    fun getNextRank(currentRank: Rank): Rank? {
        val currentIndex = ranks.indexOfFirst { it.id == currentRank.id }
        return if (currentIndex != -1 && currentIndex < ranks.size - 1) {
            ranks[currentIndex + 1]
        } else {
            null
        }
    }

    fun getAvailableDifficultiesFor(rank: Rank, module: Module): List<Difficulty> {
        return rank.unlockedContent.find { it.module == module }?.availableDifficulties ?: emptyList()
    }
}