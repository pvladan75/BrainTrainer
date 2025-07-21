package com.program.braintrainer.score

import android.content.Context
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module

class ScoreManager(context: Context) {

    private val prefs = context.getSharedPreferences(SCORE_PREFS, Context.MODE_PRIVATE)

    companion object {
        private const val SCORE_PREFS = "BrainTrainerScores"
        private const val TOTAL_XP_KEY = "TOTAL_XP"
        private const val TOTAL_PUZZLES_SOLVED_KEY = "TOTAL_PUZZLES_SOLVED"
        private const val SOLVED_IN_MODULE_PREFIX = "SOLVED_IN_MODULE_"
        private const val PERFECT_STREAK_KEY = "PERFECT_STREAK"
        private const val SOLVED_COUNT_PREFIX = "SOLVED_COUNT_"
        // NOVO: Ključ za praćenje savršeno rešenih zagonetki
        private const val PERFECT_SOLVED_COUNT_PREFIX = "PERFECT_SOLVED_COUNT_"
    }

    private fun getKey(module: Module, difficulty: Difficulty): String {
        return "HIGHSCORE_${module.name}_${difficulty.name}"
    }

    fun saveScore(module: Module, difficulty: Difficulty, newScore: Int) {
        val key = getKey(module, difficulty)
        val currentHighScore = getHighScore(module, difficulty)
        if (newScore > currentHighScore) {
            prefs.edit().putInt(key, newScore).apply()
        }
    }

    fun getHighScore(module: Module, difficulty: Difficulty): Int {
        val key = getKey(module, difficulty)
        return prefs.getInt(key, 0)
    }

    fun getAllHighScores(): Map<Module, Map<Difficulty, Int>> {
        return Module.values().associateWith { module ->
            Difficulty.values().associateWith { difficulty ->
                getHighScore(module, difficulty)
            }
        }
    }

    fun addXp(xpToAdd: Int) {
        prefs.edit().putInt(TOTAL_XP_KEY, getTotalXp() + xpToAdd).apply()
    }

    fun getTotalXp(): Int {
        return prefs.getInt(TOTAL_XP_KEY, 0)
    }

    fun incrementTotalPuzzlesSolved() {
        prefs.edit().putInt(TOTAL_PUZZLES_SOLVED_KEY, getTotalPuzzlesSolved() + 1).apply()
    }

    fun getTotalPuzzlesSolved(): Int {
        return prefs.getInt(TOTAL_PUZZLES_SOLVED_KEY, 0)
    }

    fun incrementSolvedInModule(module: Module) {
        val key = SOLVED_IN_MODULE_PREFIX + module.name
        prefs.edit().putInt(key, getSolvedInModule(module) + 1).apply()
    }

    fun getSolvedInModule(module: Module): Int {
        val key = SOLVED_IN_MODULE_PREFIX + module.name
        return prefs.getInt(key, 0)
    }

    fun incrementPerfectStreak() {
        prefs.edit().putInt(PERFECT_STREAK_KEY, getPerfectStreak() + 1).apply()
    }

    fun resetPerfectStreak() {
        prefs.edit().putInt(PERFECT_STREAK_KEY, 0).apply()
    }

    fun getPerfectStreak(): Int {
        return prefs.getInt(PERFECT_STREAK_KEY, 0)
    }

    fun incrementSolvedCount(module: Module, difficulty: Difficulty) {
        val key = "${SOLVED_COUNT_PREFIX}${module.name}_${difficulty.name}"
        prefs.edit().putInt(key, getSolvedCount(module, difficulty) + 1).apply()
    }

    fun getSolvedCount(module: Module, difficulty: Difficulty): Int {
        val key = "${SOLVED_COUNT_PREFIX}${module.name}_${difficulty.name}"
        return prefs.getInt(key, 0)
    }

    // NOVO: Metode za praćenje broja rešenih BEZ GREŠKE po modulu i težini
    fun incrementPerfectSolvedCount(module: Module, difficulty: Difficulty) {
        val key = "${PERFECT_SOLVED_COUNT_PREFIX}${module.name}_${difficulty.name}"
        prefs.edit().putInt(key, getPerfectSolvedCount(module, difficulty) + 1).apply()
    }

    fun getPerfectSolvedCount(module: Module, difficulty: Difficulty): Int {
        val key = "${PERFECT_SOLVED_COUNT_PREFIX}${module.name}_${difficulty.name}"
        return prefs.getInt(key, 0)
    }

    fun resetAllScores() {
        prefs.edit().clear().apply()
    }
}
