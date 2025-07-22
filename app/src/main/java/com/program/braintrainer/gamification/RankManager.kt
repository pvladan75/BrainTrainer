package com.program.braintrainer.gamification

import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module

data class UnlockedContent(
    val module: Module,
    val availableDifficulties: List<Difficulty>
)

data class Rank(
    val title: String,
    val requiredXp: Int,
    val requiredAchievements: List<AchievementId>,
    val unlockedContent: List<UnlockedContent>
)

object RankManager {

    private val ranks = listOf(
        Rank(
            title = "Početnik",
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
            title = "Učenik",
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
            title = "Amater",
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
            title = "Iskusni Igrač",
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
            title = "Majstor",
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
            title = "Velemajstor",
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
        val currentIndex = ranks.indexOf(currentRank)
        return if (currentIndex < ranks.size - 1) {
            ranks[currentIndex + 1]
        } else {
            null
        }
    }

    fun getAvailableDifficultiesFor(rank: Rank, module: Module): List<Difficulty> {
        return rank.unlockedContent.find { it.module == module }?.availableDifficulties ?: emptyList()
    }
}
