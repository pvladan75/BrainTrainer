package com.program.braintrainer.chess.model.data

import android.content.Context
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.chess.model.Problem
import java.io.IOException
import kotlin.random.Random

class ProblemLoader(private val context: Context) {

    /**
     * Učitava [count] nasumičnih zagonetki za dati modul i težinu.
     *
     * Fajlovi u assets-u su JSONL, imenovani npr. "module1_easy_puzzles.jsonl",
     * sa jednom zagonetkom po redu. Uzorak bira [ProblemSampler] u jednom
     * prolazu, bez učitavanja i parsiranja celog fajla.
     */
    fun loadRandomProblems(
        module: Module,
        difficulty: Difficulty,
        count: Int,
        random: Random = Random.Default
    ): List<Problem> {
        val fileName = "${module.name.lowercase()}_${difficulty.name.lowercase()}_puzzles.jsonl"

        return try {
            context.assets.open(fileName).use { stream ->
                ProblemSampler.sample(stream, count, random)
            }
        } catch (ioException: IOException) {
            println("ERROR: Could not load file $fileName. Does it exist in assets and is named correctly? ${ioException.message}")
            ioException.printStackTrace()
            emptyList()
        }
    }
}
