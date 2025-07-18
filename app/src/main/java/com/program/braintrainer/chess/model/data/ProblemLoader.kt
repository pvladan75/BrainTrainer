package com.program.braintrainer.chess.data

import android.content.Context
import com.program.braintrainer.chess.model.Difficulty
import com.program.braintrainer.chess.model.Module
import com.program.braintrainer.chess.model.Problem
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.IOException

class ProblemLoader(private val context: Context) {

    // Konfiguracija Json objekta
    private val json = Json {
        ignoreUnknownKeys = true // Ignoriše nepoznate ključeve u JSON-u (dobro za kompatibilnost unazad)
        prettyPrint = true     // Formatira izlaz ako serializuješ (nije primarno za deserializaciju, ali korisno)
    }

    /**
     * Učitava listu šahovskih problema iz JSON fajla za dati modul i težinu.
     * Fajlovi su imenovani npr. "module1_easy_puzzles.json", i svaki fajl
     * sada sadrži LISTU objekata Problem.
     */
    fun loadProblemsForModuleAndDifficulty(module: Module, difficulty: Difficulty): List<Problem> {
        val fileName = "${module.name.lowercase()}_${difficulty.name.lowercase()}_puzzles.json"

        return try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            // Sada parsiramo kao LISTU Problem objekata
            json.decodeFromString<List<Problem>>(jsonString).shuffled()
        } catch (ioException: IOException) {
            println("ERROR: Could not load file $fileName. Does it exist in assets and is named correctly? ${ioException.message}")
            ioException.printStackTrace()
            emptyList() // Vraća praznu listu ako fajl ne postoji ili je problem sa I/O
        } catch (serializationException: Exception) {
            println("ERROR: Could not parse JSON from $fileName. Check JSON structure (should be a list of problems) and Problem data class. ${serializationException.message}")
            serializationException.printStackTrace()
            emptyList() // Vraća praznu listu ako dođe do greške prilikom parsiranja JSON-a
        }
    }
}