package com.program.braintrainer.gamification

import com.program.braintrainer.R

/**
 * Enum koji jedinstveno identifikuje svako dostignuće.
 * Koristi se za čuvanje u DataStore-u.
 */
enum class AchievementId {
    FIRST_PUZZLE_SOLVED,
    PERFECT_PUZZLE,
    STREAK_5,
    MODULE_1_SPECIALIST,
    QUICK_THINKER
}

/**
 * Data klasa koja predstavlja jedno dostignuće.
 * @param id Jedinstveni ID.
 * @param title Naziv dostignuća.
 * @param description Opis kako se otključava.
 * @param iconResId Ikonica dostignuća.
 */
data class Achievement(
    val id: AchievementId,
    val title: String,
    val description: String,
    val iconResId: Int
)

/**
 * Singleton objekat koji sadrži listu svih mogućih dostignuća u igri.
 */
object AchievementsList {
    val allAchievements = listOf(
        Achievement(
            id = AchievementId.FIRST_PUZZLE_SOLVED,
            title = "Prvi Koraci",
            description = "Reši svoju prvu zagonetku.",
            iconResId = R.drawable.ic_achievement_first_step
        ),
        Achievement(
            id = AchievementId.PERFECT_PUZZLE,
            title = "Perfekcionista",
            description = "Reši zagonetku bez ijedne greške.",
            iconResId = R.drawable.ic_achievement_perfect
        ),
        Achievement(
            id = AchievementId.STREAK_5,
            title = "Nepogrešivi Niz",
            description = "Reši 5 zagonetki zaredom bez greške.",
            iconResId = R.drawable.ic_achievement_streak
        ),
        Achievement(
            id = AchievementId.MODULE_1_SPECIALIST,
            title = "Specijalista za Modul 1",
            description = "Reši 10 zagonetki u Modulu 1.",
            iconResId = R.drawable.ic_achievement_module1
        ),
        Achievement(
            id = AchievementId.QUICK_THINKER,
            title = "Brzi Mislilac",
            description = "Reši zagonetku za manje od 15 sekundi.",
            iconResId = R.drawable.ic_achievement_timer
        )
        // NAPOMENA: Potrebno je dodati ove ikonice u res/drawable folder
    )
}
