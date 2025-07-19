package com.program.braintrainer.chess.solver

import android.util.Log
import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Move
import com.program.braintrainer.chess.model.Color as ChessColor
import com.program.braintrainer.chess.model.Square
import com.program.braintrainer.rules.PuzzleRules
import java.util.ArrayDeque

/**
 * Univerzalni solver za šahovske zagonetke koji koristi BFS algoritam.
 * Rešava zagonetke bazirane na pravilima definisanim u instanci [PuzzleRules].
 *
 * @param rules Instanca [PuzzleRules] koja definiše specifična pravila i cilj zagonetke.
 */
class UniversalPuzzleSolver(private val rules: PuzzleRules) {

    private val TAG = "UniversalPuzzleSolver"

    /**
     * Data klasa koja predstavlja stanje pretrage unutar BFS algoritma.
     *
     * @param board Trenutno stanje šahovske table.
     * @param currentPath Lista poteza koji su doveli do [board] stanja.
     * @param whitePieceSquare Trenutna pozicija bele figure (ako je relevantno i ako je samo jedna).
     * Koristi se za efikasnije praćenje posećenih stanja.
     */
    data class SolverState(
        val board: Board,
        val currentPath: List<Move>,
        val whitePieceSquare: Square? // Pozicija ključne bele figure za praćenje stanja
    )

    /**
     * Pokušava da reši zagonetku počevši od [initialBoard] za datog [playerColor].
     *
     * @param initialBoard Početno stanje šahovske table.
     * @param playerColor Boja igrača koji rešava zagonetku (obično PieceColor.WHITE).
     * @return [PuzzleSolution] objekat koji sadrži rezultat rešavanja.
     */
    fun solve(initialBoard: Board, playerColor: ChessColor = ChessColor.WHITE): PuzzleSolution {
        val queue = ArrayDeque<SolverState>()
        // Koristimo FEN notaciju table i poziciju bele figure za praćenje posećenih stanja
        // da bismo izbegli cikluse i ponovnu obradu istih stanja.
        val visitedStates = mutableSetOf<Pair<String, Square?>>()

        // Pronalazimo poziciju bele figure na početnoj tabli.
        // Pretpostavljamo da je samo jedna bela figura relevantna za ove zagonetke.
        val initialWhitePieceEntry = initialBoard.pieces.entries.find { it.value.color == playerColor }
        val initialWhitePieceSquare = initialWhitePieceEntry?.key

        // Inicijalno stanje za BFS
        val initialState = SolverState(
            board = initialBoard,
            currentPath = emptyList(),
            whitePieceSquare = initialWhitePieceSquare
        )
        queue.offer(initialState)
        // Pretpostavka: Vaša Board klasa ima toFEN() metodu
        visitedStates.add(Pair(initialBoard.toFEN(), initialWhitePieceSquare))

        Log.d(TAG, "Pokrećem univerzalni solver sa pravilima: ${rules::class.simpleName}")

        while (queue.isNotEmpty()) {
            val currentState = queue.removeFirst()
            val currentBoard = currentState.board
            val currentPath = currentState.currentPath

            // Korak 1: Proveravamo da li je cilj dostignut sa trenutnom tablom
            if (rules.isGoalReached(currentBoard)) {
                Log.d(TAG, "Cilj dostignut! Putanja: ${currentPath.joinToString(" -> ")}")
                return PuzzleSolution(true, currentPath, currentBoard, "Zagonetka rešena!")
            }

            // Korak 2: Generišemo sve legalne poteze za trenutnog igrača (beli)
            val legalMovesForPlayer = rules.getAllLegalChessMoves(currentBoard, playerColor)

            for (move in legalMovesForPlayer) {
                // Korak 3: Pre-provera validnosti poteza prema pravilima modula.
                if (!rules.isMoveValidForModule(move, currentBoard)) {
                    continue // Potez nije validan po pravilima modula, pređi na sledeći
                }

                // Korak 4: Simuliramo potez i dobijamo novo stanje table
                // Pretpostavka: Vaša Board klasa ima applyMove metodu
                val nextBoard = currentBoard.applyMove(move.start, move.end) ?: continue
                val newWhitePieceSquare = move.end

                // Korak 5: Kreiramo ključ za posećena stanja i proveravamo da li je stanje već posećeno
                val nextStateKey = Pair(nextBoard.toFEN(), newWhitePieceSquare)

                if (nextStateKey !in visitedStates) {
                    visitedStates.add(nextStateKey)
                    val newPath = currentPath + move
                    queue.offer(SolverState(nextBoard, newPath, newWhitePieceSquare))
                }
            }
        }

        Log.w(TAG, "Nije pronađeno rešenje za zagonetku sa pravilima: ${rules::class.simpleName}.")
        return PuzzleSolution(false, emptyList(), initialBoard, "Nije pronađeno rešenje.")
    }
}

// Potrebno je definisati i ovu data klasu u vašem projektu, ako već ne postoji
data class PuzzleSolution(
    val isSolved: Boolean,
    val path: List<Move>,
    val finalBoard: Board,
    val message: String
)
