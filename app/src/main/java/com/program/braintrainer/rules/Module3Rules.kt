package com.program.braintrainer.rules

import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Color
import com.program.braintrainer.chess.model.Move
import com.program.braintrainer.chess.model.Piece
import com.program.braintrainer.chess.model.PieceType

/**
 * Definiše pravila specifična za Modul 3.
 * Cilj: Pojesti crnog kralja.
 * Pravilo: Beli igrač ne sme stati na polje koje je napadnuto od strane crnih figura.
 */
class Module3Rules : PuzzleRules {

    /**
     * Proverava da li je cilj zagonetke (Modul 3) dostignut u datom stanju table.
     * Cilj: Pojesti crnog kralja.
     */
    override fun isGoalReached(boardState: Board): Boolean {
        // Cilj je dostignut ako na tabli više ne postoji crni kralj.
        val blackKingPresent = boardState.pieces.any { (_, piece) ->
            piece.type == PieceType.KING && piece.color == Color.BLACK
        }
        return !blackKingPresent
    }

    /**
     * Proverava da li je dati potez validan za izvršavanje u kontekstu pravila Modula 3.
     * Pravilo je isto kao i za Modul 2.
     */
    override fun isMoveValidForModule(move: Move, boardState: Board): Boolean {
        // Simulišemo stanje table NAKON poteza
        val boardAfterMove = boardState.applyMove(move.start, move.end) ?: return false

        // Proveravamo da li je polje na koje je figura sletela napadnuto od strane CRNIH figura.
        val attackedByBlack = boardAfterMove.getAttackedSquares(Color.BLACK)

        // Potez je validan ako odredišno polje NIJE napadnuto.
        return !attackedByBlack.contains(move.end)
    }

    /**
     * Generiše sve šahovski legalne poteze za datog igrača,
     * i filtrira ih prema pravilima Modula 3.
     */
    override fun getAllLegalChessMoves(boardState: Board, playerColor: Color): List<Move> {
        val allSafeMoves = mutableListOf<Move>()

        // Logika je identična kao za Modul 2: pronalazimo sve poteze koji se završavaju na sigurnom polju.
        for ((startSquare, piece) in boardState.pieces) {
            if (piece.color == playerColor) {
                val legalMovesForPiece = boardState.getLegalMoves(startSquare)
                for (endSquare in legalMovesForPiece) {
                    val move = Move(startSquare, endSquare)
                    if (isMoveValidForModule(move, boardState)) {
                        allSafeMoves.add(move)
                    }
                }
            }
        }
        return allSafeMoves
    }
}
