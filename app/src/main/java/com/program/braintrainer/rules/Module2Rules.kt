package com.program.braintrainer.rules

import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Color
import com.program.braintrainer.chess.model.Move


/**
 * Definiše pravila specifična za Modul 2.
 * Cilj: Pojesti sve crne figure.
 * Pravilo: Beli igrač ne sme stati na polje koje je napadnuto od strane crnih figura.
 */
class Module2Rules : PuzzleRules {

    /**
     * Proverava da li je cilj zagonetke (Modul 2) dostignut u datom stanju table.
     * Cilj: Pojesti sve crne figure.
     */
    override fun isGoalReached(boardState: Board): Boolean {
        return !boardState.hasBlackPiecesRemaining()
    }

    /**
     * Proverava da li je dati potez validan za izvršavanje u kontekstu pravila Modula 2.
     * Pravilo: Potez je validan ako ciljno polje NIJE napadnuto od strane crnih figura.
     */
    override fun isMoveValidForModule(move: Move, boardState: Board): Boolean {
        // Simulišemo stanje table NAKON poteza
        val boardAfterMove = boardState.applyMove(move.start, move.end) ?: return false

        // Proveravamo da li je polje na koje je figura sletela napadnuto od strane CRNIH figura.
        // Važno: Proveravamo na simuliranoj tabli (boardAfterMove).
        val attackedByBlack = boardAfterMove.getAttackedSquares(Color.BLACK)

        // Potez je validan za modul 2 ako odredišno polje NIJE napadnuto od protivnika.
        return !attackedByBlack.contains(move.end)
    }

    /**
     * Generiše sve šahovski legalne poteze za datog igrača,
     * i filtrira ih prema pravilima Modula 2.
     */
    override fun getAllLegalChessMoves(boardState: Board, playerColor: Color): List<Move> {
        val allSafeMoves = mutableListOf<Move>()

        // Prolazimo kroz sve figure na tabli
        for ((startSquare, piece) in boardState.pieces) {
            // Ako je figura u boji igrača koji je na potezu
            if (piece.color == playerColor) {
                // Uzimamo sve šahovski legalne poteze za tu figuru
                val legalMovesForPiece = boardState.getLegalMoves(startSquare)

                // Za svaki legalan potez, proveravamo da li zadovoljava pravilo modula
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
