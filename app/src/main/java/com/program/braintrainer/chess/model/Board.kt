package com.program.braintrainer.chess.model

class Board {
    private val squares: Array<Array<Piece?>> = Array(8) { arrayOfNulls(8) }

    init {
        // Inicijalno prazna tabla, ili je možeš postaviti na standardnu startnu poziciju
        // Pošto ćeš učitavati iz FEN-a, verovatno ti ne treba standardna pozicija ovde.
    }

    fun getPiece(square: Square): Piece? {
        return squares[square.y][square.x]
    }

    fun setPiece(square: Square, piece: Piece?) {
        squares[square.y][square.x] = piece
    }

    fun getPiece(x: Int, y: Int): Piece? {
        if (x !in 0..7 || y !in 0..7) return null
        return squares[y][x]
    }

    fun setPiece(x: Int, y: Int, piece: Piece?) {
        if (x !in 0..7 || y !in 0..7) return
        squares[y][x] = piece
    }

    /**
     * Prints a basic representation of the board to the console for debugging.
     */
    fun printBoard() {
        println("  a b c d e f g h")
        println(" +-----------------+")
        for (y in 7 downTo 0) { // Rank 8 to 1
            print("${y + 1}|")
            for (x in 0..7) { // File a to h
                val piece = getPiece(x, y)
                val char = piece?.toFenChar() ?: '.'
                print("$char ")
            }
            println("|${y + 1}")
        }
        println(" +-----------------+")
        println("  a b c d e f g h")
    }

    // Možda ćeš kasnije dodati metodu za resetovanje table, kopiranje table itd.
}