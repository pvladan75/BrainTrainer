package com.program.braintrainer.chess.model

data class Problem(
    val id: String, // Jedinstveni ID zagonetke
    val module: Module, // Koji modul pripada ova zagonetka
    val difficulty: Difficulty, // Koji nivo težine ima ova zagonetka
    val fen: String, // FEN string pozicije za zagonetku
    val description: String, // Opis cilja zagonetke (npr. "Beli matira u 2 poteza")
    val solution: Solution, // Rešenje zagonetke
    val maxMovesAllowed: Int, // Maksimalan broj poteza dozvoljenih za rešavanje (za bodovanje)
    val baseScore: Int // Bazni broj poena za rešavanje zagonetke
)