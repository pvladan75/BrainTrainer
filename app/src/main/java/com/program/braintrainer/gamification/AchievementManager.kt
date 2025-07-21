package com.program.braintrainer.gamification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.program.braintrainer.chess.model.Module
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.achievementDataStore: DataStore<Preferences> by preferencesDataStore(name = "achievements")

/**
 * Data klasa koja sadrži sve relevantne podatke o završenoj zagonetki,
 * potrebne za proveru uslova za otključavanje dostignuća.
 */
data class PuzzleResultData(
    val module: Module,
    val wasSuccess: Boolean,
    val mistakesMade: Int,
    val timeTakenSeconds: Int,
    val currentStreak: Int,
    val totalPuzzlesSolved: Int, // Potrebno je pratiti ukupan broj rešenih zagonetki
    val totalSolvedInModule: Int // Potrebno je pratiti broj rešenih u specifičnom modulu
)

/**
 * Menadžer koji upravlja logikom dostignuća.
 */
class AchievementManager(private val context: Context) {

    companion object {
        val UNLOCKED_ACHIEVEMENTS_KEY = stringSetPreferencesKey("unlocked_achievements")
    }

    // Flow koji emituje novo otključano dostignuće - za prikazivanje notifikacije (Snackbar)
    private val _newlyUnlockedAchievementFlow = MutableSharedFlow<Achievement>(extraBufferCapacity = 1)
    val newlyUnlockedAchievementFlow = _newlyUnlockedAchievementFlow.asSharedFlow()

    /**
     * Vraća Flow sa setom ID-jeva svih otključanih dostignuća.
     */
    val unlockedAchievementsFlow: Flow<Set<AchievementId>> = context.achievementDataStore.data
        .map { preferences ->
            (preferences[UNLOCKED_ACHIEVEMENTS_KEY] ?: emptySet()).mapNotNull { idString ->
                try {
                    AchievementId.valueOf(idString)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }.toSet()
        }

    /**
     * Glavna funkcija koja proverava da li su uslovi za neko dostignuće ispunjeni.
     * @param resultData Podaci o upravo završenoj zagonetki.
     */
    suspend fun checkAndUnlockAchievements(resultData: PuzzleResultData) {
        if (!resultData.wasSuccess) return // Dostignuća se otključavaju samo za uspešno rešene zagonetke

        val unlockedIds = unlockedAchievementsFlow.first()

        // Provera za "Prvi Koraci"
        if (resultData.totalPuzzlesSolved == 1 && !unlockedIds.contains(AchievementId.FIRST_PUZZLE_SOLVED)) {
            unlockAchievement(AchievementId.FIRST_PUZZLE_SOLVED)
        }

        // Provera za "Perfekcionista"
        if (resultData.mistakesMade == 0 && !unlockedIds.contains(AchievementId.PERFECT_PUZZLE)) {
            unlockAchievement(AchievementId.PERFECT_PUZZLE)
        }

        // Provera za "Nepogrešivi Niz"
        if (resultData.currentStreak >= 5 && !unlockedIds.contains(AchievementId.STREAK_5)) {
            unlockAchievement(AchievementId.STREAK_5)
        }

        // Provera za "Brzi Mislilac"
        if (resultData.timeTakenSeconds < 15 && !unlockedIds.contains(AchievementId.QUICK_THINKER)) {
            unlockAchievement(AchievementId.QUICK_THINKER)
        }

        // Provera za "Specijalista za Modul 1"
        if (resultData.module == Module.Module1 && resultData.totalSolvedInModule >= 10 && !unlockedIds.contains(AchievementId.MODULE_1_SPECIALIST)) {
            unlockAchievement(AchievementId.MODULE_1_SPECIALIST)
        }
    }

    /**
     * Čuva ID otključanog dostignuća i emituje ga u flow za notifikacije.
     */
    private suspend fun unlockAchievement(id: AchievementId) {
        context.achievementDataStore.edit { settings ->
            val currentUnlocked = settings[UNLOCKED_ACHIEVEMENTS_KEY] ?: emptySet()
            settings[UNLOCKED_ACHIEVEMENTS_KEY] = currentUnlocked + id.name
        }
        // Pronađi puno Achievement telo i emituj ga
        AchievementsList.allAchievements.find { it.id == id }?.let {
            _newlyUnlockedAchievementFlow.tryEmit(it)
        }
    }
}
