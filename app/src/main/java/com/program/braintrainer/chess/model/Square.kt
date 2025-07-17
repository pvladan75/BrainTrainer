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

    /**
     * Proverava da li su koordinate polja unutar granica table (0-7 za x i y).
     */
    fun isValid(): Boolean {
        // Pošto init blok već osigurava da su file i rank validni pri kreiranju,
        // ovo je uglavnom za slučajeve kada se Square objekat kreira sa izmenjenim x/y
        // kroz matematičke operacije (npr. x + dx) koje još nisu pretvorene u novi Square.
        // U našem slučaju, kada se koristi Square.fromCoordinates, validacija je već tamo.
        // Ipak, zbog Board.kt logike koja eksplicitno poziva isValid() na novoizračunatim koordinatama,
        // ova funkcija je neophodna.
        return x in 0..7 && y in 0..7
    }

    /**
     * Vraća notaciju polja u šahovskom formatu (npr. "a1", "h8").
     * Ista funkcionalnost kao toString(), ali je eksplicitno pozvana u ChessScreen.kt.
     */
    fun toNotation(): String {
        return toString() // Pozivamo postojeću toString() metodu
    }

    companion object {
        fun fromCoordinates(x: Int, y: Int): Square {
            // fromCoordinates već ima require provere koje osiguravaju da su
            // x i y u opsegu 0-7 pre kreiranja Square objekta.
            // Zato je svaka Square instanca kreirana preko fromCoordinates već validna.
            require(x in 0..7) { "X coordinate must be between 0 and 7" }
            require(y in 0..7) { "Y coordinate must be between 0 and 7" }
            val file = 'a' + x
            val rank = y + 1
            return Square(file, rank)
        }
    }
}