package com.program.braintrainer.gamification

import android.content.Context
import com.program.braintrainer.R

/**
 * Enum koji jedinstveno identifikuje svako dostignuće.
 */
enum class AchievementId {
    // === Dostignuća za prelazak sa "Početnik" na "Učenik" ===
    FIRST_PUZZLE_SOLVED,
    SOLVE_10_M1,
    STREAK_3_PERFECT,

    // === Dostignuća za prelazak sa "Učenik" na "Amater" ===
    SOLVE_5_M1_MEDIUM,
    SOLVE_10_M2_PERFECT,

    // === Dostignuća za prelazak sa "Amater" na "Iskusni Igrač" ===
    SOLVE_5_HARD,
    SOLVE_5_M2_MEDIUM,
    SOLVE_10_M3,

    // === Dostignuća za prelazak sa "Iskusni Igrač" na "Majstor" ===
    SOLVE_5_M2_HARD,
    SOLVE_5_M3_MEDIUM,

    // === Dostignuća za prelazak sa "Majstor" na "Velemajstor" ===
    SOLVE_20_M3_HARD_PERFECT,
    SOLVE_20_M2_HARD_PERFECT
}

/**
 * Data klasa koja predstavlja jedno dostignuće.
 */
data class Achievement(
    val id: AchievementId,
    val title: String,
    val description: String
)

/**
 * Funkcija koja vraća listu svih mogućih dostignuća sa lokalizovanim tekstom.
 * @param context Kontekst aplikacije potreban za pristup string resursima.
 * @return Lista svih Achievement objekata.
 */
fun getAchievementsList(context: Context): List<Achievement> {
    return listOf(
        // --- Početnik ---
        Achievement(AchievementId.FIRST_PUZZLE_SOLVED, context.getString(R.string.achievement_first_steps_title), context.getString(R.string.achievement_first_steps_desc)),
        Achievement(AchievementId.SOLVE_10_M1, context.getString(R.string.achievement_m1_explorer_title), context.getString(R.string.achievement_m1_explorer_desc)),
        Achievement(AchievementId.STREAK_3_PERFECT, context.getString(R.string.achievement_on_a_streak_title), context.getString(R.string.achievement_on_a_streak_desc)),

        // --- Učenik ---
        Achievement(AchievementId.SOLVE_5_M1_MEDIUM, context.getString(R.string.achievement_middle_class_title), context.getString(R.string.achievement_middle_class_desc)),
        Achievement(AchievementId.SOLVE_10_M2_PERFECT, context.getString(R.string.achievement_careful_explorer_title), context.getString(R.string.achievement_careful_explorer_desc)),

        // --- Amater ---
        Achievement(AchievementId.SOLVE_5_HARD, context.getString(R.string.achievement_challenge_accepted_title), context.getString(R.string.achievement_challenge_accepted_desc)),
        Achievement(AchievementId.SOLVE_5_M2_MEDIUM, context.getString(R.string.achievement_competitor_title), context.getString(R.string.achievement_competitor_desc)),
        Achievement(AchievementId.SOLVE_10_M3, context.getString(R.string.achievement_kings_gambit_title), context.getString(R.string.achievement_kings_gambit_desc)),

        // --- Iskusni Igrač ---
        Achievement(AchievementId.SOLVE_5_M2_HARD, context.getString(R.string.achievement_strategist_title), context.getString(R.string.achievement_strategist_desc)),
        Achievement(AchievementId.SOLVE_5_M3_MEDIUM, context.getString(R.string.achievement_kings_hunter_title), context.getString(R.string.achievement_kings_hunter_desc)),

        // --- Majstor ---
        Achievement(AchievementId.SOLVE_20_M3_HARD_PERFECT, context.getString(R.string.achievement_virtuoso_title), context.getString(R.string.achievement_virtuoso_desc)),
        Achievement(AchievementId.SOLVE_20_M2_HARD_PERFECT, context.getString(R.string.achievement_domination_title), context.getString(R.string.achievement_domination_desc))
    )
}