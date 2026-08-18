package com.program.braintrainer.chess

import com.program.braintrainer.chess.model.Board
import com.program.braintrainer.chess.model.Color
import com.program.braintrainer.chess.model.Square
import com.program.braintrainer.chess.model.data.ProblemSampler
import com.program.braintrainer.chess.parser.FenParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Generisanje poteza je ubrzano tako što se kandidati izvode iz pravila kretanja
 * figure umesto da se proverava svih 64 polja. Ovi testovi porede novi rezultat
 * sa starom, sirovom logikom na stvarnim pozicijama iz assets-a.
 */
class BoardMoveGenerationTest {

    /** Stara implementacija: probaj svako polje na tabli. */
    private fun bruteForceLegalMoves(board: Board, start: Square): List<Square> {
        val moves = mutableListOf<Square>()
        for (x in 0..7) {
            for (y in 0..7) {
                val end = Square.fromCoordinates(x, y)
                if (start != end && board.isValidMove(start, end)) moves.add(end)
            }
        }
        return moves
    }

    private fun boardsFromAssets(count: Int): List<Board> {
        val boards = mutableListOf<Board>()
        for (file in File("src/main/assets").listFiles()!!.filter { it.extension == "jsonl" }) {
            ProblemSampler.sample(file.inputStream(), count, Random(7)).forEach { problem ->
                boards.add(FenParser.parseFenToBoard(problem.fen).first)
            }
        }
        return boards
    }

    @Test
    fun `getLegalMoves daje isti rezultat kao provera svih 64 polja`() {
        val boards = boardsFromAssets(40)
        assertTrue(boards.size > 100)

        for (board in boards) {
            for (start in board.pieces.keys) {
                assertEquals(
                    "pozicija ${board.toFEN()}, figura na $start",
                    bruteForceLegalMoves(board, start),
                    board.getLegalMoves(start)
                )
            }
        }
    }

    @Test
    fun `isSquareAttackedBy se slaze sa getAttackedSquares`() {
        for (board in boardsFromAssets(20)) {
            for (color in Color.entries) {
                val attacked = board.getAttackedSquares(color)
                for (x in 0..7) {
                    for (y in 0..7) {
                        val square = Square.fromCoordinates(x, y)
                        // Polje na kome napadač stoji nije "napadnuto" u novoj proveri;
                        // stara je i to izbegavala jer figura ne napada svoje polje.
                        assertEquals(
                            "pozicija ${board.toFEN()}, polje $square, boja $color",
                            attacked.contains(square),
                            board.isSquareAttackedBy(square, color)
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `ista koordinata uvek daje istu instancu polja`() {
        assertTrue(Square.fromCoordinates(3, 4) === Square.fromCoordinates(3, 4))
        assertEquals(Square('d', 5), Square.fromCoordinates(3, 4))
    }
}
