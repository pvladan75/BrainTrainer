package com.program.braintrainer.rules

import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Move

import com.program.braintrainer.chess.model.Color


interface PuzzleRules {
    /**
     * Proverava da li je dati potez legalan za izvršavanje **u kontekstu pravila ovog modula**.
     * Ovo je mesto za dodatne provere izvan osnovnih šahovskih pravila (npr. "ne smeš u branjeno polje").
     * Pretpostavlja se da je potez već šahovski legalan.
     */
    fun isMoveValidForModule(move: Move, boardState: Board): Boolean

    /**
     * Proverava da li je cilj zagonetke dostignut u datom stanju table.
     */
    fun isGoalReached(boardState: Board): Boolean

    /**
     * Generiše sve **šahovski legalne poteze** za datog igrača u trenutnom stanju table.
     * Ova metoda ne bi trebalo da sadrži logiku specifičnu za modul, već samo osnovna šahovska pravila
     * (kretanje figura, provere šaha, blokade, itd.).
     * Implementacija bi verovatno pozivala neku globalnu ChessCore ili GameMechanics klasu.
     */
    fun getAllLegalChessMoves(boardState: Board, playerColor: Color): List<Move>
}