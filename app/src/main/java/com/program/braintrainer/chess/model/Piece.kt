package com.program.braintrainer.chess.model

data class Piece(val type: PieceType, val color: Color) {

    /** Slovo figure u FEN notaciji — veliko za belu, malo za crnu. */
    fun toFenChar(): Char {
        val char = when (type) {
            PieceType.PAWN -> 'p'
            PieceType.KNIGHT -> 'n'
            PieceType.BISHOP -> 'b'
            PieceType.ROOK -> 'r'
            PieceType.QUEEN -> 'q'
            PieceType.KING -> 'k'
        }
        return if (color == Color.WHITE) char.uppercaseChar() else char
    }
}