package com.program.braintrainer.gamification

/**
 * Enum koji jedinstveno identifikuje svako dostignuće.
 * Ovo nam omogućava da lako pratimo koja su dostignuća otključana.
 */
enum class AchievementId {
    // === Dostignuća za prelazak sa "Početnik" na "Učenik" ===
    FIRST_PUZZLE_SOLVED,
    SOLVE_10_M1,
    STREAK_3_PERFECT,
    FINISH_SESSION,
    SOLVE_UNDER_15_SECS,

    // === Dostignuća za prelazak sa "Učenik" na "Amater" ===
    SOLVE_5_M1_MEDIUM,
    PERFECT_SESSION,
    SOLVE_1_M2_UNDER_30_SECS,
    SOLVE_10_M2_PERFECT,

    // === Dostignuća za prelazak sa "Amater" na "Iskusni Igrač" ===
    SOLVE_5_HARD,
    SOLVE_5_M2_MEDIUM,
    SOLVE_1_HARD_UNDER_60_SECS,
    SOLVE_1_MEDIUM_UNDER_45_SECS,
    SOLVE_10_M3,

    // === Dostignuća za prelazak sa "Iskusni Igrač" na "Majstor" ===
    SOLVE_5_M2_HARD,
    SOLVE_5_M3_MEDIUM,
    SOLVE_1_M2_HARD_PERFECT_UNDER_60_SECS,
    SOLVE_1_M3_MEDIUM_PERFECT_UNDER_45_SECS,

    // === Dostignuća za prelazak sa "Majstor" na "Velemajstor" ===
    SOLVE_20_M3_HARD_PERFECT,
    SOLVE_20_M2_HARD_PERFECT,
    SOLVE_1_M3_HARD_PERFECT_UNDER_120_SECS,
    SOLVE_1_M3_MEDIUM_PERFECT_UNDER_60_SECS
}

/**
 * Data klasa koja predstavlja jedno dostignuće.
 * @param id Jedinstveni ID.
 * @param title Naziv dostignuća.
 * @param description Opis kako se otključava.
 */
data class Achievement(
    val id: AchievementId,
    val title: String,
    val description: String
)

/**
 * Singleton objekat koji sadrži listu svih mogućih dostignuća u igri.
 * Ovo je centralno mesto za definisanje svih dostignuća.
 */
object AchievementsList {
    val allAchievements = listOf(
        // --- Početnik ---
        Achievement(AchievementId.FIRST_PUZZLE_SOLVED, "Prvi Koraci", "Reši svoju prvu zagonetku."),
        Achievement(AchievementId.SOLVE_10_M1, "Istraživač Modula 1", "Reši 10 zadataka u Modulu 1."),
        Achievement(AchievementId.STREAK_3_PERFECT, "U Nizu", "Reši 3 uzastopna zadatka bez greške."),
        Achievement(AchievementId.FINISH_SESSION, "Maratonac", "Završi jednu celu sesiju."),
        Achievement(AchievementId.SOLVE_UNDER_15_SECS, "Brzi Mislilac", "Reši zadatak za manje od 15 sekundi."),

        // --- Učenik ---
        Achievement(AchievementId.SOLVE_5_M1_MEDIUM, "Srednja Klasa", "Reši 5 srednjih zadataka u Modulu 1."),
        Achievement(AchievementId.PERFECT_SESSION, "Perfektna Sesija", "Reši čitavu sesiju bez greške."),
        Achievement(AchievementId.SOLVE_1_M2_UNDER_30_SECS, "Brzopotezno Učenje", "Reši 1 zadatak u modulu 2 za manje od 30 sekundi."),
        Achievement(AchievementId.SOLVE_10_M2_PERFECT, "Oprezni Istraživač", "Reši 10 zadataka u modulu 2 bez greške."),

        // --- Amater ---
        Achievement(AchievementId.SOLVE_5_HARD, "Izazov Prihvaćen", "Reši 5 zadataka na nivou Teško."),
        Achievement(AchievementId.SOLVE_5_M2_MEDIUM, "Takmičar", "Reši 5 zadataka srednje težine u Modulu 2."),
        Achievement(AchievementId.SOLVE_1_HARD_UNDER_60_SECS, "Pod Pritiskom", "Reši 1 zadatak težine Teško za manje od 60 sekundi."),
        Achievement(AchievementId.SOLVE_1_MEDIUM_UNDER_45_SECS, "Fokusiran", "Reši 1 zadatak srednje težine za manje od 45 sekundi."),
        Achievement(AchievementId.SOLVE_10_M3, "Kraljev Gambit", "Reši 10 zadataka u Modulu 3."),

        // --- Iskusni Igrač ---
        Achievement(AchievementId.SOLVE_5_M2_HARD, "Strateg", "Reši 5 zadataka na nivou Teško u Modulu 2."),
        Achievement(AchievementId.SOLVE_5_M3_MEDIUM, "Kraljev Lovac", "Reši 5 zadataka srednje težine u Modulu 3."),
        Achievement(AchievementId.SOLVE_1_M2_HARD_PERFECT_UNDER_60_SECS, "Hladnokrvnost", "Reši 1 zadatak težine Teško u Modulu 2 za manje od 60 sekundi bez greške."),
        Achievement(AchievementId.SOLVE_1_M3_MEDIUM_PERFECT_UNDER_45_SECS, "Preciznost", "Reši 1 zadatak srednje težine u Modulu 3 za manje od 45 sekundi bez greške."),

        // --- Majstor ---
        Achievement(AchievementId.SOLVE_20_M3_HARD_PERFECT, "Virtuoz", "Reši 20 zadataka na nivou Teško u Modulu 3 bez greške."),
        Achievement(AchievementId.SOLVE_20_M2_HARD_PERFECT, "Dominacija", "Reši 20 zadataka težine Teško u Modulu 2 bez greške."),
        Achievement(AchievementId.SOLVE_1_M3_HARD_PERFECT_UNDER_120_SECS, "Genijalnost", "Reši 1 zadatak težine Teško u Modulu 3 za manje od 120 sekundi bez greške."),
        Achievement(AchievementId.SOLVE_1_M3_MEDIUM_PERFECT_UNDER_60_SECS, "Elegancija", "Reši 1 zadatak srednje težine u Modulu 3 za manje od 60 sekundi bez greške.")
    )
}
