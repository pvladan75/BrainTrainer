package com.program.braintrainer.gamification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import android.media.MediaPlayer
import com.program.braintrainer.R
import com.program.braintrainer.chess.model.data.SettingsManager

private val Context.achievementDataStore: DataStore<Preferences> by preferencesDataStore(name = "achievements")

/**
 * Data klasa koja sadrži sve relevantne podatke o završenoj zagonetki,
 * potrebne za proveru uslova za otključavanje dostignuća.
 */
data class PuzzleResultData(
    val module: Module,
    val difficulty: Difficulty,
    val wasSuccess: Boolean,
    val mistakesMade: Int,
    val timeTakenSeconds: Int,
    val currentStreak: Int,
    val totalPuzzlesSolved: Int,
    val totalSolvedInModule: Int
)

/**
 * Menadžer koji upravlja logikom dostignuća.
 */
class AchievementManager(private val context: Context,
                         private val settingsManager: SettingsManager) {

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
        if (!resultData.wasSuccess) return

        val unlockedIds = unlockedAchievementsFlow.first()

        // === Početnik ===
        if (resultData.totalPuzzlesSolved >= 1 && !unlockedIds.contains(AchievementId.FIRST_PUZZLE_SOLVED)) {
            unlockAchievement(AchievementId.FIRST_PUZZLE_SOLVED)
        }

        if (resultData.module == Module.Module1 && resultData.totalSolvedInModule >= 10 &&
            !unlockedIds.contains(AchievementId.SOLVE_10_M1)) {
            unlockAchievement(AchievementId.SOLVE_10_M1)
        }

        if (resultData.currentStreak >= 3 && !unlockedIds.contains(AchievementId.STREAK_3_PERFECT)) {
            unlockAchievement(AchievementId.STREAK_3_PERFECT)
        }

        // TODO: Implement session tracking for FINISH_SESSION
        // TODO: Implement time tracking for SOLVE_UNDER_15_SECS

        // === Učenik ===
        if (resultData.module == Module.Module1 && resultData.difficulty == Difficulty.MEDIUM &&
            resultData.totalSolvedInModule >= 5 && !unlockedIds.contains(AchievementId.SOLVE_5_M1_MEDIUM)) {
            unlockAchievement(AchievementId.SOLVE_5_M1_MEDIUM)
        }
        if (resultData.module == Module.Module2 && resultData.difficulty == Difficulty.EASY &&
            resultData.mistakesMade == 0 && resultData.totalSolvedInModule >= 10 &&
            !unlockedIds.contains(AchievementId.SOLVE_10_M2_PERFECT)) {
            unlockAchievement(AchievementId.SOLVE_10_M2_PERFECT)
        }
        // TODO: Implement perfect session tracking for PERFECT_SESSION


        // === Amater ===
        if (resultData.difficulty == Difficulty.HARD && resultData.totalPuzzlesSolved >= 5 &&
            !unlockedIds.contains(AchievementId.SOLVE_5_HARD)) {
            unlockAchievement(AchievementId.SOLVE_5_HARD)
        }

        if (resultData.module == Module.Module2 && resultData.difficulty == Difficulty.MEDIUM &&
            resultData.totalSolvedInModule >= 5 && !unlockedIds.contains(AchievementId.SOLVE_5_M2_MEDIUM)) {
            unlockAchievement(AchievementId.SOLVE_5_M2_MEDIUM)
        }





        if (resultData.module == Module.Module3 && resultData.totalSolvedInModule >= 10 &&
            !unlockedIds.contains(AchievementId.SOLVE_10_M3)) {
            unlockAchievement(AchievementId.SOLVE_10_M3)
        }

        // === Iskusni Igrač ===
        if (resultData.module == Module.Module2 && resultData.difficulty == Difficulty.HARD &&
            resultData.totalSolvedInModule >= 5 && !unlockedIds.contains(AchievementId.SOLVE_5_M2_HARD)) {
            unlockAchievement(AchievementId.SOLVE_5_M2_HARD)
        }

        if (resultData.module == Module.Module3 && resultData.difficulty == Difficulty.MEDIUM &&
            resultData.totalSolvedInModule >= 5 && !unlockedIds.contains(AchievementId.SOLVE_5_M3_MEDIUM)) {
            unlockAchievement(AchievementId.SOLVE_5_M3_MEDIUM)
        }





        // === Majstor ===
        if (resultData.module == Module.Module3 && resultData.difficulty == Difficulty.HARD &&
            resultData.mistakesMade == 0 && resultData.totalSolvedInModule >= 20 &&
            !unlockedIds.contains(AchievementId.SOLVE_20_M3_HARD_PERFECT)) {
            unlockAchievement(AchievementId.SOLVE_20_M3_HARD_PERFECT)
        }

        if (resultData.module == Module.Module2 && resultData.difficulty == Difficulty.HARD &&
            resultData.mistakesMade == 0 && resultData.totalSolvedInModule >= 20 &&
            !unlockedIds.contains(AchievementId.SOLVE_20_M2_HARD_PERFECT)) {
            unlockAchievement(AchievementId.SOLVE_20_M2_HARD_PERFECT)
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
        // IZMENA: Pozivamo novu funkciju da dobijemo prevedenu listu
        getAchievementsList(context).find { it.id == id }?.let {
            _newlyUnlockedAchievementFlow.tryEmit(it)
            val areSoundsEnabled = settingsManager.settingsFlow.first().isSoundEnabled
            if (areSoundsEnabled) {
                MediaPlayer.create(context, R.raw.achievement_unlocked).start()
            }
        }
    }
}