package com.program.braintrainer.gamification

import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module

/**
 * Predstavlja otključane module i težine za određeni rang.
 */
data class UnlockedContent(
    val module: Module,
    val availableDifficulties: List<Difficulty>
)

/**
 * Data klasa koja predstavlja jedan rang u igri.
 * @param title Naziv ranga.
 * @param requiredXp Minimalni XP potreban za ovaj rang.
 * @param requiredAchievements Lista dostignuća potrebnih za prelazak SA ovog ranga na sledeći.
 * @param unlockedContent Lista modula i težina dostupnih NA ovom rangu.
 */
data class Rank(
    val title: String,
    val requiredXp: Int,
    val requiredAchievements: List<AchievementId>,
    val unlockedContent: List<UnlockedContent>
)

/**
 * Singleton objekat koji upravlja logikom rangova i XP poena.
 */
object RankManager {

    private val ranks = listOf(
        Rank(
            title = "Početnik",
            requiredXp = 0,
            requiredAchievements = listOf(
                AchievementId.FIRST_PUZZLE_SOLVED,
                AchievementId.SOLVE_10_M1,
                AchievementId.STREAK_3_PERFECT,
                AchievementId.FINISH_SESSION,
                AchievementId.SOLVE_UNDER_15_SECS
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
                AchievementId.PERFECT_SESSION,
                AchievementId.SOLVE_1_M2_UNDER_30_SECS,
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
                AchievementId.SOLVE_1_HARD_UNDER_60_SECS,
                AchievementId.SOLVE_1_MEDIUM_UNDER_45_SECS,
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
                AchievementId.SOLVE_5_M3_MEDIUM,
                AchievementId.SOLVE_1_M2_HARD_PERFECT_UNDER_60_SECS,
                AchievementId.SOLVE_1_M3_MEDIUM_PERFECT_UNDER_45_SECS
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
                AchievementId.SOLVE_20_M2_HARD_PERFECT,
                AchievementId.SOLVE_1_M3_HARD_PERFECT_UNDER_120_SECS,
                AchievementId.SOLVE_1_M3_MEDIUM_PERFECT_UNDER_60_SECS
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
            requiredAchievements = emptyList(), // Nema više dostignuća za prelazak
            unlockedContent = listOf(
                UnlockedContent(Module.Module1, Difficulty.values().toList()),
                UnlockedContent(Module.Module2, Difficulty.values().toList()),
                UnlockedContent(Module.Module3, Difficulty.values().toList())
            )
        )
    )

    fun getRankForXp(totalXp: Int): Rank {
        // Vraća poslednji rang za koji igrač ima dovoljno poena.
        return ranks.lastOrNull { totalXp >= it.requiredXp } ?: ranks.first()
    }

    fun getNextRank(currentRank: Rank): Rank? {
        val currentIndex = ranks.indexOf(currentRank)
        return if (currentIndex < ranks.size - 1) {
            ranks[currentIndex + 1]
        } else {
            null
        }
    }

    /**
     * Vraća listu dostupnih težina za dati rang i modul.
     */
    fun getAvailableDifficultiesFor(rank: Rank, module: Module): List<Difficulty> {
        return rank.unlockedContent.find { it.module == module }?.availableDifficulties ?: emptyList()
    }
}
