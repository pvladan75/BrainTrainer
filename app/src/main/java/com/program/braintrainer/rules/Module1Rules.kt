package com.program.braintrainer.rules

import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Color
import com.program.braintrainer.chess.model.Move

/**
 * Definiše pravila specifična za Modul 1.
 * Cilj: Pojesti sve crne figure.
 * Pravilo: Svaki potez mora biti uzimanje protivničke figure.
 */
class Module1Rules : PuzzleRules {

    /**
     * Proverava da li je dati potez validan za izvršavanje u kontekstu pravila Modula 1.
     * Pravilo: Potez mora biti hvatanje crne figure.
     *
     * @param move Potez koji se proverava.
     * @param boardState Trenutno stanje table (PRE nego što je potez izvršen).
     * @return true ako je potez validan po pravilima modula, inače false.
     */
    override fun isMoveValidForModule(move: Move, boardState: Board): Boolean {
        val targetPiece = boardState.getPiece(move.end)
        // Potez je validan za modul 1 samo ako na ciljnom polju postoji protivnička (crna) figura.
        return targetPiece != null && targetPiece.color == Color.BLACK
    }

    /**
     * Proverava da li je cilj zagonetke (Modul 1) dostignut u datom stanju table.
     * Cilj: Pojesti sve crne figure.
     *
     * @param boardState Trenutno stanje table.
     * @return true ako na tabli nema više crnih figura, inače false.
     */
    override fun isGoalReached(boardState: Board): Boolean {
        // Cilj je dostignut ako nema više crnih figura na tabli.
        // Koristimo postojeću `hasBlackPiecesRemaining` metodu iz Board.kt
        return !boardState.hasBlackPiecesRemaining()
    }

    /**
     * Generiše sve šahovski legalne poteze za datog igrača,
     * i filtrira ih prema pravilima Modula 1.
     * Pravilo: Svaki potez mora biti hvatanje crne figure.
     *
     * @param boardState Trenutno stanje table.
     * @param playerColor Boja igrača za koga se generišu potezi (uvek WHITE za ove zagonetke).
     * @return Lista Move objekata koji predstavljaju dozvoljene poteze.
     */
    override fun getAllLegalChessMoves(boardState: Board, playerColor: Color): List<Move> {
        val allLegalCaptureMoves = mutableListOf<Move>()

        // Prolazimo kroz sve figure na tabli
        for ((startSquare, piece) in boardState.pieces) {
            // Ako je figura u boji igrača koji je na potezu
            if (piece.color == playerColor) {
                // Uzimamo sve šahovski legalne poteze za tu figuru
                // Metoda `getLegalMoves` iz Board.kt već proverava sve (kretanje, blokade, šah)
                val legalMovesForPiece = boardState.getLegalMoves(startSquare)

                // Filtriramo samo one poteze koji su uzimanje protivničke figure
                for (endSquare in legalMovesForPiece) {
                    val targetPiece = boardState.getPiece(endSquare)
                    if (targetPiece != null && targetPiece.color != playerColor) {
                        allLegalCaptureMoves.add(Move(startSquare, endSquare))
                    }
                }
            }
        }
        return allLegalCaptureMoves
    }
}
