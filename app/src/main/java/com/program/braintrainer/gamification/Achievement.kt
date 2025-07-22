package com.program.braintrainer.gamification

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
 * Singleton objekat koji sadrži listu svih mogućih dostignuća u igri.
 */
object AchievementsList {
    val allAchievements = listOf(
        // --- Početnik ---
        Achievement(AchievementId.FIRST_PUZZLE_SOLVED, "Prvi Koraci", "Reši svoju prvu zagonetku."),
        Achievement(AchievementId.SOLVE_10_M1, "Istraživač Modula 1", "Reši 10 zadataka u Modulu 1."),
        Achievement(AchievementId.STREAK_3_PERFECT, "U Nizu", "Reši 3 uzastopna zadatka bez greške."),

        // --- Učenik ---
        Achievement(AchievementId.SOLVE_5_M1_MEDIUM, "Srednja Klasa", "Reši 5 srednjih zadataka u Modulu 1."),
        Achievement(AchievementId.SOLVE_10_M2_PERFECT, "Oprezni Istraživač", "Reši 10 lakih zadataka u modulu 2 bez greške."),

        // --- Amater ---
        Achievement(AchievementId.SOLVE_5_HARD, "Izazov Prihvaćen", "Reši 5 zadataka na nivou Teško."),
        Achievement(AchievementId.SOLVE_5_M2_MEDIUM, "Takmičar", "Reši 5 zadataka srednje težine u Modulu 2."),
        Achievement(AchievementId.SOLVE_10_M3, "Kraljev Gambit", "Reši 10 zadataka u Modulu 3."),

        // --- Iskusni Igrač ---
        Achievement(AchievementId.SOLVE_5_M2_HARD, "Strateg", "Reši 5 zadataka na nivou Teško u Modulu 2."),
        Achievement(AchievementId.SOLVE_5_M3_MEDIUM, "Kraljev Lovac", "Reši 5 zadataka srednje težine u Modulu 3."),

        // --- Majstor ---
        Achievement(AchievementId.SOLVE_20_M3_HARD_PERFECT, "Virtuoz", "Reši 20 zadataka na nivou Teško u Modulu 3 bez greške."),
        Achievement(AchievementId.SOLVE_20_M2_HARD_PERFECT, "Dominacija", "Reši 20 zadataka težine Teško u Modulu 2 bez greške.")
    )
}
