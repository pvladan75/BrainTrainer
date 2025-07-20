package com.program.braintrainer.gamification

import com.program.braintrainer.R

/**
 * Data klasa koja predstavlja jedan rang u igri.
 * @param title Naziv ranga (npr. "Početnik").
 * @param requiredXp Minimalni broj XP poena potreban za ovaj rang.
 * @param iconResId ID drawable resursa za ikonicu ranga.
 */
data class Rank(
    val title: String,
    val requiredXp: Int,
    val iconResId: Int
)

/**
 * Singleton objekat koji upravlja logikom rangova i XP poena.
 */
object RankManager {

    // Definišemo sve rangove u igri, od najnižeg ka najvišem.
    private val ranks = listOf(
        Rank("Početnik", 0, R.drawable.ic_rank_beginner),
        Rank("Učenik", 1000, R.drawable.ic_rank_student),
        Rank("Amater", 3000, R.drawable.ic_rank_amateur),
        Rank("Iskusni Igrač", 7000, R.drawable.ic_rank_experienced),
        Rank("Majstor", 15000, R.drawable.ic_rank_master),
        Rank("Velemajstor", 30000, R.drawable.ic_rank_grandmaster)
        // NAPOMENA: Potrebno je dodati ove ikonice u res/drawable folder
    )

    /**
     * Vraća trenutni rang igrača na osnovu ukupnog broja XP poena.
     * @param totalXp Ukupan broj XP poena igrača.
     * @return Odgovarajući Rank objekat.
     */
    fun getRankForXp(totalXp: Int): Rank {
        // Vraća poslednji rang za koji igrač ima dovoljno poena.
        return ranks.lastOrNull { totalXp >= it.requiredXp } ?: ranks.first()
    }

    /**
     * Vraća sledeći rang koji igrač može dostići.
     * @param currentRank Trenutni rang igrača.
     * @return Sledeći Rank objekat, ili null ako je igrač na najvišem rangu.
     */
    fun getNextRank(currentRank: Rank): Rank? {
        val currentIndex = ranks.indexOf(currentRank)
        return if (currentIndex < ranks.size - 1) {
            ranks[currentIndex + 1]
        } else {
            null // Nema sledećeg ranga
        }
    }
}
