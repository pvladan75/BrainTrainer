package com.program.braintrainer.chess.model

data class Square(val file: Char, val rank: Int) { // npr. 'a', 1
    val x: Int = file.lowercaseChar() - 'a' // 0-7 za a-h
    val y: Int = rank - 1 // 0-7 za 1-8

    init {
        require(file in 'a'..'h') { "File must be between 'a' and 'h'" }
        require(rank in 1..8) { "Rank must be between 1 and 8" }
    }

    override fun toString(): String {
        return "$file$rank"
    }

    companion object {
        /**
         * Svih 64 polja postoje unapred. Generisanje poteza i solver traže polja
         * u petljama, a polje je nepromenljivo — nema razloga da se alocira novo.
         */
        private val ALL = Array(64) { index -> Square('a' + index / 8, index % 8 + 1) }

        fun fromCoordinates(x: Int, y: Int): Square {
            require(x in 0..7) { "X coordinate must be between 0 and 7" }
            require(y in 0..7) { "Y coordinate must be between 0 and 7" }
            return ALL[x * 8 + y]
        }
    }
}