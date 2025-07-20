package com.program.braintrainer.score

import android.content.Context
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module

class ScoreManager(context: Context) {

    // Pristup SharedPreferences
    private val prefs = context.getSharedPreferences(SCORE_PREFS, Context.MODE_PRIVATE)

    companion object {
        // Jedinstveno ime za naš SharedPreferences fajl
        private const val SCORE_PREFS = "BrainTrainerScores"
    }

    /**
     * Generiše jedinstveni ključ za svaku kombinaciju modula i težine.
     * Npr. "HIGHSCORE_Module1_EASY"
     */
    private fun getKey(module: Module, difficulty: Difficulty): String {
        return "HIGHSCORE_${module.name}_${difficulty.name}"
    }

    /**
     * Čuva novi rezultat ako je veći od postojećeg najboljeg rezultata.
     */
    fun saveScore(module: Module, difficulty: Difficulty, newScore: Int) {
        val key = getKey(module, difficulty)
        val currentHighScore = getHighScore(module, difficulty)

        if (newScore > currentHighScore) {
            prefs.edit().putInt(key, newScore).apply()
        }
    }

    /**
     * Vraća najbolji rezultat za datu kombinaciju. Vraća 0 ako rezultat ne postoji.
     */
    fun getHighScore(module: Module, difficulty: Difficulty): Int {
        val key = getKey(module, difficulty)
        return prefs.getInt(key, 0)
    }

    /**
     * Vraća mapu svih najboljih rezultata, korisno za ekran sa rezultatima.
     */
    fun getAllHighScores(): Map<Module, Map<Difficulty, Int>> {
        // Prolazimo kroz sve module
        return Module.values().associateWith { module ->
            // Za svaki modul, prolazimo kroz sve težine
            Difficulty.values().associateWith { difficulty ->
                getHighScore(module, difficulty)
            }
        }
    }

    /**
     * NOVO: Briše sve sačuvane rezultate.
     * Ova funkcija se poziva iz SettingsViewModel-a kada korisnik potvrdi resetovanje.
     */
    fun resetAllScores() {
        prefs.edit().clear().apply()
    }
}
