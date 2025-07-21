package com.program.braintrainer.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Data klasa koja predstavlja stanje za UI na ekranu sa dostignućima.
 * @param achievements Lista parova, gde svaki par sadrži dostignuće i boolean vrednost
 * koja označava da li je otključano.
 */
data class AchievementsUiState(
    val achievements: List<Pair<Achievement, Boolean>> = emptyList()
)

/**
 * ViewModel za AchievementsScreen.
 */
class AchievementsViewModel(achievementManager: AchievementManager) : ViewModel() {

    // Kreiramo StateFlow koji će UI automatski pratiti.
    val uiState: StateFlow<AchievementsUiState> =
        // Uzimamo flow otključanih dostignuća iz menadžera
        achievementManager.unlockedAchievementsFlow.map { unlockedIds ->
            // Za svako moguće dostignuće u igri, proveravamo da li se njegov ID nalazi
            // u setu otključanih ID-jeva i kreiramo listu parova.
            val achievementsWithStatus = AchievementsList.allAchievements.map { achievement ->
                Pair(achievement, unlockedIds.contains(achievement.id))
            }
            AchievementsUiState(achievementsWithStatus)
        }.stateIn(
            // Konfiguracija za StateFlow
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AchievementsUiState() // Početna vrednost je prazna lista
        )
}
