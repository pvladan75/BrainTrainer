package com.program.braintrainer.score

import android.content.Context
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import androidx.core.content.edit

class ScoreManager(context: Context) {

    private val prefs = context.getSharedPreferences(SCORE_PREFS, Context.MODE_PRIVATE)

    companion object {
        private const val SCORE_PREFS = "BrainTrainerScores"
        private const val TOTAL_XP_KEY = "TOTAL_XP"
        // NOVO: Ključevi za praćenje statistike za dostignuća
        private const val TOTAL_PUZZLES_SOLVED_KEY = "TOTAL_PUZZLES_SOLVED"
        private const val SOLVED_IN_MODULE_PREFIX = "SOLVED_IN_MODULE_"
    }

    private fun getKey(module: Module, difficulty: Difficulty): String {
        return "HIGHSCORE_${module.name}_${difficulty.name}"
    }

    // --- Metode za XP i Highscore ---

    fun saveScore(module: Module, difficulty: Difficulty, newScore: Int) {
        val key = getKey(module, difficulty)
        val currentHighScore = getHighScore(module, difficulty)
        if (newScore > currentHighScore) {
            prefs.edit { putInt(key, newScore) }
        }
    }

    private fun getHighScore(module: Module, difficulty: Difficulty): Int {
        val key = getKey(module, difficulty)
        return prefs.getInt(key, 0)
    }

    fun getAllHighScores(): Map<Module, Map<Difficulty, Int>> {
        return Module.entries.associateWith { module ->
            Difficulty.entries.associateWith { difficulty ->
                getHighScore(module, difficulty)
            }
        }
    }

    fun addXp(xpToAdd: Int) {
        val currentXp = getTotalXp()
        prefs.edit { putInt(TOTAL_XP_KEY, currentXp + xpToAdd) }
    }

    fun getTotalXp(): Int {
        return prefs.getInt(TOTAL_XP_KEY, 0)
    }

    // --- NOVO: Metode za statistiku potrebnu za dostignuća ---

    /**
     * Povećava ukupan broj rešenih zagonetki za 1.
     */
    fun incrementTotalPuzzlesSolved() {
        val currentTotal = getTotalPuzzlesSolved()
        prefs.edit { putInt(TOTAL_PUZZLES_SOLVED_KEY, currentTotal + 1) }
    }

    /**
     * Vraća ukupan broj rešenih zagonetki.
     */
    fun getTotalPuzzlesSolved(): Int {
        return prefs.getInt(TOTAL_PUZZLES_SOLVED_KEY, 0)
    }

    /**
     * Povećava broj rešenih zagonetki za specifični modul.
     */
    fun incrementSolvedInModule(module: Module) {
        val key = SOLVED_IN_MODULE_PREFIX + module.name
        val currentTotal = getSolvedInModule(module)
        prefs.edit { putInt(key, currentTotal + 1) }
    }

    /**
     * Vraća broj rešenih zagonetki za specifični modul.
     */
    fun getSolvedInModule(module: Module): Int {
        val key = SOLVED_IN_MODULE_PREFIX + module.name
        return prefs.getInt(key, 0)
    }

    /**
     * Briše sve sačuvane rezultate, XP i statistiku.
     */
    fun resetAllScores() {
        prefs.edit { clear() }
    }
}
