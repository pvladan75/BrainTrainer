package com.program.braintrainer.chess.parser

import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Color
import com.program.braintrainer.chess.model.Piece
import com.program.braintrainer.chess.model.PieceType
import com.program.braintrainer.chess.model.Square

object FenParser {

    /**
     * Parsira FEN string i popunjava Board objekat.
     * Trenutno parsira samo raspored figura i čiji je red na potezu.
     * Ostali delovi FEN-a (rokada, en passant, brojači) su ignorisani za potrebe ove metode.
     */
    fun parseFenToBoard(fen: String): Pair<Board, Color> {
        val parts = fen.split(" ")
        require(parts.size >= 2) { "Invalid FEN string: Too few parts." }

        val piecePlacement = parts[0]
        val activeColorChar = parts[1]

        val board = Board()

        // 1. Parsiranje rasporeda figura
        val ranks = piecePlacement.split("/")
        require(ranks.size == 8) { "Invalid FEN string: Incorrect number of ranks." }

        for (y in 0..7) { // Iteriramo od ranka 8 (y=7) do ranka 1 (y=0)
            val rankFen = ranks[7 - y] // FEN počinje od 8. ranka (gore), naša matrica od 0. (dole)
            var x = 0 // file 'a' (x=0)

            for (char in rankFen) {
                if (char.isDigit()) {
                    x += char.toString().toInt() // Prazna polja
                } else {
                    val piece = pieceFromFenChar(char)
                    if (piece != null) {
                        board.setPiece(x, y, piece)
                    }
                    x++
                }
            }
        }

        // 2. Parsiranje čiji je red na potezu
        val activeColor = when (activeColorChar.lowercase()) {
            "w" -> Color.WHITE
            "b" -> Color.BLACK
            else -> throw IllegalArgumentException("Invalid FEN string: Unknown active color '$activeColorChar'.")
        }

        return Pair(board, activeColor)
    }

    /**
     * Pomoćna funkcija za pretvaranje FEN karaktera u Piece objekat.
     */
    private fun pieceFromFenChar(fenChar: Char): Piece? {
        val color = if (fenChar.isUpperCase()) Color.WHITE else Color.BLACK
        val type = when (fenChar.lowercaseChar()) {
            'p' -> PieceType.PAWN
            'n' -> PieceType.KNIGHT
            'b' -> PieceType.BISHOP
            'r' -> PieceType.ROOK
            'q' -> PieceType.QUEEN
            'k' -> PieceType.KING
            else -> null // Nepoznat karakter
        }
        return if (type != null) Piece(type, color) else null
    }

    /**
     * Konvertuje trenutno stanje Board objekta i aktivnu boju u FEN string.
     * Generiše samo deo FEN-a koji se odnosi na raspored figura i aktivnu boju.
     * Ostali delovi (rokada, en passant, brojači) su postavljeni na podrazumevane vrednosti.
     */
    fun boardToFen(board: Board, activeColor: Color): String {
        val piecePlacementBuilder = StringBuilder()

        for (y in 7 downTo 0) { // Iteriramo od ranka 8 (y=7) do ranka 1 (y=0)
            var emptyCount = 0
            for (x in 0..7) { // Iteriramo kroz fajlove a-h
                val piece = board.getPiece(x, y)
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        piecePlacementBuilder.append(emptyCount)
                        emptyCount = 0
                    }
                    piecePlacementBuilder.append(piece.toFenChar())
                }
            }
            if (emptyCount > 0) {
                piecePlacementBuilder.append(emptyCount)
            }
            if (y > 0) {
                piecePlacementBuilder.append("/")
            }
        }

        val activeColorChar = if (activeColor == Color.WHITE) "w" else "b"

        // Za sada, ostali FEN delovi su defaultni
        val castlingRights = "-" // Nema rokada (za zagonetke ti možda ovo i ne treba)
        val enPassantTarget = "-" // Nema en passant (za zagonetke ti možda ovo i ne treba)
        val halfmoveClock = "0" // Brojač pola poteza
        val fullmoveNumber = "1" // Broj punih poteza

        return "${piecePlacementBuilder.toString()} $activeColorChar $castlingRights $enPassantTarget $halfmoveClock $fullmoveNumber"
    }
}