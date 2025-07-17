package com.program.braintrainer.chess.model

enum class Difficulty(val label: String) { // Dodajemo 'val label: String'
    EASY("Lako"),
    MEDIUM("Srednje"),
    HARD("Teško");

    // Možeš dodati i druge podatke vezane za težinu ovde, npr. maksimalno vreme, broj poteza itd.
}