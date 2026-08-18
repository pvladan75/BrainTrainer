package com.program.braintrainer.chess.solver

import android.util.Log
import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Move
import com.program.braintrainer.chess.model.Square
import com.program.braintrainer.rules.PuzzleRules
import java.util.ArrayDeque
import com.program.braintrainer.chess.model.Color as ChessColor

/**
 * Univerzalni solver za šahovske zagonetke koji koristi BFS algoritam.
 * Rešava zagonetke bazirane na pravilima definisanim u instanci [PuzzleRules].
 *
 * Pretraga ima budžet: [timeBudgetMillis] i [maxVisitedStates]. Bez njih je na
 * teškoj poziciji mogla da traje neograničeno dugo i da pojede memoriju, a
 * poziva se sa ekrana (hint). Dubina se namerno ne ograničava — rešenja u
 * assets-u idu i do 38 poteza.
 *
 * @param rules Instanca [PuzzleRules] koja definiše specifična pravila i cilj zagonetke.
 */
class UniversalPuzzleSolver(
    private val rules: PuzzleRules,
    private val timeBudgetMillis: Long = DEFAULT_TIME_BUDGET_MILLIS,
    private val maxVisitedStates: Int = DEFAULT_MAX_VISITED_STATES
) {

    /**
     * Čvor pretrage. Putanja se ne kopira u svako stanje, nego se rekonstruiše
     * unazad preko [parent] — ranije je svaki čvor nosio celu listu poteza.
     */
    private class SearchNode(
        val board: Board,
        val whitePieceSquare: Square?,
        val parent: SearchNode?,
        val move: Move?
    ) {
        val depth: Int = if (parent == null) 0 else parent.depth + 1

        fun path(): List<Move> {
            val moves = ArrayDeque<Move>()
            var node: SearchNode? = this
            while (node?.move != null) {
                moves.addFirst(node.move)
                node = node.parent
            }
            return moves.toList()
        }
    }

    /**
     * Pokušava da reši zagonetku počevši od [initialBoard] za datog [playerColor].
     *
     * @return [PuzzleSolution]; `isSolved` je false i kada je pretraga prekinuta
     * zbog budžeta, pa pozivalac tretira oba slučaja isto.
     */
    fun solve(initialBoard: Board, playerColor: ChessColor = ChessColor.WHITE): PuzzleSolution {
        val deadline = System.currentTimeMillis() + timeBudgetMillis
        val queue = ArrayDeque<SearchNode>()

        // FEN table plus pozicija bele figure identifikuju stanje — dovoljno da
        // se izbegnu ciklusi i ponovna obrada.
        val visitedStates = mutableSetOf<Pair<String, Square?>>()

        val initialWhitePieceSquare = initialBoard.pieces.entries
            .find { it.value.color == playerColor }?.key

        queue.offer(SearchNode(initialBoard, initialWhitePieceSquare, parent = null, move = null))
        visitedStates.add(Pair(initialBoard.toFEN(), initialWhitePieceSquare))

        Log.d(TAG, "Pokrećem univerzalni solver sa pravilima: ${rules::class.simpleName}")

        var expanded = 0
        while (queue.isNotEmpty()) {
            // Provera budžeta je na svakih 64 čvora da ne bi sama postala trošak.
            if (expanded++ % 64 == 0 && System.currentTimeMillis() > deadline) {
                return abandoned(initialBoard, "isteklo je $timeBudgetMillis ms", visitedStates.size)
            }
            if (visitedStates.size > maxVisitedStates) {
                return abandoned(initialBoard, "pređeno je $maxVisitedStates stanja", visitedStates.size)
            }

            val currentNode = queue.removeFirst()
            val currentBoard = currentNode.board

            if (rules.isGoalReached(currentBoard)) {
                val path = currentNode.path()
                Log.d(TAG, "Cilj dostignut u ${path.size} poteza: ${path.joinToString(" -> ")}")
                return PuzzleSolution(true, path, currentBoard, "Zagonetka rešena!")
            }

            for (move in rules.getAllLegalChessMoves(currentBoard, playerColor)) {
                if (!rules.isMoveValidForModule(move, currentBoard)) continue

                val nextBoard = currentBoard.applyMove(move.start, move.end) ?: continue
                val nextStateKey = Pair(nextBoard.toFEN(), move.end)

                if (visitedStates.add(nextStateKey)) {
                    queue.offer(SearchNode(nextBoard, move.end, currentNode, move))
                }
            }
        }

        Log.w(TAG, "Nije pronađeno rešenje za zagonetku sa pravilima: ${rules::class.simpleName}.")
        return PuzzleSolution(false, emptyList(), initialBoard, "Nije pronađeno rešenje.")
    }

    private fun abandoned(board: Board, razlog: String, visited: Int): PuzzleSolution {
        Log.w(TAG, "Pretraga prekinuta ($razlog, obrađeno $visited stanja).")
        return PuzzleSolution(false, emptyList(), board, "Pretraga je prekinuta.")
    }

    private companion object {
        const val TAG = "UniversalPuzzleSolver"

        /** Hint se traži sa ekrana — duže od ovoga korisnik doživljava kao zaglavljivanje. */
        const val DEFAULT_TIME_BUDGET_MILLIS = 3_000L

        /** Gornja granica za memoriju: svako stanje drži FEN string i tablu. */
        const val DEFAULT_MAX_VISITED_STATES = 200_000
    }
}

data class PuzzleSolution(
    val isSolved: Boolean,
    val path: List<Move>,
    val finalBoard: Board,
    val message: String
)
