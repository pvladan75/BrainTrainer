package com.program.braintrainer.chess.parser

import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Color
import com.program.braintrainer.chess.model.Piece
import com.program.braintrainer.chess.model.PieceType
import com.program.braintrainer.chess.model.Square // Make sure this is imported

object FenParser {

    fun parseFenToBoard(fen: String): Pair<Board, Color> {
        val parts = fen.split(" ")
        val piecePlacement = parts[0]
        val activeColorChar = parts[1]

        var board = Board() // Start with an empty board

        var rank = 7 // FEN starts from rank 8 (index 7)
        var file = 0 // FEN starts from file 'a' (index 0)

        for (char in piecePlacement) {
            when {
                char.isDigit() -> {
                    file += char.toString().toInt()
                }
                char == '/' -> {
                    rank--
                    file = 0
                }
                else -> {
                    val pieceColor = if (char.isUpperCase()) Color.WHITE else Color.BLACK
                    val pieceType = when (char.lowercaseChar()) {
                        'p' -> PieceType.PAWN
                        'n' -> PieceType.KNIGHT
                        'b' -> PieceType.BISHOP
                        'r' -> PieceType.ROOK
                        'q' -> PieceType.QUEEN
                        'k' -> PieceType.KING
                        else -> throw IllegalArgumentException("Unknown piece character: $char")
                    }
                    val piece = Piece(pieceType, pieceColor)

                    // CORRECTED LINE: Create a Square object
                    val square = Square.fromCoordinates(file, rank)
                    board = board.setPiece(square, piece) // CORRECTED: Pass Square object and Piece
                    file++
                }
            }
        }

        val activeColor = if (activeColorChar == "w") Color.WHITE else Color.BLACK

        return Pair(board, activeColor)
    }

    fun toFenString(board: Board, activeColor: Color): String {
        val stringBuilder = StringBuilder()

        for (rank in 7 downTo 0) { // Iterate from rank 8 down to 1
            var emptySquares = 0
            for (file in 0..7) { // Iterate from file 'a' to 'h'
                // CORRECTED LINE: Create a Square object
                val square = Square.fromCoordinates(file, rank)
                val piece = board.getPiece(square) // CORRECTED: Pass Square object

                if (piece == null) {
                    emptySquares++
                } else {
                    if (emptySquares > 0) {
                        stringBuilder.append(emptySquares)
                        emptySquares = 0
                    }
                    val pieceChar = when (piece.type) {
                        PieceType.PAWN -> 'p'
                        PieceType.KNIGHT -> 'n'
                        PieceType.BISHOP -> 'b'
                        PieceType.ROOK -> 'r'
                        PieceType.QUEEN -> 'q'
                        PieceType.KING -> 'k'
                    }
                    stringBuilder.append(if (piece.color == Color.WHITE) pieceChar.uppercaseChar() else pieceChar)
                }
            }
            if (emptySquares > 0) {
                stringBuilder.append(emptySquares)
            }
            if (rank > 0) {
                stringBuilder.append("/")
            }
        }

        stringBuilder.append(" ")
        stringBuilder.append(if (activeColor == Color.WHITE) "w" else "b")
        stringBuilder.append(" KQkq - 0 1") // Simplified castling, en passant, halfmove, fullmove

        return stringBuilder.toString()
    }

    /**
     * Parsira potez u algebarskoj notaciji (npr. "e2e4") u par Square objekata.
     * @return Pair<Square, Square> - Prvi element je početno polje, drugi je krajnje polje.
     * @throws IllegalArgumentException ako notacija nije validna.
     */
    fun parseMove(move: String): Pair<Square, Square> {
        if (move.length != 4) {
            throw IllegalArgumentException("Nevažeća dužina poteza: $move. Očekivano 4 karaktera (npr. e2e4).")
        }
        val startFileChar = move[0]
        val startRankChar = move[1]
        val endFileChar = move[2]
        val endRankChar = move[3]

        val startFile = startFileChar - 'a'
        val startRank = startRankChar.toString().toInt() - 1

        val endFile = endFileChar - 'a'
        val endRank = endRankChar.toString().toInt() - 1

        val startSquare = Square.fromCoordinates(startFile, startRank)
        val endSquare = Square.fromCoordinates(endFile, endRank)

        return Pair(startSquare, endSquare)
    }
}