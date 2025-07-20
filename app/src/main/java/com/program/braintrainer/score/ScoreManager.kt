package com.program.braintrainer.score

import android.content.Context
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module

class ScoreManager(context: Context) {

    private val prefs = context.getSharedPreferences(SCORE_PREFS, Context.MODE_PRIVATE)

    companion object {
        private const val SCORE_PREFS = "BrainTrainerScores"
        // NOVO: Ključ za čuvanje ukupnog XP-a
        private const val TOTAL_XP_KEY = "TOTAL_XP"
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

    /**
     * NOVO: Dodaje osvojene poene (XP) na ukupan zbir.
     * @param xpToAdd Broj poena za dodavanje.
     */
    fun addXp(xpToAdd: Int) {
        val currentXp = getTotalXp()
        prefs.edit().putInt(TOTAL_XP_KEY, currentXp + xpToAdd).apply()
    }

    /**
     * NOVO: Vraća ukupan broj XP poena koje je igrač sakupio.
     */
    fun getTotalXp(): Int {
        return prefs.getInt(TOTAL_XP_KEY, 0)
    }

    /**
     * AŽURIRANO: Briše sve sačuvane rezultate I UKUPAN XP.
     */
    fun resetAllScores() {
        prefs.edit().clear().apply()
    }
}
